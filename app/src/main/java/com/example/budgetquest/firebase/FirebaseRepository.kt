package com.example.budgetquest.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class FirebaseRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val database: DatabaseReference =
        FirebaseDatabase.getInstance(DATABASE_URL).reference

    companion object {
        private const val DATABASE_URL =
            "https://budgetquest-19b9c-default-rtdb.europe-west1.firebasedatabase.app"
    }

    private fun userReference(uid: String): DatabaseReference {
        return database.child("users").child(uid)
    }

    private fun errorMessage(exception: Exception, fallback: String): String {
        return exception.message ?: fallback
    }


    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun logout() {
        auth.signOut()
    }

    fun registerUser(
        displayName: String,
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid

                if (uid == null) {
                    onError("Account created, but user information could not be loaded.")
                    return@addOnSuccessListener
                }

                val profile = FirebaseUserProfile(
                    uid = uid,
                    displayName = displayName,
                    email = email
                )

                userReference(uid)
                    .child("profile")
                    .setValue(profile)
                    .addOnSuccessListener {
                        onSuccess(uid)
                    }
                    .addOnFailureListener { exception ->
                        onError(
                            errorMessage(
                                exception,
                                "Account created, but profile could not be saved."
                            )
                        )
                    }
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Registration failed."))
            }
    }

    fun loginUser(
        email: String,
        password: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid

                if (uid == null) {
                    onError("Login successful, but user information could not be loaded.")
                } else {
                    onSuccess(uid)
                }
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Login failed."))
            }
    }

    fun getUserProfile(
        uid: String,
        onSuccess: (FirebaseUserProfile?) -> Unit,
        onError: (String) -> Unit
    ) {
        userReference(uid)
            .child("profile")
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.getValue(FirebaseUserProfile::class.java))
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not load user profile."))
            }
    }


    fun saveCategory(
        uid: String,
        category: FirebaseCategory,
        onSuccess: (FirebaseCategory) -> Unit,
        onError: (String) -> Unit
    ) {
        val categoryReference = if (category.id.isBlank()) {
            userReference(uid).child("categories").push()
        } else {
            userReference(uid).child("categories").child(category.id)
        }

        val categoryId = categoryReference.key

        if (categoryId == null) {
            onError("Could not create category ID.")
            return
        }

        val savedCategory = category.copy(id = categoryId)

        categoryReference
            .setValue(savedCategory)
            .addOnSuccessListener {
                onSuccess(savedCategory)
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not save category."))
            }
    }

    fun getCategories(
        uid: String,
        onSuccess: (List<FirebaseCategory>) -> Unit,
        onError: (String) -> Unit
    ) {
        userReference(uid)
            .child("categories")
            .get()
            .addOnSuccessListener { snapshot ->
                val categories = snapshot.children
                    .mapNotNull { it.getValue(FirebaseCategory::class.java) }
                    .sortedBy { it.name.lowercase() }

                onSuccess(categories)
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not load categories."))
            }
    }

    fun getCategoryByName(
        uid: String,
        categoryName: String,
        onSuccess: (FirebaseCategory?) -> Unit,
        onError: (String) -> Unit
    ) {
        getCategories(
            uid = uid,
            onSuccess = { categories ->
                val matchingCategory = categories.firstOrNull {
                    it.name.equals(categoryName, ignoreCase = true)
                }

                onSuccess(matchingCategory)
            },
            onError = onError
        )
    }

    fun deleteCategory(
        uid: String,
        categoryId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        userReference(uid)
            .child("categories")
            .child(categoryId)
            .removeValue()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not delete category."))
            }
    }


    fun saveExpense(
        uid: String,
        expense: FirebaseExpense,
        onSuccess: (FirebaseExpense) -> Unit,
        onError: (String) -> Unit
    ) {
        val expenseReference = if (expense.id.isBlank()) {
            userReference(uid).child("expenses").push()
        } else {
            userReference(uid).child("expenses").child(expense.id)
        }

        val expenseId = expenseReference.key

        if (expenseId == null) {
            onError("Could not create expense ID.")
            return
        }

        val savedExpense = expense.copy(id = expenseId)

        expenseReference
            .setValue(savedExpense)
            .addOnSuccessListener {
                onSuccess(savedExpense)
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not save expense."))
            }
    }

    fun getExpenses(
        uid: String,
        onSuccess: (List<FirebaseExpense>) -> Unit,
        onError: (String) -> Unit
    ) {
        userReference(uid)
            .child("expenses")
            .get()
            .addOnSuccessListener { snapshot ->
                val expenses = snapshot.children
                    .mapNotNull { it.getValue(FirebaseExpense::class.java) }
                    .sortedByDescending { it.date }

                onSuccess(expenses)
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not load expenses."))
            }
    }

    fun getExpenseById(
        uid: String,
        expenseId: String,
        onSuccess: (FirebaseExpense?) -> Unit,
        onError: (String) -> Unit
    ) {
        userReference(uid)
            .child("expenses")
            .child(expenseId)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.getValue(FirebaseExpense::class.java))
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not load expense."))
            }
    }

    fun getExpensesByCategory(
        uid: String,
        categoryName: String,
        onSuccess: (List<FirebaseExpense>) -> Unit,
        onError: (String) -> Unit
    ) {
        getExpenses(
            uid = uid,
            onSuccess = { expenses ->
                val categoryExpenses = expenses.filter {
                    it.category.equals(categoryName, ignoreCase = true)
                }

                onSuccess(categoryExpenses)
            },
            onError = onError
        )
    }

    fun getExpensesBetweenDates(
        uid: String,
        startDate: String,
        endDate: String,
        onSuccess: (List<FirebaseExpense>) -> Unit,
        onError: (String) -> Unit
    ) {
        getExpenses(
            uid = uid,
            onSuccess = { expenses ->
                val matchingExpenses = expenses.filter {
                    it.date >= startDate && it.date <= endDate
                }

                onSuccess(matchingExpenses)
            },
            onError = onError
        )
    }

    fun getCategoryTotalsBetweenDates(
        uid: String,
        startDate: String,
        endDate: String,
        onSuccess: (Map<String, Double>) -> Unit,
        onError: (String) -> Unit
    ) {
        getExpensesBetweenDates(
            uid = uid,
            startDate = startDate,
            endDate = endDate,
            onSuccess = { expenses ->
                val totals = expenses
                    .groupBy { it.category }
                    .mapValues { entry ->
                        entry.value.sumOf { it.amount }
                    }

                onSuccess(totals)
            },
            onError = onError
        )
    }

    fun deleteExpense(
        uid: String,
        expenseId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        userReference(uid)
            .child("expenses")
            .child(expenseId)
            .removeValue()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not delete expense."))
            }
    }


    fun saveMonthlyGoal(
        uid: String,
        monthlyGoal: FirebaseMonthlyGoal,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        userReference(uid)
            .child("monthlyGoal")
            .setValue(monthlyGoal)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not save monthly goal."))
            }
    }

    fun getMonthlyGoal(
        uid: String,
        onSuccess: (FirebaseMonthlyGoal?) -> Unit,
        onError: (String) -> Unit
    ) {
        userReference(uid)
            .child("monthlyGoal")
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.getValue(FirebaseMonthlyGoal::class.java))
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not load monthly goal."))
            }
    }


    fun saveSavingsGoal(
        uid: String,
        savingsGoal: FirebaseSavingsGoal,
        onSuccess: (FirebaseSavingsGoal) -> Unit,
        onError: (String) -> Unit
    ) {
        val goalReference = if (savingsGoal.id.isBlank()) {
            userReference(uid).child("savingsGoals").push()
        } else {
            userReference(uid).child("savingsGoals").child(savingsGoal.id)
        }

        val goalId = goalReference.key

        if (goalId == null) {
            onError("Could not create savings goal ID.")
            return
        }

        val savedGoal = savingsGoal.copy(id = goalId)

        goalReference
            .setValue(savedGoal)
            .addOnSuccessListener {
                onSuccess(savedGoal)
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not save savings goal."))
            }
    }

    fun getSavingsGoals(
        uid: String,
        onSuccess: (List<FirebaseSavingsGoal>) -> Unit,
        onError: (String) -> Unit
    ) {
        userReference(uid)
            .child("savingsGoals")
            .get()
            .addOnSuccessListener { snapshot ->
                val goals = snapshot.children
                    .mapNotNull { it.getValue(FirebaseSavingsGoal::class.java) }
                    .sortedByDescending { it.createdDate }

                onSuccess(goals)
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not load savings goals."))
            }
    }

    fun getSavingsGoalById(
        uid: String,
        goalId: String,
        onSuccess: (FirebaseSavingsGoal?) -> Unit,
        onError: (String) -> Unit
    ) {
        userReference(uid)
            .child("savingsGoals")
            .child(goalId)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.getValue(FirebaseSavingsGoal::class.java))
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not load savings goal."))
            }
    }

    fun deleteSavingsGoal(
        uid: String,
        goalId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        userReference(uid)
            .child("savingsGoals")
            .child(goalId)
            .removeValue()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not delete savings goal."))
            }
    }


    fun saveDebt(
        uid: String,
        debt: FirebaseDebt,
        onSuccess: (FirebaseDebt) -> Unit,
        onError: (String) -> Unit
    ) {
        val debtReference = if (debt.id.isBlank()) {
            userReference(uid).child("debts").push()
        } else {
            userReference(uid).child("debts").child(debt.id)
        }

        val debtId = debtReference.key

        if (debtId == null) {
            onError("Could not create debt ID.")
            return
        }

        val savedDebt = debt.copy(id = debtId)

        debtReference
            .setValue(savedDebt)
            .addOnSuccessListener {
                onSuccess(savedDebt)
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not save debt."))
            }
    }

    fun getDebts(
        uid: String,
        onSuccess: (List<FirebaseDebt>) -> Unit,
        onError: (String) -> Unit
    ) {
        userReference(uid)
            .child("debts")
            .get()
            .addOnSuccessListener { snapshot ->
                val debts = snapshot.children
                    .mapNotNull { it.getValue(FirebaseDebt::class.java) }
                    .sortedBy { it.dueDate }

                onSuccess(debts)
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not load debts."))
            }
    }

    fun getDebtById(
        uid: String,
        debtId: String,
        onSuccess: (FirebaseDebt?) -> Unit,
        onError: (String) -> Unit
    ) {
        userReference(uid)
            .child("debts")
            .child(debtId)
            .get()
            .addOnSuccessListener { snapshot ->
                onSuccess(snapshot.getValue(FirebaseDebt::class.java))
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not load debt."))
            }
    }

    fun getDebtByExpenseCategory(
        uid: String,
        categoryName: String,
        onSuccess: (FirebaseDebt?) -> Unit,
        onError: (String) -> Unit
    ) {
        getDebts(
            uid = uid,
            onSuccess = { debts ->
                val matchingDebt = debts.firstOrNull {
                    it.expenseCategory.equals(categoryName, ignoreCase = true)
                }

                onSuccess(matchingDebt)
            },
            onError = onError
        )
    }

    fun deleteDebt(
        uid: String,
        debtId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        userReference(uid)
            .child("debts")
            .child(debtId)
            .removeValue()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not delete debt."))
            }
    }


    fun saveBadgeAward(
        uid: String,
        award: FirebaseBadgeAward,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val awardId = buildAwardId(
            badgeType = award.badgeType,
            awardReference = award.awardReference
        )

        val savedAward = award.copy(id = awardId)

        userReference(uid)
            .child("badgeAwards")
            .child(awardId)
            .setValue(savedAward)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not save badge award."))
            }
    }

    fun getBadgeAwards(
        uid: String,
        onSuccess: (List<FirebaseBadgeAward>) -> Unit,
        onError: (String) -> Unit
    ) {
        userReference(uid)
            .child("badgeAwards")
            .get()
            .addOnSuccessListener { snapshot ->
                val awards = snapshot.children
                    .mapNotNull { it.getValue(FirebaseBadgeAward::class.java) }
                    .sortedByDescending { it.earnedDate }

                onSuccess(awards)
            }
            .addOnFailureListener { exception ->
                onError(errorMessage(exception, "Could not load badge awards."))
            }
    }

    private fun buildAwardId(
        badgeType: String,
        awardReference: String
    ): String {
        return "${badgeType}_$awardReference"
            .replace('.', '_')
            .replace('#', '_')
            .replace('$', '_')
            .replace('[', '_')
            .replace(']', '_')
            .replace('/', '_')
    }
}