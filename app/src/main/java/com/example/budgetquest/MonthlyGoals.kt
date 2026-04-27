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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_goals)

        db = AppDatabase.getDatabase(this)

        edtMinGoal = findViewById(R.id.edtMinGoal)
        edtMaxGoal = findViewById(R.id.edtMaxGoal)
        btnSaveGoalChanges = findViewById(R.id.btnSaveGoalChanges)

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
            startActivity(Intent(this, Home::class.java))
        }

        navCategories.setOnClickListener {
            startActivity(Intent(this, Categories::class.java))
        }

        navAddExpense.setOnClickListener {
            startActivity(Intent(this, Expenses::class.java))
        }

        navGoals.setOnClickListener {
            Toast.makeText(this, "You are already on the Monthly Goals screen", Toast.LENGTH_SHORT).show()
        }

        navProfile.setOnClickListener {
            Toast.makeText(this, "Profile screen will be added soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveGoals() {
        val minText = edtMinGoal.text.toString().trim()
        val maxText = edtMaxGoal.text.toString().trim()

        if (minText.isEmpty() || maxText.isEmpty()) {
            Toast.makeText(this, "Please enter both minimum and maximum goals", Toast.LENGTH_SHORT).show()
            return
        }

        val minGoal = minText.toDoubleOrNull()
        val maxGoal = maxText.toDoubleOrNull()

        if (minGoal == null || maxGoal == null) {
            Toast.makeText(this, "Please enter valid goal amounts", Toast.LENGTH_SHORT).show()
            return
        }

        if (minGoal > maxGoal) {
            Toast.makeText(this, "Minimum goal cannot be greater than maximum goal", Toast.LENGTH_SHORT).show()
            return
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
                Toast.makeText(this@MonthlyGoals, "Goals saved successfully", Toast.LENGTH_SHORT).show()
                edtMinGoal.text.clear()
                edtMaxGoal.text.clear()
            }
        }
    }
}