package com.meneses.budgethunter.budgetEntry.data.repository

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.toDb
import com.meneses.budgethunter.budgetEntry.data.toDomain
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
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

    override fun getAllFilteredBy(budgetEntry: BudgetEntry) =
        cachedEntries.filter { it.type == budgetEntry.type }

    override fun create(budgetEntry: BudgetEntry) =
        budgetEntryLocalDataSource.insert(budgetEntry.toDb())

    override fun update(budgetEntry: BudgetEntry) =
        budgetEntryLocalDataSource.update(budgetEntry.toDb())

    companion object {
        private var cachedEntries = emptyList<BudgetEntry>()
    }
}