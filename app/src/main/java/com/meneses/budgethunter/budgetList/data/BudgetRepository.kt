package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetList.domain.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    val budgetList: Flow<List<Budget>>
    fun getAllBudgets()
    fun getBudgetsBy(budget: Budget)
    fun createBudget(budget: Budget): Budget
}