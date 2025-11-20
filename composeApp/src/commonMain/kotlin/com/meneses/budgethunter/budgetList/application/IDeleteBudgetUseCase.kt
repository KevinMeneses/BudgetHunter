package com.meneses.budgethunter.budgetList.application

interface IDeleteBudgetUseCase {
    suspend fun execute(budgetId: Long)
}
