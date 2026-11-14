package com.moneytracker.app.sync

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class UserProfile(
    val isSignedIn: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = ""
)

object GoogleAuthDriveHelper {
    private const val PREFS_NAME = "money_tracker_user_prefs"
    private const val KEY_SIGNED_IN = "is_signed_in"
    private const val KEY_NAME = "user_name"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_PHOTO = "user_photo"
    private const val KEY_DRIVE_LAST_BACKUP = "drive_last_backup"

    fun getGoogleSignInClient(context: Context): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    fun saveUserAccount(context: Context, account: GoogleSignInAccount) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_SIGNED_IN, true)
            .putString(KEY_NAME, account.displayName ?: "")
            .putString(KEY_EMAIL, account.email ?: "")
            .putString(KEY_PHOTO, account.photoUrl?.toString() ?: "")
            .apply()
    }

    fun saveManualUser(context: Context, name: String, email: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_SIGNED_IN, true)
            .putString(KEY_NAME, name.trim())
            .putString(KEY_EMAIL, email.trim())
            .putString(KEY_PHOTO, "")
            .apply()
    }

    fun getUserProfile(context: Context): UserProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isSignedIn = prefs.getBoolean(KEY_SIGNED_IN, false)
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val name = prefs.getString(KEY_NAME, "") ?: ""
        val photo = prefs.getString(KEY_PHOTO, "") ?: ""
        if (isSignedIn && email.isNotBlank()) {
            return UserProfile(
                isSignedIn = true,
                displayName = name.ifBlank { "User" },
                email = email,
                photoUrl = photo
            )
        }
        val lastAccount = try {
            GoogleSignIn.getLastSignedInAccount(context)
        } catch (_: Exception) { null }
        if (lastAccount != null) {
            return UserProfile(
                isSignedIn = true,
                displayName = lastAccount.displayName ?: "Google User",
                email = lastAccount.email ?: "",
                photoUrl = lastAccount.photoUrl?.toString() ?: ""
            )
        }
        return UserProfile()
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        try {
            getGoogleSignInClient(context).signOut().addOnCompleteListener {
                onComplete()
            }
        } catch (_: Exception) {
            onComplete()
        }
    }

    fun getLastDriveBackupTime(context: Context): Long {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_DRIVE_LAST_BACKUP, 0L)
    }

    fun setLastDriveBackupTime(context: Context, timeMillis: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_DRIVE_LAST_BACKUP, timeMillis)
            .apply()
    }

    fun createBackupJsonFile(
        context: Context,
        transactions: List<TransactionEntity>,
        bankAccounts: List<BankAccountEntity>,
        creditCards: List<CreditCardEntity>,
        recurringItems: List<RecurringItemEntity>,
        borrowedItems: List<BorrowedItemEntity>
    ): File? {
        val root = JSONObject()
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(now))

        root.put("version", 3)
        root.put("app", "MoneyTracker")
        root.put("backupDate", dateStr)
        root.put("timestamp", now)

        // Transactions
        val txArr = JSONArray()
        transactions.forEach { tx ->
            val obj = JSONObject()
            obj.put("id", tx.id)
            obj.put("title", tx.title)
            obj.put("amount", tx.amount)
            obj.put("type", tx.type.name)
            obj.put("category", tx.category.name)
            obj.put("paymentMethod", tx.paymentMethod.name)
            obj.put("bankAccountId", tx.bankAccountId ?: JSONObject.NULL)
            obj.put("bankName", tx.bankName ?: JSONObject.NULL)
            obj.put("creditCardId", tx.creditCardId ?: JSONObject.NULL)
            obj.put("dateMillis", tx.dateMillis)
            obj.put("note", tx.note)
            txArr.put(obj)
        }
        root.put("transactions", txArr)

        // Banks
        val bankArr = JSONArray()
        bankAccounts.forEach { bank ->
            val obj = JSONObject()
            obj.put("id", bank.id)
            obj.put("bankName", bank.bankName)
            obj.put("accountHolder", bank.accountHolder)
            obj.put("accountNumberLast4", bank.accountNumberLast4)
            obj.put("accountType", bank.accountType.name)
            obj.put("initialBalance", bank.initialBalance)
            obj.put("colorHex", bank.colorHex)
            obj.put("isPrimary", bank.isPrimary)
            bankArr.put(obj)
        }
        root.put("bankAccounts", bankArr)

        // Credit Cards
        val cardArr = JSONArray()
        creditCards.forEach { card ->
            val obj = JSONObject()
            obj.put("id", card.id)
            obj.put("cardName", card.cardName)
            obj.put("bankName", card.bankName)
            obj.put("last4Digits", card.last4Digits)
            obj.put("creditLimit", card.creditLimit)
            obj.put("billingCycleDay", card.billingCycleDay)
            obj.put("paymentDueDay", card.paymentDueDay)
            obj.put("network", card.network)
            cardArr.put(obj)
        }
        root.put("creditCards", cardArr)

        // Recurring
        val recArr = JSONArray()
        recurringItems.forEach { rec ->
            val obj = JSONObject()
            obj.put("id", rec.id)
            obj.put("title", rec.title)
            obj.put("amount", rec.amount)
            obj.put("recurringType", rec.recurringType.name)
            obj.put("frequency", rec.frequency.name)
            obj.put("dueDayOfMonth", rec.dueDayOfMonth)
            obj.put("defaultPaymentMethod", rec.defaultPaymentMethod.name)
            obj.put("isActive", rec.isActive)
            obj.put("isLoan", rec.isLoan)
            obj.put("totalTenureMonths", rec.totalTenureMonths)
            obj.put("paidTenureMonths", rec.paidTenureMonths)
            obj.put("interestRatePercent", rec.interestRatePercent)
            obj.put("nextDueDateMillis", rec.nextDueDateMillis)
            recArr.put(obj)
        }
        root.put("recurringItems", recArr)

        // Borrowed
        val borrowedArr = JSONArray()
        borrowedItems.forEach { b ->
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("personName", b.personName)
            obj.put("totalAmount", b.totalAmount)
            obj.put("amountReceived", b.amountReceived)
            obj.put("receiveMethod", b.receiveMethod)
            obj.put("dueDateMillis", b.dueDateMillis)
            obj.put("isSplit", b.isSplit)
            obj.put("splitCount", b.splitCount)
            obj.put("splitDetails", b.splitDetails)
            obj.put("isSettled", b.isSettled)
            obj.put("notes", b.notes)
            borrowedArr.put(obj)
        }
        root.put("borrowedItems", borrowedArr)

        val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(now))
        val file = File(context.cacheDir, "MoneyTracker_Backup_$timestampStr.json")
        return try {
            FileOutputStream(file).use { fos ->
                fos.write(root.toString(2).toByteArray(Charsets.UTF_8))
            }
            setLastDriveBackupTime(context, now)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun parseBackupJson(jsonString: String): UserDataSnapshot? {
        return try {
            val root = JSONObject(jsonString)

            val txList = mutableListOf<TransactionEntity>()
            val txArr = root.optJSONArray("transactions")
            if (txArr != null) {
                for (i in 0 until txArr.length()) {
                    val obj = txArr.getJSONObject(i)
                    txList.add(
                        TransactionEntity(
                            title = obj.optString("title", "Transaction"),
                            amount = obj.optDouble("amount", 0.0),
                            type = try { TransactionType.valueOf(obj.optString("type", "EXPENSE")) } catch (_: Exception) { TransactionType.EXPENSE },
                            category = try { Category.valueOf(obj.optString("category", "OTHER")) } catch (_: Exception) { Category.OTHER },
                            paymentMethod = try { PaymentMethod.valueOf(obj.optString("paymentMethod", "UPI")) } catch (_: Exception) { PaymentMethod.UPI },
                            bankAccountId = if (obj.has("bankAccountId") && !obj.isNull("bankAccountId")) obj.optLong("bankAccountId") else null,
                            bankName = if (obj.has("bankName") && !obj.isNull("bankName")) obj.optString("bankName") else null,
                            creditCardId = if (obj.has("creditCardId") && !obj.isNull("creditCardId")) obj.optLong("creditCardId") else null,
                            dateMillis = obj.optLong("dateMillis", System.currentTimeMillis()),
                            note = obj.optString("note", "")
                        )
                    )
                }
            }

            val bankList = mutableListOf<BankAccountEntity>()
            val bankArr = root.optJSONArray("bankAccounts")
            if (bankArr != null) {
                for (i in 0 until bankArr.length()) {
                    val obj = bankArr.getJSONObject(i)
                    bankList.add(
                        BankAccountEntity(
                            bankName = obj.optString("bankName", "Bank"),
                            accountHolder = obj.optString("accountHolder", ""),
                            accountNumberLast4 = obj.optString("accountNumberLast4", ""),
                            accountType = try { BankAccountType.valueOf(obj.optString("accountType", "SAVINGS")) } catch (_: Exception) { BankAccountType.SAVINGS },
                            initialBalance = obj.optDouble("initialBalance", 0.0),
                            colorHex = obj.optString("colorHex", "#10B981"),
                            isPrimary = obj.optBoolean("isPrimary", false)
                        )
                    )
                }
            }

            val cardList = mutableListOf<CreditCardEntity>()
            val cardArr = root.optJSONArray("creditCards")
            if (cardArr != null) {
                for (i in 0 until cardArr.length()) {
                    val obj = cardArr.getJSONObject(i)
                    cardList.add(
                        CreditCardEntity(
                            cardName = obj.optString("cardName", "Card"),
                            bankName = obj.optString("bankName", ""),
                            last4Digits = obj.optString("last4Digits", "0000"),
                            creditLimit = obj.optDouble("creditLimit", 50000.0),
                            billingCycleDay = obj.optInt("billingCycleDay", 1),
                            paymentDueDay = obj.optInt("paymentDueDay", 20),
                            network = obj.optString("network", "VISA")
                        )
                    )
                }
            }

            val recList = mutableListOf<RecurringItemEntity>()
            val recArr = root.optJSONArray("recurringItems")
            if (recArr != null) {
                for (i in 0 until recArr.length()) {
                    val obj = recArr.getJSONObject(i)
                    recList.add(
                        RecurringItemEntity(
                            title = obj.optString("title", "Recurring"),
                            amount = obj.optDouble("amount", 0.0),
                            recurringType = try { RecurringType.valueOf(obj.optString("recurringType", "SUBSCRIPTION")) } catch (_: Exception) { RecurringType.SUBSCRIPTION },
                            frequency = try { RecurringFrequency.valueOf(obj.optString("frequency", "MONTHLY")) } catch (_: Exception) { RecurringFrequency.MONTHLY },
                            dueDayOfMonth = obj.optInt("dueDayOfMonth", 1),
                            defaultPaymentMethod = try { PaymentMethod.valueOf(obj.optString("defaultPaymentMethod", "UPI")) } catch (_: Exception) { PaymentMethod.UPI },
                            isActive = obj.optBoolean("isActive", true),
                            isLoan = obj.optBoolean("isLoan", false),
                            totalTenureMonths = obj.optInt("totalTenureMonths", 0),
                            paidTenureMonths = obj.optInt("paidTenureMonths", 0),
                            interestRatePercent = obj.optDouble("interestRatePercent", 0.0),
                            nextDueDateMillis = obj.optLong("nextDueDateMillis", System.currentTimeMillis())
                        )
                    )
                }
            }

            val borrowedList = mutableListOf<BorrowedItemEntity>()
            val borrowedArr = root.optJSONArray("borrowedItems")
            if (borrowedArr != null) {
                for (i in 0 until borrowedArr.length()) {
                    val obj = borrowedArr.getJSONObject(i)
                    borrowedList.add(
                        BorrowedItemEntity(
                            personName = obj.optString("personName", "Friend"),
                            totalAmount = obj.optDouble("totalAmount", 0.0),
                            amountReceived = obj.optDouble("amountReceived", 0.0),
                            receiveMethod = obj.optString("receiveMethod", "UPI"),
                            dueDateMillis = obj.optLong("dueDateMillis", System.currentTimeMillis()),
                            isSplit = obj.optBoolean("isSplit", false),
                            splitCount = obj.optInt("splitCount", 1),
                            splitDetails = obj.optString("splitDetails", ""),
                            isSettled = obj.optBoolean("isSettled", false),
                            notes = obj.optString("notes", "")
                        )
                    )
                }
            }

            UserDataSnapshot(txList, bankList, cardList, recList, borrowedList)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun restoreBackupFromUri(context: Context, uri: Uri): UserDataSnapshot? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            if (inputStream != null) {
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                val jsonStr = reader.readText()
                reader.close()
                inputStream.close()
                parseBackupJson(jsonStr)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun launchGoogleDriveBackup(context: Context, backupFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Money Tracker Cloud Backup")
            putExtra(Intent.EXTRA_TEXT, "Money Tracker Google Drive Backup File: ${backupFile.name}")
            setPackage("com.google.android.apps.docs")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            val chooser = Intent.createChooser(intent, "Backup to Google Drive")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            val genericIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Money Tracker Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val fallbackChooser = Intent.createChooser(genericIntent, "Save Backup to Google Drive / Cloud")
            fallbackChooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallbackChooser)
        }
    }
}
