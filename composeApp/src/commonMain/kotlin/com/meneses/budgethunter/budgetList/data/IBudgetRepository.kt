package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import kotlinx.coroutines.flow.Flow

interface IBudgetRepository {
    val budgets: Flow<List<Budget>>
    suspend fun getById(id: Int): Budget?
    suspend fun getAllCached(): List<Budget>
    suspend fun getAllFilteredBy(filter: BudgetFilter): List<Budget>
    suspend fun create(budget: Budget): Budget
    suspend fun update(budget: Budget)
}
