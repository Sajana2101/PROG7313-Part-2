package com.example.budgetquest

import com.example.budgetquest.firebase.FirebaseBadgeAward
import com.example.budgetquest.firebase.FirebaseBadgeTypes
import com.example.budgetquest.firebase.FirebaseDebt
import com.example.budgetquest.firebase.FirebaseExpense
import com.example.budgetquest.firebase.FirebaseMonthlyGoal
import com.example.budgetquest.firebase.FirebaseRepository
import com.example.budgetquest.firebase.FirebaseSavingsGoal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BadgeEvaluator {

    fun evaluateAndSaveAwards(
        userUid: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (userUid.isBlank()) {
            onError("User not found. Awards could not be evaluated.")
            return
        }

        val repository = FirebaseRepository()

        repository.getSavingsGoals(
            uid = userUid,
            onSuccess = { savingsGoals ->
                repository.getDebts(
                    uid = userUid,
                    onSuccess = { debts ->
                        repository.getMonthlyGoal(
                            uid = userUid,
                            onSuccess = { monthlyGoal ->
                                repository.getExpenses(
                                    uid = userUid,
                                    onSuccess = { expenses ->
                                        val awards = buildEligibleAwards(
                                            savingsGoals = savingsGoals,
                                            debts = debts,
                                            monthlyGoal = monthlyGoal,
                                            expenses = expenses
                                        )

                                        saveAwardsToFirebase(
                                            repository = repository,
                                            userUid = userUid,
                                            awards = awards,
                                            index = 0,
                                            onComplete = onComplete,
                                            onError = onError
                                        )
                                    },
                                    onError = onError
                                )
                            },
                            onError = onError
                        )
                    },
                    onError = onError
                )
            },
            onError = onError
        )
    }

    private fun buildEligibleAwards(
        savingsGoals: List<FirebaseSavingsGoal>,
        debts: List<FirebaseDebt>,
        monthlyGoal: FirebaseMonthlyGoal?,
        expenses: List<FirebaseExpense>
    ): List<FirebaseBadgeAward> {
        val awards = mutableListOf<FirebaseBadgeAward>()

        evaluateSavingsBadges(
            savingsGoals = savingsGoals,
            expenses = expenses,
            awards = awards
        )

        evaluateDebtBadges(
            debts = debts,
            expenses = expenses,
            awards = awards
        )

        evaluateMonthlyBudgetBadges(
            monthlyGoal = monthlyGoal,
            expenses = expenses,
            awards = awards
        )

        evaluateWeeklyTrackerBadges(
            expenses = expenses,
            awards = awards
        )

        return awards
    }

    /*
        Savings Champion:
        Awarded once for each savings goal that reaches its target amount.
     */
    private fun evaluateSavingsBadges(
        savingsGoals: List<FirebaseSavingsGoal>,
        expenses: List<FirebaseExpense>,
        awards: MutableList<FirebaseBadgeAward>
    ) {
        savingsGoals.forEach { goal ->
            val amountSaved = expenses
                .filter { expense ->
                    expense.category.equals(
                        goal.expenseCategory,
                        ignoreCase = true
                    )
                }
                .sumOf { expense ->
                    expense.amount
                }

            if (goal.targetAmount > 0 && amountSaved >= goal.targetAmount) {
                awards.add(
                    FirebaseBadgeAward(
                        badgeType = FirebaseBadgeTypes.SAVINGS_CHAMPION,
                        awardReference = "savings_${goal.id}",
                        displayDetails =
                            "${goal.goalName} goal completed - ${formatMoney(amountSaved)} saved",
                        earnedDate = todayDate()
                    )
                )
            }
        }
    }

    /*
        Debt Free:
        Awarded once for each debt that is fully paid through expense entries.
     */
    private fun evaluateDebtBadges(
        debts: List<FirebaseDebt>,
        expenses: List<FirebaseExpense>,
        awards: MutableList<FirebaseBadgeAward>
    ) {
        debts.forEach { debt ->
            val totalPaid = expenses
                .filter { expense ->
                    expense.category.equals(
                        debt.expenseCategory,
                        ignoreCase = true
                    )
                }
                .sumOf { expense ->
                    expense.amount
                }

            if (debt.totalAmount > 0 && totalPaid >= debt.totalAmount) {
                awards.add(
                    FirebaseBadgeAward(
                        badgeType = FirebaseBadgeTypes.DEBT_FREE,
                        awardReference = "debt_${debt.id}",
                        displayDetails =
                            "${debt.debtName} cleared - ${formatMoney(debt.totalAmount)} repaid",
                        earnedDate = todayDate()
                    )
                )
            }
        }
    }

    /*
        Monthly budget awards are evaluated for completed months only.
        The current month is not awarded yet because the user may still
        add more expenses before the month ends.
     */
    private fun evaluateMonthlyBudgetBadges(
        monthlyGoal: FirebaseMonthlyGoal?,
        expenses: List<FirebaseExpense>,
        awards: MutableList<FirebaseBadgeAward>
    ) {
        val savedGoal = monthlyGoal ?: return

        if (savedGoal.maxGoal <= 0) {
            return
        }

        val currentMonth = monthKey(todayDate())

        val completedMonthExpenses = expenses
            .filter { expense ->
                isValidDate(expense.date) &&
                        monthKey(expense.date) < currentMonth
            }
            .groupBy { expense ->
                monthKey(expense.date)
            }

        completedMonthExpenses.forEach { (month, monthExpenses) ->
            val amountSpent = monthExpenses.sumOf { expense ->
                expense.amount
            }

            if (amountSpent <= 0) {
                return@forEach
            }

            val percentageUsed =
                (amountSpent / savedGoal.maxGoal) * 100

            if (amountSpent <= savedGoal.maxGoal) {
                awards.add(
                    FirebaseBadgeAward(
                        badgeType = FirebaseBadgeTypes.BUDGET_KEEPER,
                        awardReference = month,
                        displayDetails =
                            "${formatMonth(month)} - spent ${formatMoney(amountSpent)} of ${formatMoney(savedGoal.maxGoal)}",
                        earnedDate = todayDate()
                    )
                )
            }

            if (percentageUsed <= 75) {
                awards.add(
                    FirebaseBadgeAward(
                        badgeType = FirebaseBadgeTypes.SMART_SAVER,
                        awardReference = month,
                        displayDetails =
                            "${formatMonth(month)} - used ${percentageUsed.toInt()}% of budget",
                        earnedDate = todayDate()
                    )
                )
            }

            if (percentageUsed <= 50) {
                awards.add(
                    FirebaseBadgeAward(
                        badgeType = FirebaseBadgeTypes.SUPER_SAVER,
                        awardReference = month,
                        displayDetails =
                            "${formatMonth(month)} - used ${percentageUsed.toInt()}% of budget",
                        earnedDate = todayDate()
                    )
                )
            }
        }
    }

    /*
        Weekly Tracker:
        Awarded when the user logs at least one expense per day
        for seven consecutive calendar days.
     */
    private fun evaluateWeeklyTrackerBadges(
        expenses: List<FirebaseExpense>,
        awards: MutableList<FirebaseBadgeAward>
    ) {
        val today = todayDate()

        val uniqueExpenseDates = expenses
            .map { expense ->
                expense.date
            }
            .filter { expenseDate ->
                isValidDate(expenseDate) && expenseDate <= today
            }
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
                    addWeeklyAwardsForRun(
                        dates = currentRun,
                        awards = awards
                    )

                    currentRun = mutableListOf(expenseDate)
                }
            }
        }

        addWeeklyAwardsForRun(
            dates = currentRun,
            awards = awards
        )
    }

    private fun addWeeklyAwardsForRun(
        dates: List<String>,
        awards: MutableList<FirebaseBadgeAward>
    ) {
        var startIndex = 0

        while (startIndex + 7 <= dates.size) {
            val startDate = dates[startIndex]
            val endDate = dates[startIndex + 6]

            awards.add(
                FirebaseBadgeAward(
                    badgeType = FirebaseBadgeTypes.WEEKLY_TRACKER,
                    awardReference = "${startDate}_$endDate",
                    displayDetails =
                        "${formatFullDate(startDate)} - ${formatFullDate(endDate)}",
                    earnedDate = todayDate()
                )
            )

            startIndex += 7
        }
    }

    /*
        Saves awards one at a time. FirebaseRepository creates a stable award ID,
        so an already-earned badge is updated instead of being duplicated.
     */
    private fun saveAwardsToFirebase(
        repository: FirebaseRepository,
        userUid: String,
        awards: List<FirebaseBadgeAward>,
        index: Int,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (index >= awards.size) {
            onComplete()
            return
        }

        repository.saveBadgeAward(
            uid = userUid,
            award = awards[index],
            onSuccess = {
                saveAwardsToFirebase(
                    repository = repository,
                    userUid = userUid,
                    awards = awards,
                    index = index + 1,
                    onComplete = onComplete,
                    onError = onError
                )
            },
            onError = onError
        )
    }

    private fun daysBetween(
        firstDate: String,
        secondDate: String
    ): Long {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
        formatter.isLenient = false

        val first = formatter.parse(firstDate) ?: return 0
        val second = formatter.parse(secondDate) ?: return 0

        val millisecondsInDay = 24 * 60 * 60 * 1000L

        return (second.time - first.time) / millisecondsInDay
    }

    private fun isValidDate(date: String): Boolean {
        return try {
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
            formatter.isLenient = false
            formatter.parse(date) != null
        } catch (_: Exception) {
            false
        }
    }

    private fun monthKey(date: String): String {
        return date.take(7)
    }

    private fun formatMonth(month: String): String {
        return try {
            val inputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
            inputFormatter.isLenient = false

            val outputFormatter = SimpleDateFormat("MMMM yyyy", Locale.UK)

            val parsedDate = inputFormatter.parse("$month-01")
            outputFormatter.format(parsedDate ?: Date())
        } catch (_: Exception) {
            month
        }
    }

    private fun formatFullDate(date: String): String {
        return try {
            val inputFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
            inputFormatter.isLenient = false

            val outputFormatter = SimpleDateFormat("dd MMM yyyy", Locale.UK)

            val parsedDate = inputFormatter.parse(date)
            outputFormatter.format(parsedDate ?: Date())
        } catch (_: Exception) {
            date
        }
    }

    private fun todayDate(): String {
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.UK
        ).format(Date())
    }

    private fun formatMoney(amount: Double): String {
        return String.format(
            Locale.US,
            "R%.2f",
            amount
        )
    }
}