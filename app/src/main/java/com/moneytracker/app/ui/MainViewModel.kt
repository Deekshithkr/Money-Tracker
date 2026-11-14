package com.moneytracker.app.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.moneytracker.app.data.CategorySpend
import com.moneytracker.app.data.MoneyTrackerRepository
import com.moneytracker.app.data.PaymentMethodSpend
import com.moneytracker.app.model.BankAccountEntity
import com.moneytracker.app.model.BankAccountType
import com.moneytracker.app.model.BorrowedItemEntity
import com.moneytracker.app.model.Category
import com.moneytracker.app.model.CreditCardEntity
import com.moneytracker.app.model.PaymentMethod
import com.moneytracker.app.model.RecurringItemEntity
import com.moneytracker.app.model.TransactionEntity
import com.moneytracker.app.model.TransactionType
import com.moneytracker.app.sync.GoogleAuthDriveHelper
import com.moneytracker.app.sync.MongoDbSyncHelper
import com.moneytracker.app.sync.UserDataSnapshot
import com.moneytracker.app.sync.UserProfile
import com.moneytracker.app.ui.components.MonthTrendData
import com.moneytracker.app.util.ExcelExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class BankCashFlow(
    val bank: BankAccountEntity,
    val totalInflow: Double,
    val totalOutflow: Double,
    val netCashFlow: Double,
    val currentBalance: Double
)

data class DashboardUiState(
    val selectedBankId: Long? = null,
    val totalBalance: Double = 0.0,
    val combinedBalance: Double = 0.0,
    val monthlyIncome: Double = 0.0,
    val monthlyExpense: Double = 0.0,
    val monthlyCreditCardSpend: Double = 0.0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val creditCards: List<CreditCardEntity> = emptyList(),
    val bankAccounts: List<BankAccountEntity> = emptyList(),
    val bankCashFlows: List<BankCashFlow> = emptyList(),
    val cardSpendMap: Map<Long, Double> = emptyMap(),
    val upcomingRecurring: List<RecurringItemEntity> = emptyList(),
    val activeLoans: List<RecurringItemEntity> = emptyList(),
    val nonLoanRecurring: List<RecurringItemEntity> = emptyList(),
    val categoryBreakdown: List<CategorySpend> = emptyList(),
    val paymentBreakdown: List<PaymentMethodSpend> = emptyList(),
    val trendData: List<MonthTrendData> = emptyList(),
    val borrowedItems: List<BorrowedItemEntity> = emptyList(),
    val totalBorrowedPending: Double = 0.0
)

private data class CoreData(
    val recents: List<TransactionEntity>,
    val cards: List<CreditCardEntity>,
    val recurring: List<RecurringItemEntity>,
    val banks: List<BankAccountEntity>
)

private data class ExtraData(
    val loans: List<RecurringItemEntity>,
    val allTx: List<TransactionEntity>,
    val borrowed: List<BorrowedItemEntity>
)

