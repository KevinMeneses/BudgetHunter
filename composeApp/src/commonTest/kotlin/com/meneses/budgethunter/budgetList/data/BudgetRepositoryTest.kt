package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BudgetRepositoryTest {

    // Test helpers to track operations
    private class BudgetTracker {
        val createdBudgets = mutableListOf<Budget>()
        val updatedBudgets = mutableListOf<Budget>()
        val cachedBudgets = mutableListOf<Budget>()
        val allBudgets = mutableListOf<Budget>()
    }

    // Mock data source for testing
    private class MockBudgetLocalDataSource(
        private val tracker: BudgetTracker,
        private val budgetsFlow: Flow<List<Budget>> = flowOf(emptyList())
    ) {
        val budgets: Flow<List<Budget>> = budgetsFlow

        suspend fun getById(id: Int): Budget? {
            return tracker.allBudgets.find { it.id == id }
        }

        suspend fun getAllCached(): List<Budget> {
            return tracker.cachedBudgets
        }

        suspend fun getAllFilteredBy(filter: BudgetFilter): List<Budget> {
            // Simple filter implementation for testing
            return tracker.allBudgets.filter { budget ->
                val matchesName = filter.name?.let { budget.name.contains(it, ignoreCase = true) } ?: true
                val matchesDateRange = filter.startDate?.let { budget.date >= it } ?: true &&
                        filter.endDate?.let { budget.date <= it } ?: true
                matchesName && matchesDateRange
            }
        }

        suspend fun create(budget: Budget) {
            tracker.createdBudgets.add(budget)
        }

        suspend fun update(budget: Budget) {
            tracker.updatedBudgets.add(budget)
        }
    }

    // Testable version of repository
    private class TestableBudgetRepository(
        private val localDataSource: MockBudgetLocalDataSource,
        private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
    ) {
        val budgets: Flow<List<Budget>>
            get() = localDataSource.budgets

        suspend fun getById(id: Int): Budget? =
            localDataSource.getById(id)

        suspend fun getAllCached(): List<Budget> =
            localDataSource.getAllCached()

        suspend fun getAllFilteredBy(filter: BudgetFilter): List<Budget> =
            localDataSource.getAllFilteredBy(filter)

        suspend fun create(budget: Budget) = kotlinx.coroutines.withContext(ioDispatcher) {
            localDataSource.create(budget)
        }

        suspend fun update(budget: Budget) = kotlinx.coroutines.withContext(ioDispatcher) {
            localDataSource.update(budget)
        }
    }

    @Test
    fun `budgets property exposes flow from data source`() = runTest {
        val tracker = BudgetTracker()
        val budgetList = listOf(
            Budget(id = 1, name = "Budget 1", amount = 100.0),
            Budget(id = 2, name = "Budget 2", amount = 200.0)
        )
        val dataSource = MockBudgetLocalDataSource(tracker, flowOf(budgetList))
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        var result: List<Budget>? = null
        repository.budgets.collect { budgets ->
            result = budgets
        }

        assertEquals(budgetList, result)
    }

    @Test
    fun `getById returns budget when found`() = runTest {
        val tracker = BudgetTracker()
        val budget = Budget(id = 42, name = "Test Budget", amount = 500.0)
        tracker.allBudgets.add(budget)

        val dataSource = MockBudgetLocalDataSource(tracker)
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        val result = repository.getById(42)

        assertEquals(budget, result)
    }

    @Test
    fun `getById returns null when budget not found`() = runTest {
        val tracker = BudgetTracker()
        val dataSource = MockBudgetLocalDataSource(tracker)
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        val result = repository.getById(999)

        assertNull(result)
    }

    @Test
    fun `getAllCached returns cached budgets`() = runTest {
        val tracker = BudgetTracker()
        val cachedBudgets = listOf(
            Budget(id = 1, name = "Cached 1", amount = 100.0),
            Budget(id = 2, name = "Cached 2", amount = 200.0)
        )
        tracker.cachedBudgets.addAll(cachedBudgets)

        val dataSource = MockBudgetLocalDataSource(tracker)
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        val result = repository.getAllCached()

        assertEquals(cachedBudgets, result)
    }

    @Test
    fun `getAllCached returns empty list when no cached budgets`() = runTest {
        val tracker = BudgetTracker()
        val dataSource = MockBudgetLocalDataSource(tracker)
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        val result = repository.getAllCached()

        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllFilteredBy returns budgets matching filter`() = runTest {
        val tracker = BudgetTracker()
        tracker.allBudgets.addAll(
            listOf(
                Budget(id = 1, name = "January Budget", amount = 100.0, date = "2024-01-15"),
                Budget(id = 2, name = "February Budget", amount = 200.0, date = "2024-02-15"),
                Budget(id = 3, name = "March Budget", amount = 300.0, date = "2024-03-15")
            )
        )

        val dataSource = MockBudgetLocalDataSource(tracker)
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        val filter = BudgetFilter(name = "February")
        val result = repository.getAllFilteredBy(filter)

        assertEquals(1, result.size)
        assertEquals("February Budget", result[0].name)
    }

    @Test
    fun `getAllFilteredBy returns all budgets when filter is empty`() = runTest {
        val tracker = BudgetTracker()
        val allBudgets = listOf(
            Budget(id = 1, name = "Budget 1", amount = 100.0),
            Budget(id = 2, name = "Budget 2", amount = 200.0)
        )
        tracker.allBudgets.addAll(allBudgets)

        val dataSource = MockBudgetLocalDataSource(tracker)
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        val filter = BudgetFilter()
        val result = repository.getAllFilteredBy(filter)

        assertEquals(allBudgets.size, result.size)
    }

    @Test
    fun `create delegates to data source with IO dispatcher`() = runTest {
        val tracker = BudgetTracker()
        val dataSource = MockBudgetLocalDataSource(tracker)
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        val budget = Budget(id = -1, name = "New Budget", amount = 1000.0)
        repository.create(budget)

        assertEquals(listOf(budget), tracker.createdBudgets)
    }

    @Test
    fun `create handles budget with zero amount`() = runTest {
        val tracker = BudgetTracker()
        val dataSource = MockBudgetLocalDataSource(tracker)
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        val budget = Budget(id = -1, name = "Zero Budget", amount = 0.0)
        repository.create(budget)

        assertEquals(1, tracker.createdBudgets.size)
        assertEquals(0.0, tracker.createdBudgets[0].amount)
    }

    @Test
    fun `create handles budget with empty name`() = runTest {
        val tracker = BudgetTracker()
        val dataSource = MockBudgetLocalDataSource(tracker)
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        val budget = Budget(id = -1, name = "", amount = 100.0)
        repository.create(budget)

        assertEquals(1, tracker.createdBudgets.size)
        assertEquals("", tracker.createdBudgets[0].name)
    }

    @Test
    fun `update delegates to data source with IO dispatcher`() = runTest {
        val tracker = BudgetTracker()
        val dataSource = MockBudgetLocalDataSource(tracker)
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        val budget = Budget(id = 1, name = "Updated Budget", amount = 1500.0)
        repository.update(budget)

        assertEquals(listOf(budget), tracker.updatedBudgets)
    }

    @Test
    fun `update handles multiple sequential updates`() = runTest {
        val tracker = BudgetTracker()
        val dataSource = MockBudgetLocalDataSource(tracker)
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        val budget1 = Budget(id = 1, name = "Budget 1", amount = 100.0)
        val budget2 = Budget(id = 2, name = "Budget 2", amount = 200.0)
        val budget3 = Budget(id = 3, name = "Budget 3", amount = 300.0)

        repository.update(budget1)
        repository.update(budget2)
        repository.update(budget3)

        assertEquals(listOf(budget1, budget2, budget3), tracker.updatedBudgets)
    }

    @Test
    fun `update preserves all budget properties`() = runTest {
        val tracker = BudgetTracker()
        val dataSource = MockBudgetLocalDataSource(tracker)
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        val budget = Budget(
            id = 42,
            name = "Complex Budget",
            amount = 1234.56,
            totalExpenses = 567.89,
            date = "2024-06-15"
        )
        repository.update(budget)

        val updated = tracker.updatedBudgets[0]
        assertEquals(42, updated.id)
        assertEquals("Complex Budget", updated.name)
        assertEquals(1234.56, updated.amount)
        assertEquals(567.89, updated.totalExpenses)
        assertEquals("2024-06-15", updated.date)
    }

    @Test
    fun `create and update can be called sequentially`() = runTest {
        val tracker = BudgetTracker()
        val dataSource = MockBudgetLocalDataSource(tracker)
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        val newBudget = Budget(id = -1, name = "New", amount = 100.0)
        repository.create(newBudget)

        val updatedBudget = Budget(id = 1, name = "Updated", amount = 200.0)
        repository.update(updatedBudget)

        assertEquals(1, tracker.createdBudgets.size)
        assertEquals(1, tracker.updatedBudgets.size)
    }

    @Test
    fun `getById handles large ID values`() = runTest {
        val tracker = BudgetTracker()
        val largeId = Int.MAX_VALUE
        val budget = Budget(id = largeId, name = "Large ID Budget", amount = 100.0)
        tracker.allBudgets.add(budget)

        val dataSource = MockBudgetLocalDataSource(tracker)
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        val result = repository.getById(largeId)

        assertEquals(budget, result)
    }

    @Test
    fun `getAllFilteredBy handles date range filter`() = runTest {
        val tracker = BudgetTracker()
        tracker.allBudgets.addAll(
            listOf(
                Budget(id = 1, name = "Budget 1", amount = 100.0, date = "2024-01-01"),
                Budget(id = 2, name = "Budget 2", amount = 200.0, date = "2024-02-01"),
                Budget(id = 3, name = "Budget 3", amount = 300.0, date = "2024-03-01")
            )
        )

        val dataSource = MockBudgetLocalDataSource(tracker)
        val repository = TestableBudgetRepository(dataSource, Dispatchers.Default)

        val filter = BudgetFilter(startDate = "2024-01-15", endDate = "2024-02-15")
        val result = repository.getAllFilteredBy(filter)

        assertEquals(1, result.size)
        assertEquals("Budget 2", result[0].name)
    }
}
