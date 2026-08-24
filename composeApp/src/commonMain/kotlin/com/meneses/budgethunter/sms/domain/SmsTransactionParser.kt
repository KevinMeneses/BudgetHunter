package com.meneses.budgethunter.sms.domain

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

/**
 * Reads the text of a bank SMS to decide whether it reports a movement,
 * in which direction the money went and how much it was.
 *
 * All the matching is done over an accent insensitive lowercase copy of the message
 * because banks are not consistent with accents.
 */
object SmsTransactionParser {

    fun analyze(messageBody: String, bankConfig: BankSmsConfig): SmsAnalysis {
        if (messageBody.isBlank()) return SmsAnalysis.Ignored

        val text = messageBody.normalizeForMatching()

        if (IGNORED_PATTERN.containsMatchIn(text)) {
            return SmsAnalysis.Ignored
        }

        // No marker tells where the money went, the wording is not covered yet
        val type = detectType(text) ?: return SmsAnalysis.Unrecognized
        val amount = extractAmount(messageBody, bankConfig) ?: return SmsAnalysis.AmountNotFound
        val description = extractDescription(messageBody, bankConfig)

        return SmsAnalysis.Transaction(
            amount = amount,
            description = description,
            type = type
        )
    }

    /**
     * The direction is given by the marker that appears first in the message, since banks
     * lead with the verb ("Recibiste ... de una transferencia"). Transfer only messages,
     * like most Bre-B ones, are treated as money going out.
     */
    private fun detectType(text: String): BudgetEntry.Type? {
        val incomeIndex = INCOME_PATTERN.find(text)?.range?.first
        val outcomeIndex = OUTCOME_PATTERN.find(text)?.range?.first
        return when {
            incomeIndex != null && (outcomeIndex == null || incomeIndex < outcomeIndex) -> BudgetEntry.Type.INCOME
            outcomeIndex != null -> BudgetEntry.Type.OUTCOME
            TRANSFER_PATTERN.containsMatchIn(text) -> BudgetEntry.Type.OUTCOME
            else -> null
        }
    }

    /**
     * Markers are matched as whole words, otherwise a notice about the limit for "compras"
     * would be read as a "compra" and a movement would be invented out of an advice.
     */
    private fun markersPattern(vararg markerLists: List<String>): Regex {
        val alternatives = markerLists
            .flatMap { it }
            .sortedByDescending { it.length } // the longest phrase has to win the alternation
            .joinToString("|")
        return Regex("""\b(?:$alternatives)\b""")
    }

    /**
     * Currency anchored patterns are tried first because they are the only ones that cannot
     * confuse the amount with an account number, a card ending or a number of installments.
     * The bank specific regex is kept as a second option for the formats it was tuned for.
     */
    private fun extractAmount(messageBody: String, bankConfig: BankSmsConfig): String? {
        for (pattern in CURRENCY_ANCHORED_PATTERNS) {
            firstValidAmount(pattern, messageBody)?.let { return it }
        }

        bankConfig.transactionAmountRegex?.let { regex ->
            firstValidAmount(regex, messageBody)?.let { return it }
        }

        return firstValidAmount(KEYWORD_ANCHORED_PATTERN, messageBody)
    }

    private fun firstValidAmount(regex: Regex, messageBody: String): String? {
        for (match in regex.findAll(messageBody)) {
            val group = match.groupValues
                .drop(1)
                .firstOrNull { it.isNotBlank() && it.any { char -> char.isDigit() } }
                ?: continue

            val start = messageBody.indexOf(group, startIndex = match.range.first)
            if (start > 0 && messageBody[start - 1] == '*') {
                // Masked account or card number, not an amount
                continue
            }

            val normalized = normalizeAmount(group)
            val value = normalized.toDoubleOrNull() ?: continue
            if (value > 0) return normalized
        }
        return null
    }

    /**
     * The first sentence of the message, without the bank name that opens it. It keeps the
     * merchant, the account and the date together so the entry can be reviewed and
     * categorized by hand later on.
     */
    private fun extractDescription(messageBody: String, bankConfig: BankSmsConfig): String? =
        messageBody
            .removeBankPrefix(bankConfig)
            .takeFirstSentence()
            .trim()
            .take(MAX_DESCRIPTION_LENGTH)
            .trim()
            .takeIf { it.isNotEmpty() }

    private fun String.removeBankPrefix(bankConfig: BankSmsConfig): String {
        val colonIndex = indexOf(':')
        if (colonIndex in 0 until BANK_PREFIX_MAX_LENGTH) {
            return substring(colonIndex + 1)
        }

        val keyword = bankConfig.senderKeywords.firstOrNull { startsWith(it, ignoreCase = true) }
        return if (keyword != null) substring(keyword.length) else this
    }

    /**
     * A dot only closes the sentence when a space or the end of the message follows it,
     * so neither the dots of "COP71.000,00" nor the ones of "T.Cred" cut the description.
     */
    private fun String.takeFirstSentence(): String {
        for (index in indices) {
            if (this[index] != '.') continue
            val next = getOrNull(index + 1)
            if (next == null || next.isWhitespace()) return substring(0, index)
        }
        return this
    }

    /**
     * Colombian banks write amounts as "50.000" (dot as thousands separator) but some
     * messages use the american format, so the separators are resolved by position and length.
     */
    fun normalizeAmount(amount: String): String {
        val clean = amount
            .replace(Regex("""[^\d.,]"""), "")
            .trim()
            .trim('.', ',')

        if (clean.isEmpty()) return ""

        val hasDot = clean.contains('.')
        val hasComma = clean.contains(',')

        return when {
            hasDot && hasComma ->
                if (clean.lastIndexOf(',') > clean.lastIndexOf('.')) {
                    // Colombian format: "560.575,67"
                    clean.replace(".", "").replace(",", ".")
                } else {
                    // American format: "125,678.00"
                    clean.replace(",", "")
                }

            hasComma -> clean.resolveSingleSeparator(',')
            hasDot -> clean.resolveSingleSeparator('.')
            else -> clean
        }
    }

