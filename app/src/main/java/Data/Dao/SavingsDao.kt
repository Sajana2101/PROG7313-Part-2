package Data.Dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.budgetquest.data.SavingsGoal

@Dao
interface SavingsGoalDao {

    @Insert
    suspend fun insertSavingsGoal(goal: SavingsGoal)

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoal)

    @Query("SELECT * FROM savings_goals WHERE userId = :userId ORDER BY id DESC")
    suspend fun getSavingsGoalsByUser(userId: Int): List<SavingsGoal>

    @Query("SELECT * FROM savings_goals WHERE id = :goalId LIMIT 1")
    suspend fun getSavingsGoalById(goalId: Int): SavingsGoal?
}