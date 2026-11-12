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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.app.model.PaymentMethod
import com.moneytracker.app.ui.DashboardUiState
import com.moneytracker.app.ui.components.AnimatedDonutChart
import com.moneytracker.app.ui.components.SpendingBarChart
import com.moneytracker.app.ui.theme.BorderDark
import com.moneytracker.app.ui.theme.ExpenseRed
import com.moneytracker.app.ui.theme.IncomeGreen
import com.moneytracker.app.ui.theme.PrimaryNeon
import com.moneytracker.app.ui.theme.SurfaceDark
import com.moneytracker.app.ui.theme.SurfaceElevated
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AnalyticsScreen(
    state: DashboardUiState,
    currencySymbol: String
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }
    val totalExpense = state.monthlyExpense
    val totalIncome = state.monthlyIncome
    val savingsRate = remember(totalIncome, totalExpense) {
        if (totalIncome > 0) (((totalIncome - totalExpense) / totalIncome) * 100).toInt().coerceAtLeast(0) else 0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Spending Intelligence",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Visual Analytics",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Financial Health & Savings Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderDark, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Monthly Savings Ratio", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Text(
                                text = "$savingsRate% Saved",
                                color = IncomeGreen,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(IncomeGreen.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Healthy Margin",
                                color = IncomeGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { (savingsRate / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = IncomeGreen,
                        trackColor = Color(0xFF334155)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Net Surplus: $currencySymbol${formatter.format((totalIncome - totalExpense).toLong())}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Avg: $currencySymbol${formatter.format((totalExpense / 30).toLong())} / day",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Category Breakdown Donut Chart
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderDark, RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Category Distribution",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedDonutChart(
                        slices = state.categoryBreakdown,
                        centerAmount = state.monthlyExpense,
                        currencySymbol = currencySymbol
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Category Legend List
                    state.categoryBreakdown.forEach { cat ->
                        val percent = if (totalExpense > 0) ((cat.totalAmount / totalExpense) * 100).toInt() else 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(cat.category.colorHex), CircleShape)
                                )
                                Text(
                                    text = "${cat.category.emoji} ${cat.category.displayName}",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$percent%",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$currencySymbol${formatter.format(cat.totalAmount.toLong())}",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Payment Method Rail Share (Requirement #2 & #4)
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderDark, RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Text(
                        text = "Payment Method Share",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "How you paid across Credit, UPI, Debit & Cash",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val totalMethodSpend = state.paymentBreakdown.sumOf { it.totalAmount }.let { if (it <= 0) 1.0 else it }

                    state.paymentBreakdown.forEach { methodSpend ->
                        val percent = ((methodSpend.totalAmount / totalMethodSpend) * 100).toInt()
                        val methodColor = when (methodSpend.paymentMethod) {
                            PaymentMethod.CREDIT_CARD -> PrimaryNeon
                            PaymentMethod.UPI -> Color(0xFF10B981)
                            PaymentMethod.DEBIT_CARD -> Color(0xFF38BDF8)
                            PaymentMethod.CASH -> Color(0xFFFBBF24)
                            PaymentMethod.NET_BANKING -> Color(0xFFA855F7)
                        }

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${methodSpend.paymentMethod.icon} ${methodSpend.paymentMethod.displayName}",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "$percent% • $currencySymbol${formatter.format(methodSpend.totalAmount.toLong())}",
                                    color = methodColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (percent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = methodColor,
                                trackColor = Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }

        // 6-Month Income vs Expense Trend Bar Chart
        item {
            Spacer(modifier = Modifier.height(20.dp))
            SpendingBarChart(
                data = state.trendData,
                currencySymbol = currencySymbol,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}
