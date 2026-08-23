package com.meneses.budgethunter.sms.application

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.transaction_added
import budgethunter.composeapp.generated.resources.transaction_added_message
import budgethunter.composeapp.generated.resources.transaction_error
import budgethunter.composeapp.generated.resources.transaction_error_message
import budgethunter.composeapp.generated.resources.transaction_failed
import budgethunter.composeapp.generated.resources.transaction_failed_message
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.resources.StringResourceProvider
import com.meneses.budgethunter.sms.data.SmsMapper
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.sms.domain.SmsService

class ProcessSmsUseCase(
    private val smsMapper: SmsMapper,
    private val budgetEntryRepository: BudgetEntryRepository,
    private val notificationManager: NotificationManager,
    private val stringResourceProvider: StringResourceProvider
) : SmsService {
    override suspend fun processSms(messageBody: String, bankConfigs: Set<BankSmsConfig>) {
        try {
            // Try to process the SMS with each bank configuration
            for (bankConfig in bankConfigs) {
                val budgetEntry = smsMapper.smsToBudgetEntry(messageBody, bankConfig)
                if (budgetEntry != null) {
                    budgetEntryRepository.create(budgetEntry)
                    notificationManager.showNotification(
                        title = stringResourceProvider.getString(Res.string.transaction_added),
                        message = stringResourceProvider.getString(
                            Res.string.transaction_added_message,
                            budgetEntry.amount
                        )
                    )
                    return // Exit after successful processing
                }
            }

            // If no bank configuration matched, show a notification
            notificationManager.showNotification(
                title = stringResourceProvider.getString(Res.string.transaction_failed),
                message = stringResourceProvider.getString(Res.string.transaction_failed_message)
            )
        } catch (e: Exception) {
            notificationManager.showNotification(
                title = stringResourceProvider.getString(Res.string.transaction_error),
                message = stringResourceProvider.getString(
                    Res.string.transaction_error_message,
                    e.message.orEmpty()
                )
            )
        }
    }
}
