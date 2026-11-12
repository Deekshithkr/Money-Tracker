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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.moneytracker.app.model.CreditCardEntity
import com.moneytracker.app.model.PaymentMethod
import com.moneytracker.app.ui.DashboardUiState
import com.moneytracker.app.ui.components.CreditCardVisual
import com.moneytracker.app.ui.theme.BorderDark
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
            }
        }

        // Aggregate Summary Card (Directly answers Requirement #4)
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
                            Text(
                                text = "$currencySymbol${formatter.format(totalCcSpend.toLong())}",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Utilization Status Tag
                        val isSafe = overallUtilization < 30
                        Box(
                            modifier = Modifier
                                .background(
                                    (if (isSafe) IncomeGreen else WarningAmber).copy(alpha = 0.15f),
                                    RoundedCornerShape(10.dp)
                                )
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
                            text = "Available: $currencySymbol${formatter.format((totalLimit - totalCcSpend).toLong())}",
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

        // Realistic Cards List
        items(state.creditCards) { card ->
            val cardSpend = state.cardSpendMap[card.id] ?: 0.0
            CreditCardVisual(
                card = card,
                currentSpend = cardSpend,
                currencySymbol = currencySymbol,
                onPayClick = {
                    settlingCard = it
                    settleAmountInput = cardSpend.toInt().toString()
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }

        // Filter Bar by Card for transactions
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Credit Card Transactions",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

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
                        label = { Text(card.cardName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryNeon,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

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
