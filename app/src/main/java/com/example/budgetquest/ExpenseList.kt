package com.example.budgetquest

import Data.Database.AppDatabase
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgetquest.data.Expense
import kotlinx.coroutines.launch

class ExpenseList : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var expenseListContainer: LinearLayout
    private lateinit var tvExpenseListTitle: TextView
    private lateinit var btnBackHome: Button

    private var categoryName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

        db = AppDatabase.getDatabase(this)

        tvExpenseListTitle = findViewById(R.id.tvExpenseListTitle)
        expenseListContainer = findViewById(R.id.expenseListContainer)
        btnBackHome = findViewById(R.id.btnBackHome)

        categoryName = intent.getStringExtra("categoryName") ?: ""

        tvExpenseListTitle.text = "$categoryName Expenses"

        btnBackHome.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
            finish()
        }

        setupBottomNav()
        loadExpenses()

    }
    override fun onResume() {
        super.onResume()

        if (categoryName.isNotEmpty()) {
            loadExpenses()
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val expenses = db.expenseDao().getExpensesByCategory(categoryName)

            runOnUiThread {
                expenseListContainer.removeAllViews()

                if (expenses.isEmpty()) {
                    val emptyText = TextView(this@ExpenseList)
                    emptyText.text = "No expenses found for this category."
                    emptyText.textSize = 16f
                    expenseListContainer.addView(emptyText)
                } else {
                    expenses.forEach { expense ->
                        addExpenseBubble(expense)
                    }
                }
            }
        }
    }

    private fun addExpenseBubble(expense: Expense) {
        val bubble = LinearLayout(this)
        bubble.orientation = LinearLayout.VERTICAL
        bubble.setPadding(18, 18, 18, 18)
        bubble.setBackgroundResource(R.drawable.login_card_bg)

        val bubbleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        bubbleParams.setMargins(0, 0, 0, 18)
        bubble.layoutParams = bubbleParams

        val amountText = TextView(this)
        amountText.text = "Amount: R${expense.amount}"
        amountText.textSize = 18f
        amountText.setTypeface(null, Typeface.BOLD)

        val dateText = TextView(this)
        dateText.text = "Date: ${expense.date}"
        dateText.textSize = 15f

        val timeText = TextView(this)
        timeText.text = "Time: ${expense.startTime} - ${expense.endTime}"
        timeText.textSize = 15f

        val descriptionText = TextView(this)
        descriptionText.text = "Description: ${expense.description}"
        descriptionText.textSize = 15f

        val buttonRow = LinearLayout(this)
        buttonRow.orientation = LinearLayout.HORIZONTAL

        val editButton = Button(this)
        editButton.text = "Edit"
        editButton.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val deleteButton = Button(this)
        deleteButton.text = "Delete"
        deleteButton.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        editButton.setOnClickListener {
            val intent = Intent(this, EditExpense::class.java)
            intent.putExtra("expenseId", expense.id)
            startActivity(intent)
        }

        deleteButton.setOnClickListener {
            lifecycleScope.launch {
                db.expenseDao().deleteExpense(expense)

                runOnUiThread {
                    Toast.makeText(this@ExpenseList, "Expense deleted", Toast.LENGTH_SHORT).show()
                    loadExpenses()
                }
            }
        }

        buttonRow.addView(editButton)
        buttonRow.addView(deleteButton)

        bubble.addView(amountText)
        bubble.addView(dateText)
        bubble.addView(timeText)
        bubble.addView(descriptionText)
        bubble.addView(buttonRow)

        expenseListContainer.addView(bubble)
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