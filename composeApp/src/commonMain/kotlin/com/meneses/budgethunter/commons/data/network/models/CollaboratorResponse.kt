package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

@Serializable
data class CollaboratorResponse(
    val budgetId: Long,
    val budgetName: String,
    val collaboratorEmail: String,
    val collaboratorName: String
)
