package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

@Serializable
data class BudgetEntryResponse(
    val id: Long,
    val budgetId: Long,
    val amount: Double,
    val description: String,
    val category: String,
    val type: String,
    val createdByEmail: String,
    val updatedByEmail: String?,
    val creationDate: String,
    val modificationDate: String
)
