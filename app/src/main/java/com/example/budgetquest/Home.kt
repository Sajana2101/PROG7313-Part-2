package com.example.budgetquest

import Data.Database.AppDatabase
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.launch

class Home : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var categoryContainer: LinearLayout
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvTotalLimit: TextView
    private lateinit var pieChartHome: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        db = AppDatabase.getDatabase(this)

        categoryContainer = findViewById(R.id.categoryContainer)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        tvTotalLimit = findViewById(R.id.tvTotalLimit)
        pieChartHome = findViewById(R.id.pieChartHome)

        tvTotalLimit.setOnClickListener {
            startActivity(Intent(this, MonthlyGoals::class.java))
        }

        setupBottomNav()
        loadDashboard()
    }

    private fun loadDashboard() {
        lifecycleScope.launch {
            val categories = db.categoryDao().getAllCategories()
            val expenses = db.expenseDao().getAllExpenses()

            val totalExpenses = expenses.sumOf { it.amount }
            val monthlyGoal = db.monthlyGoalDao().getGoal()
            val totalLimit = monthlyGoal?.maxGoal ?: 0.0

            runOnUiThread {
                tvTotalExpenses.text = "Total: R$totalExpenses"
                tvTotalLimit.text = "Monthly Limit: R$totalLimit  (tap to edit)"

                categoryContainer.removeAllViews()

                val pieEntries = ArrayList<PieEntry>()

                if (categories.isEmpty()) {
                    val emptyText = TextView(this@Home)
                    emptyText.text = "No categories added yet."
                    emptyText.textSize = 16f
                    categoryContainer.addView(emptyText)

                    pieChartHome.clear()
                    pieChartHome.centerText = "No data yet"
                    pieChartHome.invalidate()
                } else {
                    categories.forEach { category ->
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

                            addCategoryCard(
                                categoryName = category.name,
                                spent = categoryTotal,
                                limit = category.monthlyLimit
                            )
                        }
                    }

                    setupPieChart(pieEntries)
                }
            }
        }
    }

    private fun setupPieChart(entries: ArrayList<PieEntry>) {
        if (entries.isEmpty()) {
            pieChartHome.clear()
            pieChartHome.centerText = "No expenses yet"
            pieChartHome.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "Category Spending")
        dataSet.valueTextSize = 12f
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

        val title = TextView(this)
        title.text = categoryName
        title.textSize = 18f
        title.setTypeface(null, Typeface.BOLD)

        val amount = TextView(this)
        amount.text = "R$spent / R$limit"
        amount.textSize = 14f

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progressBar.max = 100
        progressBar.progress = if (limit > 0) {
            ((spent / limit) * 100).toInt().coerceAtMost(100)
        } else {
            0
        }

        card.addView(title)
        card.addView(amount)
        card.addView(progressBar)

        card.setOnClickListener {
            val intent = Intent(this, ExpenseList::class.java)
            intent.putExtra("categoryName", categoryName)
            startActivity(intent)
        }

        categoryContainer.addView(card)
    }

    private fun setupBottomNav() {
        findViewById<TextView>(R.id.navHome).setOnClickListener {
            Toast.makeText(this, "You are already on Home", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.navCategories).setOnClickListener {
            startActivity(Intent(this, Categories::class.java))
        }

        findViewById<TextView>(R.id.navAddExpense).setOnClickListener {
            startActivity(Intent(this, Expenses::class.java))
        }

        findViewById<TextView>(R.id.navGoals).setOnClickListener {
            startActivity(Intent(this, MonthlyGoals::class.java))
        }

        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            Toast.makeText(this, "Profile screen will be added soon", Toast.LENGTH_SHORT).show()
        }
    }
}