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
import kotlinx.coroutines.withContext

class BudgetLocalDataSource(
    private val queries: BudgetQueries,
    private val dispatcher: CoroutineDispatcher
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

    /**
     * Reads the database instead of [cachedList]: the cache is only filled while [budgets] is
     * collected (a screen is open) and lags behind writes such as [markAsSynced], so sync code
     * running in the background or right after a write would see a missing or stale budget.
     */
    suspend fun getById(id: Int): Budget? = withContext(dispatcher) {
        queries.selectById(id.toLong(), ::mapSelectAllToBudget).executeAsOneOrNull()
    }

    fun getByServerId(serverId: Long): Budget? =
        queries.selectByServerId(serverId, ::mapSelectAllToBudget).executeAsOneOrNull()

    fun getUnsynced(): List<Budget> =
        queries.selectUnsynced(::mapSelectAllToBudget).executeAsList()

    suspend fun getAllFilteredBy(filter: BudgetFilter): List<Budget> = cacheMutex.withLock {
        cachedList.filter {
            if (filter.name.isNullOrBlank()) true
            else it.name.lowercase()
                .contains(filter.name.lowercase())
        }
    }

    fun create(budget: Budget): Budget {
        var savedId = 0

        queries.transaction {
            queries.insert(
                name = budget.name,
                amount = budget.amount,
                date = budget.date,
                server_id = budget.serverId,
                is_synced = if (budget.isSynced) 1L else 0L,
                last_synced_at = budget.lastSyncedAt
            )

            savedId = queries
                .selectLastId()
                .executeAsOne()
                .toInt() // SQLite lastInsertRowId is Long; domain model uses Int for local IDs
        }

        return budget.copy(id = savedId)
    }

    fun update(budget: Budget) = queries.update(
        id = budget.id.toLong(), // Domain Int → SQLite Long for query parameter
        amount = budget.amount,
        name = budget.name,
        date = budget.date,
        server_id = budget.serverId,
        is_synced = if (budget.isSynced) 1L else 0L,
        last_synced_at = budget.lastSyncedAt
    )

    fun markAsSynced(id: Int, serverId: Long, lastSyncedAt: String) = queries.markAsSynced(
        server_id = serverId,
        last_synced_at = lastSyncedAt,
        id = id.toLong() // Domain Int → SQLite Long for query parameter
    )

    fun delete(id: Long) = queries.delete(id)

    fun clearAllData() = queries.deleteAll()

    fun getSyncedServerIds(): Set<Long> =
        queries.selectSyncedServerIds().executeAsList().toSet()

    /**
     * Removes the budgets with these server ids together with their entries. The schema declares
     * `ON DELETE CASCADE`, but SQLite only honours it with `PRAGMA foreign_keys`, which is off.
     */
    fun deleteByServerIds(serverIds: Collection<Long>) {
        if (serverIds.isEmpty()) return
        queries.transaction {
            queries.deleteEntriesByBudgetServerIds(serverIds)
            queries.deleteByServerIds(serverIds)
        }
    }

    /**
     * Removes every budget that reached the server, with its entries, keeping the ones created
     * offline that were never pushed.
     */
    fun deleteSynced() = queries.transaction {
        queries.deleteEntriesOfSyncedBudgets()
        queries.deleteSynced()
    }
}
