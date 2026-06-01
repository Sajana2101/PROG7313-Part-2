package com.example.budgetquest

import android.annotation.SuppressLint
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
import com.example.budgetquest.firebase.FirebaseMonthlyGoal
import com.example.budgetquest.firebase.FirebaseRepository
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@SuppressLint("SetTextI18n")
class SpendingTrends : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository

    private lateinit var edtTrendStartDate: EditText
    private lateinit var edtTrendEndDate: EditText
    private lateinit var btnGenerateTrendGraph: Button
    private lateinit var barChartSpendingTrends: BarChart
    private lateinit var tvTrendPeriod: TextView
    private lateinit var tvTrendTotal: TextView
    private lateinit var tvNoTrendData: TextView

    private var userUid: String = ""

    private val databaseDateFormatter =
        SimpleDateFormat("yyyy-MM-dd", Locale.UK)

    private val displayDateFormatter =
        SimpleDateFormat("dd MMM yyyy", Locale.UK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spending_trends)

        repository = FirebaseRepository()

        userUid = intent.getStringExtra("userUid")
            ?: repository.getCurrentUserId().orEmpty()

        if (userUid.isBlank()) {
            Toast.makeText(
                this,
                "User not found. Please log in again.",
                Toast.LENGTH_SHORT
            ).show()

            openLoginPage()
            return
        }

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

        NavigationHelper.setupBottomNavigation(
            activity = this,
            userUid = userUid,
            currentPage = "Profile"
        )

        setDefaultCurrentMonth()
        validateAndLoadGraph()
    }

    private fun setDefaultCurrentMonth() {
        val todayCalendar = Calendar.getInstance()

        val startCalendar = Calendar.getInstance()
        startCalendar.set(Calendar.DAY_OF_MONTH, 1)

        edtTrendStartDate.setText(
            databaseDateFormatter.format(startCalendar.time)
        )

        edtTrendEndDate.setText(
            databaseDateFormatter.format(todayCalendar.time)
        )
    }

    private fun showDatePicker(targetField: EditText) {
        val calendar = Calendar.getInstance()
        val currentValue = targetField.text.toString().trim()

        if (currentValue.isNotEmpty()) {
            val selectedDate = parseDate(currentValue)

            if (selectedDate != null) {
                calendar.time = selectedDate
            }
        }

        DatePickerDialog(
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
        ).show()
    }

    private fun validateAndLoadGraph() {
        val startDateText = edtTrendStartDate.text.toString().trim()
        val endDateText = edtTrendEndDate.text.toString().trim()

        val startDate = parseDate(startDateText)
        val endDate = parseDate(endDateText)

        if (startDate == null || endDate == null) {
            Toast.makeText(
                this,
                "Please select a valid date range.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (startDate.after(endDate)) {
            Toast.makeText(
                this,
                "Start date cannot be after end date.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        loadGraphData(
            startDateText = startDateText,
            endDateText = endDateText,
            startDate = startDate,
            endDate = endDate
        )
    }

    private fun loadGraphData(
        startDateText: String,
        endDateText: String,
        startDate: Date,
        endDate: Date
    ) {
        repository.getExpensesBetweenDates(
            uid = userUid,
            startDate = startDateText,
            endDate = endDateText,
            onSuccess = { expenses ->
                val categoryTotals = expenses
                    .groupBy { expense ->
                        expense.category
                    }
                    .mapValues { entry ->
                        entry.value.sumOf { expense ->
                            expense.amount
                        }
                    }
                    .filterValues { total ->
                        total > 0
                    }
                    .toList()
                    .sortedByDescending { categoryTotal ->
                        categoryTotal.second
                    }

                repository.getMonthlyGoal(
                    uid = userUid,
                    onSuccess = { monthlyGoal ->
                        displayGraphData(
                            categoryTotals = categoryTotals,
                            monthlyGoal = monthlyGoal,
                            startDate = startDate,
                            endDate = endDate
                        )
                    },
                    onError = { errorMessage ->
                        Toast.makeText(
                            this,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()

                        displayGraphData(
                            categoryTotals = categoryTotals,
                            monthlyGoal = null,
                            startDate = startDate,
                            endDate = endDate
                        )
                    }
                )
            },
            onError = { errorMessage ->
                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun displayGraphData(
        categoryTotals: List<Pair<String, Double>>,
        monthlyGoal: FirebaseMonthlyGoal?,
        startDate: Date,
        endDate: Date
    ) {
        val totalSpent = categoryTotals.sumOf { categoryTotal ->
            categoryTotal.second
        }

        tvTrendPeriod.text =
            "${displayDateFormatter.format(startDate)} - ${displayDateFormatter.format(endDate)}"

        tvTrendTotal.text =
            "Total Spending: ${formatMoney(totalSpent)}"

        if (categoryTotals.isEmpty()) {
            tvNoTrendData.visibility = View.VISIBLE
            barChartSpendingTrends.visibility = View.GONE
            barChartSpendingTrends.clear()
            barChartSpendingTrends.invalidate()
            return
        }

        tvNoTrendData.visibility = View.GONE
        barChartSpendingTrends.visibility = View.VISIBLE

        val entries = ArrayList<BarEntry>()
        val bottomLabels = ArrayList<String>()

        categoryTotals.forEachIndexed { index, categoryTotal ->
            val categoryName = categoryTotal.first
            val totalAmount = categoryTotal.second

            entries.add(
                BarEntry(
                    index.toFloat(),
                    totalAmount.toFloat()
                )
            )

            bottomLabels.add(
                "${shortenLabel(categoryName)}|${formatMoney(totalAmount)}"
            )
        }

        setupBarChart(
            entries = entries,
            bottomLabels = bottomLabels,
            monthlyGoal = monthlyGoal
        )
    }

    private fun setupBarChart(
        entries: ArrayList<BarEntry>,
        bottomLabels: ArrayList<String>,
        monthlyGoal: FirebaseMonthlyGoal?
    ) {
        val dataSet = BarDataSet(entries, "Amount Spent")

        dataSet.valueTextSize = 11f
        dataSet.valueTextColor = Color.parseColor("#263238")
        dataSet.color = Color.parseColor("#6D50B6")

        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getBarLabel(barEntry: BarEntry?): String {
                return formatMoney(
                    (barEntry?.y ?: 0f).toDouble()
                )
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

        barChartSpendingTrends.setXAxisRenderer(
            TwoLineXAxisRenderer(
                barChartSpendingTrends.viewPortHandler,
                xAxis,
                barChartSpendingTrends.getTransformer(
                    YAxis.AxisDependency.LEFT
                )
            )
        )

        val leftAxis = barChartSpendingTrends.axisLeft

        leftAxis.axisMinimum = 0f
        leftAxis.textColor = Color.parseColor("#546E7A")
        leftAxis.textSize = 10f
        leftAxis.removeAllLimitLines()

        monthlyGoal?.let { goal ->
            if (goal.minGoal > 0) {
                val minLine = LimitLine(
                    goal.minGoal.toFloat(),
                    "Min Goal"
                ).apply {
                    lineColor = Color.parseColor("#4CAF50")
                    lineWidth = 2f
                    textColor = Color.parseColor("#4CAF50")
                    textSize = 10f
                }

                leftAxis.addLimitLine(minLine)
            }

            if (goal.maxGoal > 0) {
                val maxLine = LimitLine(
                    goal.maxGoal.toFloat(),
                    "Max Goal"
                ).apply {
                    lineColor = Color.parseColor("#D32F2F")
                    lineWidth = 2f
                    textColor = Color.parseColor("#D32F2F")
                    textSize = 10f
                }

                leftAxis.addLimitLine(maxLine)
            }
        }

        barChartSpendingTrends.axisRight.isEnabled = false

        val maximumExpenseValue =
            entries.maxOfOrNull { entry -> entry.y } ?: 0f

        val maximumGoalValue =
            monthlyGoal?.maxGoal?.toFloat() ?: 0f

        val graphMaximum =
            maxOf(maximumExpenseValue, maximumGoalValue)

        leftAxis.axisMaximum = if (graphMaximum > 0f) {
            graphMaximum * 1.1f
        } else {
            100f
        }

        barChartSpendingTrends.animateY(700)
        barChartSpendingTrends.invalidate()
    }

    private fun parseDate(dateText: String): Date? {
        return try {
            databaseDateFormatter.isLenient = false
            databaseDateFormatter.parse(dateText)
        } catch (_: Exception) {
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
        return String.format(
            Locale.US,
            "R%.2f",
            amount
        )
    }

    private fun openLoginPage() {
        repository.logout()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    class TwoLineXAxisRenderer(
        viewPortHandler: ViewPortHandler,
        xAxis: XAxis,
        transformer: Transformer
    ) : XAxisRenderer(
        viewPortHandler,
        xAxis,
        transformer
    ) {

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