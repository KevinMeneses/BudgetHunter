package com.meneses.budgethunter.budgetEntry.domain

data class BudgetEntryFilter(
    val description: String? = null,
    val type: BudgetEntry.Type? = null,
    val category: BudgetEntry.Category? = null,
    val startDate: String? = null,
    val endDate: String? = null
)