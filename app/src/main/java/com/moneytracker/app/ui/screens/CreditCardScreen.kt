package com.moneytracker.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.app.model.CreditCardEntity
import com.moneytracker.app.model.PaymentMethod
import com.moneytracker.app.ui.DashboardUiState
import com.moneytracker.app.ui.components.CreditCardVisual
import com.moneytracker.app.ui.theme.BorderDark
import com.moneytracker.app.ui.theme.ExpenseRed
import com.moneytracker.app.ui.theme.IncomeGreen
import com.moneytracker.app.ui.theme.PrimaryNeon
import com.moneytracker.app.ui.theme.SurfaceDark
import com.moneytracker.app.ui.theme.SurfaceElevated
import com.moneytracker.app.ui.theme.WarningAmber
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CreditCardScreen(
    state: DashboardUiState,
    currencySymbol: String,
    onSettleCard: (CreditCardEntity, Double) -> Unit,
    onAddNewCard: (CreditCardEntity) -> Unit
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }
    var selectedFilterCardId by remember { mutableStateOf<Long?>(null) }
    var settlingCard by remember { mutableStateOf<CreditCardEntity?>(null) }
    var settleAmountInput by remember { mutableStateOf("") }
    var showAddCardDialog by remember { mutableStateOf(false) }

    val totalCcSpend = state.monthlyCreditCardSpend
    val totalLimit = state.creditCards.sumOf { it.creditLimit }.let { if (it <= 0) 1.0 else it }
    val overallUtilization = ((totalCcSpend / totalLimit) * 100).toInt()

    val ccTransactions = state.recentTransactions.filter {
        it.paymentMethod == PaymentMethod.CREDIT_CARD &&
                (selectedFilterCardId == null || it.creditCardId == selectedFilterCardId)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Credit & Debt Hub",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Credit Card Tracker",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { showAddCardDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Card", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Aggregate Summary Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderDark, RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL CREDIT CARD SPEND THIS MONTH",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$currencySymbol${formatter.format(totalCcSpend.toLong())}",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Utilization Badge
                        val isSafe = overallUtilization <= 30
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSafe) IncomeGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSafe) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isSafe) IncomeGreen else WarningAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "$overallUtilization% used",
                                    color = if (isSafe) IncomeGreen else WarningAmber,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total Credit Limit: $currencySymbol${formatter.format(totalLimit.toLong())}",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Available: $currencySymbol${formatter.format((totalLimit - totalCcSpend).coerceAtLeast(0.0).toLong())}",
                            color = IncomeGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Section Title: My Cards
        item {
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Cards (${state.creditCards.size})",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (state.creditCards.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No credit cards added yet",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap '+ Add Card' to add your credit cards and track limits, billing cycles, and dues.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(state.creditCards) { card ->
                val cardSpend = state.cardSpendMap[card.id] ?: 0.0
                CreditCardVisual(
                    card = card,
                    currentSpend = cardSpend,
                    onPayClick = {
                        settlingCard = card
                        settleAmountInput = cardSpend.toString()
                    },
                    currencySymbol = currencySymbol,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }

        // Section Title: Card Spends Breakdown
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Filter Transactions by Card",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Card Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilterCardId == null,
                    onClick = { selectedFilterCardId = null },
                    label = { Text("All Cards") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryNeon,
                        selectedLabelColor = Color.White
                    )
                )

                state.creditCards.forEach { card ->
                    FilterChip(
                        selected = selectedFilterCardId == card.id,
                        onClick = { selectedFilterCardId = card.id },
                        label = { Text("••${card.last4Digits}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryNeon,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Transactions list for selected credit card
        if (ccTransactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No transactions found for this card.", color = Color(0xFF64748B), fontSize = 13.sp)
                }
            }
        } else {
            items(ccTransactions) { tx ->
                TransactionItemRow(
                    tx = tx,
                    currencySymbol = currencySymbol,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
        }
    }

    // Add Credit Card Dialog (Mandatory fields marked with *)
    if (showAddCardDialog) {
        var cardName by remember { mutableStateOf("") }
        var bankName by remember { mutableStateOf("") }
        var last4Digits by remember { mutableStateOf("") }
        var creditLimit by remember { mutableStateOf("") }
        var billingDay by remember { mutableStateOf("15") }
        var dueDay by remember { mutableStateOf("5") }
        var selectedNetwork by remember { mutableStateOf("VISA") }
        var formError by remember { mutableStateOf(false) }

        val networks = listOf("VISA", "MASTERCARD", "RUPAY", "AMEX")

        AlertDialog(
            onDismissRequest = { showAddCardDialog = false },
            title = { Text("Add Credit Card") },
            text = {
                Column {
                    OutlinedTextField(
                        value = cardName,
                        onValueChange = { cardName = it },
                        label = {
                            Row {
                                Text("Card Name")
                                Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold)
                            }
                        },
                        placeholder = { Text("e.g. Regalia Gold, Amazon Pay") },
                        isError = formError && cardName.isBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = {
                            Row {
                                Text("Bank Name")
                                Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold)
                            }
                        },
                        placeholder = { Text("e.g. HDFC Bank, ICICI Bank") },
                        isError = formError && bankName.isBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = last4Digits,
                            onValueChange = { if (it.length <= 4) last4Digits = it },
                            label = {
                                Row {
                                    Text("Last 4")
                                    Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold)
                                }
                            },
                            placeholder = { Text("4892") },
                            isError = formError && last4Digits.length < 4,
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = creditLimit,
                            onValueChange = { creditLimit = it },
                            label = {
                                Row {
                                    Text("Limit")
                                    Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold)
                                }
                            },
                            placeholder = { Text("200000") },
                            isError = formError && (creditLimit.toDoubleOrNull() ?: 0.0) <= 0,
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = billingDay,
                            onValueChange = { billingDay = it },
                            label = {
                                Row {
                                    Text("Billing Day")
                                    Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = dueDay,
                            onValueChange = { dueDay = it },
                            label = {
                                Row {
                                    Text("Due Day")
                                    Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Card Network", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(networks) { net ->
                            FilterChip(
                                selected = selectedNetwork == net,
                                onClick = { selectedNetwork = net },
                                label = { Text(net) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedLimit = creditLimit.toDoubleOrNull() ?: 0.0
                        val bDay = billingDay.toIntOrNull() ?: 15
                        val dDay = dueDay.toIntOrNull() ?: 5
                        if (cardName.isNotBlank() && bankName.isNotBlank() && last4Digits.length == 4 && parsedLimit > 0) {
                            onAddNewCard(
                                CreditCardEntity(
                                    cardName = cardName.trim(),
                                    bankName = bankName.trim(),
                                    last4Digits = last4Digits.trim(),
                                    creditLimit = parsedLimit,
                                    billingCycleDay = bDay,
                                    paymentDueDay = dDay,
                                    network = selectedNetwork
                                )
                            )
                            showAddCardDialog = false
                        } else {
                            formError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
                ) {
                    Text("Save Card")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Settle Bill Dialog
    settlingCard?.let { card ->
        AlertDialog(
            onDismissRequest = { settlingCard = null },
            title = {
                Text("Settle Bill for ${card.cardName}")
            },
            text = {
                Column {
                    Text(
                        text = "Enter amount to pay ending in •••• ${card.last4Digits}:",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = settleAmountInput,
                        onValueChange = { settleAmountInput = it },
                        label = { Text("Payment Amount ($currencySymbol)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = settleAmountInput.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onSettleCard(card, amt)
                        }
                        settlingCard = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
                ) {
                    Text("Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(onClick = { settlingCard = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
