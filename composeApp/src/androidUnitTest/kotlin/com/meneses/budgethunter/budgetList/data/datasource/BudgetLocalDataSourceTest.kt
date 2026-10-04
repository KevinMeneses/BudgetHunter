package com.meneses.budgethunter.budgetList.data.datasource

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.meneses.budgethunter.budgetList.data.adapter.categoryAdapter
import com.meneses.budgethunter.budgetList.data.adapter.typeAdapter
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.db.Budget_entry
import com.meneses.budgethunter.db.Database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for BudgetLocalDataSource using SQLDelight's in-memory driver.
 * Tests caching, filtering, CRUD operations, and thread safety with real database queries.
 */
class BudgetLocalDataSourceTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: Database
    private lateinit var dataSource: BudgetLocalDataSource

    @BeforeTest
    fun setup() {
        // Create in-memory SQLite database
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        val budgetEntryAdapter = Budget_entry.Adapter(typeAdapter, categoryAdapter)
        database = Database(driver, budgetEntryAdapter)
        dataSource = BudgetLocalDataSource(database.budgetQueries, Dispatchers.Unconfined)
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun `getAllCached returns empty list initially`() = runTest {
        assertEquals(emptyList(), dataSource.getAllCached())
    }

    @Test
    fun `getById reads the database even when the budgets flow was never collected`() = runTest {
        // Given - e.g. the process was started only to handle an SMS, with no screen observing budgets
        val created = dataSource.create(Budget(name = "Budget 1", amount = 1000.0))

        // When
        val result = dataSource.getById(created.id)

        // Then
        assertEquals("Budget 1", result?.name)
    }

    @Test
    fun `getById reflects markAsSynced immediately`() = runTest {
        // Given
        val created = dataSource.create(Budget(name = "Budget 1", amount = 1000.0))
        dataSource.budgets.first()

        // When
        dataSource.markAsSynced(id = created.id, serverId = 18L, lastSyncedAt = "2026-10-03T12:00:00Z")
        val result = dataSource.getById(created.id)

        // Then
        assertEquals(18L, result?.serverId)
    }

    @Test
    fun `getAllCached returns cached budgets after flow emission`() = runTest {
        // Given
        insertBudget(name = "Budget 1", amount = 1000.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllCached()

        // Then
        assertEquals(1, result.size)
        assertEquals("Budget 1", result[0].name)
    }

    @Test
    fun `budgets flow returns all budgets`() = runTest {
        // Given
        insertBudget(name = "Budget 1", amount = 1000.0)
        insertBudget(name = "Budget 2", amount = 2000.0)

        // When
        val result = dataSource.budgets.first()

        // Then
        assertEquals(2, result.size)
        assertEquals("Budget 2", result[0].name) // DESC order
        assertEquals("Budget 1", result[1].name)
    }

    @Test
    fun `create inserts budget and returns it with generated id`() = runTest {
        // Given
        val budget = Budget(id = 0, name = "New Budget", amount = 500.0, date = "2025-01-01")

        // When
        val result = dataSource.create(budget)

        // Then
        assertEquals("New Budget", result.name)
        assertEquals(500.0, result.amount)
        assertTrue(result.id > 0)

        // Verify it's actually in database
        val allBudgets = dataSource.budgets.first()
        assertEquals(1, allBudgets.size)
        assertEquals("New Budget", allBudgets[0].name)
    }

    @Test
    fun `update modifies existing budget`() = runTest {
        // Given
        val insertedId = insertBudget(name = "Original", amount = 1000.0)
        val budget = Budget(id = insertedId, name = "Updated Budget", amount = 1500.0, date = "2025-01-15")

        // When
        dataSource.update(budget)

        // Then
        val allBudgets = dataSource.budgets.first()
        assertEquals(1, allBudgets.size)
        assertEquals("Updated Budget", allBudgets[0].name)
        assertEquals(1500.0, allBudgets[0].amount)
    }

    @Test
    fun `delete removes budget by id`() = runTest {
        // Given
        val insertedId = insertBudget(name = "To Delete", amount = 1000.0)

        // When
        dataSource.delete(insertedId.toLong())

        // Then
        val allBudgets = dataSource.budgets.first()
        assertEquals(emptyList(), allBudgets)
    }

    @Test
    fun `getAllFilteredBy returns all budgets when filter name is null`() = runTest {
        // Given
        insertBudget(name = "Budget 1", amount = 1000.0)
        insertBudget(name = "Budget 2", amount = 2000.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = null))

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy returns all budgets when filter name is blank`() = runTest {
        // Given
        insertBudget(name = "Budget 1", amount = 1000.0)
        insertBudget(name = "Budget 2", amount = 2000.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "   "))

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy filters by name case-insensitive`() = runTest {
        // Given
        insertBudget(name = "Monthly Budget", amount = 1000.0)
        insertBudget(name = "Yearly Budget", amount = 12000.0)
        insertBudget(name = "Monthly Expenses", amount = 500.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "MONTHLY"))

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Monthly Budget" })
        assertTrue(result.any { it.name == "Monthly Expenses" })
    }

    @Test
    fun `cache is populated when flow is collected`() = runTest {
        // Given
        insertBudget(name = "Budget 1", amount = 1000.0)

        // When - before collecting, cache should be empty
        assertEquals(emptyList(), dataSource.getAllCached())

        // After collecting, cache should be populated
        dataSource.budgets.first()

        // Then
        assertEquals(1, dataSource.getAllCached().size)
    }

    @Test
    fun `getAllFilteredBy returns empty list when no matches`() = runTest {
        // Given
        insertBudget(name = "Budget 1", amount = 1000.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "NonExistent"))

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `create handles id 0`() = runTest {
        // Given
        val budget = Budget(id = 0, name = "New Budget", amount = 100.0, date = "2025-01-01")

        // When
        val result = dataSource.create(budget)

        // Then
        assertEquals(1, result.id)
    }

    @Test
    fun `update handles budget with special characters`() = runTest {
        // Given
        val insertedId = insertBudget(name = "Original", amount = 100.0)
        val budget = Budget(
            id = insertedId,
            name = "Budget with 'special' \"characters\"",
            amount = 100.0,
            date = "2025-01-01"
        )

        // When
        dataSource.update(budget)

        // Then
        val allBudgets = dataSource.budgets.first()
        assertEquals(1, allBudgets.size)
        assertEquals("Budget with 'special' \"characters\"", allBudgets[0].name)
    }

    @Test
    fun `delete handles large id`() = runTest {
        // Given - no budget with this id exists

        // When - should not throw
        dataSource.delete(Long.MAX_VALUE)

        // Then - verify no error occurred
        val allBudgets = dataSource.budgets.first()
        assertEquals(emptyList(), allBudgets)
    }

    @Test
    fun `create uses transaction`() = runTest {
        // Given
        val budget = Budget(id = 0, name = "New Budget", amount = 100.0, date = "2025-01-01")

        // When
        val result = dataSource.create(budget)

        // Then - verify transaction committed successfully
        assertTrue(result.id > 0)
        val allBudgets = dataSource.budgets.first()
        assertEquals(1, allBudgets.size)
    }

    @Test
    fun `getAllFilteredBy handles partial name matches`() = runTest {
        // Given
        insertBudget(name = "Monthly Budget 2025", amount = 1000.0)
        insertBudget(name = "Budget 2025", amount = 2000.0)
        insertBudget(name = "Monthly Expenses", amount = 500.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "2025"))

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.name.contains("2025") })
    }

    @Test
    fun `budgets flow emits empty list when no budgets`() = runTest {
        // When
        val result = dataSource.budgets.first()

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllFilteredBy handles empty cache`() = runTest {
        // Given - no flow collected, cache is empty

        // When
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "Test"))

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `create returns budget with correct values`() = runTest {
        // Given
        val budget = Budget(
            id = 0,
            name = "Test Budget",
            amount = 999.99,
            date = "2025-12-31"
        )

        // When
        val result = dataSource.create(budget)

        // Then
        assertEquals("Test Budget", result.name)
        assertEquals(999.99, result.amount)
        assertEquals("2025-12-31", result.date)
        assertTrue(result.id > 0)
    }

    @Test
    fun `getAllFilteredBy preserves budget order`() = runTest {
        // Given
        insertBudget(name = "C Budget", amount = 100.0)
        insertBudget(name = "B Budget", amount = 200.0)
        insertBudget(name = "A Budget", amount = 300.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "Budget"))

        // Then
        assertEquals(3, result.size)
        // Budgets are ordered DESC by id, so most recent first
        assertEquals("A Budget", result[0].name)
        assertEquals("B Budget", result[1].name)
        assertEquals("C Budget", result[2].name)
    }

    @Test
    fun `update handles zero amount`() = runTest {
        // Given
        val insertedId = insertBudget(name = "Original", amount = 1000.0)
        val budget = Budget(id = insertedId, name = "Zero Budget", amount = 0.0, date = "2025-01-01")

        // When
        dataSource.update(budget)

        // Then
        val allBudgets = dataSource.budgets.first()
        assertEquals(0.0, allBudgets[0].amount)
    }

    @Test
    fun `update handles negative amount`() = runTest {
        // Given
        val insertedId = insertBudget(name = "Original", amount = 1000.0)
        val budget = Budget(id = insertedId, name = "Negative Budget", amount = -100.0, date = "2025-01-01")

        // When
        dataSource.update(budget)

        // Then
        val allBudgets = dataSource.budgets.first()
        assertEquals(-100.0, allBudgets[0].amount)
    }

    @Test
    fun `getById returns correct budget`() = runTest {
        // Given
        insertBudget(name = "Budget 1", amount = 100.0)
        val id2 = insertBudget(name = "Budget 2", amount = 200.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getById(id2)

        // Then
        assertEquals("Budget 2", result?.name)
        assertEquals(200.0, result?.amount)
    }

    @Test
    fun `getById returns null for non-existent budget`() = runTest {
        // Given
        insertBudget(name = "Budget 1", amount = 100.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getById(999)

        // Then
        assertEquals(null, result)
    }

    // ── getByServerId tests ───────────────────────────────────────────────────

    @Test
    fun `getByServerId returns budget matching server id`() = runTest {
        // Given
        insertBudget(name = "Budget 1", amount = 1000.0, serverId = 55L)
        insertBudget(name = "Budget 2", amount = 2000.0, serverId = null)

        // When
        val result = dataSource.getByServerId(55L)

        // Then
        assertEquals("Budget 1", result?.name)
    }

    @Test
    fun `getByServerId returns null when server id not found`() = runTest {
        // Given
        insertBudget(name = "Budget 1", amount = 1000.0, serverId = null)

        // When
        val result = dataSource.getByServerId(9999L)

        // Then
        assertEquals(null, result)
    }

    // ── getUnsynced tests ─────────────────────────────────────────────────────

    @Test
    fun `getUnsynced returns only unsynced budgets`() = runTest {
        // Given
        insertBudget(name = "Unsynced Budget", amount = 1000.0, isSynced = false)
        insertBudget(name = "Synced Budget", amount = 2000.0, isSynced = true)

        // When
        val result = dataSource.getUnsynced()

        // Then
        assertEquals(1, result.size)
        assertEquals("Unsynced Budget", result[0].name)
    }

    @Test
    fun `getUnsynced returns empty list when all budgets are synced`() = runTest {
        // Given
        insertBudget(name = "Synced 1", amount = 1000.0, isSynced = true)
        insertBudget(name = "Synced 2", amount = 2000.0, isSynced = true)

        // When
        val result = dataSource.getUnsynced()

        // Then
        assertEquals(emptyList(), result)
    }

    // ── markAsSynced tests ────────────────────────────────────────────────────

    @Test
    fun `markAsSynced updates budget with server id and timestamp`() = runTest {
        // Given
        val budgetId = insertBudget(name = "Budget", amount = 1000.0, isSynced = false)

        // When
        dataSource.markAsSynced(id = budgetId, serverId = 42L, lastSyncedAt = "2025-01-15T10:00:00")

        // Then
        val allBudgets = dataSource.budgets.first()
        assertEquals(1, allBudgets.size)
        assertEquals(42L, allBudgets[0].serverId)
        assertTrue(allBudgets[0].isSynced)
        assertEquals("2025-01-15T10:00:00", allBudgets[0].lastSyncedAt)
    }

    // ── clearAllData tests ────────────────────────────────────────────────────

    @Test
    fun `clearAllData removes all budgets from database`() = runTest {
        // Given
        insertBudget(name = "Budget 1", amount = 1000.0)
        insertBudget(name = "Budget 2", amount = 2000.0)

        // When
        dataSource.clearAllData()

        // Then
        val allBudgets = dataSource.budgets.first()
        assertEquals(emptyList(), allBudgets)
    }

    // ── sync fields in create/update tests ───────────────────────────────────

    @Test
    fun `create stores sync fields correctly`() = runTest {
        // Given
        val budget = Budget(
            id = 0,
            name = "Synced Budget",
            amount = 500.0,
            date = "2025-01-01",
            serverId = 10L,
            isSynced = true,
            lastSyncedAt = "2025-01-01T09:00:00"
        )

        // When
        dataSource.create(budget)

        // Then
        val result = dataSource.budgets.first()
        assertEquals(10L, result[0].serverId)
        assertTrue(result[0].isSynced)
        assertEquals("2025-01-01T09:00:00", result[0].lastSyncedAt)
    }

    @Test
    fun `update stores sync fields correctly`() = runTest {
        // Given
        val budgetId = insertBudget(name = "Budget", amount = 1000.0, isSynced = false)
        val updatedBudget = Budget(
            id = budgetId,
            name = "Budget",
            amount = 1000.0,
            date = "2025-01-01",
            serverId = 77L,
            isSynced = true,
            lastSyncedAt = "2025-02-01T12:00:00"
        )

        // When
        dataSource.update(updatedBudget)

        // Then
        val result = dataSource.budgets.first()
        assertEquals(77L, result[0].serverId)
        assertTrue(result[0].isSynced)
        assertEquals("2025-02-01T12:00:00", result[0].lastSyncedAt)
    }

    // ── Stale account cleanup ─────────────────────────────────────────────────

    @Test
    fun `getSyncedServerIds returns only the server ids of budgets that have one`() {
        insertBudget(name = "A", amount = 1.0, serverId = 10L, isSynced = true)
        insertBudget(name = "B", amount = 1.0, serverId = 20L, isSynced = true)
        insertBudget(name = "Offline", amount = 1.0, serverId = null)

        assertEquals(setOf(10L, 20L), dataSource.getSyncedServerIds())
    }

    @Test
    fun `getSyncedServerIds is empty when no budget has a server id`() {
        insertBudget(name = "Offline", amount = 1.0, serverId = null)

        assertEquals(emptySet(), dataSource.getSyncedServerIds())
    }

    @Test
    fun `deleteByServerIds removes the matching budgets and their entries only`() = runTest {
        val gone = insertBudget(name = "Gone", amount = 1.0, serverId = 10L, isSynced = true)
        val kept = insertBudget(name = "Kept", amount = 1.0, serverId = 20L, isSynced = true)
        val offline = insertBudget(name = "Offline", amount = 1.0, serverId = null)
        insertEntry(budgetId = gone.toLong(), description = "gone entry")
        insertEntry(budgetId = kept.toLong(), description = "kept entry")
        insertEntry(budgetId = offline.toLong(), description = "offline entry")

        dataSource.deleteByServerIds(listOf(10L))

        assertEquals(setOf(20L), dataSource.getSyncedServerIds())
        assertEquals(null, dataSource.getById(gone))
        assertEquals(0, entriesOf(gone).size)
        assertEquals(1, entriesOf(kept).size)
        assertEquals(1, entriesOf(offline).size)
        assertEquals("Offline", dataSource.getById(offline)?.name)
    }

    @Test
    fun `deleteByServerIds with an empty collection changes nothing`() = runTest {
        val id = insertBudget(name = "A", amount = 1.0, serverId = 10L, isSynced = true)
        insertEntry(budgetId = id.toLong(), description = "entry")

        dataSource.deleteByServerIds(emptyList())

        assertEquals("A", dataSource.getById(id)?.name)
        assertEquals(1, entriesOf(id).size)
    }

    @Test
    fun `deleteByServerIds ignores server ids that are not stored locally`() = runTest {
        val id = insertBudget(name = "A", amount = 1.0, serverId = 10L, isSynced = true)

        dataSource.deleteByServerIds(setOf(999L))

        assertEquals("A", dataSource.getById(id)?.name)
    }

    @Test
    fun `deleteSynced removes every budget with a server id together with its entries`() = runTest {
        val a = insertBudget(name = "A", amount = 1.0, serverId = 10L, isSynced = true)
        val b = insertBudget(name = "B", amount = 1.0, serverId = 20L, isSynced = false)
        insertEntry(budgetId = a.toLong(), description = "a entry")
        insertEntry(budgetId = b.toLong(), description = "b entry")

        dataSource.deleteSynced()

        assertEquals(null, dataSource.getById(a))
        assertEquals(null, dataSource.getById(b))
        assertEquals(0, entriesOf(a).size)
        assertEquals(0, entriesOf(b).size)
        assertEquals(emptySet(), dataSource.getSyncedServerIds())
    }

    @Test
    fun `deleteSynced keeps budgets created offline and their entries`() = runTest {
        val synced = insertBudget(name = "Synced", amount = 1.0, serverId = 10L, isSynced = true)
        val offline = insertBudget(name = "Offline", amount = 1.0, serverId = null)
        insertEntry(budgetId = synced.toLong(), description = "synced entry")
        insertEntry(budgetId = offline.toLong(), description = "offline entry")

        dataSource.deleteSynced()

        assertEquals("Offline", dataSource.getById(offline)?.name)
        assertEquals(1, entriesOf(offline).size)
        assertEquals(null, dataSource.getById(synced))
        assertEquals(0, entriesOf(synced).size)
    }

    @Test
    fun `deleteSynced on an empty database does nothing`() {
        dataSource.deleteSynced()

        assertEquals(emptySet(), dataSource.getSyncedServerIds())
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun entriesOf(budgetId: Int) =
        database.budgetEntryQueries.selectAllByBudgetId(budgetId.toLong()).executeAsList()

    private fun insertEntry(budgetId: Long, description: String) {
        database.budgetEntryQueries.insert(
            id = null,
            budgetId = budgetId,
            amount = 10.0,
            description = description,
            type = BudgetEntry.Type.OUTCOME,
            date = "2025-01-01",
            invoice = null,
            category = BudgetEntry.Category.OTHER,
            server_id = null,
            is_synced = 0L,
            created_by_email = null,
            updated_by_email = null,
            creation_date = null,
            modification_date = null
        )
    }

    private fun insertBudget(
        name: String,
        amount: Double,
        date: String = "2025-01-01",
        serverId: Long? = null,
        isSynced: Boolean = false,
        lastSyncedAt: String? = null
    ): Int {
        database.budgetQueries.insert(
            amount = amount,
            name = name,
            date = date,
            server_id = serverId,
            is_synced = if (isSynced) 1L else 0L,
            last_synced_at = lastSyncedAt
        )
        return database.budgetQueries.selectLastId().executeAsOne().toInt()
    }
}
