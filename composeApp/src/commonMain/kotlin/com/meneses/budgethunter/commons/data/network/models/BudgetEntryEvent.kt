package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

/**
 * Lightweight SSE notification for budget entry changes.
 *
 * The server now sends only notifications instead of full entry data to reduce bandwidth.
 * Clients should fetch the complete entry data separately when needed.
 */
@Serializable
data class BudgetEntryEvent(
    val budgetId: Long,
    val entryId: Long,
    val action: BudgetEntryAction,
    val userInfo: UserInfo
)

@Serializable
enum class BudgetEntryAction {
    CREATED,
    UPDATED,
    DELETED
}
