package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val email: String,
    val name: String,
    /** Whether this member owns the budget (`GET /api/budgets/{id}/collaborators`); false from older servers. */
    val isOwner: Boolean = false
)
