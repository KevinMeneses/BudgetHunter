package com.meneses.budgethunter.sms

import android.content.Context
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.commons.bank.BankSmsConfig
import com.meneses.budgethunter.commons.platform.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ProcessSmsUseCase(
    private val context: Context,
    private val smsMapper: SmsMapper,
    private val budgetEntryRepository: BudgetEntryRepository,
    private val notificationService: NotificationService,
    private val scope: CoroutineScope
) : SmsService {
    override fun processSms(messageBody: String, bankConfigs: Set<BankSmsConfig>) {
        scope.launch {
            try {
                // Try to process the SMS with each bank configuration
                for (bankConfig in bankConfigs) {
                    val budgetEntry = smsMapper.smsToBudgetEntry(messageBody, bankConfig)
                    if (budgetEntry != null) {
                        budgetEntryRepository.create(budgetEntry)
                        notificationService.showNotification(
                            title = context.getString(R.string.transaction_added),
                            message = context.getString(R.string.transaction_detected_message, budgetEntry.amount)
                        )
                        return@launch // Exit after successful processing
                    }
                }

                // If no bank configuration matched, show a notification
                notificationService.showNotification(
                    title = context.getString(R.string.transaction_failed),
                    message = "No matching bank configuration found for this SMS"
                )
            } catch (e: Exception) {
                notificationService.showNotification(
                    title = context.getString(R.string.transaction_failed),
                    message = "Error procesando SMS: ${e.message}"
                )
            }
        }
    }
}
