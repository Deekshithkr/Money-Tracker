package com.moneytracker.app.model

enum class Category(
    val displayName: String,
    val emoji: String,
    val colorHex: Long,
    val isExpenseCategory: Boolean = true
) {
    FOOD("Food & Dining", "🍔", 0xFFFF7043),
    GROCERIES("Groceries", "🛒", 0xFF26A69A),
    SHOPPING("Shopping", "🛍️", 0xFFEC407A),
    BILLS("Bills & Utilities", "💡", 0xFFFFCA28),
    TRANSPORT("Transport & Fuel", "🚗", 0xFF42A5F5),
    ENTERTAINMENT("Entertainment", "🎬", 0xFFAB47BC),
    HEALTH("Health & Medical", "💊", 0xFF26C6DA),
    LOAN_EMI("Loan & EMI", "🏦", 0xFFEF5350),
    INVESTMENT("Investments", "📈", 0xFF66BB6A),
    SALARY("Salary & Income", "💰", 0xFF10B981, isExpenseCategory = false),
    FREELANCE("Freelance & Business", "💼", 0xFF34D399, isExpenseCategory = false),
    CASHBACK("Cashback & Rewards", "🎁", 0xFFFBBF24, isExpenseCategory = false),
    OTHER("Others", "📦", 0xFF78909C)
}
