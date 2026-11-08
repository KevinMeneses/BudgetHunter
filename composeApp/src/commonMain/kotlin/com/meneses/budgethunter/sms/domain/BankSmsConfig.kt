package com.meneses.budgethunter.sms.domain

data class BankSmsConfig(
    val id: String,
    val displayName: String,
    val senderKeywords: List<String> = emptyList(),
    val transactionAmountRegex: Regex? = null,
    /** Android packages of the bank's app, for banks read from their app notifications. */
    val appPackages: List<String> = emptyList(),
    /**
     * Domains of the bank's alert emails ("nequi.com.co"). Its emails name it reliably only there,
     * in the sender or the links: the text may name another bank ("desde el banco BBVA").
     */
    val emailDomains: List<String> = emptyList()
) {
    /** Movements come by SMS, recognized by the sender. */
    val readsSms: Boolean get() = senderKeywords.isNotEmpty()

    /** Movements come in the notifications of the bank's app (Android only). */
    val readsNotifications: Boolean get() = appPackages.isNotEmpty()

    /**
     * The description of an entry from this bank, opening with its name ("Bancolombia: Compraste ...")
     * so the entry tells which bank it came from. Most messages do not say it, since the sender or
     * the app already does; one that already names the bank is kept as it is.
     */
    fun describe(text: String): String = when {
        text.isBlank() -> displayName
        mentionsItself(text) -> text
        else -> "$displayName: $text"
    }

    /** Whether [text] names the bank, by its name or a sender keyword that is a word, never a number. */
    private fun mentionsItself(text: String): Boolean = mentionIndex(text) != null

    /** Where [text] first names the bank as a whole word, or null when it does not. */
    fun mentionIndex(text: String): Int? {
        val normalizedText = text.normalizeForMatching()
        return (senderKeywords + displayName)
            .filter { name -> name.any { it.isLetter() } }
            .mapNotNull { normalizedText.wordIndex(it.normalizeForMatching()) }
            .minOrNull()
    }

    /** Whether [text] carries one of the bank's email domains, as a sender or in a link. */
    fun hasEmailDomainIn(text: String): Boolean {
        val lowercaseText = text.lowercase()
        return emailDomains.any { lowercaseText.contains(it.lowercase()) }
    }

    /** Whole words only: "Nu" is not found inside "nueva". */
    private fun String.wordIndex(word: String): Int? {
        var index = indexOf(word)
        while (index >= 0) {
            val before = getOrNull(index - 1)
            val after = getOrNull(index + word.length)
            if (before?.isLetterOrDigit() != true && after?.isLetterOrDigit() != true) return index
            index = indexOf(word, index + 1)
        }
        return null
    }
}
