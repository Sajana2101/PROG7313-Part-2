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

@RunWith(RobolectricTestRunner::class)
class DatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertUser_andLoginUser_returnsUser() = runTest {

        val user = User(
            username = "testuser",
            password = "1234"
        )

        db.userDao().insertUser(user)

        val result = db.userDao().loginUser(
            "testuser",
            "1234"
        )

        assertNotNull(result)
        assertEquals("testuser", result?.username)
    }

    @Test
    fun insertExpense_returnsSavedExpense() = runTest {

        val expense = Expense(
            category = "Food",
            amount = 150.0,
            date = "2026-04-27",
            startTime = "10:00",
            endTime = "11:00",
            description = "Lunch",
            photoUrl = null
        )

        db.expenseDao().insertExpense(expense)

        val expenses = db.expenseDao().getAllExpenses()

        assertEquals(1, expenses.size)
        assertEquals("Food", expenses[0].category)
    }
}