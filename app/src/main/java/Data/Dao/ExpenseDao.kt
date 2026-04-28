package Data.Dao

import Data.Database.CategoryTotal
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.budgetquest.data.Expense

@Dao
interface ExpenseDao {

    // adds a new expense to the db
    @Insert
    suspend fun insertExpense(expense: Expense)

    // updates an existing expense
    @Update
    suspend fun updateExpense(expense: Expense)

    // removes an expense from the db
    @Delete
    suspend fun deleteExpense(expense: Expense)

    // gets all saved expenses for the logged in user
    @Query("SELECT * FROM expenses WHERE userId = :userId")
    suspend fun getExpensesByUser(userId: Int): List<Expense>

    // gets expenses for a specific category and logged in user
    @Query("SELECT * FROM expenses WHERE category = :categoryName AND userId = :userId")
    suspend fun getExpensesByCategoryAndUser(
        categoryName: String,
        userId: Int
    ): List<Expense>

    // gets a single expense using its id
    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getExpenseById(expenseId: Int): Expense?

    // gets expenses between selected dates for the logged in user
    @Query("SELECT * FROM expenses WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    suspend fun getExpenseBetweenDates(
        userId: Int,
        startDate: String,
        endDate: String
    ): List<Expense>

    // calculates total spending per category for the report for the logged in user
    @Query("""
        SELECT category, SUM(amount) AS totalAmount
        FROM expenses
        WHERE userId = :userId
        AND date BETWEEN :startDate AND :endDate
        GROUP BY category
    """)
    suspend fun getTotalSpentByCategory(
        userId: Int,
        startDate: String,
        endDate: String
    ): List<CategoryTotal>
}