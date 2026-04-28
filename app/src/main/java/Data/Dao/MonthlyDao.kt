package Data.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.budgetquest.data.MonthlyGoal

@Dao
interface MonthlyDao {

    // saves a new monthly goal
    @Insert
    suspend fun insertGoal(goal: MonthlyGoal)

    // updates an existing goal
    @Update
    suspend fun updateGoal(goal: MonthlyGoal)

    // gets the saved monthly goal for the logged in user
    @Query("SELECT * FROM monthly_goals WHERE userId = :userId LIMIT 1")
    suspend fun getGoalByUser(userId: Int): MonthlyGoal?
}