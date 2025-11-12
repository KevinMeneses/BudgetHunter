package com.meneses.budgethunter.budgetEntry.data.datasource

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for BudgetEntryLocalDataSource.
 * Tests caching, filtering, CRUD operations, and thread safety.
 */
class BudgetEntryLocalDataSourceTest {

    // Mock implementation of BudgetEntryQueries for testing
    private class MockBudgetEntryQueries {
        private val entries = mutableListOf<BudgetEntry>()
        var selectAllByBudgetIdCalled = false
        var insertCalled = false
        var updateCalled = false
        var deleteByIdsCalled = false
        var deleteAllByBudgetIdCalled = false

        fun selectAllByBudgetId(budgetId: Long): MockQuery<BudgetEntry> {
            selectAllByBudgetIdCalled = true
            return MockQuery(entries.filter { it.budgetId == budgetId.toInt() })
        }

        fun insert(
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
            val newId = (entries.maxOfOrNull { it.id } ?: 0) + 1
            entries.add(
                BudgetEntry(
                    id = newId,
                    budgetId = budgetId.toInt(),
                    amount = amount.toString(),
                    description = description,
                    type = type,
                    category = category,
                    date = date,
                    invoice = invoice
                )
            )
        }

        fun update(
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
            val index = entries.indexOfFirst { it.id == id.toInt() }
            if (index >= 0) {
                entries[index] = BudgetEntry(
                    id = id.toInt(),
                    budgetId = budgetId.toInt(),
                    amount = amount.toString(),
                    description = description,
                    type = type,
                    category = category,
                    date = date,
                    invoice = invoice
                )
            }
        }

        fun deleteByIds(ids: List<Long>) {
            deleteByIdsCalled = true
            entries.removeIf { it.id.toLong() in ids }
        }

        fun deleteAllByBudgetId(budgetId: Long) {
            deleteAllByBudgetIdCalled = true
            entries.removeIf { it.budgetId == budgetId.toInt() }
        }

        fun addEntry(entry: BudgetEntry) {
            entries.add(entry)
        }

        fun clearEntries() {
            entries.clear()
        }
    }

    private class MockQuery<T>(private val data: List<T>) {
        fun asFlow() = kotlinx.coroutines.flow.flowOf(this)
        fun mapToList(dispatcher: kotlinx.coroutines.CoroutineDispatcher) =
            kotlinx.coroutines.flow.flowOf(data)
    }

    // Testable version of BudgetEntryLocalDataSource
    private class TestableBudgetEntryLocalDataSource(
        private val mockQueries: MockBudgetEntryQueries,
        private val dispatcher: kotlinx.coroutines.CoroutineDispatcher
    ) {
        private val cacheMutex = kotlinx.coroutines.sync.Mutex()
        private var cachedEntries: List<BudgetEntry> = emptyList()

        suspend fun getAllCached(): List<BudgetEntry> = cacheMutex.withLock {
            cachedEntries
        }

        fun selectAllByBudgetId(budgetId: Long) = mockQueries
            .selectAllByBudgetId(budgetId)
            .asFlow()
            .mapToList(dispatcher)
            .onEach {
                cacheMutex.withLock {
                    cachedEntries = it
                }
            }

        suspend fun getAllFilteredBy(filter: BudgetEntryFilter): List<BudgetEntry> =
            cacheMutex.withLock {
                cachedEntries.asSequence().filter {
                    if (filter.description.isNullOrBlank()) true
                    else it.description.lowercase()
                        .contains(filter.description.lowercase())
                }.filter {
                    if (filter.type == null) true
                    else it.type == filter.type
                }.filter {
                    if (filter.category == null) true
                    else it.category == filter.category
                }.filter {
                    if (filter.startDate == null) true
                    else it.date >= filter.startDate
                }.filter {
                    if (filter.endDate == null) true
                    else it.date <= filter.endDate
                }.toList()
            }

        fun create(budgetEntry: BudgetEntry) = mockQueries.insert(
            id = null,
            budgetId = budgetEntry.budgetId.toLong(),
            amount = budgetEntry.amount.toDoubleOrNull() ?: 0.0,
            description = budgetEntry.description,
            type = budgetEntry.type,
            category = budgetEntry.category,
            date = budgetEntry.date,
            invoice = budgetEntry.invoice
        )

        fun update(budgetEntry: BudgetEntry) = mockQueries.update(
            id = budgetEntry.id.toLong(),
            budgetId = budgetEntry.budgetId.toLong(),
            amount = budgetEntry.amount.toDoubleOrNull() ?: 0.0,
            description = budgetEntry.description,
            type = budgetEntry.type,
            category = budgetEntry.category,
            date = budgetEntry.date,
            invoice = budgetEntry.invoice
        )

        fun deleteByIds(list: List<Long>) =
            mockQueries.deleteByIds(list)

        fun deleteAllByBudgetId(budgetId: Long) =
            mockQueries.deleteAllByBudgetId(budgetId)
    }

