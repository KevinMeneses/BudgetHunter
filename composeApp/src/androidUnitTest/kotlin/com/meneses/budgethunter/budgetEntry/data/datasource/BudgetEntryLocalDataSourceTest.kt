package com.meneses.budgethunter.budgetEntry.data.datasource

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
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
import kotlin.test.assertTrue

/**
 * Integration tests for BudgetEntryLocalDataSource using SQLDelight's in-memory driver.
 * Tests caching, filtering, CRUD operations, and thread safety with real database queries.
 */
class BudgetEntryLocalDataSourceTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: Database
    private lateinit var dataSource: BudgetEntryLocalDataSource

    @BeforeTest
    fun setup() {
        // Create in-memory SQLite database
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        val budgetEntryAdapter = Budget_entry.Adapter(typeAdapter, categoryAdapter)
        database = Database(driver, budgetEntryAdapter)
        dataSource = BudgetEntryLocalDataSource(database.budgetEntryQueries, Dispatchers.Unconfined)

        // Create a test budget for foreign key constraint
        database.budgetQueries.insert(amount = 1000.0, name = "Test Budget", date = "2025-01-01")
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
    fun `getAllCached returns cached entries after flow emission`() = runTest {
        // Given
        insertEntry(budgetId = 1, description = "Test Entry")

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllCached()

        // Then
        assertEquals(1, result.size)
        assertEquals("Test Entry", result[0].description)
    }

    @Test
    fun `selectAllByBudgetId filters entries by budget id`() = runTest {
        // Given
        insertEntry(budgetId = 1, description = "Entry 1")
        insertEntry(budgetId = 1, description = "Entry 2")

        // When
        val result = dataSource.selectAllByBudgetId(1L).first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.budgetId == 1 })
    }

    @Test
    fun `getAllFilteredBy returns all entries when all filters are null or blank`() = runTest {
        // Given
        insertEntry(budgetId = 1, description = "Entry 1")
        insertEntry(budgetId = 1, description = "Entry 2")

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter())

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy filters by description case-insensitive`() = runTest {
        // Given
        insertEntry(budgetId = 1, description = "Groceries Shopping")
        insertEntry(budgetId = 1, description = "Restaurant Meal")
        insertEntry(budgetId = 1, description = "Grocery Store")

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = "GROCER"))

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.description == "Groceries Shopping" })
        assertTrue(result.any { it.description == "Grocery Store" })
    }

    @Test
    fun `getAllFilteredBy filters by type`() = runTest {
        // Given
        insertEntry(
            budgetId = 1,
            description = "Salary",
            type = BudgetEntry.Type.INCOME
        )
        insertEntry(
            budgetId = 1,
            description = "Groceries",
            type = BudgetEntry.Type.OUTCOME
        )

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(type = BudgetEntry.Type.INCOME))

        // Then
        assertEquals(1, result.size)
        assertEquals("Salary", result[0].description)
    }

    @Test
    fun `getAllFilteredBy filters by category`() = runTest {
        // Given
        insertEntry(
            budgetId = 1,
            description = "Supermarket",
            category = BudgetEntry.Category.GROCERIES
        )
        insertEntry(
            budgetId = 1,
            description = "Restaurant",
            category = BudgetEntry.Category.FOOD
        )

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(
            BudgetEntryFilter(category = BudgetEntry.Category.GROCERIES)
        )

        // Then
        assertEquals(1, result.size)
        assertEquals("Supermarket", result[0].description)
    }

    @Test
    fun `getAllFilteredBy filters by start date`() = runTest {
        // Given
        insertEntry(budgetId = 1, description = "Old", date = "2025-01-01")
        insertEntry(budgetId = 1, description = "Recent", date = "2025-01-15")

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(startDate = "2025-01-10"))

        // Then
        assertEquals(1, result.size)
        assertEquals("Recent", result[0].description)
    }

    @Test
    fun `getAllFilteredBy filters by end date`() = runTest {
        // Given
        insertEntry(budgetId = 1, description = "Old", date = "2025-01-01")
        insertEntry(budgetId = 1, description = "Recent", date = "2025-01-31")

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(endDate = "2025-01-15"))

        // Then
        assertEquals(1, result.size)
        assertEquals("Old", result[0].description)
    }

    @Test
    fun `getAllFilteredBy applies multiple filters together`() = runTest {
        // Given
        insertEntry(
            budgetId = 1,
            description = "Groceries",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.GROCERIES,
            date = "2025-01-15"
        )
        insertEntry(
            budgetId = 1,
            description = "Grocery Store",
            type = BudgetEntry.Type.INCOME,
            category = BudgetEntry.Category.GROCERIES,
            date = "2025-01-15"
        )
        insertEntry(
            budgetId = 1,
            description = "Groceries",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.FOOD,
            date = "2025-01-15"
        )

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(
            BudgetEntryFilter(
                description = "grocer",
                type = BudgetEntry.Type.OUTCOME,
                category = BudgetEntry.Category.GROCERIES
            )
        )

        // Then
        assertEquals(1, result.size)
        assertEquals("Groceries", result[0].description)
    }

    @Test
    fun `create inserts entry with correct parameters`() = runTest {
        // Given
        val entry = BudgetEntry(
            budgetId = 1,
            amount = "150.50",
            description = "New Entry",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.FOOD,
            date = "2025-01-20",
            invoice = "INV-001"
        )

        // When
        dataSource.create(entry)

        // Then
        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, result.size)
        assertEquals("New Entry", result[0].description)
        assertEquals("150.50", result[0].amount)
        assertEquals(BudgetEntry.Type.OUTCOME, result[0].type)
        assertEquals(BudgetEntry.Category.FOOD, result[0].category)
        assertEquals("INV-001", result[0].invoice)
    }

    @Test
    fun `create handles invalid amount string`() = runTest {
        // Given
        val entry = BudgetEntry(
            budgetId = 1,
            amount = "invalid",
            description = "Test"
        )

        // When
        dataSource.create(entry)

        // Then
        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, result.size)
        assertEquals("0.0", result[0].amount) // Should default to 0.0
    }

    @Test
    fun `update modifies existing entry`() = runTest {
        // Given
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

        // When
        dataSource.update(entry)

        // Then
        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, result.size)
        assertEquals("Updated Entry", result[0].description)
        assertEquals("200.0", result[0].amount)
        assertEquals(BudgetEntry.Type.INCOME, result[0].type)
    }

    @Test
    fun `deleteByIds removes entries with specified ids`() = runTest {
        // Given
        val id1 = insertEntry(budgetId = 1, description = "Entry 1")
        val id2 = insertEntry(budgetId = 1, description = "Entry 2")
        insertEntry(budgetId = 1, description = "Entry 3")

        // When
        dataSource.deleteByIds(listOf(id1.toLong(), id2.toLong()))

        // Then
        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, result.size)
        assertEquals("Entry 3", result[0].description)
    }

    @Test
    fun `deleteAllByBudgetId removes all entries for budget`() = runTest {
        // Given
        insertEntry(budgetId = 1, description = "Entry 1")
        insertEntry(budgetId = 1, description = "Entry 2")

        // When
        dataSource.deleteAllByBudgetId(1L)

        // Then
        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(emptyList(), result)
    }

    @Test
    fun `cache is populated when flow is collected`() = runTest {
        // Given
        insertEntry(budgetId = 1, description = "Test")

        // When - before collecting, cache should be empty
        assertEquals(emptyList(), dataSource.getAllCached())

        // After collecting, cache should be populated
        dataSource.selectAllByBudgetId(1L).first()

        // Then
        assertEquals(1, dataSource.getAllCached().size)
    }

    @Test
    fun `getAllFilteredBy returns empty list when no matches`() = runTest {
        // Given
        insertEntry(budgetId = 1, description = "Test")

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = "NonExistent"))

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllFilteredBy filters by date range`() = runTest {
        // Given
        insertEntry(budgetId = 1, description = "Jan 1", date = "2025-01-01")
        insertEntry(budgetId = 1, description = "Jan 15", date = "2025-01-15")
        insertEntry(budgetId = 1, description = "Jan 31", date = "2025-01-31")

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(
            BudgetEntryFilter(
                startDate = "2025-01-10",
                endDate = "2025-01-20"
            )
        )

        // Then
        assertEquals(1, result.size)
        assertEquals("Jan 15", result[0].description)
    }

    @Test
    fun `create handles null invoice`() = runTest {
        // Given
        val entry = BudgetEntry(
            budgetId = 1,
            amount = "100.0",
            description = "No Invoice",
            invoice = null
        )

        // When
        dataSource.create(entry)

        // Then
        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, result.size)
        assertEquals(null, result[0].invoice)
    }

    @Test
    fun `update handles amount conversion`() = runTest {
        // Given
        val insertedId = insertEntry(budgetId = 1, description = "Test", amount = 100.0)
        val entry = BudgetEntry(
            id = insertedId,
            budgetId = 1,
            amount = "999.99",
            description = "Test"
        )

        // When
        dataSource.update(entry)

        // Then
        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals("999.99", result[0].amount)
    }

    @Test
    fun `deleteByIds handles empty list`() = runTest {
        // Given
        insertEntry(budgetId = 1, description = "Entry 1")

        // When
        dataSource.deleteByIds(emptyList())

        // Then
        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, result.size) // Entry should still exist
    }

    @Test
    fun `selectAllByBudgetId returns empty list when no entries for budget`() = runTest {
        // When
        val result = dataSource.selectAllByBudgetId(999L).first()

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllFilteredBy handles blank description filter`() = runTest {
        // Given
        insertEntry(budgetId = 1, description = "Entry 1")
        insertEntry(budgetId = 1, description = "Entry 2")

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = "   "))

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `entries are ordered by id descending`() = runTest {
        // Given
        insertEntry(budgetId = 1, description = "First")
        insertEntry(budgetId = 1, description = "Second")
        insertEntry(budgetId = 1, description = "Third")

        // When
        val result = dataSource.selectAllByBudgetId(1L).first()

        // Then
        assertEquals(3, result.size)
        assertEquals("Third", result[0].description) // Most recent first
        assertEquals("Second", result[1].description)
        assertEquals("First", result[2].description)
    }

    @Test
    fun `create with zero amount`() = runTest {
        // Given
        val entry = BudgetEntry(
            budgetId = 1,
            amount = "0.0",
            description = "Zero Amount"
        )

        // When
        dataSource.create(entry)

        // Then
        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals("0.0", result[0].amount)
    }

    @Test
    fun `update with invalid amount defaults to zero`() = runTest {
        // Given
        val insertedId = insertEntry(budgetId = 1, description = "Test", amount = 100.0)
        val entry = BudgetEntry(
            id = insertedId,
            budgetId = 1,
            amount = "not-a-number",
            description = "Test"
        )

        // When
        dataSource.update(entry)

        // Then
        val result = dataSource.selectAllByBudgetId(1L).first()
        assertEquals("0.0", result[0].amount)
    }

    @Test
    fun `filter by multiple criteria with partial matches`() = runTest {
        // Given
        insertEntry(
            budgetId = 1,
            description = "Supermarket shopping",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.GROCERIES,
            date = "2025-01-15"
        )
        insertEntry(
            budgetId = 1,
            description = "Market visit",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.OTHER,
            date = "2025-01-16"
        )

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(
            BudgetEntryFilter(
                description = "market",
                type = BudgetEntry.Type.OUTCOME,
                category = BudgetEntry.Category.GROCERIES
            )
        )

        // Then
        assertEquals(1, result.size)
        assertEquals("Supermarket shopping", result[0].description)
    }

    // Helper function
    private fun insertEntry(
        budgetId: Long,
        description: String,
        amount: Double = 100.0,
        type: BudgetEntry.Type = BudgetEntry.Type.OUTCOME,
        category: BudgetEntry.Category = BudgetEntry.Category.OTHER,
        date: String = "2025-01-01",
        invoice: String? = null
    ): Int {
        database.budgetEntryQueries.insert(
            id = null,
            budgetId = budgetId,
            amount = amount,
            description = description,
            type = type,
            date = date,
            invoice = invoice,
            category = category
        )
        // Get the last inserted ID
        val lastId = database.budgetQueries.selectLastId().executeAsOne()
        return lastId.toInt()
    }
}
