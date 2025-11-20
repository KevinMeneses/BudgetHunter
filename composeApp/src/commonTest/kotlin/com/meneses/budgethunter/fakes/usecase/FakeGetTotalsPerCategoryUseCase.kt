package com.meneses.budgethunter.fakes.usecase

import com.meneses.budgethunter.budgetMetrics.application.GetTotalsPerCategoryUseCase
import kotlinx.coroutines.Dispatchers

class FakeGetTotalsPerCategoryUseCase(
    private val totalsMap: Map<String, Double> = emptyMap()
) : GetTotalsPerCategoryUseCase(
    budgetDetailRepository = null!!,
    ioDispatcher = Dispatchers.Default
) {
    override suspend fun execute(): Map<String, Double> {
        return totalsMap
    }
}
