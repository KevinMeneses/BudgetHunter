package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

@Serializable
data class CreateBudgetRequest(
    val name: String,
    val amount: Double
)
