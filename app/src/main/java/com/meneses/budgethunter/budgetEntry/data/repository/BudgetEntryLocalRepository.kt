package com.meneses.budgethunter.budgetEntry.data.repository

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.toDb
import com.meneses.budgethunter.budgetEntry.data.toDomain
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class BudgetEntryLocalRepository(
    private val budgetEntryLocalDataSource: BudgetEntryLocalDataSource = BudgetEntryLocalDataSource()
) : BudgetEntryRepository {

    override fun getAll() = cachedEntries

    override fun getAllByBudgetId(budgetId: Int) = budgetEntryLocalDataSource
        .selectAllByBudgetId(budgetId.toLong())
        .map { it.toDomain() }
        .onEach { cachedEntries = it }

    override fun getAllFilteredBy(filter: BudgetEntryFilter) =
        cachedEntries
            .filter {
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

    override fun create(budgetEntry: BudgetEntry) =
        budgetEntryLocalDataSource.insert(budgetEntry.toDb())

    override fun update(budgetEntry: BudgetEntry) =
        budgetEntryLocalDataSource.update(budgetEntry.toDb())

    override fun deleteByIds(ids: List<Int>) {
        val dbIds = ids.map { it.toLong() }
        budgetEntryLocalDataSource.deleteByIds(dbIds)
    }

    companion object {
        private var cachedEntries = emptyList<BudgetEntry>()
    }
}