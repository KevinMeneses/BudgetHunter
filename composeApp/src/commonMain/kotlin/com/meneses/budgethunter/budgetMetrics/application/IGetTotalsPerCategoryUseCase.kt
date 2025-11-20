package com.meneses.budgethunter.budgetMetrics.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

interface IGetTotalsPerCategoryUseCase {
    suspend fun execute(): Map<BudgetEntry.Category, Double>
}
