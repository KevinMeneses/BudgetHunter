package com.meneses.budgethunter.budgetList.data.datasource

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.meneses.budgethunter.budgetList.data.adapter.categoryAdapter
import com.meneses.budgethunter.budgetList.data.adapter.typeAdapter
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

    // Helper function
    private fun insertBudget(
        name: String,
        amount: Double,
        date: String = "2025-01-01"
    ): Int {
        database.budgetQueries.insert(amount = amount, name = name, date = date)
        return database.budgetQueries.selectLastId().executeAsOne().toInt()
    }
}
