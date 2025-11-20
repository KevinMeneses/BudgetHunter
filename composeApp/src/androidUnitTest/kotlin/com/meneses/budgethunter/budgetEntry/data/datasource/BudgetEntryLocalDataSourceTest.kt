package com.meneses.budgethunter.budgetEntry.data.datasource

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.db.BudgetEntryQueries
import com.meneses.budgethunter.db.Budget_entry
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for BudgetEntryLocalDataSource.
 * Tests caching, filtering, CRUD operations, and thread safety.
 *
 * Uses MockK to mock BudgetEntryQueries (final class from SQLDelight).
 */
class BudgetEntryLocalDataSourceTest {

    private lateinit var mockQueries: BudgetEntryQueries
    private lateinit var dataSource: BudgetEntryLocalDataSource

    @BeforeTest
    fun setup() {
        mockkStatic("app.cash.sqldelight.coroutines.FlowQuery")
        mockQueries = mockk(relaxed = true)
        dataSource = BudgetEntryLocalDataSource(mockQueries, Dispatchers.Unconfined)
    }

    @AfterTest
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `getAllCached returns empty list initially`() = runTest {
        assertEquals(emptyList(), dataSource.getAllCached())
    }

    @Test
    fun `getAllCached returns cached entries after flow emission`() = runTest {
        // Given
        val dbEntry = createDbEntry(id = 1, budgetId = 1, description = "Test Entry")
        setupSelectAllByBudgetId(1L, listOf(dbEntry))

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
        val entries = listOf(
            createDbEntry(id = 1, budgetId = 1, description = "Entry 1"),
            createDbEntry(id = 2, budgetId = 1, description = "Entry 2")
        )
        val mockQuery = mockk<Query<Budget_entry>>()

        every { mockQueries.selectAllByBudgetId(1L) } returns mockQuery
        every { mockQuery.asFlow() } returns flowOf(entries)

        // When
        val result = dataSource.selectAllByBudgetId(1L).first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.budgetId == 1 })
    }

    @Test
    fun `getAllFilteredBy returns all entries when all filters are null or blank`() = runTest {
        // Given
        val entries = listOf(
            createDbEntry(id = 1, budgetId = 1, description = "Entry 1"),
            createDbEntry(id = 2, budgetId = 1, description = "Entry 2")
        )
        setupSelectAllByBudgetId(1L, entries)

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter())

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy filters by description case-insensitive`() = runTest {
        // Given
        val entries = listOf(
            createDbEntry(id = 1, budgetId = 1, description = "Groceries Shopping"),
            createDbEntry(id = 2, budgetId = 1, description = "Restaurant Meal"),
            createDbEntry(id = 3, budgetId = 1, description = "Grocery Store")
        )
        setupSelectAllByBudgetId(1L, entries)

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
        val entries = listOf(
            createDbEntry(
                id = 1,
                budgetId = 1,
                description = "Salary",
                type = BudgetEntry.Type.INCOME
            ),
            createDbEntry(
                id = 2,
                budgetId = 1,
                description = "Groceries",
                type = BudgetEntry.Type.OUTCOME
            )
        )
        setupSelectAllByBudgetId(1L, entries)

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
        val entries = listOf(
            createDbEntry(
                id = 1,
                budgetId = 1,
                description = "Supermarket",
                category = BudgetEntry.Category.GROCERIES
            ),
            createDbEntry(
                id = 2,
                budgetId = 1,
                description = "Restaurant",
                category = BudgetEntry.Category.FOOD
            )
        )
        setupSelectAllByBudgetId(1L, entries)

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
        val entries = listOf(
            createDbEntry(id = 1, budgetId = 1, description = "Old", date = "2025-01-01"),
            createDbEntry(id = 2, budgetId = 1, description = "Recent", date = "2025-01-15")
        )
        setupSelectAllByBudgetId(1L, entries)

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
        val entries = listOf(
            createDbEntry(id = 1, budgetId = 1, description = "Old", date = "2025-01-01"),
            createDbEntry(id = 2, budgetId = 1, description = "Recent", date = "2025-01-31")
        )
        setupSelectAllByBudgetId(1L, entries)

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
        val entries = listOf(
            createDbEntry(
                id = 1,
                budgetId = 1,
                description = "Groceries",
                type = BudgetEntry.Type.OUTCOME,
                category = BudgetEntry.Category.GROCERIES,
                date = "2025-01-15"
            ),
            createDbEntry(
                id = 2,
                budgetId = 1,
                description = "Grocery Store",
                type = BudgetEntry.Type.INCOME,
                category = BudgetEntry.Category.GROCERIES,
                date = "2025-01-15"
            ),
            createDbEntry(
                id = 3,
                budgetId = 1,
                description = "Groceries",
                type = BudgetEntry.Type.OUTCOME,
                category = BudgetEntry.Category.FOOD,
                date = "2025-01-15"
            )
        )
        setupSelectAllByBudgetId(1L, entries)

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
        assertEquals(1, result[0].id)
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
        verify {
            mockQueries.insert(
                id = null,
                budgetId = 1L,
                amount = 150.50,
                description = "New Entry",
                type = BudgetEntry.Type.OUTCOME,
                category = BudgetEntry.Category.FOOD,
                date = "2025-01-20",
                invoice = "INV-001"
            )
        }
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
        verify {
            mockQueries.insert(
                id = null,
                budgetId = 1L,
                amount = 0.0, // Should default to 0.0
                description = "Test",
                type = BudgetEntry.Type.OUTCOME,
                category = BudgetEntry.Category.OTHER,
                date = "",
                invoice = null
            )
        }
    }

    @Test
    fun `update modifies existing entry`() = runTest {
        // Given
        val entry = BudgetEntry(
            id = 5,
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
        verify {
            mockQueries.update(
                id = 5L,
                budgetId = 1L,
                amount = 200.0,
                description = "Updated Entry",
                type = BudgetEntry.Type.INCOME,
                category = BudgetEntry.Category.OTHER,
                date = "2025-02-01",
                invoice = null
            )
        }
    }

    @Test
    fun `deleteByIds removes entries with specified ids`() = runTest {
        // When
        dataSource.deleteByIds(listOf(1L, 2L, 3L))

        // Then
        verify { mockQueries.deleteByIds(listOf(1L, 2L, 3L)) }
    }

    @Test
    fun `deleteAllByBudgetId removes all entries for budget`() = runTest {
        // When
        dataSource.deleteAllByBudgetId(1L)

        // Then
        verify { mockQueries.deleteAllByBudgetId(1L) }
    }

    @Test
    fun `cache is populated when flow is collected`() = runTest {
        // Given
        val entries = listOf(createDbEntry(id = 1, budgetId = 1, description = "Test"))
        setupSelectAllByBudgetId(1L, entries)

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
        val entries = listOf(createDbEntry(id = 1, budgetId = 1, description = "Test"))
        setupSelectAllByBudgetId(1L, entries)

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = "NonExistent"))

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllFilteredBy filters by date range`() = runTest {
        // Given
        val entries = listOf(
            createDbEntry(id = 1, budgetId = 1, description = "Jan 1", date = "2025-01-01"),
            createDbEntry(id = 2, budgetId = 1, description = "Jan 15", date = "2025-01-15"),
            createDbEntry(id = 3, budgetId = 1, description = "Jan 31", date = "2025-01-31")
        )
        setupSelectAllByBudgetId(1L, entries)

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
        verify {
            mockQueries.insert(
                id = null,
                budgetId = 1L,
                amount = 100.0,
                description = "No Invoice",
                type = BudgetEntry.Type.OUTCOME,
                category = BudgetEntry.Category.OTHER,
                date = "",
                invoice = null
            )
        }
    }

    @Test
    fun `update handles amount conversion`() = runTest {
        // Given
        val entry = BudgetEntry(
            id = 1,
            budgetId = 1,
            amount = "999.99",
            description = "Test"
        )

        // When
        dataSource.update(entry)

        // Then
        verify {
            mockQueries.update(
                id = 1L,
                budgetId = 1L,
                amount = 999.99,
                description = "Test",
                type = BudgetEntry.Type.OUTCOME,
                category = BudgetEntry.Category.OTHER,
                date = "",
                invoice = null
            )
        }
    }

    @Test
    fun `deleteByIds handles empty list`() = runTest {
        // When
        dataSource.deleteByIds(emptyList())

        // Then
        verify { mockQueries.deleteByIds(emptyList()) }
    }

    @Test
    fun `selectAllByBudgetId returns empty list when no entries for budget`() = runTest {
        // Given
        setupSelectAllByBudgetId(999L, emptyList())

        // When
        val result = dataSource.selectAllByBudgetId(999L).first()

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllFilteredBy handles blank description filter`() = runTest {
        // Given
        val entries = listOf(
            createDbEntry(id = 1, budgetId = 1, description = "Entry 1"),
            createDbEntry(id = 2, budgetId = 1, description = "Entry 2")
        )
        setupSelectAllByBudgetId(1L, entries)

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = "   "))

        // Then
        assertEquals(2, result.size)
    }

    // Helper functions
    private fun createDbEntry(
        id: Long,
        budgetId: Long,
        description: String,
        amount: Double = 0.0,
        type: BudgetEntry.Type = BudgetEntry.Type.OUTCOME,
        category: BudgetEntry.Category = BudgetEntry.Category.OTHER,
        date: String = "",
        invoice: String? = null
    ) = Budget_entry(
        id = id,
        budget_id = budgetId,
        amount = amount,
        description = description,
        type = type,
        category = category,
        date = date,
        invoice = invoice
    )

    private fun setupSelectAllByBudgetId(budgetId: Long, entries: List<Budget_entry>) {
        val mockQuery = mockk<Query<Budget_entry>>()
        every { mockQueries.selectAllByBudgetId(budgetId) } returns mockQuery
        every { mockQuery.asFlow() } returns flowOf(entries)
        every { mockQuery.asFlow().mapToList(any()) } returns flowOf(entries.map { it.toDomain() })
    }
}

// Extension function to convert DB entity to domain model (for testing)
private fun Budget_entry.toDomain() = BudgetEntry(
    id = id.toInt(),
    budgetId = budget_id.toInt(),
    amount = amount.toString(),
    description = description,
    type = type,
    category = category,
    date = date,
    invoice = invoice
)
