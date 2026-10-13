package com.meneses.budgethunter.budgetMetrics.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

sealed interface BudgetMetricsIntent {
    data class SelectType(val type: BudgetEntry.Type) : BudgetMetricsIntent
    data object ConfirmCategorization : BudgetMetricsIntent
    data object DeclineCategorization : BudgetMetricsIntent
    data object OutcomeShown : BudgetMetricsIntent
}
