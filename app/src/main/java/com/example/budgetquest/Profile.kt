package com.example.budgetquest

import Data.Database.AppDatabase
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgetquest.data.BadgeAward
import kotlinx.coroutines.launch

class Profile : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var btnLogout: Button
    private lateinit var badgesContainer: LinearLayout
    private lateinit var tvBadgeSummary: TextView

    private var userId: Int = -1

    private val badgeDefinitions = listOf(
        BadgeDefinition(
            badgeType = BadgeAward.BUDGET_KEEPER,
            icon = "🏅",
            title = "Budget Keeper",
            description = "Stayed within your monthly budget."
        ),
        BadgeDefinition(
            badgeType = BadgeAward.SMART_SAVER,
            icon = "🌟",
            title = "Smart Saver",
            description = "Used 75% or less of your monthly budget."
        ),
        BadgeDefinition(
            badgeType = BadgeAward.SUPER_SAVER,
            icon = "💎",
            title = "Super Saver",
            description = "Used 50% or less of your monthly budget."
        ),
        BadgeDefinition(
            badgeType = BadgeAward.WEEKLY_TRACKER,
            icon = "🔥",
            title = "Weekly Tracker",
            description = "Logged expenses every day for a full week."
        ),
        BadgeDefinition(
            badgeType = BadgeAward.SAVINGS_CHAMPION,
            icon = "🏆",
            title = "Savings Champion",
            description = "Completed a savings goal."
        ),
        BadgeDefinition(
            badgeType = BadgeAward.DEBT_FREE,
            icon = "⭐",
            title = "Debt Free",
            description = "Fully cleared a recorded debt."
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        userId = intent.getIntExtra("userId", -1)

        if (userId == -1) {
            Toast.makeText(
                this,
                "User not found. Please login again.",
                Toast.LENGTH_SHORT
            ).show()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        db = AppDatabase.getDatabase(this)

        btnLogout = findViewById(R.id.btnLogout)
        badgesContainer = findViewById(R.id.badgesContainer)
        tvBadgeSummary = findViewById(R.id.tvBadgeSummary)

        btnLogout.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()

        if (::db.isInitialized) {
            loadBadges()
        }
    }

    private fun loadBadges() {
        lifecycleScope.launch {

            BadgeEvaluator.evaluateAndSaveAwards(db, userId)

            val allAwards = db.badgeAwardDao().getAwardsByUser(userId)

            badgesContainer.removeAllViews()

            val totalAwards = allAwards.size
            val unlockedBadgeTypes = allAwards
                .map { it.badgeType }
                .distinct()
                .size

            tvBadgeSummary.text = if (totalAwards == 0) {
                "No badges earned yet. Complete goals and manage your budget to unlock awards."
            } else {
                "You have earned $totalAwards award(s) across $unlockedBadgeTypes badge type(s)."
            }

            badgeDefinitions.forEach { definition ->
                val awardsForType = allAwards.filter {
                    it.badgeType == definition.badgeType
                }

                addBadgeCard(definition, awardsForType)
            }
        }
    }

    private fun addBadgeCard(
        definition: BadgeDefinition,
        awards: List<BadgeAward>
    ) {
        val unlocked = awards.isNotEmpty()

        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(18), dp(16), dp(18), dp(16))
        card.setBackgroundResource(R.drawable.login_card_bg)

        val cardParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardParams.setMargins(0, 0, 0, dp(14))
        card.layoutParams = cardParams

        val headingRow = LinearLayout(this)
        headingRow.orientation = LinearLayout.HORIZONTAL

        val title = TextView(this)
        title.text = "${definition.icon}  ${definition.title}"
        title.textSize = 17f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(
            if (unlocked) Color.parseColor("#43A047")
            else Color.parseColor("#757575")
        )

        title.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val count = TextView(this)
        count.text = if (unlocked) "${awards.size} earned" else "Locked"
        count.textSize = 13f
        count.setTypeface(null, Typeface.BOLD)
        count.setTextColor(
            if (unlocked) Color.parseColor("#F9A825")
            else Color.parseColor("#757575")
        )

        headingRow.addView(title)
        headingRow.addView(count)

        val description = TextView(this)
        description.text = definition.description
        description.textSize = 13f
        description.setTextColor(Color.parseColor("#546E7A"))
        description.setPadding(0, dp(8), 0, dp(6))

        val actionText = TextView(this)
        actionText.text = if (unlocked) {
            "View awards ›"
        } else {
            "Complete this achievement to unlock"
        }
        actionText.textSize = 13f
        actionText.setTypeface(null, Typeface.BOLD)
        actionText.setTextColor(
            if (unlocked) Color.parseColor("#2196F3")
            else Color.parseColor("#9E9E9E")
        )

        card.addView(headingRow)
        card.addView(description)
        card.addView(actionText)

        card.setOnClickListener {
            if (unlocked) {
                showAwardHistory(definition, awards)
            } else {
                showLockedBadgeMessage(definition)
            }
        }

        badgesContainer.addView(card)
    }

    private fun showAwardHistory(
        definition: BadgeDefinition,
        awards: List<BadgeAward>
    ) {
        val historyText = awards.joinToString("\n\n") { award ->
            "• ${award.displayDetails}"
        }

        AlertDialog.Builder(this)
            .setTitle("${definition.icon} ${definition.title}")
            .setMessage(historyText)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showLockedBadgeMessage(definition: BadgeDefinition) {
        AlertDialog.Builder(this)
            .setTitle("${definition.icon} ${definition.title}")
            .setMessage(definition.description)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    data class BadgeDefinition(
        val badgeType: String,
        val icon: String,
        val title: String,
        val description: String
    )
}