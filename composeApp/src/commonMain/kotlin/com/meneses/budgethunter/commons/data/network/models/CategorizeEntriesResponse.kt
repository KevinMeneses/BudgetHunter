package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

/**
 * What a run of `POST /api/budgets/{id}/entries/categorize` did to a budget.
 */
@Serializable
data class CategorizeEntriesResponse(
    /** Entries that were given a category by this run. */
    val categorized: Int,

    /**
     * Entries still without a category afterwards: ones nobody could place, or that could not be
     * asked about yet because the AI provider's quota ran out. Asking again later retries them.
     */
    val pending: Long
)
