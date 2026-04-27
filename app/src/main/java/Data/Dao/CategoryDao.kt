package Data.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.budgetquest.data.Category

@Dao
interface CategoryDao {

    @Insert
    suspend fun insertCategory(category: Category)

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategories(): List<Category>

    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?
}