package com.example.budgetquest

import com.example.budgetquest.firebase.FirebaseBadgeAward
import com.example.budgetquest.firebase.FirebaseBadgeTypes
import com.example.budgetquest.firebase.FirebaseCategory
import com.example.budgetquest.firebase.FirebaseDebt
import com.example.budgetquest.firebase.FirebaseExpense
import com.example.budgetquest.firebase.FirebaseMonthlyGoal
import com.example.budgetquest.firebase.FirebaseRepository
import com.example.budgetquest.firebase.FirebaseSavingsGoal
import com.example.budgetquest.firebase.FirebaseUserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import org.junit.After
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirebaseRepositoryTest {

    private lateinit var repository: FirebaseRepository

    private var userUid: String = ""
    private var testEmail: String = ""

    companion object {
        private const val DATABASE_URL =
            "https://budgetquest-19b9c-default-rtdb.europe-west1.firebasedatabase.app"

        private const val AUTH_EMULATOR_HOST = "10.0.2.2"
        private const val AUTH_EMULATOR_PORT = 9099

        private const val DATABASE_EMULATOR_HOST = "10.0.2.2"
        private const val DATABASE_EMULATOR_PORT = 9000

        @BeforeClass
        @JvmStatic
        fun connectToFirebaseEmulators() {
            FirebaseAuth.getInstance().useEmulator(
                AUTH_EMULATOR_HOST,
                AUTH_EMULATOR_PORT
            )

            FirebaseDatabase.getInstance(DATABASE_URL).useEmulator(
                DATABASE_EMULATOR_HOST,
                DATABASE_EMULATOR_PORT
            )
        }
    }

    @Before
    fun setup() {
        repository = FirebaseRepository()

        testEmail =
            "budgetquest_${UUID.randomUUID()}@example.com"

        userUid = awaitValue { onSuccess, onError ->
            repository.registerUser(
                displayName = "Test User",
                email = testEmail,
                password = "BudgetTest123",
                onSuccess = onSuccess,
                onError = onError
            )
        }
    }

    @After
    fun tearDown() {
        repository.logout()
    }

    @Test
    fun registerUser_savesProfile_andLoginReturnsSameUser() {
        val profile = awaitValue<FirebaseUserProfile?> { onSuccess, onError ->
            repository.getUserProfile(
                uid = userUid,
                onSuccess = onSuccess,
                onError = onError
            )
        }

        Assert.assertNotNull(profile)
        Assert.assertEquals(userUid, profile?.uid)
        Assert.assertEquals("Test User", profile?.displayName)
        Assert.assertEquals(testEmail, profile?.email)

        repository.logout()

        val loggedInUid = awaitValue<String> { onSuccess, onError ->
            repository.loginUser(
                email = testEmail,
                password = "BudgetTest123",
                onSuccess = onSuccess,
                onError = onError
            )
        }

        Assert.assertEquals(userUid, loggedInUid)
    }

    @Test
    fun saveCategory_andGetCategories_returnsSavedCategory() {
        val category = FirebaseCategory(
            name = "Groceries",
            monthlyLimit = 2500.0
        )

        val savedCategory = awaitValue<FirebaseCategory> { onSuccess, onError ->
            repository.saveCategory(
                uid = userUid,
                category = category,
                onSuccess = onSuccess,
                onError = onError
            )
        }

        val categories = awaitValue<List<FirebaseCategory>> { onSuccess, onError ->
            repository.getCategories(
                uid = userUid,
                onSuccess = onSuccess,
                onError = onError
            )
        }

        Assert.assertTrue(savedCategory.id.isNotBlank())
        Assert.assertEquals(1, categories.size)
        Assert.assertEquals("Groceries", categories.first().name)
        Assert.assertEquals(2500.0, categories.first().monthlyLimit, 0.0)
    }

    @Test
    fun saveExpense_andGetExpensesByCategory_returnsSavedExpense() {
        val expense = FirebaseExpense(
            category = "Groceries",
            amount = 150.0,
            date = "2026-06-01",
            startTime = "10:00",
            endTime = "10:30",
            description = "Food shopping",
            photoUrl = null
        )

        val savedExpense = awaitValue<FirebaseExpense> { onSuccess, onError ->
            repository.saveExpense(
                uid = userUid,
                expense = expense,
                onSuccess = onSuccess,
                onError = onError
            )
        }

        val expenses = awaitValue<List<FirebaseExpense>> { onSuccess, onError ->
            repository.getExpensesByCategory(
                uid = userUid,
                categoryName = "Groceries",
                onSuccess = onSuccess,
                onError = onError
            )
        }

        Assert.assertTrue(savedExpense.id.isNotBlank())
        Assert.assertEquals(1, expenses.size)
        Assert.assertEquals("Groceries", expenses.first().category)
        Assert.assertEquals(150.0, expenses.first().amount, 0.0)
        Assert.assertEquals("Food shopping", expenses.first().description)
    }

    @Test
    fun saveMonthlyGoal_andGetMonthlyGoal_returnsSavedGoal() {
        val monthlyGoal = FirebaseMonthlyGoal(
            minGoal = 1000.0,
            maxGoal = 5000.0
        )

        awaitCompletion { onSuccess, onError ->
            repository.saveMonthlyGoal(
                uid = userUid,
                monthlyGoal = monthlyGoal,
                onSuccess = onSuccess,
                onError = onError
            )
        }

        val savedGoal = awaitValue<FirebaseMonthlyGoal?> { onSuccess, onError ->
            repository.getMonthlyGoal(
                uid = userUid,
                onSuccess = onSuccess,
                onError = onError
            )
        }

        Assert.assertNotNull(savedGoal)
        Assert.assertEquals(1000.0, savedGoal?.minGoal ?: 0.0, 0.0)
        Assert.assertEquals(5000.0, savedGoal?.maxGoal ?: 0.0, 0.0)
    }

    @Test
    fun saveSavingsGoal_andGetSavingsGoals_returnsSavedGoal() {
        val savingsGoal = FirebaseSavingsGoal(
            goalName = "Holiday",
            targetAmount = 5000.0,
            expenseCategory = "Savings - Holiday",
            createdDate = "2026-06-01"
        )

        val savedGoal = awaitValue<FirebaseSavingsGoal> { onSuccess, onError ->
            repository.saveSavingsGoal(
                uid = userUid,
                savingsGoal = savingsGoal,
                onSuccess = onSuccess,
                onError = onError
            )
        }

        val savingsGoals = awaitValue<List<FirebaseSavingsGoal>> { onSuccess, onError ->
            repository.getSavingsGoals(
                uid = userUid,
                onSuccess = onSuccess,
                onError = onError
            )
        }

        Assert.assertTrue(savedGoal.id.isNotBlank())
        Assert.assertEquals(1, savingsGoals.size)
        Assert.assertEquals("Holiday", savingsGoals.first().goalName)
        Assert.assertEquals("Savings - Holiday", savingsGoals.first().expenseCategory)
        Assert.assertEquals(5000.0, savingsGoals.first().targetAmount, 0.0)
    }

    @Test
    fun saveDebt_andGetDebtByExpenseCategory_returnsSavedDebt() {
        val debt = FirebaseDebt(
            debtName = "Laptop",
            totalAmount = 3000.0,
            dueDate = "2026-12-01",
            expenseCategory = "Debt Payment - Laptop"
        )

        val savedDebt = awaitValue<FirebaseDebt> { onSuccess, onError ->
            repository.saveDebt(
                uid = userUid,
                debt = debt,
                onSuccess = onSuccess,
                onError = onError
            )
        }

        val retrievedDebt = awaitValue<FirebaseDebt?> { onSuccess, onError ->
            repository.getDebtByExpenseCategory(
                uid = userUid,
                categoryName = "Debt Payment - Laptop",
                onSuccess = onSuccess,
                onError = onError
            )
        }

        Assert.assertTrue(savedDebt.id.isNotBlank())
        Assert.assertNotNull(retrievedDebt)
        Assert.assertEquals("Laptop", retrievedDebt?.debtName)
        Assert.assertEquals(3000.0, retrievedDebt?.totalAmount ?: 0.0, 0.0)
        Assert.assertEquals("Debt Payment - Laptop", retrievedDebt?.expenseCategory)
    }

    @Test
    fun saveBadgeAward_andGetBadgeAwards_returnsSavedAward() {
        val award = FirebaseBadgeAward(
            badgeType = FirebaseBadgeTypes.SAVINGS_CHAMPION,
            awardReference = "savings_test_goal",
            displayDetails = "Holiday goal completed - R5000.00 saved",
            earnedDate = "2026-06-01"
        )

        awaitCompletion { onSuccess, onError ->
            repository.saveBadgeAward(
                uid = userUid,
                award = award,
                onSuccess = onSuccess,
                onError = onError
            )
        }

        val awards = awaitValue<List<FirebaseBadgeAward>> { onSuccess, onError ->
            repository.getBadgeAwards(
                uid = userUid,
                onSuccess = onSuccess,
                onError = onError
            )
        }

        Assert.assertEquals(1, awards.size)
        Assert.assertEquals(FirebaseBadgeTypes.SAVINGS_CHAMPION, awards.first().badgeType)
        Assert.assertEquals("savings_test_goal", awards.first().awardReference)
    }

    private fun <T> awaitValue(
        operation: (
            onSuccess: (T) -> Unit,
            onError: (String) -> Unit
        ) -> Unit
    ): T {
        val latch = CountDownLatch(1)

        var returnedValue: Any? = null
        var returnedError: String? = null

        operation(
            { value ->
                returnedValue = value
                latch.countDown()
            },
            { errorMessage ->
                returnedError = errorMessage
                latch.countDown()
            }
        )

        val completed = latch.await(15, TimeUnit.SECONDS)

        Assert.assertTrue(
            "Firebase operation timed out. Check that the local Firebase emulators are running.",
            completed
        )

        if (returnedError != null) {
            Assert.fail(returnedError)
        }

        @Suppress("UNCHECKED_CAST")
        return returnedValue as T
    }

    private fun awaitCompletion(
        operation: (
            onSuccess: () -> Unit,
            onError: (String) -> Unit
        ) -> Unit
    ) {
        val latch = CountDownLatch(1)

        var returnedError: String? = null

        operation(
            {
                latch.countDown()
            },
            { errorMessage ->
                returnedError = errorMessage
                latch.countDown()
            }
        )

        val completed = latch.await(15, TimeUnit.SECONDS)

        Assert.assertTrue(
            "Firebase operation timed out. Check that the local Firebase emulators are running.",
            completed
        )

        if (returnedError != null) {
            Assert.fail(returnedError)
        }
    }
}