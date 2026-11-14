package com.moneytracker.app.sync

import com.moneytracker.app.model.BankAccountEntity
import com.moneytracker.app.model.BorrowedItemEntity
import com.moneytracker.app.model.CreditCardEntity
import com.moneytracker.app.model.RecurringItemEntity
import com.moneytracker.app.model.TransactionEntity
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
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

object MongoDbSyncHelper {
    // Primary SRV connection string provided by user
    private const val MONGO_SRV_URI = "mongodb+srv://deekshithkr1905_db_user:kocXbPuY6LHiFGZy@cluster0.zrezwp0.mongodb.net/?retryWrites=true&w=majority"

    // Direct standard connection string as fallback for Android JVM environments without JNDI DNS-SRV
    private const val MONGO_DIRECT_URI = "mongodb://deekshithkr1905_db_user:kocXbPuY6LHiFGZy@ac-iaix62n-shard-00-00.zrezwp0.mongodb.net:27017,ac-iaix62n-shard-00-01.zrezwp0.mongodb.net:27017,ac-iaix62n-shard-00-02.zrezwp0.mongodb.net:27017/?ssl=true&replicaSet=atlas-ag4r6i-shard-0&authSource=admin&retryWrites=true&w=majority"

    private const val DB_NAME = "money_tracker_db"

