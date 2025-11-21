package com.meneses.budgethunter.fakes.usecase

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetMetrics.application.IGetTotalsPerCategoryUseCase

class FakeGetTotalsPerCategoryUseCase(
    private val totalsMap: Map<BudgetEntry.Category, Double> = emptyMap()
) : IGetTotalsPerCategoryUseCase {
    override suspend fun execute(): Map<BudgetEntry.Category, Double> {
        return totalsMap
    }
}
