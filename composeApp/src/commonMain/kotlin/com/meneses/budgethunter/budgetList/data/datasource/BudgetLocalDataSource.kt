package com.meneses.budgethunter.budgetList.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.meneses.budgethunter.budgetList.data.mapSelectAllToBudget
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.db.BudgetQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BudgetLocalDataSource(
    private val queries: BudgetQueries,
    dispatcher: CoroutineDispatcher
) {
    private val cacheMutex = Mutex()
    private var cachedList: List<Budget> = emptyList()

    val budgets = queries
        .selectAll(::mapSelectAllToBudget)
        .asFlow()
        .mapToList(dispatcher)
        .onEach { 
            cacheMutex.withLock { 
                cachedList = it 
            } 
        }

    suspend fun getAllCached(): List<Budget> = cacheMutex.withLock { 
        cachedList 
    }

    suspend fun getById(id: Int): Budget? = cacheMutex.withLock {
        cachedList.firstOrNull { it.id == id }
    }

    suspend fun getAllFilteredBy(filter: BudgetFilter): List<Budget> = cacheMutex.withLock {
        cachedList.filter {
            if (filter.name.isNullOrBlank()) true
            else it.name.lowercase()
                .contains(filter.name.lowercase())
        }
    }

    fun create(budget: Budget): Budget {
        queries.insert(
            id = null,
            name = budget.name,
            amount = budget.amount,
            date = budget.date
        )

        val savedId = queries
            .selectLastId()
            .executeAsOne()
            .toInt()

        return budget.copy(id = savedId)
    }

    fun update(budget: Budget) = queries.update(
        id = budget.id.toLong(),
        amount = budget.amount,
        name = budget.name,
        date = budget.date
    )

    fun delete(id: Long) = queries.delete(id)
}