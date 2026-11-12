package com.meneses.budgethunter.budgetEntry.data.datasource

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.adapter.categoryAdapter
import com.meneses.budgethunter.budgetList.data.adapter.typeAdapter
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for BudgetEntryLocalDataSource using SQLDelight's in-memory driver.
 * Tests caching, CRUD operations, and sync-related functionality with real database queries.
 */
class BudgetEntryLocalDataSourceTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: Database
    private lateinit var dataSource: BudgetEntryLocalDataSource

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        val budgetEntryAdapter = Budget_entry.Adapter(typeAdapter, categoryAdapter)
        database = Database(driver, budgetEntryAdapter)
        dataSource = BudgetEntryLocalDataSource(database.budgetEntryQueries, Dispatchers.Unconfined)

        // Create a test budget for foreign key constraint
        database.budgetQueries.insert(
            amount = 1000.0,
            name = "Test Budget",
            date = "2025-01-01",
            server_id = null,
            is_synced = 0L,
            last_synced_at = null,
            collaborator_count = 1L
        )
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    // ── Cache tests ───────────────────────────────────────────────────────────

    @Test
    fun `getAllCached returns empty list initially`() = runTest {
        assertEquals(emptyList(), dataSource.getAllCached())
    }

    @Test
    fun `getAllCached returns cached entries after flow emission`() = runTest {
        insertEntry(budgetId = 1, description = "Test Entry")

        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllCached()

        assertEquals(1, result.size)
        assertEquals("Test Entry", result[0].description)
    }

    @Test
    fun `cache is populated when flow is collected`() = runTest {
        insertEntry(budgetId = 1, description = "Test")

        assertEquals(emptyList(), dataSource.getAllCached())

        dataSource.selectAllByBudgetId(1L).first()

        assertEquals(1, dataSource.getAllCached().size)
    }

    // ── selectAllByBudgetId tests ─────────────────────────────────────────────

    @Test
    fun `selectAllByBudgetId filters entries by budget id`() = runTest {
        insertEntry(budgetId = 1, description = "Entry 1")
        insertEntry(budgetId = 1, description = "Entry 2")

        val result = dataSource.selectAllByBudgetId(1L).first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.budgetId == 1 })
    }

    @Test
    fun `selectAllByBudgetId returns empty list when no entries for budget`() = runTest {
        val result = dataSource.selectAllByBudgetId(999L).first()

        assertEquals(emptyList(), result)
    }

    @Test
    fun `entries are ordered by id descending`() = runTest {
        insertEntry(budgetId = 1, description = "First")
        insertEntry(budgetId = 1, description = "Second")
        insertEntry(budgetId = 1, description = "Third")

        val result = dataSource.selectAllByBudgetId(1L).first()

        assertEquals(3, result.size)
        assertEquals("Third", result[0].description)
        assertEquals("Second", result[1].description)
        assertEquals("First", result[2].description)
    }

    // ── create tests ──────────────────────────────────────────────────────────

    @Test
    fun `create inserts entry with correct parameters`() = runTest {
        val entry = BudgetEntry(
            budgetId = 1,
            amount = "150.50",
            description = "New Entry",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.FOOD,
            date = "2025-01-20",
            invoice = "INV-001"
        )

        dataSource.create(entry)

        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, result.size)
        assertEquals("New Entry", result[0].description)
        assertEquals("150.50", result[0].amount)
        assertEquals(BudgetEntry.Type.OUTCOME, result[0].type)
        assertEquals(BudgetEntry.Category.FOOD, result[0].category)
        assertEquals("INV-001", result[0].invoice)
    }

    @Test
    fun `create handles null invoice`() = runTest {
        val entry = BudgetEntry(budgetId = 1, amount = "100.0", description = "No Invoice", invoice = null)

        dataSource.create(entry)

        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, result.size)
        assertNull(result[0].invoice)
    }

    @Test
    fun `create handles invalid amount string`() = runTest {
        val entry = BudgetEntry(budgetId = 1, amount = "invalid", description = "Test")

        dataSource.create(entry)

        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, result.size)
        assertEquals("0", result[0].amount)
    }

    @Test
    fun `create with zero amount`() = runTest {
        val entry = BudgetEntry(budgetId = 1, amount = "0.0", description = "Zero Amount")

        dataSource.create(entry)

        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals("0", result[0].amount)
    }

    @Test
    fun `create stores sync fields correctly`() = runTest {
        val entry = BudgetEntry(
            budgetId = 1,
            amount = "50.0",
            description = "Synced Entry",
            serverId = 99L,
            isSynced = true,
            createdByEmail = "user@example.com",
            creationDate = "2025-01-01"
        )

        dataSource.create(entry)

        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, result.size)
        assertEquals(99L, result[0].serverId)
        assertTrue(result[0].isSynced)
        assertEquals("user@example.com", result[0].createdByEmail)
        assertEquals("2025-01-01", result[0].creationDate)
    }

    // ── update tests ──────────────────────────────────────────────────────────

    @Test
    fun `update modifies existing entry`() = runTest {
        val insertedId = insertEntry(budgetId = 1, description = "Original", amount = 100.0)
        val entry = BudgetEntry(
            id = insertedId,
            budgetId = 1,
            amount = "200.0",
            description = "Updated Entry",
            type = BudgetEntry.Type.INCOME,
            category = BudgetEntry.Category.OTHER,
            date = "2025-02-01"
        )

        dataSource.update(entry)

        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, result.size)
        assertEquals("Updated Entry", result[0].description)
        assertEquals("200", result[0].amount)
        assertEquals(BudgetEntry.Type.INCOME, result[0].type)
    }

    @Test
    fun `update handles amount conversion`() = runTest {
        val insertedId = insertEntry(budgetId = 1, description = "Test", amount = 100.0)
        val entry = BudgetEntry(id = insertedId, budgetId = 1, amount = "999.99", description = "Test")

        dataSource.update(entry)

        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals("999.99", result[0].amount)
    }

    @Test
    fun `update with invalid amount defaults to zero`() = runTest {
        val insertedId = insertEntry(budgetId = 1, description = "Test", amount = 100.0)
        val entry = BudgetEntry(id = insertedId, budgetId = 1, amount = "not-a-number", description = "Test")

        dataSource.update(entry)

        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals("0", result[0].amount)
    }

    @Test
    fun `update stores sync fields correctly`() = runTest {
        val insertedId = insertEntry(budgetId = 1, description = "Test")
        val entry = BudgetEntry(
            id = insertedId,
            budgetId = 1,
            amount = "100.0",
            description = "Test",
            serverId = 42L,
            isSynced = true,
            updatedByEmail = "editor@example.com",
            modificationDate = "2025-03-01"
        )

        dataSource.update(entry)

        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(42L, result[0].serverId)
        assertTrue(result[0].isSynced)
        assertEquals("editor@example.com", result[0].updatedByEmail)
        assertEquals("2025-03-01", result[0].modificationDate)
    }

    @Test
    fun `update with a copy read before the push keeps the server id the push stored`() = runTest {
        // Given - The entry screen holds a copy read before the entry reached the server
        insertEntry(budgetId = 1, description = "Lunch", amount = 20.0)
        val staleCopy = dataSource.selectAllByBudgetId(1L).first().single()
        dataSource.savePushResponse(pushed = staleCopy, synced = staleCopy.copy(serverId = 42L, isSynced = true))

        // When - The user saves an edit made on that copy
        dataSource.update(staleCopy.copy(amount = "25", isSynced = false))

        // Then - Still the server's entry 42, so the next push updates it instead of creating another
        val entry = dataSource.selectAllByBudgetId(1L).first().single()
        assertEquals("25", entry.amount)
        assertEquals(42L, entry.serverId)
        assertFalse(entry.isSynced)
    }

    // ── savePushResponse tests ────────────────────────────────────────────────

    @Test
    fun `savePushResponse stores the server values of an entry unchanged since the push`() = runTest {
        insertEntry(budgetId = 1, description = "Lunch", amount = 20.0, category = BudgetEntry.Category.FOOD)
        val pushed = dataSource.selectAllByBudgetId(1L).first().single()

        dataSource.savePushResponse(
            pushed = pushed,
            synced = pushed.copy(
                serverId = 42L,
                isSynced = true,
                createdByEmail = "me@example.com",
                creationDate = "2025-01-01T10:00:00"
            )
        )

        val entry = dataSource.selectAllByBudgetId(1L).first().single()
        assertEquals(42L, entry.serverId)
        assertTrue(entry.isSynced)
        assertEquals("me@example.com", entry.createdByEmail)
        assertEquals("2025-01-01T10:00:00", entry.creationDate)
    }

    @Test
    fun `savePushResponse keeps pending an entry edited while its push was in flight`() = runTest {
        // Given - The push read the entry, then the user changed the amount before the answer came back
        insertEntry(budgetId = 1, description = "Lunch", amount = 20.0)
        val pushed = dataSource.selectAllByBudgetId(1L).first().single()
        dataSource.update(pushed.copy(amount = "25", isSynced = false))

        // When
        dataSource.savePushResponse(pushed = pushed, synced = pushed.copy(serverId = 42L, isSynced = true))

        // Then - It has its server id (the next push updates it) but the edit is kept and still pending
        val entry = dataSource.selectAllByBudgetId(1L).first().single()
        assertEquals("25", entry.amount)
        assertEquals(42L, entry.serverId)
        assertFalse(entry.isSynced)
        assertEquals(1L, dataSource.countUnsynced())
    }

    @Test
    fun `savePushResponse marks synced an entry whose only change meanwhile was its invoice`() = runTest {
        // The invoice is not sent to the server: changing it does not need another push
        insertEntry(budgetId = 1, description = "Lunch", amount = 20.0)
        val pushed = dataSource.selectAllByBudgetId(1L).first().single()
        dataSource.update(pushed.copy(invoice = "receipt.jpg", isSynced = false))

        dataSource.savePushResponse(pushed = pushed, synced = pushed.copy(serverId = 42L, isSynced = true))

        val entry = dataSource.selectAllByBudgetId(1L).first().single()
        assertEquals("receipt.jpg", entry.invoice)
        assertTrue(entry.isSynced)
    }

    // ── deleteByIds tests ─────────────────────────────────────────────────────

    @Test
    fun `deleteByIds removes entries with specified ids`() = runTest {
        val id1 = insertEntry(budgetId = 1, description = "Entry 1")
        val id2 = insertEntry(budgetId = 1, description = "Entry 2")
        insertEntry(budgetId = 1, description = "Entry 3")

        dataSource.deleteByIds(listOf(id1.toLong(), id2.toLong()))

        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, result.size)
        assertEquals("Entry 3", result[0].description)
    }

    @Test
    fun `deleteByIds handles empty list`() = runTest {
        insertEntry(budgetId = 1, description = "Entry 1")

        dataSource.deleteByIds(emptyList())

        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, result.size)
    }

    // ── delete tests ──────────────────────────────────────────────────────────

    @Test
    fun `delete removes entry by id`() = runTest {
        val id1 = insertEntry(budgetId = 1, description = "Entry 1")
        insertEntry(budgetId = 1, description = "Entry 2")

        dataSource.delete(id1.toLong())

        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, result.size)
        assertEquals("Entry 2", result[0].description)
    }

    @Test
    fun `delete with non-existent id does not affect other entries`() = runTest {
        insertEntry(budgetId = 1, description = "Entry 1")

        dataSource.delete(9999L)

        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, result.size)
    }

    // ── deleteAllByBudgetId tests ─────────────────────────────────────────────

    @Test
    fun `deleteAllByBudgetId removes all entries for budget`() = runTest {
        insertEntry(budgetId = 1, description = "Entry 1")
        insertEntry(budgetId = 1, description = "Entry 2")

        dataSource.deleteAllByBudgetId(1L)

        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(emptyList(), result)
    }

    // ── clearAllData tests ────────────────────────────────────────────────────

    @Test
    fun `clearAllData removes all entries from database`() = runTest {
        insertEntry(budgetId = 1, description = "Entry 1")
        insertEntry(budgetId = 1, description = "Entry 2")

        dataSource.clearAllData()

        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(emptyList(), result)
    }

    // ── getUnsynced tests ─────────────────────────────────────────────────────

    @Test
    fun `getUnsynced returns only unsynced entries for budget`() = runTest {
        insertEntry(budgetId = 1, description = "Unsynced Entry", isSynced = false)
        insertEntry(budgetId = 1, description = "Synced Entry", isSynced = true)

        val result = dataSource.getUnsynced(localBudgetId = 1)

        assertEquals(1, result.size)
        assertEquals("Unsynced Entry", result[0].description)
    }

    @Test
    fun `getUnsynced returns empty list when all entries are synced`() = runTest {
        insertEntry(budgetId = 1, description = "Synced 1", isSynced = true)
        insertEntry(budgetId = 1, description = "Synced 2", isSynced = true)

        val result = dataSource.getUnsynced(localBudgetId = 1)

        assertEquals(emptyList(), result)
    }

    @Test
    fun `countUnsynced counts only entries with is_synced 0`() = runTest {
        insertEntry(budgetId = 1, description = "Unsynced 1", isSynced = false)
        insertEntry(budgetId = 1, description = "Unsynced 2", isSynced = false)
        insertEntry(budgetId = 1, description = "Synced", isSynced = true)

        assertEquals(2L, dataSource.countUnsynced())
    }

    @Test
    fun `countUnsynced is zero when every entry is synced`() = runTest {
        insertEntry(budgetId = 1, description = "Synced", isSynced = true)

        assertEquals(0L, dataSource.countUnsynced())
    }

    // ── selectByServerId tests ────────────────────────────────────────────────

    @Test
    fun `selectByServerId returns entry matching server id`() = runTest {
        insertEntry(budgetId = 1, description = "Server Entry", serverId = 77L)
        insertEntry(budgetId = 1, description = "Other Entry", serverId = null)

        val result = dataSource.selectByServerId(77L)

        assertNotNull(result)
        assertEquals("Server Entry", result.description)
    }

    @Test
    fun `selectByServerId returns null when server id not found`() = runTest {
        insertEntry(budgetId = 1, description = "Entry", serverId = null)

        val result = dataSource.selectByServerId(9999L)

        assertNull(result)
    }

    // ── selectByUniqueFields tests ────────────────────────────────────────────

    @Test
    fun `selectByUniqueFields returns matching entry`() = runTest {
        insertEntry(
            budgetId = 1,
            description = "Coffee",
            amount = 5.0,
            creationDate = "2025-01-15"
        )

        val result = dataSource.selectByUniqueFields(
            budgetId = 1L,
            amount = 5.0,
            description = "Coffee",
            creationDate = "2025-01-15"
        )

        assertNotNull(result)
        assertEquals("Coffee", result.description)
    }

    @Test
    fun `selectByUniqueFields returns null when no match`() = runTest {
        insertEntry(budgetId = 1, description = "Coffee", amount = 5.0, creationDate = "2025-01-15")

        val result = dataSource.selectByUniqueFields(
            budgetId = 1L,
            amount = 5.0,
            description = "Coffee",
            creationDate = "2025-01-16" // different date
        )

        assertNull(result)
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun insertEntry(
        budgetId: Long,
        description: String,
        amount: Double = 100.0,
        type: BudgetEntry.Type = BudgetEntry.Type.OUTCOME,
        category: BudgetEntry.Category = BudgetEntry.Category.OTHER,
        date: String = "2025-01-01",
        invoice: String? = null,
        serverId: Long? = null,
        isSynced: Boolean = false,
        creationDate: String? = null
    ): Int {
        database.budgetEntryQueries.insert(
            id = null,
            budgetId = budgetId,
            amount = amount,
            description = description,
            type = type,
            date = date,
            invoice = invoice,
            category = category,
            server_id = serverId,
            is_synced = if (isSynced) 1L else 0L,
            created_by_email = null,
            updated_by_email = null,
            creation_date = creationDate,
            modification_date = null
        )
        return database.budgetQueries.selectLastId().executeAsOne().toInt()
    }
}
