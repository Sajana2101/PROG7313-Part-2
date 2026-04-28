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

    // gets all saved expenses
    @Query("SELECT * FROM expenses")
    suspend fun getAllExpenses(): List<Expense>

    // gets expenses for a specific category
    @Query("SELECT * FROM expenses WHERE category = :categoryName")
    suspend fun getExpensesByCategory(categoryName: String): List<Expense>

    // gets a single expense using its id
    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getExpenseById(expenseId: Int): Expense?

    // gets expenses between selected dates
    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getExpenseBetweenDates(
        startDate: String,
        endDate: String
    ): List<Expense>

    // calculates total spending per category for the report
    @Query("""
        SELECT category, SUM(amount) AS totalAmount
        FROM expenses
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY category
    """)
    suspend fun getTotalSpentByCategory(
        startDate: String,
        endDate: String
    ): List<CategoryTotal>
}