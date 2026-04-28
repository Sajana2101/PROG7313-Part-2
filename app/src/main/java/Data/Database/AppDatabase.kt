package Data.Database

import Data.Dao.CategoryDao
import Data.Dao.ExpenseDao
import Data.Dao.MonthlyDao
import Data.Dao.UserDao
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.budgetquest.data.Category
import com.example.budgetquest.data.Expense
import com.example.budgetquest.data.MonthlyGoal
import com.example.budgetquest.data.User

@Database(
    entities = [User::class, Expense::class, MonthlyGoal::class, Category::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun monthlyGoalDao(): MonthlyDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        // creates only one database instance for the app
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tracker_database"
                )
                    // resets db if schema changes
                    .fallbackToDestructiveMigration(true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}