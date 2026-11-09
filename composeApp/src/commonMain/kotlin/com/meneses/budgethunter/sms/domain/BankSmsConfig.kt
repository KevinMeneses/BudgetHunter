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
     * written by whoever sends the email, so anyone can call themselves "Bancolombia". In the body
     * of an email handed over by the iOS shortcut they name the bank more reliably than its text,
     * which may name another bank ("desde el banco BBVA").
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
    private fun mentionsItself(text: String): Boolean = mentionIndex(text) != null

    /** Where [text] first names the bank as a whole word, or null when it does not. */
    fun mentionIndex(text: String): Int? {
        val normalizedText = text.normalizeForMatching()
        return (senderKeywords + displayName)
            .filter { name -> name.any { it.isLetter() } }
            .mapNotNull { normalizedText.wordIndex(it.normalizeForMatching()) }
            .minOrNull()
    }

    /**
     * Where [text] first carries one of the bank's email domains, as a sender or in a link, or null.
     * The domain must stand whole: nothing of a name before it ("xnequi.com.co", "-nequi.com.co") and
     * nothing that continues it after ("nequi.com.cox", "nequi.com.co.evil.com"). "www.", "@", a space
     * or a closing dot of a sentence are fine.
     */
    fun emailDomainIndex(text: String): Int? {
        val lowercaseText = text.lowercase()
        return emailDomains
            .mapNotNull { domain -> lowercaseText.domainIndex(domain.lowercase()) }
            .minOrNull()
    }

    private fun String.domainIndex(domain: String): Int? {
        var index = indexOf(domain)
        while (index >= 0) {
            val end = index + domain.length
            // A dot before is a subdomain ("www.", "alertas."); a letter, digit or hyphen is another name
            val before = getOrNull(index - 1)
            val startsWhole = before == null || !(before.isLetterOrDigit() || before == '-')
            val after = getOrNull(end)
            val endsWhole = after == null || !after.isDomainPart() ||
                (after == '.' && getOrNull(end + 1)?.isLetterOrDigit() != true)
            if (startsWhole && endsWhole) return index
            index = indexOf(domain, index + 1)
        }
        return null
    }

    /** A character that can be part of a host name; a dot only counts when a label follows it. */
    private fun Char.isDomainPart(): Boolean = isLetterOrDigit() || this == '-' || this == '.'

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
