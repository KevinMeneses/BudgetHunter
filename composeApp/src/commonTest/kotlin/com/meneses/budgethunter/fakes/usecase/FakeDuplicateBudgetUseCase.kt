package com.meneses.budgethunter.fakes.usecase

import com.meneses.budgethunter.budgetList.application.IDuplicateBudgetUseCase
import com.meneses.budgethunter.budgetList.domain.Budget

class FakeDuplicateBudgetUseCase : IDuplicateBudgetUseCase {
    val duplicatedBudgets = mutableListOf<Budget>()

    override suspend fun execute(budget: Budget): Budget {
        duplicatedBudgets.add(budget)
        return budget.copy(id = budget.id + 1000)
    }
}
