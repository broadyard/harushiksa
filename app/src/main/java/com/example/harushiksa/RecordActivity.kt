package com.example.harushiksa

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.harushiksa.adapter.RecordFoodAdapter
import com.example.harushiksa.db.Food
import com.example.harushiksa.db.FoodDatabaseHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import android.graphics.Color
import android.text.style.ForegroundColorSpan

// 날짜 선택, 음식 기록 보여주는 화면
class RecordActivity : AppCompatActivity() {

    private lateinit var calendarView: MaterialCalendarView
    private lateinit var textDate: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var dbHelper: FoodDatabaseHelper
    private lateinit var btnAddFromList: ImageButton

    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        calendarView = findViewById(R.id.calendarView)
        textDate = findViewById(R.id.textDate)
        recyclerView = findViewById(R.id.foodRecyclerView)
        btnAddFromList = findViewById(R.id.btnAddFromList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        dbHelper = FoodDatabaseHelper(this)

        // 음식 기록이 있는 날짜에 점 표시
        val datesWithFood = dbHelper.getAllFoods()
            .filter { it.date.isNotEmpty() }
            .mapNotNull {
                val parts = it.date.split("-")
                if (parts.size == 3) {
                    val year = parts[0].toInt()
                    val month = parts[1].toInt()
                    val day = parts[2].toInt()
                    CalendarDay.from(year, month - 1, day)
                } else null
            }.toHashSet()
        calendarView.addDecorator(FoodDotDecorator(datesWithFood))

        // 오늘 날짜로 초기 표시
        val today = CalendarDay.today()
        calendarView.selectedDate = today
        selectedDate = String.format("%04d-%02d-%02d", today.year, today.month + 1, today.day)
        textDate.text = "선택한 날짜: $selectedDate"
        updateFoodListForDate(selectedDate)

        // 날짜 선택 이벤트
        calendarView.setOnDateChangedListener { _, date, _ ->
            selectedDate = String.format("%04d-%02d-%02d", date.year, date.month + 1, date.day)
            textDate.text = "선택한 날짜: $selectedDate"
            updateFoodListForDate(selectedDate)
        }

        // 음식 추가 버튼
        btnAddFromList.setOnClickListener {
            showFoodSelectBottomSheet()
        }

        // 통계 화면 이동
        findViewById<ImageButton>(R.id.btnRank).setOnClickListener {
            startActivity(Intent(this, RecordStatsActivity::class.java))
        }

        // 하단 내비게이션
        findViewById<ImageButton>(R.id.nav_home).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<ImageButton>(R.id.nav_record).setOnClickListener {

        }
        findViewById<ImageButton>(R.id.nav_manage).setOnClickListener {
            startActivity(Intent(this, FoodManageActivity::class.java))
            finish()
        }
    }

    // 날짜별 음식 목록 업데이트
    private fun updateFoodListForDate(date: String) {
        val foodList: List<Food> = dbHelper.getFoodsByDate(date)
        val foodNames = foodList.map { it.name }.toMutableList()
        val imageMap = mutableMapOf<String, Uri>()

        for (item in foodList) {
            if (item.imageUri.isNotEmpty()) {
                imageMap[item.name] = Uri.parse(item.imageUri)
            }
        }

        recyclerView.adapter = RecordFoodAdapter(
            context = this,
            foodList = foodNames,
            imageMap = imageMap,
            onDeleteClick = { foodName ->
                dbHelper.deleteFoodByNameAndDate(foodName, date)
                updateFoodListForDate(date) // 삭제 후 갱신
            }
        )
    }

    // 음식 리스트에서 선택해 추가하는 바텀시트
    private fun showFoodSelectBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.food_select, null)
        dialog.setContentView(view)

        val editSearch = view.findViewById<EditText>(R.id.editSearchFood)
        val listView = view.findViewById<ListView>(R.id.listViewFood)

        val baseFoods = dbHelper.getAllFoods().filter { it.date.isEmpty() }.distinctBy { it.name }
        val foodNames = baseFoods.map { it.name }.toMutableList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, foodNames)
        listView.adapter = adapter

        editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val keyword = s.toString()
                val filtered = baseFoods.map { it.name }.filter { name -> name.contains(keyword) }
                adapter.clear()
                adapter.addAll(filtered)
                adapter.notifyDataSetChanged()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedName = adapter.getItem(position)
            val matched = baseFoods.firstOrNull { it.name == selectedName }
            if (matched != null) {
                val foodToInsert = matched.copy(date = selectedDate)
                dbHelper.insertFood(foodToInsert)
                updateFoodListForDate(selectedDate)
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // 날짜에 점 찍는 장식
    class FoodDotDecorator(private val dates: Set<CalendarDay>) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean = dates.contains(day)
        override fun decorate(view: DayViewFacade) {
            view.addSpan(ForegroundColorSpan(Color.parseColor("#08d501")))
        }
    }
}
