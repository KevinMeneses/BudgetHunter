package com.meneses.budgethunter.sms.domain

/**
 * Reads the alert emails banks send for each movement, which an iOS Shortcuts automation (trigger
 * "Correo") hands to the app the same way it hands an SMS.
 *
 * An email is not read like an SMS: it greets the user, ends with links and legal text, and may name
 * another bank ("Recibiste 525.000 de ... desde el banco BBVA" in a Nequi email). So only the
 * sentence that reports the movement is read, and the bank is told by its email domain first.
 */
object BankEmail {

    /** An SMS fits in a few hundred characters on one or two lines; an email does not. */
    fun looksLikeEmail(text: String): Boolean =
        text.length > SMS_MAX_LENGTH || text.lines().count { it.isNotBlank() } >= EMAIL_MIN_LINES

    /**
     * The first sentence that names a movement and carries a number: "Recibiste $65,680 de JUAN
     * PEREZ". The subject ("¡Recibiste plata por Bre-B!") names it too but has no amount, and the
     * greeting, the dates and the footer do not name one.
     */
    fun movementPassage(text: String): String? =
        text.replace(SENTENCE_END, "$1\n")
            .lines()
            .map { it.trim() }
            .firstOrNull { sentence ->
                sentence.any { it.isDigit() } && SmsTransactionParser.mentionsMovement(sentence)
            }

    /**
     * The bank that sent the email: the one whose domain it carries, else the one it names first.
     * Null when it names none, and the caller then reads it as an SMS would be.
     */
    fun bankOf(text: String, banks: Collection<BankSmsConfig>): BankSmsConfig? =
        banks.firstOrNull { it.hasEmailDomainIn(text) }
            ?: banks
                .mapNotNull { bank -> bank.mentionIndex(text)?.let { bank to it } }
                .minByOrNull { (_, index) -> index }
                ?.first

    private const val SMS_MAX_LENGTH = 320
    private const val EMAIL_MIN_LINES = 3

    /** A sentence ends at ".", "!" or "?" followed by a space, so "a.m," and "$110,000.00" stay whole. */
    private val SENTENCE_END = Regex("""([.!?])\s+""")
}
