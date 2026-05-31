package com.example.budgetquest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val goalName: String,
    val targetAmount: Double,

    // This category name is used when logging saved money as an expense.
    // It remains unchanged even if the goal display name is edited.
    val expenseCategory: String,

    val createdDate: String
)