package com.meneses.budgethunter.fakes.usecase

import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase

class FakeDeleteBudgetUseCase : DeleteBudgetUseCase(null!!, null!!, null!!) {
    val deletedBudgetIds = mutableListOf<Long>()

    override suspend fun execute(budgetId: Long) {
        deletedBudgetIds.add(budgetId)
    }
}
