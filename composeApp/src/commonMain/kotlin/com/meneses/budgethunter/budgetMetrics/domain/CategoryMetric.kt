package com.meneses.budgethunter.budgetMetrics.domain

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

/**
 * How much of a budget went into one category, and what share of the total that is.
 */
data class CategoryMetric(
    val category: BudgetEntry.Category,
    val amount: Double,
    val percentage: Double
)
