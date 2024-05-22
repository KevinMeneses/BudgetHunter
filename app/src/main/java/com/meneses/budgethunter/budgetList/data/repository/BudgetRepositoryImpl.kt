package com.meneses.budgethunter.budgetList.data.repository

import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.data.datasource.BudgetRemoteDataSource
import com.meneses.budgethunter.budgetList.data.toDb
import com.meneses.budgethunter.budgetList.data.toDomain
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withContext

class BudgetRepositoryImpl(
    private val localDataSource: BudgetLocalDataSource = BudgetLocalDataSource(),
    private val remoteDataSource: BudgetRemoteDataSource = BudgetRemoteDataSource(),
    private val preferencesManager: PreferencesManager = PreferencesManager(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BudgetRepository {
    override val budgets
        get() = localDataSource
            .budgets
            .toDomain()
            .onEach { cachedList = it }

    override fun getAll() = cachedList

    override fun getAllFilteredBy(filter: BudgetFilter) =
        cachedList.filter {
            if (filter.name.isNullOrBlank()) true
            else it.name.lowercase()
                .contains(filter.name.lowercase())
        }.filter {
            if (filter.frequency == null) true
            else it.frequency == filter.frequency
        }

    override fun create(budget: Budget): Budget {
        localDataSource.insert(budget.toDb())
        val savedId = localDataSource.selectLastId()
        return budget.copy(id = savedId)
    }

    override suspend fun update(budget: Budget) {
        localDataSource.update(budget.toDb())
        if (preferencesManager.isCollaborationEnabled) {
            remoteDataSource.sendUpdate(budget)
        }
    }

    override suspend fun delete(budget: Budget) {
        localDataSource.delete(budget.id.toLong())
        if (preferencesManager.isCollaborationEnabled) {
            remoteDataSource.closeStream()
        }
    }

    override suspend fun startCollaboration() = withContext(ioDispatcher) {
        preferencesManager.isCollaborationEnabled = true
        remoteDataSource.getBudgetStream()
            .takeWhile { preferencesManager.isCollaborationEnabled }
            .collect { budget ->
                val index = cachedList.indexOfFirst { it.id == budget.id }
                if (index != -1 && cachedList[index] != budget) {
                    localDataSource.update(budget.toDb())
                }
            }
    }

    companion object {
        private var cachedList = emptyList<Budget>()
    }
}
