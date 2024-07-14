package com.meneses.budgethunter.budgetEntry.data.repository

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.toDomain
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.db.Budget_entry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class BudgetEntryRepositoryTest {

    private val dataSource: BudgetEntryLocalDataSource = mockk()
    private val repository = BudgetEntryRepository(dataSource)

    @Test
    fun getAll() {
        Assert.assertTrue(repository.getAllCached().isEmpty())
    }

    @Test
    fun getAllByBudgetId() = runTest {
        val id = 10
        val entries = flowOf(listOf(mockk<Budget_entry>(relaxed = true)))
        every { dataSource.selectAllByBudgetId(id.toLong()) } returns entries
        val actualEntries = repository.getAllByBudgetId(id)
        verify { dataSource.selectAllByBudgetId(id.toLong()) }

        entries.collect { db ->
            actualEntries.collect { domain ->
                Assert.assertEquals(db.toDomain(), domain)
            }
        }
    }

    @Test
    fun getAllFilteredByNullParams() {
        val filter = BudgetEntryFilter()
        val entries = repository.getAllFilteredBy(filter)
        Assert.assertTrue(entries.isEmpty())
    }

    @Test
    fun getAllFilteredBy() {
        val filter = BudgetEntryFilter(
            description = "description",
            type = BudgetEntry.Type.INCOME,
            startDate = "10/10/2020",
            endDate = "10/10/2022"
        )
        val entries = repository.getAllFilteredBy(filter)
        Assert.assertTrue(entries.isEmpty())
    }

    @Test
    fun create() {
        val budgetEntry = BudgetEntry(amount = "20.0")
        every { dataSource.create(any()) } returns Unit
        repository.create(budgetEntry)
        verify { dataSource.create(any()) }
    }

    @Test
    fun update() {
        val budgetEntry = BudgetEntry(amount = "20.0")
        every { dataSource.update(any()) } returns Unit
        repository.update(budgetEntry)
        verify { dataSource.update(any()) }
    }

    @Test
    fun deleteByIds() {
        every { dataSource.deleteByIds(any()) } returns Unit
        repository.deleteByIds(listOf(1, 2, 3))
        verify { dataSource.deleteByIds(any()) }
    }
}
