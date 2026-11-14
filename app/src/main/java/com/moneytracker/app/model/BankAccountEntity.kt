package com.moneytracker.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String,
    val accountHolder: String = "",
    val accountNumberLast4: String = "",
    val accountType: BankAccountType = BankAccountType.SAVINGS,
    val initialBalance: Double = 0.0,
    val colorHex: String = "#10B981",
    val isPrimary: Boolean = false,
    val createdAtMillis: Long = System.currentTimeMillis()
)
