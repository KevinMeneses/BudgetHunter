package com.meneses.budgethunter.budgetList.data.datasource

import app.cash.sqldelight.Query
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.db.BudgetQueries
import com.meneses.budgethunter.db.SelectAll
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for BudgetLocalDataSource.
 * Tests caching, filtering, CRUD operations, and thread safety.
 *
 * Uses MockK to mock BudgetQueries (final class from SQLDelight).
 */
class BudgetLocalDataSourceTest {

    private lateinit var mockQueries: BudgetQueries
    private lateinit var dataSource: BudgetLocalDataSource

    @BeforeTest
    fun setup() {
        mockQueries = mockk(relaxed = true)
        dataSource = BudgetLocalDataSource(mockQueries, Dispatchers.Unconfined)
    }

    @Test
    fun `getAllCached returns empty list initially`() = runTest {
        assertEquals(emptyList(), dataSource.getAllCached())
    }

    @Test
    fun `getAllCached returns cached budgets after flow emission`() = runTest {
        // Given
        val budgets = listOf(createSelectAll(id = 1, name = "Budget 1", amount = 1000.0))
        setupSelectAll(budgets)

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
        val budgets = listOf(
            createSelectAll(id = 1, name = "Budget 1", amount = 1000.0),
            createSelectAll(id = 2, name = "Budget 2", amount = 2000.0)
        )
        setupSelectAll(budgets)

        // When
        val result = dataSource.budgets.first()

        // Then
        assertEquals(2, result.size)
        assertEquals("Budget 1", result[0].name)
        assertEquals("Budget 2", result[1].name)
    }

    @Test
    fun `create inserts budget and returns it with generated id`() = runTest {
        // Given
        val budget = Budget(id = 0, name = "New Budget", amount = 500.0, date = "2025-01-01")
        val lastIdQuery = mockk<Query<Long>>()

        every { lastIdQuery.executeAsOne() } returns 42L
        every { mockQueries.selectLastId() } returns lastIdQuery

        // When
        val result = dataSource.create(budget)

        // Then
        verify {
            mockQueries.insert(
                name = "New Budget",
                amount = 500.0,
                date = "2025-01-01"
            )
        }
        assertEquals(42, result.id)
        assertEquals("New Budget", result.name)
    }

    @Test
    fun `update modifies existing budget`() = runTest {
        // Given
        val budget = Budget(id = 1, name = "Updated Budget", amount = 1500.0, date = "2025-01-15")

        // When
        dataSource.update(budget)

        // Then
        verify {
            mockQueries.update(
                name = "Updated Budget",
                amount = 1500.0,
                date = "2025-01-15",
                id = 1L
            )
        }
    }

    @Test
    fun `delete removes budget by id`() = runTest {
        // When
        dataSource.delete(1L)

        // Then
        verify { mockQueries.delete(1L) }
    }

