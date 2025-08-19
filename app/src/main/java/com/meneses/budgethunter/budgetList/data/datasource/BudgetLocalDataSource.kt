package com.meneses.budgethunter.budgetList.data.datasource

import androidx.lifecycle.AtomicReference
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.meneses.budgethunter.budgetList.data.mapSelectAllToBudget
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.commons.data.AndroidDatabaseFactory
import com.meneses.budgethunter.db.BudgetQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach

class BudgetLocalDataSource(
    private val queries: BudgetQueries = AndroidDatabaseFactory().create().budgetQueries,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val budgets = queries
        .selectAll(::mapSelectAllToBudget)
        .asFlow()
        .mapToList(dispatcher)
        .onEach { cachedList.set(it) }

    fun getAllCached(): List<Budget> = cachedList.get()

    fun getById(id: Int): Budget? =
        cachedList.get().firstOrNull { it.id == id }

    fun getAllFilteredBy(filter: BudgetFilter) =
        cachedList.get().filter {
            if (filter.name.isNullOrBlank()) true
            else it.name.lowercase()
                .contains(filter.name.lowercase())
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

    companion object {
        private val cachedList = AtomicReference(emptyList<Budget>())
    }
}
