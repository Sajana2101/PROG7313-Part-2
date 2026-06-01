package com.example.budgetquest

import android.annotation.SuppressLint
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
import com.example.budgetquest.firebase.FirebaseDebt
import com.example.budgetquest.firebase.FirebaseExpense
import com.example.budgetquest.firebase.FirebaseRepository
import com.example.budgetquest.firebase.FirebaseSavingsGoal
import java.util.Locale

@SuppressLint("SetTextI18n")
class SavingsDebt : AppCompatActivity() {

    private lateinit var repository: FirebaseRepository
    private lateinit var savingsContainer: LinearLayout
    private lateinit var debtContainer: LinearLayout

    private var userUid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_savings_debt)

        repository = FirebaseRepository()

        userUid = intent.getStringExtra("userUid")
            ?: repository.getCurrentUserId().orEmpty()

        if (userUid.isBlank()) {
            Toast.makeText(
                this,
                "User not found. Please log in again.",
                Toast.LENGTH_SHORT
            ).show()

            openLoginPage()
            return
        }

        savingsContainer = findViewById(R.id.savingsContainer)
        debtContainer = findViewById(R.id.debtContainer)

        findViewById<Button>(R.id.btnAddSavingsGoal).setOnClickListener {
            val intent = Intent(this, AddSavingsGoal::class.java)
            intent.putExtra("userUid", userUid)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnAddDebt).setOnClickListener {
            val intent = Intent(this, AddDebt::class.java)
            intent.putExtra("userUid", userUid)
            startActivity(intent)
        }

        NavigationHelper.setupBottomNavigation(
            activity = this,
            userUid = userUid,
            currentPage = "Savings"
        )
    }

    override fun onResume() {
        super.onResume()

        if (::repository.isInitialized && userUid.isNotBlank()) {
            loadSavingsAndDebts()
        }
    }

    private fun loadSavingsAndDebts() {
        repository.getSavingsGoals(
            uid = userUid,
            onSuccess = { savingsGoals ->
                repository.getDebts(
                    uid = userUid,
                    onSuccess = { debts ->
                        repository.getExpenses(
                            uid = userUid,
                            onSuccess = { expenses ->
                                displaySavingsAndDebts(
                                    savingsGoals = savingsGoals,
                                    debts = debts,
                                    expenses = expenses
                                )
                            },
                            onError = { errorMessage ->
                                showError(errorMessage)
                            }
                        )
                    },
                    onError = { errorMessage ->
                        showError(errorMessage)
                    }
                )
            },
            onError = { errorMessage ->
                showError(errorMessage)
            }
        )
    }

    private fun displaySavingsAndDebts(
        savingsGoals: List<FirebaseSavingsGoal>,
        debts: List<FirebaseDebt>,
        expenses: List<FirebaseExpense>
    ) {
        savingsContainer.removeAllViews()
        debtContainer.removeAllViews()

        if (savingsGoals.isEmpty()) {
            addEmptyMessage(
                container = savingsContainer,
                message = "No savings goals added yet."
            )
        } else {
            savingsGoals.forEach { goal ->
                val savedAmount = expenses
                    .filter {
                        it.category.equals(goal.expenseCategory, ignoreCase = true)
                    }
                    .sumOf { it.amount }

                addSavingsCard(
                    goal = goal,
                    savedAmount = savedAmount
                )
            }
        }

        if (debts.isEmpty()) {
            addEmptyMessage(
                container = debtContainer,
                message = "No active debts added yet."
            )
        } else {
            debts.forEach { debt ->
                val totalPaid = expenses
                    .filter {
                        it.category.equals(debt.expenseCategory, ignoreCase = true)
                    }
                    .sumOf { it.amount }

                addDebtCard(
                    debt = debt,
                    totalPaid = totalPaid
                )
            }
        }
    }

    private fun addSavingsCard(
        goal: FirebaseSavingsGoal,
        savedAmount: Double
    ) {
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

        val statusMessage = when {
            savedAmount >= goal.targetAmount -> "Goal reached!"
            percentage >= 80 -> "Almost there!"
            percentage >= 40 -> "Good progress"
            else -> "Keep saving"
        }

        val status = createStatusText(
            "$percentage% complete - $statusMessage",
            progressColor
        )

        val progressBar = createProgressBar(
            progress = safeProgress,
            color = progressColor
        )

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

    private fun addDebtCard(
        debt: FirebaseDebt,
        totalPaid: Double
    ) {
        val remainingBalance =
            (debt.totalAmount - totalPaid).coerceAtLeast(0.0)

        val percentagePaid = if (debt.totalAmount > 0) {
            ((totalPaid / debt.totalAmount) * 100).toInt()
        } else {
            0
        }

        val safeProgress = percentagePaid.coerceIn(0, 100)
        val progressColor = getProgressColor(percentagePaid)

        val card = createCard()

        val title = createTitle(
            if (remainingBalance <= 0) {
                "✓ ${debt.debtName}"
            } else {
                debt.debtName
            }
        )

        title.setTextColor(progressColor)

        val remaining = createText(
            "Remaining: ${formatMoney(remainingBalance)} of ${formatMoney(debt.totalAmount)}"
        )

        val dueDate = createText(
            "Due date: ${debt.dueDate}"
        )

        val categoryText = createText(
            "Log payments under: ${debt.expenseCategory}"
        )

        val statusMessage = if (remainingBalance <= 0) {
            "Fully paid"
        } else {
            "$percentagePaid% paid"
        }

        val status = createStatusText(
            text = statusMessage,
            color = progressColor
        )

        val progressBar = createProgressBar(
            progress = safeProgress,
            color = progressColor
        )

        val actions = LinearLayout(this)
        actions.orientation = LinearLayout.HORIZONTAL

        val logPaymentButton = createActionButton("Log Payment")
        val optionsButton = createActionButton("Options")

        logPaymentButton.isEnabled = remainingBalance > 0

        logPaymentButton.setOnClickListener {
            openExpenseScreen(debt.expenseCategory)
        }

        optionsButton.setOnClickListener {
            showDebtOptions(debt)
        }

        actions.addView(logPaymentButton)
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
        intent.putExtra("userUid", userUid)
        intent.putExtra("preselectedCategory", categoryName)
        startActivity(intent)
    }

    private fun showSavingsOptions(goal: FirebaseSavingsGoal) {
        val options = arrayOf("Edit Goal", "Delete Goal")

        AlertDialog.Builder(this)
            .setTitle(goal.goalName)
            .setItems(options) { _, selectedOption ->
                when (selectedOption) {
                    0 -> {
                        val intent = Intent(this, AddSavingsGoal::class.java)
                        intent.putExtra("userUid", userUid)
                        intent.putExtra("goalId", goal.id)
                        startActivity(intent)
                    }

                    1 -> confirmDeleteSavingsGoal(goal)
                }
            }
            .show()
    }

    private fun confirmDeleteSavingsGoal(goal: FirebaseSavingsGoal) {
        AlertDialog.Builder(this)
            .setTitle("Delete Savings Goal")
            .setMessage(
                "Delete ${goal.goalName}? The category and logged expenses will remain in your expense history."
            )
            .setPositiveButton("Delete") { _, _ ->
                repository.deleteSavingsGoal(
                    uid = userUid,
                    goalId = goal.id,
                    onSuccess = {
                        Toast.makeText(
                            this,
                            "Savings goal deleted.",
                            Toast.LENGTH_SHORT
                        ).show()

                        loadSavingsAndDebts()
                    },
                    onError = { errorMessage ->
                        showError(errorMessage)
                    }
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDebtOptions(debt: FirebaseDebt) {
        val options = arrayOf("Edit Debt", "Delete Debt")

        AlertDialog.Builder(this)
            .setTitle(debt.debtName)
            .setItems(options) { _, selectedOption ->
                when (selectedOption) {
                    0 -> {
                        val intent = Intent(this, AddDebt::class.java)
                        intent.putExtra("userUid", userUid)
                        intent.putExtra("debtId", debt.id)
                        startActivity(intent)
                    }

                    1 -> confirmDeleteDebt(debt)
                }
            }
            .show()
    }

    private fun confirmDeleteDebt(debt: FirebaseDebt) {
        AlertDialog.Builder(this)
            .setTitle("Delete Debt")
            .setMessage(
                "Delete ${debt.debtName}? The category and logged payments will remain in your expense history."
            )
            .setPositiveButton("Delete") { _, _ ->
                repository.deleteDebt(
                    uid = userUid,
                    debtId = debt.id,
                    onSuccess = {
                        Toast.makeText(
                            this,
                            "Debt deleted.",
                            Toast.LENGTH_SHORT
                        ).show()

                        loadSavingsAndDebts()
                    },
                    onError = { errorMessage ->
                        showError(errorMessage)
                    }
                )
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

    private fun createStatusText(
        text: String,
        color: Int
    ): TextView {
        val status = TextView(this)
        status.text = text
        status.textSize = 14f
        status.setTextColor(color)
        status.setTypeface(null, Typeface.BOLD)
        status.setPadding(0, dp(6), 0, dp(6))

        return status
    }

    private fun createProgressBar(
        progress: Int,
        color: Int
    ): ProgressBar {
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

    private fun addEmptyMessage(
        container: LinearLayout,
        message: String
    ) {
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

    private fun showError(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun openLoginPage() {
        repository.logout()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    private fun formatMoney(amount: Double): String {
        return String.format(Locale.US, "R%.2f", amount)
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}