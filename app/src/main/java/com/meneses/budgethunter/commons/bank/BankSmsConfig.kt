package com.meneses.budgethunter.commons.bank

data class BankSmsConfig(
    val id: String,
    val displayName: String,
    val senderKeywords: List<String> = emptyList(),
    val transactionAmountRegex: Regex? = null,
    val transactionDescriptionRegex: Regex? = null
)