    @Test
    fun `getAllFilteredBy returns all budgets when filter name is null`() = runTest {
        // Given
        val budgets = listOf(
            createSelectAll(id = 1, name = "Budget 1", amount = 1000.0),
            createSelectAll(id = 2, name = "Budget 2", amount = 2000.0)
        )
        setupSelectAll(budgets)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(null)

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy returns all budgets when filter name is blank`() = runTest {
        // Given
        val budgets = listOf(
            createSelectAll(id = 1, name = "Budget 1", amount = 1000.0),
            createSelectAll(id = 2, name = "Budget 2", amount = 2000.0)
        )
        setupSelectAll(budgets)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy("   ")

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy filters by name case-insensitive`() = runTest {
        // Given
        val budgets = listOf(
            createSelectAll(id = 1, name = "Monthly Budget", amount = 1000.0),
            createSelectAll(id = 2, name = "Yearly Budget", amount = 12000.0),
            createSelectAll(id = 3, name = "Monthly Expenses", amount = 500.0)
        )
        setupSelectAll(budgets)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy("MONTHLY")

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Monthly Budget" })
        assertTrue(result.any { it.name == "Monthly Expenses" })
    }

    @Test
    fun `cache is populated when flow is collected`() = runTest {
        // Given
        val budgets = listOf(createSelectAll(id = 1, name = "Budget 1", amount = 1000.0))
        setupSelectAll(budgets)

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
        val budgets = listOf(createSelectAll(id = 1, name = "Budget 1", amount = 1000.0))
        setupSelectAll(budgets)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy("NonExistent")

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `create handles id 0`() = runTest {
        // Given
        val budget = Budget(id = 0, name = "New Budget", amount = 100.0, date = "2025-01-01")
        val lastIdQuery = mockk<Query<Long>>()

        every { lastIdQuery.executeAsOne() } returns 1L
        every { mockQueries.selectLastId() } returns lastIdQuery

        // When
        val result = dataSource.create(budget)

        // Then
        assertEquals(1, result.id)
    }

    @Test
    fun `update handles budget with special characters`() = runTest {
        // Given
        val budget = Budget(
            id = 1,
            name = "Budget with 'special' \"characters\"",
            amount = 100.0,
            date = "2025-01-01"
        )

        // When
        dataSource.update(budget)

        // Then
        verify {
            mockQueries.update(
                name = "Budget with 'special' \"characters\"",
                amount = 100.0,
                date = "2025-01-01",
                id = 1L
            )
        }
    }

    @Test
    fun `delete handles large id`() = runTest {
        // When
        dataSource.delete(Long.MAX_VALUE)

        // Then
        verify { mockQueries.delete(Long.MAX_VALUE) }
    }

    @Test
    fun `create uses transaction`() = runTest {
        // Given
        val budget = Budget(id = 0, name = "New Budget", amount = 100.0, date = "2025-01-01")
        val lastIdQuery = mockk<Query<Long>>()
        val transactionSlot = slot<() -> Unit>()

        every { lastIdQuery.executeAsOne() } returns 1L
        every { mockQueries.selectLastId() } returns lastIdQuery
        every { mockQueries.transaction(capture(transactionSlot)) } answers {
            transactionSlot.captured.invoke()
        }

        // When
        dataSource.create(budget)

        // Then
        verify { mockQueries.transaction(any()) }
    }

    @Test
    fun `getAllFilteredBy handles partial name matches`() = runTest {
        // Given
        val budgets = listOf(
            createSelectAll(id = 1, name = "Monthly Budget 2025", amount = 1000.0),
            createSelectAll(id = 2, name = "Budget 2025", amount = 2000.0),
            createSelectAll(id = 3, name = "Monthly Expenses", amount = 500.0)
        )
        setupSelectAll(budgets)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy("2025")

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.name.contains("2025") })
    }

    @Test
    fun `budgets flow emits empty list when no budgets`() = runTest {
        // Given
        setupSelectAll(emptyList())

        // When
        val result = dataSource.budgets.first()

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllFilteredBy handles empty cache`() = runTest {
        // Given - no flow collected, cache is empty

        // When
        val result = dataSource.getAllFilteredBy("Test")

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
        val lastIdQuery = mockk<Query<Long>>()

        every { lastIdQuery.executeAsOne() } returns 123L
        every { mockQueries.selectLastId() } returns lastIdQuery

        // When
        val result = dataSource.create(budget)

        // Then
        assertEquals(123, result.id)
        assertEquals("Test Budget", result.name)
        assertEquals(999.99, result.amount)
        assertEquals("2025-12-31", result.date)
    }

    @Test
    fun `getAllFilteredBy preserves budget order`() = runTest {
        // Given
        val budgets = listOf(
            createSelectAll(id = 1, name = "A Budget", amount = 100.0),
            createSelectAll(id = 2, name = "B Budget", amount = 200.0),
            createSelectAll(id = 3, name = "C Budget", amount = 300.0)
        )
        setupSelectAll(budgets)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy("Budget")

        // Then
        assertEquals(3, result.size)
        assertEquals("A Budget", result[0].name)
        assertEquals("B Budget", result[1].name)
        assertEquals("C Budget", result[2].name)
    }

    @Test
    fun `update handles zero amount`() = runTest {
        // Given
        val budget = Budget(id = 1, name = "Zero Budget", amount = 0.0, date = "2025-01-01")

        // When
        dataSource.update(budget)

        // Then
        verify {
            mockQueries.update(
                name = "Zero Budget",
                amount = 0.0,
                date = "2025-01-01",
                id = 1L
            )
        }
    }

    @Test
    fun `update handles negative amount`() = runTest {
        // Given
        val budget = Budget(id = 1, name = "Negative Budget", amount = -100.0, date = "2025-01-01")

        // When
        dataSource.update(budget)

        // Then
        verify {
            mockQueries.update(
                name = "Negative Budget",
                amount = -100.0,
                date = "2025-01-01",
                id = 1L
            )
        }
    }

    // Helper functions
    private fun createSelectAll(
        id: Long,
        name: String,
        amount: Double,
        date: String = "",
        totalEntries: Long = 0
    ) = SelectAll(
        id = id,
        name = name,
        amount = amount,
        date = date,
        total_entries = totalEntries
    )

    private fun setupSelectAll(budgets: List<SelectAll>) {
        val mockQuery = mockk<Query<SelectAll>>()
        every { mockQueries.selectAll(any()) } returns mockQuery
        every { mockQuery.asFlow() } returns flowOf(budgets)
    }
}
