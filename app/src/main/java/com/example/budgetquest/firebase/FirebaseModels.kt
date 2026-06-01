package com.example.budgetquest.firebase

data class FirebaseUserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = ""
)

data class FirebaseCategory(
    val id: String = "",
    val name: String = "",
    val monthlyLimit: Double = 0.0
)

data class FirebaseExpense(
    val id: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val description: String = "",
    val photoUrl: String? = null
)

data class FirebaseMonthlyGoal(
    val minGoal: Double = 0.0,
    val maxGoal: Double = 0.0
)

data class FirebaseSavingsGoal(
    val id: String = "",
    val goalName: String = "",
    val targetAmount: Double = 0.0,
    val expenseCategory: String = "",
    val createdDate: String = ""
)

data class FirebaseDebt(
    val id: String = "",
    val debtName: String = "",
    val totalAmount: Double = 0.0,
    val dueDate: String = "",
    val expenseCategory: String = ""
)

data class FirebaseBadgeAward(
    val id: String = "",
    val badgeType: String = "",
    val awardReference: String = "",
    val displayDetails: String = "",
    val earnedDate: String = ""
)

object FirebaseBadgeTypes {
    const val BUDGET_KEEPER = "BUDGET_KEEPER"
    const val SMART_SAVER = "SMART_SAVER"
    const val SUPER_SAVER = "SUPER_SAVER"
    const val WEEKLY_TRACKER = "WEEKLY_TRACKER"
    const val SAVINGS_CHAMPION = "SAVINGS_CHAMPION"
    const val DEBT_FREE = "DEBT_FREE"
}