package com.example.budgetquest

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetquest.firebase.FirebaseExpense
import com.example.budgetquest.firebase.FirebaseRepository
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.util.Calendar
import java.util.Locale

class ExpenseList : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository

    private lateinit var expenseListContainer: LinearLayout
    private lateinit var tvExpenseListTitle: TextView
    private lateinit var btnBackHome: Button
    private lateinit var barChart: BarChart
    private lateinit var edtStartDate: EditText
    private lateinit var edtEndDate: EditText
    private lateinit var btnGenerateGraph: Button
    private lateinit var btnViewExpensesByDate: TextView
    private lateinit var tvDateFilterStatus: TextView
    private lateinit var btnShowAllExpenses: TextView
    private lateinit var tvGraphSummary: TextView

    private var categoryName: String = ""
    private var startDate: String = ""
    private var endDate: String = ""
    private var userUid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_list)

        repository = FirebaseRepository()

        userUid = intent.getStringExtra("userUid")
            ?: repository.getCurrentUserId().orEmpty()

        categoryName = intent.getStringExtra("categoryName").orEmpty()

        if (userUid.isBlank()) {
            Toast.makeText(
                this,
                "User not found. Please log in again.",
                Toast.LENGTH_SHORT
            ).show()

            openLoginPage()
            return
        }

        if (categoryName.isBlank()) {
            Toast.makeText(
                this,
                "Category not found.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        tvExpenseListTitle = findViewById(R.id.tvExpenseListTitle)
        expenseListContainer = findViewById(R.id.expenseListContainer)
        btnBackHome = findViewById(R.id.btnBackHome)
        edtStartDate = findViewById(R.id.edtStartDate)
        edtEndDate = findViewById(R.id.edtEndDate)
        btnGenerateGraph = findViewById(R.id.btnGenerateGraph)
        barChart = findViewById(R.id.barChart)
        btnViewExpensesByDate = findViewById(R.id.btnViewExpensesByDate)
        tvDateFilterStatus = findViewById(R.id.tvDateFilterStatus)
        btnShowAllExpenses = findViewById(R.id.btnShowAllExpenses)
        tvGraphSummary = findViewById(R.id.tvGraphSummary)

        tvExpenseListTitle.text = "$categoryName Expenses"

        btnBackHome.setOnClickListener {
            openHomePage()
        }

        edtStartDate.setOnClickListener {
            showDatePicker { selectedDate ->
                startDate = selectedDate
                edtStartDate.setText(selectedDate)
                resetExpenseCardFilter()
            }
        }

        edtEndDate.setOnClickListener {
            showDatePicker { selectedDate ->
                endDate = selectedDate
                edtEndDate.setText(selectedDate)
                resetExpenseCardFilter()
            }
        }

        btnViewExpensesByDate.setOnClickListener {
            if (!hasSelectedDateRange()) {
                Toast.makeText(
                    this,
                    "Please select both start and end dates.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (startDate > endDate) {
                Toast.makeText(
                    this,
                    "Start date cannot be after end date.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            loadExpensesByDateRange()
        }

        btnShowAllExpenses.setOnClickListener {
            startDate = ""
            endDate = ""

            edtStartDate.text.clear()
            edtEndDate.text.clear()

            tvDateFilterStatus.text = "Showing all expenses for this category."

            barChart.clear()
            barChart.invalidate()
            tvGraphSummary.text = ""

            loadExpenses()
        }

        btnGenerateGraph.setOnClickListener {
            if (!hasSelectedDateRange()) {
                Toast.makeText(
                    this,
                    "Please select both start and end dates.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (startDate > endDate) {
                Toast.makeText(
                    this,
                    "Start date cannot be after end date.",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            loadGraphData()
        }

        NavigationHelper.setupBottomNavigation(
            activity = this,
            userUid = userUid,
            currentPage = "ExpenseList"
        )

        loadExpenses()
    }

    override fun onResume() {
        super.onResume()

        if (::repository.isInitialized && userUid.isNotBlank() && categoryName.isNotBlank()) {
            loadExpenses()
        }
    }

    private fun loadExpenses() {
        repository.getExpensesByCategory(
            uid = userUid,
            categoryName = categoryName,
            onSuccess = { expenses ->
                displayExpenses(expenses)
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

    private fun loadExpensesByDateRange() {
        repository.getExpensesByCategory(
            uid = userUid,
            categoryName = categoryName,
            onSuccess = { expenses ->
                // Dates are saved as yyyy-MM-dd, so text comparison keeps date order.
                val filteredExpenses = expenses.filter {
                    it.date >= startDate && it.date <= endDate
                }

                tvDateFilterStatus.text =
                    "Showing expenses from $startDate to $endDate."

                displayExpenses(filteredExpenses)
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

    private fun displayExpenses(expenses: List<FirebaseExpense>) {
        expenseListContainer.removeAllViews()

        if (expenses.isEmpty()) {
            val emptyText = TextView(this)
            emptyText.text = "No expenses found for this category."
            emptyText.textSize = 16f
            emptyText.setTextColor(Color.parseColor("#263238"))

            expenseListContainer.addView(emptyText)
            return
        }

        expenses
            .sortedByDescending { it.date }
            .forEach { expense ->
                addExpenseBubble(expense)
            }
    }

    private fun resetExpenseCardFilter() {
        tvDateFilterStatus.text = "Showing all expenses for this category."
        loadExpenses()
    }

    private fun addExpenseBubble(expense: FirebaseExpense) {
        val bubble = LinearLayout(this)
        bubble.orientation = LinearLayout.VERTICAL
        bubble.setPadding(dp(18), dp(18), dp(18), dp(18))
        bubble.setBackgroundResource(R.drawable.login_card_bg)

        val bubbleParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        bubbleParams.setMargins(0, 0, 0, dp(18))
        bubble.layoutParams = bubbleParams

        val amountText = TextView(this)
        amountText.text = "Amount: ${formatMoney(expense.amount)}"
        amountText.textSize = 18f
        amountText.setTypeface(null, Typeface.BOLD)
        amountText.setTextColor(Color.parseColor("#263238"))

        val dateText = TextView(this)
        dateText.text = "Date: ${expense.date}"
        dateText.textSize = 15f
        dateText.setTextColor(Color.parseColor("#263238"))

        val timeText = TextView(this)
        timeText.text = "Time: ${expense.startTime} - ${expense.endTime}"
        timeText.textSize = 15f
        timeText.setTextColor(Color.parseColor("#263238"))

        val descriptionText = TextView(this)
        descriptionText.text = "Description: ${expense.description}"
        descriptionText.textSize = 15f
        descriptionText.setTextColor(Color.parseColor("#263238"))

        bubble.addView(amountText)
        bubble.addView(dateText)
        bubble.addView(timeText)
        bubble.addView(descriptionText)

        if (!expense.photoUrl.isNullOrEmpty()) {
            // Firebase stores the URI string used to reopen the selected receipt image.
            val receiptImage = ImageView(this)

            receiptImage.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(220)
            )

            receiptImage.scaleType = ImageView.ScaleType.CENTER_CROP
            receiptImage.setImageURI(Uri.parse(expense.photoUrl))

            receiptImage.setOnClickListener {
                val intent = Intent(this, FullImageActivity::class.java)
                intent.putExtra("imageUri", expense.photoUrl)
                startActivity(intent)
            }

            bubble.addView(receiptImage)
        }

        val buttonRow = LinearLayout(this)
        buttonRow.orientation = LinearLayout.HORIZONTAL

        val editButton = Button(this)
        editButton.text = "Edit"
        editButton.isAllCaps = false

        val editButtonParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        editButtonParams.setMargins(0, dp(8), dp(6), 0)
        editButton.layoutParams = editButtonParams

        val deleteButton = Button(this)
        deleteButton.text = "Delete"
        deleteButton.isAllCaps = false

        val deleteButtonParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        deleteButtonParams.setMargins(dp(6), dp(8), 0, 0)
        deleteButton.layoutParams = deleteButtonParams

        editButton.setOnClickListener {
            val intent = Intent(this, EditExpense::class.java)
            intent.putExtra("expenseId", expense.id)
            intent.putExtra("userUid", userUid)
            startActivity(intent)
        }

        deleteButton.setOnClickListener {
            deleteExpense(expense)
        }

        buttonRow.addView(editButton)
        buttonRow.addView(deleteButton)

        bubble.addView(buttonRow)

        expenseListContainer.addView(bubble)
    }

    private fun deleteExpense(expense: FirebaseExpense) {
        repository.deleteExpense(
            uid = userUid,
            expenseId = expense.id,
            onSuccess = {
                Toast.makeText(
                    this,
                    "Expense deleted.",
                    Toast.LENGTH_SHORT
                ).show()

                loadExpenses()
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

    private fun loadGraphData() {
        repository.getExpensesByCategory(
            uid = userUid,
            categoryName = categoryName,
            onSuccess = { expenses ->
                val filteredExpenses = expenses.filter {
                    it.date >= startDate && it.date <= endDate
                }

                val totalSpent = filteredExpenses.sumOf { it.amount }

                repository.getCategoryByName(
                    uid = userUid,
                    categoryName = categoryName,
                    onSuccess = { category ->
                        // The graph compares selected-period spending with the category limit.
                        displayGraph(
                            totalSpent = totalSpent,
                            monthlyLimit = category?.monthlyLimit ?: 0.0
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

    private fun displayGraph(
        totalSpent: Double,
        monthlyLimit: Double
    ) {
        tvGraphSummary.text =
            "Total spent in $categoryName: ${formatMoney(totalSpent)}"

        val labels = listOf("Spent", "Limit")

        val entries = listOf(
            BarEntry(0f, totalSpent.toFloat()),
            BarEntry(1f, monthlyLimit.toFloat())
        )

        val dataSet = BarDataSet(entries, "")
        dataSet.colors = listOf(
            Color.parseColor("#4CAF50"),
            Color.parseColor("#F44336")
        )
        dataSet.valueTextSize = 14f

        val data = BarData(dataSet)
        data.barWidth = 0.4f

        barChart.data = data

        barChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            granularity = 1f
            labelCount = labels.size
            valueFormatter = IndexAxisValueFormatter(labels)
        }

        barChart.axisRight.isEnabled = false

        barChart.axisLeft.apply {
            setDrawLabels(true)
            setDrawGridLines(true)
            axisMinimum = 0f
        }

        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.animateY(600)
        barChart.invalidate()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, year, month, day ->
                val date = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    year,
                    month + 1,
                    day
                )

                onDateSelected(date)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun hasSelectedDateRange(): Boolean {
        return startDate.isNotEmpty() && endDate.isNotEmpty()
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
        return String.format(Locale.US, "R%.2f", amount)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}
