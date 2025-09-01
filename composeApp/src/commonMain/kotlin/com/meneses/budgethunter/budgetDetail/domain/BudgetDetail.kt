package com.meneses.budgethunter.budgetDetail.domain

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import kotlinx.serialization.Serializable

@Serializable
data class BudgetDetail(
    val budget: Budget = Budget(),
    val entries: List<BudgetEntry> = emptyList()
)