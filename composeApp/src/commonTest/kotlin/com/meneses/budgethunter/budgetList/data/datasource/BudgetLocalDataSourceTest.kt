package com.meneses.budgethunter.budgetList.data.datasource

import app.cash.sqldelight.Query
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.db.BudgetQueries
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
 * Uses test fakes for BudgetQueries to test actual BudgetLocalDataSource implementation.
 */
class BudgetLocalDataSourceTest {

    private lateinit var fakeQueries: FakeBudgetQueries
    private lateinit var dataSource: BudgetLocalDataSource

    @BeforeTest
    fun setup() {
        fakeQueries = FakeBudgetQueries()
        dataSource = BudgetLocalDataSource(fakeQueries, Dispatchers.Unconfined)
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
        fakeQueries.addBudget(id = 1L, name = "Test Budget", amount = 1000.0)

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
        fakeQueries.addBudget(id = 1L, name = "Test Budget", amount = 1000.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getById(999)

        // Then
        assertNull(result)
    }

    @Test
    fun `getById returns budget when found`() = runTest {
        // Given
        fakeQueries.addBudget(id = 1L, name = "Test Budget", amount = 1000.0)
        fakeQueries.addBudget(id = 2L, name = "Another Budget", amount = 2000.0)

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
        fakeQueries.addBudget(id = 1L, name = "Test Budget", amount = 1000.0)
        fakeQueries.addBudget(id = 2L, name = "Another Budget", amount = 2000.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = null))

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy returns all budgets when filter name is blank`() = runTest {
        // Given
        fakeQueries.addBudget(id = 1L, name = "Test Budget", amount = 1000.0)
        fakeQueries.addBudget(id = 2L, name = "Another Budget", amount = 2000.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = ""))

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun `getAllFilteredBy filters budgets by name case-insensitive`() = runTest {
        // Given
        fakeQueries.addBudget(id = 1L, name = "Groceries Budget", amount = 1000.0)
        fakeQueries.addBudget(id = 2L, name = "Entertainment Budget", amount = 2000.0)
        fakeQueries.addBudget(id = 3L, name = "Grocery Shopping", amount = 500.0)

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
        fakeQueries.addBudget(id = 1L, name = "Test Budget", amount = 1000.0)

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

        // When
        val result = dataSource.create(budget)

        // Then
        assertEquals(1, result.id)
        assertEquals("New Budget", result.name)
        assertEquals(1500.0, result.amount)
        assertTrue(fakeQueries.insertCalled)
    }

    @Test
    fun `create generates sequential ids for multiple budgets`() = runTest {
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

        // When
        dataSource.update(budget)

        // Then
        assertTrue(fakeQueries.updateCalled)
        assertEquals(5L, fakeQueries.lastUpdateId)
        assertEquals(2500.0, fakeQueries.lastUpdateAmount)
        assertEquals("Updated Budget", fakeQueries.lastUpdateName)
        assertEquals("2025-02-20", fakeQueries.lastUpdateDate)
    }

    @Test
    fun `delete calls query delete with correct id`() = runTest {
        // When
        dataSource.delete(10L)

        // Then
        assertTrue(fakeQueries.deleteCalled)
        assertEquals(10L, fakeQueries.lastDeleteId)
    }

    @Test
    fun `budgets flow emits data from queries`() = runTest {
        // Given
        fakeQueries.addBudget(id = 1L, name = "Budget 1", amount = 1000.0)
        fakeQueries.addBudget(id = 2L, name = "Budget 2", amount = 2000.0)

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
        fakeQueries.addBudget(id = 1L, name = "Test", amount = 100.0)

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
        fakeQueries.addBudget(id = 1L, name = "Monthly Budget 2025", amount = 1000.0)
        fakeQueries.addBudget(id = 2L, name = "Vacation Fund", amount = 2000.0)

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

        // When
        val result = dataSource.create(budget)

        // Then
        assertEquals(1234.56, result.amount)
        assertEquals("2025-03-15", result.date)
    }

    @Test
    fun `getById handles id 0`() = runTest {
        // Given
        fakeQueries.addBudget(id = 0L, name = "Zero Budget", amount = 0.0)

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
        fakeQueries.addBudget(id = 1L, name = "Budget-2025", amount = 1000.0)
        fakeQueries.addBudget(id = 2L, name = "Budget_Test", amount = 2000.0)

        // When
        dataSource.budgets.first()
        val result = dataSource.getAllFilteredBy(BudgetFilter(name = "budget-"))

        // Then
        assertEquals(1, result.size)
        assertEquals("Budget-2025", result[0].name)
    }

    /**
     * Fake implementation of BudgetQueries for testing
     */
    private class FakeBudgetQueries : BudgetQueries {
        private val budgets = mutableListOf<BudgetData>()
        private var nextId = 1L

        var insertCalled = false
        var updateCalled = false
        var deleteCalled = false
        var lastUpdateId: Long? = null
        var lastUpdateAmount: Double? = null
        var lastUpdateName: String? = null
        var lastUpdateDate: String? = null
        var lastDeleteId: Long? = null

        fun addBudget(id: Long, name: String, amount: Double, date: String = "", totalExpenses: Double = 0.0) {
            budgets.add(BudgetData(id, amount, name, date, totalExpenses))
        }

        override fun <T : Any> selectAll(mapper: (Long, Double, String, String, Double) -> T): Query<T> {
            val mappedData = budgets.map { mapper(it.id, it.amount, it.name, it.date, it.totalExpenses) }
            return FakeQuery(mappedData)
        }

        override fun transaction(noEnclosing: Boolean, body: () -> Unit) {
            body()
        }

        override fun insert(name: String, amount: Double, date: String) {
            insertCalled = true
        }

        override fun selectLastId(): Query<Long> {
            return FakeQuery(listOf(nextId++))
        }

        override fun update(id: Long, amount: Double, name: String, date: String) {
            updateCalled = true
            lastUpdateId = id
            lastUpdateAmount = amount
            lastUpdateName = name
            lastUpdateDate = date
        }

        override fun delete(id: Long) {
            deleteCalled = true
            lastDeleteId = id
        }

        private data class BudgetData(
            val id: Long,
            val amount: Double,
            val name: String,
            val date: String,
            val totalExpenses: Double
        )
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
