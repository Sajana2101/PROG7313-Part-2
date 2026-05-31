package com.example.budgetquest.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "badge_awards",
    indices = [
        Index(
            value = ["userId", "badgeType", "awardReference"],
            unique = true
        )
    ]
)
data class BadgeAward(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val badgeType: String,

    // Prevents the same specific award from being added more than once.
    // Example: 2026-04, savings_2, debt_1, or 2026-05-01_2026-05-07.
    val awardReference: String,

    // The text displayed when the user clicks the badge.
    val displayDetails: String,

    // Date the achievement was evaluated and stored.
    val earnedDate: String
) {
    companion object {
        const val BUDGET_KEEPER = "BUDGET_KEEPER"
        const val SMART_SAVER = "SMART_SAVER"
        const val SUPER_SAVER = "SUPER_SAVER"
        const val WEEKLY_TRACKER = "WEEKLY_TRACKER"
        const val SAVINGS_CHAMPION = "SAVINGS_CHAMPION"
        const val DEBT_FREE = "DEBT_FREE"
    }
}