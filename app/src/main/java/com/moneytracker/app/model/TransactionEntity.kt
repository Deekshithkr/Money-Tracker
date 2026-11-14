package com.moneytracker.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: Category,
    val paymentMethod: PaymentMethod,
    val creditCardId: Long? = null,
    val bankAccountId: Long? = null,
    val bankName: String? = null,
    val dateMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val isRecurring: Boolean = false,
    val recurringItemId: Long? = null
)
