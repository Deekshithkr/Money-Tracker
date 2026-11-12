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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.moneytracker.app.ui.theme.BorderDark
import com.moneytracker.app.ui.theme.ExpenseRed
import com.moneytracker.app.ui.theme.IncomeGreen
import com.moneytracker.app.ui.theme.PrimaryNeon
import com.moneytracker.app.ui.theme.SurfaceDark
import com.moneytracker.app.ui.theme.SurfaceElevated
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBorrowedSheet(
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onSave: (
        personName: String,
        totalAmount: Double,
        receiveMethod: String,
        dueDateMillis: Long,
        isSplit: Boolean,
        splitCount: Int,
        splitDetails: String,
        notes: String
    ) -> Unit
) {
    var personName by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("UPI") }
    var isSplit by remember { mutableStateOf(false) }
    var splitCount by remember { mutableIntStateOf(2) }
    var customSplitDetails by remember { mutableStateOf("") }
    var daysUntilDue by remember { mutableIntStateOf(7) }
    var notes by remember { mutableStateOf("") }

    var showError by remember { mutableStateOf(false) }

    val receiveMethods = listOf("UPI", "Cash", "Bank Transfer", "Net Banking")
    val dueOptions = listOf(3 to "3 Days", 7 to "1 Week", 15 to "15 Days", 30 to "1 Month")

    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val splitAmountEach = if (isSplit && splitCount > 0 && parsedAmount > 0) {
        parsedAmount / splitCount
    } else 0.0

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(PrimaryNeon.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CallSplit,
                        contentDescription = null,
                        tint = PrimaryNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Track Money Lent / Borrowed",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Track how you will receive it & split among people",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Person / Borrower Name (Mandatory)
            OutlinedTextField(
                value = personName,
                onValueChange = {
                    personName = it
                    if (showError) showError = false
                },
                label = {
                    Row {
                        Text("Borrower / Person Name")
                        Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold)
                    }
                },
                placeholder = { Text("e.g. Rahul, Dinner Group, Alex") },
                isError = showError && personName.isBlank(),
                supportingText = if (showError && personName.isBlank()) {
                    { Text("Person name is required", color = ExpenseRed) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Total Amount (Mandatory)
            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                    if (showError) showError = false
                },
                label = {
                    Row {
                        Text("Total Amount Lent ($currencySymbol)")
                        Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold)
                    }
                },
                placeholder = { Text("0.00") },
                isError = showError && parsedAmount <= 0,
                supportingText = if (showError && parsedAmount <= 0) {
                    { Text("Amount must be greater than 0", color = ExpenseRed) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // How you will receive it (Method)
            Row {
                Text(
                    text = "How you will receive it",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(receiveMethods) { method ->
                    FilterChip(
                        selected = selectedMethod == method,
                        onClick = { selectedMethod = method },
                        label = { Text(method) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryNeon,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expected Date to Receive (Mandatory)
            Row {
                Text(
                    text = "Expected Date to Receive",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(" *", color = ExpenseRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dueOptions) { (days, label) ->
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }
                    val dateFormatted = SimpleDateFormat("dd MMM", Locale.getDefault()).format(cal.time)
                    FilterChip(
                        selected = daysUntilDue == days,
                        onClick = { daysUntilDue = days },
                        label = { Text("$label ($dateFormatted)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF38BDF8),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Split Toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceElevated)
                    .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Split with multiple people / parts?",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Split bill or multiple installments",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        Switch(
                            checked = isSplit,
                            onCheckedChange = { isSplit = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryNeon)
                        )
                    }

                    if (isSplit) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Number of splits / parts:",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (splitCount > 2) splitCount-- },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(SurfaceDark, CircleShape)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }

                                Text(
                                    text = "$splitCount",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )

                                IconButton(
                                    onClick = { if (splitCount < 10) splitCount++ },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .background(SurfaceDark, CircleShape)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        if (splitAmountEach > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "↳ Each share: $currencySymbol${"%.2f".format(splitAmountEach)} ($splitCount parts)",
                                color = IncomeGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customSplitDetails,
                            onValueChange = { customSplitDetails = it },
                            label = { Text("Split notes / names (optional)") },
                            placeholder = { Text("e.g. Person A: 500, Person B: 500") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Optional Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Additional Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    if (personName.isNotBlank() && parsedAmount > 0) {
                        val dueCal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, daysUntilDue)
                        }
                        val splitSummary = if (isSplit) {
                            if (customSplitDetails.isNotBlank()) customSplitDetails
                            else "$splitCount parts ($currencySymbol${"%.2f".format(splitAmountEach)} each)"
                        } else ""

                        onSave(
                            personName.trim(),
                            parsedAmount,
                            selectedMethod,
                            dueCal.timeInMillis,
                            isSplit,
                            splitCount,
                            splitSummary,
                            notes.trim()
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNeon)
            ) {
                Text(
                    text = "Save Borrowed Record",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
