package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntryList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class LocalBudgetEntryRepository : BudgetEntryRepository {

    override val budgetEntries: Flow<List<BudgetEntry>> get() = _budgetEntries
    private val _budgetEntries = MutableStateFlow<List<BudgetEntry>>(emptyList())

    override fun getEntriesByBudgetId(id: Int) {
        val entries = budgetEntryList.filter { it.budgetId == id }
        _budgetEntries.value = entries
    }

    override fun getEntriesBy(budgetEntry: BudgetEntry) {
        val itemList = budgetEntryList.filter { it.budgetId == budgetEntry.budgetId }
        val entries = itemList.filter { it.type == budgetEntry.type }
        _budgetEntries.value = entries
    }

    override fun putEntry(budgetEntry: BudgetEntry) {
        budgetEntryList.removeIf { it.id == budgetEntry.id }
        budgetEntryList.add(budgetEntry.id, budgetEntry)
    }
}