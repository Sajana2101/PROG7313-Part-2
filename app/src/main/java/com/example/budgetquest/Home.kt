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

    private var userId: Int = -1

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

        // opens monthly goals screen when clicked
        tvTotalLimit.setOnClickListener {
            val intent = Intent(this, MonthlyGoals::class.java)
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

            val totalExpenses = expenses.sumOf { it.amount }
            val monthlyGoal = db.monthlyGoalDao().getGoalByUser(userId)
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

        // creates category card layout
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

        // shows spending progress based on category limit
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

        // opens expense list for selected category
        card.setOnClickListener {
            val intent = Intent(this, ExpenseList::class.java)
            intent.putExtra("categoryName", categoryName)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        categoryContainer.addView(card)
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
    }
}