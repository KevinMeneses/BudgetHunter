package com.meneses.budgethunter.commons.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.adapter.categoryAdapter
import com.meneses.budgethunter.budgetList.data.adapter.typeAdapter
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.db.Budget_entry
import com.meneses.budgethunter.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Deletions waiting to be pushed (PendingDeletion.sq) and the other queries the sync relies on to
 * leave changes made here alone, against a real SQLite database.
 */
class PendingDeletionStoreTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: Database
    private lateinit var budgets: BudgetLocalDataSource
    private lateinit var entries: BudgetEntryLocalDataSource

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        database = Database(driver, Budget_entry.Adapter(typeAdapter, categoryAdapter))
        budgets = BudgetLocalDataSource(database.budgetQueries, database.pendingDeletionQueries, Dispatchers.Unconfined)
        entries = BudgetEntryLocalDataSource(database.budgetEntryQueries, Dispatchers.Unconfined)
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    private fun insertBudget(serverId: Long?): Int {
        database.budgetQueries.insert(
            amount = 100.0,
            name = "Budget",
            date = "2026-01-01",
            server_id = serverId,
            is_synced = if (serverId != null) 1L else 0L,
            last_synced_at = null,
            collaborator_count = 1L
        )
        return database.budgetQueries.selectLastId().executeAsOne().toInt()
    }

    private fun insertEntry(
        budgetId: Int,
        serverId: Long?,
        synced: Boolean = serverId != null,
        amount: Double = 10.0,
        description: String = "Lunch"
    ): Int {
        database.budgetEntryQueries.insert(
            id = null,
            budgetId = budgetId.toLong(),
            amount = amount,
            description = description,
            type = BudgetEntry.Type.OUTCOME,
            date = "2026-01-01",
            invoice = null,
            category = BudgetEntry.Category.FOOD,
            server_id = serverId,
            is_synced = if (synced) 1L else 0L,
            created_by_email = null,
            updated_by_email = null,
            creation_date = null,
            modification_date = null
        )
        return database.budgetQueries.selectLastId().executeAsOne().toInt()
    }

    private fun pendingCount() = database.pendingDeletionQueries.countAll().executeAsOne()

    private suspend fun entriesOf(budgetId: Int) = entries.selectAllByBudgetId(budgetId.toLong()).first()

    // ── Budgets ───────────────────────────────────────────────────────────────

    @Test
    fun `deleting a budget on the server records its deletion and removes it with its entries`() = runTest {
        val budgetId = insertBudget(serverId = 101)
        insertEntry(budgetId, serverId = 201)

        budgets.deleteRecordingPending(budgetId, deletedAt = "2026-01-02T10:00:00Z")

        assertEquals(null, budgets.getById(budgetId))
        assertEquals(emptyList(), entriesOf(budgetId))
        assertEquals(listOf(101L), budgets.getPendingDeletions())
    }

    @Test
    fun `deleting a budget never pushed records nothing`() = runTest {
        val budgetId = insertBudget(serverId = null)

        budgets.deleteRecordingPending(budgetId, deletedAt = "2026-01-02T10:00:00Z")

        assertEquals(null, budgets.getById(budgetId))
        assertEquals(0L, pendingCount())
    }

    @Test
    fun `deleting a budget drops the pending deletions of its entries, which the server deletes with it`() = runTest {
        val budgetId = insertBudget(serverId = 101)
        val entryId = insertEntry(budgetId, serverId = 201)
        entries.deleteRecordingPending(listOf(entryId.toLong()), deletedAt = "2026-01-02T10:00:00Z")

        budgets.deleteRecordingPending(budgetId, deletedAt = "2026-01-02T10:01:00Z")

        assertEquals(emptyList(), entries.getPendingDeletions(budgetServerId = 101))
        assertEquals(1L, pendingCount())
    }

    @Test
    fun `a pushed budget deletion is forgotten`() = runTest {
        budgets.deleteRecordingPending(insertBudget(serverId = 101), deletedAt = "2026-01-02T10:00:00Z")

        budgets.removePendingDeletion(101)

        assertEquals(emptyList(), budgets.getPendingDeletions())
    }

    @Test
    fun `signing out forgets the pending deletions`() = runTest {
        budgets.deleteRecordingPending(insertBudget(serverId = 101), deletedAt = "2026-01-02T10:00:00Z")

        budgets.clearAllData()

        assertEquals(0L, pendingCount())
    }

    @Test
    fun `switching account forgets the pending deletions`() = runTest {
        budgets.deleteRecordingPending(insertBudget(serverId = 101), deletedAt = "2026-01-02T10:00:00Z")
        val otherBudget = insertBudget(serverId = 102)
        entries.deleteRecordingPending(listOf(insertEntry(otherBudget, serverId = 201).toLong()), "2026-01-02T10:00:00Z")

        budgets.deleteSynced()

        assertEquals(0L, pendingCount())
    }

    @Test
    fun `pending deletions count as changes not pushed yet`() = runTest {
        budgets.deleteRecordingPending(insertBudget(serverId = 101), deletedAt = "2026-01-02T10:00:00Z")

        assertEquals(1L, budgets.countPendingDeletions())
    }

    @Test
    fun `the server's values overwrite a synced budget`() = runTest {
        val budget = budgets.getById(insertBudget(serverId = 101))!!

        budgets.updateFromServer(budget.copy(name = "From server", amount = 500.0, collaboratorCount = 3))

        with(budgets.getById(budget.id)!!) {
            assertEquals("From server", name)
            assertEquals(500.0, amount)
            assertEquals(3, collaboratorCount)
            assertTrue(isSynced)
        }
    }

    @Test
    fun `the server's values do not overwrite a budget edited after the pull read it`() = runTest {
        // The pull read it synced; the user saved an edit before the pull wrote it back
        val readByPull = budgets.getById(insertBudget(serverId = 101))!!
        budgets.update(readByPull.copy(name = "Edited here", isSynced = false))

        budgets.updateFromServer(readByPull.copy(name = "From server"))

        with(budgets.getById(readByPull.id)!!) {
            assertEquals("Edited here", name)
            assertFalse(isSynced)
        }
    }

    // ── Entries ───────────────────────────────────────────────────────────────

    @Test
    fun `deleting entries records only those on the server`() = runTest {
        val budgetId = insertBudget(serverId = 101)
        val onServer = insertEntry(budgetId, serverId = 201)
        val neverPushed = insertEntry(budgetId, serverId = null)

        entries.deleteRecordingPending(listOf(onServer.toLong(), neverPushed.toLong()), "2026-01-02T10:00:00Z")

        assertEquals(emptyList(), entriesOf(budgetId))
        assertEquals(listOf(201L), entries.getPendingDeletions(budgetServerId = 101))
    }

    @Test
    fun `deleting entries of a budget never pushed records nothing`() = runTest {
        val budgetId = insertBudget(serverId = null)
        val entryId = insertEntry(budgetId, serverId = null)

        entries.deleteRecordingPending(listOf(entryId.toLong()), "2026-01-02T10:00:00Z")

        assertEquals(0L, pendingCount())
    }

    @Test
    fun `a pushed entry deletion is forgotten`() = runTest {
        val budgetId = insertBudget(serverId = 101)
        entries.deleteRecordingPending(listOf(insertEntry(budgetId, serverId = 201).toLong()), "2026-01-02T10:00:00Z")

        entries.removePendingDeletion(201)

        assertEquals(emptyList(), entries.getPendingDeletions(budgetServerId = 101))
    }

    @Test
    fun `entries the server dropped are removed unless they hold a change still to be pushed`() = runTest {
        val budgetId = insertBudget(serverId = 101)
        insertEntry(budgetId, serverId = 201)
        insertEntry(budgetId, serverId = 202, synced = false, description = "Edited here")
        insertEntry(budgetId, serverId = 203)

        assertEquals(setOf(201L, 203L), entries.getSyncedServerIds(budgetId))

        entries.deleteSyncedByServerIds(budgetId, listOf(201L, 202L))

        assertEquals(setOf(202L, 203L), entriesOf(budgetId).map { it.serverId }.toSet())
    }

    @Test
    fun `an entry deleted by a collaborator is removed unless it holds a change still to be pushed`() = runTest {
        val budgetId = insertBudget(serverId = 101)
        insertEntry(budgetId, serverId = 201)
        insertEntry(budgetId, serverId = 202, synced = false)

        entries.deleteSyncedByServerId(201)
        entries.deleteSyncedByServerId(202)

        assertEquals(listOf(202L), entriesOf(budgetId).map { it.serverId })
    }

    @Test
    fun `the server's values do not overwrite an entry edited after the pull read it`() = runTest {
        val budgetId = insertBudget(serverId = 101)
        insertEntry(budgetId, serverId = 201)
        val readByPull = entriesOf(budgetId).single()
        entries.update(readByPull.copy(description = "Edited here", isSynced = false))

        entries.updateFromServer(readByPull.copy(description = "From server", isSynced = true))

        with(entriesOf(budgetId).single()) {
            assertEquals("Edited here", description)
            assertFalse(isSynced)
        }
    }

    @Test
    fun `the server's values overwrite a synced entry`() = runTest {
        val budgetId = insertBudget(serverId = 101)
        insertEntry(budgetId, serverId = 201)
        val synced = entriesOf(budgetId).single()

        entries.updateFromServer(synced.copy(description = "From server", category = BudgetEntry.Category.OTHER))

        with(entriesOf(budgetId).single()) {
            assertEquals("From server", description)
            assertEquals(BudgetEntry.Category.OTHER, category)
            assertTrue(isSynced)
        }
    }

    @Test
    fun `a pushed amount read back from the database still matches what was pushed`() = runTest {
        // The push compares the amount it sent with the stored one (markPushedIfUnchanged)
        val budgetId = insertBudget(serverId = 101)
        listOf(0.1, 1234.56, 99999.99, 0.3).forEach { amount ->
            insertEntry(budgetId, serverId = null, amount = amount, description = "Amount $amount")
        }
        val pushed = entriesOf(budgetId)

        pushed.forEachIndexed { index, entry ->
            entries.savePushResponse(pushed = entry, synced = entry.copy(serverId = 300L + index, isSynced = true))
        }

        assertTrue(entriesOf(budgetId).all { it.isSynced })
    }
}
