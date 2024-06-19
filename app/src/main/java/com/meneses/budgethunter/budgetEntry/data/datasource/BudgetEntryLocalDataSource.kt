package com.meneses.budgethunter.budgetEntry.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.meneses.budgethunter.budgetEntry.data.toDomain
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.commons.data.AndroidDatabaseFactory
import com.meneses.budgethunter.db.BudgetEntryQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class BudgetEntryLocalDataSource(
    private val queries: BudgetEntryQueries = AndroidDatabaseFactory().create().budgetEntryQueries,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    fun getAll() = cachedEntries

    fun selectAllByBudgetId(budgetId: Long) = queries
        .selectAllByBudgetId(budgetId)
        .asFlow()
        .mapToList(dispatcher)
        .map { it.toDomain() }
        .onEach { cachedEntries = it }

    fun getAllFilteredBy(filter: BudgetEntryFilter) =
        cachedEntries.filter {
            if (filter.description.isNullOrBlank()) true
            else it.description.lowercase()
                .contains(filter.description.lowercase())
        }.filter {
            if (filter.type == null) true
            else it.type == filter.type
        }.filter {
            if (filter.startDate == null) true
            else it.date >= filter.startDate
        }.filter {
            if (filter.endDate == null) true
            else it.date <= filter.endDate
        }

    fun create(budgetEntry: BudgetEntry) = queries.insert(
        id = null,
        budgetId = budgetEntry.budgetId.toLong(),
        amount = budgetEntry.amount.toDoubleOrNull() ?: 0.0,
        description = budgetEntry.description,
        type = budgetEntry.type,
        date = budgetEntry.date,
        invoice = budgetEntry.invoice
    )

    fun update(budgetEntry: BudgetEntry) = queries.update(
        id = budgetEntry.id.toLong(),
        budgetId = budgetEntry.budgetId.toLong(),
        amount = budgetEntry.amount.toDoubleOrNull() ?: 0.0,
        description = budgetEntry.description,
        type = budgetEntry.type,
        date = budgetEntry.date,
        invoice = budgetEntry.invoice
    )

    fun deleteByIds(list: List<Long>) =
        queries.deleteByIds(list)

    companion object {
        private var cachedEntries = emptyList<BudgetEntry>()
    }
}
