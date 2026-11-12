package com.moneytracker.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneytracker.app.ui.theme.ExpenseRed
import com.moneytracker.app.ui.theme.IncomeGreen
import com.moneytracker.app.ui.theme.SurfaceElevated

data class MonthTrendData(
    val monthLabel: String,
    val income: Double,
    val expense: Double
)

@Composable
fun SpendingBarChart(
    data: List<MonthTrendData>,
    modifier: Modifier = Modifier,
    currencySymbol: String = "₹"
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(800))
    }

    val maxAmount = remember(data) {
        val peak = data.maxOfOrNull { maxOf(it.income, it.expense) } ?: 10000.0
        if (peak <= 0.0) 10000.0 else peak
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceElevated, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Income vs Expense Trend",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(IncomeGreen, CircleShape))
                    Text("Income", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(ExpenseRed, CircleShape))
                    Text("Expense", color = Color(0xFF94A3B8), fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height - 24.dp.toPx()
            val barWidth = 14.dp.toPx()
            val pairSpacing = canvasWidth / (data.size.coerceAtLeast(1))

            data.forEachIndexed { index, item ->
                val centerX = (index * pairSpacing) + (pairSpacing / 2f)

                val incomeHeight = ((item.income / maxAmount) * canvasHeight * animProgress.value).toFloat()
                val expenseHeight = ((item.expense / maxAmount) * canvasHeight * animProgress.value).toFloat()

                // Income bar (left)
                val incomeX = centerX - barWidth - 2.dp.toPx()
                val incomeY = canvasHeight - incomeHeight
                drawRoundRect(
                    color = IncomeGreen,
                    topLeft = Offset(incomeX, incomeY),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )

                // Expense bar (right)
                val expenseX = centerX + 2.dp.toPx()
                val expenseY = canvasHeight - expenseHeight
                drawRoundRect(
                    color = ExpenseRed,
                    topLeft = Offset(expenseX, expenseY),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }
        }

        // Labels row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEach { item ->
                Text(
                    text = item.monthLabel,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
        }
    }
}
