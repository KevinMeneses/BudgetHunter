package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

@Serializable
data class AddCollaboratorRequest(
    val budgetId: Long,
    val email: String
)
