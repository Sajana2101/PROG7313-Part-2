package com.example.budgetquest

import Data.Database.AppDatabase
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Typeface
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgetquest.data.Expense
import kotlinx.coroutines.launch
import android.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
//imports for the horizontal bar chart
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

class ExpenseList : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var expenseListContainer: LinearLayout
    private lateinit var tvExpenseListTitle: TextView
    private lateinit var btnBackHome: Button

    private lateinit var barChart: BarChart
    private lateinit var edtSartDate: EditText
    private lateinit var edtEndDate: EditText
    private lateinit var btnGenerateGraph: Button
    private lateinit var btnViewExpensesByDate: TextView
    private lateinit var tvDateFilterStatus: TextView
    private lateinit var btnShowAllExpenses: TextView


    private var categoryName: String = ""
    private var startDate = ""
    private var endDate = ""

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
        edtSartDate = findViewById(R.id.edtStartDate)
        edtEndDate = findViewById(R.id.edtEndDate)
        btnGenerateGraph = findViewById(R.id.btnGenerateGraph)
        barChart = findViewById(R.id.barChart)
        btnViewExpensesByDate = findViewById(R.id.btnViewExpensesByDate)
        tvDateFilterStatus = findViewById(R.id.tvDateFilterStatus)
        btnShowAllExpenses = findViewById(R.id.btnShowAllExpenses)


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

        edtSartDate.setOnClickListener {
            showDatePicker {
                startDate = it
                edtSartDate.setText(it)

                // resets the visible expense cards when the user changes the date range
                resetExpenseCardFilter()
            }
        }

        edtEndDate.setOnClickListener {
            showDatePicker {
                endDate = it
                edtEndDate.setText(it)

                // resets the visible expense cards when the user changes the date range
                resetExpenseCardFilter()
            }
        }

        btnViewExpensesByDate.setOnClickListener {
            if (startDate.isEmpty() || endDate.isEmpty()) {
                Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadExpensesByDateRange()
        }

        btnShowAllExpenses.setOnClickListener {
            startDate = ""
            endDate = ""

            edtSartDate.setText("")
            edtEndDate.setText("")

            tvDateFilterStatus.text = "Showing all expenses for this category."

            barChart.clear()
            barChart.invalidate()

            loadExpenses()
        }

        btnGenerateGraph.setOnClickListener {
            if (startDate.isEmpty() || endDate.isEmpty()) {
                Toast.makeText(this, "Please enter both dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // graph uses the selected dates, but the expense cards stay unfiltered
            resetExpenseCardFilter()
            loadGraphData()
        }


    }

    private fun showDatePicker( onDateSelected: (String) -> Unit ) {

        val calendar = Calendar.getInstance()

        DatePickerDialog(this, { _, year, month, day ->

                val date = String.format("%04d-%02d-%02d",year, month + 1, day )

                onDateSelected(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
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

    private fun resetExpenseCardFilter() {
        tvDateFilterStatus.text = "Showing all expenses for this category."
        loadExpenses()
    }

    private fun loadExpensesByDateRange() {
        lifecycleScope.launch {
            val expenses = db.expenseDao().getExpensesByCategoryAndUser(categoryName, userId)

            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

            try {
                val start = formatter.parse(startDate)
                val end = formatter.parse(endDate)

                val filteredExpenses = expenses.filter {
                    val expenseDate = formatter.parse(it.date)

                    expenseDate != null &&
                            start != null &&
                            end != null &&
                            !expenseDate.before(start) &&
                            !expenseDate.after(end)
                }

                runOnUiThread {
                    tvDateFilterStatus.text = "Showing expenses from $startDate to $endDate."
                    expenseListContainer.removeAllViews()

                    if (filteredExpenses.isEmpty()) {
                        val emptyText = TextView(this@ExpenseList)
                        emptyText.text = "No expenses found for this category in the selected date range."
                        emptyText.textSize = 16f
                        expenseListContainer.addView(emptyText)
                    } else {
                        filteredExpenses.forEach { expense ->
                            addExpenseBubble(expense)
                        }
                    }
                }
            } catch (exception: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ExpenseList, "Invalid date range selected", Toast.LENGTH_SHORT).show()
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


    private fun loadGraphData() {
        lifecycleScope.launch {

            val expenses = db.expenseDao()
                .getExpensesByCategoryAndUser(categoryName, userId)

            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

            val start = formatter.parse(startDate)
            val end = formatter.parse(endDate)

            val filtered = expenses.filter {
                val d = formatter.parse(it.date)
                d != null && start != null && end != null && !d.before(start) && !d.after(end)
            }

            val totalSpent = filtered.sumOf { it.amount }

            val goal = db.monthlyGoalDao().getGoalByUser(userId)

            runOnUiThread {

                //creates bar entries
                val minEntry = BarEntry(0f, goal?.minGoal?.toFloat() ?: 0f)
                val spentEntry = BarEntry(0f, totalSpent.toFloat())
                val maxEntry = BarEntry(0f, goal?.maxGoal?.toFloat() ?: 0f)

                val minSet = BarDataSet(listOf(minEntry), "Min Goal")
                val spentSet = BarDataSet(listOf(spentEntry), "Spent")
                val maxSet = BarDataSet(listOf(maxEntry), "Max Goal")

               //give each bar a different colour
                spentSet.color = Color.parseColor("#4CAF50") // green
                minSet.color = Color.parseColor("#FF9800")   // orange
                maxSet.color = Color.parseColor("#F44336")   // red

                spentSet.valueTextColor = Color.BLACK
                minSet.valueTextColor = Color.BLACK
                maxSet.valueTextColor = Color.BLACK

                spentSet.valueTextSize = 14f
                minSet.valueTextSize = 14f
                maxSet.valueTextSize = 14f

               //bar information
                val data = BarData(minSet,spentSet,maxSet)

                val groupSpace = 0.2f
                val barSpace = 0f
                val barWidth = 0.25f

                data.barWidth = barWidth

                barChart.data = data

               //ensure bars are spaced properly
                barChart.xAxis.axisMinimum = 0f
                barChart.xAxis.axisMaximum = 1f

                barChart.groupBars(0f, groupSpace, barSpace)


                barChart.xAxis.position =
                    com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM

                barChart.xAxis.setDrawGridLines(false)

                barChart.xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return when (value.toInt()) {
                            0 -> ""
                            else -> ""
                        }
                    }
                }

                //no values on the y-axis
                barChart.axisLeft.setDrawLabels(false)
                barChart.axisRight.setDrawLabels(false)
                barChart.axisLeft.setDrawGridLines(false)
                barChart.axisRight.setDrawGridLines(false)

                barChart.description.isEnabled = false
                barChart.legend.isEnabled = true

                // -----------------------------
                // 7. FINAL REFRESH
                // -----------------------------
                barChart.invalidate()
            }
        }
    }


}

