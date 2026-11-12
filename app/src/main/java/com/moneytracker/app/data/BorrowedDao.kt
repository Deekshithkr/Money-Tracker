package com.moneytracker.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneytracker.app.model.BorrowedItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BorrowedDao {
    @Query("SELECT * FROM borrowed_items ORDER BY isSettled ASC, dueDateMillis ASC")
    fun getAllBorrowedItems(): Flow<List<BorrowedItemEntity>>

    @Query("SELECT * FROM borrowed_items WHERE isSettled = 0 ORDER BY dueDateMillis ASC")
    fun getActiveBorrowedItems(): Flow<List<BorrowedItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: BorrowedItemEntity): Long

    @Update
    suspend fun update(item: BorrowedItemEntity)

    @Delete
    suspend fun delete(item: BorrowedItemEntity)

    @Query("DELETE FROM borrowed_items")
    suspend fun deleteAll()
}
