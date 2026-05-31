package Data.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.budgetquest.data.Debt

@Dao
interface DebtDao {

    @Insert
    suspend fun insertDebt(debt: Debt)

    @Update
    suspend fun updateDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)

    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY dueDate ASC")
    suspend fun getDebtsByUser(userId: Int): List<Debt>

    @Query("SELECT * FROM debts WHERE id = :debtId LIMIT 1")
    suspend fun getDebtById(debtId: Int): Debt?
}