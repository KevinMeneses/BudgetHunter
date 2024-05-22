package com.meneses.budgethunter.budgetList.data.repository

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    val budgets: Flow<List<Budget>>
    fun getAll(): List<Budget>
    fun getAllFilteredBy(filter: BudgetFilter): List<Budget>
    fun create(budget: Budget): Budget
    suspend fun update(budget: Budget)
    suspend fun delete(budget: Budget)
}
