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

    /**
     * An SMS fits in a few hundred characters; an email, with its greeting and footer, does not.
     * Only the length decides: a bank SMS may also be broken over several lines.
     */
    fun looksLikeEmail(text: String): Boolean = text.length > SMS_MAX_LENGTH

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
     * The bank that sent the email: the one whose domain it carries first, else the one it names
     * first. A bank named in the [passage] of the movement may be the other party's ("desde el banco
     * BBVA"), so names in the rest of the email (the signature, the links) count before those.
     * Null when it carries or names none.
     */
    fun bankOf(text: String, banks: Collection<BankSmsConfig>, passage: String? = null): BankSmsConfig? {
        banks.firstMentioned { it.emailDomainIndex(text) }?.let { return it }
        if (passage != null) {
            val rest = text.replace(passage, " ")
            banks.firstMentioned { it.mentionIndex(rest) }?.let { return it }
        }
        return banks.firstMentioned { it.mentionIndex(text) }
    }

    private fun Collection<BankSmsConfig>.firstMentioned(indexIn: (BankSmsConfig) -> Int?): BankSmsConfig? =
        mapNotNull { bank -> indexIn(bank)?.let { bank to it } }
            .minByOrNull { (_, index) -> index }
            ?.first

    private const val SMS_MAX_LENGTH = 320

    /** A sentence ends at ".", "!" or "?" followed by a space, so "a.m," and "$110,000.00" stay whole. */
    private val SENTENCE_END = Regex("""([.!?])\s+""")
}
