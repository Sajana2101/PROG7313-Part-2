package com.example.budgetquest

import Data.Database.AppDatabase
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.budgetquest.data.Expense
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class Expenses : AppCompatActivity() {

    private lateinit var edtExpCtgry: EditText
    private lateinit var edtExpAmnt: EditText
    private lateinit var edtExpD8: EditText
    private lateinit var edtStartTime: EditText
    private lateinit var edtEndTime: EditText
    private lateinit var edtExpDescrip: EditText
    private lateinit var btnPhoto: Button
    private lateinit var btnExpSave: Button

    private lateinit var db: AppDatabase
    private var selectedPhotoUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_expenses)

        db = AppDatabase.getDatabase(this)

        edtExpCtgry = findViewById(R.id.edtExpCtgry)
        edtExpAmnt = findViewById(R.id.edtExpAmnt)
        edtExpD8 = findViewById(R.id.edtExpD8)
        edtStartTime = findViewById(R.id.edtStartTime)
        edtEndTime = findViewById(R.id.edtEndTime)
        edtExpDescrip = findViewById(R.id.edtExpDescrip)
        btnPhoto = findViewById(R.id.btnPhoto)
        btnExpSave = findViewById(R.id.btnExpSave)

        edtExpD8.setOnClickListener {
            showDatePicker()
        }

        edtStartTime.setOnClickListener {
            showTimePicker(edtStartTime)
        }

        edtEndTime.setOnClickListener {
            showTimePicker(edtEndTime)
        }

        btnPhoto.setOnClickListener {
            Toast.makeText(this, "Photo feature will be added later", Toast.LENGTH_SHORT).show()
        }

        btnExpSave.setOnClickListener {
            saveExpense()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun saveExpense() {
        val category = edtExpCtgry.text.toString().trim()
        val amountText = edtExpAmnt.text.toString().trim()
        val date = edtExpD8.text.toString().trim()
        val startTime = edtStartTime.text.toString().trim()
        val endTime = edtEndTime.text.toString().trim()
        val description = edtExpDescrip.text.toString().trim()

        if (
            category.isEmpty() ||
            amountText.isEmpty() ||
            date.isEmpty() ||
            startTime.isEmpty() ||
            endTime.isEmpty() ||
            description.isEmpty()
        ) {
            Toast.makeText(this, "Please fill in all the required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()

        if (amount == null) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val expense = Expense(
            category = category,
            amount = amount,
            date = date,
            startTime = startTime,
            endTime = endTime,
            description = description,
            photoUrl = selectedPhotoUri
        )

        lifecycleScope.launch {
            db.expenseDao().insertExpense(expense)

            runOnUiThread {
                Toast.makeText(this@Expenses, "Expense saved successfully", Toast.LENGTH_SHORT).show()

                edtExpCtgry.text.clear()
                edtExpAmnt.text.clear()
                edtExpD8.text.clear()
                edtStartTime.text.clear()
                edtEndTime.text.clear()
                edtExpDescrip.text.clear()
                selectedPhotoUri = null
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
                edtExpD8.setText(selectedDate)
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