    /**
     * A single separator followed by exactly three digits is a thousands separator
     * ("50.000", "1,500"), anything else is a decimal separator ("125.50", "50,5").
     */
    private fun String.resolveSingleSeparator(separator: Char): String {
        val isThousands = count { it == separator } > 1 || substringAfterLast(separator).length == 3
        return if (isThousands) replace(separator.toString(), "") else replace(separator, '.')
    }

    private fun String.normalizeForMatching(): String {
        val builder = StringBuilder(length)
        for (char in lowercase()) {
            val replacement = when (char) {
                'á' -> 'a'
                'é' -> 'e'
                'í' -> 'i'
                'ó' -> 'o'
                'ú', 'ü' -> 'u'
                'ñ' -> 'n'
                else -> char
            }
            builder.append(replacement)
        }
        return builder.toString()
    }

    /** Length of the longest bank name that can open a message, "Scotiabank Colpatria:". */
    private const val BANK_PREFIX_MAX_LENGTH = 30

    private const val MAX_DESCRIPTION_LENGTH = 200

    private val CURRENCY_ANCHORED_PATTERNS = listOf(
        Regex("""\$\s*([0-9][0-9.,]*)"""),
        Regex("""\b(?:cop|usd)\s*\$?\s*([0-9][0-9.,]*)""", RegexOption.IGNORE_CASE),
        Regex("""\b([0-9][0-9.,]*)\s*(?:pesos|cop)\b""", RegexOption.IGNORE_CASE)
    )

    private val KEYWORD_ANCHORED_PATTERN = Regex(
        """\b(?:por valor de|valor de|valor|monto de|monto|por|de)\s+\$?\s*([0-9][0-9.,]*)""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Money coming in. Kept ahead of the outgoing markers on purpose, a message can
     * mention both ("Recibiste una transferencia").
     */
    private val INCOME_MARKERS = listOf(
        "recibiste",
        "recibio",
        "has recibido",
        "recepcion de",
        "te enviaron",
        "te transfirieron",
        "te consignaron",
        "te llego",
        "te llegaron",
        "te depositaron",
        "depositaron",
        "consignacion",
        "consignaron",
        "abono",
        "abonamos",
        "abonaron",
        "acreditamos",
        "acreditado",
        "transferencia recibida",
        "ingreso por",
        "nomina",
        "devolucion",
        "reintegro",
        "reverso",
        "reversion"
    )

    /** Money going out. */
    private val OUTCOME_MARKERS = listOf(
        "compra",
        "compraste",
        "pagaste",
        "pago por",
        "pago de",
        "pago exitoso",
        "pago aprobado",
        "retiro",
        "retiraste",
        "avance",
        "transferiste",
        "enviaste",
        "envio de dinero",
        "se debito",
        "debito por",
        "debitamos",
        "cargo por",
        "descontamos",
        "transaccion por"
    )

    /** Movements without an explicit direction, most Bre-B messages fall here. */
    private val TRANSFER_MARKERS = listOf(
        "transferencia",
        "bre-b",
        "bre b",
        "breb",
        "llave"
    )

    /**
     * Advertising, offers and reminders. They frequently carry amounts and even
     * transaction words, so they veto the message before the direction is resolved.
     */
    private val PROMOTIONAL_MARKERS = listOf(
        "pre aprobado",
        "pre-aprobado",
        "preaprobado",
        "pre aprobada",
        "pre-aprobada",
        "preaprobada",
        "preaprobados",
        "preaprobadas",
        "te aprobamos",
        "libera tu cupo",
        "aumentamos tu cupo",
        "felicitaciones",
        "felicidades",
        "promocion",
        "promociones",
        "promocional",
        "descuento",
        "descuentos",
        "aprovecha",
        "aprovechalo",
        "sorteo",
        "sorteos",
        "premio",
        "premios",
        "ganate",
        "participa",
        "publicidad",
        "solicitalo",
        "solicita ya",
        "adquiere",
        "estrena",
        "tasa desde",
        "meses sin intereses",
        "sin cuota de manejo",
        "difiere tus compras",
        "compra ahora",
        "paga despues",
        "te invitamos",
        "actualiza tus datos",
        "encuesta",
        "aplican terminos",
        "aplican tyc",
        "aplican t&c",
        "pago minimo",
        "fecha limite de pago",
        "fecha maxima de pago",
        "recuerda pagar",
        "recuerde pagar",
        "no olvides pagar"
    )

    /**
     * Bank notices that talk about money without any money moving: limits, security codes
     * and declined operations. They are discarded as deliberately as advertising.
     */
    private val NON_MOVEMENT_MARKERS = listOf(
        "tope",
        "topes",
        "superaste",
        "excediste",
        "clave temporal",
        "clave dinamica",
        "codigo de seguridad",
        "codigo de verificacion",
        "rechazada",
        "rechazado",
        "declinada",
        "declinado",
        "no fue aprobada",
        "no pudimos procesar",
        "intento de compra",
        "intento de transaccion",
        "activaste",
        "desactivaste",
        "inscribiste",
        "registraste"
    )

    private val INCOME_PATTERN = markersPattern(INCOME_MARKERS)

    private val OUTCOME_PATTERN = markersPattern(OUTCOME_MARKERS)

    private val TRANSFER_PATTERN = markersPattern(TRANSFER_MARKERS)

    private val IGNORED_PATTERN = markersPattern(PROMOTIONAL_MARKERS, NON_MOVEMENT_MARKERS)
}
