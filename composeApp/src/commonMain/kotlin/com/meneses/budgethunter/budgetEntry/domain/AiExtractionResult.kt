package com.meneses.budgethunter.budgetEntry.domain

import com.meneses.budgethunter.billing.domain.Allowance

/**
 * Outcome of asking the AI to extract a budget entry from an invoice.
 * Distinguishes why nothing was extracted so the UI can tell the user instead of failing silently.
 */
sealed interface AiExtractionResult {
    /** This month's receipt scans after this one, as the server answered them; null when it did not say. */
    val scans: Allowance?

    data class Success(val entry: BudgetEntry, override val scans: Allowance? = null) : AiExtractionResult
    data class Failure(val reason: AiFailureReason, override val scans: Allowance? = null) : AiExtractionResult
}

enum class AiFailureReason {
    /** The model answered but the document is not a receipt, invoice or bill. */
    NOT_AN_INVOICE,

    /** No connection, DNS or transport error. */
    NETWORK,

    /** The request took too long. */
    TIMEOUT,

    /** HTTP 429: quota or rate limit reached. */
    RATE_LIMITED,

    /** HTTP 5xx from the server or the AI service. */
    SERVER,

    /** The model refused to process the content (safety / policy). */
    BLOCKED,

    /** The service rejected the request (HTTP 4xx other than 429), e.g. invalid key or unreadable file. */
    REJECTED,

    /** The response could not be understood. */
    INVALID_RESPONSE,

    /** The invoice file could not be read or converted, or is too large or of an unsupported type. */
    FILE,

    /** The account used all the receipt scans its plan includes this month (HTTP 402). */
    QUOTA_EXCEEDED,

    /** Reading receipts goes through the account, so it needs a signed-in user (HTTP 401, offline mode). */
    SIGN_IN_REQUIRED,

    UNKNOWN
}
