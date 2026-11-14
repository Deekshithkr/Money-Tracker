package com.moneytracker.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.moneytracker.app.MainActivity
import com.moneytracker.app.MoneyTrackerApp
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class RecurringReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = context.applicationContext as? MoneyTrackerApp ?: return Result.success()
            val recurringItems = app.repository.activeRecurringItems.first()
            val now = System.currentTimeMillis()
            val oneDayMillis = 24 * 60 * 60 * 1000L

            createNotificationChannel(context)

            recurringItems.forEach { item ->
                val timeDiff = item.nextDueDateMillis - now
                // Trigger reminder if due within roughly 1 day (between 12 hours and 36 hours ahead)
                if (timeDiff in (12 * 60 * 60 * 1000L)..(36 * 60 * 60 * 1000L)) {
                    showNotification(context, item.id.toInt(), item.title, item.amount, item.defaultPaymentMethod.displayName)
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val CHANNEL_ID = "recurring_bills_reminders"
        const val WORK_NAME = "RecurringReminderPeriodicWork"

        fun schedule(context: Context) {
            createNotificationChannel(context)
            val request = PeriodicWorkRequestBuilder<RecurringReminderWorker>(
                12, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun checkAndTriggerDirectly(context: Context, items: List<com.moneytracker.app.model.RecurringItemEntity>) {
            createNotificationChannel(context)
            val now = System.currentTimeMillis()
            items.forEach { item ->
                val timeDiff = item.nextDueDateMillis - now
                if (timeDiff in (0L)..(48 * 60 * 60 * 1000L)) {
                    val daysText = if (timeDiff <= 24 * 60 * 60 * 1000L) "Tomorrow / Today" else "in 1-2 days"
                    showNotification(context, item.id.toInt(), item.title, item.amount, item.defaultPaymentMethod.displayName, daysText)
                }
            }
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Upcoming Bill Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts 1 day before recurring bills and loan EMIs are due"
                    enableVibration(true)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            }
        }

        private fun showNotification(
            context: Context,
            notificationId: Int,
            title: String,
            amount: Double,
            paymentMethod: String,
            timingText: String = "tomorrow"
        ) {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val formattedAmount = "₹%,.0f".format(amount)
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Bill Due Reminder: $formattedAmount for $title")
                .setContentText("Payment is due $timingText via $paymentMethod. Keep funds ready!")
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText("Payment of $formattedAmount for $title is scheduled $timingText via $paymentMethod. Please ensure your linked account or card has sufficient balance to prevent auto-debit failure.")
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            try {
                NotificationManagerCompat.from(context).notify(notificationId, notification)
            } catch (e: SecurityException) {
                // Permission not yet granted on Android 13+
            }
        }
    }
}
