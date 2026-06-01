package com.example.budgetquest

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetquest.firebase.FirebaseCategory
import com.example.budgetquest.firebase.FirebaseDebt
import com.example.budgetquest.firebase.FirebaseRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@SuppressLint("SetTextI18n")
class AddDebt : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository

    private lateinit var edtDebtName: EditText
    private lateinit var edtDebtAmount: EditText
    private lateinit var edtDebtDueDate: EditText
    private lateinit var btnSaveDebt: Button
    private lateinit var tvDebtFormTitle: TextView

    private var userUid: String = ""
    private var debtId: String = ""
    private var existingDebt: FirebaseDebt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_debt)

        repository = FirebaseRepository()

        userUid = intent.getStringExtra("userUid")
            ?: repository.getCurrentUserId().orEmpty()

        debtId = intent.getStringExtra("debtId").orEmpty()

        if (userUid.isBlank()) {
            Toast.makeText(
                this,
                "User not found. Please log in again.",
                Toast.LENGTH_SHORT
            ).show()

            openLoginPage()
            return
        }

        edtDebtName = findViewById(R.id.edtDebtName)
        edtDebtAmount = findViewById(R.id.edtDebtAmount)
        edtDebtDueDate = findViewById(R.id.edtDebtDueDate)
        btnSaveDebt = findViewById(R.id.btnSaveDebt)
        tvDebtFormTitle = findViewById(R.id.tvDebtFormTitle)

        edtDebtDueDate.setOnClickListener {
            showDatePicker()
        }

        findViewById<TextView>(R.id.btnBackDebt).setOnClickListener {
            finish()
        }

        if (debtId.isNotBlank()) {
            tvDebtFormTitle.text = "Edit Debt"
            btnSaveDebt.text = "Update Debt"
            loadExistingDebt()
        }

        btnSaveDebt.setOnClickListener {
            saveDebt()
        }
    }

    private fun loadExistingDebt() {
        repository.getDebtById(
            uid = userUid,
            debtId = debtId,
            onSuccess = { debt ->
                if (debt == null) {
                    Toast.makeText(
                        this,
                        "Debt not found.",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                    return@getDebtById
                }

                existingDebt = debt

                edtDebtName.setText(debt.debtName)
                edtDebtAmount.setText(debt.totalAmount.toString())
                edtDebtDueDate.setText(debt.dueDate)
            },
            onError = { errorMessage ->
                showError(errorMessage)
            }
        )
    }

    private fun saveDebt() {
        val debtName = edtDebtName.text.toString().trim()
        val totalAmount = edtDebtAmount.text
            .toString()
            .trim()
            .toDoubleOrNull()

        val dueDate = edtDebtDueDate.text.toString().trim()

        if (debtName.isEmpty()) {
            Toast.makeText(
                this,
                "Please enter a debt name.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (totalAmount == null || totalAmount <= 0) {
            Toast.makeText(
                this,
                "Please enter a positive debt amount.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!isValidDate(dueDate)) {
            Toast.makeText(
                this,
                "Please select a valid due date.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        btnSaveDebt.isEnabled = false

        val debtBeingEdited = existingDebt

        if (debtBeingEdited == null) {
            createNewDebt(
                debtName = debtName,
                totalAmount = totalAmount,
                dueDate = dueDate
            )
        } else {
            updateExistingDebt(
                currentDebt = debtBeingEdited,
                debtName = debtName,
                totalAmount = totalAmount,
                dueDate = dueDate
            )
        }
    }

    private fun createNewDebt(
        debtName: String,
        totalAmount: Double,
        dueDate: String
    ) {
        val categoryName = "Debt Payment - $debtName"

        repository.getCategoryByName(
            uid = userUid,
            categoryName = categoryName,
            onSuccess = { existingCategory ->
                val categoryToSave = existingCategory?.copy(
                    monthlyLimit = totalAmount
                ) ?: FirebaseCategory(
                    name = categoryName,
                    monthlyLimit = totalAmount
                )

                repository.saveCategory(
                    uid = userUid,
                    category = categoryToSave,
                    onSuccess = {
                        val newDebt = FirebaseDebt(
                            debtName = debtName,
                            totalAmount = totalAmount,
                            dueDate = dueDate,
                            expenseCategory = categoryName
                        )

                        saveDebtToFirebase(newDebt)
                    },
                    onError = { errorMessage ->
                        saveFailed(errorMessage)
                    }
                )
            },
            onError = { errorMessage ->
                saveFailed(errorMessage)
            }
        )
    }

    private fun updateExistingDebt(
        currentDebt: FirebaseDebt,
        debtName: String,
        totalAmount: Double,
        dueDate: String
    ) {
        val updatedDebt = currentDebt.copy(
            debtName = debtName,
            totalAmount = totalAmount,
            dueDate = dueDate
        )

        repository.getCategoryByName(
            uid = userUid,
            categoryName = currentDebt.expenseCategory,
            onSuccess = { existingCategory ->
                val categoryToSave = existingCategory?.copy(
                    monthlyLimit = totalAmount
                ) ?: FirebaseCategory(
                    name = currentDebt.expenseCategory,
                    monthlyLimit = totalAmount
                )

                repository.saveCategory(
                    uid = userUid,
                    category = categoryToSave,
                    onSuccess = {
                        saveDebtToFirebase(updatedDebt)
                    },
                    onError = { errorMessage ->
                        saveFailed(errorMessage)
                    }
                )
            },
            onError = { errorMessage ->
                saveFailed(errorMessage)
            }
        )
    }

    private fun saveDebtToFirebase(debt: FirebaseDebt) {
        repository.saveDebt(
            uid = userUid,
            debt = debt,
            onSuccess = {
                btnSaveDebt.isEnabled = true

                Toast.makeText(
                    this,
                    "Debt saved. Log payments as an expense.",
                    Toast.LENGTH_LONG
                ).show()

                finish()
            },
            onError = { errorMessage ->
                saveFailed(errorMessage)
            }
        )
    }

    private fun saveFailed(errorMessage: String) {
        btnSaveDebt.isEnabled = true
        showError(errorMessage)
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val existingDate = edtDebtDueDate.text.toString().trim()

        if (
            existingDate.isNotEmpty()) {
            try {
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
                formatter.isLenient = false

                val parsedDate = formatter.parse(existingDate)

                if (parsedDate != null) {
                    calendar.time = parsedDate
                }
            } catch (_: Exception) {
                // Uses today's date when the existing value cannot be parsed.
            }
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()

                selectedDate.set(
                    selectedYear,
                    selectedMonth,
                    selectedDay
                )

                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)

                edtDebtDueDate.setText(
                    formatter.format(selectedDate.time)
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.minDate =
            System.currentTimeMillis() - 1000

        datePickerDialog.show()
    }

    private fun isValidDate(date: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
            formatter.isLenient = false
            formatter.parse(date) != null
        } catch (_: Exception) {
            false
        }
    }

    private fun showError(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun openLoginPage() {
        repository.logout()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }
}