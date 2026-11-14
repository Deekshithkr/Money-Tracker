package com.moneytracker.app.ui

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
import com.moneytracker.app.sync.SyncResult
import com.moneytracker.app.sync.UserProfile
import com.moneytracker.app.ui.components.MonthTrendData
import com.moneytracker.app.util.ExcelExportHelper
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
    val totalBalance: Double = 0.0,
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

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<String> = MutableStateFlow("dark")

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

    private val _syncStatus = MutableStateFlow<SyncResult?>(null)
    val syncStatus: StateFlow<SyncResult?> = _syncStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Combined Dashboard state with pure dynamic calculation
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
        repository.getPaymentMethodBreakdown(currentMonthRange.first, currentMonthRange.second)
    ) { core, extra, categories, paymentMethods ->
        val recents = core.recents
        val cards = core.cards
        val recurring = core.recurring
        val banks = core.banks
        val loans = extra.loans
        val allTx = extra.allTx
        val borrowedItems = extra.borrowed

        val (start, end) = currentMonthRange

        // Real calculations from user transactions
        val totalIncomeAll = allTx.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpenseAll = allTx.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalBalance = totalIncomeAll - totalExpenseAll

        val monthlyExpense = categories.sumOf { it.totalAmount }
        val monthlyIncome = allTx.filter { it.type == TransactionType.INCOME && it.dateMillis in start..end }.sumOf { it.amount }
        val monthlyCCSpend = paymentMethods.find { it.paymentMethod == PaymentMethod.CREDIT_CARD }?.totalAmount ?: 0.0

        // Estimated card spends from actual transactions
        val cardSpends = mutableMapOf<Long, Double>()
        cards.forEach { card ->
            cardSpends[card.id] = allTx
                .filter { it.creditCardId == card.id && it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
        }

        // Bank Cash Flows
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

        // Real dynamic 4-month trend
        val trends = calculateTrends(allTx)

        val totalPending = borrowedItems.filter { !it.isSettled }.sumOf { it.remainingAmount }

        DashboardUiState(
            totalBalance = totalBalance,
            monthlyIncome = monthlyIncome,
            monthlyExpense = monthlyExpense,
            monthlyCreditCardSpend = monthlyCCSpend,
            recentTransactions = recents,
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
            repository.addTransaction(
                TransactionEntity(
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
            )
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
            repository.addBankAccount(
                BankAccountEntity(
                    bankName = bankName,
                    accountHolder = accountHolder,
                    accountNumberLast4 = accountNumberLast4,
                    accountType = accountType,
                    initialBalance = initialBalance,
                    colorHex = colorHex
                )
            )
        }
    }

    fun deleteBankAccount(bank: BankAccountEntity) {
        viewModelScope.launch {
            repository.deleteBankAccount(bank)
        }
    }

    fun testMongoConnection(onResult: (SyncResult) -> Unit = {}) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = MongoDbSyncHelper.testConnection()
            _syncStatus.value = result
            _isSyncing.value = false
            onResult(result)
        }
    }

    fun pushToMongoDb(onResult: (SyncResult) -> Unit = {}) {
        viewModelScope.launch {
            _isSyncing.value = true
            val txs = allTransactions.value
            val banks = allBankAccounts.value
            val cards = allCreditCards.value
            val recs = activeRecurringItems.value
            val borrowed = allBorrowedItems.value
            val email = _userProfile.value.email.ifBlank { "guest@moneytracker.app" }

            val result = MongoDbSyncHelper.pushAllData(
                userEmail = email,
                transactions = txs,
                bankAccounts = banks,
                creditCards = cards,
                recurringItems = recs,
                borrowedItems = borrowed
            )
            _syncStatus.value = result
            _isSyncing.value = false
            onResult(result)
        }
    }

    fun backupToGoogleDrive(context: android.content.Context) {
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

    fun exportToExcel(context: android.content.Context) {
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

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
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
            repository.addBorrowedItem(
                BorrowedItemEntity(
                    personName = personName,
                    totalAmount = totalAmount,
                    receiveMethod = receiveMethod,
                    dueDateMillis = dueDateMillis,
                    isSplit = isSplit,
                    splitCount = splitCount,
                    splitDetails = splitDetails,
                    notes = notes
                )
            )
        }
    }

    fun recordBorrowedRepayment(
        item: BorrowedItemEntity,
        amountReceived: Double,
        paymentMethod: PaymentMethod = PaymentMethod.UPI
    ) {
        viewModelScope.launch {
            repository.recordBorrowedRepayment(item, amountReceived, paymentMethod)
        }
    }

    fun deleteBorrowedItem(item: BorrowedItemEntity) {
        viewModelScope.launch {
            repository.deleteBorrowedItem(item)
        }
    }

    fun recordLoanPayment(loan: RecurringItemEntity) {
        viewModelScope.launch {
            repository.recordLoanPayment(loan)
        }
    }

    fun settleCreditCard(card: CreditCardEntity, amount: Double) {
        viewModelScope.launch {
            repository.settleCreditCardBill(card, amount)
        }
    }

    fun addCreditCard(card: CreditCardEntity) {
        viewModelScope.launch {
            repository.addCreditCard(card)
        }
    }

    fun addRecurringItem(item: RecurringItemEntity) {
        viewModelScope.launch {
            repository.addRecurringItem(item)
        }
    }

    fun deleteRecurringItem(item: RecurringItemEntity) {
        viewModelScope.launch {
            repository.deleteRecurringItem(item)
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

    class Factory(private val repository: MoneyTrackerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
