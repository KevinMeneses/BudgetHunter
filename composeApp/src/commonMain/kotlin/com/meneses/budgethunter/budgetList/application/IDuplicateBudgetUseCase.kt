package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetList.domain.Budget

interface IDuplicateBudgetUseCase {
    suspend fun execute(budget: Budget)
}
