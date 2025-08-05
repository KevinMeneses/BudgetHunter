package com.meneses.budgethunter.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.meneses.budgethunter.MyApplication
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.bank.BankSmsConfig
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SmsService(
    private val context: Context,
    private val preferencesManager: PreferencesManager = MyApplication.preferencesManager,
    private val budgetEntryRepository: BudgetEntryRepository = BudgetEntryRepository(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    fun processSms(messageBody: String, bankConfig: BankSmsConfig) {
        scope.launch {
            try {
                val amountString = bankConfig.transactionAmountRegex?.find(messageBody)?.groups?.get(1)?.value
                val description = bankConfig.transactionDescriptionRegex?.find(messageBody)?.groups?.get(1)?.value?.trim()

                if (amountString != null) {
                    val cleanedAmountString = amountString.replace(",", "") // Quita comas para el parseo
                    val amount = cleanedAmountString.toDoubleOrNull()

                    if (amount != null) {
                        val budgetEntry = BudgetEntry(
                            amount = amount.toString(),
                            description = description ?: "Transacción de ${bankConfig.displayName}",
                            type = BudgetEntry.Type.OUTCOME,
                            budgetId = preferencesManager.defaultBudgetId
                        )
                        budgetEntryRepository.create(budgetEntry)
                        showTransactionNotification(budgetEntry)
                    } else {
                        Log.e("SmsService", "No se pudo parsear el monto: $amountString para ${bankConfig.displayName}")
                    }
                } else {
                    Log.d("SmsService", "No se encontró monto en el SMS para ${bankConfig.displayName} usando regex: ${bankConfig.transactionAmountRegex}")
                }

            } catch (e: Exception) {
                Log.e("SmsService", "Error procesando SMS para ${bankConfig.displayName}: ${e.message}", e)
                showNotification(
                    context.getString(R.string.transaction_failed),
                    "Error procesando SMS: ${e.message}"
                )
            }
        }
    }

    private suspend fun showTransactionNotification(budgetEntry: BudgetEntry) {
        withContext(Dispatchers.Main) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.budget_hunter_logo)
                .setContentTitle(context.getString(R.string.transaction_detected))
                .setContentText(
                    context.getString(
                        R.string.transaction_detected_message,
                        budgetEntry.amount
                    )
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(budgetEntry.description)
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkNotificationsPermission(context)) {
                NotificationManagerCompat
                    .from(context)
                    .notify(TRANSACTION_NOTIFICATION_ID, notification)
            }
        }
    }

    private suspend fun showNotification(title: String, message: String) {
        withContext(Dispatchers.Main) {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.budget_hunter_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
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
        private const val TRANSACTION_NOTIFICATION_ID = 1001
        private const val INFO_NOTIFICATION_ID = 1002
    }
} 
