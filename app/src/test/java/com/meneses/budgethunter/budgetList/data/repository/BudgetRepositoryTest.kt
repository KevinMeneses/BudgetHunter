package com.meneses.budgethunter.budgetList.data.repository

import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class BudgetRepositoryTest {
    private val dataSource: BudgetLocalDataSource = mockk(relaxed = true)
    private val repository = BudgetRepository(dataSource)

    private val budget = com.meneses.budgethunter.budgetList.domain.Budget()

    @Test
    fun getBudgets() = runTest {
        val budgets = flowOf(listOf<com.meneses.budgethunter.budgetList.domain.Budget>(mockk(relaxed = true)))
        every { dataSource.budgets } returns budgets
        val domainBudgets = repository.budgets
        verify { dataSource.budgets }

        budgets.collect { db ->
            domainBudgets.collect { domain ->
                Assert.assertEquals(db.first(), domain.first())
            }
        }
    }

    @Test
    fun getAll() {
        Assert.assertTrue(repository.getAllCached().isEmpty())
    }

    @Test
    fun getAllFilteredBy() {
        val filter = BudgetFilter("name")
        val budgets = repository.getAllFilteredBy(filter)
        Assert.assertTrue(budgets.isEmpty())
    }

    @Test
    fun getAllFilteredByNullFilter() {
        val filter = BudgetFilter("name")
        val budgets = repository.getAllFilteredBy(filter)
        Assert.assertTrue(budgets.isEmpty())
    }

    @Test
    fun create() = runTest {
        every { dataSource.create(any()) } returns mockk()
        val actualBudget = repository.create(budget)
        Assert.assertEquals(1, actualBudget.id)
        verify { dataSource.create(any()) }
    }

    @Test
    fun update() = runTest {
        every { dataSource.update(any()) } returns Unit
        repository.update(budget)
        verify { dataSource.update(any()) }
    }
}
