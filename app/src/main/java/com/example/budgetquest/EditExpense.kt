package com.example.budgetquest

import Data.Database.AppDatabase
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgetquest.data.Expense
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class EditExpense : AppCompatActivity() {

    private lateinit var db: AppDatabase

    private lateinit var spnEditCategory: Spinner
    private lateinit var edtEditAmount: EditText
    private lateinit var edtEditDate: EditText
    private lateinit var edtEditStartTime: EditText
    private lateinit var edtEditEndTime: EditText
    private lateinit var edtEditDescription: EditText
    private lateinit var btnUpdateExpense: Button
    private lateinit var btnCancelEdit: Button

    private val categoryNames = mutableListOf<String>()
    private var currentExpense: Expense? = null
    private var expenseId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_expense)

        db = AppDatabase.getDatabase(this)

        spnEditCategory = findViewById(R.id.spnEditCategory)
        edtEditAmount = findViewById(R.id.edtEditAmount)
        edtEditDate = findViewById(R.id.edtEditDate)
        edtEditStartTime = findViewById(R.id.edtEditStartTime)
        edtEditEndTime = findViewById(R.id.edtEditEndTime)
        edtEditDescription = findViewById(R.id.edtEditDescription)
        btnUpdateExpense = findViewById(R.id.btnUpdateExpense)
        btnCancelEdit = findViewById(R.id.btnCancelEdit)

        expenseId = intent.getIntExtra("expenseId", -1)

        if (expenseId == -1) {
            Toast.makeText(this, "Expense not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadCategoriesAndExpense()

        edtEditDate.setOnClickListener {
            showDatePicker()
        }

        edtEditStartTime.setOnClickListener {
            showTimePicker(edtEditStartTime)
        }

        edtEditEndTime.setOnClickListener {
            showTimePicker(edtEditEndTime)
        }

        btnUpdateExpense.setOnClickListener {
            updateExpense()
        }

        btnCancelEdit.setOnClickListener {
            finish()
        }
    }

    private fun loadCategoriesAndExpense() {
        lifecycleScope.launch {
            val categories = db.categoryDao().getAllCategories()
            val expense = db.expenseDao().getExpenseById(expenseId)

            runOnUiThread {
                if (expense == null) {
                    Toast.makeText(this@EditExpense, "Expense not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@runOnUiThread
                }

                currentExpense = expense

                categoryNames.clear()
                categoryNames.add("Select category")

                categories.forEach {
                    categoryNames.add(it.name)
                }

                val adapter = ArrayAdapter(
                    this@EditExpense,
                    android.R.layout.simple_spinner_item,
                    categoryNames
                )

                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spnEditCategory.adapter = adapter

                val selectedIndex = categoryNames.indexOf(expense.category)
                if (selectedIndex >= 0) {
                    spnEditCategory.setSelection(selectedIndex)
                }

                edtEditAmount.setText(expense.amount.toString())
                edtEditDate.setText(expense.date)
                edtEditStartTime.setText(expense.startTime)
                edtEditEndTime.setText(expense.endTime)
                edtEditDescription.setText(expense.description)
            }
        }
    }

    private fun updateExpense() {
        val oldExpense = currentExpense

        if (oldExpense == null) {
            Toast.makeText(this, "Expense not loaded", Toast.LENGTH_SHORT).show()
            return
        }

        val category = spnEditCategory.selectedItem.toString()
        val amountText = edtEditAmount.text.toString().trim()
        val date = edtEditDate.text.toString().trim()
        val startTime = edtEditStartTime.text.toString().trim()
        val endTime = edtEditEndTime.text.toString().trim()
        val description = edtEditDescription.text.toString().trim()

        if (
            category == "Select category" ||
            amountText.isEmpty() ||
            date.isEmpty() ||
            startTime.isEmpty() ||
            endTime.isEmpty() ||
            description.isEmpty()
        ) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()

        if (amount == null) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedExpense = oldExpense.copy(
            category = category,
            amount = amount,
            date = date,
            startTime = startTime,
            endTime = endTime,
            description = description
        )

        lifecycleScope.launch {
            db.expenseDao().updateExpense(updatedExpense)

            runOnUiThread {
                Toast.makeText(this@EditExpense, "Expense updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedMonth = String.format(Locale.getDefault(), "%02d", selectedMonth + 1)
                val formattedDay = String.format(Locale.getDefault(), "%02d", selectedDay)
                val selectedDate = "$selectedYear-$formattedMonth-$formattedDay"
                edtEditDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.show()
    }

    private fun showTimePicker(targetEditText: EditText) {
        val calendar = Calendar.getInstance()

        val timePickerDialog = TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                targetEditText.setText(selectedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        )

        timePickerDialog.show()
    }
}