package com.meneses.budgethunter.sms

import com.meneses.budgethunter.commons.bank.BankSmsConfig

interface SmsService {
    fun processSms(messageBody: String, bankConfigs: Set<BankSmsConfig>)
}
