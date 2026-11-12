package com.moneytracker.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.moneytracker.app.model.Category
import com.moneytracker.app.model.CreditCardEntity
import com.moneytracker.app.model.PaymentMethod
import com.moneytracker.app.model.RecurringFrequency
import com.moneytracker.app.model.RecurringItemEntity
import com.moneytracker.app.model.RecurringType
import com.moneytracker.app.model.TransactionEntity
import com.moneytracker.app.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        TransactionEntity::class,
        CreditCardEntity::class,
        RecurringItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun recurringDao(): RecurringDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_tracker_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(db: AppDatabase) {
                val cardDao = db.creditCardDao()
                val transactionDao = db.transactionDao()
                val recurringDao = db.recurringDao()

                // 1. Initial Credit Cards
                val hdfcCardId = cardDao.insert(
                    CreditCardEntity(
                        cardName = "HDFC Regalia Gold",
                        bankName = "HDFC Bank",
                        last4Digits = "4892",
                        creditLimit = 400000.0,
                        billingCycleDay = 15,
                        paymentDueDay = 5,
                        cardGradientStart = 0xFF1E3A8A, // Navy
                        cardGradientEnd = 0xFF4338CA,   // Indigo
                        network = "VISA"
                    )
                )

                val iciciCardId = cardDao.insert(
                    CreditCardEntity(
                        cardName = "ICICI Amazon Pay",
                        bankName = "ICICI Bank",
                        last4Digits = "7154",
                        creditLimit = 250000.0,
                        billingCycleDay = 20,
                        paymentDueDay = 10,
                        cardGradientStart = 0xFF047857, // Emerald
                        cardGradientEnd = 0xFF0F766E,   // Teal
                        network = "MASTERCARD"
                    )
                )

                val sbiCardId = cardDao.insert(
                    CreditCardEntity(
                        cardName = "SBI SimplyCLICK",
                        bankName = "State Bank of India",
                        last4Digits = "3391",
                        creditLimit = 150000.0,
                        billingCycleDay = 1,
                        paymentDueDay = 21,
                        cardGradientStart = 0xFF9333EA, // Purple
                        cardGradientEnd = 0xFFC026D3,   // Fuchsia
                        network = "RUPAY"
                    )
                )

                // 2. Initial Recurring Items (Salary, Loans, Subscriptions)
                recurringDao.insert(
                    RecurringItemEntity(
                        title = "Monthly Salary Credit",
                        amount = 145000.0,
                        recurringType = RecurringType.SALARY,
                        frequency = RecurringFrequency.MONTHLY,
                        dueDayOfMonth = 1,
                        defaultPaymentMethod = PaymentMethod.NET_BANKING,
                        isLoan = false
                    )
                )

                recurringDao.insert(
                    RecurringItemEntity(
                        title = "Home Loan EMI (HDFC)",
                        amount = 38500.0,
                        recurringType = RecurringType.HOME_LOAN_EMI,
                        frequency = RecurringFrequency.MONTHLY,
                        dueDayOfMonth = 10,
                        defaultPaymentMethod = PaymentMethod.NET_BANKING,
                        isLoan = true,
                        totalTenureMonths = 240,
                        paidTenureMonths = 34,
                        originalLoanAmount = 4500000.0,
                        interestRatePercent = 8.5
                    )
                )

                recurringDao.insert(
                    RecurringItemEntity(
                        title = "Car Loan EMI (SBI)",
                        amount = 14200.0,
                        recurringType = RecurringType.CAR_LOAN_EMI,
                        frequency = RecurringFrequency.MONTHLY,
                        dueDayOfMonth = 5,
                        defaultPaymentMethod = PaymentMethod.UPI,
                        isLoan = true,
                        totalTenureMonths = 60,
                        paidTenureMonths = 22,
                        originalLoanAmount = 800000.0,
                        interestRatePercent = 9.1
                    )
                )

                recurringDao.insert(
                    RecurringItemEntity(
                        title = "Netflix Premium 4K",
                        amount = 649.0,
                        recurringType = RecurringType.SUBSCRIPTION,
                        frequency = RecurringFrequency.MONTHLY,
                        dueDayOfMonth = 12,
                        defaultPaymentMethod = PaymentMethod.CREDIT_CARD,
                        linkedCreditCardId = iciciCardId,
                        isLoan = false
                    )
                )

                recurringDao.insert(
                    RecurringItemEntity(
                        title = "High-Speed Fiber Internet",
                        amount = 1179.0,
                        recurringType = RecurringType.UTILITY_BILL,
                        frequency = RecurringFrequency.MONTHLY,
                        dueDayOfMonth = 18,
                        defaultPaymentMethod = PaymentMethod.CREDIT_CARD,
                        linkedCreditCardId = hdfcCardId,
                        isLoan = false
                    )
                )

                recurringDao.insert(
                    RecurringItemEntity(
                        title = "Nifty 50 Index Fund SIP",
                        amount = 15000.0,
                        recurringType = RecurringType.SIP_INVESTMENT,
                        frequency = RecurringFrequency.MONTHLY,
                        dueDayOfMonth = 7,
                        defaultPaymentMethod = PaymentMethod.UPI,
                        isLoan = false
                    )
                )

                // 3. Populate Recent Transactions for current and past months
                val now = Calendar.getInstance()
                val currentMillis = now.timeInMillis

                val sampleTransactions = listOf(
                    TransactionEntity(
                        title = "Monthly Salary Credited",
                        amount = 145000.0,
                        type = TransactionType.INCOME,
                        category = Category.SALARY,
                        paymentMethod = PaymentMethod.NET_BANKING,
                        dateMillis = currentMillis - (10L * 86400000L),
                        note = "Direct bank deposit"
                    ),
                    TransactionEntity(
                        title = "Home Loan EMI Auto-Debit",
                        amount = 38500.0,
                        type = TransactionType.EXPENSE,
                        category = Category.LOAN_EMI,
                        paymentMethod = PaymentMethod.NET_BANKING,
                        dateMillis = currentMillis - (2L * 86400000L),
                        note = "EMI 34 of 240"
                    ),
                    TransactionEntity(
                        title = "Nature's Basket Organic Groceries",
                        amount = 4850.0,
                        type = TransactionType.EXPENSE,
                        category = Category.GROCERIES,
                        paymentMethod = PaymentMethod.CREDIT_CARD,
                        creditCardId = iciciCardId,
                        dateMillis = currentMillis - (1L * 86400000L),
                        note = "Weekly pantry refill"
                    ),
                    TransactionEntity(
                        title = "Fuel & Petrol Station",
                        amount = 3200.0,
                        type = TransactionType.EXPENSE,
                        category = Category.TRANSPORT,
                        paymentMethod = PaymentMethod.CREDIT_CARD,
                        creditCardId = hdfcCardId,
                        dateMillis = currentMillis - (3L * 86400000L),
                        note = "Full tank refill"
                    ),
                    TransactionEntity(
                        title = "Weekend Fine Dining & Bistro",
                        amount = 3680.0,
                        type = TransactionType.EXPENSE,
                        category = Category.FOOD,
                        paymentMethod = PaymentMethod.CREDIT_CARD,
                        creditCardId = hdfcCardId,
                        dateMillis = currentMillis - (4L * 86400000L),
                        note = "Family dinner"
                    ),
                    TransactionEntity(
                        title = "Zomato Gourmet Lunch",
                        amount = 740.0,
                        type = TransactionType.EXPENSE,
                        category = Category.FOOD,
                        paymentMethod = PaymentMethod.UPI,
                        dateMillis = currentMillis - (5L * 86400000L),
                        note = "Office team lunch"
                    ),
                    TransactionEntity(
                        title = "Amazon Electronics & Gadgets",
                        amount = 12499.0,
                        type = TransactionType.EXPENSE,
                        category = Category.SHOPPING,
                        paymentMethod = PaymentMethod.CREDIT_CARD,
                        creditCardId = iciciCardId,
                        dateMillis = currentMillis - (6L * 86400000L),
                        note = "Noise Cancelling Headphones"
                    ),
                    TransactionEntity(
                        title = "Nifty 50 SIP Auto-Debit",
                        amount = 15000.0,
                        type = TransactionType.EXPENSE,
                        category = Category.INVESTMENT,
                        paymentMethod = PaymentMethod.UPI,
                        dateMillis = currentMillis - (7L * 86400000L),
                        note = "Wealth building"
                    ),
                    TransactionEntity(
                        title = "Morning Coffee & Snacks",
                        amount = 260.0,
                        type = TransactionType.EXPENSE,
                        category = Category.FOOD,
                        paymentMethod = PaymentMethod.CASH,
                        dateMillis = currentMillis - (1L * 3600000L),
                        note = "Cafe visit"
                    ),
                    TransactionEntity(
                        title = "PharmEasy Health Checkup & Meds",
                        amount = 2150.0,
                        type = TransactionType.EXPENSE,
                        category = Category.HEALTH,
                        paymentMethod = PaymentMethod.UPI,
                        dateMillis = currentMillis - (8L * 86400000L),
                        note = "Vitamins & prescription"
                    ),
                    TransactionEntity(
                        title = "Freelance UI Consulting",
                        amount = 32000.0,
                        type = TransactionType.INCOME,
                        category = Category.FREELANCE,
                        paymentMethod = PaymentMethod.UPI,
                        dateMillis = currentMillis - (9L * 86400000L),
                        note = "Client milestone payout"
                    ),
                    TransactionEntity(
                        title = "Flight Booking to Mumbai",
                        amount = 8900.0,
                        type = TransactionType.EXPENSE,
                        category = Category.TRANSPORT,
                        paymentMethod = PaymentMethod.CREDIT_CARD,
                        creditCardId = sbiCardId,
                        dateMillis = currentMillis - (12L * 86400000L),
                        note = "Conference travel"
                    )
                )

                transactionDao.insertAll(sampleTransactions)
            }
        }
    }
}
