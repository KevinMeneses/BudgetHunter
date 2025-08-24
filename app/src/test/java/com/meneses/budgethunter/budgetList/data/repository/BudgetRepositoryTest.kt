package com.meneses.budgethunter.budgetList.data.repository

import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.commons.data.KtorRealtimeMessagingClient
import com.meneses.budgethunter.commons.data.PreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.Dispatchers
import org.junit.Assert
import org.junit.Before
import org.junit.After
import org.junit.Test
import java.time.LocalDate

@ExperimentalCoroutinesApi
class BudgetRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dataSource: BudgetLocalDataSource = mockk(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val messagingClient = mockk<() -> KtorRealtimeMessagingClient>(relaxed = true)
    private lateinit var repository: BudgetRepository

    // Test data
    private val testBudget1 = Budget(
        id = 1,
        name = "Groceries Budget",
        amount = 500.0,
        totalExpenses = 200.0,
        date = LocalDate.now().toString()
    )

    private val testBudget2 = Budget(
        id = 2,
        name = "Entertainment Budget",
        amount = 300.0,
        totalExpenses = 50.0,
        date = LocalDate.now().toString()
    )

    private val testBudgets = listOf(testBudget1, testBudget2)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = BudgetRepository(
            localDataSource = dataSource,
            preferencesManager = preferencesManager,
            ioDispatcher = testDispatcher,
            messagingClient = messagingClient
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `budgets should return flow from data source`() = runTest {
        // Given
        val budgetsFlow = flowOf(testBudgets)
        every { dataSource.budgets } returns budgetsFlow

        // When
        val result = repository.budgets

        // Then
        verify { dataSource.budgets }
        val resultList = result.toList()
        Assert.assertEquals(1, resultList.size)
        Assert.assertEquals(testBudgets, resultList[0])
    }

    @Test
    fun `getAllCached should return cached budgets from data source`() {
        // Given
        every { dataSource.getAllCached() } returns testBudgets

        // When
        val result = repository.getAllCached()

        // Then
        verify { dataSource.getAllCached() }
        Assert.assertEquals(testBudgets, result)
    }

    @Test
    fun `getAllFilteredBy should delegate to data source`() {
        // Given
        val filter = BudgetFilter("Groceries")
        val filteredBudgets = listOf(testBudget1)
        every { dataSource.getAllFilteredBy(filter) } returns filteredBudgets

        // When
        val result = repository.getAllFilteredBy(filter)

        // Then
        verify { dataSource.getAllFilteredBy(filter) }
        Assert.assertEquals(filteredBudgets, result)
    }

    @Test
    fun `getById should delegate to data source`() {
        // Given
        val budgetId = 1
        every { dataSource.getById(budgetId) } returns testBudget1

        // When
        val result = repository.getById(budgetId)

        // Then
        verify { dataSource.getById(budgetId) }
        Assert.assertEquals(testBudget1, result)
    }

    @Test
    fun `getById should return null when budget not found`() {
        // Given
        val budgetId = 999
        every { dataSource.getById(budgetId) } returns null

        // When
        val result = repository.getById(budgetId)

        // Then
        verify { dataSource.getById(budgetId) }
        Assert.assertNull(result)
    }

    @Test
    fun `create should call data source create and return created budget`() = runTest {
        // Given
        val newBudget = Budget(name = "New Budget", amount = 1000.0)
        val createdBudget = newBudget.copy(id = 3)
        every { dataSource.create(newBudget) } returns createdBudget

        // When
        val result = repository.create(newBudget)

        // Then
        verify { dataSource.create(newBudget) }
        Assert.assertEquals(3, result.id)
        Assert.assertEquals("New Budget", result.name)
        Assert.assertEquals(1000.0, result.amount, 0.001)
    }

    @Test
    fun `update should call data source update`() = runTest {
        // Given
        every { dataSource.update(testBudget1) } returns Unit

        // When
        repository.update(testBudget1)

        // Then
        verify { dataSource.update(testBudget1) }
    }

    // Note: Repository doesn't have delete method, only create and update

    @Test
    fun `joinCollaboration should call messaging client`() = runTest {
        // Given
        val collaborationCode = 12345
        val mockMessagingClient = mockk<KtorRealtimeMessagingClient>()
        every { messagingClient() } returns mockMessagingClient
        coEvery { mockMessagingClient.joinCollaboration(collaborationCode) } returns Unit

        // When
        repository.joinCollaboration(collaborationCode)

        // Then
        coVerify { mockMessagingClient.joinCollaboration(collaborationCode) }
    }

    @Test
    fun `create should handle budget with negative id correctly`() = runTest {
        // Given - new budget with negative id
        val newBudget = testBudget1.copy(id = -1)
        val createdBudget = newBudget.copy(id = 5)
        every { dataSource.create(newBudget) } returns createdBudget

        // When
        val result = repository.create(newBudget)

        // Then
        verify { dataSource.create(newBudget) }
        Assert.assertEquals(5, result.id)
    }

    @Test
    fun `update should handle budget with valid id correctly`() = runTest {
        // Given - existing budget with positive id
        val existingBudget = testBudget1.copy(name = "Updated Budget")
        every { dataSource.update(existingBudget) } returns Unit

        // When
        repository.update(existingBudget)

        // Then
        verify { dataSource.update(existingBudget) }
    }
}
