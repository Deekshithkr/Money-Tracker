package com.moneytracker.app.data

import com.moneytracker.app.model.Category
import com.moneytracker.app.model.CreditCardEntity
import com.moneytracker.app.model.PaymentMethod
import com.moneytracker.app.model.RecurringItemEntity
import com.moneytracker.app.model.TransactionEntity
import com.moneytracker.app.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class MoneyTrackerRepository(
    private val transactionDao: TransactionDao,
    private val creditCardDao: CreditCardDao,
    private val recurringDao: RecurringDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = transactionDao.getRecentTransactions(15)
    val allCreditCards: Flow<List<CreditCardEntity>> = creditCardDao.getAllCards()
    val activeRecurringItems: Flow<List<RecurringItemEntity>> = recurringDao.getActiveRecurringItems()
    val activeLoans: Flow<List<RecurringItemEntity>> = recurringDao.getActiveLoans()
    val nonLoanRecurringItems: Flow<List<RecurringItemEntity>> = recurringDao.getNonLoanRecurringItems()

    fun getCategorySpending(startTime: Long, endTime: Long): Flow<List<CategorySpend>> =
        transactionDao.getCategorySpending(startTime, endTime)

    fun getPaymentMethodBreakdown(startTime: Long, endTime: Long): Flow<List<PaymentMethodSpend>> =
        transactionDao.getPaymentMethodBreakdown(startTime, endTime)

    fun getTotalExpenseInRange(startTime: Long, endTime: Long): Flow<Double?> =
        transactionDao.getTotalExpenseInRange(startTime, endTime)

    fun getTotalIncomeInRange(startTime: Long, endTime: Long): Flow<Double?> =
        transactionDao.getTotalIncomeInRange(startTime, endTime)

    fun getTotalCreditCardSpendInRange(startTime: Long, endTime: Long): Flow<Double?> =
        transactionDao.getTotalCreditCardSpendInRange(startTime, endTime)

    fun getCardSpendInRange(cardId: Long, startTime: Long, endTime: Long): Flow<Double?> =
        transactionDao.getCardSpendInRange(cardId, startTime, endTime)

    fun getTransactionsByPaymentMethod(method: PaymentMethod): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByPaymentMethod(method)

    fun getTransactionsByCreditCard(cardId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByCreditCard(cardId)

    suspend fun addTransaction(transaction: TransactionEntity): Long =
        transactionDao.insert(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.delete(transaction)

    suspend fun addCreditCard(card: CreditCardEntity): Long =
        creditCardDao.insert(card)

    suspend fun addRecurringItem(item: RecurringItemEntity): Long =
        recurringDao.insert(item)

    suspend fun deleteRecurringItem(item: RecurringItemEntity) =
        recurringDao.delete(item)

    suspend fun recordLoanPayment(loanItem: RecurringItemEntity) {
        val updatedPaidMonths = loanItem.paidTenureMonths + 1
        val isFinished = updatedPaidMonths >= loanItem.totalTenureMonths
        
        // 1. Update recurring item
        recurringDao.update(
            loanItem.copy(
                paidTenureMonths = updatedPaidMonths,
                isActive = !isFinished
            )
        )

        // 2. Insert expense transaction
        val now = System.currentTimeMillis()
        transactionDao.insert(
            TransactionEntity(
                title = "${loanItem.title} (EMI $updatedPaidMonths/${loanItem.totalTenureMonths})",
                amount = loanItem.amount,
                type = TransactionType.EXPENSE,
                category = Category.LOAN_EMI,
                paymentMethod = loanItem.defaultPaymentMethod,
                creditCardId = loanItem.linkedCreditCardId,
                dateMillis = now,
                note = "Auto-recorded recurring EMI payment",
                isRecurring = true,
                recurringItemId = loanItem.id
            )
        )
    }

    suspend fun settleCreditCardBill(card: CreditCardEntity, paymentAmount: Double) {
        val now = System.currentTimeMillis()
        // Record as an expense/transfer settlement
        transactionDao.insert(
            TransactionEntity(
                title = "Credit Card Bill Payment (${card.cardName})",
                amount = paymentAmount,
                type = TransactionType.TRANSFER,
                category = Category.BILLS,
                paymentMethod = PaymentMethod.NET_BANKING,
                creditCardId = card.id,
                dateMillis = now,
                note = "Statement settlement for ending in ${card.last4Digits}"
            )
        )
    }
}
