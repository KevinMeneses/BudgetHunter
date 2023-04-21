package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetList.domain.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    val budgetList: Flow<List<Budget>>
    fun getAllBudgets(): List<Budget>
    fun getBudgetsBy(budget: Budget): List<Budget>
    fun createBudget(budget: Budget): Budget
    fun updateBudget(budget: Budget)
    fun deleteBudget(budget: Budget)
}