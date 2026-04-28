package com.example.budgetquest

import Data.Database.AppDatabase
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgetquest.data.MonthlyGoal
import kotlinx.coroutines.launch
import android.content.Intent
import android.widget.TextView

class MonthlyGoals : AppCompatActivity() {

    private lateinit var edtMinGoal: EditText
    private lateinit var edtMaxGoal: EditText
    private lateinit var btnSaveGoalChanges: Button

    private lateinit var db: AppDatabase

    // stores the currently logged in user
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_goals)

        // gets logged in user id from previous screen
        userId = intent.getIntExtra("userId", -1)

        // if no user id is found send them back to login
        if (userId == -1) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        // gets db instance
        db = AppDatabase.getDatabase(this)

        edtMinGoal = findViewById(R.id.edtMinGoal)
        edtMaxGoal = findViewById(R.id.edtMaxGoal)
        btnSaveGoalChanges = findViewById(R.id.btnSaveGoalChanges)

        // saves the monthly goal when button is clicked
        btnSaveGoalChanges.setOnClickListener {
            saveGoals()
        }

        // bottom nav buttons
        val navHome = findViewById<TextView>(R.id.navHome)
        val navCategories = findViewById<TextView>(R.id.navCategories)
        val navAddExpense = findViewById<TextView>(R.id.navAddExpense)
        val navGoals = findViewById<TextView>(R.id.navGoals)
        val navProfile = findViewById<TextView>(R.id.navProfile)

        navHome.setOnClickListener {
            val intent = Intent(this, Home::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        navCategories.setOnClickListener {
            val intent = Intent(this, Categories::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        navAddExpense.setOnClickListener {
            val intent = Intent(this, Expenses::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        navGoals.setOnClickListener {
            Toast.makeText(this, "You are already on the Monthly Goals screen", Toast.LENGTH_SHORT).show()
        }

        navProfile.setOnClickListener {
            Toast.makeText(this, "Profile screen will be added soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveGoals() {
        // gets the values from the text boxes
        val minText = edtMinGoal.text.toString().trim()
        val maxText = edtMaxGoal.text.toString().trim()

        // checks if both fields are filled in
        if (minText.isEmpty() || maxText.isEmpty()) {
            Toast.makeText(this, "Please enter both minimum and maximum goals", Toast.LENGTH_SHORT).show()
            return
        }

        val minGoal = minText.toDoubleOrNull()
        val maxGoal = maxText.toDoubleOrNull()

        // checks if the values are valid numbers
        if (minGoal == null || maxGoal == null) {
            Toast.makeText(this, "Please enter valid goal amounts", Toast.LENGTH_SHORT).show()
            return
        }

        // makes sure minimum is not higher than maximum
        if (minGoal > maxGoal) {
            Toast.makeText(this, "Minimum goal cannot be greater than maximum goal", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // checks if a goal already exists for the logged in user
            val existingGoal = db.monthlyGoalDao().getGoalByUser(userId)

            if (existingGoal == null) {
                // creates a new monthly goal
                val newGoal = MonthlyGoal(
                    userId = userId,
                    minGoal = minGoal,
                    maxGoal = maxGoal
                )
                db.monthlyGoalDao().insertGoal(newGoal)
            } else {
                // updates the existing monthly goal
                val updatedGoal = existingGoal.copy(
                    userId = userId,
                    minGoal = minGoal,
                    maxGoal = maxGoal
                )
                db.monthlyGoalDao().updateGoal(updatedGoal)
            }

            runOnUiThread {
                Toast.makeText(this@MonthlyGoals, "Goals saved successfully", Toast.LENGTH_SHORT).show()

                // clears fields after saving
                edtMinGoal.text.clear()
                edtMaxGoal.text.clear()
            }
        }
    }
}