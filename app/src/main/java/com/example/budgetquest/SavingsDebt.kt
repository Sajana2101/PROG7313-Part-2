package com.example.budgetquest

import Data.Database.AppDatabase
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.budgetquest.data.Debt
import com.example.budgetquest.data.SavingsGoal
import kotlinx.coroutines.launch
import java.util.Locale

class SavingsDebt : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var savingsContainer: LinearLayout
    private lateinit var debtContainer: LinearLayout

    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings_debt)

        userId = intent.getIntExtra("userId", -1)

        if (userId == -1) {
            Toast.makeText(this, "User not found. Please login again.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        db = AppDatabase.getDatabase(this)

        savingsContainer = findViewById(R.id.savingsContainer)
        debtContainer = findViewById(R.id.debtContainer)

        findViewById<Button>(R.id.btnAddSavingsGoal).setOnClickListener {
            val intent = Intent(this, AddSavingsGoal::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnAddDebt).setOnClickListener {
            val intent = Intent(this, AddDebt::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        setupBottomNav()
    }

    override fun onResume() {
        super.onResume()

        if (::db.isInitialized) {
            loadSavingsAndDebts()
        }
    }

    private fun loadSavingsAndDebts() {
        lifecycleScope.launch {
            val savingsGoals = db.savingsGoalDao().getSavingsGoalsByUser(userId)
            val debts = db.debtDao().getDebtsByUser(userId)

            savingsContainer.removeAllViews()
            debtContainer.removeAllViews()

            if (savingsGoals.isEmpty()) {
                addEmptyMessage(savingsContainer, "No savings goals added yet.")
            } else {
                savingsGoals.forEach { goal ->
                    val contributionExpenses = db.expenseDao()
                        .getExpensesByCategoryAndUser(goal.expenseCategory, userId)

                    val savedAmount = contributionExpenses.sumOf { it.amount }

                    addSavingsCard(goal, savedAmount)
                }
            }

            if (debts.isEmpty()) {
                addEmptyMessage(debtContainer, "No active debts added yet.")
            } else {
                debts.forEach { debt ->
                    val paymentExpenses = db.expenseDao()
                        .getExpensesByCategoryAndUser(debt.expenseCategory, userId)

                    val totalPaid = paymentExpenses.sumOf { it.amount }

                    addDebtCard(debt, totalPaid)
                }
            }
        }
    }

    private fun addSavingsCard(goal: SavingsGoal, savedAmount: Double) {
        val card = createCard()

        val percentage = if (goal.targetAmount > 0) {
            ((savedAmount / goal.targetAmount) * 100).toInt()
        } else {
            0
        }

        val safeProgress = percentage.coerceIn(0, 100)
        val progressColor = getProgressColor(percentage)

        val title = createTitle(goal.goalName)
        val amount = createText(
            "${formatMoney(savedAmount)} saved of ${formatMoney(goal.targetAmount)}"
        )

        val categoryText = createText(
            "Log contributions under: ${goal.expenseCategory}"
        )

        val statusText = when {
            savedAmount >= goal.targetAmount -> "Goal reached!"
            percentage >= 80 -> "Almost there!"
            percentage >= 40 -> "Good progress"
            else -> "Keep saving"
        }

        val status = createStatusText("$percentage% complete - $statusText", progressColor)
        val progressBar = createProgressBar(safeProgress, progressColor)

        val actions = LinearLayout(this)
        actions.orientation = LinearLayout.HORIZONTAL

        val logExpenseButton = createActionButton("Log Expense")
        val optionsButton = createActionButton("Options")

        logExpenseButton.setOnClickListener {
            openExpenseScreen(goal.expenseCategory)
        }

        optionsButton.setOnClickListener {
            showSavingsOptions(goal)
        }

        actions.addView(logExpenseButton)
        actions.addView(optionsButton)

        card.addView(title)
        card.addView(amount)
        card.addView(categoryText)
        card.addView(status)
        card.addView(progressBar)
        card.addView(actions)

        savingsContainer.addView(card)
    }

    private fun addDebtCard(debt: Debt, totalPaid: Double) {
        val remainingBalance = (debt.totalAmount - totalPaid).coerceAtLeast(0.0)

        val percentagePaid = if (debt.totalAmount > 0) {
            ((totalPaid / debt.totalAmount) * 100).toInt()
        } else {
            0
        }

        val safeProgress = percentagePaid.coerceIn(0, 100)
        val progressColor = getProgressColor(percentagePaid)

        val card = createCard()

        val title = createTitle(
            if (remainingBalance <= 0) "✓ ${debt.debtName}" else debt.debtName
        )
        title.setTextColor(progressColor)

        val remaining = createText(
            "Remaining: ${formatMoney(remainingBalance)} of ${formatMoney(debt.totalAmount)}"
        )

        val dueDate = createText("Due date: ${debt.dueDate}")

        val categoryText = createText(
            "Log payments under: ${debt.expenseCategory}"
        )

        val statusText = if (remainingBalance <= 0) {
            "Fully paid"
        } else {
            "$percentagePaid% paid"
        }

        val status = createStatusText(statusText, progressColor)
        val progressBar = createProgressBar(safeProgress, progressColor)

        val actions = LinearLayout(this)
        actions.orientation = LinearLayout.HORIZONTAL

        val logExpenseButton = createActionButton("Log Payment")
        val optionsButton = createActionButton("Options")

        logExpenseButton.isEnabled = remainingBalance > 0

        logExpenseButton.setOnClickListener {
            openExpenseScreen(debt.expenseCategory)
        }

        optionsButton.setOnClickListener {
            showDebtOptions(debt)
        }

        actions.addView(logExpenseButton)
        actions.addView(optionsButton)

        card.addView(title)
        card.addView(remaining)
        card.addView(dueDate)
        card.addView(categoryText)
        card.addView(status)
        card.addView(progressBar)
        card.addView(actions)

        debtContainer.addView(card)
    }

    private fun openExpenseScreen(categoryName: String) {
        val intent = Intent(this, Expenses::class.java)
        intent.putExtra("userId", userId)
        intent.putExtra("preselectedCategory", categoryName)
        startActivity(intent)
    }

    private fun showSavingsOptions(goal: SavingsGoal) {
        val options = arrayOf("Edit Goal", "Delete Goal")

        AlertDialog.Builder(this)
            .setTitle(goal.goalName)
            .setItems(options) { _, selectedOption ->
                when (selectedOption) {
                    0 -> {
                        val intent = Intent(this, AddSavingsGoal::class.java)
                        intent.putExtra("userId", userId)
                        intent.putExtra("goalId", goal.id)
                        startActivity(intent)
                    }

                    1 -> confirmDeleteSavingsGoal(goal)
                }
            }
            .show()
    }

    private fun confirmDeleteSavingsGoal(goal: SavingsGoal) {
        AlertDialog.Builder(this)
            .setTitle("Delete Savings Goal")
            .setMessage(
                "Delete ${goal.goalName}? The category and logged expenses will remain in your expense history."
            )
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.savingsGoalDao().deleteSavingsGoal(goal)

                    Toast.makeText(
                        this@SavingsDebt,
                        "Savings goal deleted",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadSavingsAndDebts()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDebtOptions(debt: Debt) {
        val options = arrayOf("Edit Debt", "Delete Debt")

        AlertDialog.Builder(this)
            .setTitle(debt.debtName)
            .setItems(options) { _, selectedOption ->
                when (selectedOption) {
                    0 -> {
                        val intent = Intent(this, AddDebt::class.java)
                        intent.putExtra("userId", userId)
                        intent.putExtra("debtId", debt.id)
                        startActivity(intent)
                    }

                    1 -> confirmDeleteDebt(debt)
                }
            }
            .show()
    }

    private fun confirmDeleteDebt(debt: Debt) {
        AlertDialog.Builder(this)
            .setTitle("Delete Debt")
            .setMessage(
                "Delete ${debt.debtName}? The category and logged expense payments will remain in your expense history."
            )
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    db.debtDao().deleteDebt(debt)

                    Toast.makeText(
                        this@SavingsDebt,
                        "Debt deleted",
                        Toast.LENGTH_SHORT
                    ).show()

                    loadSavingsAndDebts()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createCard(): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(16), dp(16), dp(16), dp(16))
        card.setBackgroundResource(R.drawable.login_card_bg)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 0, 0, dp(14))
        card.layoutParams = params

        return card
    }

    private fun createTitle(text: String): TextView {
        val title = TextView(this)
        title.text = text
        title.textSize = 18f
        title.setTextColor(Color.parseColor("#263238"))
        title.setTypeface(null, Typeface.BOLD)
        return title
    }

    private fun createText(text: String): TextView {
        val textView = TextView(this)
        textView.text = text
        textView.textSize = 14f
        textView.setTextColor(Color.parseColor("#546E7A"))
        textView.setPadding(0, dp(4), 0, 0)
        return textView
    }

    private fun createStatusText(text: String, color: Int): TextView {
        val status = TextView(this)
        status.text = text
        status.textSize = 14f
        status.setTextColor(color)
        status.setTypeface(null, Typeface.BOLD)
        status.setPadding(0, dp(6), 0, dp(6))
        return status
    }

    private fun createProgressBar(progress: Int, color: Int): ProgressBar {
        val progressBar = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        )

        progressBar.max = 100
        progressBar.progress = progress
        progressBar.progressTintList = ColorStateList.valueOf(color)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(14)
        )

        params.setMargins(0, dp(4), 0, dp(12))
        progressBar.layoutParams = params

        return progressBar
    }

    private fun createActionButton(text: String): Button {
        val button = Button(this)
        button.text = text
        button.textSize = 12f
        button.isAllCaps = false

        val params = LinearLayout.LayoutParams(
            0,
            dp(46),
            1f
        )

        params.setMargins(dp(4), 0, dp(4), 0)
        button.layoutParams = params

        return button
    }

    private fun addEmptyMessage(container: LinearLayout, message: String) {
        val emptyText = TextView(this)
        emptyText.text = message
        emptyText.textSize = 14f
        emptyText.setTextColor(Color.parseColor("#546E7A"))
        emptyText.setPadding(0, dp(4), 0, dp(16))
        container.addView(emptyText)
    }

    private fun getProgressColor(percentage: Int): Int {
        return when {
            percentage >= 80 -> Color.parseColor("#43A047")
            percentage >= 40 -> Color.parseColor("#F9A825")
            else -> Color.parseColor("#E53935")
        }
    }

    private fun setupBottomNav() {
        findViewById<TextView>(R.id.navHome).setOnClickListener {
            val intent = Intent(this, Home::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.navCategories).setOnClickListener {
            val intent = Intent(this, Categories::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.navAddExpense).setOnClickListener {
            val intent = Intent(this, Expenses::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.navGoals).setOnClickListener {
            val intent = Intent(this, MonthlyGoals::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.navSavingsDebt).setOnClickListener {
            Toast.makeText(
                this,
                "You are already on Savings & Debt",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<TextView>(R.id.navProfile).setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }
    }

    private fun formatMoney(amount: Double): String {
        return String.format(Locale.US, "R%.2f", amount)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}