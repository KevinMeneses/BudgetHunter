package com.meneses.budgethunter.budgetEntry.domain

/**
 * Outcome of asking the AI to extract a budget entry from an invoice.
 * Distinguishes why nothing was extracted so the UI can tell the user instead of failing silently.
 */
sealed interface AiExtractionResult {
    data class Success(val entry: BudgetEntry) : AiExtractionResult
    data class Failure(val reason: AiFailureReason) : AiExtractionResult
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

    /** HTTP 5xx from the AI service. */
    SERVER,

    /** The model refused to process the content (safety / policy). */
    BLOCKED,

    /** The service rejected the request (HTTP 4xx other than 429), e.g. invalid key or unreadable file. */
    REJECTED,

    /** The response could not be understood. */
    INVALID_RESPONSE,

    /** The invoice file could not be read or converted. */
    FILE,

    UNKNOWN
}
