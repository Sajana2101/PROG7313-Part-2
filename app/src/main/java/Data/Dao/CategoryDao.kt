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

    // gets all categories and sorts them alphabetically
    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategories(): List<Category>

    // checks if a category with the same name already exists
    @Query("SELECT * FROM categories WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?

    // updates an existing category
    @Update
    suspend fun updateCategory(category: Category)

    // deletes a category from the db
    @Delete
    suspend fun deleteCategory(category: Category)
}