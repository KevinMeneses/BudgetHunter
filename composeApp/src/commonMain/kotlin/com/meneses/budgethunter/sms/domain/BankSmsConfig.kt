package com.meneses.budgethunter.sms.domain

data class BankSmsConfig(
    val id: String,
    val displayName: String,
    val senderKeywords: List<String> = emptyList(),
    val transactionAmountRegex: Regex? = null,
    /** Android packages of the bank's app, for banks read from their app notifications. */
    val appPackages: List<String> = emptyList()
) {
    /** Movements come by SMS, recognized by the sender. */
    val readsSms: Boolean get() = senderKeywords.isNotEmpty()

    /** Movements come in the notifications of the bank's app (Android only). */
    val readsNotifications: Boolean get() = appPackages.isNotEmpty()
}
