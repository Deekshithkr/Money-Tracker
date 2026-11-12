package com.moneytracker.app.model

enum class PaymentMethod(val displayName: String, val icon: String) {
    CREDIT_CARD("Credit Card", "💳"),
    UPI("UPI / Instant Transfer", "📱"),
    DEBIT_CARD("Debit Card", "🏧"),
    CASH("Cash", "💵"),
    NET_BANKING("Net Banking", "🌐")
}
