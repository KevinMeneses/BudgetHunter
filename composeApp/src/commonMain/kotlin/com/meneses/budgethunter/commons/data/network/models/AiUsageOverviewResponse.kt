package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

/** `GET /api/ai/usage`: every monthly AI allowance of the account. A feature off on the server has nulls. */
@Serializable
data class AiUsageOverviewResponse(
    val plan: String,
    val period: String,
    val receiptScansUsed: Int? = null,
    val receiptScansLimit: Int? = null,
    val receiptScansRemaining: Int? = null,
    val categorizedEntriesUsed: Int? = null,
    val categorizedEntriesLimit: Int? = null,
    val categorizedEntriesRemaining: Int? = null,
    val resetsOn: String
)