class MainViewModel(
    private val repository: MoneyTrackerRepository
) : ViewModel() {

    private val _currencySymbol = MutableStateFlow("₹")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _selectedBankFilterId = MutableStateFlow<Long?>(null)
    val selectedBankFilterId: StateFlow<Long?> = _selectedBankFilterId.asStateFlow()

    // Filter time range: Default current month
    private val currentMonthRange = getMonthTimeRange(0)

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCreditCards: StateFlow<List<CreditCardEntity>> = repository.allCreditCards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBankAccounts: StateFlow<List<BankAccountEntity>> = repository.allBankAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeRecurringItems: StateFlow<List<RecurringItemEntity>> = repository.activeRecurringItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeLoans: StateFlow<List<RecurringItemEntity>> = repository.activeLoans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nonLoanRecurring: StateFlow<List<RecurringItemEntity>> = repository.nonLoanRecurringItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBorrowedItems: StateFlow<List<BorrowedItemEntity>> = repository.allBorrowedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private fun currentEmail(): String =
        _userProfile.value.email.ifBlank { "guest@moneytracker.app" }

    fun selectBankFilter(bankId: Long?) {
        _selectedBankFilterId.value = bankId
    }

    // Combined Dashboard state with pure dynamic calculation and bank separation/filter
    val dashboardState: StateFlow<DashboardUiState> = combine(
        combine(
            repository.recentTransactions,
            repository.allCreditCards,
            repository.activeRecurringItems,
            repository.allBankAccounts
        ) { recents, cards, recurring, banks ->
            CoreData(recents, cards, recurring, banks)
        },
        combine(
            repository.activeLoans,
            repository.allTransactions,
            repository.allBorrowedItems
        ) { loans, allTx, borrowed ->
            ExtraData(loans, allTx, borrowed)
        },
        repository.getCategorySpending(currentMonthRange.first, currentMonthRange.second),
        repository.getPaymentMethodBreakdown(currentMonthRange.first, currentMonthRange.second),
        _selectedBankFilterId
    ) { core, extra, categories, paymentMethods, selectedBankId ->
        val recents = core.recents
        val cards = core.cards
        val recurring = core.recurring
        val banks = core.banks
        val loans = extra.loans
        val allTx = extra.allTx
        val borrowedItems = extra.borrowed

        val (start, end) = currentMonthRange

        // Combined calculations (All Banks / Overall)
        val combinedIncomeAll = allTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val combinedExpenseAll = allTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val combinedBalance = if (banks.isNotEmpty()) {
            banks.sumOf { bank ->
                val inflow = allTx.filter { it.bankAccountId == bank.id && it.type == TransactionType.INCOME }.sumOf { it.amount }
                val outflow = allTx.filter { it.bankAccountId == bank.id && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                bank.initialBalance + inflow - outflow
            }
        } else {
            combinedIncomeAll - combinedExpenseAll
        }

        // Bank Cash Flows per bank
        val bankFlows = banks.map { bank ->
            val inflow = allTx.filter { it.bankAccountId == bank.id && it.type == TransactionType.INCOME }.sumOf { it.amount }
            val outflow = allTx.filter { it.bankAccountId == bank.id && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val net = inflow - outflow
            BankCashFlow(
                bank = bank,
                totalInflow = inflow,
                totalOutflow = outflow,
                netCashFlow = net,
                currentBalance = bank.initialBalance + net
            )
        }

        // Filtered calculations if a bank is selected
        val (displayBalance, monthlyIncome, monthlyExpense, displayRecents) = if (selectedBankId == null) {
            val mIncome = allTx.filter { it.type == TransactionType.INCOME && it.dateMillis in start..end }.sumOf { it.amount }
            val mExpense = allTx.filter { it.type == TransactionType.EXPENSE && it.dateMillis in start..end }.sumOf { it.amount }
            Quad(combinedBalance, mIncome, mExpense, recents)
        } else {
            val selectedFlow = bankFlows.find { it.bank.id == selectedBankId }
            val bankBal = selectedFlow?.currentBalance ?: 0.0
            val mIncome = allTx.filter { it.bankAccountId == selectedBankId && it.type == TransactionType.INCOME && it.dateMillis in start..end }.sumOf { it.amount }
            val mExpense = allTx.filter { it.bankAccountId == selectedBankId && it.type == TransactionType.EXPENSE && it.dateMillis in start..end }.sumOf { it.amount }
            val bankRecents = recents.filter { it.bankAccountId == selectedBankId }
            Quad(bankBal, mIncome, mExpense, bankRecents)
        }

        val monthlyCCSpend = paymentMethods.find { it.paymentMethod == PaymentMethod.CREDIT_CARD }?.totalAmount ?: 0.0

        // Card spends
        val cardSpends = mutableMapOf<Long, Double>()
        cards.forEach { card ->
            cardSpends[card.id] = allTx
                .filter { it.creditCardId == card.id && it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
        }

        val trends = calculateTrends(allTx)
        val totalPending = borrowedItems.filter { !it.isSettled }.sumOf { it.remainingAmount }

        DashboardUiState(
            selectedBankId = selectedBankId,
            totalBalance = displayBalance,
            combinedBalance = combinedBalance,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            monthlyCreditCardSpend = monthlyCCSpend,
            recentTransactions = displayRecents,
            creditCards = cards,
            bankAccounts = banks,
            bankCashFlows = bankFlows,
            cardSpendMap = cardSpends,
            upcomingRecurring = recurring,
            activeLoans = loans,
            nonLoanRecurring = emptyList(),
            categoryBreakdown = categories,
            paymentBreakdown = paymentMethods,
            trendData = trends,
            borrowedItems = borrowedItems,
            totalBorrowedPending = totalPending
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun setUserProfile(profile: UserProfile) {
        _userProfile.value = profile
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        category: Category,
        paymentMethod: PaymentMethod,
        creditCardId: Long? = null,
        bankAccountId: Long? = null,
        bankName: String? = null,
        note: String = "",
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                title = title,
                amount = amount,
                type = type,
                category = category,
                paymentMethod = paymentMethod,
                creditCardId = creditCardId,
                bankAccountId = bankAccountId,
                bankName = bankName,
                dateMillis = dateMillis,
                note = note
            )
            val newId = repository.addTransaction(entity)
            val savedEntity = entity.copy(id = newId)

            // Auto-sync to MongoDB in background
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    MongoDbSyncHelper.autoSyncTransaction(currentEmail(), savedEntity)
                } catch (_: Exception) {}
            }
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    MongoDbSyncHelper.autoSyncTransaction(currentEmail(), transaction)
                } catch (_: Exception) {}
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    MongoDbSyncHelper.autoDeleteTransaction(currentEmail(), transaction.id)
                } catch (_: Exception) {}
            }
        }
    }

    fun addBankAccount(
        bankName: String,
        accountHolder: String = "",
        accountNumberLast4: String = "",
        accountType: BankAccountType = BankAccountType.SAVINGS,
        initialBalance: Double = 0.0,
        colorHex: String = "#10B981"
    ) {
        viewModelScope.launch {
            val bank = BankAccountEntity(
                bankName = bankName,
                accountHolder = accountHolder,
                accountNumberLast4 = accountNumberLast4,
                accountType = accountType,
                initialBalance = initialBalance,
                colorHex = colorHex
            )
            val id = repository.addBankAccount(bank)
            val savedBank = bank.copy(id = id)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    MongoDbSyncHelper.autoSyncBankAccount(currentEmail(), savedBank)
                } catch (_: Exception) {}
            }
        }
    }

    fun deleteBankAccount(bank: BankAccountEntity) {
        viewModelScope.launch {
            repository.deleteBankAccount(bank)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    MongoDbSyncHelper.autoDeleteBankAccount(currentEmail(), bank.id)
                } catch (_: Exception) {}
            }
        }
    }

    fun addCreditCard(card: CreditCardEntity) {
        viewModelScope.launch {
            val id = repository.addCreditCard(card)
            val savedCard = card.copy(id = id)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    MongoDbSyncHelper.autoSyncCreditCard(currentEmail(), savedCard)
                } catch (_: Exception) {}
            }
        }
    }

    fun addRecurringItem(item: RecurringItemEntity) {
        viewModelScope.launch {
            val id = repository.addRecurringItem(item)
            val saved = item.copy(id = id)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    MongoDbSyncHelper.autoSyncRecurringItem(currentEmail(), saved)
                } catch (_: Exception) {}
            }
        }
    }

    fun updateRecurringItem(item: RecurringItemEntity) {
        viewModelScope.launch {
            repository.updateRecurringItem(item)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    MongoDbSyncHelper.autoSyncRecurringItem(currentEmail(), item)
                } catch (_: Exception) {}
            }
        }
    }

    fun deleteRecurringItem(item: RecurringItemEntity) {
        viewModelScope.launch {
            repository.deleteRecurringItem(item)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    MongoDbSyncHelper.autoDeleteRecurringItem(currentEmail(), item.id)
                } catch (_: Exception) {}
            }
        }
    }

    fun addBorrowedItem(
        personName: String,
        totalAmount: Double,
        receiveMethod: String = "UPI",
        dueDateMillis: Long = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000,
        isSplit: Boolean = false,
        splitCount: Int = 1,
        splitDetails: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val item = BorrowedItemEntity(
                personName = personName,
                totalAmount = totalAmount,
                receiveMethod = receiveMethod,
                dueDateMillis = dueDateMillis,
                isSplit = isSplit,
                splitCount = splitCount,
                splitDetails = splitDetails,
                notes = notes
            )
            val id = repository.addBorrowedItem(item)
            val saved = item.copy(id = id)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    MongoDbSyncHelper.autoSyncBorrowedItem(currentEmail(), saved)
                } catch (_: Exception) {}
            }
        }
    }

    fun updateBorrowedItem(item: BorrowedItemEntity) {
        viewModelScope.launch {
            repository.updateBorrowedItem(item)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    MongoDbSyncHelper.autoSyncBorrowedItem(currentEmail(), item)
                } catch (_: Exception) {}
            }
        }
    }

    fun recordBorrowedRepayment(
        item: BorrowedItemEntity,
        amountReceived: Double,
        paymentMethod: PaymentMethod = PaymentMethod.UPI
    ) {
        viewModelScope.launch {
            repository.recordBorrowedRepayment(item, amountReceived, paymentMethod)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val updated = item.copy(
                        amountReceived = item.amountReceived + amountReceived,
                        isSettled = (item.amountReceived + amountReceived) >= item.totalAmount
                    )
                    MongoDbSyncHelper.autoSyncBorrowedItem(currentEmail(), updated)
                } catch (_: Exception) {}
            }
        }
    }

    fun deleteBorrowedItem(item: BorrowedItemEntity) {
        viewModelScope.launch {
            repository.deleteBorrowedItem(item)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    MongoDbSyncHelper.autoDeleteBorrowedItem(currentEmail(), item.id)
                } catch (_: Exception) {}
            }
        }
    }

    fun recordLoanPayment(loan: RecurringItemEntity) {
        viewModelScope.launch {
            repository.recordLoanPayment(loan)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val updatedPaid = loan.paidTenureMonths + 1
                    val updated = loan.copy(
                        paidTenureMonths = updatedPaid,
                        isActive = updatedPaid < loan.totalTenureMonths
                    )
                    MongoDbSyncHelper.autoSyncRecurringItem(currentEmail(), updated)
                } catch (_: Exception) {}
            }
        }
    }

    fun settleCreditCard(card: CreditCardEntity, amount: Double) {
        viewModelScope.launch {
            repository.settleCreditCardBill(card, amount)
        }
    }

    fun backupToGoogleDrive(context: Context) {
        val txs = allTransactions.value
        val banks = allBankAccounts.value
        val cards = allCreditCards.value
        val recs = activeRecurringItems.value
        val borrowed = allBorrowedItems.value

        val file = GoogleAuthDriveHelper.createBackupJsonFile(
            context = context,
            transactions = txs,
            bankAccounts = banks,
            creditCards = cards,
            recurringItems = recs,
            borrowedItems = borrowed
        )
        if (file != null) {
            GoogleAuthDriveHelper.launchGoogleDriveBackup(context, file)
        }
    }

    fun exportToExcel(context: Context) {
        val txs = allTransactions.value
        val banks = allBankAccounts.value
        val cards = allCreditCards.value
        val recs = activeRecurringItems.value
        val borrowed = allBorrowedItems.value

        val file = ExcelExportHelper.exportToExcel(
            context = context,
            transactions = txs,
            bankAccounts = banks,
            creditCards = cards,
            recurringItems = recs,
            borrowedItems = borrowed
        )
        if (file != null) {
            ExcelExportHelper.shareExportFile(context, file)
        }
    }

    fun restoreUserData(snapshot: UserDataSnapshot, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.clearAllData()
                repository.restoreAllData(
                    transactions = snapshot.transactions,
                    bankAccounts = snapshot.bankAccounts,
                    creditCards = snapshot.creditCards,
                    recurringItems = snapshot.recurringItems,
                    borrowedItems = snapshot.borrowedItems
                )
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    fun pullUserDataFromMongo(email: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            _isSyncing.value = true
            val snapshot = MongoDbSyncHelper.pullUserData(email)
            _isSyncing.value = false
            if (snapshot != null) {
                val totalItems = snapshot.transactions.size + snapshot.bankAccounts.size +
                        snapshot.creditCards.size + snapshot.recurringItems.size + snapshot.borrowedItems.size
                if (totalItems > 0) {
                    restoreUserData(snapshot) { success ->
                        if (success) {
                            onResult(true, "Restored $totalItems items from your MongoDB Cloud account.")
                        } else {
                            onResult(false, "Failed to write restored items to local database.")
                        }
                    }
                } else {
                    onResult(false, "No cloud data found for $email.")
                }
            } else {
                onResult(false, "Could not connect to MongoDB or fetch user cloud data.")
            }
        }
    }

    fun restoreFromDriveBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val snapshot = GoogleAuthDriveHelper.restoreBackupFromUri(context, uri)
            if (snapshot != null) {
                val totalItems = snapshot.transactions.size + snapshot.bankAccounts.size +
                        snapshot.creditCards.size + snapshot.recurringItems.size + snapshot.borrowedItems.size
                restoreUserData(snapshot) { success ->
                    if (success) {
                        onResult(true, "Restored $totalItems items from Google Drive backup file.")
                    } else {
                        onResult(false, "Failed to import items into database.")
                    }
                }
            } else {
                onResult(false, "Invalid or corrupted backup JSON file.")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    private fun calculateTrends(allTx: List<TransactionEntity>): List<MonthTrendData> {
        val monthNames = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val list = mutableListOf<MonthTrendData>()
        for (i in 3 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            val monthIdx = c.get(Calendar.MONTH)
            c.set(Calendar.DAY_OF_MONTH, 1)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            val mStart = c.timeInMillis

            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
            c.set(Calendar.HOUR_OF_DAY, 23)
            c.set(Calendar.MINUTE, 59)
            c.set(Calendar.SECOND, 59)
            val mEnd = c.timeInMillis

            val inc = allTx.filter { it.type == TransactionType.INCOME && it.dateMillis in mStart..mEnd }.sumOf { it.amount }
            val exp = allTx.filter { it.type == TransactionType.EXPENSE && it.dateMillis in mStart..mEnd }.sumOf { it.amount }
            list.add(MonthTrendData(monthNames[monthIdx], inc, exp))
        }
        return list
    }

    private fun getMonthTimeRange(monthsOffset: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -monthsOffset)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val start = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val end = cal.timeInMillis

        return Pair(start, end)
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    class Factory(private val repository: MoneyTrackerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
