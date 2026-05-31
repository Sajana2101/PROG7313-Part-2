package Data.Database

import Data.Dao.CategoryDao
import Data.Dao.DebtDao
import Data.Dao.DebtPaymentDao
import Data.Dao.ExpenseDao
import Data.Dao.MonthlyDao
import Data.Dao.SavingsGoalDao
import Data.Dao.UserDao
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.budgetquest.data.Category
import com.example.budgetquest.data.Debt
import com.example.budgetquest.data.DebtPayment
import com.example.budgetquest.data.Expense
import com.example.budgetquest.data.MonthlyGoal
import com.example.budgetquest.data.SavingsGoal
import com.example.budgetquest.data.User

@Database(
    entities = [
        User::class,
        Expense::class,
        MonthlyGoal::class,
        Category::class,
        SavingsGoal::class,
        Debt::class,
        DebtPayment::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun monthlyGoalDao(): MonthlyDao
    abstract fun categoryDao(): CategoryDao

    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun debtDao(): DebtDao
    abstract fun debtPaymentDao(): DebtPaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tracker_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}