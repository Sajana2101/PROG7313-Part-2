package com.example.budgetquest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetquest.firebase.FirebaseMonthlyGoal
import com.example.budgetquest.firebase.FirebaseRepository

class MonthlyGoals : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository

    private lateinit var edtMinGoal: EditText
    private lateinit var edtMaxGoal: EditText
    private lateinit var btnSaveGoalChanges: Button

    private var userUid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_monthly_goals)

        repository = FirebaseRepository()

        userUid = intent.getStringExtra("userUid")
            ?: repository.getCurrentUserId().orEmpty()

        if (userUid.isBlank()) {
            Toast.makeText(
                this,
                "User not found. Please log in again.",
                Toast.LENGTH_SHORT
            ).show()

            openLoginPage()
            return
        }

        edtMinGoal = findViewById(R.id.edtMinGoal)
        edtMaxGoal = findViewById(R.id.edtMaxGoal)
        btnSaveGoalChanges = findViewById(R.id.btnSaveGoalChanges)

        btnSaveGoalChanges.setOnClickListener {
            saveGoals()
        }

        NavigationHelper.setupBottomNavigation(
            activity = this,
            userUid = userUid,
            currentPage = "Goals"
        )

        loadExistingGoals()
    }

    private fun loadExistingGoals() {
        repository.getMonthlyGoal(
            uid = userUid,
            onSuccess = { existingGoal ->
                if (existingGoal != null) {
                    edtMinGoal.setText(existingGoal.minGoal.toString())
                    edtMaxGoal.setText(existingGoal.maxGoal.toString())
                }
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

    private fun saveGoals() {
        val minText = edtMinGoal.text.toString().trim()
        val maxText = edtMaxGoal.text.toString().trim()

        if (minText.isEmpty() || maxText.isEmpty()) {
            Toast.makeText(
                this,
                "Please enter both minimum and maximum goals.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val minGoal = minText.toDoubleOrNull()
        val maxGoal = maxText.toDoubleOrNull()

        if (minGoal == null || maxGoal == null) {
            Toast.makeText(
                this,
                "Please enter valid goal amounts.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (minGoal < 0 || maxGoal <= 0) {
            Toast.makeText(
                this,
                "Goal amounts must be positive.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (minGoal > maxGoal) {
            Toast.makeText(
                this,
                "Minimum goal cannot be greater than maximum goal.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val monthlyGoal = FirebaseMonthlyGoal(
            minGoal = minGoal,
            maxGoal = maxGoal
        )

        btnSaveGoalChanges.isEnabled = false

        repository.saveMonthlyGoal(
            uid = userUid,
            monthlyGoal = monthlyGoal,
            onSuccess = {
                btnSaveGoalChanges.isEnabled = true

                Toast.makeText(
                    this,
                    "Goals saved successfully.",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onError = { errorMessage ->
                btnSaveGoalChanges.isEnabled = true

                Toast.makeText(
                    this,
                    errorMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun openLoginPage() {
        repository.logout()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}