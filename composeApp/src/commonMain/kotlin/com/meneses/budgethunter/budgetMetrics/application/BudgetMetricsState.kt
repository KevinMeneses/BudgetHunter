package com.meneses.budgethunter.budgetMetrics.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetMetrics.domain.CategoryMetric

data class BudgetMetricsState(
    val selectedType: BudgetEntry.Type = BudgetEntry.Type.OUTCOME,
    val categoryMetrics: List<CategoryMetric> = emptyList(),
    val total: Double = 0.0
)
