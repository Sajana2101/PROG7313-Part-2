package com.example.budgetquest

import Data.Database.AppDatabase
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SpendingTrends : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var edtTrendStartDate: EditText
    private lateinit var edtTrendEndDate: EditText
    private lateinit var btnGenerateTrendGraph: Button
    private lateinit var barChartSpendingTrends: BarChart
    private lateinit var tvTrendPeriod: TextView
    private lateinit var tvTrendTotal: TextView
    private lateinit var tvNoTrendData: TextView

    private var userId: Int = -1

    private val databaseDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
    private val displayDateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.UK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spending_trends)

        userId = intent.getIntExtra("userId", -1)

        if (userId == -1) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        db = AppDatabase.getDatabase(this)

        edtTrendStartDate = findViewById(R.id.edtTrendStartDate)
        edtTrendEndDate = findViewById(R.id.edtTrendEndDate)
        btnGenerateTrendGraph = findViewById(R.id.btnGenerateTrendGraph)
        barChartSpendingTrends = findViewById(R.id.barChartSpendingTrends)
        tvTrendPeriod = findViewById(R.id.tvTrendPeriod)
        tvTrendTotal = findViewById(R.id.tvTrendTotal)
        tvNoTrendData = findViewById(R.id.tvNoTrendData)

        findViewById<TextView>(R.id.btnBackTrends).setOnClickListener {
            finish()
        }

        edtTrendStartDate.setOnClickListener {
            showDatePicker(edtTrendStartDate)
        }

        edtTrendEndDate.setOnClickListener {
            showDatePicker(edtTrendEndDate)
        }

        btnGenerateTrendGraph.setOnClickListener {
            validateAndLoadGraph()
        }

        setDefaultCurrentMonth()
        validateAndLoadGraph()
        NavigationHelper.setupBottomNavigation(
            activity = this,
            userId = userId,
            currentPage = "Profile"
        )
    }

    private fun setDefaultCurrentMonth() {
        val todayCalendar = Calendar.getInstance()

        val startCalendar = Calendar.getInstance()
        startCalendar.set(Calendar.DAY_OF_MONTH, 1)

        edtTrendStartDate.setText(databaseDateFormatter.format(startCalendar.time))
        edtTrendEndDate.setText(databaseDateFormatter.format(todayCalendar.time))
    }

    private fun showDatePicker(targetField: EditText) {
        val calendar = Calendar.getInstance()
        val existingText = targetField.text.toString().trim()

        if (existingText.isNotEmpty()) {
            try {
                databaseDateFormatter.isLenient = false
                val selectedDate = databaseDateFormatter.parse(existingText)

                if (selectedDate != null) {
                    calendar.time = selectedDate
                }
            } catch (exception: Exception) {
                // Calendar remains on today's date.
            }
        }

        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, day)

                targetField.setText(
                    databaseDateFormatter.format(selectedCalendar.time)
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        dialog.show()
    }

    private fun validateAndLoadGraph() {
        val startDateText = edtTrendStartDate.text.toString().trim()
        val endDateText = edtTrendEndDate.text.toString().trim()

        val startDate = parseDate(startDateText)
        val endDate = parseDate(endDateText)

        if (startDate == null || endDate == null) {
            Toast.makeText(this, "Please select a valid date range", Toast.LENGTH_SHORT).show()
            return
        }

        if (startDate.after(endDate)) {
            Toast.makeText(this, "Start date cannot be after end date", Toast.LENGTH_SHORT).show()
            return
        }

        loadGraphData(startDateText, endDateText, startDate, endDate)
    }

    private fun loadGraphData(
        startDateText: String,
        endDateText: String,
        startDate: Date,
        endDate: Date
    ) {
        lifecycleScope.launch {

            val categoryTotals = db.expenseDao().getTotalSpentByCategory(
                userId = userId,
                startDate = startDateText,
                endDate = endDateText
            ).filter { it.totalAmount > 0 }

            val goal = db.monthlyGoalDao().getGoalByUser(userId)

            val totalSpent = categoryTotals.sumOf { it.totalAmount }

            tvTrendPeriod.text =
                "${displayDateFormatter.format(startDate)} - ${displayDateFormatter.format(endDate)}"
            tvTrendTotal.text = "Total Spending: ${formatMoney(totalSpent)}"

            if (categoryTotals.isEmpty()) {
                tvNoTrendData.visibility = View.VISIBLE
                barChartSpendingTrends.visibility = View.GONE
                barChartSpendingTrends.clear()
                barChartSpendingTrends.invalidate()
                return@launch
            }

            tvNoTrendData.visibility = View.GONE
            barChartSpendingTrends.visibility = View.VISIBLE

            val entries = ArrayList<BarEntry>()
            val bottomLabels = ArrayList<String>()

            categoryTotals.forEachIndexed { index, categoryTotal ->
                entries.add(
                    BarEntry(
                        index.toFloat(),
                        categoryTotal.totalAmount.toFloat()
                    )
                )

                val shortenedCategory = shortenLabel(categoryTotal.category)

                bottomLabels.add(
                    "$shortenedCategory|${formatMoney(categoryTotal.totalAmount)}"
                )
            }

            setupBarChart(entries, bottomLabels, goal)
        }
    }

    private fun setupBarChart(
        entries: ArrayList<BarEntry>,
        bottomLabels: ArrayList<String>,
        goal: com.example.budgetquest.data.MonthlyGoal?

    ) {
        val dataSet = BarDataSet(entries, "Amount Spent")
        dataSet.valueTextSize = 11f
        dataSet.valueTextColor = Color.parseColor("#263238")
        dataSet.color = Color.parseColor("#6D50B6")

        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getBarLabel(barEntry: BarEntry?): String {
                return formatMoney((barEntry?.y ?: 0f).toDouble())
            }
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.55f

        barChartSpendingTrends.data = barData
        barChartSpendingTrends.description.isEnabled = false
        barChartSpendingTrends.legend.isEnabled = false
        barChartSpendingTrends.setFitBars(true)
        barChartSpendingTrends.setScaleEnabled(false)
        barChartSpendingTrends.setPinchZoom(false)
        barChartSpendingTrends.setDrawGridBackground(false)
        barChartSpendingTrends.setExtraBottomOffset(34f)
        barChartSpendingTrends.setExtraTopOffset(14f)

        val xAxis = barChartSpendingTrends.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        xAxis.labelCount = bottomLabels.size
        xAxis.textSize = 10f
        xAxis.textColor = Color.parseColor("#263238")
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val index = value.toInt()

                return if (index in bottomLabels.indices) {
                    bottomLabels[index]
                } else {
                    ""
                }
            }
        }

        /*
           Custom x-axis renderer:
           First line displays the category.
           Second line displays the total amount underneath it.
         */
        barChartSpendingTrends.setXAxisRenderer(
            TwoLineXAxisRenderer(
                barChartSpendingTrends.viewPortHandler,
                xAxis,
                barChartSpendingTrends.getTransformer(
                    com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT
                )
            )
        )

        //y axis and goals lines (for min and max monthly goals)
        val leftAxis = barChartSpendingTrends.axisLeft

        leftAxis.axisMinimum = 0f
        leftAxis.textColor = Color.parseColor("#546E7A")
        leftAxis.textSize = 10f

        leftAxis.removeAllLimitLines()

        goal?.let {

            val minLine = com.github.mikephil.charting.components.LimitLine(
                it.minGoal.toFloat(),
                "Min Goal"
            ).apply {
                lineColor = Color.parseColor("#4CAF50")
                lineWidth = 2f
                textColor = Color.parseColor("#4CAF50")
                textSize = 10f
            }

            val maxLine = com.github.mikephil.charting.components.LimitLine(
                it.maxGoal.toFloat(),
                "Max Goal"
            ).apply {
                lineColor = Color.parseColor("#D32F2F")
                lineWidth = 2f
                textColor = Color.parseColor("#D32F2F")
                textSize = 10f
            }

            leftAxis.addLimitLine(minLine)
            leftAxis.addLimitLine(maxLine)
        }

        barChartSpendingTrends.axisRight.isEnabled = false


        barChartSpendingTrends.animateY(700)

        //ensures that the max monthly goal line always shows
        val maxDataValue = entries.maxOfOrNull { it.y } ?: 0f
        val maxGoalValue = goal?.maxGoal?.toFloat() ?: 0f

        leftAxis.axisMaximum = maxOf(maxDataValue, maxGoalValue) * 1.1f

        barChartSpendingTrends.invalidate()

    }

    private fun parseDate(dateText: String): Date? {
        return try {
            databaseDateFormatter.isLenient = false
            databaseDateFormatter.parse(dateText)
        } catch (exception: Exception) {
            null
        }
    }

    private fun shortenLabel(category: String): String {
        return if (category.length <= 12) {
            category
        } else {
            category.take(10) + "…"
        }
    }

    private fun formatMoney(amount: Double): String {
        return String.format(Locale.US, "R%.2f", amount)
    }

    private fun formatShortMoney(amount: Double): String {
        return when {
            amount >= 1000 -> String.format(Locale.US, "R%.1fk", amount / 1000)
            else -> String.format(Locale.US, "R%.0f", amount)
        }
    }

    class TwoLineXAxisRenderer(
        viewPortHandler: ViewPortHandler,
        xAxis: XAxis,
        transformer: Transformer
    ) : XAxisRenderer(viewPortHandler, xAxis, transformer) {

        override fun drawLabel(
            canvas: Canvas,
            formattedLabel: String,
            x: Float,
            y: Float,
            anchor: MPPointF,
            angleDegrees: Float
        ) {
            val lines = formattedLabel.split("|")

            val categoryLine = lines.getOrElse(0) { "" }
            val amountLine = lines.getOrElse(1) { "" }

            Utils.drawXAxisValue(
                canvas,
                categoryLine,
                x,
                y,
                mAxisLabelPaint,
                anchor,
                angleDegrees
            )

            Utils.drawXAxisValue(
                canvas,
                amountLine,
                x,
                y + Utils.convertDpToPixel(14f),
                mAxisLabelPaint,
                anchor,
                angleDegrees
            )
        }
    }
}