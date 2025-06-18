package com.example.harushiksa

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.harushiksa.db.Food
import com.example.harushiksa.db.FoodDatabaseHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: FoodDatabaseHelper
    private var currentRecommendation: String? = null // 현재 추천된 음식 이름
    private var currentImagePath: String? = null // 현재 추천된 음식 이미지 경로

    override fun onCreate(savedInstanceState: Bundle?) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // 앱 실행 시 강제로 라이트 모드 설정
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = FoodDatabaseHelper(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // 사용자에게 설정 권한 요청 화면으로 이동
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
            } else {
                setMealReminder(this)
            }
        } else {
            setMealReminder(this)
        }


        val btnRecommend = findViewById<Button>(R.id.btnRecommend) // 음식 추천 버튼
        val btnAddToday = findViewById<Button>(R.id.btnAddToday) // 오늘 음식으로 저장 버튼
        val tvResult = findViewById<TextView>(R.id.tvResult) // 추천 결과 텍스트
        val foodImage = findViewById<ImageView>(R.id.foodImage) // 추천 음식 이미지

        // 추천 버튼 클릭 시
        btnRecommend.setOnClickListener {
            val allFoods = dbHelper.getAllFoods().filter { it.date.isEmpty() }

            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val excluded = prefs.getStringSet("excludedFoods", emptySet()) ?: emptySet()
            val candidates = allFoods.filterNot { excluded.contains(it.name) } // 제외된 음식 제외

            val recommendedFood = candidates.randomOrNull() // 무작위 추천

            if (recommendedFood == null) {
                tvResult.text = "추천할 음식이 없습니다."
                foodImage.visibility = View.GONE
                btnAddToday.visibility = View.GONE
                return@setOnClickListener
            }

            // 추천된 음식 정보 저장
            currentRecommendation = recommendedFood.name
            currentImagePath = recommendedFood.imageUri
            tvResult.text = "오늘은 \"${recommendedFood.name}\" 어떠세요?"

            // 이미지 설정
            try {
                if (recommendedFood.imageUri.startsWith("default:")) {
                    val resName = recommendedFood.imageUri.substringAfter("default:")
                    val resId = resources.getIdentifier(resName, "drawable", packageName)
                    if (resId != 0) {
                        foodImage.setImageResource(resId)
                    } else {
                        foodImage.setImageResource(R.drawable.placeholder_image)
                    }
                } else {
                    val file = File(Uri.parse(recommendedFood.imageUri).path ?: "")
                    if (file.exists()) {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        foodImage.setImageBitmap(bitmap)
                    } else {
                        foodImage.setImageResource(R.drawable.placeholder_image)
                    }
                }
            } catch (e: Exception) {
                foodImage.setImageResource(R.drawable.placeholder_image)
            }

            foodImage.visibility = View.VISIBLE
            btnAddToday.visibility = View.VISIBLE
        }

        // 오늘 음식으로 추가 버튼 클릭 시
        btnAddToday.setOnClickListener {
            val foodName = currentRecommendation
            val imageUri = currentImagePath
            if (foodName != null && imageUri != null) {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                dbHelper.insertFood(Food(0, foodName, today, imageUri)) // 오늘 날짜로 저장
                Toast.makeText(this, "\"$foodName\" 저장 완료!", Toast.LENGTH_SHORT).show()
                btnAddToday.visibility = View.GONE
            }
        }

        // 네비게이션 버튼
        findViewById<ImageButton>(R.id.nav_home).setOnClickListener {
        }
        findViewById<ImageButton>(R.id.nav_record).setOnClickListener {
            startActivity(Intent(this, RecordActivity::class.java))
            finish()
        }
        findViewById<ImageButton>(R.id.nav_manage).setOnClickListener {
            startActivity(Intent(this, FoodManageActivity::class.java))
            finish()
        }
    }

    // 정확한 식사 알림 설정
    fun setMealReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 19)      // 오후 7시
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)

            // 현재 시간이 이미 오후 7시 이후면 다음 날로 설정
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // 절전 모드에서도 작동 가능한 정확한 알람 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        Log.d("ReminderTest", "알람 설정됨: ${calendar.time}")
    }

}
