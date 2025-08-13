package com.meneses.budgethunter.budgetEntry.data.repository

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
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
    fun getAllByBudgetId() = runTest {
        val id = 10L
        val entries = flowOf(listOf(mockk<BudgetEntry>(relaxed = true)))
        every { dataSource.selectAllByBudgetId(id) } returns entries
        val actualEntries = repository.getAllByBudgetId(id)
        verify { dataSource.selectAllByBudgetId(id) }

        entries.collect { db ->
            actualEntries.collect { domain ->
                Assert.assertEquals(db, domain)
            }
        }
    }

    @Test
    fun create() = runTest {
        val budgetEntry = BudgetEntry(amount = "20.0")
        every { dataSource.create(any()) } returns Unit
        repository.create(budgetEntry)
        verify { dataSource.create(any()) }
    }

    @Test
    fun update() = runTest {
        val budgetEntry = BudgetEntry(amount = "20.0")
        every { dataSource.update(any()) } returns Unit
        repository.update(budgetEntry)
        verify { dataSource.update(any()) }
    }
}
