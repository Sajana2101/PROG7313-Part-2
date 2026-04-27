package com.example.budgetquest

import Data.Database.AppDatabase
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class Home : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var categoryContainer: LinearLayout
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvTotalLimit: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        db = AppDatabase.getDatabase(this)

        categoryContainer = findViewById(R.id.categoryContainer)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        tvTotalLimit = findViewById(R.id.tvTotalLimit)

        setupBottomNav()
        loadDashboard()
    }

    private fun loadDashboard() {
        lifecycleScope.launch {
            val categories = db.categoryDao().getAllCategories()
            val expenses = db.expenseDao().getAllExpenses()

            val totalExpenses = expenses.sumOf { it.amount }
            val totalLimit = categories.sumOf { it.monthlyLimit }

            runOnUiThread {
                tvTotalExpenses.text = "Total: R$totalExpenses"
                tvTotalLimit.text = "Monthly Limit: R$totalLimit"

                categoryContainer.removeAllViews()

                if (categories.isEmpty()) {
                    val emptyText = TextView(this@Home)
                    emptyText.text = "No categories added yet."
                    emptyText.textSize = 16f
                    categoryContainer.addView(emptyText)
                } else {
                    categories.forEach { category ->
                        val categoryTotal = expenses
                            .filter { it.category.equals(category.name, ignoreCase = true) }
                            .sumOf { it.amount }

                        addCategoryCard(
                            categoryName = category.name,
                            spent = categoryTotal,
                            limit = category.monthlyLimit
                        )
                    }
                }
            }
        }
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
        title.setTypeface(null, android.graphics.Typeface.BOLD)

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