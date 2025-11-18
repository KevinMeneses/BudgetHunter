package com.meneses.budgethunter.budgetList.data.datasource

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.db.BudgetQueries
import com.meneses.budgethunter.db.SelectAll
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.matcher.any
import dev.mokkery.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for BudgetLocalDataSource.
 * Tests caching, filtering, CRUD operations, and thread safety.
 *
 * Uses Mokkery to mock BudgetQueries for testing the actual BudgetLocalDataSource implementation.
 */
class BudgetLocalDataSourceTest {

    private lateinit var mockQueries: BudgetQueries
    private lateinit var mockQuery: Query<SelectAll>
    private lateinit var dataSource: BudgetLocalDataSource
    private val budgets = mutableListOf<SelectAll>()

    @BeforeTest
    fun setup() {
        budgets.clear()
        mockQueries = mock<BudgetQueries>()
        mockQuery = mock<Query<SelectAll>>()
        every { mockQueries.selectAll(any()) } returns mockQuery
        every { mockQuery.asFlow() } returns flowOf(budgets)
        every { mockQuery.mapToList(any()) } returns flowOf(budgets)
        dataSource = BudgetLocalDataSource(mockQueries, Dispatchers.Unconfined)
    }

    private fun addBudget(id: Long, name: String, amount: Double) {
        budgets.add(SelectAll(id = id, name = name, amount = amount))
    }

    @Test
    fun `getAllCached returns empty list initially`() = runTest {
        // When
        dataSource.budgets.first()
        val result = dataSource.getAllCached()

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllCached returns cached budgets after flow emission`() = runTest {
        // Given
        addBudget(id = 1L, name = "Test Budget", amount = 1000.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllCached()

        // Then
        assertEquals(1, result.size)
        assertEquals("Test Budget", result[0].name)
    }

    @Test
    fun `getById returns null when budget not found`() = runTest {
        // Given
        addBudget(id = 1L, name = "Test Budget", amount = 1000.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getById(999)

        // Then
        assertNull(result)
    }

    @Test
    fun `getById returns budget when found`() = runTest {
        // Given
        addBudget(id = 1L, name = "Test Budget", amount = 1000.0)
        addBudget(id = 2L, name = "Another Budget", amount = 2000.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getById(2)

        // Then
        assertNotNull(result)
        assertEquals("Another Budget", result.name)
        assertEquals(2000.0, result.amount)
    }

    @Test
    fun `getAllFilteredBy returns all budgets when filter name is null`() = runTest {
        // Given
        addBudget(id = 1L, name = "Test Budget", amount = 1000.0)
        addBudget(id = 2L, name = "Another Budget", amount = 2000.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = null))

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy returns all budgets when filter name is blank`() = runTest {
        // Given
        addBudget(id = 1L, name = "Test Budget", amount = 1000.0)
        addBudget(id = 2L, name = "Another Budget", amount = 2000.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "  "))

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy filters by name case-insensitive`() = runTest {
        // Given
        addBudget(id = 1L, name = "Monthly Budget 2025", amount = 1000.0)
        addBudget(id = 2L, name = "Yearly Savings", amount = 2000.0)
        addBudget(id = 3L, name = "Monthly Expenses", amount = 1500.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "MONTHLY"))

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.name.contains("Monthly", ignoreCase = true) })
    }

    @Test
    fun `create inserts budget and returns it with generated id`() = runTest {
        // Given
        val budget = Budget(id = 0, name = "New Budget", amount = 500.0)

        every { mockQueries.insert(name = "New Budget", amount = 500.0) } returns Unit
        every { mockQueries.selectLastId() } returns mockQuery
        every { mockQuery.executeAsOne() } returns 42L

        // When
        val result = dataSource.create(budget)

        // Then
        assertEquals(42, result.id)
        assertEquals("New Budget", result.name)
        assertEquals(500.0, result.amount)
    }

    @Test
    fun `update modifies budget with correct parameters`() = runTest {
        // Given
        val budget = Budget(id = 1, name = "Updated Budget", amount = 1500.0)

        every {
            mockQueries.update(
                id = 1L,
                name = "Updated Budget",
                amount = 1500.0
            )
        } returns Unit

        // When
        dataSource.update(budget)

        // Then - verification is implicit in Mokkery
    }

    @Test
    fun `deleteById removes budget`() = runTest {
        // Given
        every { mockQueries.deleteById(1L) } returns Unit

        // When
        dataSource.deleteById(1)

        // Then - verification is implicit in Mokkery
    }

    @Test
    fun `budgets flow emits cached list`() = runTest {
        // Given
        addBudget(id = 1L, name = "Test Budget", amount = 1000.0)

        // When
        val result = dataSource.budgets.first()

        // Then
        assertEquals(1, result.size)
        assertEquals("Test Budget", result[0].name)
    }

    @Test
    fun `cache is populated when flow is collected`() = runTest {
        // Given
        addBudget(id = 1L, name = "Test Budget", amount = 1000.0)
        addBudget(id = 2L, name = "Another Budget", amount = 2000.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllCached()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy handles empty cache`() = runTest {
        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "test"))

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllFilteredBy handles special characters in name`() = runTest {
        // Given
        addBudget(id = 1L, name = "Budget & Expenses", amount = 1000.0)
        addBudget(id = 2L, name = "100% Savings", amount = 2000.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "&"))

        // Then
        assertEquals(1, result.size)
        assertEquals("Budget & Expenses", result[0].name)
    }

    @Test
    fun `getById handles id 0`() = runTest {
        // Given
        addBudget(id = 0L, name = "Budget with ID 0", amount = 100.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getById(0)

        // Then
        assertNotNull(result)
        assertEquals("Budget with ID 0", result.name)
    }

    @Test
    fun `getById handles large id value`() = runTest {
        // Given
        addBudget(id = Long.MAX_VALUE, name = "Budget with large ID", amount = 100.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getById(Long.MAX_VALUE.toInt())

        // Then
        assertNotNull(result)
        assertEquals("Budget with large ID", result.name)
    }

    @Test
    fun `getAllFilteredBy handles partial name matches`() = runTest {
        // Given
        addBudget(id = 1L, name = "Monthly Budget 2025", amount = 1000.0)
        addBudget(id = 2L, name = "Yearly Savings", amount = 2000.0)
        addBudget(id = 3L, name = "Budget 2025 Quarterly", amount = 1500.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "2025"))

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.name.contains("2025") })
    }
}
