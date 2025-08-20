package com.meneses.budgethunter.budgetEntry.data.repository

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class BudgetEntryRepositoryTest {

    private val dataSource: BudgetEntryLocalDataSource = mockk(relaxed = true)
    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: BudgetEntryRepository

    // Test data
    private val testEntry1 = BudgetEntry(
        id = 1,
        budgetId = 1,
        amount = "20.0",
        description = "Groceries",
        type = BudgetEntry.Type.OUTCOME,
        category = BudgetEntry.Category.FOOD
    )

    private val testEntry2 = BudgetEntry(
        id = 2,
        budgetId = 1,
        amount = "500.0",
        description = "Salary",
        type = BudgetEntry.Type.INCOME,
        category = BudgetEntry.Category.OTHER
    )

    private val testEntries = listOf(testEntry1, testEntry2)

    @Before
    fun setup() {
        repository = BudgetEntryRepository(dataSource, dispatcher)
    }

    @Test
    fun `getAllByBudgetId should return flow of entries from data source`() = runTest {
        // Given
        val budgetId = 1L
        val entriesFlow = flowOf(testEntries)
        every { dataSource.selectAllByBudgetId(budgetId) } returns entriesFlow

        // When
        val result = repository.getAllByBudgetId(budgetId)

        // Then
        verify { dataSource.selectAllByBudgetId(budgetId) }
        val resultList = result.toList()
        Assert.assertEquals(1, resultList.size)
        Assert.assertEquals(testEntries, resultList[0])
    }

    @Test
    fun `create should call data source create with correct entry`() = runTest(dispatcher) {
        // Given
        every { dataSource.create(testEntry1) } returns Unit

        // When
        repository.create(testEntry1)

        // Then
        verify { dataSource.create(testEntry1) }
    }

    @Test
    fun `update should call data source update with correct entry`() = runTest(dispatcher) {
        // Given
        every { dataSource.update(testEntry1) } returns Unit

        // When
        repository.update(testEntry1)

        // Then
        verify { dataSource.update(testEntry1) }
    }

    // Note: Repository only has basic CRUD operations - deleteByIds, getAllCached, and getAllFilteredBy
    // are not in the current repository interface, so these tests are removed

    @Test
    fun `create should handle entry with negative id correctly`() = runTest(dispatcher) {
        // Given - new entry with negative id
        val newEntry = testEntry1.copy(id = -1)
        every { dataSource.create(newEntry) } returns Unit

        // When
        repository.create(newEntry)

        // Then
        verify { dataSource.create(newEntry) }
    }

    @Test
    fun `update should handle entry with valid id correctly`() = runTest(dispatcher) {
        // Given - existing entry with positive id
        val existingEntry = testEntry1.copy(id = 5)
        every { dataSource.update(existingEntry) } returns Unit

        // When
        repository.update(existingEntry)

        // Then
        verify { dataSource.update(existingEntry) }
    }
}
