package com.meneses.budgethunter.budgetEntry.data.datasource

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.db.BudgetEntryQueries
import com.meneses.budgethunter.db.Budget_entry
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
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for BudgetEntryLocalDataSource.
 * Tests caching, filtering, CRUD operations, and thread safety.
 *
 * Uses Mokkery to mock BudgetEntryQueries for testing the actual BudgetEntryLocalDataSource implementation.
 */
class BudgetEntryLocalDataSourceTest {

    private lateinit var mockQueries: BudgetEntryQueries
    private lateinit var mockQuery: Query<Budget_entry>
    private lateinit var dataSource: BudgetEntryLocalDataSource
    private val entries = mutableListOf<Budget_entry>()

    @BeforeTest
    fun setup() {
        entries.clear()
        mockQueries = mock<BudgetEntryQueries>()
        mockQuery = mock<Query<Budget_entry>>()
        dataSource = BudgetEntryLocalDataSource(mockQueries, Dispatchers.Unconfined)
    }

    private fun setupSelectAllByBudgetId(budgetId: Long) {
        val filtered = entries.filter { it.budget_id == budgetId }
        every { mockQueries.selectAllByBudgetId(budgetId) } returns mockQuery
        every { mockQuery.asFlow() } returns flowOf(filtered)
        every { mockQuery.mapToList(any()) } returns flowOf(filtered)
    }

    private fun addEntry(
        id: Long,
        budgetId: Long,
        description: String,
        amount: Double = 0.0,
        type: BudgetEntry.Type = BudgetEntry.Type.OUTCOME,
        category: BudgetEntry.Category = BudgetEntry.Category.OTHER,
        date: String = "",
        invoice: String? = null
    ) {
        entries.add(
            Budget_entry(
                id = id,
                budget_id = budgetId,
                amount = amount,
                description = description,
                type = type,
                category = category,
                date = date,
                invoice = invoice
            )
        )
    }

    @Test
    fun `getAllCached returns empty list initially`() = runTest {
        assertEquals(emptyList(), dataSource.getAllCached())
    }

