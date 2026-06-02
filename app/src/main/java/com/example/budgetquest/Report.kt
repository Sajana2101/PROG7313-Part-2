package com.example.budgetquest

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetquest.firebase.FirebaseRepository
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@SuppressLint("SetTextI18n")
class Report : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository

    private lateinit var tvReportPeriod: TextView
    private lateinit var tvReportMinimum: TextView
    private lateinit var tvReportSpent: TextView
    private lateinit var tvReportMaximum: TextView
    private lateinit var tvReportStatus: TextView
    private lateinit var tvReportAdvice: TextView
    private lateinit var tvReportExplanation: TextView
    private lateinit var btnBackHome: TextView
    private lateinit var budgetReportBarChart: BarChart

    private var userUid: String = ""

    private val dateFormatter =
        SimpleDateFormat("yyyy-MM-dd", Locale.UK)

    private val displayFormatter =
        SimpleDateFormat("dd MMMM yyyy", Locale.UK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

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

        tvReportPeriod = findViewById(R.id.tvReportPeriod)
        tvReportMinimum = findViewById(R.id.tvReportMinimum)
        tvReportSpent = findViewById(R.id.tvReportSpent)
        tvReportMaximum = findViewById(R.id.tvReportMaximum)
        tvReportStatus = findViewById(R.id.tvReportStatus)
        tvReportAdvice = findViewById(R.id.tvReportAdvice)
        tvReportExplanation = findViewById(R.id.tvReportExplanation)
        btnBackHome = findViewById(R.id.btnBackHome)
        budgetReportBarChart = findViewById(R.id.budgetReportBarChart)

        btnBackHome.setOnClickListener {
            openHomePage()
        }

        loadBudgetReport()
    }

    private fun loadBudgetReport() {
        // Combines expenses from the past month with the user's saved monthly goal values.
        repository.getExpenses(
            uid = userUid,
            onSuccess = { expenses ->
                val lastMonthExpenses = expenses.filter { expense ->
                    isExpenseInLastMonth(expense.date)
                }

                val totalSpent = lastMonthExpenses.sumOf { expense ->
                    expense.amount
                }

                repository.getMonthlyGoal(
                    uid = userUid,
                    onSuccess = { monthlyGoal ->
                        val minGoal = monthlyGoal?.minGoal ?: 0.0
                        val maxGoal = monthlyGoal?.maxGoal ?: 0.0

                        updateReportText(
                            totalSpent = totalSpent,
                            minGoal = minGoal,
                            maxGoal = maxGoal
                        )

                        setupBarChart(
                            totalSpent = totalSpent,
                            minGoal = minGoal,
                            maxGoal = maxGoal
                        )
                    },
                    onError = { errorMessage ->
                        Toast.makeText(
                            this,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()

                        updateReportText(
                            totalSpent = totalSpent,
                            minGoal = 0.0,
                            maxGoal = 0.0
                        )

                        setupBarChart(
                            totalSpent = totalSpent,
                            minGoal = 0.0,
                            maxGoal = 0.0
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

    private fun updateReportText(
        totalSpent: Double,
        minGoal: Double,
        maxGoal: Double
    ) {
        val startDate = getStartDate()
        val endDate = getEndDate()

        tvReportPeriod.text =
            "Past month: ${displayFormatter.format(startDate)} - ${
                displayFormatter.format(endDate)
            }"

        tvReportMinimum.text =
            "Minimum goal: ${formatMoney(minGoal)}"

        tvReportSpent.text =
            "Actual spending: ${formatMoney(totalSpent)}"

        tvReportMaximum.text =
            "Maximum goal: ${formatMoney(maxGoal)}"

        val statusColor: Int
        val statusText: String
        val adviceText: String

        val percentage = if (maxGoal > 0) {
            ((totalSpent / maxGoal) * 100).toInt()
        } else {
            0
        }

        when {
            maxGoal <= 0 -> {
                statusText = "Status: No monthly goals set"
                adviceText =
                    "Set your minimum and maximum monthly goals to view this report."
                statusColor = Color.parseColor("#546E7A")
            }

            totalSpent < minGoal -> {
                statusText = "Status: Below minimum goal"
                adviceText =
                    "Your spending is below your minimum monthly goal for this period."
                statusColor = Color.parseColor("#1E88E5")
            }

            totalSpent > maxGoal -> {
                val overspentAmount = totalSpent - maxGoal

                statusText = "Status: Over maximum goal"
                adviceText =
                    "You exceeded your maximum monthly goal by ${
                        formatMoney(overspentAmount)
                    }."

                statusColor = Color.parseColor("#E53935")
            }

            percentage >= 90 -> {
                statusText = "Status: Nearing maximum goal"
                adviceText =
                    "You are nearing your maximum monthly spending goal."
                statusColor = Color.parseColor("#FB8C00")
            }

            else -> {
                statusText = "Status: Within budget range"
                adviceText =
                    "Your spending is within your minimum and maximum monthly goals."
                statusColor = Color.parseColor("#43A047")
            }
        }

        tvReportStatus.text = statusText
        tvReportStatus.setTextColor(statusColor)

        tvReportAdvice.text = adviceText
        tvReportAdvice.setTextColor(statusColor)

        tvReportExplanation.text =
            "The graph compares your minimum goal, actual spending, and maximum goal for the past month."
    }

    private fun setupBarChart(
        totalSpent: Double,
        minGoal: Double,
        maxGoal: Double
    ) {
        val entries = listOf(
            BarEntry(0f, minGoal.toFloat()),
            BarEntry(1f, totalSpent.toFloat()),
            BarEntry(2f, maxGoal.toFloat())
        )

        val dataSet = BarDataSet(entries, "Budget Performance")

        dataSet.colors = listOf(
            Color.parseColor("#FF9800"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#F44336")
        )

        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK

        val barData = BarData(dataSet)
        barData.barWidth = 0.5f

        budgetReportBarChart.data = barData

        val labels = listOf(
            "Min Goal",
            "Spent",
            "Max Goal"
        )

        budgetReportBarChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            labelCount = labels.size
            setDrawGridLines(false)
        }

        budgetReportBarChart.axisRight.isEnabled = false

        budgetReportBarChart.axisLeft.apply {
            axisMinimum = 0f

            val highestValue = maxOf(
                totalSpent.toFloat(),
                minGoal.toFloat(),
                maxGoal.toFloat()
            )

            axisMaximum = if (highestValue > 0f) {
                highestValue * 1.1f
            } else {
                100f
            }
        }

        budgetReportBarChart.description.isEnabled = false
        budgetReportBarChart.legend.isEnabled = false
        budgetReportBarChart.setFitBars(true)

        budgetReportBarChart.animateY(800)
        budgetReportBarChart.invalidate()
    }

    private fun isExpenseInLastMonth(date: String): Boolean {
        return try {
            dateFormatter.isLenient = false

            val expenseDate: Date =
                dateFormatter.parse(date) ?: return false

            !expenseDate.before(getStartDate()) &&
                    !expenseDate.after(getEndDate())
        } catch (_: Exception) {
            false
        }
    }

    private fun getStartDate(): Date {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)

        return calendar.time
    }

    private fun getEndDate(): Date {
        return Calendar.getInstance().time
    }

    private fun openHomePage() {
        val intent = Intent(this, Home::class.java)
        intent.putExtra("userUid", userUid)
        startActivity(intent)
        finish()
    }

    private fun openLoginPage() {
        repository.logout()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    private fun formatMoney(amount: Double): String {
        return String.format(
            Locale.US,
            "R%.2f",
            amount
        )
    }
}
