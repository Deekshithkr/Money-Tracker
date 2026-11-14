package com.moneytracker.app.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.moneytracker.app.model.BankAccountType
import com.moneytracker.app.notification.RecurringReminderWorker
import com.moneytracker.app.sync.GoogleAuthDriveHelper
import com.moneytracker.app.sync.UserProfile
import com.moneytracker.app.ui.BankCashFlow
import com.moneytracker.app.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val dashboardState by viewModel.dashboardState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var showAddBankDialog by remember { mutableStateOf(false) }
    var showManualAccountDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var reminderNotificationEnabled by remember { mutableStateOf(true) }
    var statusFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // Google Sign-In launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                if (account != null) {
                    GoogleAuthDriveHelper.saveUserAccount(context, account)
                    val profile = UserProfile(
                        isSignedIn = true,
                        displayName = account.displayName ?: "Google User",
                        email = account.email ?: "",
                        photoUrl = account.photoUrl?.toString() ?: ""
                    )
                    viewModel.setUserProfile(profile)
                    // Automatically pull any cloud data for this user
                    viewModel.pullUserDataFromMongo(profile.email) { _, msg ->
                        statusFeedbackMessage = msg
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                statusFeedbackMessage = "Google Sign-In failed (${e.message}). You can use Manual Email Account creation below."
            }
        } else {
            statusFeedbackMessage = "Google Sign-In canceled or unconfigured. You can create an instant email account below."
        }
    }

    // Google Drive / JSON Import launcher
    val driveImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.restoreFromDriveBackup(context, uri) { _, msg ->
                statusFeedbackMessage = msg
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Preferences & Cloud",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF151F32))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Help,
                        contentDescription = "Help & FAQ",
                        tint = Color(0xFF38BDF8)
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 1. Account & SSO / Manual Creation Card
        // -------------------------------------------------------------
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ACCOUNT & CLOUD IDENTITY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (userProfile.isSignedIn) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF10B981), Color(0xFF047857))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile.displayName.take(1).uppercase(),
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userProfile.displayName.ifBlank { "Active User" },
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = userProfile.email,
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Cloud Partition Active",
                                        color = Color(0xFF10B981),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    GoogleAuthDriveHelper.signOut(context) {
                                        viewModel.setUserProfile(UserProfile())
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Sign Out", fontSize = 12.sp, color = Color(0xFFEF4444))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Button to pull cloud data for this account
                        OutlinedButton(
                            onClick = {
                                viewModel.pullUserDataFromMongo(userProfile.email) { _, msg ->
                                    statusFeedbackMessage = msg
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSyncing
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pull / Restore My Cloud Data", fontSize = 12.sp, color = Color(0xFF38BDF8))
                        }
                    } else {
                        Column {
                            Text(
                                text = "Sign in or create your personal account to ensure all transactions and loans are backed up to your dedicated cloud partition.",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val client = GoogleAuthDriveHelper.getGoogleSignInClient(context)
                                        googleSignInLauncher.launch(client.signInIntent)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Google SSO", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { showManualAccountDialog = true },
                                    modifier = Modifier.weight(1.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Manual Account", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. Cloud Backup & Automatic Sync (No Manual Push)
        // -------------------------------------------------------------
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CLOUD BACKUP & AUTO-SYNC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9CA3AF),
                            letterSpacing = 1.sp
                        )
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF10B981)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Real-Time MongoDB Auto-Sync Active Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF064E3B).copy(alpha = 0.5f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF047857)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Auto-Sync",
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚡ Real-Time Auto-Sync: ACTIVE",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Every transaction, loan & bank auto-syncs silently to MongoDB in the background.",
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Google Drive Backup & Import Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E293B))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1D4ED8)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Google Drive",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google Drive Backup",
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            val lastBackup = GoogleAuthDriveHelper.getLastDriveBackupTime(context)
                            val backupText = if (lastBackup > 0) {
                                "Last: " + SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(lastBackup))
                            } else {
                                "Ready to backup / restore"
                            }
                            Text(
                                text = backupText,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.backupToGoogleDrive(context) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Backup to Drive", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { driveImportLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import from Drive", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 3. Export to Excel Button & Dashboard Report
        // -------------------------------------------------------------
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "DATA EXPORT & SPREADSHEETS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Export your entire financial diary with executive KPI summaries, bank cash flows, category spending, loans, and borrowed money registers directly into Microsoft Excel or Google Sheets.",
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { viewModel.exportToExcel(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF047857))
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export to Excel (.csv / .xlsx)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 4. Bank Accounts & Cash Flow Management
        // -------------------------------------------------------------
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "BANK ACCOUNTS & CASH FLOW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF9CA3AF),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${dashboardState.bankAccounts.size} banks connected",
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Button(
                            onClick = { showAddBankDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Bank", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (dashboardState.bankCashFlows.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No bank accounts added yet",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Add your bank accounts to see inflows, outflows, and net cash flow per bank.",
                                    color = Color(0xFF94A3AF),
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        dashboardState.bankCashFlows.forEach { flow ->
                            BankCashFlowCard(
                                flow = flow,
                                onDelete = { viewModel.deleteBankAccount(flow.bank) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 5. Notifications & Reminders
        // -------------------------------------------------------------
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "NOTIFICATIONS & REMINDERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF312E81)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFA5B4FC),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "1 Day Before Due Reminder",
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Alerts 24 hours prior to recurring bills & EMIs",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = reminderNotificationEnabled,
                            onCheckedChange = {
                                reminderNotificationEnabled = it
                                if (it) {
                                    RecurringReminderWorker.schedule(context)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0xFF064E3B)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            RecurringReminderWorker.checkAndTriggerDirectly(
                                context,
                                dashboardState.upcomingRecurring
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Trigger Test Bill Notification", fontSize = 13.sp, color = Color(0xFFA5B4FC))
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 6. Data Reset & Safety
        // -------------------------------------------------------------
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "DATA RESET & SAFETY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF271317))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reset All App Data",
                                color = Color(0xFFF87171),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Erase all transactions, banks, cards & loans to start fresh",
                                color = Color(0xFFFCA5A5),
                                fontSize = 11.sp
                            )
                        }
                        Button(
                            onClick = { showResetDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                        ) {
                            Text("Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // App Version Footer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Money Tracker v1.3.0 (Build 4)",
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Crafted with Jetpack Compose & Material 3",
                    color = Color(0xFF4B5563),
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // -------------------------------------------------------------
    // Manual Account Dialog
    // -------------------------------------------------------------
    if (showManualAccountDialog) {
        ManualAccountDialog(
            onDismiss = { showManualAccountDialog = false },
            onSave = { name, email ->
                GoogleAuthDriveHelper.saveManualUser(context, name, email)
                val profile = UserProfile(
                    isSignedIn = true,
                    displayName = name,
                    email = email
                )
                viewModel.setUserProfile(profile)
                showManualAccountDialog = false
                viewModel.pullUserDataFromMongo(email) { _, msg ->
                    statusFeedbackMessage = msg
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Add Bank Account Dialog
    // -------------------------------------------------------------
    if (showAddBankDialog) {
        AddBankDialog(
            onDismiss = { showAddBankDialog = false },
            onSave = { name, holder, last4, type, balance, color ->
                viewModel.addBankAccount(name, holder, last4, type, balance, color)
                showAddBankDialog = false
            }
        )
    }

    // -------------------------------------------------------------
    // Reset Data Confirmation Dialog
    // -------------------------------------------------------------
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = Color(0xFF1E293B),
            title = {
                Text("Reset All Data?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Are you sure you want to clear all data? This will permanently remove all transactions, bank accounts, credit cards, and borrowed money records.",
                    color = Color(0xFFCBD5E1)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Yes, Reset Everything", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    // -------------------------------------------------------------
    // Help & FAQ Dialog
    // -------------------------------------------------------------
    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    // Status Feedback Alert
    statusFeedbackMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { statusFeedbackMessage = null },
            containerColor = Color(0xFF1E293B),
            title = { Text("Cloud & Backup Status", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = Color(0xFFCBD5E1), fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = { statusFeedbackMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun ManualAccountDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, email: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = { Text("Create / Sign In Account", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Enter your Name and Email to partition your data on MongoDB Cloud and sync between devices.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = "" },
                    label = { Text("Your Name *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; errorMsg = "" },
                    label = { Text("Your Email *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg.isNotBlank()) {
                    Text(errorMsg, color = Color(0xFFEF4444), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || !email.contains("@")) {
                        errorMsg = "Please enter a valid Name and Email"
                        return@Button
                    }
                    onSave(name.trim(), email.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Activate Account", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun BankCashFlowCard(
    flow: BankCashFlow,
    onDelete: () -> Unit
) {
    val bank = flow.bank
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color(android.graphics.Color.parseColor(bank.colorHex.ifBlank { "#10B981" })),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = bank.bankName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${bank.accountType.displayName} ${if (bank.accountNumberLast4.isNotBlank()) "• xx" + bank.accountNumberLast4 else ""}",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Bank",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF334155), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Cash Flow Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Cash Inflow", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text("+₹%,.0f".format(flow.totalInflow), color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text("Cash Outflow", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text("-₹%,.0f".format(flow.totalOutflow), color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text("Net Cash Flow", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    val netColor = if (flow.netCashFlow >= 0) Color(0xFF34D399) else Color(0xFFF87171)
                    val sign = if (flow.netCashFlow >= 0) "+" else ""
                    Text("$sign₹%,.0f".format(flow.netCashFlow), color = netColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column {
                    Text("Current Bal", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    Text("₹%,.0f".format(flow.currentBalance), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBankDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, holder: String, last4: String, type: BankAccountType, balance: Double, color: String) -> Unit
) {
    var bankName by remember { mutableStateOf("") }
    val accountHolder by remember { mutableStateOf("") }
    var last4Digits by remember { mutableStateOf("") }
    var initialBalance by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(BankAccountType.SAVINGS) }
    val selectedColor by remember { mutableStateOf("#10B981") }
    var errorMessage by remember { mutableStateOf("") }

    val presetBanks = listOf("HDFC Bank", "State Bank of India", "ICICI Bank", "Axis Bank", "Kotak Mahindra", "Punjab National Bank")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Text("Add Bank Account", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Select popular bank or enter custom:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetBanks.take(3).forEach { preset ->
                        FilterChip(
                            selected = bankName == preset,
                            onClick = { bankName = preset },
                            label = { Text(preset.take(8), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it; errorMessage = "" },
                    label = { Text("Bank Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF475569)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = last4Digits,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) last4Digits = it },
                        label = { Text("Last 4 Digits") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF475569)
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = initialBalance,
                        onValueChange = { initialBalance = it },
                        label = { Text("Opening Bal (₹) *") },
                        modifier = Modifier.weight(1.2f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF475569)
                        ),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Account Type:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BankAccountType.values().forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.displayName.take(7), fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage, color = Color(0xFFEF4444), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (bankName.isBlank()) {
                        errorMessage = "Please enter Bank Name"
                        return@Button
                    }
                    val bal = initialBalance.toDoubleOrNull() ?: 0.0
                    onSave(bankName, accountHolder, last4Digits, selectedType, bal, selectedColor)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Save Bank Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E293B),
        title = {
            Text("Help & User Guide", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            LazyColumn(modifier = Modifier.height(350.dp)) {
                item {
                    HelpSectionItem(
                        title = "Bank Accounts & Cash Flow",
                        body = "Link your Bank Accounts to see how much cash flows in and out of each account. When adding transactions, choose the bank to automatically calculate real-time net balances."
                    )
                    HelpSectionItem(
                        title = "Borrowed & Split Money",
                        body = "Track money lent to colleagues or friends, split restaurant bills into parts with custom names, choose repayment methods (UPI, Cash, Bank Transfer), and record repayments."
                    )
                    HelpSectionItem(
                        title = "MongoDB Real-Time Sync",
                        body = "Whatever data is added or modified is automatically pushed to your dedicated MongoDB Cloud account in the background without needing manual sync."
                    )
                    HelpSectionItem(
                        title = "Google Drive Backup & Import",
                        body = "Backup your data or import a backup JSON file whenever switching accounts or devices."
                    )
                    HelpSectionItem(
                        title = "Export to Excel & Sheets",
                        body = "Generates a complete multi-sheet spreadsheet report formatted for Excel and Google Sheets, including KPI summaries, bank balances, category analysis, and active loan EMIs."
                    )
                    HelpSectionItem(
                        title = "1-Day Before Payment Alerts",
                        body = "Automatically notifies you 24 hours prior to recurring bill and loan EMI due dates so you never miss an auto-debit."
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Text("Got it")
            }
        }
    )
}

@Composable
fun HelpSectionItem(title: String, body: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(title, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(body, color = Color(0xFFCBD5E1), fontSize = 12.sp, lineHeight = 16.sp)
    }
}
