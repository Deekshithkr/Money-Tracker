package com.moneytracker.app.data

import androidx.room.TypeConverter
import com.moneytracker.app.model.Category
import com.moneytracker.app.model.PaymentMethod
import com.moneytracker.app.model.RecurringFrequency
import com.moneytracker.app.model.RecurringType
import com.moneytracker.app.model.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromPaymentMethod(method: PaymentMethod): String = method.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)

    @TypeConverter
    fun fromCategory(category: Category): String = category.name

    @TypeConverter
    fun toCategory(value: String): Category = Category.valueOf(value)

    @TypeConverter
    fun fromRecurringType(type: RecurringType): String = type.name

    @TypeConverter
    fun toRecurringType(value: String): RecurringType = RecurringType.valueOf(value)

    @TypeConverter
    fun fromRecurringFrequency(freq: RecurringFrequency): String = freq.name

    @TypeConverter
    fun toRecurringFrequency(value: String): RecurringFrequency = RecurringFrequency.valueOf(value)
}
