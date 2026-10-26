package com.meneses.budgethunter.sms.domain

/**
 * Finds who the money went to (or came from) in a bank SMS, for a short summary of the movement.
 *
 * Banks write the merchant or the person in capitals after a preposition ("en EXITO POBLADO",
 * "a COMPAÑIA ENERGETICA", "de JUAN PEREZ"), so that is the only shape accepted. Precision over
 * recall: when nothing fits, the summary simply leaves the name out.
 */
object SmsCounterpartExtractor {

    fun extract(messageBody: String): String? {
        val match = COUNTERPART_PATTERN.find(messageBody) ?: return null
        val words = match.groupValues[1]
            .trim()
            .trimEnd('.', '-', '/')
            .split(WHITESPACE)
            .filter { it.isNotEmpty() }

        val kept = mutableListOf<String>()
        for (word in words) {
            if ((kept + word).joinToString(" ").length > MAX_LENGTH) break
            kept += word
        }
        // A name cut short must not end in "de" or "la"
        while (kept.size > 1 && kept.last().lowercase() in CONNECTORS) kept.removeAt(kept.lastIndex)

        return kept.joinToString(" ")
            .takeIf { it.isNotEmpty() }
            ?.toDisplayCase()
    }

    /** "EXITO POBLADO" reads as "Exito Poblado", short connectors stay in lowercase. */
    private fun String.toDisplayCase(): String =
        split(' ').mapIndexed { index, word ->
            val lower = word.lowercase()
            if (index > 0 && lower in CONNECTORS) lower else lower.replaceFirstChar { it.uppercaseChar() }
        }.joinToString(" ")

    private const val MAX_LENGTH = 40

    private val WHITESPACE = Regex("""\s+""")

    private val CONNECTORS = setOf("de", "del", "la", "las", "los", "el", "y")

    private const val CAPITAL = "A-ZÁÉÍÓÚÑÜ"
    private const val WORD_CHAR = "${CAPITAL}0-9&'.\\-/"

    /**
     * A lowercase preposition followed by one or more capitalised words. The first word starts
     * with a letter and is not a currency ("de COP50.000"), and every word must end where the
     * text does, so the "T.C" of "T.Cred" is not taken for a name.
     */
    private val COUNTERPART_PATTERN = Regex(
        """\b(?:en|a|de)\s+((?!(?:COP|USD))[$CAPITAL][$WORD_CHAR]+(?:[ ]+[$WORD_CHAR]+)*)(?=$|[\s,;:)])"""
    )
}
