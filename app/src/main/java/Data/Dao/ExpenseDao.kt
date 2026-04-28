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

    @Insert
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpenses(): List<Expense>

    @Query("SELECT * FROM expenses WHERE category = :categoryName")
    suspend fun getExpensesByCategory(categoryName: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE id = :expenseId LIMIT 1")
    suspend fun getExpenseById(expenseId: Int): Expense?

    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getExpenseBetweenDates(
        startDate: String,
        endDate: String
    ): List<Expense>

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