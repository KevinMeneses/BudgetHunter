package com.meneses.budgethunter.fakes.usecase

import com.meneses.budgethunter.budgetMetrics.application.IGetTotalsPerCategoryUseCase

class FakeGetTotalsPerCategoryUseCase(
    private val totalsMap: Map<String, Double> = emptyMap()
) : IGetTotalsPerCategoryUseCase {
    override suspend fun execute(): Map<String, Double> {
        return totalsMap
    }
}
