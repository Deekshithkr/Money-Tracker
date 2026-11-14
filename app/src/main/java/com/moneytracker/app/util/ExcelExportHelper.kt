package com.moneytracker.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.moneytracker.app.model.BankAccountEntity
import com.moneytracker.app.model.BorrowedItemEntity
import com.moneytracker.app.model.CreditCardEntity
import com.moneytracker.app.model.RecurringItemEntity
import com.moneytracker.app.model.TransactionEntity
import com.moneytracker.app.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExportHelper {

    fun exportToExcel(
        context: Context,
        transactions: List<TransactionEntity>,
        bankAccounts: List<BankAccountEntity>,
        creditCards: List<CreditCardEntity>,
        recurringItems: List<RecurringItemEntity>,
        borrowedItems: List<BorrowedItemEntity>
    ): File? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = fileDateFormat.format(Date())
        val fileName = "MoneyTracker_Report_$timestamp.csv"

        val file = File(context.cacheDir, fileName)
        val sb = StringBuilder()

        // Write UTF-8 Byte Order Mark (BOM) so Microsoft Excel opens Unicode (₹, accents) flawlessly
        sb.append('\uFEFF')

        // -------------------------------------------------------------
        // SECTION 1: EXECUTIVE DASHBOARD & FINANCIAL OVERVIEW
        // -------------------------------------------------------------
        sb.append("========================================================================\n")
        sb.append("MONEY TRACKER - EXECUTIVE FINANCIAL DASHBOARD REPORT\n")
        sb.append("Generated on: ").append(dateFormat.format(Date())).append("\n")
        sb.append("========================================================================\n\n")

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val netCashFlow = totalIncome - totalExpense
        val totalBankBalance = bankAccounts.sumOf { bank ->
            val inflow = transactions.filter { it.bankAccountId == bank.id && it.type == TransactionType.INCOME }.sumOf { it.amount }
            val outflow = transactions.filter { it.bankAccountId == bank.id && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            bank.initialBalance + inflow - outflow
        }
        val totalCardDebt = creditCards.sumOf { card ->
            transactions.filter { it.creditCardId == card.id && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        }
        val totalMoneyLentPending = borrowedItems.filter { !it.isSettled }.sumOf { it.remainingAmount }

        sb.append("EXECUTIVE SUMMARY METRICS,VALUE (INR)\n")
        sb.append("Total Income Logged,").append("%.2f".format(totalIncome)).append("\n")
        sb.append("Total Expenses Logged,").append("%.2f".format(totalExpense)).append("\n")
        sb.append("Net Cash Flow (Savings),").append("%.2f".format(netCashFlow)).append("\n")
        sb.append("Total Bank & Liquid Wealth,").append("%.2f".format(totalBankBalance)).append("\n")
        sb.append("Active Credit Card Spend,").append("%.2f".format(totalCardDebt)).append("\n")
        sb.append("Money Lent (Pending Receivables),").append("%.2f".format(totalMoneyLentPending)).append("\n\n")

        // -------------------------------------------------------------
        // SECTION 2: BANK ACCOUNTS & CASH FLOW ANALYSIS
        // -------------------------------------------------------------
        sb.append("========================================================================\n")
        sb.append("BANK ACCOUNTS & CASH FLOW BREAKDOWN\n")
        sb.append("========================================================================\n")
        sb.append("Bank Name,Account Type,Last 4 Digits,Opening Balance,Total Inflow (+),Total Outflow (-),Net Flow,Current Balance\n")

        if (bankAccounts.isEmpty()) {
            sb.append("No Bank Accounts configured,Default Wallet,-,0.00,")
                .append("%.2f".format(totalIncome)).append(",")
                .append("%.2f".format(totalExpense)).append(",")
                .append("%.2f".format(netCashFlow)).append(",")
                .append("%.2f".format(netCashFlow)).append("\n")
        } else {
            bankAccounts.forEach { bank ->
                val inflow = transactions.filter { it.bankAccountId == bank.id && it.type == TransactionType.INCOME }.sumOf { it.amount }
                val outflow = transactions.filter { it.bankAccountId == bank.id && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val netFlow = inflow - outflow
                val currentBal = bank.initialBalance + netFlow
                sb.append(escapeCsv(bank.bankName)).append(",")
                    .append(escapeCsv(bank.accountType.displayName)).append(",")
                    .append(if (bank.accountNumberLast4.isNotBlank()) "xx" + bank.accountNumberLast4 else "-").append(",")
                    .append("%.2f".format(bank.initialBalance)).append(",")
                    .append("%.2f".format(inflow)).append(",")
                    .append("%.2f".format(outflow)).append(",")
                    .append("%.2f".format(netFlow)).append(",")
                    .append("%.2f".format(currentBal)).append("\n")
            }
        }
        sb.append("\n")

        // -------------------------------------------------------------
        // SECTION 3: CATEGORY SPENDING BREAKDOWN
        // -------------------------------------------------------------
        sb.append("========================================================================\n")
        sb.append("CATEGORY SPENDING BREAKDOWN\n")
        sb.append("========================================================================\n")
        sb.append("Category,Transactions Count,Total Amount Spent (INR),Percentage of Total Expense\n")

        val expensesByCategory = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }

        expensesByCategory.forEach { (cat, list) ->
            val sum = list.sumOf { it.amount }
            val pct = if (totalExpense > 0) (sum / totalExpense) * 100 else 0.0
            sb.append(escapeCsv(cat.displayName)).append(",")
                .append(list.size).append(",")
                .append("%.2f".format(sum)).append(",")
                .append("%.1f%%".format(pct)).append("\n")
        }
        sb.append("\n")

        // -------------------------------------------------------------
        // SECTION 4: DETAILED TRANSACTIONS REGISTER
        // -------------------------------------------------------------
        sb.append("========================================================================\n")
        sb.append("DETAILED TRANSACTIONS REGISTER\n")
        sb.append("========================================================================\n")
        sb.append("Date,Merchant / Title,Type,Category,Amount (INR),Payment Method,Bank / Card,Notes\n")

        transactions.sortedByDescending { it.dateMillis }.forEach { tx ->
            val bankOrCard = tx.bankName ?: creditCards.find { it.id == tx.creditCardId }?.cardName ?: "-"
            sb.append(dateFormat.format(Date(tx.dateMillis))).append(",")
                .append(escapeCsv(tx.title)).append(",")
                .append(tx.type.name).append(",")
                .append(escapeCsv(tx.category.displayName)).append(",")
                .append("%.2f".format(tx.amount)).append(",")
                .append(escapeCsv(tx.paymentMethod.displayName)).append(",")
                .append(escapeCsv(bankOrCard)).append(",")
                .append(escapeCsv(tx.note)).append("\n")
        }
        sb.append("\n")

        // -------------------------------------------------------------
        // SECTION 5: MONEY LENT & SPLITS
        // -------------------------------------------------------------
        sb.append("========================================================================\n")
        sb.append("MONEY LENT, BORROWED & SPLITS REGISTER\n")
        sb.append("========================================================================\n")
        sb.append("Borrower Name,Total Lent (INR),Amount Received (INR),Pending Amount (INR),How To Receive,Expected Due Date,Split Count,Split Details,Status\n")

        borrowedItems.forEach { item ->
            val dueDateStr = dateFormat.format(Date(item.dueDateMillis))
            sb.append(escapeCsv(item.personName)).append(",")
                .append("%.2f".format(item.totalAmount)).append(",")
                .append("%.2f".format(item.amountReceived)).append(",")
                .append("%.2f".format(item.remainingAmount)).append(",")
                .append(escapeCsv(item.receiveMethod)).append(",")
                .append(dueDateStr).append(",")
                .append(item.splitCount).append(",")
                .append(escapeCsv(item.splitDetails)).append(",")
                .append(if (item.isSettled) "SETTLED" else "PENDING").append("\n")
        }

        return try {
            FileOutputStream(file).use { fos ->
                fos.write(sb.toString().toByteArray(Charsets.UTF_8))
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareExportFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Money Tracker Financial Report")
            putExtra(Intent.EXTRA_TEXT, "Here is the exported Financial Excel & Dashboard report from Money Tracker.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "Open / Share Report in Excel or Google Sheets")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
