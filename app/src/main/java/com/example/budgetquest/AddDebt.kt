package com.example.budgetquest

import Data.Database.AppDatabase
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgetquest.data.Category
import com.example.budgetquest.data.Debt
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddDebt : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var edtDebtName: EditText
    private lateinit var edtDebtAmount: EditText
    private lateinit var edtDebtDueDate: EditText
    private lateinit var btnSaveDebt: Button
    private lateinit var tvDebtFormTitle: TextView

    private var userId: Int = -1
    private var debtId: Int = -1
    private var existingDebt: Debt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_debt)

        userId = intent.getIntExtra("userId", -1)
        debtId = intent.getIntExtra("debtId", -1)

        if (userId == -1) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db = AppDatabase.getDatabase(this)

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

        if (debtId != -1) {
            tvDebtFormTitle.text = "Edit Debt"
            btnSaveDebt.text = "Update Debt"
            loadExistingDebt()
        }

        btnSaveDebt.setOnClickListener {
            saveDebt()
        }
    }

    private fun loadExistingDebt() {
        lifecycleScope.launch {
            existingDebt = db.debtDao().getDebtById(debtId)

            existingDebt?.let { debt ->
                edtDebtName.setText(debt.debtName)
                edtDebtAmount.setText(debt.totalAmount.toString())
                edtDebtDueDate.setText(debt.dueDate)
            }
        }
    }

    private fun saveDebt() {
        val debtName = edtDebtName.text.toString().trim()
        val totalAmount = edtDebtAmount.text.toString().trim().toDoubleOrNull()
        val dueDate = edtDebtDueDate.text.toString().trim()

        if (debtName.isEmpty()) {
            Toast.makeText(this, "Please enter a debt name", Toast.LENGTH_SHORT).show()
            return
        }

        if (totalAmount == null || totalAmount <= 0) {
            Toast.makeText(this, "Please enter a positive debt amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidDate(dueDate)) {
            Toast.makeText(this, "Please select a valid due date", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val debtBeingEdited = existingDebt

            if (debtBeingEdited == null) {
                val categoryName = "Debt Payment - $debtName"

                val newDebt = Debt(
                    userId = userId,
                    debtName = debtName,
                    totalAmount = totalAmount,
                    dueDate = dueDate,
                    expenseCategory = categoryName
                )

                val existingCategory = db.categoryDao()
                    .getCategoryByNameAndUser(categoryName, userId)

                if (existingCategory == null) {
                    val debtCategory = Category(
                        userId = userId,
                        name = categoryName,
                        monthlyLimit = totalAmount
                    )

                    db.categoryDao().insertCategory(debtCategory)
                } else {
                    db.categoryDao().updateCategory(
                        existingCategory.copy(monthlyLimit = totalAmount)
                    )
                }

                db.debtDao().insertDebt(newDebt)

            } else {
                val updatedDebt = debtBeingEdited.copy(
                    debtName = debtName,
                    totalAmount = totalAmount,
                    dueDate = dueDate
                )

                db.debtDao().updateDebt(updatedDebt)

                val linkedCategory = db.categoryDao()
                    .getCategoryByNameAndUser(debtBeingEdited.expenseCategory, userId)

                if (linkedCategory != null) {
                    db.categoryDao().updateCategory(
                        linkedCategory.copy(monthlyLimit = totalAmount)
                    )
                }
            }

            Toast.makeText(
                this@AddDebt,
                "Debt saved. Log payments as an expense.",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val existingDate = edtDebtDueDate.text.toString().trim()

        if (existingDate.isNotEmpty()) {
            try {
                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
                formatter.isLenient = false
                val parsedDate = formatter.parse(existingDate)

                if (parsedDate != null) {
                    calendar.time = parsedDate
                }
            } catch (exception: Exception) {
                // The calendar remains on today's date.
            }
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(selectedYear, selectedMonth, selectedDay)

                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
                edtDebtDueDate.setText(formatter.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun isValidDate(date: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
            formatter.isLenient = false
            formatter.parse(date)
            true
        } catch (exception: Exception) {
            false
        }
    }
}