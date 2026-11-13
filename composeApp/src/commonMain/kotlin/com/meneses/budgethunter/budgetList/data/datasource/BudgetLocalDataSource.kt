package com.meneses.budgethunter.budgetList.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.meneses.budgethunter.budgetList.data.mapSelectAllToBudget
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.db.BudgetQueries
import com.meneses.budgethunter.db.PendingDeletionQueries
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class BudgetLocalDataSource(
    private val queries: BudgetQueries,
    private val pendingDeletionQueries: PendingDeletionQueries,
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

    /** Every budget, read from the database: see [getById] for why sync code must not use the cache. */
    suspend fun getAll(): List<Budget> = withContext(dispatcher) {
        queries.selectAll(::mapSelectAllToBudget).executeAsList()
    }

    fun getByServerId(serverId: Long): Budget? =
        queries.selectByServerId(serverId, ::mapSelectAllToBudget).executeAsOneOrNull()

    fun getUnsynced(): List<Budget> =
        queries.selectUnsynced(::mapSelectAllToBudget).executeAsList()

    fun countUnsynced(): Long = queries.countUnsynced().executeAsOne()

    /** Budgets and entries deleted here whose deletion has not reached the server yet. */
    fun countPendingDeletions(): Long = pendingDeletionQueries.countAll().executeAsOne()

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
                last_synced_at = budget.lastSyncedAt,
                collaborator_count = budget.collaboratorCount.toLong()
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
        last_synced_at = budget.lastSyncedAt,
        collaborator_count = budget.collaboratorCount.toLong()
    )

    /**
     * Stores what a pull brought for [budget] only if it has no change of its own still to be
     * pushed: one saved after the pull read the budget stays, pending, instead of being overwritten.
     */
    fun updateFromServer(budget: Budget) = queries.updateFromServerIfSynced(
        amount = budget.amount,
        name = budget.name,
        date = budget.date,
        collaborator_count = budget.collaboratorCount.toLong(),
        last_synced_at = budget.lastSyncedAt,
        id = budget.id.toLong()
    )

    /** Writes only a changed count: every write makes the budget list query run again. */
    suspend fun updateCollaboratorCount(serverId: Long, count: Int) = withContext(dispatcher) {
        val current = queries.selectByServerId(serverId, ::mapSelectAllToBudget).executeAsOneOrNull()
        if (current != null && current.collaboratorCount != count) {
            queries.updateCollaboratorCount(collaborator_count = count.toLong(), server_id = serverId)
        }
    }

    /**
     * Stores the result of pushing [pushed]: the budget always gets its server id (so the next
     * push updates it instead of creating it again), but is only marked synced if it still holds
     * the pushed name, amount and date. An edit made while the request was in flight stays
     * pending and is pushed next, instead of being marked synced and never sent.
     */
    fun markAsSynced(id: Int, serverId: Long, lastSyncedAt: String, pushed: Budget) = queries.transaction {
        // Domain Int → SQLite Long for query parameters
        queries.setServerId(server_id = serverId, id = id.toLong())
        queries.markSyncedIfUnchanged(
            last_synced_at = lastSyncedAt,
            id = id.toLong(),
            pushedName = pushed.name,
            pushedAmount = pushed.amount,
            pushedDate = pushed.date
        )
    }

    /**
     * Deletes a budget and its entries, recording the deletion to be pushed if the budget reached
     * the server (see PendingDeletion.sq). Entries are deleted here too: the schema's
     * `ON DELETE CASCADE` needs `PRAGMA foreign_keys`, which is off.
     */
    fun deleteRecordingPending(id: Int, deletedAt: String) = queries.transaction {
        queries.recordDeletion(created_at = deletedAt, id = id.toLong())
        queries.deleteEntriesOfBudget(id.toLong())
        queries.delete(id.toLong())
    }

    /** Server ids of the budgets deleted here whose deletion has not reached the server yet. */
    fun getPendingDeletions(): List<Long> = queries.selectPendingDeletions().executeAsList()

    fun removePendingDeletion(serverId: Long) = queries.removePendingDeletion(serverId)

    /** Also forgets the deletions waiting to be pushed: they belong to the account signing out. */
    fun clearAllData() = queries.transaction {
        queries.deleteAll()
        pendingDeletionQueries.deleteAll()
    }

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
     * offline that were never pushed. The deletions waiting to be pushed go too: they were made on
     * the previous account's budgets.
     */
    fun deleteSynced() = queries.transaction {
        queries.deleteEntriesOfSyncedBudgets()
        queries.deleteSynced()
        pendingDeletionQueries.deleteAll()
    }
}
