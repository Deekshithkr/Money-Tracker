package com.moneytracker.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import com.moneytracker.app.model.RecurringItemEntity
import com.moneytracker.app.ui.theme.BorderDark
import com.moneytracker.app.ui.theme.IncomeGreen
import com.moneytracker.app.ui.theme.PrimaryNeon
import com.moneytracker.app.ui.theme.SurfaceElevated
import java.text.NumberFormat
import java.util.Locale

@Composable
fun LoanProgressCard(
    loan: RecurringItemEntity,
    onPayEmiClick: (RecurringItemEntity) -> Unit,
    modifier: Modifier = Modifier,
    currencySymbol: String = "₹"
) {
    val formatter = remember { NumberFormat.getNumberInstance(Locale("en", "IN")) }

    val progress = remember(loan.paidTenureMonths, loan.totalTenureMonths) {
        if (loan.totalTenureMonths > 0) {
            (loan.paidTenureMonths.toFloat() / loan.totalTenureMonths.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }

    val totalPaidApprox = loan.paidTenureMonths * loan.amount
    val remainingApprox = (loan.totalTenureMonths - loan.paidTenureMonths) * loan.amount

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceElevated)
            .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .padding(8.dp)
                ) {
                    Text("🏦", fontSize = 18.sp)
                }
                Column {
                    Text(
                        text = loan.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Due on ${loan.dueDayOfMonth}th of each month",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(Color(0xFF3B82F6).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${loan.paidTenureMonths}/${loan.totalTenureMonths} EMIs",
                    color = Color(0xFF93C5FD),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Progress Bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape),
            color = IncomeGreen,
            trackColor = Color(0xFF334155)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // EMI Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Monthly EMI", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text(
                    text = "$currencySymbol${formatter.format(loan.amount.toLong())}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Paid So Far", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text(
                    text = "$currencySymbol${formatter.format(totalPaidApprox.toLong())}",
                    color = IncomeGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Remaining", color = Color(0xFF94A3B8), fontSize = 11.sp)
                Text(
                    text = "$currencySymbol${formatter.format(remainingApprox.toLong())}",
                    color = Color(0xFFF87171),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action button
        OutlinedButton(
            onClick = { onPayEmiClick(loan) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PrimaryNeon
            )
        ) {
            Text(
                text = "Record EMI #${loan.paidTenureMonths + 1} Paid",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
