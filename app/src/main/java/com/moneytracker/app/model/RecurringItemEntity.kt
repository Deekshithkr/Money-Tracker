package com.moneytracker.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecurringType(val displayName: String, val isCredit: Boolean) {
    SALARY("Salary Inflow", isCredit = true),
    RENTAL_INCOME("Rental Income", isCredit = true),
    FREELANCE_RETAINER("Freelance Retainer", isCredit = true),
    HOME_LOAN_EMI("Home Loan EMI", isCredit = false),
    CAR_LOAN_EMI("Car Loan EMI", isCredit = false),
    PERSONAL_LOAN_EMI("Personal Loan EMI", isCredit = false),
    SUBSCRIPTION("Subscription (Netflix/Spotify/etc)", isCredit = false),
    HOUSE_RENT("House Rent", isCredit = false),
    UTILITY_BILL("Utility Bill (WiFi/Electricity)", isCredit = false),
    SIP_INVESTMENT("SIP / Mutual Fund", isCredit = false)
}

enum class RecurringFrequency(val displayName: String) {
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    WEEKLY("Weekly")
}

@Entity(tableName = "recurring_items")
data class RecurringItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val recurringType: RecurringType,
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val dueDayOfMonth: Int, // 1 - 31
    val defaultPaymentMethod: PaymentMethod = PaymentMethod.UPI,
    val linkedCreditCardId: Long? = null,
    
    // Loan tracking details
    val isLoan: Boolean = false,
    val totalTenureMonths: Int = 0,
    val paidTenureMonths: Int = 0,
    val originalLoanAmount: Double = 0.0,
    val interestRatePercent: Double = 0.0,

    val nextDueDateMillis: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
