package com.meneses.budgethunter.budgetMetrics.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetMetrics.domain.CategoryMetric

data class BudgetMetricsState(
    val selectedType: BudgetEntry.Type = BudgetEntry.Type.OUTCOME,
    val categoryMetrics: List<CategoryMetric> = emptyList(),
    val total: Double = 0.0,
    /** True until the metrics of [selectedType] are read, so an empty chart means "nothing there". */
    val isLoading: Boolean = true,
    /** Entries the dialog offers to categorize; null while there is nothing to offer. */
    val categorizationOffer: Int? = null,
    val isCategorizing: Boolean = false,
    /** What the last run ended in, until the screen has shown it. */
    val categorizationOutcome: CategorizationOutcome? = null
)
