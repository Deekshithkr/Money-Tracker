package com.moneytracker.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneytracker.app.model.RecurringItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_items WHERE isActive = 1 ORDER BY dueDayOfMonth ASC")
    fun getActiveRecurringItems(): Flow<List<RecurringItemEntity>>

    @Query("SELECT * FROM recurring_items WHERE isLoan = 1 AND isActive = 1 ORDER BY dueDayOfMonth ASC")
    fun getActiveLoans(): Flow<List<RecurringItemEntity>>

    @Query("SELECT * FROM recurring_items WHERE isLoan = 0 AND isActive = 1 ORDER BY dueDayOfMonth ASC")
    fun getNonLoanRecurringItems(): Flow<List<RecurringItemEntity>>

    @Query("SELECT * FROM recurring_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RecurringItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: RecurringItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<RecurringItemEntity>)

    @Update
    suspend fun update(item: RecurringItemEntity)

    @Delete
    suspend fun delete(item: RecurringItemEntity)
}
