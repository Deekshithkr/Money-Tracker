package com.moneytracker.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardName: String,
    val bankName: String,
    val last4Digits: String,
    val creditLimit: Double,
    val billingCycleDay: Int, // e.g. 15th of month
    val paymentDueDay: Int,   // e.g. 5th of next month
    val cardGradientStart: Long = 0xFF3B82F6,
    val cardGradientEnd: Long = 0xFF1D4ED8,
    val network: String = "VISA" // VISA, MASTERCARD, RUPAY, AMEX
)
