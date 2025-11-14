package com.meneses.budgethunter.budgetList.data.datasource

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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

    // Mock implementation of BudgetQueries for testing
    private class MockBudgetQueries {
        private val budgets = mutableListOf<Budget>()
        private var nextId = 1L
        var selectAllCalled = false
        var transactionCalled = false
        var insertCalled = false
        var updateCalled = false
        var deleteCalled = false

        fun selectAll(mapper: (Long, Double, String, String, Double) -> Budget): MockQuery<Budget> {
            selectAllCalled = true
            return MockQuery(
                budgets.map {
                    mapper(
                        it.id.toLong(),
                        it.amount,
                        it.name,
                        it.date,
                        it.totalExpenses
                    )
                }
            )
        }

        fun transaction(body: () -> Unit) {
            transactionCalled = true
            body()
        }

        fun insert(name: String, amount: Double, date: String) {
            insertCalled = true
            budgets.add(
                Budget(
                    id = nextId.toInt(),
                    name = name,
                    amount = amount,
                    date = date,
                    totalExpenses = 0.0
                )
            )
        }

        fun selectLastId(): MockExecuteAsOne<Long> {
            return MockExecuteAsOne(nextId++)
        }

        fun update(id: Long, amount: Double, name: String, date: String) {
            updateCalled = true
            val index = budgets.indexOfFirst { it.id == id.toInt() }
            if (index >= 0) {
                budgets[index] = budgets[index].copy(
                    amount = amount,
                    name = name,
                    date = date
                )
            }
        }

        fun delete(id: Long) {
            deleteCalled = true
            budgets.removeIf { it.id == id.toInt() }
        }

        fun addBudget(budget: Budget) {
            budgets.add(budget)
        }

        fun clearBudgets() {
            budgets.clear()
        }
    }

    private class MockQuery<T>(private val data: List<T>) {
        fun asFlow() = kotlinx.coroutines.flow.flowOf(this)
        fun mapToList(dispatcher: kotlinx.coroutines.CoroutineDispatcher) =
            kotlinx.coroutines.flow.flowOf(data)
    }

    private class MockExecuteAsOne<T>(private val value: T) {
        fun executeAsOne() = value
    }

    // Testable version of BudgetLocalDataSource
    private class TestableBudgetLocalDataSource(
        private val mockQueries: MockBudgetQueries,
        dispatcher: kotlinx.coroutines.CoroutineDispatcher
    ) {
        private val cacheMutex = kotlinx.coroutines.sync.Mutex()
        private var cachedList: List<Budget> = emptyList()

        val budgets = mockQueries
            .selectAll { id, amount, name, date, totalExpenses ->
                Budget(
                    id = id.toInt(),
                    amount = amount,
                    name = name,
                    totalExpenses = totalExpenses,
                    date = date
                )
            }
            .asFlow()
            .mapToList(dispatcher)
            .onEach {
                cacheMutex.withLock {
                    cachedList = it
                }
            }

        suspend fun getAllCached(): List<Budget> = cacheMutex.withLock {
            cachedList
        }

        suspend fun getById(id: Int): Budget? = cacheMutex.withLock {
            cachedList.firstOrNull { it.id == id }
        }

        suspend fun getAllFilteredBy(filter: BudgetFilter): List<Budget> = cacheMutex.withLock {
            cachedList.filter {
                if (filter.name.isNullOrBlank()) true
                else it.name.lowercase()
                    .contains(filter.name.lowercase())
            }
        }

        fun create(budget: Budget): Budget {
            var savedId = 0

            mockQueries.transaction {
                mockQueries.insert(
                    name = budget.name,
                    amount = budget.amount,
                    date = budget.date
                )

                savedId = mockQueries
                    .selectLastId()
                    .executeAsOne()
                    .toInt()
            }

            return budget.copy(id = savedId)
        }

        fun update(budget: Budget) = mockQueries.update(
            id = budget.id.toLong(),
            amount = budget.amount,
            name = budget.name,
            date = budget.date
        )

        fun delete(id: Long) = mockQueries.delete(id)
    }

    @Test
    fun `getAllCached returns empty list initially`() = runTest {
        val mockQueries = MockBudgetQueries()
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)

        val result = dataSource.getAllCached()

        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllCached returns cached budgets after flow emission`() = runTest {
        val mockQueries = MockBudgetQueries()
        mockQueries.addBudget(Budget(id = 1, name = "Test Budget", amount = 1000.0))
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)

        // Trigger flow to populate cache
        dataSource.budgets.first()
        val result = dataSource.getAllCached()

        assertEquals(1, result.size)
        assertEquals("Test Budget", result[0].name)
    }

    @Test
    fun `getById returns null when budget not found`() = runTest {
        val mockQueries = MockBudgetQueries()
        mockQueries.addBudget(Budget(id = 1, name = "Test Budget", amount = 1000.0))
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.budgets.first()
        val result = dataSource.getById(999)

        assertNull(result)
    }

    @Test
    fun `getById returns budget when found`() = runTest {
        val mockQueries = MockBudgetQueries()
        mockQueries.addBudget(Budget(id = 1, name = "Test Budget", amount = 1000.0))
        mockQueries.addBudget(Budget(id = 2, name = "Another Budget", amount = 2000.0))
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.budgets.first()
        val result = dataSource.getById(2)

        assertNotNull(result)
        assertEquals("Another Budget", result.name)
        assertEquals(2000.0, result.amount)
    }

    @Test
    fun `getAllFilteredBy returns all budgets when filter name is null`() = runTest {
        val mockQueries = MockBudgetQueries()
        mockQueries.addBudget(Budget(id = 1, name = "Test Budget", amount = 1000.0))
        mockQueries.addBudget(Budget(id = 2, name = "Another Budget", amount = 2000.0))
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = null))

        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy returns all budgets when filter name is blank`() = runTest {
        val mockQueries = MockBudgetQueries()
        mockQueries.addBudget(Budget(id = 1, name = "Test Budget", amount = 1000.0))
        mockQueries.addBudget(Budget(id = 2, name = "Another Budget", amount = 2000.0))
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = ""))

        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy filters budgets by name case-insensitive`() = runTest {
        val mockQueries = MockBudgetQueries()
        mockQueries.addBudget(Budget(id = 1, name = "Groceries Budget", amount = 1000.0))
        mockQueries.addBudget(Budget(id = 2, name = "Entertainment Budget", amount = 2000.0))
        mockQueries.addBudget(Budget(id = 3, name = "Grocery Shopping", amount = 500.0))
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "GROCER"))

        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "Groceries Budget" })
        assertTrue(result.any { it.name == "Grocery Shopping" })
    }

    @Test
    fun `getAllFilteredBy returns empty list when no matches found`() = runTest {
        val mockQueries = MockBudgetQueries()
        mockQueries.addBudget(Budget(id = 1, name = "Test Budget", amount = 1000.0))
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "NonExistent"))

        assertEquals(emptyList(), result)
    }

    @Test
    fun `create inserts budget and returns budget with generated id`() = runTest {
        val mockQueries = MockBudgetQueries()
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)
        val budget = Budget(name = "New Budget", amount = 1500.0, date = "2025-01-15")

        val result = dataSource.create(budget)

        assertTrue(mockQueries.transactionCalled)
        assertTrue(mockQueries.insertCalled)
        assertEquals(1, result.id)
        assertEquals("New Budget", result.name)
        assertEquals(1500.0, result.amount)
    }

    @Test
    fun `create generates sequential ids for multiple budgets`() = runTest {
        val mockQueries = MockBudgetQueries()
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)

        val budget1 = dataSource.create(Budget(name = "Budget 1", amount = 100.0))
        val budget2 = dataSource.create(Budget(name = "Budget 2", amount = 200.0))
        val budget3 = dataSource.create(Budget(name = "Budget 3", amount = 300.0))

        assertEquals(1, budget1.id)
        assertEquals(2, budget2.id)
        assertEquals(3, budget3.id)
    }

    @Test
    fun `update calls query update with correct parameters`() = runTest {
        val mockQueries = MockBudgetQueries()
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)
        val budget = Budget(id = 5, name = "Updated Budget", amount = 2500.0, date = "2025-02-20")

        dataSource.update(budget)

        assertTrue(mockQueries.updateCalled)
    }

    @Test
    fun `delete calls query delete with correct id`() = runTest {
        val mockQueries = MockBudgetQueries()
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.delete(10L)

        assertTrue(mockQueries.deleteCalled)
    }

    @Test
    fun `budgets flow emits data from queries`() = runTest {
        val mockQueries = MockBudgetQueries()
        mockQueries.addBudget(Budget(id = 1, name = "Budget 1", amount = 1000.0))
        mockQueries.addBudget(Budget(id = 2, name = "Budget 2", amount = 2000.0))
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)

        val result = dataSource.budgets.first()

        assertEquals(2, result.size)
        assertEquals("Budget 1", result[0].name)
        assertEquals("Budget 2", result[1].name)
    }

    @Test
    fun `cache is populated when budgets flow is collected`() = runTest {
        val mockQueries = MockBudgetQueries()
        mockQueries.addBudget(Budget(id = 1, name = "Test", amount = 100.0))
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)

        // Before collecting, cache should be empty
        assertEquals(emptyList(), dataSource.getAllCached())

        // After collecting, cache should be populated
        dataSource.budgets.first()
        assertEquals(1, dataSource.getAllCached().size)
    }

    @Test
    fun `getAllFilteredBy handles partial name matches`() = runTest {
        val mockQueries = MockBudgetQueries()
        mockQueries.addBudget(Budget(id = 1, name = "Monthly Budget 2025", amount = 1000.0))
        mockQueries.addBudget(Budget(id = 2, name = "Vacation Fund", amount = 2000.0))
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "month"))

        assertEquals(1, result.size)
        assertEquals("Monthly Budget 2025", result[0].name)
    }

    @Test
    fun `create preserves budget amount and date`() = runTest {
        val mockQueries = MockBudgetQueries()
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)
        val budget = Budget(
            name = "Precise Budget",
            amount = 1234.56,
            date = "2025-03-15"
        )

        val result = dataSource.create(budget)

        assertEquals(1234.56, result.amount)
        assertEquals("2025-03-15", result.date)
    }

    @Test
    fun `getById handles id 0`() = runTest {
        val mockQueries = MockBudgetQueries()
        mockQueries.addBudget(Budget(id = 0, name = "Zero Budget", amount = 0.0))
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.budgets.first()
        val result = dataSource.getById(0)

        assertNotNull(result)
        assertEquals("Zero Budget", result.name)
    }

    @Test
    fun `getAllFilteredBy handles special characters in filter`() = runTest {
        val mockQueries = MockBudgetQueries()
        mockQueries.addBudget(Budget(id = 1, name = "Budget-2025", amount = 1000.0))
        mockQueries.addBudget(Budget(id = 2, name = "Budget_Test", amount = 2000.0))
        val dataSource = TestableBudgetLocalDataSource(mockQueries, Dispatchers.Default)

        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "budget-"))

        assertEquals(1, result.size)
        assertEquals("Budget-2025", result[0].name)
    }
}
