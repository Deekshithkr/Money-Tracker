package com.moneytracker.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "borrowed_items")
data class BorrowedItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personName: String,
    val totalAmount: Double,
    val amountReceived: Double = 0.0,
    val receiveMethod: String = "UPI",
    val dueDateMillis: Long = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000,
    val isSplit: Boolean = false,
    val splitCount: Int = 1,
    val splitDetails: String = "",
    val isSettled: Boolean = false,
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    val remainingAmount: Double
        get() = (totalAmount - amountReceived).coerceAtLeast(0.0)

    val progressPercent: Float
        get() = if (totalAmount > 0) ((amountReceived / totalAmount).coerceIn(0.0, 1.0)).toFloat() else 0f
}
