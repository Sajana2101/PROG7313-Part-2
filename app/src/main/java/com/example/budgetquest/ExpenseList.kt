package com.example.budgetquest

import Data.Database.AppDatabase
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
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

    // stores the currently logged in user
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

        // gets logged in user id from previous screen
        userId = intent.getIntExtra("userId", -1)

        // if no user id is found send them back to login
        if (userId == -1) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // gets database instance
        db = AppDatabase.getDatabase(this)

        tvExpenseListTitle = findViewById(R.id.tvExpenseListTitle)
        expenseListContainer = findViewById(R.id.expenseListContainer)
        btnBackHome = findViewById(R.id.btnBackHome)

        // gets category name passed from previous screen
        categoryName = intent.getStringExtra("categoryName") ?: ""

        tvExpenseListTitle.text = "$categoryName Expenses"

        btnBackHome.setOnClickListener {
            val intent = Intent(this, Home::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }

        setupBottomNav()
        loadExpenses()
    }

    override fun onResume() {
        super.onResume()
        // reloads expenses when coming back from edit screen
        if (categoryName.isNotEmpty() && userId != -1) {
            loadExpenses()
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val expenses = db.expenseDao().getExpensesByCategoryAndUser(categoryName, userId)

            runOnUiThread {
                expenseListContainer.removeAllViews()

                // shows message if no expenses exist
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
        // creates the expense card/bubble
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

        // shows the receipt image if the user attached one
        val receiptImage = ImageView(this)
        receiptImage.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            450
        )
        receiptImage.scaleType = ImageView.ScaleType.CENTER_CROP

        if (!expense.photoUrl.isNullOrEmpty()) {
            receiptImage.setImageURI(Uri.parse(expense.photoUrl))

            // opens full image when user clicks the receipt image
            receiptImage.setOnClickListener {
                val intent = Intent(this, FullImageActivity::class.java)
                intent.putExtra("imageUri", expense.photoUrl)
                startActivity(intent)
            }
        }
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

        // opens edit screen and sends expense id
        editButton.setOnClickListener {
            val intent = Intent(this, EditExpense::class.java)
            intent.putExtra("expenseId", expense.id)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        // deletes expense from db
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

        // only adds the image view if an image exists
        if (!expense.photoUrl.isNullOrEmpty()) {
            bubble.addView(receiptImage)
        }

        bubble.addView(buttonRow)

        expenseListContainer.addView(bubble)
    }

    // handles bottom navigation clicks
    private fun setupBottomNav() {
        findViewById<TextView>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, Home::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
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