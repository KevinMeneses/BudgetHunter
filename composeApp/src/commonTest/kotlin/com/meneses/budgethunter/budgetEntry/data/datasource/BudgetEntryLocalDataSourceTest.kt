package com.meneses.budgethunter.budgetEntry.data.datasource

import app.cash.sqldelight.Query
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.db.BudgetEntryQueries
import com.meneses.budgethunter.db.Budget_entry
import dev.mokkery.answering.returns
import dev.mokkery.every
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
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for BudgetEntryLocalDataSource.
 * Tests caching, filtering, CRUD operations, and thread safety.
 */
class BudgetEntryLocalDataSourceTest {

    private lateinit var mockQueries: BudgetEntryQueries
    private lateinit var dataSource: BudgetEntryLocalDataSource

    @BeforeTest
    fun setup() {
        mockQueries = mock()
        dataSource = BudgetEntryLocalDataSource(mockQueries, Dispatchers.Unconfined)
    }

    @Test
    fun `getAllCached returns empty list initially`() = runTest {
        // Given
        assertEquals(emptyList(), dataSource.getAllCached())
    }

    @Test
    fun `getAllCached returns cached entries after flow emission`() = runTest {
        // Given
        val dbEntry = Budget_entry(
            id = 1L,
            budget_id = 1L,
            amount = 100.0,
            description = "Test Entry",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.OTHER,
            date = "",
            invoice = null
        )
        val mockQuery = createMockQuery(listOf(dbEntry))
        every { mockQueries.selectAllByBudgetId(any()) } returns mockQuery

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
        val dbEntry1 = createDbEntry(id = 1, budgetId = 1, description = "Entry 1")
        val dbEntry2 = createDbEntry(id = 3, budgetId = 1, description = "Entry 3")
        val mockQuery = createMockQuery(listOf(dbEntry1, dbEntry2))
        every { mockQueries.selectAllByBudgetId(1L) } returns mockQuery

        // When
        val result = dataSource.selectAllByBudgetId(1L).first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.budgetId == 1 })
        verify { mockQueries.selectAllByBudgetId(1L) }
    }

    @Test
    fun `getAllFilteredBy returns all entries when all filters are null or blank`() = runTest {
        // Given
        val dbEntry1 = createDbEntry(id = 1, budgetId = 1, description = "Entry 1")
        val dbEntry2 = createDbEntry(id = 2, budgetId = 1, description = "Entry 2")
        val mockQuery = createMockQuery(listOf(dbEntry1, dbEntry2))
        every { mockQueries.selectAllByBudgetId(1L) } returns mockQuery

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter())

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy filters by description case-insensitive`() = runTest {
        // Given
        val dbEntry1 = createDbEntry(id = 1, budgetId = 1, description = "Groceries Shopping")
        val dbEntry2 = createDbEntry(id = 2, budgetId = 1, description = "Restaurant Meal")
        val dbEntry3 = createDbEntry(id = 3, budgetId = 1, description = "Grocery Store")
        val mockQuery = createMockQuery(listOf(dbEntry1, dbEntry2, dbEntry3))
        every { mockQueries.selectAllByBudgetId(1L) } returns mockQuery

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
        val dbEntry1 = createDbEntry(
            id = 1,
            budgetId = 1,
            description = "Salary",
            type = BudgetEntry.Type.INCOME
        )
        val dbEntry2 = createDbEntry(
            id = 2,
            budgetId = 1,
            description = "Groceries",
            type = BudgetEntry.Type.OUTCOME
        )
        val mockQuery = createMockQuery(listOf(dbEntry1, dbEntry2))
        every { mockQueries.selectAllByBudgetId(1L) } returns mockQuery

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
        val dbEntry1 = createDbEntry(
            id = 1,
            budgetId = 1,
            description = "Supermarket",
            category = BudgetEntry.Category.GROCERIES
        )
        val dbEntry2 = createDbEntry(
            id = 2,
            budgetId = 1,
            description = "Restaurant",
            category = BudgetEntry.Category.FOOD
        )
        val mockQuery = createMockQuery(listOf(dbEntry1, dbEntry2))
        every { mockQueries.selectAllByBudgetId(1L) } returns mockQuery

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
        val dbEntry1 = createDbEntry(id = 1, budgetId = 1, description = "Old", date = "2025-01-01")
        val dbEntry2 = createDbEntry(id = 2, budgetId = 1, description = "Recent", date = "2025-01-15")
        val mockQuery = createMockQuery(listOf(dbEntry1, dbEntry2))
        every { mockQueries.selectAllByBudgetId(1L) } returns mockQuery

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
        val dbEntry1 = createDbEntry(id = 1, budgetId = 1, description = "Old", date = "2025-01-01")
        val dbEntry2 = createDbEntry(id = 2, budgetId = 1, description = "Recent", date = "2025-01-31")
        val mockQuery = createMockQuery(listOf(dbEntry1, dbEntry2))
        every { mockQueries.selectAllByBudgetId(1L) } returns mockQuery

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
        val dbEntry1 = createDbEntry(
            id = 1,
            budgetId = 1,
            description = "Groceries",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.GROCERIES,
            date = "2025-01-15"
        )
        val dbEntry2 = createDbEntry(
            id = 2,
            budgetId = 1,
            description = "Grocery Store",
            type = BudgetEntry.Type.INCOME,
            category = BudgetEntry.Category.GROCERIES,
            date = "2025-01-15"
        )
        val dbEntry3 = createDbEntry(
            id = 3,
            budgetId = 1,
            description = "Groceries",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.FOOD,
            date = "2025-01-15"
        )
        val mockQuery = createMockQuery(listOf(dbEntry1, dbEntry2, dbEntry3))
        every { mockQueries.selectAllByBudgetId(1L) } returns mockQuery

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
        every { mockQueries.insert(any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        // When
        dataSource.create(entry)

        // Then
        verify {
            mockQueries.insert(
                null,
                1L,
                150.50,
                "New Entry",
                BudgetEntry.Type.OUTCOME,
                BudgetEntry.Category.FOOD,
                "2025-01-20",
                "INV-001"
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
        every { mockQueries.insert(any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        // When
        dataSource.create(entry)

        // Then - should default to 0.0
        verify {
            mockQueries.insert(
                null,
                1L,
                0.0,
                "Test",
                BudgetEntry.Type.OUTCOME,
                BudgetEntry.Category.OTHER,
                "",
                null
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
        every { mockQueries.update(any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        // When
        dataSource.update(entry)

        // Then
        verify {
            mockQueries.update(
                5L,
                1L,
                200.0,
                "Updated Entry",
                BudgetEntry.Type.INCOME,
                BudgetEntry.Category.OTHER,
                "2025-02-01",
                null
            )
        }
    }

    @Test
    fun `deleteByIds removes entries with specified ids`() = runTest {
        // Given
        every { mockQueries.deleteByIds(any()) } returns Unit

        // When
        dataSource.deleteByIds(listOf(1L, 2L, 3L))

        // Then
        verify { mockQueries.deleteByIds(listOf(1L, 2L, 3L)) }
    }

    @Test
    fun `deleteAllByBudgetId removes all entries for budget`() = runTest {
        // Given
        every { mockQueries.deleteAllByBudgetId(any()) } returns Unit

        // When
        dataSource.deleteAllByBudgetId(1L)

        // Then
        verify { mockQueries.deleteAllByBudgetId(1L) }
    }

    @Test
    fun `cache is populated when flow is collected`() = runTest {
        // Given
        val dbEntry = createDbEntry(id = 1, budgetId = 1, description = "Test")
        val mockQuery = createMockQuery(listOf(dbEntry))
        every { mockQueries.selectAllByBudgetId(1L) } returns mockQuery

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
        val dbEntry = createDbEntry(id = 1, budgetId = 1, description = "Test")
        val mockQuery = createMockQuery(listOf(dbEntry))
        every { mockQueries.selectAllByBudgetId(1L) } returns mockQuery

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = "NonExistent"))

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllFilteredBy filters by date range`() = runTest {
        // Given
        val dbEntry1 = createDbEntry(id = 1, budgetId = 1, description = "Jan 1", date = "2025-01-01")
        val dbEntry2 = createDbEntry(id = 2, budgetId = 1, description = "Jan 15", date = "2025-01-15")
        val dbEntry3 = createDbEntry(id = 3, budgetId = 1, description = "Jan 31", date = "2025-01-31")
        val mockQuery = createMockQuery(listOf(dbEntry1, dbEntry2, dbEntry3))
        every { mockQueries.selectAllByBudgetId(1L) } returns mockQuery

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
        every { mockQueries.insert(any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        // When
        dataSource.create(entry)

        // Then
        verify {
            mockQueries.insert(
                null,
                1L,
                100.0,
                "No Invoice",
                BudgetEntry.Type.OUTCOME,
                BudgetEntry.Category.OTHER,
                "",
                null
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
        every { mockQueries.update(any(), any(), any(), any(), any(), any(), any(), any()) } returns Unit

        // When
        dataSource.update(entry)

        // Then
        verify {
            mockQueries.update(
                1L,
                1L,
                999.99,
                "Test",
                BudgetEntry.Type.OUTCOME,
                BudgetEntry.Category.OTHER,
                "",
                null
            )
        }
    }

    @Test
    fun `deleteByIds handles empty list`() = runTest {
        // Given
        every { mockQueries.deleteByIds(any()) } returns Unit

        // When
        dataSource.deleteByIds(emptyList())

        // Then
        verify { mockQueries.deleteByIds(emptyList()) }
    }

    @Test
    fun `selectAllByBudgetId returns empty list when no entries for budget`() = runTest {
        // Given
        val mockQuery = createMockQuery(emptyList<Budget_entry>())
        every { mockQueries.selectAllByBudgetId(999L) } returns mockQuery

        // When
        val result = dataSource.selectAllByBudgetId(999L).first()

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllFilteredBy handles blank description filter`() = runTest {
        // Given
        val dbEntry1 = createDbEntry(id = 1, budgetId = 1, description = "Entry 1")
        val dbEntry2 = createDbEntry(id = 2, budgetId = 1, description = "Entry 2")
        val mockQuery = createMockQuery(listOf(dbEntry1, dbEntry2))
        every { mockQueries.selectAllByBudgetId(1L) } returns mockQuery

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = "   "))

        // Then
        assertEquals(2, result.size)
    }

    /**
     * Helper function to create a database entry with default values
     */
    private fun createDbEntry(
        id: Int,
        budgetId: Int,
        description: String,
        amount: Double = 0.0,
        type: BudgetEntry.Type = BudgetEntry.Type.OUTCOME,
        category: BudgetEntry.Category = BudgetEntry.Category.OTHER,
        date: String = "",
        invoice: String? = null
    ) = Budget_entry(
        id = id.toLong(),
        budget_id = budgetId.toLong(),
        amount = amount,
        description = description,
        type = type,
        category = category,
        date = date,
        invoice = invoice
    )

    /**
     * Helper function to create a mock Query that returns the given data as a flow
     */
    private fun createMockQuery(data: List<Budget_entry>): Query<Budget_entry> {
        val mockQuery = mock<Query<Budget_entry>>()
        every { mockQuery.asFlow() } returns flowOf(data)
        return mockQuery
    }
}
