package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

@Serializable
data class BudgetResponse(
    val id: Long,
    val name: String,
    val amount: Double,
    val date: String? = null,
    /** Users with access, the caller included; null from a server that predates the field. */
    val collaboratorCount: Int? = null
)
