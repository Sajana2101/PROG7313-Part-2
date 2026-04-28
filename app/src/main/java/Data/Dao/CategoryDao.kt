package Data.Dao

import androidx.room.Delete
import androidx.room.Update
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.budgetquest.data.Category

@Dao
interface CategoryDao {

    // adds a new category to the db
    @Insert
    suspend fun insertCategory(category: Category)

    // gets only the logged in user's categories and sorts them alphabetically
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name ASC")
    suspend fun getCategoriesByUser(userId: Int): List<Category>

    // checks if a category with the same name already exists for that specific user
    @Query("""
        SELECT * FROM categories 
        WHERE LOWER(name) = LOWER(:name) 
        AND userId = :userId 
        LIMIT 1
    """)
    suspend fun getCategoryByNameAndUser(
        name: String,
        userId: Int
    ): Category?

    // updates an existing category
    @Update
    suspend fun updateCategory(category: Category)

    // deletes a category from the db
    @Delete
    suspend fun deleteCategory(category: Category)
}