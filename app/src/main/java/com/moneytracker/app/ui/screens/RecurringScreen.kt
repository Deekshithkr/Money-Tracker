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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.app.model.PaymentMethod
import com.moneytracker.app.model.RecurringFrequency
import com.moneytracker.app.model.RecurringItemEntity
import com.moneytracker.app.model.RecurringType
import com.moneytracker.app.ui.DashboardUiState
import com.moneytracker.app.ui.components.LoanProgressCard
import com.moneytracker.app.ui.theme.BorderDark
import com.moneytracker.app.ui.theme.ExpenseRed
import com.moneytracker.app.ui.theme.IncomeGreen
import com.moneytracker.app.ui.theme.PrimaryNeon
import com.moneytracker.app.ui.theme.SurfaceDark
import com.moneytracker.app.ui.theme.SurfaceElevated
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RecurringScreen(
    state: DashboardUiState,
    currencySymbol: String,
    onRecordLoanPayment: (RecurringItemEntity) -> Unit,
    onAddRecurringItem: (RecurringItemEntity) -> Unit
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val fixedInflow = state.upcomingRecurring.filter { it.recurringType.isCredit }.sumOf { it.amount }
    val fixedOutflow = state.upcomingRecurring.filter { !it.recurringType.isCredit }.sumOf { it.amount }
    val fixedSurplus = fixedInflow - fixedOutflow

    val filteredItems = when (selectedTab) {
        1 -> state.activeLoans // Loans & EMIs
        2 -> state.nonLoanRecurring.filter { !it.recurringType.isCredit } // Subscriptions & Bills
        3 -> state.upcomingRecurring.filter { it.recurringType.isCredit } // Credits / Salary
        else -> state.upcomingRecurring
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top Header
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
                        text = "Salary, Loans & Subscriptions",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Recurring Cashflows",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                ElevatedButton(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = PrimaryNeon,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("New", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Cashflow Summary Banner
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
                    Text(
                        text = "NET MONTHLY COMMITTED SURPLUS",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$currencySymbol${formatter.format(fixedSurplus.toLong())}",
                        color = if (fixedSurplus >= 0) IncomeGreen else ExpenseRed,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Fixed Inflow (Salary)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text(
                                "+$currencySymbol${formatter.format(fixedInflow.toLong())}",
                                color = IncomeGreen,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Fixed Outflow (Loans + Bills)", color = Color(0xFF94A3B8), fontSize = 11.sp)
                            Text(
                                "-$currencySymbol${formatter.format(fixedOutflow.toLong())}",
                                color = ExpenseRed,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Tabs Row: All, Loans & EMIs, Subscriptions, Salary
        item {
            Spacer(modifier = Modifier.height(18.dp))
            val tabs = listOf("All", "Loans & EMIs", "Subscriptions", "Salary")
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = PrimaryNeon,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryNeon
                    )
                },
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (filteredItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceElevated)
                        .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No recurring items found",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap '+ Add Item' above to track your recurring expenses, bills, or loans.",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredItems) { item ->
                if (item.isLoan) {
                    LoanProgressCard(
                        loan = item,
                        currencySymbol = currencySymbol,
                        onPayEmiClick = { onRecordLoanPayment(it) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                } else {
                    RecurringGeneralRow(
                        item = item,
                        currencySymbol = currencySymbol,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    // Add Recurring Item Dialog
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var dueDay by remember { mutableStateOf("1") }
        var isLoan by remember { mutableStateOf(false) }
        var totalMonths by remember { mutableStateOf("36") }
        var formError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Recurring Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = {
                            Row {
                                Text("Title (e.g. Netflix, Car EMI)")
                                Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold)
                            }
                        },
                        isError = formError && title.isBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = {
                            Row {
                                Text("Amount ($currencySymbol)")
                                Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold)
                            }
                        },
                        isError = formError && (amount.toDoubleOrNull() ?: 0.0) <= 0,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dueDay,
                        onValueChange = { dueDay = it },
                        label = {
                            Row {
                                Text("Day of Month (1 - 31)")
                                Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isLoan,
                            onClick = { isLoan = false },
                            label = { Text("Regular Bill/Salary") }
                        )
                        FilterChip(
                            selected = isLoan,
                            onClick = { isLoan = true },
                            label = { Text("Loan / EMI") }
                        )
                    }

                    if (isLoan) {
                        OutlinedTextField(
                            value = totalMonths,
                            onValueChange = { totalMonths = it },
                            label = {
                                Row {
                                    Text("Total Tenure (Months)")
                                    Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                        val parsedDay = dueDay.toIntOrNull() ?: 1
                        val parsedMonths = totalMonths.toIntOrNull() ?: 12

                        if (title.isNotBlank() && parsedAmount > 0) {
                            onAddRecurringItem(
                                RecurringItemEntity(
                                    title = title.trim(),
                                    amount = parsedAmount,
                                    recurringType = if (isLoan) RecurringType.PERSONAL_LOAN_EMI else RecurringType.SUBSCRIPTION,
                                    frequency = RecurringFrequency.MONTHLY,
                                    dueDayOfMonth = parsedDay,
                                    isLoan = isLoan,
                                    totalTenureMonths = if (isLoan) parsedMonths else 0,
                                    paidTenureMonths = 0,
                                    originalLoanAmount = if (isLoan) (parsedAmount * parsedMonths) else 0.0
                                )
                            )
                            showAddDialog = false
                        } else {
                            formError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecurringGeneralRow(
    item: RecurringItemEntity,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }
    val isCredit = item.recurringType.isCredit

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        (if (isCredit) IncomeGreen else PrimaryNeon).copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isCredit) "💰" else "🔄", fontSize = 18.sp)
            }

            Column {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Due on ${item.dueDayOfMonth}th • ${item.defaultPaymentMethod.displayName}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = "${if (isCredit) "+" else "-"}$currencySymbol${formatter.format(item.amount.toLong())}",
            color = if (isCredit) IncomeGreen else Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
