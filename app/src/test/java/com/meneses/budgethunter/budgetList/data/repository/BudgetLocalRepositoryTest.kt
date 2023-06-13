package com.meneses.budgethunter.budgetList.data.repository

import com.meneses.budgethunter.budgetList.data.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.data.toDomain
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.db.Budget
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class BudgetLocalRepositoryTest {
    private val dataSource: BudgetLocalDataSource = mockk(relaxed = true)
    private val repository = BudgetLocalRepository(dataSource)

    private val budget = com.meneses.budgethunter.budgetList.domain.Budget()

    @Test
    fun getBudgets() = runTest {
        val budgets = flowOf(listOf<Budget>(mockk(relaxed = true)))
        every { dataSource.budgets } returns budgets
        val domainBudgets = repository.budgets
        verify { dataSource.budgets }

        budgets.collect { db ->
            domainBudgets.collect { domain ->
                Assert.assertEquals(db.first().toDomain(), domain.first())
            }
        }
    }

    @Test
    fun getAll() {
        Assert.assertTrue(repository.getAll().isEmpty())
    }

    @Test
    fun getAllFilteredBy() {
        val filter = BudgetFilter("name", com.meneses.budgethunter.budgetList.domain.Budget.Frequency.UNIQUE)
        val budgets = repository.getAllFilteredBy(filter)
        Assert.assertTrue(budgets.isEmpty())
    }

    @Test
    fun getAllFilteredByNullFilter() {
        val filter = BudgetFilter("name", com.meneses.budgethunter.budgetList.domain.Budget.Frequency.UNIQUE)
        val budgets = repository.getAllFilteredBy(filter)
        Assert.assertTrue(budgets.isEmpty())
    }

    @Test
    fun create() {
        every { dataSource.insert(any()) } returns Unit
        every { dataSource.selectLastId() } returns 1
        val actualBudget = repository.create(budget)
        Assert.assertEquals(1, actualBudget.id)
        verify {
            dataSource.insert(any())
            dataSource.selectLastId()
        }
    }

    @Test
    fun update() {
        every { dataSource.update(any()) } returns Unit
        repository.update(budget)
        verify { dataSource.update(any()) }
    }

    @Test
    fun delete() {
        every { dataSource.delete(any()) } returns Unit
        repository.delete(budget)
        verify { dataSource.delete(any()) }
    }
}
