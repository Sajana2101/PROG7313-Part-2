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
import android.content.Intent
import android.widget.TextView
// importing these because they are used for navigation between screens
// and for handling bottom nav text buttons


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

        db = AppDatabase.getDatabase(this) // initialises roomDb to store
        // and retrieve data for expenses

        edtExpCtgry = findViewById(R.id.edtExpCtgry)
        edtExpAmnt = findViewById(R.id.edtExpAmnt)
        edtExpD8 = findViewById(R.id.edtExpD8)
        edtStartTime = findViewById(R.id.edtStartTime)
        edtEndTime = findViewById(R.id.edtEndTime)
        edtExpDescrip = findViewById(R.id.edtExpDescrip)
        btnPhoto = findViewById(R.id.btnPhoto)
        btnExpSave = findViewById(R.id.btnExpSave)

        edtExpD8.setOnClickListener {
            showDatePicker() // opens date picker to prevent manually typing
        }

        // time picker opens for start and end time fields
        edtStartTime.setOnClickListener {
            showTimePicker(edtStartTime)
        }

        edtEndTime.setOnClickListener {
            showTimePicker(edtEndTime)
        }

        // placeholder for photo functionality (to be implemented later by team member in charge of this requirement)
        // currently just shows a message so the app doesn't crash or do nothing at sll
        btnPhoto.setOnClickListener {
            Toast.makeText(this, "Photo feature will be added later", Toast.LENGTH_SHORT).show()
        }

        btnExpSave.setOnClickListener {
            saveExpense() // inputs are validated/stored in db when users click Save
        }

        // bottom nav button listeners for the expense page
        // (placeholders/toasts are used for screens that aren't implemented yet)
        val navHome = findViewById<TextView>(R.id.navHome)
        val navCategories = findViewById<TextView>(R.id.navCategories)
        val navAddExpense = findViewById<TextView>(R.id.navAddExpense)
        val navGoals = findViewById<TextView>(R.id.navGoals)
        val navProfile = findViewById<TextView>(R.id.navProfile)

        navHome.setOnClickListener {
            startActivity(Intent(this, Home::class.java))
        }

        navCategories.setOnClickListener {
            Toast.makeText(this, "Categories screen will be added soon", Toast.LENGTH_SHORT).show()
        }

        navAddExpense.setOnClickListener {
            Toast.makeText(this, "You are already on the Add Expense screen", Toast.LENGTH_SHORT).show()
        }

        navGoals.setOnClickListener {
            startActivity(Intent(this, MonthlyGoals::class.java))
        }

        navProfile.setOnClickListener {
            Toast.makeText(this, "Profile screen will be added soon", Toast.LENGTH_SHORT).show()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // the saveExpense function collects user input, validates it,
// and then saves the expense into the Room db
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
        } // validations added, so no required fields are left empty

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
                // clearing all input fields after saving to allow new entries to be made
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