package com.meneses.budgethunter.sms.domain

/**
 * Reads the alert emails banks send for each movement, which an iOS Shortcuts automation (trigger
 * "Correo") hands to the app the same way it hands an SMS: the body as one text, without the sender.
 * Android reads emails another way, from the email app's notification ([BankEmail]).
 *
 * An email is not read like an SMS: it greets the user, ends with links and legal text, and may name
 * another bank ("Recibiste 525.000 de ... desde el banco BBVA" in a Nequi email). So only the
 * sentence that reports the movement is read, and the bank is told by its email domain first.
 */
object ShortcutEmail {

    /**
     * An SMS fits in a few hundred characters; an email, with its greeting and footer, does not.
     * A bank SMS may also be broken over several lines, so the line count says nothing. A short text
     * that carries the email domain of one of [banks] is an email all the same: no SMS links there.
     */
    fun looksLikeEmail(text: String, banks: Collection<BankSmsConfig> = emptyList()): Boolean =
        text.length > SMS_MAX_LENGTH || banks.any { it.emailDomainIndex(text) != null }

    /**
     * The first sentence that names a movement and carries a number: "Recibiste $65,680 de JUAN
     * PEREZ". The subject ("¡Recibiste plata por Bre-B!") names it too but has no amount, and the
     * greeting, the dates and the footer do not name one.
     */
    fun movementPassage(text: String): String? =
        firstMovementSentence(text) ?: firstMovementSentence(unwrapParagraphs(text))

    /**
     * A plain-text body may come hard-wrapped (lines of about 78 characters), which can leave the
     * verb and the amount on different lines. Joined within each paragraph (blank lines keep them
     * apart), the sentence is whole again. Only tried when no line holds a movement as it is, so
     * emails that keep one item per line (account, date, ID) are not run together.
     */
    private fun unwrapParagraphs(text: String): String =
        text.split(PARAGRAPH_BREAK).joinToString("\n") { paragraph ->
            paragraph.lines().joinToString(" ") { it.trim() }
        }

    private fun firstMovementSentence(text: String): String? =
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
            // The passage may come from joined lines, so both are compared with their spacing collapsed
            val rest = text.collapseSpaces().replace(passage.collapseSpaces(), " ")
            banks.firstMentioned { it.mentionIndex(rest) }?.let { return it }
        }
        return banks.firstMentioned { it.mentionIndex(text) }
    }

    private fun Collection<BankSmsConfig>.firstMentioned(indexIn: (BankSmsConfig) -> Int?): BankSmsConfig? =
        mapNotNull { bank -> indexIn(bank)?.let { bank to it } }
            .minByOrNull { (_, index) -> index }
            ?.first

    private fun String.collapseSpaces(): String = replace(WHITESPACE, " ")

    /** Two SMS segments (160 characters each): a bank alert by SMS fits well under it. */
    private const val SMS_MAX_LENGTH = 320

    private val WHITESPACE = Regex("""\s+""")

    /** A blank line, maybe with spaces on it, between paragraphs. */
    private val PARAGRAPH_BREAK = Regex("""\n[ \t]*\n""")

    /** A sentence ends at ".", "!" or "?" followed by a space, so "a.m," and "$110,000.00" stay whole. */
    private val SENTENCE_END = Regex("""([.!?])\s+""")
}
