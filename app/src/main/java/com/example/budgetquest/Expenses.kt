package com.example.budgetquest

import Data.Database.AppDatabase
import android.app.DatePickerDialog
import android.icu.util.Calendar
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
import com.example.budgetquest.data.MonthlyGoal
import kotlinx.coroutines.launch

class Expenses : AppCompatActivity() {

    private lateinit var etExpCtgry: EditText
    private lateinit var etExpD8: EditText
    private lateinit var etExpAmnt: EditText
    private lateinit var etExpDescrip: EditText
    private lateinit var btnPhoto: Button
    private lateinit var btnExpSave: Button
    private lateinit var etExpMinAmnt: EditText
    private lateinit var etExpMaxAmnt: EditText
    private lateinit var btnSaveGoals: Button

    private lateinit var db: AppDatabase

    private var selectedPhotoUri: String? = null
    // nullable, because the assignment rubric says adding a pic is optional



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_expenses)

        db = AppDatabase.getDatabase(this)

        //typecasting
        etExpCtgry = findViewById(R.id.etExpCtgry)
        etExpD8 = findViewById(R.id.etExpD8)
        etExpAmnt = findViewById(R.id.etExpAmnt)
        etExpDescrip = findViewById(R.id.etExpDescrip)
        btnPhoto = findViewById(R.id.btnPhoto)
        btnExpSave = findViewById(R.id.btnExpSave)
        etExpMinAmnt = findViewById(R.id.etExpMinAmnt)
        etExpMinAmnt = findViewById(R.id.etExpMaxAmnt)
        btnSaveGoals = findViewById(R.id.btnSaveGoals)

        etExpD8.setOnClickListener {
            showDatePicker()
        }

        btnPhoto.setOnClickListener {
            saveExpenses()
        }

        btnSaveGoals.setOnClickListener {
            SaveGoals()
        }








        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun saveExpenses() {
        val category = etExpCtgry.text.toString().trim()
        val amountText = etExpAmnt.text.toString().trim()
        val date = etExpD8.text.toString().trim()
        val description = etExpDescrip.text.toString().trim()


        //validation checks
        if (category.isEmpty() || amountText.isEmpty() || date.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull()

        if (amount == null) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        //storing the users input (from the edit texts)
        //takes it to the database to be saved
        val expense = Expense(
            category = category,
            amount = amount,
            date = date,
            description = description,
            photoUrl = selectedPhotoUri
        )

        lifecycleScope.launch {
            db.expenseDao().insertExpense(expense)

            runOnUiThread {
                Toast.makeText(this@Expenses, "Expense saved successfully", Toast.LENGTH_SHORT)
                    .show()

                etExpCtgry.text.clear()
                etExpAmnt.text.clear()
                etExpD8.text.clear()
                etExpDescrip.text.clear()
                selectedPhotoUri = null

            }
        }


    }


    private fun SaveGoals() {
        val minText = etExpMinAmnt.text.toString().trim()
        val maxText = etExpMaxAmnt.text.toString().trim()

        if (minText.isEmpty() || maxText.isEmpty()) {
            Toast.makeText(this, "Please enter both minimum and maximum goals", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val minGoal = minText.toDoubleOrNull()
        val maxGoal = maxText.toDoubleOrNull()

        if (minGoal == null || maxGoal == null) {
            Toast.makeText(this, "Please enter valid amounts", Toast.LENGTH_SHORT).show()
            return
        }

        if (minGoal > maxGoal) {
            Toast.makeText(
                this, "Minimum goal cannot be greater than maximum goal", Toast.LENGTH_SHORT
            ).show()
        }

        lifecycleScope.launch {
            val existingGoal = db.monthlyGoalDao().getGoal()

            if (existingGoal == null) {
                val newGoal = MonthlyGoal(
                    minGoal = minGoal,
                    maxGoal = maxGoal
                )
                db.monthlyGoalDao().insertGoal(newGoal)
            } else {
                    val updatedGoal = existingGoal.copy(
                    minGoal = minGoal,
                    maxGoal = maxGoal
                )

                db.monthlyGoalDao().updateGoal(updatedGoal)
            }

            runOnUiThread {
                Toast.makeText(this@Expenses, "Goals saved successfully", Toast.LENGTH_SHORT).show()
                etExpMinAmnt.text.clear()
                etExpMaxAmnt.text.clear()
            }
        }
    }


    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)


        val datePickerDialog = DatePickerDialog(
            this, { _, selectedYear, selectedMonth, selectedDay ->
                val formattedMonth = String.format("%02d", selectedMonth + 1)
                val formattedDay = String.format("%02d", selectedDay)

                val selectedDate = "$selectedYear-$formattedMonth-$formattedDay"
                etExpD8.setText(selectedDate)

            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }
}

