package com.moneytracker.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.app.model.TransactionType
import com.moneytracker.app.ui.screens.AddBorrowedSheet
import com.moneytracker.app.ui.screens.AddTransactionSheet
import com.moneytracker.app.ui.screens.AnalyticsScreen
import com.moneytracker.app.ui.screens.CreditCardScreen
import com.moneytracker.app.ui.screens.DashboardScreen
import com.moneytracker.app.ui.screens.RecurringScreen
import com.moneytracker.app.ui.screens.TransactionsScreen
import com.moneytracker.app.ui.theme.BgDark
import com.moneytracker.app.ui.theme.PrimaryNeon
import com.moneytracker.app.ui.theme.SurfaceDark

sealed class Screen(val title: String, val icon: ImageVector, val index: Int) {
    object Dashboard : Screen("Overview", Icons.Default.Home, 0)
    object Analytics : Screen("Analytics", Icons.Default.PieChart, 1)
    object CreditCards : Screen("Cards", Icons.Default.CreditCard, 2)
    object Recurring : Screen("Recurring", Icons.Default.Sync, 3)
    object History : Screen("History", Icons.AutoMirrored.Filled.ReceiptLong, 4)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showAddBorrowedSheet by remember { mutableStateOf(false) }
    var defaultSheetType by remember { mutableStateOf(TransactionType.EXPENSE) }

    val screens = listOf(
        Screen.Dashboard,
        Screen.Analytics,
        Screen.CreditCards,
        Screen.Recurring,
        Screen.History
    )

    Scaffold(
        containerColor = BgDark,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 8.dp
            ) {
                screens.forEach { screen ->
                    val isSelected = selectedTabIndex == screen.index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabIndex = screen.index },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.White,
                            indicatorColor = PrimaryNeon,
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    defaultSheetType = TransactionType.EXPENSE
                    showAddSheet = true
                },
                containerColor = PrimaryNeon,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Quick Add Transaction"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgDark)
        ) {
            AnimatedContent(
                targetState = selectedTabIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { targetIndex ->
                when (targetIndex) {
                    0 -> DashboardScreen(
                        state = dashboardState,
                        currencySymbol = currencySymbol,
                        onNavigateToCards = { selectedTabIndex = 2 },
                        onNavigateToRecurring = { selectedTabIndex = 3 },
                        onQuickAddExpense = {
                            defaultSheetType = TransactionType.EXPENSE
                            showAddSheet = true
                        },
                        onQuickAddIncome = {
                            defaultSheetType = TransactionType.INCOME
                            showAddSheet = true
                        },
                        onAddBorrowed = {
                            showAddBorrowedSheet = true
                        },
                        onReceiveRepayment = { item, amt ->
                            viewModel.recordBorrowedRepayment(item, amt)
                        }
                    )
                    1 -> AnalyticsScreen(
                        state = dashboardState,
                        currencySymbol = currencySymbol
                    )
                    2 -> CreditCardScreen(
                        state = dashboardState,
                        currencySymbol = currencySymbol,
                        onSettleCard = { card, amt ->
                            viewModel.settleCreditCard(card, amt)
                        },
                        onAddNewCard = { card ->
                            viewModel.addCreditCard(card)
                        }
                    )
                    3 -> RecurringScreen(
                        state = dashboardState,
                        currencySymbol = currencySymbol,
                        onRecordLoanPayment = { loan ->
                            viewModel.recordLoanPayment(loan)
                        },
                        onAddRecurringItem = { item ->
                            viewModel.addRecurringItem(item)
                        }
                    )
                    4 -> TransactionsScreen(
                        transactions = allTransactions,
                        currencySymbol = currencySymbol,
                        onDeleteTransaction = { tx ->
                            viewModel.deleteTransaction(tx)
                        }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddTransactionSheet(
            creditCards = dashboardState.creditCards,
            currencySymbol = currencySymbol,
            defaultType = defaultSheetType,
            onDismiss = { showAddSheet = false },
            onSave = { title, amount, type, category, paymentMethod, cardId, note, dateMillis ->
                viewModel.addTransaction(
                    title = title,
                    amount = amount,
                    type = type,
                    category = category,
                    paymentMethod = paymentMethod,
                    creditCardId = cardId,
                    note = note,
                    dateMillis = dateMillis
                )
            }
        )
    }

    if (showAddBorrowedSheet) {
        AddBorrowedSheet(
            currencySymbol = currencySymbol,
            onDismiss = { showAddBorrowedSheet = false },
            onSave = { name, amt, method, due, isSplit, splitCount, details, notes ->
                viewModel.addBorrowedItem(
                    personName = name,
                    totalAmount = amt,
                    receiveMethod = method,
                    dueDateMillis = due,
                    isSplit = isSplit,
                    splitCount = splitCount,
                    splitDetails = details,
                    notes = notes
                )
            }
        )
    }
}
