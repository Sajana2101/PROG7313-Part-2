package com.example.budgetquest

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.budgetquest.firebase.FirebaseCategory
import com.example.budgetquest.firebase.FirebaseRepository
import com.example.budgetquest.firebase.FirebaseSavingsGoal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("SetTextI18n")
class AddSavingsGoal : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository

    private lateinit var edtSavingsName: EditText
    private lateinit var edtSavingsTarget: EditText
    private lateinit var btnSaveSavingsGoal: Button
    private lateinit var tvSavingsFormTitle: TextView

    private var userUid: String = ""
    private var goalId: String = ""
    private var existingGoal: FirebaseSavingsGoal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_savings_goal)

        repository = FirebaseRepository()

        userUid = intent.getStringExtra("userUid")
            ?: repository.getCurrentUserId().orEmpty()

        goalId = intent.getStringExtra("goalId").orEmpty()

        if (userUid.isBlank()) {
            Toast.makeText(
                this,
                "User not found. Please log in again.",
                Toast.LENGTH_SHORT
            ).show()

            openLoginPage()
            return
        }

        edtSavingsName = findViewById(R.id.edtSavingsName)
        edtSavingsTarget = findViewById(R.id.edtSavingsTarget)
        btnSaveSavingsGoal = findViewById(R.id.btnSaveSavingsGoal)
        tvSavingsFormTitle = findViewById(R.id.tvSavingsFormTitle)

        findViewById<TextView>(R.id.btnBackSavings).setOnClickListener {
            finish()
        }

        if (goalId.isNotBlank()) {
            tvSavingsFormTitle.text = "Edit Savings Goal"
            btnSaveSavingsGoal.text = "Update Savings Goal"
            loadExistingGoal()
        }

        btnSaveSavingsGoal.setOnClickListener {
            saveSavingsGoal()
        }
    }

    private fun loadExistingGoal() {
        repository.getSavingsGoalById(
            uid = userUid,
            goalId = goalId,
            onSuccess = { goal ->
                if (goal == null) {
                    Toast.makeText(
                        this,
                        "Savings goal not found.",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                    return@getSavingsGoalById
                }

                existingGoal = goal
                edtSavingsName.setText(goal.goalName)
                edtSavingsTarget.setText(goal.targetAmount.toString())
            },
            onError = { errorMessage ->
                showError(errorMessage)
            }
        )
    }

    private fun saveSavingsGoal() {
        val goalName = edtSavingsName.text.toString().trim()
        val targetAmount = edtSavingsTarget.text
            .toString()
            .trim()
            .toDoubleOrNull()

        if (goalName.isEmpty()) {
            Toast.makeText(
                this,
                "Please enter a savings goal name.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (targetAmount == null || targetAmount <= 0) {
            Toast.makeText(
                this,
                "Please enter a positive target amount.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        btnSaveSavingsGoal.isEnabled = false

        val goalBeingEdited = existingGoal

        if (goalBeingEdited == null) {
            createNewSavingsGoal(
                goalName = goalName,
                targetAmount = targetAmount
            )
        } else {
            updateExistingSavingsGoal(
                currentGoal = goalBeingEdited,
                goalName = goalName,
                targetAmount = targetAmount
            )
        }
    }

    private fun createNewSavingsGoal(
        goalName: String,
        targetAmount: Double
    ) {
        val categoryName = "Savings - $goalName"

        repository.getCategoryByName(
            uid = userUid,
            categoryName = categoryName,
            onSuccess = { existingCategory ->
                val categoryToSave = existingCategory?.copy(
                    monthlyLimit = targetAmount
                ) ?: FirebaseCategory(
                    name = categoryName,
                    monthlyLimit = targetAmount
                )

                repository.saveCategory(
                    uid = userUid,
                    category = categoryToSave,
                    onSuccess = {
                        val newGoal = FirebaseSavingsGoal(
                            goalName = goalName,
                            targetAmount = targetAmount,
                            expenseCategory = categoryName,
                            createdDate = todayDate()
                        )

                        saveGoalToFirebase(newGoal)
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

    private fun updateExistingSavingsGoal(
        currentGoal: FirebaseSavingsGoal,
        goalName: String,
        targetAmount: Double
    ) {
        val updatedGoal = currentGoal.copy(
            goalName = goalName,
            targetAmount = targetAmount
        )

        repository.getCategoryByName(
            uid = userUid,
            categoryName = currentGoal.expenseCategory,
            onSuccess = { existingCategory ->
                val categoryToSave = existingCategory?.copy(
                    monthlyLimit = targetAmount
                ) ?: FirebaseCategory(
                    name = currentGoal.expenseCategory,
                    monthlyLimit = targetAmount
                )

                repository.saveCategory(
                    uid = userUid,
                    category = categoryToSave,
                    onSuccess = {
                        saveGoalToFirebase(updatedGoal)
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

    private fun saveGoalToFirebase(goal: FirebaseSavingsGoal) {
        repository.saveSavingsGoal(
            uid = userUid,
            savingsGoal = goal,
            onSuccess = {
                btnSaveSavingsGoal.isEnabled = true

                Toast.makeText(
                    this,
                    "Savings goal saved. Log contributions as an expense.",
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
        btnSaveSavingsGoal.isEnabled = true
        showError(errorMessage)
    }

    private fun showError(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun todayDate(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.UK
        ).format(Date())
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