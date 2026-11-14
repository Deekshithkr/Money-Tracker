package com.moneytracker.app.sync

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.moneytracker.app.model.BankAccountEntity
import com.moneytracker.app.model.BorrowedItemEntity
import com.moneytracker.app.model.CreditCardEntity
import com.moneytracker.app.model.RecurringItemEntity
import com.moneytracker.app.model.TransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
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

    fun getUserProfile(context: Context): UserProfile {
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null) {
            return UserProfile(
                isSignedIn = true,
                displayName = lastAccount.displayName ?: "Google User",
                email = lastAccount.email ?: "",
                photoUrl = lastAccount.photoUrl?.toString() ?: ""
            )
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isSignedIn = prefs.getBoolean(KEY_SIGNED_IN, false)
        return UserProfile(
            isSignedIn = isSignedIn,
            displayName = prefs.getString(KEY_NAME, "") ?: "",
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            photoUrl = prefs.getString(KEY_PHOTO, "") ?: ""
        )
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        getGoogleSignInClient(context).signOut().addOnCompleteListener {
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
            obj.put("splitCount", b.splitCount)
            obj.put("splitDetails", b.splitDetails)
            obj.put("isSettled", b.isSettled)
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
            // Target Google Drive if installed
            setPackage("com.google.android.apps.docs")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            val chooser = Intent.createChooser(intent, "Backup to Google Drive")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            // If Drive package not directly found, open generic chooser
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
