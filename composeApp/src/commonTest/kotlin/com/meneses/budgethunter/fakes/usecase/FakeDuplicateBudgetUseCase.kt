package com.meneses.budgethunter.fakes.usecase

import com.meneses.budgethunter.budgetList.application.DuplicateBudgetUseCase
import com.meneses.budgethunter.budgetList.domain.Budget

class FakeDuplicateBudgetUseCase : DuplicateBudgetUseCase(null!!, null!!, null!!) {
    val duplicatedBudgets = mutableListOf<Budget>()

    override suspend fun execute(budget: Budget) {
        duplicatedBudgets.add(budget)
    }
}
