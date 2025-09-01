package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class BudgetRepository(
    private val localDataSource: BudgetLocalDataSource,
    private val ioDispatcher: CoroutineDispatcher
) {
    val budgets: Flow<List<Budget>>
        get() = localDataSource.budgets

    suspend fun getById(id: Int): Budget? =
        localDataSource.getById(id)

    suspend fun getAllCached(): List<Budget> =
        localDataSource.getAllCached()

    suspend fun getAllFilteredBy(filter: BudgetFilter): List<Budget> =
        localDataSource.getAllFilteredBy(filter)

    suspend fun create(budget: Budget) = withContext(ioDispatcher) {
        localDataSource.create(budget)
    }

    suspend fun update(budget: Budget) = withContext(ioDispatcher) {
        localDataSource.update(budget)
    }
}