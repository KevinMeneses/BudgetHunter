package com.meneses.budgethunter.sms.application

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.sms.data.SmsMapper
import com.meneses.budgethunter.sms.domain.SmsService

class ProcessSmsUseCase(
    private val smsMapper: SmsMapper,
    private val budgetEntryRepository: BudgetEntryRepository,
    private val notificationManager: NotificationManager,
) : SmsService {
    override suspend fun processSms(messageBody: String, bankConfigs: Set<BankSmsConfig>) {
        try {
            // Try to process the SMS with each bank configuration
            for (bankConfig in bankConfigs) {
                val budgetEntry = smsMapper.smsToBudgetEntry(messageBody, bankConfig)
                if (budgetEntry != null) {
                    budgetEntryRepository.create(budgetEntry)
                    notificationManager.showNotification(
                        title = "Transacción agregada", // TODO: get from string resources
                        message = "Se detectó una transacción por ${budgetEntry.amount}"
                    )
                    return // Exit after successful processing
                }
            }

            // If no bank configuration matched, show a notification
            notificationManager.showNotification(
                title = "Transacción fallida", // TODO: get from string resources
                message = "No se encontró configuración bancaria para este SMS"
            )
        } catch (e: Exception) {
            notificationManager.showNotification(
                title = "Error de transacción", // TODO: get from string resources
                message = "Error procesando SMS: ${e.message}"
            )
        }
    }
}