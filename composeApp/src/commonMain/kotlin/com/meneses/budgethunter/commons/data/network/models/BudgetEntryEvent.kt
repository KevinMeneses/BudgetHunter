package com.meneses.budgethunter.commons.data.network.models

import kotlinx.serialization.Serializable

@Serializable
data class BudgetEntryEvent(
    val budgetEntry: BudgetEntryResponse,
    val userInfo: UserInfo
)