    private fun getClient(): MongoClient {
        return try {
            val settings = MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(MONGO_SRV_URI))
                .applyToSocketSettings { it.connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS) }
                .build()
            MongoClients.create(settings)
        } catch (e: Exception) {
            // Fallback to direct replica set hosts
            val fallbackSettings = MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(MONGO_DIRECT_URI))
                .applyToSocketSettings { it.connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS) }
                .build()
            MongoClients.create(fallbackSettings)
        }
    }

    suspend fun testConnection(): SyncResult = withContext(Dispatchers.IO) {
        var client: MongoClient? = null
        try {
            client = getClient()
            val db = client.getDatabase(DB_NAME)
            val ping = db.runCommand(Document("ping", 1))
            val ok = ping.getDouble("ok") ?: 1.0
            if (ok >= 1.0) {
                SyncResult(true, "Successfully connected to MongoDB Atlas (cluster0.zrezwp0)!")
            } else {
                SyncResult(false, "MongoDB connection ping returned: $ok")
            }
        } catch (e: Exception) {
            // If primary throws JNDI error, try fallback directly
            try {
                val fallbackSettings = MongoClientSettings.builder()
                    .applyConnectionString(ConnectionString(MONGO_DIRECT_URI))
                    .applyToSocketSettings { it.connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS) }
                    .build()
                client = MongoClients.create(fallbackSettings)
                val db = client.getDatabase(DB_NAME)
                db.runCommand(Document("ping", 1))
                SyncResult(true, "Successfully connected to MongoDB Atlas via Direct Shard connection!")
            } catch (fallbackEx: Exception) {
                SyncResult(false, "Connection error: ${fallbackEx.localizedMessage ?: fallbackEx.message}")
            }
        } finally {
            try { client?.close() } catch (_: Exception) {}
        }
    }

    suspend fun pushAllData(
        userEmail: String,
        transactions: List<TransactionEntity>,
        bankAccounts: List<BankAccountEntity>,
        creditCards: List<CreditCardEntity>,
        recurringItems: List<RecurringItemEntity>,
        borrowedItems: List<BorrowedItemEntity>
    ): SyncResult = withContext(Dispatchers.IO) {
        var client: MongoClient? = null
        try {
            client = try {
                getClient()
            } catch (e: Exception) {
                val fallbackSettings = MongoClientSettings.builder()
                    .applyConnectionString(ConnectionString(MONGO_DIRECT_URI))
                    .applyToSocketSettings { it.connectTimeout(15, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS) }
                    .build()
                MongoClients.create(fallbackSettings)
            }

            val db = client.getDatabase(DB_NAME)
            val replaceOptions = ReplaceOptions().upsert(true)
            val now = System.currentTimeMillis()
            var totalCount = 0

            // 1. Transactions collection
            val txCol = db.getCollection("transactions")
            transactions.forEach { tx ->
                val doc = Document()
                    .append("_id", "tx_${tx.id}_${userEmail.ifBlank { "default" }}")
                    .append("localId", tx.id)
                    .append("userEmail", userEmail)
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
                    .append("isRecurring", tx.isRecurring)
                    .append("syncedAt", now)
                txCol.replaceOne(Document("_id", doc.getString("_id")), doc, replaceOptions)
                totalCount++
            }

            // 2. Bank Accounts collection
            val bankCol = db.getCollection("bank_accounts")
            bankAccounts.forEach { bank ->
                val doc = Document()
                    .append("_id", "bank_${bank.id}_${userEmail.ifBlank { "default" }}")
                    .append("localId", bank.id)
                    .append("userEmail", userEmail)
                    .append("bankName", bank.bankName)
                    .append("accountHolder", bank.accountHolder)
                    .append("accountNumberLast4", bank.accountNumberLast4)
                    .append("accountType", bank.accountType.name)
                    .append("initialBalance", bank.initialBalance)
                    .append("colorHex", bank.colorHex)
                    .append("isPrimary", bank.isPrimary)
                    .append("syncedAt", now)
                bankCol.replaceOne(Document("_id", doc.getString("_id")), doc, replaceOptions)
                totalCount++
            }

            // 3. Credit Cards collection
            val cardCol = db.getCollection("credit_cards")
            creditCards.forEach { card ->
                val doc = Document()
                    .append("_id", "card_${card.id}_${userEmail.ifBlank { "default" }}")
                    .append("localId", card.id)
                    .append("userEmail", userEmail)
                    .append("cardName", card.cardName)
                    .append("last4Digits", card.last4Digits)
                    .append("creditLimit", card.creditLimit)
                    .append("billingCycleDay", card.billingCycleDay)
                    .append("paymentDueDay", card.paymentDueDay)
                    .append("cardGradientStart", card.cardGradientStart)
                    .append("cardGradientEnd", card.cardGradientEnd)
                    .append("network", card.network)
                    .append("syncedAt", now)
                cardCol.replaceOne(Document("_id", doc.getString("_id")), doc, replaceOptions)
                totalCount++
            }

            // 4. Recurring Bills collection
            val recurringCol = db.getCollection("recurring_bills")
            recurringItems.forEach { rec ->
                val doc = Document()
                    .append("_id", "rec_${rec.id}_${userEmail.ifBlank { "default" }}")
                    .append("localId", rec.id)
                    .append("userEmail", userEmail)
                    .append("title", rec.title)
                    .append("amount", rec.amount)
                    .append("recurringType", rec.recurringType.name)
                    .append("frequency", rec.frequency.name)
                    .append("dueDayOfMonth", rec.dueDayOfMonth)
                    .append("defaultPaymentMethod", rec.defaultPaymentMethod.name)
                    .append("nextDueDateMillis", rec.nextDueDateMillis)
                    .append("isActive", rec.isActive)
                    .append("isLoan", rec.isLoan)
                    .append("syncedAt", now)
                recurringCol.replaceOne(Document("_id", doc.getString("_id")), doc, replaceOptions)
                totalCount++
            }

            // 5. Borrowed & Splits collection
            val borrowedCol = db.getCollection("borrowed_items")
            borrowedItems.forEach { borrowed ->
                val doc = Document()
                    .append("_id", "borrowed_${borrowed.id}_${userEmail.ifBlank { "default" }}")
                    .append("localId", borrowed.id)
                    .append("userEmail", userEmail)
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
                    .append("syncedAt", now)
                borrowedCol.replaceOne(Document("_id", doc.getString("_id")), doc, replaceOptions)
                totalCount++
            }

            // 6. Sync metadata collection
            val metaCol = db.getCollection("sync_metadata")
            val metaDoc = Document()
                .append("_id", "meta_${userEmail.ifBlank { "default" }}")
                .append("userEmail", userEmail)
                .append("lastSyncMillis", now)
                .append("totalItemsSynced", totalCount)
            metaCol.replaceOne(Document("_id", metaDoc.getString("_id")), metaDoc, replaceOptions)

            SyncResult(
                isSuccess = true,
                message = "Synced $totalCount items to MongoDB Atlas successfully!",
                itemsSynced = totalCount,
                timestamp = now
            )
        } catch (e: Exception) {
            e.printStackTrace()
            SyncResult(
                isSuccess = false,
                message = "MongoDB sync failed: ${e.localizedMessage ?: e.message}",
                timestamp = System.currentTimeMillis()
            )
        } finally {
            try { client?.close() } catch (_: Exception) {}
        }
    }
}
