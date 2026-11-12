package com.meneses.budgethunter.budgetEntry.data.datasource

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.meneses.budgethunter.budgetEntry.data.toDomain
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
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

    fun setServerId(id: Int, serverId: Long) =
        queries.setServerId(server_id = serverId, id = id.toLong())

    /**
     * Stores the server's answer to pushing [pushed]. The entry always gets its server id (so the
     * next push updates it instead of creating it again), but takes the server's values and is
     * marked synced only if it still holds what was pushed. An edit made while the request was in
     * flight stays pending and is pushed next, instead of being overwritten and marked synced.
     */
    fun savePushResponse(pushed: BudgetEntry, synced: BudgetEntry) = queries.transaction {
        val id = pushed.id.toLong()
        queries.setServerId(server_id = synced.serverId, id = id)
        queries.markPushedIfUnchanged(
            amount = synced.amount.toDoubleOrNull() ?: 0.0,
            description = synced.description,
            type = synced.type,
            date = synced.date,
            category = synced.category,
            created_by_email = synced.createdByEmail,
            updated_by_email = synced.updatedByEmail,
            creation_date = synced.creationDate,
            modification_date = synced.modificationDate,
            id = id,
            pushedAmount = pushed.amount.toDoubleOrNull() ?: 0.0,
            pushedDescription = pushed.description,
            pushedType = pushed.type,
            pushedDate = pushed.date,
            pushedCategory = pushed.category
        )
    }

    fun deleteByIds(list: List<Long>) =
        queries.deleteByIds(list)

    fun delete(id: Long) =
        queries.deleteByIds(listOf(id))

    fun deleteAllByBudgetId(budgetId: Long) =
        queries.deleteAllByBudgetId(budgetId)

    fun clearAllData() = queries.deleteAll()

    suspend fun selectByServerId(serverId: Long) = cacheMutex.withLock {
        queries.selectByServerId(serverId).executeAsOneOrNull()
    }

    fun countUnsynced(): Long = queries.countUnsynced().executeAsOne()

    fun getUnsynced(localBudgetId: Int): List<BudgetEntry> {
        return queries.selectUnsyncedByBudgetId(localBudgetId.toLong())
            .executeAsList()
            .toDomain()
    }

    suspend fun selectByUniqueFields(
        budgetId: Long,
        amount: Double,
        description: String,
        creationDate: String
    ) = cacheMutex.withLock {
        queries.selectByUniqueFields(
            budgetId = budgetId,
            amount = amount,
            description = description,
            creationDate = creationDate
        ).executeAsOneOrNull()
    }
}
