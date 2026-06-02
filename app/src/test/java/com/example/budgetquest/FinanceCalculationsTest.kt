package com.example.budgetquest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceCalculationsTest {

    @Test
    fun calculateProgress_whenSavingTowardsGoal_returnsCorrectPercentage() {
        val progress = FinanceCalculations.calculateProgress(
            currentAmount = 500.0,
            targetAmount = 5000.0
        )

        assertEquals(10, progress)
    }

    @Test
    fun calculateProgress_whenAmountExceedsTarget_capsProgressAtOneHundred() {
        val progress = FinanceCalculations.calculateProgress(
            currentAmount = 6000.0,
            targetAmount = 5000.0
        )

        assertEquals(100, progress)
    }

    @Test
    fun calculateProgress_whenTargetIsZero_returnsZero() {
        val progress = FinanceCalculations.calculateProgress(
            currentAmount = 500.0,
            targetAmount = 0.0
        )

        assertEquals(0, progress)
    }

    @Test
    fun calculateRemainingDebt_whenPaymentIsMade_returnsOutstandingBalance() {
        val remainingBalance = FinanceCalculations.calculateRemainingDebt(
            totalAmount = 3000.0,
            totalPaid = 300.0
        )

        assertEquals(2700.0, remainingBalance, 0.0)
    }

    @Test
    fun calculateRemainingDebt_whenDebtIsOverpaid_doesNotReturnNegativeValue() {
        val remainingBalance = FinanceCalculations.calculateRemainingDebt(
            totalAmount = 3000.0,
            totalPaid = 3500.0
        )

        assertEquals(0.0, remainingBalance, 0.0)
    }

    @Test
    fun canRecordDebtPayment_whenPaymentFitsRemainingBalance_returnsTrue() {
        val canRecordPayment = FinanceCalculations.canRecordDebtPayment(
            totalAmount = 3000.0,
            totalPaid = 1000.0,
            paymentAmount = 500.0
        )

        assertTrue(canRecordPayment)
    }

    @Test
    fun canRecordDebtPayment_whenPaymentExceedsRemainingBalance_returnsFalse() {
        val canRecordPayment = FinanceCalculations.canRecordDebtPayment(
            totalAmount = 3000.0,
            totalPaid = 2500.0,
            paymentAmount = 600.0
        )

        assertFalse(canRecordPayment)
    }

    @Test
    fun canRecordDebtPayment_whenDebtIsAlreadyPaid_returnsFalse() {
        val canRecordPayment = FinanceCalculations.canRecordDebtPayment(
            totalAmount = 3000.0,
            totalPaid = 3000.0,
            paymentAmount = 100.0
        )

        assertFalse(canRecordPayment)
    }

    @Test
    fun isSavingsGoalReached_whenSavedAmountEqualsTarget_returnsTrue() {
        val goalReached = FinanceCalculations.isSavingsGoalReached(
            savedAmount = 5000.0,
            targetAmount = 5000.0
        )

        assertTrue(goalReached)
    }

    @Test
    fun isSavingsGoalReached_whenSavedAmountIsBelowTarget_returnsFalse() {
        val goalReached = FinanceCalculations.isSavingsGoalReached(
            savedAmount = 4500.0,
            targetAmount = 5000.0
        )

        assertFalse(goalReached)
    }
}