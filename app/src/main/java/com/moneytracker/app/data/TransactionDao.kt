package com.moneytracker.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneytracker.app.model.Category
import com.moneytracker.app.model.PaymentMethod
import com.moneytracker.app.model.TransactionEntity
import com.moneytracker.app.model.TransactionType
import kotlinx.coroutines.flow.Flow

data class CategorySpend(
    val category: Category,
    val totalAmount: Double
)

data class PaymentMethodSpend(
    val paymentMethod: PaymentMethod,
    val totalAmount: Double
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 10): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE dateMillis >= :startTime AND dateMillis <= :endTime ORDER BY dateMillis DESC")
    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE paymentMethod = :method ORDER BY dateMillis DESC")
    fun getTransactionsByPaymentMethod(method: PaymentMethod): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE creditCardId = :cardId ORDER BY dateMillis DESC")
    fun getTransactionsByCreditCard(cardId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND dateMillis >= :startTime AND dateMillis <= :endTime")
    fun getTotalExpenseInRange(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND dateMillis >= :startTime AND dateMillis <= :endTime")
    fun getTotalIncomeInRange(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE paymentMethod = 'CREDIT_CARD' AND dateMillis >= :startTime AND dateMillis <= :endTime")
    fun getTotalCreditCardSpendInRange(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE creditCardId = :cardId AND dateMillis >= :startTime AND dateMillis <= :endTime")
    fun getCardSpendInRange(cardId: Long, startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT category, SUM(amount) as totalAmount FROM transactions WHERE type = 'EXPENSE' AND dateMillis >= :startTime AND dateMillis <= :endTime GROUP BY category ORDER BY totalAmount DESC")
    fun getCategorySpending(startTime: Long, endTime: Long): Flow<List<CategorySpend>>

    @Query("SELECT paymentMethod, SUM(amount) as totalAmount FROM transactions WHERE type = 'EXPENSE' AND dateMillis >= :startTime AND dateMillis <= :endTime GROUP BY paymentMethod ORDER BY totalAmount DESC")
    fun getPaymentMethodBreakdown(startTime: Long, endTime: Long): Flow<List<PaymentMethodSpend>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
