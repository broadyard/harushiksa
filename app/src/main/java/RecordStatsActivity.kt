package com.example.harushiksa

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.harushiksa.db.FoodDatabaseHelper
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.time.DayOfWeek
import java.time.LocalDate

class RecordStatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_stats)

        val btnClose = findViewById<ImageButton>(R.id.btnClose)
        val spinner = findViewById<Spinner>(R.id.spinnerFilter)
        val chart = findViewById<BarChart>(R.id.barChart)
        val textStats = findViewById<TextView>(R.id.textStats)

        btnClose.setOnClickListener { finish() }

        val options = arrayOf("전체", "월별", "주간")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateChartAndStats(options[position], chart, textStats)
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    // 선택된 필터 기준으로 차트와 통계 텍스트 업데이트
    private fun updateChartAndStats(filter: String, barChart: BarChart, textStats: TextView) {
        val dbHelper = FoodDatabaseHelper(this)
        val allFoods = dbHelper.getAllFoods().filter { it.date.isNotEmpty() }
        val today = LocalDate.now()

        // 필터에 따라 기간별 데이터 필터링
        val filteredFoods = when (filter) {
            "월별" -> allFoods.filter { LocalDate.parse(it.date) >= today.withDayOfMonth(1) }
            "주간" -> allFoods.filter { LocalDate.parse(it.date) >= today.with(DayOfWeek.MONDAY) }
            else -> allFoods
        }

        // 음식 이름별 횟수 정렬
        val rankMap = filteredFoods.groupingBy { it.name }.eachCount()
            .toList().sortedByDescending { it.second }

        // 상위 5개 음식 랭킹 텍스트 생성
        val rankText = buildString {
            rankMap.take(5).forEachIndexed { index, (name, count) ->
                append("${index + 1}위: $name ($count)\n")
            }
        }

        // 통계 수치 계산
        val foodsThisWeek = allFoods.count { LocalDate.parse(it.date) >= today.with(DayOfWeek.MONDAY) }
        val foodsThisMonth = allFoods.count { LocalDate.parse(it.date) >= today.withDayOfMonth(1) }
        val total = allFoods.size

        // 통계 텍스트 표시
        val statsText = buildString {
            appendLine("$filter 음식 기록 TOP 5")
            appendLine()
            rankMap.take(5).forEachIndexed { index, (name, count) ->
                appendLine("${index + 1}위: $name ($count)")
            }
            appendLine()
            appendLine("이번 주 총 먹은 수: $foodsThisWeek")
            appendLine("이번 달 총 먹은 수: $foodsThisMonth")
            appendLine("전체 기록 수: $total")
        }
        textStats.text = statsText


        // 차트 데이터 구성
        val entries = rankMap.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.second.toFloat())
        }
        val labels = rankMap.map { it.first }

        val colorList = listOf(
            "#FF8A65", "#4DB6AC", "#9575CD", "#F06292", "#64B5F6",
            "#81C784", "#BA68C8", "#FFB74D", "#4DD0E1", "#AED581"
        ).map { Color.parseColor(it) }

        val dataSet = BarDataSet(entries, "$filter 음식 기록").apply {
            colors = colorList
            valueTextSize = 12f
        }

        // 차트 설정
        barChart.apply {
            data = BarData(dataSet)

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textSize = 12f
                labelRotationAngle = -30f
            }

            axisLeft.textSize = 12f
            axisRight.isEnabled = false
            description.isEnabled = false
            legend.isEnabled = false
            setVisibleXRangeMaximum(6f)
            isDragEnabled = true
            setScaleEnabled(false)
            animateY(1000)
            invalidate()
        }
    }
}
