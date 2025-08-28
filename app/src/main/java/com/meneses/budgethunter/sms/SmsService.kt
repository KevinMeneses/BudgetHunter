package com.meneses.budgethunter.sms

import com.meneses.budgethunter.commons.bank.BankSmsConfig

interface SmsService {
    suspend fun processSms(messageBody: String, bankConfigs: Set<BankSmsConfig>)
}
