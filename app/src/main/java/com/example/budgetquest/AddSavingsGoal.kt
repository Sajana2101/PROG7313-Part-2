package com.example.budgetquest

import Data.Database.AppDatabase
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgetquest.data.Category
import com.example.budgetquest.data.SavingsGoal
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddSavingsGoal : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var edtSavingsName: EditText
    private lateinit var edtSavingsTarget: EditText
    private lateinit var btnSaveSavingsGoal: Button
    private lateinit var tvSavingsFormTitle: TextView

    private var userId: Int = -1
    private var goalId: Int = -1
    private var existingGoal: SavingsGoal? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_savings_goal)

        userId = intent.getIntExtra("userId", -1)
        goalId = intent.getIntExtra("goalId", -1)

        if (userId == -1) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        db = AppDatabase.getDatabase(this)

        edtSavingsName = findViewById(R.id.edtSavingsName)
        edtSavingsTarget = findViewById(R.id.edtSavingsTarget)
        btnSaveSavingsGoal = findViewById(R.id.btnSaveSavingsGoal)
        tvSavingsFormTitle = findViewById(R.id.tvSavingsFormTitle)

        findViewById<TextView>(R.id.btnBackSavings).setOnClickListener {
            finish()
        }

        if (goalId != -1) {
            tvSavingsFormTitle.text = "Edit Savings Goal"
            btnSaveSavingsGoal.text = "Update Savings Goal"
            loadExistingGoal()
        }

        btnSaveSavingsGoal.setOnClickListener {
            saveSavingsGoal()
        }
    }

    private fun loadExistingGoal() {
        lifecycleScope.launch {
            existingGoal = db.savingsGoalDao().getSavingsGoalById(goalId)

            existingGoal?.let { goal ->
                edtSavingsName.setText(goal.goalName)
                edtSavingsTarget.setText(goal.targetAmount.toString())
            }
        }
    }

    private fun saveSavingsGoal() {
        val goalName = edtSavingsName.text.toString().trim()
        val targetAmount = edtSavingsTarget.text.toString().trim().toDoubleOrNull()

        if (goalName.isEmpty()) {
            Toast.makeText(this, "Please enter a savings goal name", Toast.LENGTH_SHORT).show()
            return
        }

        if (targetAmount == null || targetAmount <= 0) {
            Toast.makeText(this, "Please enter a positive target amount", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val goalBeingEdited = existingGoal

            if (goalBeingEdited == null) {
                val categoryName = "Savings - $goalName"

                val newGoal = SavingsGoal(
                    userId = userId,
                    goalName = goalName,
                    targetAmount = targetAmount,
                    expenseCategory = categoryName,
                    createdDate = todayDate()
                )

                val existingCategory = db.categoryDao()
                    .getCategoryByNameAndUser(categoryName, userId)

                if (existingCategory == null) {
                    val savingsCategory = Category(
                        userId = userId,
                        name = categoryName,
                        monthlyLimit = targetAmount
                    )

                    db.categoryDao().insertCategory(savingsCategory)
                } else {
                    db.categoryDao().updateCategory(
                        existingCategory.copy(monthlyLimit = targetAmount)
                    )
                }

                db.savingsGoalDao().insertSavingsGoal(newGoal)

            } else {
                val updatedGoal = goalBeingEdited.copy(
                    goalName = goalName,
                    targetAmount = targetAmount
                )

                db.savingsGoalDao().updateSavingsGoal(updatedGoal)

                val linkedCategory = db.categoryDao()
                    .getCategoryByNameAndUser(goalBeingEdited.expenseCategory, userId)

                if (linkedCategory != null) {
                    db.categoryDao().updateCategory(
                        linkedCategory.copy(monthlyLimit = targetAmount)
                    )
                }
            }

            Toast.makeText(
                this@AddSavingsGoal,
                "Savings goal saved. Log contributions as an expense.",
                Toast.LENGTH_LONG
            ).show()

            finish()
        }
    }

    private fun todayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(Date())
    }
}