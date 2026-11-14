package com.moneytracker.app

import android.app.Application
import com.moneytracker.app.data.AppDatabase
import com.moneytracker.app.data.MoneyTrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MoneyTrackerApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy {
        MoneyTrackerRepository(
            database.transactionDao(),
            database.creditCardDao(),
            database.recurringDao(),
            database.borrowedDao(),
            database.bankAccountDao()
        )
    }
}
