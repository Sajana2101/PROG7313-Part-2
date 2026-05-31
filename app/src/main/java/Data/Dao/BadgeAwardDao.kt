package Data.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.budgetquest.data.BadgeAward

@Dao
interface BadgeAwardDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAward(award: BadgeAward)

    @Query("""
        SELECT * FROM badge_awards 
        WHERE userId = :userId 
        ORDER BY id DESC
    """)
    suspend fun getAwardsByUser(userId: Int): List<BadgeAward>

    @Query("""
        SELECT * FROM badge_awards 
        WHERE userId = :userId 
        AND badgeType = :badgeType
        ORDER BY id DESC
    """)
    suspend fun getAwardsByType(
        userId: Int,
        badgeType: String
    ): List<BadgeAward>
}