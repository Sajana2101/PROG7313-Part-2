package com.example.budgetquest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val debtName: String,
    val totalAmount: Double,
    val dueDate: String,

    // Category used when the user logs a debt payment as an expense.
    val expenseCategory: String
)