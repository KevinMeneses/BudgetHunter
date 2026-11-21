package com.meneses.budgethunter.billing.domain

/** The account's plan as the server sees it right now. */
data class BillingStatus(
    val isPremium: Boolean,
    /** End of the paid period (ISO-8601, UTC), or null when free or when it does not run out. */
    val premiumUntil: String?,
    /** Null when the server does not send it (older servers). */
    val sharing: SharingStatus? = null
)

/** How a purchase ended, for the paywall. */
sealed interface PurchaseOutcome {
    /**
     * Paid. [status] is what the server says after syncing, or null when it could not be asked: the webhook
     * still brings premium a moment later, so this is not a failure.
     */
    data class Purchased(val status: BillingStatus?) : PurchaseOutcome

    data object Cancelled : PurchaseOutcome

    data object Pending : PurchaseOutcome
}

/** One monthly AI allowance. */
data class Allowance(val used: Int, val limit: Int, val remaining: Int) {
    /**
     * Few enough left to say so where they are spent: [RUNNING_LOW] or fewer. A count, not a share of the
     * limit, because "few" is a count to the user: on FREE (10 scans) it warns for the last 3, and on PREMIUM
     * (1000, fair use) also for the last 3 instead of from 200 on, which would nag a paying account for weeks.
     */
    val isRunningLow: Boolean get() = remaining <= RUNNING_LOW

    companion object {
        const val RUNNING_LOW = 3
    }
}

/** This month's AI usage (`GET /api/ai/usage`). A feature turned off on the server has a null allowance. */
data class AiUsage(
    val receiptScans: Allowance?,
    val categorizations: Allowance?,
    /** First day of next month (ISO date, UTC), when the counts start again. */
    val resetsOn: String
)
