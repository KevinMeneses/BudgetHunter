package com.meneses.budgethunter.budgetEntry.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.meneses.budgethunter.budgetEntry.data.toDomain
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.db.BudgetEntryQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BudgetEntryLocalDataSource(
    private val queries: BudgetEntryQueries,
    private val dispatcher: CoroutineDispatcher
) {
    private val cacheMutex = Mutex()
    private var cachedEntries: List<BudgetEntry> = emptyList()

    suspend fun getAllCached(): List<BudgetEntry> = cacheMutex.withLock {
        cachedEntries
    }

    fun selectAllByBudgetId(budgetId: Long) = queries
        .selectAllByBudgetId(budgetId)
        .asFlow()
        .mapToList(dispatcher)
        .map { it.toDomain() }
        .onEach {
            cacheMutex.withLock {
                cachedEntries = it
            }
        }

    suspend fun getAllFilteredBy(filter: BudgetEntryFilter): List<BudgetEntry> =
        cacheMutex.withLock {
            cachedEntries.asSequence().filter {
                if (filter.description.isNullOrBlank()) true
                else it.description.lowercase()
                    .contains(filter.description.lowercase())
            }.filter {
                if (filter.type == null) true
                else it.type == filter.type
            }.filter {
                if (filter.category == null) true
                else it.category == filter.category
            }.filter {
                if (filter.startDate == null) true
                else it.date >= filter.startDate
            }.filter {
                if (filter.endDate == null) true
                else it.date <= filter.endDate
            }.toList()
        }

    fun create(budgetEntry: BudgetEntry) = queries.insert(
        id = null,
        budgetId = budgetEntry.budgetId.toLong(),
        amount = budgetEntry.amount.toDoubleOrNull() ?: 0.0,
        description = budgetEntry.description,
        type = budgetEntry.type,
        category = budgetEntry.category,
        date = budgetEntry.date,
        invoice = budgetEntry.invoice,
        server_id = budgetEntry.serverId,
        is_synced = if (budgetEntry.isSynced) 1L else 0L,
        created_by_email = budgetEntry.createdByEmail,
        updated_by_email = budgetEntry.updatedByEmail,
        creation_date = budgetEntry.creationDate,
        modification_date = budgetEntry.modificationDate
    )

    fun update(budgetEntry: BudgetEntry) = queries.update(
        id = budgetEntry.id.toLong(),
        budgetId = budgetEntry.budgetId.toLong(),
        amount = budgetEntry.amount.toDoubleOrNull() ?: 0.0,
        description = budgetEntry.description,
        type = budgetEntry.type,
        category = budgetEntry.category,
        date = budgetEntry.date,
        invoice = budgetEntry.invoice,
        server_id = budgetEntry.serverId,
        is_synced = if (budgetEntry.isSynced) 1L else 0L,
        created_by_email = budgetEntry.createdByEmail,
        updated_by_email = budgetEntry.updatedByEmail,
        creation_date = budgetEntry.creationDate,
        modification_date = budgetEntry.modificationDate
    )

    fun deleteByIds(list: List<Long>) =
        queries.deleteByIds(list)

    fun delete(id: Long) =
        queries.deleteByIds(listOf(id))

    fun deleteAllByBudgetId(budgetId: Long) =
        queries.deleteAllByBudgetId(budgetId)

    fun clearAllData() = queries.deleteAll()
}
