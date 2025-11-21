package com.meneses.budgethunter.fakes.usecase

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetMetrics.application.IGetTotalsPerCategoryUseCase

class FakeGetTotalsPerCategoryUseCase(
    private val totalsMap: Map<String, Double> = emptyMap()
) : IGetTotalsPerCategoryUseCase {
    override suspend fun execute(): Map<BudgetEntry.Category, Double> {
        // Convert String keys to BudgetEntry.Category
        return totalsMap.mapKeys { (key, _) ->
            BudgetEntry.Category.entries.firstOrNull { it.name == key }
                ?: BudgetEntry.Category.OTHER
        }
    }
}
