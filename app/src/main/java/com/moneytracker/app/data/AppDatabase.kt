package com.moneytracker.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.moneytracker.app.model.BorrowedItemEntity
import com.moneytracker.app.model.CreditCardEntity
import com.moneytracker.app.model.RecurringItemEntity
import com.moneytracker.app.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope

@Database(
    entities = [
        TransactionEntity::class,
        CreditCardEntity::class,
        RecurringItemEntity::class,
        BorrowedItemEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun recurringDao(): RecurringDao
    abstract fun borrowedDao(): BorrowedDao

    suspend fun clearAllData() {
        transactionDao().deleteAll()
        creditCardDao().deleteAll()
        recurringDao().deleteAll()
        borrowedDao().deleteAll()
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_tracker_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
