package com.meneses.budgethunter.sms.domain

/**
 * What to type in a Shortcuts "Message" automation to catch the SMS of one bank: the numbers it
 * sends from and the names it signs with. Both come from the same keywords the Android receiver
 * matches, so the two platforms accept the same messages.
 */
data class BankMessageFilter(
    val bankName: String,
    val senders: List<String>,
    val keywords: List<String>
) {
    companion object {
        fun of(bankConfig: BankSmsConfig): BankMessageFilter {
            val (senders, names) = bankConfig.senderKeywords.partition { keyword -> keyword.all { it.isDigit() } }
            return BankMessageFilter(
                bankName = bankConfig.displayName,
                senders = senders.distinct(),
                // "Bancolombia" and "BANCOLOMBIA" are the same filter, matching ignores case
                keywords = names.distinctBy { it.lowercase() }
            )
        }

        val ALL: List<BankMessageFilter> = SupportedBanks.ALL_BANKS.map(::of)
    }
}
