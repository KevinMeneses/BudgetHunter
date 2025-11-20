package com.meneses.budgethunter.fakes.usecase

import com.meneses.budgethunter.budgetList.application.IDeleteBudgetUseCase

class FakeDeleteBudgetUseCase : IDeleteBudgetUseCase {
    val deletedBudgetIds = mutableListOf<Long>()

    override suspend fun execute(budgetId: Long) {
        deletedBudgetIds.add(budgetId)
    }
}
