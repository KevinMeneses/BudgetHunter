package com.meneses.budgethunter.sms.domain

data class BankSmsConfig(
    val id: String,
    val displayName: String,
    val senderKeywords: List<String> = emptyList(),
    val transactionAmountRegex: Regex? = null,
    /** Android packages of the bank's app, for banks read from their app notifications. */
    val appPackages: List<String> = emptyList()
)
