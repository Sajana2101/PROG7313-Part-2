package com.example.budgetquest.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val category: String,
    val amount: Double,
    val date: String,
    val startTime: String,
    val endTime: String,
    val description: String,
    val photoUrl: String? = null
)