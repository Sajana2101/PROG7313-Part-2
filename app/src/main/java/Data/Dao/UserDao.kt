package Data.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.budgetquest.data.User

@Dao
interface UserDao {

    // saves a new user during registration
    @Insert
    suspend fun insertUser(user: User)

    // checks if a username already exists
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    // checks login details and returns matching user
    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    suspend fun loginUser(username: String, password: String): User?
}