    @Test
    fun `getAllCached returns cached entries after flow emission`() = runTest {
        // Given
        addEntry(id = 1L, budgetId = 1L, amount = 100.0, description = "Test Entry")
        setupSelectAllByBudgetId(1L)

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
        addEntry(id = 1L, budgetId = 1L, description = "Entry 1")
        addEntry(id = 2L, budgetId = 2L, description = "Entry 2")
        addEntry(id = 3L, budgetId = 1L, description = "Entry 3")
        setupSelectAllByBudgetId(1L)

        // When
        val result = dataSource.selectAllByBudgetId(1L).first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.budgetId == 1 })
    }

    @Test
    fun `getAllFilteredBy returns all entries when all filters are null or blank`() = runTest {
        // Given
        addEntry(id = 1L, budgetId = 1L, description = "Entry 1")
        addEntry(id = 2L, budgetId = 1L, description = "Entry 2")
        setupSelectAllByBudgetId(1L)

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter())

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy filters by description case-insensitive`() = runTest {
        // Given
        addEntry(id = 1L, budgetId = 1L, description = "Groceries Shopping")
        addEntry(id = 2L, budgetId = 1L, description = "Restaurant Meal")
        addEntry(id = 3L, budgetId = 1L, description = "Grocery Store")
        setupSelectAllByBudgetId(1L)

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = "GROCER"))

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.description.contains("Grocer", ignoreCase = true) })
    }

    @Test
    fun `getAllFilteredBy filters by type`() = runTest {
        // Given
        addEntry(id = 1L, budgetId = 1L, description = "Salary", type = BudgetEntry.Type.INCOME)
        addEntry(id = 2L, budgetId = 1L, description = "Rent", type = BudgetEntry.Type.OUTCOME)
        addEntry(id = 3L, budgetId = 1L, description = "Bonus", type = BudgetEntry.Type.INCOME)
        setupSelectAllByBudgetId(1L)

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(type = BudgetEntry.Type.INCOME))

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.type == BudgetEntry.Type.INCOME })
    }

    @Test
    fun `getAllFilteredBy filters by category`() = runTest {
        // Given
        addEntry(id = 1L, budgetId = 1L, description = "Groceries", category = BudgetEntry.Category.FOOD)
        addEntry(id = 2L, budgetId = 1L, description = "Gas", category = BudgetEntry.Category.TRANSPORT)
        addEntry(id = 3L, budgetId = 1L, description = "Restaurant", category = BudgetEntry.Category.FOOD)
        setupSelectAllByBudgetId(1L)

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(category = BudgetEntry.Category.FOOD))

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.category == BudgetEntry.Category.FOOD })
    }

    @Test
    fun `getAllFilteredBy filters by date range`() = runTest {
        // Given
        addEntry(id = 1L, budgetId = 1L, description = "Old Entry", date = "2025-01-01")
        addEntry(id = 2L, budgetId = 1L, description = "Current Entry", date = "2025-01-15")
        addEntry(id = 3L, budgetId = 1L, description = "Future Entry", date = "2025-01-30")
        setupSelectAllByBudgetId(1L)

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(
            BudgetEntryFilter(startDate = "2025-01-10", endDate = "2025-01-20")
        )

        // Then
        assertEquals(1, result.size)
        assertEquals("Current Entry", result[0].description)
    }

    @Test
    fun `getAllFilteredBy combines multiple filters`() = runTest {
        // Given
        addEntry(
            id = 1L, budgetId = 1L, description = "Food Shopping",
            type = BudgetEntry.Type.OUTCOME, category = BudgetEntry.Category.FOOD,
            date = "2025-01-15"
        )
        addEntry(
            id = 2L, budgetId = 1L, description = "Salary Income",
            type = BudgetEntry.Type.INCOME, category = BudgetEntry.Category.OTHER,
            date = "2025-01-15"
        )
        addEntry(
            id = 3L, budgetId = 1L, description = "Restaurant Food",
            type = BudgetEntry.Type.OUTCOME, category = BudgetEntry.Category.FOOD,
            date = "2025-01-20"
        )
        setupSelectAllByBudgetId(1L)

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(
            BudgetEntryFilter(
                description = "food",
                type = BudgetEntry.Type.OUTCOME,
                category = BudgetEntry.Category.FOOD,
                startDate = "2025-01-01",
                endDate = "2025-01-16"
            )
        )

        // Then
        assertEquals(1, result.size)
        assertEquals("Food Shopping", result[0].description)
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

        every {
            mockQueries.insert(
                id = null,
                budget_id = 1L,
                amount = 150.50,
                description = "New Entry",
                type = BudgetEntry.Type.OUTCOME,
                category = BudgetEntry.Category.FOOD,
                date = "2025-01-20",
                invoice = "INV-001"
            )
        } returns Unit

        // When
        dataSource.create(entry)

        // Then - verification is implicit in Mokkery
    }

    @Test
    fun `create handles invalid amount string`() = runTest {
        // Given
        val entry = BudgetEntry(budgetId = 1, amount = "invalid", description = "Test")

        every {
            mockQueries.insert(
                id = null,
                budget_id = 1L,
                amount = 0.0,
                description = "Test",
                type = BudgetEntry.Type.OUTCOME,
                category = BudgetEntry.Category.OTHER,
                date = "",
                invoice = null
            )
        } returns Unit

        // When
        dataSource.create(entry)

        // Then - verification is implicit in Mokkery
    }

    @Test
    fun `update modifies entry with correct parameters`() = runTest {
        // Given
        val entry = BudgetEntry(
            id = 1,
            budgetId = 1,
            amount = "200.00",
            description = "Updated Entry",
            type = BudgetEntry.Type.INCOME,
            category = BudgetEntry.Category.SALARY,
            date = "2025-01-21",
            invoice = "INV-002"
        )

        every {
            mockQueries.update(
                id = 1L,
                budget_id = 1L,
                amount = 200.00,
                description = "Updated Entry",
                type = BudgetEntry.Type.INCOME,
                category = BudgetEntry.Category.SALARY,
                date = "2025-01-21",
                invoice = "INV-002"
            )
        } returns Unit

        // When
        dataSource.update(entry)

        // Then - verification is implicit in Mokkery
    }

    @Test
    fun `update handles invalid amount string`() = runTest {
        // Given
        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "not-a-number", description = "Test")

        every {
            mockQueries.update(
                id = 1L,
                budget_id = 1L,
                amount = 0.0,
                description = "Test",
                type = BudgetEntry.Type.OUTCOME,
                category = BudgetEntry.Category.OTHER,
                date = "",
                invoice = null
            )
        } returns Unit

        // When
        dataSource.update(entry)

        // Then - verification is implicit in Mokkery
    }

    @Test
    fun `deleteByIds deletes multiple entries`() = runTest {
        // Given
        val ids = listOf(1L, 2L, 3L)

        every { mockQueries.deleteByIds(ids) } returns Unit

        // When
        dataSource.deleteByIds(ids)

        // Then - verification is implicit in Mokkery
    }

    @Test
    fun `deleteByIds handles empty list`() = runTest {
        // Given
        val ids = emptyList<Long>()

        every { mockQueries.deleteByIds(ids) } returns Unit

        // When
        dataSource.deleteByIds(ids)

        // Then - verification is implicit in Mokkery
    }

    @Test
    fun `deleteAllByBudgetId deletes all entries for budget`() = runTest {
        // Given
        every { mockQueries.deleteAllByBudgetId(1L) } returns Unit

        // When
        dataSource.deleteAllByBudgetId(1L)

        // Then - verification is implicit in Mokkery
    }

    @Test
    fun `selectAllByBudgetId returns empty list when no entries exist`() = runTest {
        // Given
        setupSelectAllByBudgetId(999L)

        // When
        val result = dataSource.selectAllByBudgetId(999L).first()

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllFilteredBy handles null description filter`() = runTest {
        // Given
        addEntry(id = 1L, budgetId = 1L, description = "Entry 1")
        addEntry(id = 2L, budgetId = 1L, description = "Entry 2")
        setupSelectAllByBudgetId(1L)

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = null))

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy handles blank description filter`() = runTest {
        // Given
        addEntry(id = 1L, budgetId = 1L, description = "Entry 1")
        addEntry(id = 2L, budgetId = 1L, description = "Entry 2")
        setupSelectAllByBudgetId(1L)

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = "   "))

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy handles special characters in description`() = runTest {
        // Given
        addEntry(id = 1L, budgetId = 1L, description = "Café & Restaurant")
        addEntry(id = 2L, budgetId = 1L, description = "100% Organic")
        setupSelectAllByBudgetId(1L)

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = "&"))

        // Then
        assertEquals(1, result.size)
        assertEquals("Café & Restaurant", result[0].description)
    }

    @Test
    fun `selectAllByBudgetId handles entries with id 0`() = runTest {
        // Given
        addEntry(id = 0L, budgetId = 1L, description = "Entry with ID 0")
        setupSelectAllByBudgetId(1L)

        // When
        val result = dataSource.selectAllByBudgetId(1L).first()

        // Then
        assertEquals(1, result.size)
        assertEquals(0, result[0].id)
    }

    @Test
    fun `selectAllByBudgetId handles entries with null invoice`() = runTest {
        // Given
        addEntry(id = 1L, budgetId = 1L, description = "No Invoice", invoice = null)
        setupSelectAllByBudgetId(1L)

        // When
        val result = dataSource.selectAllByBudgetId(1L).first()

        // Then
        assertEquals(1, result.size)
        assertEquals(null, result[0].invoice)
    }

    @Test
    fun `getAllFilteredBy with only start date filters correctly`() = runTest {
        // Given
        addEntry(id = 1L, budgetId = 1L, description = "Before", date = "2025-01-05")
        addEntry(id = 2L, budgetId = 1L, description = "After", date = "2025-01-15")
        setupSelectAllByBudgetId(1L)

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(startDate = "2025-01-10"))

        // Then
        assertEquals(1, result.size)
        assertEquals("After", result[0].description)
    }

    @Test
    fun `getAllFilteredBy with only end date filters correctly`() = runTest {
        // Given
        addEntry(id = 1L, budgetId = 1L, description = "Before", date = "2025-01-05")
        addEntry(id = 2L, budgetId = 1L, description = "After", date = "2025-01-15")
        setupSelectAllByBudgetId(1L)

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(endDate = "2025-01-10"))

        // Then
        assertEquals(1, result.size)
        assertEquals("Before", result[0].description)
    }
}
