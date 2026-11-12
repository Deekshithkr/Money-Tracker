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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    creditCards: List<CreditCardEntity>,
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
        note: String
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
    var note by remember { mutableStateOf("") }

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

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount ($currencySymbol)") },
                placeholder = { Text("0.00") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title / Merchant
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title / Merchant (e.g. Starbucks, Amazon, Rent)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Picker
            Text(
                text = "Select Category",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
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
                            selectedContainerColor = Color(cat.colorHex).copy(alpha = 0.3f),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Payment Method Selector
            Text(
                text = "Payment Method",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
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
                Spacer(modifier = Modifier.height(12.dp))
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

            Spacer(modifier = Modifier.height(12.dp))

            // Optional Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note / Tag (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            Button(
                onClick = {
                    val parsedAmt = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && parsedAmt > 0) {
                        onSave(
                            title,
                            parsedAmt,
                            type,
                            selectedCategory,
                            selectedPaymentMethod,
                            if (selectedPaymentMethod == PaymentMethod.CREDIT_CARD) selectedCreditCardId else null,
                            note
                        )
                        onDismiss()
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
