package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

/**
 * What the account can share as a budget owner, in `GET /api/billing/status` and `POST /api/billing/sync`.
 * A null limit means unlimited (PREMIUM); [sharedBudgetsUsed] can be above the limit, since nothing already
 * shared is taken away.
 */
@Serializable
data class SharingStatusResponse(
    val plan: String = "FREE",
    val sharedBudgetsLimit: Int? = null,
    val sharedBudgetsUsed: Int = 0,
    val collaboratorsPerBudgetLimit: Int? = null
)

/** `POST /api/budgets/{id}/collaborators` past the owner's sharing limit: HTTP 402, code SHARING_LIMIT_REACHED. */
@Serializable
data class SharingErrorResponse(
    val status: Int = 0,
    val code: String = "",
    val message: String = "",
    val sharing: SharingLimitDetailResponse? = null
)

@Serializable
data class SharingLimitDetailResponse(
    /** SHARED_BUDGETS or COLLABORATORS_PER_BUDGET. */
    val limitType: String = "",
    /** The owner's plan, whose limit applies, not the caller's. */
    val plan: String = "FREE",
    val limit: Int = 0,
    val used: Int = 0,
    /** Only the owner's upgrade lifts the limit, so only the owner is offered Premium. */
    val callerIsOwner: Boolean = false
)
