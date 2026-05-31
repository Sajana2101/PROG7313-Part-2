package com.example.budgetquest

import Data.Database.AppDatabase
import com.example.budgetquest.data.BadgeAward
import com.example.budgetquest.data.Expense
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object BadgeEvaluator {

    suspend fun evaluateAndSaveAwards(
        db: AppDatabase,
        userId: Int
    ) {
        evaluateSavingsBadges(db, userId)
        evaluateDebtBadges(db, userId)
        evaluateMonthlyBudgetBadges(db, userId)
        evaluateWeeklyTrackerBadges(db, userId)
    }

    /*
        Savings Champion:
        Awarded once for each savings goal that reaches its target amount.
     */
    private suspend fun evaluateSavingsBadges(
        db: AppDatabase,
        userId: Int
    ) {
        val savingsGoals = db.savingsGoalDao().getSavingsGoalsByUser(userId)

        savingsGoals.forEach { goal ->
            val savingsExpenses = db.expenseDao()
                .getExpensesByCategoryAndUser(goal.expenseCategory, userId)

            val amountSaved = savingsExpenses.sumOf { it.amount }

            if (amountSaved >= goal.targetAmount) {
                val award = BadgeAward(
                    userId = userId,
                    badgeType = BadgeAward.SAVINGS_CHAMPION,
                    awardReference = "savings_${goal.id}",
                    displayDetails = "${goal.goalName} goal completed - ${formatMoney(amountSaved)} saved",
                    earnedDate = todayDate()
                )

                db.badgeAwardDao().insertAward(award)
            }
        }
    }

    /*
        Debt Free:
        Awarded once for each debt that has been fully paid through expense entries.
     */
    private suspend fun evaluateDebtBadges(
        db: AppDatabase,
        userId: Int
    ) {
        val debts = db.debtDao().getDebtsByUser(userId)

        debts.forEach { debt ->
            val debtExpenses = db.expenseDao()
                .getExpensesByCategoryAndUser(debt.expenseCategory, userId)

            val totalPaid = debtExpenses.sumOf { it.amount }

            if (totalPaid >= debt.totalAmount) {
                val award = BadgeAward(
                    userId = userId,
                    badgeType = BadgeAward.DEBT_FREE,
                    awardReference = "debt_${debt.id}",
                    displayDetails = "${debt.debtName} cleared - ${formatMoney(debt.totalAmount)} repaid",
                    earnedDate = todayDate()
                )

                db.badgeAwardDao().insertAward(award)
            }
        }
    }

    /*
        Monthly awards:
        Only completed months are evaluated. The current month is not evaluated yet
        because the user may still add expenses before the month ends.

        Important:
        MonthlyGoal currently stores one current maximum goal rather than a historical
        goal for each month, so historical badges are checked against the saved
        maximum monthly goal available in the app.
     */
    private suspend fun evaluateMonthlyBudgetBadges(
        db: AppDatabase,
        userId: Int
    ) {
        val monthlyGoal = db.monthlyGoalDao().getGoalByUser(userId) ?: return

        if (monthlyGoal.maxGoal <= 0) {
            return
        }

        val allExpenses = db.expenseDao().getExpensesByUser(userId)
        val currentMonth = monthKey(todayDate())

        val completedMonthExpenses = allExpenses
            .filter { isValidDate(it.date) && monthKey(it.date) < currentMonth }
            .groupBy { monthKey(it.date) }

        completedMonthExpenses.forEach { (month, expenses) ->
            val amountSpent = expenses.sumOf { it.amount }

            // Requires financial activity in that month before awarding badges.
            if (amountSpent <= 0) {
                return@forEach
            }

            val percentageUsed = (amountSpent / monthlyGoal.maxGoal) * 100

            if (amountSpent <= monthlyGoal.maxGoal) {
                db.badgeAwardDao().insertAward(
                    BadgeAward(
                        userId = userId,
                        badgeType = BadgeAward.BUDGET_KEEPER,
                        awardReference = month,
                        displayDetails = "${formatMonth(month)} - spent ${formatMoney(amountSpent)} of ${formatMoney(monthlyGoal.maxGoal)}",
                        earnedDate = todayDate()
                    )
                )
            }

            if (percentageUsed <= 75) {
                db.badgeAwardDao().insertAward(
                    BadgeAward(
                        userId = userId,
                        badgeType = BadgeAward.SMART_SAVER,
                        awardReference = month,
                        displayDetails = "${formatMonth(month)} - used ${percentageUsed.toInt()}% of budget",
                        earnedDate = todayDate()
                    )
                )
            }

            if (percentageUsed <= 50) {
                db.badgeAwardDao().insertAward(
                    BadgeAward(
                        userId = userId,
                        badgeType = BadgeAward.SUPER_SAVER,
                        awardReference = month,
                        displayDetails = "${formatMonth(month)} - used ${percentageUsed.toInt()}% of budget",
                        earnedDate = todayDate()
                    )
                )
            }
        }
    }

    /*
        Weekly Tracker:
        Awarded when the user has logged at least one expense on every day
        for seven consecutive calendar days.

        A run of 14 consecutive days awards two weekly badges.
        A run of 8 days awards one weekly badge.
     */
    private suspend fun evaluateWeeklyTrackerBadges(
        db: AppDatabase,
        userId: Int
    ) {
        val allExpenses = db.expenseDao().getExpensesByUser(userId)
        val today = todayDate()

        val uniqueExpenseDates = allExpenses
            .map { it.date }
            .filter { isValidDate(it) && it <= today }
            .distinct()
            .sorted()

        if (uniqueExpenseDates.size < 7) {
            return
        }

        var currentRun = mutableListOf<String>()

        uniqueExpenseDates.forEach { expenseDate ->
            if (currentRun.isEmpty()) {
                currentRun.add(expenseDate)
            } else {
                val previousDate = currentRun.last()

                if (daysBetween(previousDate, expenseDate) == 1L) {
                    currentRun.add(expenseDate)
                } else {
                    saveCompletedWeeklyRuns(db, userId, currentRun)
                    currentRun = mutableListOf(expenseDate)
                }
            }
        }

        saveCompletedWeeklyRuns(db, userId, currentRun)
    }

    private suspend fun saveCompletedWeeklyRuns(
        db: AppDatabase,
        userId: Int,
        dates: List<String>
    ) {
        var startIndex = 0

        while (startIndex + 7 <= dates.size) {
            val startDate = dates[startIndex]
            val endDate = dates[startIndex + 6]

            db.badgeAwardDao().insertAward(
                BadgeAward(
                    userId = userId,
                    badgeType = BadgeAward.WEEKLY_TRACKER,
                    awardReference = "${startDate}_$endDate",
                    displayDetails = "${formatFullDate(startDate)} - ${formatFullDate(endDate)}",
                    earnedDate = todayDate()
                )
            )

            // Awards one badge for each complete non-overlapping seven-day streak.
            startIndex += 7
        }
    }

    private fun daysBetween(
        firstDate: String,
        secondDate: String
    ): Long {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
        formatter.isLenient = false

        val first = formatter.parse(firstDate) ?: return 0
        val second = formatter.parse(secondDate) ?: return 0

        val dayLength = 24 * 60 * 60 * 1000L
        return (second.time - first.time) / dayLength
    }

    private fun isValidDate(date: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
            formatter.isLenient = false
            formatter.parse(date)
            true
        } catch (exception: Exception) {
            false
        }
    }

    private fun monthKey(date: String): String {
        return date.take(7)
    }

    private fun formatMonth(month: String): String {
        return try {
            val inputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
            val outputFormatter = SimpleDateFormat("MMMM yyyy", Locale.UK)

            val parsedDate = inputFormatter.parse("$month-01")
            outputFormatter.format(parsedDate ?: Date())
        } catch (exception: Exception) {
            month
        }
    }

    private fun formatFullDate(date: String): String {
        return try {
            val inputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
            val outputFormatter = SimpleDateFormat("dd MMM yyyy", Locale.UK)

            val parsedDate = inputFormatter.parse(date)
            outputFormatter.format(parsedDate ?: Date())
        } catch (exception: Exception) {
            date
        }
    }

    private fun todayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.UK).format(Date())
    }

    private fun formatMoney(amount: Double): String {
        return String.format(Locale.US, "R%.2f", amount)
    }
}