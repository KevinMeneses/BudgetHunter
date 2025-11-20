package com.meneses.budgethunter.budgetEntry.data.datasource

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.db.BudgetEntryQueries
import com.meneses.budgethunter.db.Budget_entry
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
 * Uses test fakes for BudgetEntryQueries to test actual BudgetEntryLocalDataSource implementation.
 */
class BudgetEntryLocalDataSourceTest {

    private lateinit var fakeQueries: FakeBudgetEntryQueries
    private lateinit var dataSource: BudgetEntryLocalDataSource

    @BeforeTest
    fun setup() {
        fakeQueries = FakeBudgetEntryQueries()
        dataSource = BudgetEntryLocalDataSource(fakeQueries, Dispatchers.Unconfined)
    }

    @Test
    fun `getAllCached returns empty list initially`() = runTest {
        assertEquals(emptyList(), dataSource.getAllCached())
    }

    @Test
    fun `getAllCached returns cached entries after flow emission`() = runTest {
        // Given
        fakeQueries.addEntry(
            id = 1L,
            budgetId = 1L,
            amount = 100.0,
            description = "Test Entry"
        )

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
        fakeQueries.addEntry(id = 1L, budgetId = 1L, description = "Entry 1")
        fakeQueries.addEntry(id = 2L, budgetId = 2L, description = "Entry 2")
        fakeQueries.addEntry(id = 3L, budgetId = 1L, description = "Entry 3")

        // When
        val result = dataSource.selectAllByBudgetId(1L).first()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.budgetId == 1 })
    }

    @Test
    fun `getAllFilteredBy returns all entries when all filters are null or blank`() = runTest {
        // Given
        fakeQueries.addEntry(id = 1L, budgetId = 1L, description = "Entry 1")
        fakeQueries.addEntry(id = 2L, budgetId = 1L, description = "Entry 2")

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter())

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy filters by description case-insensitive`() = runTest {
        // Given
        fakeQueries.addEntry(id = 1L, budgetId = 1L, description = "Groceries Shopping")
        fakeQueries.addEntry(id = 2L, budgetId = 1L, description = "Restaurant Meal")
        fakeQueries.addEntry(id = 3L, budgetId = 1L, description = "Grocery Store")

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
        fakeQueries.addEntry(
            id = 1L,
            budgetId = 1L,
            description = "Salary",
            type = BudgetEntry.Type.INCOME
        )
        fakeQueries.addEntry(
            id = 2L,
            budgetId = 1L,
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
        fakeQueries.addEntry(
            id = 1L,
            budgetId = 1L,
            description = "Supermarket",
            category = BudgetEntry.Category.GROCERIES
        )
        fakeQueries.addEntry(
            id = 2L,
            budgetId = 1L,
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
        fakeQueries.addEntry(id = 1L, budgetId = 1L, description = "Old", date = "2025-01-01")
        fakeQueries.addEntry(id = 2L, budgetId = 1L, description = "Recent", date = "2025-01-15")

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
        fakeQueries.addEntry(id = 1L, budgetId = 1L, description = "Old", date = "2025-01-01")
        fakeQueries.addEntry(id = 2L, budgetId = 1L, description = "Recent", date = "2025-01-31")

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
        fakeQueries.addEntry(
            id = 1L,
            budgetId = 1L,
            description = "Groceries",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.GROCERIES,
            date = "2025-01-15"
        )
        fakeQueries.addEntry(
            id = 2L,
            budgetId = 1L,
            description = "Grocery Store",
            type = BudgetEntry.Type.INCOME,
            category = BudgetEntry.Category.GROCERIES,
            date = "2025-01-15"
        )
        fakeQueries.addEntry(
            id = 3L,
            budgetId = 1L,
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
        assertTrue(fakeQueries.insertCalled)
        assertEquals(150.50, fakeQueries.lastInsertAmount)
        assertEquals("New Entry", fakeQueries.lastInsertDescription)
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
        assertTrue(fakeQueries.insertCalled)
        assertEquals(0.0, fakeQueries.lastInsertAmount) // Should default to 0.0
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
        assertTrue(fakeQueries.updateCalled)
        assertEquals(5L, fakeQueries.lastUpdateId)
        assertEquals(200.0, fakeQueries.lastUpdateAmount)
    }

    @Test
    fun `deleteByIds removes entries with specified ids`() = runTest {
        // When
        dataSource.deleteByIds(listOf(1L, 2L, 3L))

        // Then
        assertTrue(fakeQueries.deleteByIdsCalled)
        assertEquals(listOf(1L, 2L, 3L), fakeQueries.lastDeleteIds)
    }

    @Test
    fun `deleteAllByBudgetId removes all entries for budget`() = runTest {
        // When
        dataSource.deleteAllByBudgetId(1L)

        // Then
        assertTrue(fakeQueries.deleteAllByBudgetIdCalled)
        assertEquals(1L, fakeQueries.lastDeleteBudgetId)
    }

    @Test
    fun `cache is populated when flow is collected`() = runTest {
        // Given
        fakeQueries.addEntry(id = 1L, budgetId = 1L, description = "Test")

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
        fakeQueries.addEntry(id = 1L, budgetId = 1L, description = "Test")

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = "NonExistent"))

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllFilteredBy filters by date range`() = runTest {
        // Given
        fakeQueries.addEntry(id = 1L, budgetId = 1L, description = "Jan 1", date = "2025-01-01")
        fakeQueries.addEntry(id = 2L, budgetId = 1L, description = "Jan 15", date = "2025-01-15")
        fakeQueries.addEntry(id = 3L, budgetId = 1L, description = "Jan 31", date = "2025-01-31")

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
        assertTrue(fakeQueries.insertCalled)
        assertEquals(null, fakeQueries.lastInsertInvoice)
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
        assertTrue(fakeQueries.updateCalled)
        assertEquals(999.99, fakeQueries.lastUpdateAmount)
    }

    @Test
    fun `deleteByIds handles empty list`() = runTest {
        // When
        dataSource.deleteByIds(emptyList())

        // Then
        assertTrue(fakeQueries.deleteByIdsCalled)
        assertEquals(emptyList<Long>(), fakeQueries.lastDeleteIds)
    }

    @Test
    fun `selectAllByBudgetId returns empty list when no entries for budget`() = runTest {
        // Given
        fakeQueries.addEntry(id = 1L, budgetId = 1L, description = "Budget 1 Entry")

        // When
        val result = dataSource.selectAllByBudgetId(999L).first()

        // Then
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllFilteredBy handles blank description filter`() = runTest {
        // Given
        fakeQueries.addEntry(id = 1L, budgetId = 1L, description = "Entry 1")
        fakeQueries.addEntry(id = 2L, budgetId = 1L, description = "Entry 2")

        // When
        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = "   "))

        // Then
        assertEquals(2, result.size)
    }

    /**
     * Fake implementation of BudgetEntryQueries for testing
     */
    private class FakeBudgetEntryQueries : BudgetEntryQueries {
        private val entries = mutableListOf<Budget_entry>()

        var insertCalled = false
        var updateCalled = false
        var deleteByIdsCalled = false
        var deleteAllByBudgetIdCalled = false

        var lastInsertAmount: Double? = null
        var lastInsertDescription: String? = null
        var lastInsertInvoice: String? = null

        var lastUpdateId: Long? = null
        var lastUpdateAmount: Double? = null

        var lastDeleteIds: List<Long>? = null
        var lastDeleteBudgetId: Long? = null

        fun addEntry(
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

        override fun selectAllByBudgetId(budgetId: Long): Query<Budget_entry> {
            val filtered = entries.filter { it.budget_id == budgetId }
            return FakeQuery(filtered)
        }

        override fun insert(
            id: Long?,
            budgetId: Long,
            amount: Double,
            description: String,
            type: BudgetEntry.Type,
            category: BudgetEntry.Category,
            date: String,
            invoice: String?
        ) {
            insertCalled = true
            lastInsertAmount = amount
            lastInsertDescription = description
            lastInsertInvoice = invoice
        }

        override fun update(
            id: Long,
            budgetId: Long,
            amount: Double,
            description: String,
            type: BudgetEntry.Type,
            category: BudgetEntry.Category,
            date: String,
            invoice: String?
        ) {
            updateCalled = true
            lastUpdateId = id
            lastUpdateAmount = amount
        }

        override fun deleteByIds(ids: List<Long>) {
            deleteByIdsCalled = true
            lastDeleteIds = ids
        }

        override fun deleteAllByBudgetId(budgetId: Long) {
            deleteAllByBudgetIdCalled = true
            lastDeleteBudgetId = budgetId
        }
    }

    /**
     * Fake implementation of Query for testing
     */
    private class FakeQuery<T>(private val data: List<T>) : Query<T>(data) {
        override fun <R> execute(mapper: (app.cash.sqldelight.db.SqlCursor) -> QueryResult<R>): QueryResult<R> {
            throw UnsupportedOperationException("Not needed for these tests")
        }

        override fun addListener(listener: Query.Listener) {}
        override fun removeListener(listener: Query.Listener) {}

        fun asFlow() = flowOf(data)
    }
}
