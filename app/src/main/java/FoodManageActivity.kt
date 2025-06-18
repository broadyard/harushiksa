package com.example.harushiksa

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.harushiksa.db.Food
import com.example.harushiksa.db.FoodDatabaseHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FoodManageActivity : AppCompatActivity() {

    private lateinit var dbHelper: FoodDatabaseHelper
    private lateinit var foodListLayout: LinearLayout
    private lateinit var editNewFood: EditText
    private lateinit var btnAddFood: Button

    private var selectedImageUri: Uri? = null
    private var tempNewFoodName: String? = null

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val excludedFoods = mutableSetOf<String>()

    private val imageMap = mutableMapOf<String, String>()

    // 이미지 선택 후 결과 처리
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                tempNewFoodName?.let { foodName ->
                    val savedUriString = saveImageToInternalStorage(uri, foodName)
                    dbHelper.insertFood(Food(0, foodName, "", savedUriString))
                    imageMap[foodName] = savedUriString
                    renderFoodList()
                    editNewFood.text.clear()
                    Toast.makeText(this, "$foodName 추가됨", Toast.LENGTH_SHORT).show()
                    tempNewFoodName = null
                }
            }
        } else {
            Toast.makeText(this, "이미지 선택이 취소되었습니다.", Toast.LENGTH_SHORT).show()
            tempNewFoodName = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_manage)

        // 초기화
        dbHelper = FoodDatabaseHelper(this)
        foodListLayout = findViewById(R.id.foodListLayout)
        editNewFood = findViewById(R.id.editNewFood)
        btnAddFood = findViewById(R.id.btnAddFood)
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        editor = prefs.edit()

        excludedFoods.addAll(prefs.getStringSet("excludedFoods", emptySet()) ?: emptySet())

        renderFoodList()

        // 음식 추가 버튼 클릭
        btnAddFood.setOnClickListener {
            val newFood = editNewFood.text.toString().trim()
            if (newFood.isNotEmpty()) {
                tempNewFoodName = newFood
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                imagePickerLauncher.launch(intent)
            } else {
                Toast.makeText(this, "음식 이름을 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }

        // 하단 네비게이션
        findViewById<ImageButton>(R.id.nav_home).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<ImageButton>(R.id.nav_record).setOnClickListener {
            startActivity(Intent(this, RecordActivity::class.java))
            finish()
        }
        findViewById<ImageButton>(R.id.nav_manage).setOnClickListener {
            // 현재 페이지
        }
    }

    // 음식 리스트를 화면에 표시
    private fun renderFoodList() {
        foodListLayout.removeAllViews()

        val foods = dbHelper.getAllFoods().filter { it.date.isEmpty() }

        for (food in foods) {
            val row = LayoutInflater.from(this).inflate(R.layout.item_manage_food, null)
            val imgFood = row.findViewById<ImageView>(R.id.imgFood)
            val tvFoodName = row.findViewById<TextView>(R.id.tvFoodName)
            val checkBox = row.findViewById<CheckBox>(R.id.checkbox)
            val btnDelete = row.findViewById<ImageButton>(R.id.btnDelete)

            tvFoodName.text = food.name
            checkBox.isChecked = !excludedFoods.contains(food.name)

            // 체크박스 상태 저장
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (!isChecked) excludedFoods.add(food.name) else excludedFoods.remove(food.name)
                editor.putStringSet("excludedFoods", excludedFoods)
                editor.apply()
            }

            // 이미지 로드
            try {
                if (food.imageUri.startsWith("default:")) {
                    val drawableName = food.imageUri.substringAfter("default:")
                    val resId = resources.getIdentifier(drawableName, "drawable", packageName)
                    imgFood.setImageResource(if (resId != 0) resId else R.drawable.placeholder_image)
                } else {
                    val uri = Uri.parse(food.imageUri)
                    val file = File(uri.path ?: "")
                    if (file.exists()) {
                        imgFood.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                    } else {
                        Log.e("IMAGE_LOAD", "파일 없음: ${uri.path}")
                        imgFood.setImageResource(R.drawable.placeholder_image)
                    }
                }
            } catch (e: Exception) {
                Log.e("IMAGE_LOAD", "이미지 로딩 실패", e)
                imgFood.setImageResource(R.drawable.placeholder_image)
            }

            // 삭제 버튼 클릭 시 DB 및 UI 갱신
            btnDelete.setOnClickListener {
                dbHelper.deleteFoodByName(food.name)
                excludedFoods.remove(food.name)
                editor.putStringSet("excludedFoods", excludedFoods)
                editor.apply()
                renderFoodList()
                Toast.makeText(this, "${food.name} 삭제됨", Toast.LENGTH_SHORT).show()
            }

            foodListLayout.addView(row)
        }
    }

    // 이미지 내부 저장소에 저장 후 Uri 반환
    private fun saveImageToInternalStorage(uri: Uri, fileName: String): String {
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(filesDir, "$fileName.jpg")
        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return Uri.fromFile(file).toString()
    }
}
