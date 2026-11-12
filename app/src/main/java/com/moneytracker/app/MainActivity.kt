package com.moneytracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.moneytracker.app.ui.MainScreen
import com.moneytracker.app.ui.MainViewModel
import com.moneytracker.app.ui.theme.MoneyTrackerTheme

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as MoneyTrackerApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MoneyTrackerTheme(darkTheme = true) {
                MainScreen(viewModel = mainViewModel)
            }
        }
    }
}
