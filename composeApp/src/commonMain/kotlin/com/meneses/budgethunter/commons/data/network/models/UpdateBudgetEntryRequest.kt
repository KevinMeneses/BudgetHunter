package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

@Serializable
data class UpdateBudgetEntryRequest(
    val id: Long,
    val budgetId: Long,
    val amount: Double,
    val description: String,
    val category: String,
    val type: String
)
