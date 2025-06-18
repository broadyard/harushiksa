package com.example.harushiksa.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// 음식 정보를 담는 데이터 클래스
data class Food(val id: Int, val name: String, val date: String, val imageUri: String)

// SQLiteOpenHelper: 음식 데이터를 관리하는 데이터베이스 헬퍼 클래스
class FoodDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "FoodDB", null, 1) {

    // 앱 설치 시 기본으로 저장될 음식 목록
    private val defaultFoods = listOf(
        Food(0, "김치찌개", "", "default:kimchi"),
        Food(0, "된장찌개", "", "default:soybean"),
        Food(0, "라면", "", "default:ramen"),
        Food(0, "치킨", "", "default:chicken"),
        Food(0, "돈까스", "", "default:cutlet"),
        Food(0, "비빔밥", "", "default:bibimbap"),
        Food(0, "삼겹살", "", "default:pork"),
        Food(0, "김밥", "", "default:gimbap"),
        Food(0, "김치볶음밥", "", "default:kimchi_fried_rice"),
        Food(0, "닭도리탕", "", "default:braised_chicken"),
        Food(0, "초밥", "", "default:sushi"),
        Food(0, "피자", "", "default:pizza"),
        Food(0, "햄버거", "", "default:burger"),
        Food(0, "갈비탕", "", "default:galbitang"),
        Food(0, "닭강정", "", "default:chicken_gangjeong"),
        Food(0, "냉면", "", "default:naengmyeon"),
        Food(0, "설렁탕", "", "default:seolleongtang"),
        Food(0, "순두부찌개", "", "default:soft_tofu"),
        Food(0, "육회", "", "default:yukhoe"),
        Food(0, "감자탕", "", "default:gamjatang"),
        Food(0, "제육볶음", "", "default:jeyuk"),
        Food(0, "육개장", "", "default:yukgaejang"),
        Food(0, "국밥", "", "default:gukbap"),
        Food(0, "부대찌개", "", "default:budae"),
        Food(0, "짜장면", "", "default:jjajangmyeon"),
        Food(0, "회", "", "default:rawfish"),
        Food(0, "짬뽕", "", "default:jjamppong"),
        Food(0, "족발", "", "default:jokbal"),
        Food(0, "볶음밥", "", "default:fried_rice"),
        Food(0, "마라탕", "", "default:malatang"),
        Food(0, "보쌈", "", "default:bossam"),
        Food(0, "떡볶이", "", "default:tteokbokki")
    )

    // DB 최초 생성 시 호출됨
    override fun onCreate(db: SQLiteDatabase) {
        // Food 테이블 생성
        val createTable = """
            CREATE TABLE Food (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                date TEXT NOT NULL,
                imageUri TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)

        // 기본 음식 목록 삽입
        for (food in defaultFoods) {
            val values = ContentValues().apply {
                put("name", food.name)
                put("date", "")
                put("imageUri", food.imageUri)
            }
            db.insert("Food", null, values)
        }
    }

    // DB 버전 업그레이드 시 호출됨
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS Food")
        onCreate(db)
    }

    // 새로운 음식 추가
    fun insertFood(food: Food) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("name", food.name)
            put("date", food.date)
            put("imageUri", food.imageUri)
        }
        db.insert("Food", null, values)
        db.close()
    }

    // 모든 음식 데이터 조회
    fun getAllFoods(): List<Food> {
        val list = mutableListOf<Food>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM Food", null)
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
            val imageUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri"))
            list.add(Food(id, name, date, imageUri))
        }
        cursor.close()
        db.close()
        return list
    }

    // 특정 날짜의 음식 리스트 조회
    fun getFoodsByDate(date: String): List<Food> {
        val list = mutableListOf<Food>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM Food WHERE date = ?", arrayOf(date))
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
            val foodDate = cursor.getString(cursor.getColumnIndexOrThrow("date"))
            val imageUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri"))
            list.add(Food(id, name, foodDate, imageUri))
        }
        cursor.close()
        db.close()
        return list
    }

    // 이름으로 음식 삭제
    fun deleteFoodByName(name: String) {
        val db = writableDatabase
        db.delete("Food", "name = ?", arrayOf(name))
        db.close()
    }

    // 이름과 날짜로 특정 기록 삭제
    fun deleteFoodByNameAndDate(name: String, date: String) {
        val db = writableDatabase
        db.delete("Food", "name = ? AND date = ?", arrayOf(name, date))
        db.close()
    }

    // 날짜가 있는 모든 식사 기록 날짜만 조회
    fun getAllDatesWithFood(): List<String> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT DISTINCT date FROM Food WHERE date != ''", null)
        val result = mutableListOf<String>()
        while (cursor.moveToNext()) {
            result.add(cursor.getString(cursor.getColumnIndexOrThrow("date")))
        }
        cursor.close()
        db.close()
        return result
    }

    // 가장 최근에 기록된 음식 n개 조회
    fun getRecentFoods(limit: Int): List<String> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT name FROM Food WHERE date != '' ORDER BY id DESC LIMIT ?", arrayOf(limit.toString()))
        val result = mutableListOf<String>()
        while (cursor.moveToNext()) {
            result.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()
        db.close()
        return result
    }
}
