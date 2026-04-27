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
}