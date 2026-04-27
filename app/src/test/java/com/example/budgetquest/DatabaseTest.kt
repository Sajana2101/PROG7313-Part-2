package com.example.budgetquest

import Data.Database.AppDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.budgetquest.data.User
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.example.budgetquest.data.Expense
import com.example.budgetquest.data.MonthlyGoal

@RunWith(RobolectricTestRunner::class)
class DatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        // this creates a temprorary in-memory db for testing
        // to avoid affecting the real apps db
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        // this closes the db after each test
        db.close()
    }

    @Test
    fun insertUser_andLoginUser_returnsUser() = runTest {

        //This creates a test user
        val user = User(
            username = "testuser",
            password = "1234"
        )
// Saves them to the db
        db.userDao().insertUser(user)
//checks if the login works with correct credentials
        val result = db.userDao().loginUser(
            "testuser",
            "1234"
        )
//confirms the login worked
        assertNotNull(result)
        assertEquals("testuser", result?.username)
    }

    @Test
    fun insertExpense_returnsSavedExpense() = runTest {
        //this creates a sample expense
        val expense = Expense(
            category = "Food",
            amount = 150.0,
            date = "2026-04-27",
            startTime = "10:00",
            endTime = "11:00",
            description = "Lunch",
            photoUrl = null
        )
//saves it to the db
        db.expenseDao().insertExpense(expense)
//retrieves the saved expenses
        val expenses = db.expenseDao().getAllExpenses()
//checks the info was stored correctly
        assertEquals(1, expenses.size)
        assertEquals("Food", expenses[0].category)
    }

    @Test
    fun insertMonthlyGoal_returnsSavedGoal() = runTest {
//creates goals
        val goal = MonthlyGoal(
            minGoal = 1000.0,
            maxGoal = 5000.0
        )
//saves goals
        db.monthlyGoalDao().insertGoal(goal)
//retrieves them
        val result = db.monthlyGoalDao().getGoal()
//checks it was stored correctly
        assertNotNull(result)
        assertEquals(1000.0, result?.minGoal ?: 0.0, 0.0)
    }
}