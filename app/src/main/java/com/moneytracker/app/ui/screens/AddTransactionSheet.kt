package com.moneytracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.app.model.Category
import com.moneytracker.app.model.CreditCardEntity
import com.moneytracker.app.model.PaymentMethod
import com.moneytracker.app.model.TransactionType
import com.moneytracker.app.ui.theme.BorderDark
import com.moneytracker.app.ui.theme.ExpenseRed
import com.moneytracker.app.ui.theme.IncomeGreen
import com.moneytracker.app.ui.theme.PrimaryNeon
import com.moneytracker.app.ui.theme.SurfaceDark
import com.moneytracker.app.ui.theme.SurfaceElevated
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    creditCards: List<CreditCardEntity>,
    bankAccounts: List<com.moneytracker.app.model.BankAccountEntity> = emptyList(),
    currencySymbol: String = "₹",
    defaultType: TransactionType = TransactionType.EXPENSE,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        type: TransactionType,
        category: Category,
        paymentMethod: PaymentMethod,
        creditCardId: Long?,
        bankAccountId: Long?,
        bankName: String?,
        note: String,
        dateMillis: Long
    ) -> Unit
) {
    var type by remember { mutableStateOf(defaultType) }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedCategory by remember {
        mutableStateOf(if (defaultType == TransactionType.EXPENSE) Category.FOOD else Category.SALARY)
    }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.UPI) }
    var selectedCreditCardId by remember { mutableStateOf(creditCards.firstOrNull()?.id) }
    var selectedBankAccountId by remember { mutableStateOf<Long?>(bankAccounts.firstOrNull()?.id) }
    var note by remember { mutableStateOf("") }
    var daysOffset by remember { mutableIntStateOf(0) }
    var showError by remember { mutableStateOf(false) }

    val parsedAmt = amount.toDoubleOrNull() ?: 0.0

    val dateOptions = listOf(
        0 to "Today",
        1 to "Yesterday",
        2 to "2 Days Ago",
        3 to "3 Days Ago",
        7 to "1 Week Ago"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Log New Transaction",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Transaction Type Toggle (Expense / Income)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (type == TransactionType.EXPENSE) ExpenseRed else Color.Transparent)
                        .clickable {
                            type = TransactionType.EXPENSE
                            selectedCategory = Category.FOOD
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Expense",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (type == TransactionType.INCOME) IncomeGreen else Color.Transparent)
                        .clickable {
                            type = TransactionType.INCOME
                            selectedCategory = Category.SALARY
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Income",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Input (Mandatory *)
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    if (showError) showError = false
                },
                label = {
                    Row {
                        Text("Amount ($currencySymbol)")
                        Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold)
                    }
                },
                placeholder = { Text("0.00") },
                isError = showError && parsedAmt <= 0,
                supportingText = if (showError && parsedAmt <= 0) {
                    { Text("Amount must be greater than 0", color = ExpenseRed) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Title / Merchant (Mandatory *)
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (showError) showError = false
                },
                label = {
                    Row {
                        Text("Title / Merchant (e.g. Starbucks, Rent)")
                        Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold)
                    }
                },
                placeholder = { Text("Enter title") },
                isError = showError && title.isBlank(),
                supportingText = if (showError && title.isBlank()) {
                    { Text("Title is required", color = ExpenseRed) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Date Picker / Selector (Choose date while adding expense)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Transaction Date",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dateOptions) { (days, label) ->
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }
                    val dateFormatted = SimpleDateFormat("dd MMM", Locale.getDefault()).format(cal.time)
                    val fullLabel = if (days == 0) "$label ($dateFormatted)" else "$label ($dateFormatted)"
                    FilterChip(
                        selected = daysOffset == days,
                        onClick = { daysOffset = days },
                        label = { Text(fullLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryNeon.copy(alpha = 0.4f),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Picker (Mandatory *)
            Row {
                Text(
                    text = "Category",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))

            val availableCategories = Category.values().filter {
                if (type == TransactionType.EXPENSE) it.isExpenseCategory else !it.isExpenseCategory
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableCategories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text("${cat.emoji} ${cat.displayName}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(cat.colorHex).copy(alpha = 0.35f),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Payment Method Selector (Mandatory *)
            Row {
                Text(
                    text = "Payment Method",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PaymentMethod.values()) { method ->
                    FilterChip(
                        selected = selectedPaymentMethod == method,
                        onClick = { selectedPaymentMethod = method },
                        label = { Text("${method.icon} ${method.displayName}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryNeon,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // If Credit Card is selected: Choose specific credit card
            if (selectedPaymentMethod == PaymentMethod.CREDIT_CARD && creditCards.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Select Credit Card",
                    color = Color(0xFFA5B4FC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(creditCards) { card ->
                        FilterChip(
                            selected = selectedCreditCardId == card.id,
                            onClick = { selectedCreditCardId = card.id },
                            label = { Text("${card.cardName} (••${card.last4Digits})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(card.cardGradientStart),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // If Bank Accounts exist and not paying via pure Credit Card: Select Bank
            if (bankAccounts.isNotEmpty() && selectedPaymentMethod != PaymentMethod.CREDIT_CARD) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Link Bank Account (For Cash Flow Tracking)",
                    color = Color(0xFF34D399),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(bankAccounts) { bank ->
                        FilterChip(
                            selected = selectedBankAccountId == bank.id,
                            onClick = { selectedBankAccountId = bank.id },
                            label = { Text("${bank.bankName} ${if (bank.accountNumberLast4.isNotBlank()) "(••${bank.accountNumberLast4})" else ""}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Optional Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note / Tag (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    if (title.isNotBlank() && parsedAmt > 0) {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, -daysOffset)
                        }
                        val chosenBank = bankAccounts.find { it.id == selectedBankAccountId }
                        onSave(
                            title.trim(),
                            parsedAmt,
                            type,
                            selectedCategory,
                            selectedPaymentMethod,
                            if (selectedPaymentMethod == PaymentMethod.CREDIT_CARD) selectedCreditCardId else null,
                            if (selectedPaymentMethod != PaymentMethod.CREDIT_CARD) selectedBankAccountId else null,
                            if (selectedPaymentMethod != PaymentMethod.CREDIT_CARD) chosenBank?.bankName else null,
                            note.trim(),
                            cal.timeInMillis
                        )
                        onDismiss()
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                )
            ) {
                Text(
                    text = "Save Transaction",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
