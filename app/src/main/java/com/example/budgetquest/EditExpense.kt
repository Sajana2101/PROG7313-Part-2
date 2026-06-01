package com.example.budgetquest

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetquest.firebase.FirebaseExpense
import com.example.budgetquest.firebase.FirebaseRepository
import java.util.Calendar
import java.util.Locale

class EditExpense : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository

    private lateinit var spnEditCategory: Spinner
    private lateinit var edtEditAmount: EditText
    private lateinit var edtEditDate: EditText
    private lateinit var edtEditStartTime: EditText
    private lateinit var edtEditEndTime: EditText
    private lateinit var edtEditDescription: EditText
    private lateinit var btnUpdateExpense: Button
    private lateinit var btnCancelEdit: Button

    private val categoryNames = mutableListOf<String>()

    private var currentExpense: FirebaseExpense? = null
    private var expenseId: String = ""
    private var userUid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_expense)

        repository = FirebaseRepository()

        userUid = intent.getStringExtra("userUid")
            ?: repository.getCurrentUserId().orEmpty()

        expenseId = intent.getStringExtra("expenseId").orEmpty()

        if (userUid.isBlank()) {
            Toast.makeText(
                this,
                "User not found. Please log in again.",
                Toast.LENGTH_SHORT
            ).show()

            openLoginPage()
            return
        }

        if (expenseId.isBlank()) {
            Toast.makeText(
                this,
                "Expense not found.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        spnEditCategory = findViewById(R.id.spnEditCategory)
        edtEditAmount = findViewById(R.id.edtEditAmount)
        edtEditDate = findViewById(R.id.edtEditDate)
        edtEditStartTime = findViewById(R.id.edtEditStartTime)
        edtEditEndTime = findViewById(R.id.edtEditEndTime)
        edtEditDescription = findViewById(R.id.edtEditDescription)
        btnUpdateExpense = findViewById(R.id.btnUpdateExpense)
        btnCancelEdit = findViewById(R.id.btnCancelEdit)

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

        loadCategoriesAndExpense()
    }

    private fun loadCategoriesAndExpense() {
        repository.getCategories(
            uid = userUid,
            onSuccess = { categories ->
                categoryNames.clear()
                categoryNames.add("Select category")

                categories.forEach { category ->
                    categoryNames.add(category.name)
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    categoryNames
                )

                adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
                )

                spnEditCategory.adapter = adapter

                loadSelectedExpense()
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

    private fun loadSelectedExpense() {
        repository.getExpenseById(
            uid = userUid,
            expenseId = expenseId,
            onSuccess = { expense ->
                if (expense == null) {
                    Toast.makeText(
                        this,
                        "Expense not found.",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                    return@getExpenseById
                }

                currentExpense = expense

                val selectedIndex = categoryNames.indexOfFirst {
                    it.equals(expense.category, ignoreCase = true)
                }

                if (selectedIndex >= 0) {
                    spnEditCategory.setSelection(selectedIndex)
                }

                edtEditAmount.setText(expense.amount.toString())
                edtEditDate.setText(expense.date)
                edtEditStartTime.setText(expense.startTime)
                edtEditEndTime.setText(expense.endTime)
                edtEditDescription.setText(expense.description)
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

    private fun updateExpense() {
        val oldExpense = currentExpense

        if (oldExpense == null) {
            Toast.makeText(
                this,
                "Expense has not loaded yet.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val category = spnEditCategory.selectedItem?.toString().orEmpty()
        val amountText = edtEditAmount.text.toString().trim()
        val date = edtEditDate.text.toString().trim()
        val startTime = edtEditStartTime.text.toString().trim()
        val endTime = edtEditEndTime.text.toString().trim()
        val description = edtEditDescription.text.toString().trim()

        if (
            category.isBlank() ||
            category == "Select category" ||
            amountText.isEmpty() ||
            date.isEmpty() ||
            startTime.isEmpty() ||
            endTime.isEmpty() ||
            description.isEmpty()
        ) {
            Toast.makeText(
                this,
                "Please fill in all fields.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val amount = amountText.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            Toast.makeText(
                this,
                "Please enter a positive amount.",
                Toast.LENGTH_SHORT
            ).show()
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

        btnUpdateExpense.isEnabled = false

        validateDebtPaymentAndUpdate(updatedExpense)
    }

    private fun validateDebtPaymentAndUpdate(updatedExpense: FirebaseExpense) {
        repository.getDebtByExpenseCategory(
            uid = userUid,
            categoryName = updatedExpense.category,
            onSuccess = { linkedDebt ->
                if (linkedDebt == null) {
                    saveUpdatedExpense(updatedExpense)
                    return@getDebtByExpenseCategory
                }

                repository.getExpensesByCategory(
                    uid = userUid,
                    categoryName = linkedDebt.expenseCategory,
                    onSuccess = { debtPayments ->
                        val amountPaidByOtherExpenses = debtPayments
                            .filter { it.id != updatedExpense.id }
                            .sumOf { it.amount }

                        val permittedAmount =
                            linkedDebt.totalAmount - amountPaidByOtherExpenses

                        if (updatedExpense.amount > permittedAmount) {
                            btnUpdateExpense.isEnabled = true

                            Toast.makeText(
                                this,
                                "Payment cannot exceed the remaining balance of ${
                                    formatMoney(permittedAmount.coerceAtLeast(0.0))
                                }.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            saveUpdatedExpense(updatedExpense)
                        }
                    },
                    onError = { errorMessage ->
                        btnUpdateExpense.isEnabled = true

                        Toast.makeText(
                            this,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            },
            onError = { errorMessage ->
                btnUpdateExpense.isEnabled = true

                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun saveUpdatedExpense(updatedExpense: FirebaseExpense) {
        repository.saveExpense(
            uid = userUid,
            expense = updatedExpense,
            onSuccess = {
                btnUpdateExpense.isEnabled = true

                Toast.makeText(
                    this,
                    "Expense updated successfully.",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            },
            onError = { errorMessage ->
                btnUpdateExpense.isEnabled = true

                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedMonth =
                    String.format(Locale.getDefault(), "%02d", selectedMonth + 1)

                val formattedDay =
                    String.format(Locale.getDefault(), "%02d", selectedDay)

                edtEditDate.setText(
                    "$selectedYear-$formattedMonth-$formattedDay"
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(targetEditText: EditText) {
        val calendar = Calendar.getInstance()

        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val selectedTime =
                    String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)

                targetEditText.setText(selectedTime)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun openLoginPage() {
        repository.logout()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun formatMoney(amount: Double): String {
        return String.format(Locale.US, "R%.2f", amount)
    }
}