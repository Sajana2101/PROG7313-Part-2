package com.example.budgetquest

import Data.Database.AppDatabase
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ExpenseList : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var tvExpenseListTitle: TextView
    private lateinit var tvExpenseList: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

        db = AppDatabase.getDatabase(this)

        tvExpenseListTitle = findViewById(R.id.tvExpenseListTitle)
        tvExpenseList = findViewById(R.id.tvExpenseList)

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
}