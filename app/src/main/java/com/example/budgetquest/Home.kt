package com.example.budgetquest

import Data.Database.AppDatabase
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Home : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var categoryContainer: LinearLayout
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvTotalLimit: TextView
    private lateinit var pieChartHome: PieChart

    // visual budget indicator views
    private lateinit var budgetPerformanceCard: LinearLayout
    private lateinit var tvBudgetStatus: TextView
    private lateinit var tvBudgetRange: TextView
    private lateinit var tvBudgetPercentage: TextView
    private lateinit var pbMonthlyProgress: ProgressBar
    private lateinit var tvBudgetAdvice: TextView
    private lateinit var tvBudgetPeriod: TextView
    private lateinit var btnGenerateBudgetReport: TextView

    private var userId: Int = -1
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        userId = intent.getIntExtra("userId", -1)

        if (userId == -1) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // gets db instance
        db = AppDatabase.getDatabase(this)

        categoryContainer = findViewById(R.id.categoryContainer)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        tvTotalLimit = findViewById(R.id.tvTotalLimit)
        pieChartHome = findViewById(R.id.pieChartHome)

        // connects the visual budget indicator card from activity_home.xml
        budgetPerformanceCard = findViewById(R.id.budgetPerformanceCard)
        tvBudgetStatus = findViewById(R.id.tvBudgetStatus)
        tvBudgetRange = findViewById(R.id.tvBudgetRange)
        tvBudgetPercentage = findViewById(R.id.tvBudgetPercentage)
        pbMonthlyProgress = findViewById(R.id.pbMonthlyProgress)
        tvBudgetAdvice = findViewById(R.id.tvBudgetAdvice)
        tvBudgetPeriod = findViewById(R.id.tvBudgetPeriod)
        btnGenerateBudgetReport = findViewById(R.id.btnGenerateBudgetReport)

        // opens monthly goals screen when clicked
        tvTotalLimit.setOnClickListener {
            val intent = Intent(this, MonthlyGoals::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        // adding the click action for the generate button for the module performance section
        btnGenerateBudgetReport.setOnClickListener {
            val intent = Intent(this, Report::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        setupBottomNav()
        loadDashboard()
    }

    private fun loadDashboard() {
        lifecycleScope.launch {

            // gets categories and expenses from db for the logged in user
            val categories = db.categoryDao().getCategoriesByUser(userId)
            val expenses = db.expenseDao().getExpensesByUser(userId)

            val pastMonthExpenses = expenses.filter { isExpenseInPastMonth(it.date) }
            val totalExpenses = pastMonthExpenses.sumOf { it.amount }
            val monthlyGoal = db.monthlyGoalDao().getGoalByUser(userId)
            val minGoal = monthlyGoal?.minGoal ?: 0.0
            val totalLimit = monthlyGoal?.maxGoal ?: 0.0

            runOnUiThread {
                tvTotalExpenses.text = "Total: ${formatMoney(totalExpenses)}"
                tvTotalLimit.text = "Monthly Limit: ${formatMoney(totalLimit)}  (tap to edit)"

                // updates the new visual budget performance card
                updateBudgetPerformanceCard(
                    totalSpent = totalExpenses,
                    minGoal = minGoal,
                    maxGoal = totalLimit
                )

                categoryContainer.removeAllViews()

                val pieEntries = ArrayList<PieEntry>()

                if (categories.isEmpty()) {
                    val emptyText = TextView(this@Home)
                    emptyText.text = "No categories added yet."
                    emptyText.textSize = 16f
                    categoryContainer.addView(emptyText)

                    // clears chart if no data exists
                    pieChartHome.clear()
                    pieChartHome.centerText = "No data yet"
                    pieChartHome.invalidate()
                } else {
                    categories.forEach { category ->

                        // calculates total spent for each category
                        val categoryTotal = expenses
                            .filter { it.category.equals(category.name, ignoreCase = true) }
                            .sumOf { it.amount }

                        if (categoryTotal > 0) {
                            pieEntries.add(
                                PieEntry(
                                    categoryTotal.toFloat(),
                                    category.name
                                )
                            )
                        }

                        // creates category spending card
                        addCategoryCard(
                            categoryName = category.name,
                            spent = categoryTotal,
                            limit = category.monthlyLimit
                        )
                    }

                    setupPieChart(pieEntries)
                }
            }
        }
    }

    private fun updateBudgetPerformanceCard(totalSpent: Double, minGoal: Double, maxGoal: Double) {
        tvBudgetPeriod.text = getBudgetPeriodText()

        val progressPercentage = if (maxGoal > 0) {
            ((totalSpent / maxGoal) * 100).toInt()
        } else {
            0
        }

        val safeProgress = progressPercentage.coerceIn(0, 100)

        val statusText: String
        val adviceText: String
        val statusColor: Int

        when {
            maxGoal <= 0 -> {
                statusText = "Status: No monthly goals set"
                adviceText = "Set your minimum and maximum monthly goals to view your progress."
                statusColor = Color.parseColor("#546E7A")
            }

            totalSpent < minGoal -> {
                statusText = "Status: Below minimum goal"
                adviceText = "Your spending is currently below your minimum monthly goal."
                statusColor = Color.parseColor("#1E88E5")
            }

            totalSpent > maxGoal -> {
                val overspentAmount = totalSpent - maxGoal
                statusText = "Status: Over maximum goal"
                adviceText = "You have exceeded your maximum monthly goal by ${formatMoney(overspentAmount)}."
                statusColor = Color.parseColor("#E53935")
            }

            progressPercentage >= 90 -> {
                statusText = "Status: Nearing maximum goal"
                adviceText = "Be Careful! You are nearing your maximum monthly spending goal."
                statusColor = Color.parseColor("#FB8C00")
            }

            else -> {
                statusText = "Status: Within budget range"
                adviceText = "You are within your minimum and maximum monthly spending goals."
                statusColor = Color.parseColor("#43A047")
            }
        }

        tvBudgetStatus.text = statusText
        tvBudgetStatus.setTextColor(statusColor)

        tvBudgetRange.text = "Minimum: ${formatMoney(minGoal)} | Maximum: ${formatMoney(maxGoal)}"
        tvBudgetPercentage.text = "$progressPercentage% of maximum goal used"
        tvBudgetAdvice.text = adviceText
        tvBudgetAdvice.setTextColor(statusColor)

        pbMonthlyProgress.progress = safeProgress
        pbMonthlyProgress.progressTintList = ColorStateList.valueOf(statusColor)
    }

    private fun setupPieChart(entries: ArrayList<PieEntry>) {

        // handles case where no expenses exist
        if (entries.isEmpty()) {
            pieChartHome.clear()
            pieChartHome.centerText = "No expenses yet"
            pieChartHome.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "Category Spending")
        dataSet.valueTextSize = 12f

        // sets chart colours
        dataSet.colors = listOf(
            Color.rgb(76, 175, 80),
            Color.rgb(33, 150, 243),
            Color.rgb(255, 152, 0),
            Color.rgb(233, 30, 99),
            Color.rgb(156, 39, 176),
            Color.rgb(0, 188, 212)
        )

        val pieData = PieData(dataSet)

        pieChartHome.data = pieData
        pieChartHome.description.isEnabled = false
        pieChartHome.centerText = "Spending"
        pieChartHome.setEntryLabelTextSize(11f)
        pieChartHome.animateY(800)
        pieChartHome.invalidate()
    }

    private fun addCategoryCard(categoryName: String, spent: Double, limit: Double) {

        // this will create the category card layout
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(18, 18, 18, 18)
        card.setBackgroundResource(R.drawable.login_card_bg)

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardParams.setMargins(0, 0, 0, 18)
        card.layoutParams = cardParams

        val percentage = if (limit > 0) {
            ((spent / limit) * 100).toInt()
        } else {
            0
        }

        val safeProgress = percentage.coerceIn(0, 100)

        val statusColor = when {
            limit <= 0 -> Color.parseColor("#546E7A")
            spent > limit -> Color.parseColor("#E53935")
            spent == limit -> Color.parseColor("#FB8C00")
            percentage >= 90 -> Color.parseColor("#FB8C00")
            else -> Color.parseColor("#43A047")
        }

        val statusText = when {
            limit <= 0 -> "No category limit set"
            spent > limit -> "Overspent by ${formatMoney(spent - limit)}"
            spent == limit -> "At category limit"
            percentage >= 90 -> "Nearing category limit"
            else -> "Within category limit"
        }

        val title = TextView(this)
        title.text = if (spent > limit && limit > 0) {
            "⚠ $categoryName"
        } else {
            categoryName
        }
        title.textSize = 18f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(statusColor)

        val amount = TextView(this)
        amount.text = "${formatMoney(spent)} / ${formatMoney(limit)}"
        amount.textSize = 14f

        val status = TextView(this)
        status.text = "$percentage% used - $statusText"
        status.textSize = 13f
        status.setTextColor(statusColor)
        status.setTypeface(null, Typeface.BOLD)

        //it shows spending progress based on category limit
        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progressBar.max = 100
        progressBar.progress = safeProgress
        progressBar.progressTintList = ColorStateList.valueOf(statusColor)

        card.addView(title)
        card.addView(amount)
        card.addView(status)
        card.addView(progressBar)

        // opens expense list for selected category
        card.setOnClickListener {
            val intent = Intent(this, ExpenseList::class.java)
            intent.putExtra("categoryName", categoryName)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        categoryContainer.addView(card)
    }

    private fun isExpenseInPastMonth(date: String): Boolean {
        return try {
            val expenseDate: Date = dateFormatter.parse(date) ?: return false

            val todayCalendar = Calendar.getInstance()
            val today = todayCalendar.time

            val oneMonthAgoCalendar = Calendar.getInstance()
            oneMonthAgoCalendar.add(Calendar.MONTH, -1)
            val oneMonthAgo = oneMonthAgoCalendar.time

            !expenseDate.before(oneMonthAgo) && !expenseDate.after(today)
        } catch (exception: Exception) {
            false
        }
    }

    private fun getBudgetPeriodText(): String {
        val displayFormatter = java.text.SimpleDateFormat("dd MMMM yyyy", Locale.UK)

        val todayCalendar = java.util.Calendar.getInstance()
        val today = todayCalendar.time

        val oneMonthAgoCalendar = java.util.Calendar.getInstance()
        oneMonthAgoCalendar.add(java.util.Calendar.MONTH, -1)
        val oneMonthAgo = oneMonthAgoCalendar.time

        return "Past month: ${displayFormatter.format(oneMonthAgo)} - ${displayFormatter.format(today)}"
    }

    private fun formatMoney(amount: Double): String {
        return String.format(Locale.US, "R%.2f", amount)
    }

    private fun setupBottomNav() {

        // handles bottom nav clicks
        findViewById<TextView>(R.id.navHome).setOnClickListener {
            Toast.makeText(this, "You are already on Home", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.navCategories).setOnClickListener {
            val intent = Intent(this, Categories::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.navAddExpense).setOnClickListener {
            val intent = Intent(this, Expenses::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.navGoals).setOnClickListener {
            val intent = Intent(this, MonthlyGoals::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.navSavingsDebt).setOnClickListener {
            val intent = Intent(this, SavingsDebt::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }
    }
}