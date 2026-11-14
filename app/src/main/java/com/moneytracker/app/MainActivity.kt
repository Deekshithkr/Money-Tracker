package com.moneytracker.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.moneytracker.app.notification.RecurringReminderWorker
import com.moneytracker.app.sync.GoogleAuthDriveHelper
import com.moneytracker.app.ui.MainScreen
import com.moneytracker.app.ui.MainViewModel
import com.moneytracker.app.ui.theme.MoneyTrackerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as MoneyTrackerApp).repository)
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Schedule recurring payment reminders (1 day before) in background
        RecurringReminderWorker.schedule(applicationContext)

        // Load cached Google user profile asynchronously off main thread
        lifecycleScope.launch(Dispatchers.IO) {
            val profile = GoogleAuthDriveHelper.getUserProfile(applicationContext)
            if (profile.isSignedIn) {
                mainViewModel.setUserProfile(profile)
            }
        }

        setContent {
            MoneyTrackerTheme(darkTheme = true) {
                MainScreen(viewModel = mainViewModel)
            }
        }
    }
}
