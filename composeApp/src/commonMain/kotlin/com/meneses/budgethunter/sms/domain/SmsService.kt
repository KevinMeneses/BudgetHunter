package com.meneses.budgethunter.sms.domain

interface SmsService {
    suspend fun processSms(messageBody: String, bankConfigs: Set<BankSmsConfig>)
}