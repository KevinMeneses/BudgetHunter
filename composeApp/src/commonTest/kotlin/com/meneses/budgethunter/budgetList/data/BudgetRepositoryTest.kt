package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import dev.mokkery.answering.returns
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BudgetRepositoryTest {

    @Test
    fun `budgets property exposes flow from data source`() = runTest {
        val budgetList = listOf(
            Budget(id = 1, name = "Budget 1", amount = 100.0),
            Budget(id = 2, name = "Budget 2", amount = 200.0)
        )
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(budgetList)
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        val result = repository.budgets.first()

        assertEquals(budgetList, result)
    }

    @Test
    fun `getById returns budget when found`() = runTest {
        val budget = Budget(id = 42, name = "Test Budget", amount = 500.0)
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { getById(42) } returns budget
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        val result = repository.getById(42)

        assertEquals(budget, result)
        verifySuspend { dataSource.getById(42) }
    }

    @Test
    fun `getById returns null when budget not found`() = runTest {
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { getById(999) } returns null
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        val result = repository.getById(999)

        assertNull(result)
        verifySuspend { dataSource.getById(999) }
    }

    @Test
    fun `getAllCached returns cached budgets`() = runTest {
        val cachedBudgets = listOf(
            Budget(id = 1, name = "Cached 1", amount = 100.0),
            Budget(id = 2, name = "Cached 2", amount = 200.0)
        )
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { getAllCached() } returns cachedBudgets
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        val result = repository.getAllCached()

        assertEquals(cachedBudgets, result)
        verifySuspend { dataSource.getAllCached() }
    }

    @Test
    fun `getAllCached returns empty list when no cached budgets`() = runTest {
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { getAllCached() } returns emptyList()
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        val result = repository.getAllCached()

        assertEquals(emptyList(), result)
        verifySuspend { dataSource.getAllCached() }
    }

    @Test
    fun `getAllFilteredBy returns budgets matching filter`() = runTest {
        val filter = BudgetFilter(name = "February")
        val filteredBudgets = listOf(
            Budget(id = 2, name = "February Budget", amount = 200.0, date = "2024-02-15")
        )
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { getAllFilteredBy(filter) } returns filteredBudgets
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        val result = repository.getAllFilteredBy(filter)

        assertEquals(1, result.size)
        assertEquals("February Budget", result[0].name)
        verifySuspend { dataSource.getAllFilteredBy(filter) }
    }

    @Test
    fun `getAllFilteredBy returns all budgets when filter is empty`() = runTest {
        val allBudgets = listOf(
            Budget(id = 1, name = "Budget 1", amount = 100.0),
            Budget(id = 2, name = "Budget 2", amount = 200.0)
        )
        val filter = BudgetFilter()
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { getAllFilteredBy(filter) } returns allBudgets
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        val result = repository.getAllFilteredBy(filter)

        assertEquals(allBudgets.size, result.size)
        verifySuspend { dataSource.getAllFilteredBy(filter) }
    }

    @Test
    fun `create delegates to data source with IO dispatcher`() = runTest {
        val budget = Budget(id = -1, name = "New Budget", amount = 1000.0)
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { create(budget) } returns budget.copy(id = 1)
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        repository.create(budget)

        verifySuspend { dataSource.create(budget) }
    }

    @Test
    fun `create handles budget with zero amount`() = runTest {
        val budget = Budget(id = -1, name = "Zero Budget", amount = 0.0)
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { create(budget) } returns budget.copy(id = 1)
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        repository.create(budget)

        verifySuspend { dataSource.create(budget) }
    }

    @Test
    fun `create handles budget with empty name`() = runTest {
        val budget = Budget(id = -1, name = "", amount = 100.0)
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { create(budget) } returns budget.copy(id = 1)
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        repository.create(budget)

        verifySuspend { dataSource.create(budget) }
    }

    @Test
    fun `update delegates to data source with IO dispatcher`() = runTest {
        val budget = Budget(id = 1, name = "Updated Budget", amount = 1500.0)
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { update(budget) } returns Unit
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        repository.update(budget)

        verifySuspend { dataSource.update(budget) }
    }

    @Test
    fun `update handles multiple sequential updates`() = runTest {
        val budget1 = Budget(id = 1, name = "Budget 1", amount = 100.0)
        val budget2 = Budget(id = 2, name = "Budget 2", amount = 200.0)
        val budget3 = Budget(id = 3, name = "Budget 3", amount = 300.0)
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { update(budget1) } returns Unit
            everySuspend { update(budget2) } returns Unit
            everySuspend { update(budget3) } returns Unit
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        repository.update(budget1)
        repository.update(budget2)
        repository.update(budget3)

        verifySuspend {
            dataSource.update(budget1)
            dataSource.update(budget2)
            dataSource.update(budget3)
        }
    }

    @Test
    fun `update preserves all budget properties`() = runTest {
        val budget = Budget(
            id = 42,
            name = "Complex Budget",
            amount = 1234.56,
            totalExpenses = 567.89,
            date = "2024-06-15"
        )
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { update(budget) } returns Unit
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        repository.update(budget)

        verifySuspend { dataSource.update(budget) }
    }

    @Test
    fun `create and update can be called sequentially`() = runTest {
        val newBudget = Budget(id = -1, name = "New", amount = 100.0)
        val updatedBudget = Budget(id = 1, name = "Updated", amount = 200.0)
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { create(newBudget) } returns newBudget.copy(id = 1)
            everySuspend { update(updatedBudget) } returns Unit
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        repository.create(newBudget)
        repository.update(updatedBudget)

        verifySuspend {
            dataSource.create(newBudget)
            dataSource.update(updatedBudget)
        }
    }

    @Test
    fun `getById handles large ID values`() = runTest {
        val largeId = Int.MAX_VALUE
        val budget = Budget(id = largeId, name = "Large ID Budget", amount = 100.0)
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { getById(largeId) } returns budget
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        val result = repository.getById(largeId)

        assertEquals(budget, result)
        verifySuspend { dataSource.getById(largeId) }
    }

    @Test
    fun `getAllFilteredBy handles name filter`() = runTest {
        val filter = BudgetFilter(name = "February")
        val filteredBudgets = listOf(
            Budget(id = 2, name = "February Budget", amount = 200.0, date = "2024-02-01")
        )
        val dataSource = mock<BudgetLocalDataSource> {
            everySuspend { getAllFilteredBy(filter) } returns filteredBudgets
        }
        val repository = BudgetRepository(dataSource, Dispatchers.Default)

        val result = repository.getAllFilteredBy(filter)

        assertEquals(1, result.size)
        assertEquals("February Budget", result[0].name)
        verifySuspend { dataSource.getAllFilteredBy(filter) }
    }
}
