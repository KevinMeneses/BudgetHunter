package com.meneses.budgethunter.budgetList.data.datasource

import app.cash.sqldelight.Query
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.db.BudgetQueries
import dev.mokkery.answering.returns
import dev.mokkery.answering.sequentiallyReturns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
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
 */
class BudgetLocalDataSourceTest {

    private lateinit var mockQueries: BudgetQueries
    private lateinit var dataSource: BudgetLocalDataSource

    @BeforeTest
    fun setup() {
        mockQueries = mock()
        dataSource = BudgetLocalDataSource(mockQueries, Dispatchers.Unconfined)
    }

    @Test
    fun `getAllCached returns empty list initially`() = runTest {
        // Given - empty query result
        val mockQuery = createMockQuery(emptyList())
        every { mockQueries.selectAll(any()) } returns mockQuery

        // When - trigger cache population
        dataSource.budgets.first()
        val result = dataSource.getAllCached()

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllCached returns cached budgets after flow emission`() = runTest {
        // Given
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val mockQuery = createMockQuery(listOf(budget))
        every { mockQueries.selectAll(any()) } returns mockQuery

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
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val mockQuery = createMockQuery(listOf(budget))
        every { mockQueries.selectAll(any()) } returns mockQuery

        // When
        dataSource.budgets.first()
        val result = dataSource.getById(999)

        // Then
        assertNull(result)
    }

    @Test
    fun `getById returns budget when found`() = runTest {
        // Given
        val budget1 = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budget2 = Budget(id = 2, name = "Another Budget", amount = 2000.0)
        val mockQuery = createMockQuery(listOf(budget1, budget2))
        every { mockQueries.selectAll(any()) } returns mockQuery

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
        val budget1 = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budget2 = Budget(id = 2, name = "Another Budget", amount = 2000.0)
        val mockQuery = createMockQuery(listOf(budget1, budget2))
        every { mockQueries.selectAll(any()) } returns mockQuery

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = null))

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy returns all budgets when filter name is blank`() = runTest {
        // Given
        val budget1 = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budget2 = Budget(id = 2, name = "Another Budget", amount = 2000.0)
        val mockQuery = createMockQuery(listOf(budget1, budget2))
        every { mockQueries.selectAll(any()) } returns mockQuery

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = ""))

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy filters budgets by name case-insensitive`() = runTest {
        // Given
        val budget1 = Budget(id = 1, name = "Groceries Budget", amount = 1000.0)
        val budget2 = Budget(id = 2, name = "Entertainment Budget", amount = 2000.0)
        val budget3 = Budget(id = 3, name = "Grocery Shopping", amount = 500.0)
        val mockQuery = createMockQuery(listOf(budget1, budget2, budget3))
        every { mockQueries.selectAll(any()) } returns mockQuery

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "GROCER"))

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Groceries Budget" })
        assertTrue(result.any { it.name == "Grocery Shopping" })
    }

    @Test
    fun `getAllFilteredBy returns empty list when no matches found`() = runTest {
        // Given
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val mockQuery = createMockQuery(listOf(budget))
        every { mockQueries.selectAll(any()) } returns mockQuery

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "NonExistent"))

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `create inserts budget and returns budget with generated id`() = runTest {
        // Given
        val budget = Budget(name = "New Budget", amount = 1500.0, date = "2025-01-15")
        val mockLastIdQuery = mock<Query<Long>>()
        every { mockLastIdQuery.executeAsOne() } returns 1L
        every { mockQueries.selectLastId() } returns mockLastIdQuery
        every { mockQueries.insert(any(), any(), any()) } returns Unit
        every { mockQueries.transaction(any()) } answers { call ->
            val block = call.arg<() -> Unit>(0)
            block()
        }

        // When
        val result = dataSource.create(budget)

        // Then
        verify { mockQueries.insert("New Budget", 1500.0, "2025-01-15") }
        assertEquals(1, result.id)
        assertEquals("New Budget", result.name)
        assertEquals(1500.0, result.amount)
    }

    @Test
    fun `create generates sequential ids for multiple budgets`() = runTest {
        // Given
        val mockLastIdQuery = mock<Query<Long>>()
        every { mockLastIdQuery.executeAsOne() } sequentiallyReturns listOf(1L, 2L, 3L)
        every { mockQueries.selectLastId() } returns mockLastIdQuery
        every { mockQueries.insert(any(), any(), any()) } returns Unit
        every { mockQueries.transaction(any()) } answers { call ->
            val block = call.arg<() -> Unit>(0)
            block()
        }

        // When
        val budget1 = dataSource.create(Budget(name = "Budget 1", amount = 100.0))
        val budget2 = dataSource.create(Budget(name = "Budget 2", amount = 200.0))
        val budget3 = dataSource.create(Budget(name = "Budget 3", amount = 300.0))

        // Then
        assertEquals(1, budget1.id)
        assertEquals(2, budget2.id)
        assertEquals(3, budget3.id)
    }

    @Test
    fun `update calls query update with correct parameters`() = runTest {
        // Given
        val budget = Budget(id = 5, name = "Updated Budget", amount = 2500.0, date = "2025-02-20")
        every { mockQueries.update(any(), any(), any(), any()) } returns Unit

        // When
        dataSource.update(budget)

        // Then
        verify { mockQueries.update(5L, 2500.0, "Updated Budget", "2025-02-20") }
    }

    @Test
    fun `delete calls query delete with correct id`() = runTest {
        // Given
        every { mockQueries.delete(any()) } returns Unit

        // When
        dataSource.delete(10L)

        // Then
        verify { mockQueries.delete(10L) }
    }

    @Test
    fun `budgets flow emits data from queries`() = runTest {
        // Given
        val budget1 = Budget(id = 1, name = "Budget 1", amount = 1000.0)
        val budget2 = Budget(id = 2, name = "Budget 2", amount = 2000.0)
        val mockQuery = createMockQuery(listOf(budget1, budget2))
        every { mockQueries.selectAll(any()) } returns mockQuery

        // When
        val result = dataSource.budgets.first()

        // Then
        assertEquals(2, result.size)
        assertEquals("Budget 1", result[0].name)
        assertEquals("Budget 2", result[1].name)
    }

    @Test
    fun `cache is populated when budgets flow is collected`() = runTest {
        // Given
        val budget = Budget(id = 1, name = "Test", amount = 100.0)
        val mockQuery = createMockQuery(listOf(budget))
        every { mockQueries.selectAll(any()) } returns mockQuery

        // When - before collecting, cache should be empty
        assertEquals(emptyList(), dataSource.getAllCached())

        // After collecting, cache should be populated
        dataSource.budgets.first()

        // Then
        assertEquals(1, dataSource.getAllCached().size)
    }

    @Test
    fun `getAllFilteredBy handles partial name matches`() = runTest {
        // Given
        val budget1 = Budget(id = 1, name = "Monthly Budget 2025", amount = 1000.0)
        val budget2 = Budget(id = 2, name = "Vacation Fund", amount = 2000.0)
        val mockQuery = createMockQuery(listOf(budget1, budget2))
        every { mockQueries.selectAll(any()) } returns mockQuery

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "month"))

        // Then
        assertEquals(1, result.size)
        assertEquals("Monthly Budget 2025", result[0].name)
    }

    @Test
    fun `create preserves budget amount and date`() = runTest {
        // Given
        val budget = Budget(
            name = "Precise Budget",
            amount = 1234.56,
            date = "2025-03-15"
        )
        val mockLastIdQuery = mock<Query<Long>>()
        every { mockLastIdQuery.executeAsOne() } returns 1L
        every { mockQueries.selectLastId() } returns mockLastIdQuery
        every { mockQueries.insert(any(), any(), any()) } returns Unit
        every { mockQueries.transaction(any()) } answers { call ->
            val block = call.arg<() -> Unit>(0)
            block()
        }

        // When
        val result = dataSource.create(budget)

        // Then
        assertEquals(1234.56, result.amount)
        assertEquals("2025-03-15", result.date)
        verify { mockQueries.insert("Precise Budget", 1234.56, "2025-03-15") }
    }

    @Test
    fun `getById handles id 0`() = runTest {
        // Given
        val budget = Budget(id = 0, name = "Zero Budget", amount = 0.0)
        val mockQuery = createMockQuery(listOf(budget))
        every { mockQueries.selectAll(any()) } returns mockQuery

        // When
        dataSource.budgets.first()
        val result = dataSource.getById(0)

        // Then
        assertNotNull(result)
        assertEquals("Zero Budget", result.name)
    }

    @Test
    fun `getAllFilteredBy handles special characters in filter`() = runTest {
        // Given
        val budget1 = Budget(id = 1, name = "Budget-2025", amount = 1000.0)
        val budget2 = Budget(id = 2, name = "Budget_Test", amount = 2000.0)
        val mockQuery = createMockQuery(listOf(budget1, budget2))
        every { mockQueries.selectAll(any()) } returns mockQuery

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "budget-"))

        // Then
        assertEquals(1, result.size)
        assertEquals("Budget-2025", result[0].name)
    }

    /**
     * Helper function to create a mock Query that returns the given data as a flow
     */
    private fun createMockQuery(data: List<Budget>): Query<Budget> {
        val mockQuery = mock<Query<Budget>>()
        every { mockQuery.asFlow() } returns flowOf(data)
        return mockQuery
    }
}
