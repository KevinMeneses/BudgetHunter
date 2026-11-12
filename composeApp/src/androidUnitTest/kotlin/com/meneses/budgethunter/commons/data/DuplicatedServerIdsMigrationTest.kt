package com.meneses.budgethunter.commons.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.adapter.categoryAdapter
import com.meneses.budgethunter.budgetList.data.adapter.typeAdapter
import com.meneses.budgethunter.db.Budget_entry
import com.meneses.budgethunter.db.Database
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Migration 4 repairs the rows that duplicating a synced budget left with its original's server
 * ids. It only changes data, so it is run on a database created with the current schema.
 */
class DuplicatedServerIdsMigrationTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: Database

    @BeforeTest
    fun setup() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        database = Database(driver, Budget_entry.Adapter(typeAdapter, categoryAdapter))
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    private fun insertBudget(name: String, serverId: Long?, synced: Boolean, collaborators: Long = 1): Long {
        database.budgetQueries.insert(
            amount = 100.0,
            name = name,
            date = "2026-01-01",
            server_id = serverId,
            is_synced = if (synced) 1L else 0L,
            last_synced_at = if (synced) "2026-01-02" else null,
            collaborator_count = collaborators
        )
        return database.budgetQueries.selectLastId().executeAsOne()
    }

    private fun insertEntry(
        budgetId: Long,
        description: String,
        serverId: Long?,
        synced: Boolean,
        invoice: String? = null
    ): Long {
        database.budgetEntryQueries.insert(
            id = null, budgetId = budgetId, amount = 10.0, description = description,
            type = BudgetEntry.Type.OUTCOME, date = "2026-01-01", invoice = invoice,
            category = BudgetEntry.Category.FOOD, server_id = serverId, is_synced = if (synced) 1L else 0L,
            created_by_email = if (synced) "a@test.com" else null, updated_by_email = null,
            creation_date = if (synced) "2026-01-01T10:00:00" else null, modification_date = null
        )
        return database.budgetQueries.selectLastId().executeAsOne()
    }

    private fun migrate() {
        Database.Schema.migrate(driver, 4, 5)
    }

    private fun budget(id: Long) = database.budgetQueries.selectById(id).executeAsOne()
    private fun entries(budgetId: Long) = database.budgetEntryQueries.selectAllByBudgetId(budgetId).executeAsList()

    @Test
    fun `a copy of a synced budget becomes a local budget pending to be pushed`() {
        val original = insertBudget("Shared", serverId = 101, synced = true, collaborators = 3)
        val copy = insertBudget("Shared (copy)", serverId = 101, synced = true, collaborators = 3)

        migrate()

        with(budget(original)) {
            assertEquals(101L, server_id)
            assertEquals(1L, is_synced)
            assertEquals(3L, collaborator_count)
        }
        with(budget(copy)) {
            assertNull(server_id)
            assertEquals(0L, is_synced)
            assertNull(last_synced_at)
            assertEquals(1L, collaborator_count)
        }
        // Lookups by server id work again
        assertEquals(original, database.budgetQueries.selectByServerId(101).executeAsOne().id)
    }

    @Test
    fun `the entries of a copy become local entries pending to be pushed`() {
        val original = insertBudget("Shared", serverId = 101, synced = true)
        val copy = insertBudget("Shared (copy)", serverId = 101, synced = true)
        insertEntry(original, "Lunch", serverId = 201, synced = true)
        insertEntry(copy, "Lunch", serverId = 201, synced = true)

        migrate()

        with(entries(original).single()) {
            assertEquals(201L, server_id)
            assertEquals(1L, is_synced)
        }
        with(entries(copy).single()) {
            assertEquals("Lunch", description)
            assertNull(server_id)
            assertEquals(0L, is_synced)
            assertNull(created_by_email)
            assertNull(creation_date)
        }
        assertEquals(original, database.budgetEntryQueries.selectByServerId(201).executeAsOne().budget_id)
    }

    @Test
    fun `an entry copied from one that was deleted since is detached with its budget`() {
        val original = insertBudget("Shared", serverId = 101, synced = true)
        val copy = insertBudget("Shared (copy)", serverId = 101, synced = true)
        insertEntry(copy, "Lunch", serverId = 201, synced = true)

        migrate()

        with(entries(copy).single()) {
            assertNull(server_id)
            assertEquals(0L, is_synced)
        }
        assertEquals(emptyList(), entries(original))
    }

    @Test
    fun `the same server entry twice in one budget is kept once`() {
        val budget = insertBudget("Mine", serverId = 101, synced = true)
        insertEntry(budget, "Lunch", serverId = 201, synced = true)
        insertEntry(budget, "Lunch", serverId = 201, synced = true)

        migrate()

        assertEquals(1, entries(budget).size)
    }

    @Test
    fun `consistent data is left untouched`() {
        val synced = insertBudget("Synced", serverId = 101, synced = true, collaborators = 2)
        val local = insertBudget("Local", serverId = null, synced = false)
        insertEntry(synced, "Synced entry", serverId = 201, synced = true)
        insertEntry(synced, "Pending entry", serverId = null, synced = false)
        insertEntry(local, "Local entry", serverId = null, synced = false)
        val budgetsBefore = database.budgetQueries.selectAll().executeAsList()
        val entriesBefore = entries(synced) + entries(local)

        migrate()

        assertEquals(budgetsBefore, database.budgetQueries.selectAll().executeAsList())
        assertEquals(entriesBefore, entries(synced) + entries(local))
    }

    @Test
    fun `a duplicated entry keeps the row with a change not pushed yet`() {
        val budget = insertBudget("Mine", serverId = 101, synced = true)
        insertEntry(budget, "Lunch", serverId = 201, synced = true)
        val pending = insertEntry(budget, "Lunch edited", serverId = 201, synced = false)

        migrate()

        assertEquals(listOf(pending), entries(budget).map { it.id })
    }

    @Test
    fun `a duplicated entry keeps the row with an invoice`() {
        val budget = insertBudget("Mine", serverId = 101, synced = true)
        insertEntry(budget, "Lunch", serverId = 201, synced = true)
        val withInvoice = insertEntry(budget, "Lunch", serverId = 201, synced = true, invoice = "receipt.jpg")

        migrate()

        with(entries(budget).single()) {
            assertEquals(withInvoice, id)
            assertEquals("receipt.jpg", invoice)
        }
    }

    @Test
    fun `three budgets sharing one server id leave only the original linked`() {
        val original = insertBudget("Shared", serverId = 101, synced = true)
        val copy = insertBudget("Shared (copy)", serverId = 101, synced = true)
        val copyOfCopy = insertBudget("Shared (copy) (copy)", serverId = 101, synced = true)
        listOf(original, copy, copyOfCopy).forEach { insertEntry(it, "Lunch", serverId = 201, synced = true) }

        migrate()

        assertEquals(101L, budget(original).server_id)
        assertNull(budget(copy).server_id)
        assertNull(budget(copyOfCopy).server_id)
        assertEquals(201L, entries(original).single().server_id)
        assertNull(entries(copy).single().server_id)
        assertNull(entries(copyOfCopy).single().server_id)
    }

    @Test
    fun `an original entry re-inserted after its copy keeps its server id`() {
        // The original entry was deleted here but not on the server, and a pull brought it back
        // with an id newer than the copy's
        val original = insertBudget("Shared", serverId = 101, synced = true)
        val copy = insertBudget("Shared (copy)", serverId = 101, synced = true)
        insertEntry(copy, "Lunch", serverId = 201, synced = true)
        insertEntry(original, "Lunch", serverId = 201, synced = true)

        migrate()

        assertEquals(201L, entries(original).single().server_id)
        assertNull(entries(copy).single().server_id)
    }
}