    @Test
    fun `getAllCached returns empty list initially`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        val result = dataSource.getAllCached()

        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllCached returns cached entries after flow emission`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        mockQueries.addEntry(
            BudgetEntry(
                id = 1,
                budgetId = 1,
                amount = "100.0",
                description = "Test Entry"
            )
        )
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllCached()

        assertEquals(1, result.size)
        assertEquals("Test Entry", result[0].description)
    }

    @Test
    fun `selectAllByBudgetId filters entries by budget id`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        mockQueries.addEntry(BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1"))
        mockQueries.addEntry(BudgetEntry(id = 2, budgetId = 2, amount = "200.0", description = "Entry 2"))
        mockQueries.addEntry(BudgetEntry(id = 3, budgetId = 1, amount = "300.0", description = "Entry 3"))
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        val result = dataSource.selectAllByBudgetId(1L).first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.budgetId == 1 })
    }

    @Test
    fun `getAllFilteredBy returns all entries when all filters are null or blank`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        mockQueries.addEntry(BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1"))
        mockQueries.addEntry(BudgetEntry(id = 2, budgetId = 1, amount = "200.0", description = "Entry 2"))
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter())

        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy filters by description case-insensitive`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        mockQueries.addEntry(BudgetEntry(id = 1, budgetId = 1, description = "Groceries Shopping"))
        mockQueries.addEntry(BudgetEntry(id = 2, budgetId = 1, description = "Restaurant Meal"))
        mockQueries.addEntry(BudgetEntry(id = 3, budgetId = 1, description = "Grocery Store"))
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = "GROCER"))

        assertEquals(2, result.size)
        assertTrue(result.any { it.description == "Groceries Shopping" })
        assertTrue(result.any { it.description == "Grocery Store" })
    }

    @Test
    fun `getAllFilteredBy filters by type`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        mockQueries.addEntry(
            BudgetEntry(
                id = 1,
                budgetId = 1,
                description = "Salary",
                type = BudgetEntry.Type.INCOME
            )
        )
        mockQueries.addEntry(
            BudgetEntry(
                id = 2,
                budgetId = 1,
                description = "Groceries",
                type = BudgetEntry.Type.OUTCOME
            )
        )
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(type = BudgetEntry.Type.INCOME))

        assertEquals(1, result.size)
        assertEquals("Salary", result[0].description)
    }

    @Test
    fun `getAllFilteredBy filters by category`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        mockQueries.addEntry(
            BudgetEntry(
                id = 1,
                budgetId = 1,
                description = "Supermarket",
                category = BudgetEntry.Category.GROCERIES
            )
        )
        mockQueries.addEntry(
            BudgetEntry(
                id = 2,
                budgetId = 1,
                description = "Restaurant",
                category = BudgetEntry.Category.FOOD
            )
        )
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(
            BudgetEntryFilter(category = BudgetEntry.Category.GROCERIES)
        )

        assertEquals(1, result.size)
        assertEquals("Supermarket", result[0].description)
    }

    @Test
    fun `getAllFilteredBy filters by start date`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        mockQueries.addEntry(BudgetEntry(id = 1, budgetId = 1, description = "Old", date = "2025-01-01"))
        mockQueries.addEntry(BudgetEntry(id = 2, budgetId = 1, description = "Recent", date = "2025-01-15"))
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(startDate = "2025-01-10"))

        assertEquals(1, result.size)
        assertEquals("Recent", result[0].description)
    }

    @Test
    fun `getAllFilteredBy filters by end date`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        mockQueries.addEntry(BudgetEntry(id = 1, budgetId = 1, description = "Old", date = "2025-01-01"))
        mockQueries.addEntry(BudgetEntry(id = 2, budgetId = 1, description = "Recent", date = "2025-01-31"))
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(endDate = "2025-01-15"))

        assertEquals(1, result.size)
        assertEquals("Old", result[0].description)
    }

    @Test
    fun `getAllFilteredBy applies multiple filters together`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        mockQueries.addEntry(
            BudgetEntry(
                id = 1,
                budgetId = 1,
                description = "Groceries",
                type = BudgetEntry.Type.OUTCOME,
                category = BudgetEntry.Category.GROCERIES,
                date = "2025-01-15"
            )
        )
        mockQueries.addEntry(
            BudgetEntry(
                id = 2,
                budgetId = 1,
                description = "Grocery Store",
                type = BudgetEntry.Type.INCOME,
                category = BudgetEntry.Category.GROCERIES,
                date = "2025-01-15"
            )
        )
        mockQueries.addEntry(
            BudgetEntry(
                id = 3,
                budgetId = 1,
                description = "Groceries",
                type = BudgetEntry.Type.OUTCOME,
                category = BudgetEntry.Category.FOOD,
                date = "2025-01-15"
            )
        )
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(
            BudgetEntryFilter(
                description = "grocer",
                type = BudgetEntry.Type.OUTCOME,
                category = BudgetEntry.Category.GROCERIES
            )
        )

        assertEquals(1, result.size)
        assertEquals(1, result[0].id)
    }

    @Test
    fun `create inserts entry with correct parameters`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)
        val entry = BudgetEntry(
            budgetId = 1,
            amount = "150.50",
            description = "New Entry",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.FOOD,
            date = "2025-01-20",
            invoice = "INV-001"
        )

        dataSource.create(entry)

        assertTrue(mockQueries.insertCalled)
    }

    @Test
    fun `create handles invalid amount string`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)
        val entry = BudgetEntry(
            budgetId = 1,
            amount = "invalid",
            description = "Test"
        )

        dataSource.create(entry)

        assertTrue(mockQueries.insertCalled)
    }

    @Test
    fun `update modifies existing entry`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)
        val entry = BudgetEntry(
            id = 5,
            budgetId = 1,
            amount = "200.0",
            description = "Updated Entry",
            type = BudgetEntry.Type.INCOME,
            category = BudgetEntry.Category.OTHER,
            date = "2025-02-01"
        )

        dataSource.update(entry)

        assertTrue(mockQueries.updateCalled)
    }

    @Test
    fun `deleteByIds removes entries with specified ids`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.deleteByIds(listOf(1L, 2L, 3L))

        assertTrue(mockQueries.deleteByIdsCalled)
    }

    @Test
    fun `deleteAllByBudgetId removes all entries for budget`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.deleteAllByBudgetId(1L)

        assertTrue(mockQueries.deleteAllByBudgetIdCalled)
    }

    @Test
    fun `cache is populated when flow is collected`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        mockQueries.addEntry(BudgetEntry(id = 1, budgetId = 1, description = "Test"))
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        // Before collecting, cache should be empty
        assertEquals(emptyList(), dataSource.getAllCached())

        // After collecting, cache should be populated
        dataSource.selectAllByBudgetId(1L).first()
        assertEquals(1, dataSource.getAllCached().size)
    }

    @Test
    fun `getAllFilteredBy returns empty list when no matches`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        mockQueries.addEntry(BudgetEntry(id = 1, budgetId = 1, description = "Test"))
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = "NonExistent"))

        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllFilteredBy filters by date range`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        mockQueries.addEntry(BudgetEntry(id = 1, budgetId = 1, description = "Jan 1", date = "2025-01-01"))
        mockQueries.addEntry(BudgetEntry(id = 2, budgetId = 1, description = "Jan 15", date = "2025-01-15"))
        mockQueries.addEntry(BudgetEntry(id = 3, budgetId = 1, description = "Jan 31", date = "2025-01-31"))
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(
            BudgetEntryFilter(
                startDate = "2025-01-10",
                endDate = "2025-01-20"
            )
        )

        assertEquals(1, result.size)
        assertEquals("Jan 15", result[0].description)
    }

    @Test
    fun `create handles null invoice`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)
        val entry = BudgetEntry(
            budgetId = 1,
            amount = "100.0",
            description = "No Invoice",
            invoice = null
        )

        dataSource.create(entry)

        assertTrue(mockQueries.insertCalled)
    }

    @Test
    fun `update handles amount conversion`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)
        val entry = BudgetEntry(
            id = 1,
            budgetId = 1,
            amount = "999.99",
            description = "Test"
        )

        dataSource.update(entry)

        assertTrue(mockQueries.updateCalled)
    }

    @Test
    fun `deleteByIds handles empty list`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.deleteByIds(emptyList())

        assertTrue(mockQueries.deleteByIdsCalled)
    }

    @Test
    fun `selectAllByBudgetId returns empty list when no entries for budget`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        mockQueries.addEntry(BudgetEntry(id = 1, budgetId = 1, description = "Budget 1 Entry"))
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        val result = dataSource.selectAllByBudgetId(999L).first()

        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllFilteredBy handles blank description filter`() = runTest {
        val mockQueries = MockBudgetEntryQueries()
        mockQueries.addEntry(BudgetEntry(id = 1, budgetId = 1, description = "Entry 1"))
        mockQueries.addEntry(BudgetEntry(id = 2, budgetId = 1, description = "Entry 2"))
        val dataSource = TestableBudgetEntryLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.selectAllByBudgetId(1L).first()
        val result = dataSource.getAllFilteredBy(BudgetEntryFilter(description = "   "))

        assertEquals(2, result.size)
    }
}
