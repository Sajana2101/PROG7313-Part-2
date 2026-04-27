package com.example.budgetquest

import Data.Database.AppDatabase
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ExpenseList : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var tvExpenseListTitle: TextView
    private lateinit var tvExpenseList: TextView
    private lateinit var btnBackHome: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

        db = AppDatabase.getDatabase(this)

        tvExpenseListTitle = findViewById(R.id.tvExpenseListTitle)
        tvExpenseList = findViewById(R.id.tvExpenseList)

        val btnBackHome = findViewById<Button>(R.id.btnBackHome)

        btnBackHome.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
            finish()
        }

        setupBottomNav()

        val categoryName = intent.getStringExtra("categoryName") ?: ""

        tvExpenseListTitle.text = "$categoryName Expenses"

        loadExpenses(categoryName)
    }

    private fun loadExpenses(categoryName: String) {
        lifecycleScope.launch {
            val expenses = db.expenseDao().getExpensesByCategory(categoryName)

            runOnUiThread {
                if (expenses.isEmpty()) {
                    tvExpenseList.text = "No expenses found for this category."
                } else {
                    tvExpenseList.text = expenses.joinToString(separator = "\n\n") {
                        "Amount: R${it.amount}\nDate: ${it.date}\nDescription: ${it.description}"
                    }
                }
            }
        }
    }

    private fun setupBottomNav() {
        findViewById<TextView>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, Home::class.java))
            finish()
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