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
) : IBudgetRepository {
    override val budgets: Flow<List<Budget>>
        get() = localDataSource.budgets

    override suspend fun getById(id: Int): Budget? =
        localDataSource.getById(id)

    override suspend fun getAllCached(): List<Budget> =
        localDataSource.getAllCached()

    override suspend fun getAllFilteredBy(filter: BudgetFilter): List<Budget> =
        localDataSource.getAllFilteredBy(filter)

    override suspend fun create(budget: Budget) = withContext(ioDispatcher) {
        localDataSource.create(budget)
    }

    override suspend fun update(budget: Budget) = withContext(ioDispatcher) {
        localDataSource.update(budget)
    }
}
