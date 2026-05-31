package Data.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.budgetquest.data.DebtPayment

@Dao
interface DebtPaymentDao {

    @Insert
    suspend fun insertDebtPayment(payment: DebtPayment)

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId AND userId = :userId")
    suspend fun getPaymentsForDebt(debtId: Int, userId: Int): List<DebtPayment>

    @Query("""
        SELECT COALESCE(SUM(amount), 0) 
        FROM debt_payments 
        WHERE debtId = :debtId AND userId = :userId
    """)
    suspend fun getTotalPaidForDebt(debtId: Int, userId: Int): Double

    @Query("DELETE FROM debt_payments WHERE debtId = :debtId AND userId = :userId")
    suspend fun deletePaymentsForDebt(debtId: Int, userId: Int)
}