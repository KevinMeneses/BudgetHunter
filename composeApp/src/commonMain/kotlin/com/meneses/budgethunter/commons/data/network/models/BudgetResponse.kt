package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

@Serializable
data class BudgetResponse(
    val id: Long,
    val name: String,
    val amount: Double
)
