package com.moneytracker.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneytracker.app.model.BankAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts ORDER BY isPrimary DESC, id ASC")
    fun getAllBankAccounts(): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE id = :id")
    suspend fun getBankAccountById(id: Long): BankAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(bank: BankAccountEntity): Long

    @Update
    suspend fun updateBankAccount(bank: BankAccountEntity)

    @Delete
    suspend fun deleteBankAccount(bank: BankAccountEntity)

    @Query("DELETE FROM bank_accounts")
    suspend fun deleteAll()
}
