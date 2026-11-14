package com.moneytracker.app.sync

import com.moneytracker.app.model.BankAccountEntity
import com.moneytracker.app.model.BankAccountType
import com.moneytracker.app.model.BorrowedItemEntity
import com.moneytracker.app.model.Category
import com.moneytracker.app.model.CreditCardEntity
import com.moneytracker.app.model.PaymentMethod
import com.moneytracker.app.model.RecurringFrequency
import com.moneytracker.app.model.RecurringItemEntity
import com.moneytracker.app.model.RecurringType
import com.moneytracker.app.model.TransactionEntity
import com.moneytracker.app.model.TransactionType
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import java.util.concurrent.TimeUnit

data class SyncResult(
    val isSuccess: Boolean,
    val message: String,
    val itemsSynced: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class UserDataSnapshot(
    val transactions: List<TransactionEntity> = emptyList(),
    val bankAccounts: List<BankAccountEntity> = emptyList(),
    val creditCards: List<CreditCardEntity> = emptyList(),
    val recurringItems: List<RecurringItemEntity> = emptyList(),
    val borrowedItems: List<BorrowedItemEntity> = emptyList()
)

object MongoDbSyncHelper {
    private const val MONGO_SRV_URI = "mongodb+srv://deekshithkr1905_db_user:kocXbPuY6LHiFGZy@cluster0.zrezwp0.mongodb.net/?retryWrites=true&w=majority"
    private const val MONGO_DIRECT_URI = "mongodb://deekshithkr1905_db_user:kocXbPuY6LHiFGZy@ac-iaix62n-shard-00-00.zrezwp0.mongodb.net:27017,ac-iaix62n-shard-00-01.zrezwp0.mongodb.net:27017,ac-iaix62n-shard-00-02.zrezwp0.mongodb.net:27017/?ssl=true&replicaSet=atlas-ag4r6i-shard-0&authSource=admin&retryWrites=true&w=majority"
    private const val DB_NAME = "money_tracker_db"

    @Volatile
    private var cachedClient: MongoClient? = null

    private fun getClient(): MongoClient {
        cachedClient?.let { return it }
        synchronized(this) {
            cachedClient?.let { return it }
            val client = try {
                val settings = MongoClientSettings.builder()
                    .applyConnectionString(ConnectionString(MONGO_SRV_URI))
                    .applyToSocketSettings { it.connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS) }
                    .build()
                MongoClients.create(settings)
            } catch (e: Exception) {
                val fallbackSettings = MongoClientSettings.builder()
                    .applyConnectionString(ConnectionString(MONGO_DIRECT_URI))
                    .applyToSocketSettings { it.connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS) }
                    .build()
                MongoClients.create(fallbackSettings)
            }
            cachedClient = client
            return client
        }
    }

    private fun sanitizeEmail(email: String): String {
        val trimmed = email.trim().lowercase()
        return if (trimmed.isBlank()) "guest_user" else trimmed.replace(".", "_").replace("@", "_at_")
    }

    suspend fun autoSyncTransaction(userEmail: String, tx: TransactionEntity) = withContext(Dispatchers.IO) {
        try {
            val userKey = sanitizeEmail(userEmail)
            val db = getClient().getDatabase(DB_NAME)
            val doc = Document()
                .append("_id", "${userKey}_tx_${tx.id}")
                .append("userEmail", userEmail)
                .append("localId", tx.id)
                .append("title", tx.title)
                .append("amount", tx.amount)
                .append("type", tx.type.name)
                .append("category", tx.category.name)
                .append("paymentMethod", tx.paymentMethod.name)
                .append("bankAccountId", tx.bankAccountId)
                .append("bankName", tx.bankName)
                .append("creditCardId", tx.creditCardId)
                .append("dateMillis", tx.dateMillis)
                .append("note", tx.note)
                .append("updatedAt", System.currentTimeMillis())
            db.getCollection("transactions").replaceOne(
                Filters.eq("_id", doc.getString("_id")),
                doc,
                ReplaceOptions().upsert(true)
            )
        } catch (_: Exception) {}
    }

    suspend fun autoDeleteTransaction(userEmail: String, txId: Long) = withContext(Dispatchers.IO) {
        try {
            val userKey = sanitizeEmail(userEmail)
            getClient().getDatabase(DB_NAME).getCollection("transactions")
                .deleteOne(Filters.eq("_id", "${userKey}_tx_$txId"))
        } catch (_: Exception) {}
    }

    suspend fun autoSyncBankAccount(userEmail: String, bank: BankAccountEntity) = withContext(Dispatchers.IO) {
        try {
            val userKey = sanitizeEmail(userEmail)
            val db = getClient().getDatabase(DB_NAME)
            val doc = Document()
                .append("_id", "${userKey}_bank_${bank.id}")
                .append("userEmail", userEmail)
                .append("localId", bank.id)
                .append("bankName", bank.bankName)
                .append("accountHolder", bank.accountHolder)
                .append("accountNumberLast4", bank.accountNumberLast4)
                .append("accountType", bank.accountType.name)
                .append("initialBalance", bank.initialBalance)
                .append("colorHex", bank.colorHex)
                .append("isPrimary", bank.isPrimary)
                .append("updatedAt", System.currentTimeMillis())
            db.getCollection("bank_accounts").replaceOne(
                Filters.eq("_id", doc.getString("_id")),
                doc,
                ReplaceOptions().upsert(true)
            )
        } catch (_: Exception) {}
    }

    suspend fun autoDeleteBankAccount(userEmail: String, bankId: Long) = withContext(Dispatchers.IO) {
        try {
            val userKey = sanitizeEmail(userEmail)
            getClient().getDatabase(DB_NAME).getCollection("bank_accounts")
                .deleteOne(Filters.eq("_id", "${userKey}_bank_$bankId"))
        } catch (_: Exception) {}
    }

    suspend fun autoSyncCreditCard(userEmail: String, card: CreditCardEntity) = withContext(Dispatchers.IO) {
        try {
            val userKey = sanitizeEmail(userEmail)
            val db = getClient().getDatabase(DB_NAME)
            val doc = Document()
                .append("_id", "${userKey}_card_${card.id}")
                .append("userEmail", userEmail)
                .append("localId", card.id)
                .append("cardName", card.cardName)
                .append("bankName", card.bankName)
                .append("last4Digits", card.last4Digits)
                .append("creditLimit", card.creditLimit)
                .append("billingCycleDay", card.billingCycleDay)
                .append("paymentDueDay", card.paymentDueDay)
                .append("network", card.network)
                .append("updatedAt", System.currentTimeMillis())
            db.getCollection("credit_cards").replaceOne(
                Filters.eq("_id", doc.getString("_id")),
                doc,
                ReplaceOptions().upsert(true)
            )
        } catch (_: Exception) {}
    }

    suspend fun autoSyncRecurringItem(userEmail: String, rec: RecurringItemEntity) = withContext(Dispatchers.IO) {
        try {
            val userKey = sanitizeEmail(userEmail)
            val db = getClient().getDatabase(DB_NAME)
            val doc = Document()
                .append("_id", "${userKey}_rec_${rec.id}")
                .append("userEmail", userEmail)
                .append("localId", rec.id)
                .append("title", rec.title)
                .append("amount", rec.amount)
                .append("recurringType", rec.recurringType.name)
                .append("frequency", rec.frequency.name)
                .append("dueDayOfMonth", rec.dueDayOfMonth)
                .append("defaultPaymentMethod", rec.defaultPaymentMethod.name)
                .append("nextDueDateMillis", rec.nextDueDateMillis)
                .append("isActive", rec.isActive)
                .append("isLoan", rec.isLoan)
                .append("totalTenureMonths", rec.totalTenureMonths)
                .append("paidTenureMonths", rec.paidTenureMonths)
                .append("interestRatePercent", rec.interestRatePercent)
                .append("updatedAt", System.currentTimeMillis())
            db.getCollection("recurring_bills").replaceOne(
                Filters.eq("_id", doc.getString("_id")),
                doc,
                ReplaceOptions().upsert(true)
            )
        } catch (_: Exception) {}
    }

    suspend fun autoDeleteRecurringItem(userEmail: String, recId: Long) = withContext(Dispatchers.IO) {
        try {
            val userKey = sanitizeEmail(userEmail)
            getClient().getDatabase(DB_NAME).getCollection("recurring_bills")
                .deleteOne(Filters.eq("_id", "${userKey}_rec_$recId"))
        } catch (_: Exception) {}
    }

    suspend fun autoSyncBorrowedItem(userEmail: String, borrowed: BorrowedItemEntity) = withContext(Dispatchers.IO) {
        try {
            val userKey = sanitizeEmail(userEmail)
            val db = getClient().getDatabase(DB_NAME)
            val doc = Document()
                .append("_id", "${userKey}_borrowed_${borrowed.id}")
                .append("userEmail", userEmail)
                .append("localId", borrowed.id)
                .append("personName", borrowed.personName)
                .append("totalAmount", borrowed.totalAmount)
                .append("amountReceived", borrowed.amountReceived)
                .append("receiveMethod", borrowed.receiveMethod)
                .append("dueDateMillis", borrowed.dueDateMillis)
                .append("isSplit", borrowed.isSplit)
                .append("splitCount", borrowed.splitCount)
                .append("splitDetails", borrowed.splitDetails)
                .append("isSettled", borrowed.isSettled)
                .append("notes", borrowed.notes)
                .append("updatedAt", System.currentTimeMillis())
            db.getCollection("borrowed_items").replaceOne(
                Filters.eq("_id", doc.getString("_id")),
                doc,
                ReplaceOptions().upsert(true)
            )
        } catch (_: Exception) {}
    }

    suspend fun autoDeleteBorrowedItem(userEmail: String, borrowedId: Long) = withContext(Dispatchers.IO) {
        try {
            val userKey = sanitizeEmail(userEmail)
            getClient().getDatabase(DB_NAME).getCollection("borrowed_items")
                .deleteOne(Filters.eq("_id", "${userKey}_borrowed_$borrowedId"))
        } catch (_: Exception) {}
    }

    suspend fun pullUserData(userEmail: String): UserDataSnapshot? = withContext(Dispatchers.IO) {
        try {
            val db = getClient().getDatabase(DB_NAME)
            val txList = mutableListOf<TransactionEntity>()
            db.getCollection("transactions").find(Filters.eq("userEmail", userEmail)).forEach { doc ->
                txList.add(
                    TransactionEntity(
                        title = doc.getString("title") ?: "Transaction",
                        amount = doc.getDouble("amount") ?: 0.0,
                        type = try { TransactionType.valueOf(doc.getString("type") ?: "EXPENSE") } catch (_: Exception) { TransactionType.EXPENSE },
                        category = try { Category.valueOf(doc.getString("category") ?: "OTHER") } catch (_: Exception) { Category.OTHER },
                        paymentMethod = try { PaymentMethod.valueOf(doc.getString("paymentMethod") ?: "UPI") } catch (_: Exception) { PaymentMethod.UPI },
                        bankAccountId = doc.getLong("bankAccountId"),
                        bankName = doc.getString("bankName"),
                        creditCardId = doc.getLong("creditCardId"),
                        dateMillis = doc.getLong("dateMillis") ?: System.currentTimeMillis(),
                        note = doc.getString("note") ?: ""
                    )
                )
            }

            val bankList = mutableListOf<BankAccountEntity>()
            db.getCollection("bank_accounts").find(Filters.eq("userEmail", userEmail)).forEach { doc ->
                bankList.add(
                    BankAccountEntity(
                        bankName = doc.getString("bankName") ?: "Bank",
                        accountHolder = doc.getString("accountHolder") ?: "",
                        accountNumberLast4 = doc.getString("accountNumberLast4") ?: "",
                        accountType = try { BankAccountType.valueOf(doc.getString("accountType") ?: "SAVINGS") } catch (_: Exception) { BankAccountType.SAVINGS },
                        initialBalance = doc.getDouble("initialBalance") ?: 0.0,
                        colorHex = doc.getString("colorHex") ?: "#10B981",
                        isPrimary = doc.getBoolean("isPrimary") ?: false
                    )
                )
            }

            val cardList = mutableListOf<CreditCardEntity>()
            db.getCollection("credit_cards").find(Filters.eq("userEmail", userEmail)).forEach { doc ->
                cardList.add(
                    CreditCardEntity(
                        cardName = doc.getString("cardName") ?: "Credit Card",
                        bankName = doc.getString("bankName") ?: "",
                        last4Digits = doc.getString("last4Digits") ?: "0000",
                        creditLimit = doc.getDouble("creditLimit") ?: 50000.0,
                        billingCycleDay = doc.getInteger("billingCycleDay") ?: 1,
                        paymentDueDay = doc.getInteger("paymentDueDay") ?: 20,
                        network = doc.getString("network") ?: "VISA"
                    )
                )
            }

            val recList = mutableListOf<RecurringItemEntity>()
            db.getCollection("recurring_bills").find(Filters.eq("userEmail", userEmail)).forEach { doc ->
                recList.add(
                    RecurringItemEntity(
                        title = doc.getString("title") ?: "Recurring",
                        amount = doc.getDouble("amount") ?: 0.0,
                        recurringType = try { RecurringType.valueOf(doc.getString("recurringType") ?: "SUBSCRIPTION") } catch (_: Exception) { RecurringType.SUBSCRIPTION },
                        frequency = try { RecurringFrequency.valueOf(doc.getString("frequency") ?: "MONTHLY") } catch (_: Exception) { RecurringFrequency.MONTHLY },
                        dueDayOfMonth = doc.getInteger("dueDayOfMonth") ?: 1,
                        defaultPaymentMethod = try { PaymentMethod.valueOf(doc.getString("defaultPaymentMethod") ?: "UPI") } catch (_: Exception) { PaymentMethod.UPI },
                        isLoan = doc.getBoolean("isLoan") ?: false,
                        totalTenureMonths = doc.getInteger("totalTenureMonths") ?: 0,
                        paidTenureMonths = doc.getInteger("paidTenureMonths") ?: 0,
                        interestRatePercent = doc.getDouble("interestRatePercent") ?: 0.0,
                        nextDueDateMillis = doc.getLong("nextDueDateMillis") ?: System.currentTimeMillis()
                    )
                )
            }

            val borrowedList = mutableListOf<BorrowedItemEntity>()
            db.getCollection("borrowed_items").find(Filters.eq("userEmail", userEmail)).forEach { doc ->
                borrowedList.add(
                    BorrowedItemEntity(
                        personName = doc.getString("personName") ?: "Friend",
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                        amountReceived = doc.getDouble("amountReceived") ?: 0.0,
                        receiveMethod = doc.getString("receiveMethod") ?: "UPI",
                        dueDateMillis = doc.getLong("dueDateMillis") ?: System.currentTimeMillis(),
                        isSplit = doc.getBoolean("isSplit") ?: false,
                        splitCount = doc.getInteger("splitCount") ?: 1,
                        splitDetails = doc.getString("splitDetails") ?: "",
                        isSettled = doc.getBoolean("isSettled") ?: false,
                        notes = doc.getString("notes") ?: ""
                    )
                )
            }

            UserDataSnapshot(txList, bankList, cardList, recList, borrowedList)
        } catch (_: Exception) {
            null
        }
    }
}
