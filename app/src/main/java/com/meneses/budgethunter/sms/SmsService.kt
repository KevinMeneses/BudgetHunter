package com.meneses.budgethunter.sms

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.meneses.budgethunter.MainActivity
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.commons.bank.BankSmsConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsService(
    private val context: Context,
    private val smsMapper: SmsMapper = SmsMapper(),
    private val budgetEntryRepository: BudgetEntryRepository = BudgetEntryRepository(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    fun processSms(messageBody: String, bankConfigs: Set<BankSmsConfig>) {
        scope.launch {
            try {
                // Try to process the SMS with each bank configuration
                for (bankConfig in bankConfigs) {
                    val budgetEntry = smsMapper.smsToBudgetEntry(messageBody, bankConfig)
                    if (budgetEntry != null) {
                        budgetEntryRepository.create(budgetEntry)
                        showNotification(
                            title = context.getString(R.string.transaction_added),
                            message = context.getString(R.string.transaction_detected_message, budgetEntry.amount)
                        )
                        return@launch // Exit after successful processing
                    }
                }

                // If no bank configuration matched, show a notification
                showNotification(
                    title = context.getString(R.string.transaction_failed),
                    message = "No matching bank configuration found for this SMS"
                )
            } catch (e: Exception) {
                showNotification(
                    title = context.getString(R.string.transaction_failed),
                    message = "Error procesando SMS: ${e.message}"
                )
            }
        }
    }

    private suspend fun showNotification(title: String, message: String) {
        withContext(Dispatchers.Main) {
            val intent = Intent()
                .setClass(context, MainActivity::class.java)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

            val pendingIntent = PendingIntent.getActivity(
                /* context = */ context,
                /* requestCode = */ 999,
                /* intent = */ intent,
                /* flags = */ PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.budget_hunter_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkNotificationsPermission(context)) {
                NotificationManagerCompat
                    .from(context)
                    .notify(INFO_NOTIFICATION_ID, notification)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkNotificationsPermission(context: Context) =
        ContextCompat.checkSelfPermission(
            /* context = */ context,
            /* permission = */ Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

    companion object {
        private const val CHANNEL_ID = "sms_transactions"
        private const val INFO_NOTIFICATION_ID = 1002
    }
}
