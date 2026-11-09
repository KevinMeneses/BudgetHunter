package com.meneses.budgethunter.sms.domain

data class BankSmsConfig(
    val id: String,
    val displayName: String,
    val senderKeywords: List<String> = emptyList(),
    val transactionAmountRegex: Regex? = null,
    /** Android packages of the bank's app, for banks read from their app notifications. */
    val appPackages: List<String> = emptyList(),
    /**
     * The domains the bank sends its emails from (subdomains included). When the email app tells
     * the sender's address, only these prove an email comes from the bank: a display name is
     * written by whoever sends the email, so anyone can call themselves "Bancolombia".
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

    /**
     * Whether [sender] is this bank, as an email app shows who wrote ("Bancolombia",
     * "Notificaciones Davivienda"): it carries the bank's name or one of its sender keywords.
     */
    fun isSender(sender: String): Boolean = sender.isNotBlank() && mentionsItself(sender)

    /** Whether [domain] (of a sender's address, lowercase) is one of the bank's or under one of them. */
    fun sendsEmailFrom(domain: String): Boolean =
        emailDomains.any { domain == it || domain.endsWith(".$it") }

    /** Whether [text] names the bank, by its name or a sender keyword that is a word, never a number. */
    private fun mentionsItself(text: String): Boolean {
        val normalizedText = text.normalizeForMatching()
        return (senderKeywords + displayName)
            .filter { name -> name.any { it.isLetter() } }
            .map { it.normalizeForMatching() }
            .any { normalizedText.containsWord(it) }
    }

    /** Whole words only: "Nu" is not found inside "nueva". */
    private fun String.containsWord(word: String): Boolean {
        var index = indexOf(word)
        while (index >= 0) {
            val before = getOrNull(index - 1)
            val after = getOrNull(index + word.length)
            if (before?.isLetterOrDigit() != true && after?.isLetterOrDigit() != true) return true
            index = indexOf(word, index + 1)
        }
        return false
    }
}
