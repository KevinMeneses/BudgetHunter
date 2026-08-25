package com.meneses.budgethunter.budgetMetrics.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

sealed interface BudgetMetricsIntent {
    data class SelectType(val type: BudgetEntry.Type) : BudgetMetricsIntent
}
