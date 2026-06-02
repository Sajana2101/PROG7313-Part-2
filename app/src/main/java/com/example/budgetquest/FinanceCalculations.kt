package com.example.budgetquest

object FinanceCalculations {

    fun calculateProgress(
        currentAmount: Double,
        targetAmount: Double
    ): Int {
        if (targetAmount <= 0) {
            return 0
        }

        // Keeps progress bar values within its valid 0 to 100 range.
        return ((currentAmount / targetAmount) * 100)
            .toInt()
            .coerceIn(0, 100)
    }

    fun calculateRemainingDebt(
        totalAmount: Double,
        totalPaid: Double
    ): Double {
        // Prevents an overpayment from displaying a negative remaining balance.
        return (totalAmount - totalPaid).coerceAtLeast(0.0)
    }

    fun canRecordDebtPayment(
        totalAmount: Double,
        totalPaid: Double,
        paymentAmount: Double
    ): Boolean {
        if (paymentAmount <= 0) {
            return false
        }

        val remainingBalance = calculateRemainingDebt(
            totalAmount = totalAmount,
            totalPaid = totalPaid
        )

        return remainingBalance > 0 && paymentAmount <= remainingBalance
    }

    fun isSavingsGoalReached(
        savedAmount: Double,
        targetAmount: Double
    ): Boolean {
        return targetAmount > 0 && savedAmount >= targetAmount
    }
}

