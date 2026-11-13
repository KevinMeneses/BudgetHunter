package com.meneses.budgethunter.budgetMetrics.application

/** How a run of the automatic categorization ended, in the terms the metrics screen tells apart. */
sealed interface CategorizationOutcome {
    /**
     * [pending] entries are still waiting: the server could not place them or hit its per-run cap, or, with
     * [quotaExceeded], the account's monthly AI categorizations ran out (the paywall is offered).
     */
    data class Done(
        val categorized: Int,
        val pending: Long,
        val quotaExceeded: Boolean = false
    ) : CategorizationOutcome

    /** The server is already categorizing this budget (409). */
    data object AlreadyRunning : CategorizationOutcome

    /** AI processing is off for the account even after saving the preferences (403). */
    data object AiOff : CategorizationOutcome

    data class Failed(val error: Throwable) : CategorizationOutcome
}
