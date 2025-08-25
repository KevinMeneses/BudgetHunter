package com.meneses.budgethunter.budgetList

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.application.BudgetListState
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.application.DuplicateBudgetUseCase
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@ExperimentalCoroutinesApi
class BudgetListViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val repository: BudgetRepository = mockk()
    private val duplicateBudgetUseCase = mockk<DuplicateBudgetUseCase>(relaxed = true)
    private val deleteBudgetUseCase = mockk<DeleteBudgetUseCase>(relaxed = true)

    private lateinit var viewModel: BudgetListViewModel

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
        Dispatchers.setMain(dispatcher)
        // Default repository behavior
        every { repository.budgets } returns flowOf(testBudgets)
        every { repository.getAllCached() } returns testBudgets

        viewModel = BudgetListViewModel(
            budgetRepository = repository,
            duplicateBudgetUseCase = duplicateBudgetUseCase,
            deleteBudgetUseCase = deleteBudgetUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization should load budgets and update state`() = runTest(dispatcher) {
        // Given - setup is done in @Before
        val states = mutableListOf<BudgetListState>()
        val job = launch { viewModel.uiState.toList(states) }
        runCurrent()

        // Then
        verify { repository.budgets }
        val finalState = states.last()
        Assert.assertEquals(testBudgets, finalState.budgetList)
        Assert.assertFalse(finalState.isLoading)

        job.cancel()
    }

    @Test
    fun `createBudget event should create budget and navigate to it`() = runTest(dispatcher) {
        // Given
        val newBudget = Budget(name = "New Budget", amount = 1000.0)
        val createdBudget = newBudget.copy(id = 3)
        val states = mutableListOf<BudgetListState>()
        val job = launch { viewModel.uiState.toList(states) }

        coEvery { repository.create(newBudget) } returns createdBudget

        // When
        viewModel.sendEvent(BudgetListEvent.CreateBudget(newBudget))
        runCurrent()

        // Then
        coVerify { repository.create(newBudget) }
        Assert.assertEquals(createdBudget, states.last().navigateToBudget)

        job.cancel()
    }

    @Test
    fun `openBudget event should set navigation budget in state`() = runTest {
        // When
        viewModel.sendEvent(BudgetListEvent.OpenBudget(testBudget1))

        // Then
        Assert.assertEquals(testBudget1, viewModel.uiState.value.navigateToBudget)
    }

    @Test
    fun `toggleAddModal event should update modal visibility`() = runTest {
        // Initially modal should be hidden
        Assert.assertFalse(viewModel.uiState.value.addModalVisibility)

        // When - show modal
        viewModel.sendEvent(BudgetListEvent.ToggleAddModal(true))

        // Then
        Assert.assertTrue(viewModel.uiState.value.addModalVisibility)

        // When - hide modal
        viewModel.sendEvent(BudgetListEvent.ToggleAddModal(false))

        // Then
        Assert.assertFalse(viewModel.uiState.value.addModalVisibility)
    }

    @Test
    fun `clearFilter event should restore cached budgets and clear filter`() = runTest(dispatcher) {
        // Given - set up a filter first
        viewModel.sendEvent(BudgetListEvent.ToggleSearchMode(true)) // simulate having a filter

        val states = mutableListOf<BudgetListState>()
        val job = launch { viewModel.uiState.toList(states) }

        // When
        viewModel.sendEvent(BudgetListEvent.ClearFilter)
        runCurrent()

        // Then
        verify { repository.getAllCached() }
        val finalState = states.last()
        Assert.assertEquals(testBudgets, finalState.budgetList)
        Assert.assertNull(finalState.filter)

        job.cancel()
    }

    @Test
    fun `clearNavigation event should clear navigation budget`() = runTest {
        // Given - set a budget to navigate to first
        viewModel.sendEvent(BudgetListEvent.OpenBudget(testBudget1))
        Assert.assertNotNull(viewModel.uiState.value.navigateToBudget)

        // When
        viewModel.sendEvent(BudgetListEvent.ClearNavigation)

        // Then
        Assert.assertNull(viewModel.uiState.value.navigateToBudget)
    }

    @Test
    fun `updateBudget event should update budget in repository`() = runTest(dispatcher) {
        // Given
        val updatedBudget = Budget(id = 1, name = "Updated Budget", amount = 600.0)
        coEvery { repository.update(updatedBudget) } returns Unit

        // When
        viewModel.sendEvent(BudgetListEvent.UpdateBudget(updatedBudget))
        runCurrent()

        // Then
        coVerify { repository.update(updatedBudget) }
    }

    @Test
    fun `duplicateBudget event should call duplicate use case`() = runTest(dispatcher) {
        // Given
        val testBudget = Budget(id = 1, name = "Test Budget")
        coEvery { duplicateBudgetUseCase.execute(testBudget) } returns Unit

        // When
        viewModel.sendEvent(BudgetListEvent.DuplicateBudget(testBudget))
        runCurrent()

        // Then
        coVerify { duplicateBudgetUseCase.execute(testBudget) }
    }

    @Test
    fun `deleteBudget event should call delete use case`() = runTest(dispatcher) {
        // Given
        val budgetId = 1L
        coEvery { deleteBudgetUseCase.execute(budgetId) } returns Unit

        // When
        viewModel.sendEvent(BudgetListEvent.DeleteBudget(budgetId))
        runCurrent()

        // Then
        coVerify { deleteBudgetUseCase.execute(budgetId) }
    }

    @Test
    fun `toggleUpdateModal event should update budgetToUpdate in state`() = runTest {
        // Given
        val testBudget = Budget(id = 1, name = "Test Budget")

        // When - set budget to update
        viewModel.sendEvent(BudgetListEvent.ToggleUpdateModal(testBudget))

        // Then
        Assert.assertEquals(testBudget, viewModel.uiState.value.budgetToUpdate)

        // When - clear budget to update
        viewModel.sendEvent(BudgetListEvent.ToggleUpdateModal(null))

        // Then
        Assert.assertNull(viewModel.uiState.value.budgetToUpdate)
    }

    @Test
    fun `toggleSearchMode event should update search state`() = runTest(dispatcher) {
        // Given
        val states = mutableListOf<BudgetListState>()
        val job = launch { viewModel.uiState.toList(states) }

        // When - enable search mode
        viewModel.sendEvent(BudgetListEvent.ToggleSearchMode(true))
        runCurrent()

        // Then
        Assert.assertTrue(states.last().isSearchMode)

        // When - disable search mode
        viewModel.sendEvent(BudgetListEvent.ToggleSearchMode(false))
        runCurrent()

        // Then
        val finalState = states.last()
        Assert.assertFalse(finalState.isSearchMode)
        Assert.assertEquals("", finalState.searchQuery) // Should clear search query

        job.cancel()
    }

    @Test
    fun `updateSearchQuery event should filter budgets by query`() = runTest(dispatcher) {
        // Given - budgets with different names
        val budgetWithGroceries = Budget(id = 1, name = "Groceries Budget")
        val budgetWithEntertainment = Budget(id = 2, name = "Entertainment Budget")
        val allBudgets = listOf(budgetWithGroceries, budgetWithEntertainment)

        every { repository.getAllCached() } returns allBudgets

        val states = mutableListOf<BudgetListState>()
        val job = launch { viewModel.uiState.toList(states) }

        // When - search for "groceries"
        viewModel.sendEvent(BudgetListEvent.UpdateSearchQuery("groceries"))
        runCurrent()

        // Then - only groceries budget should be shown
        val filteredState = states.last()
        Assert.assertEquals("groceries", filteredState.searchQuery)
        Assert.assertEquals(1, filteredState.budgetList.size)
        Assert.assertEquals(budgetWithGroceries, filteredState.budgetList[0])

        // When - clear search query
        viewModel.sendEvent(BudgetListEvent.UpdateSearchQuery(""))
        runCurrent()

        // Then - all budgets should be shown
        val finalState = states.last()
        Assert.assertEquals("", finalState.searchQuery)
        Assert.assertEquals(allBudgets, finalState.budgetList)

        job.cancel()
    }

    @Test
    fun `search should be case insensitive`() = runTest(dispatcher) {
        // Given
        val budgetWithEntertainment = Budget(id = 2, name = "Entertainment Budget")
        val testBudgets = listOf(Budget(id = 1, name = "Groceries Budget"), budgetWithEntertainment)
        every { repository.getAllCached() } returns testBudgets

        val states = mutableListOf<BudgetListState>()
        val job = launch { viewModel.uiState.toList(states) }

        // When - search with uppercase query
        viewModel.sendEvent(BudgetListEvent.UpdateSearchQuery("ENTERTAINMENT"))
        runCurrent()

        // Then - should find the entertainment budget
        val finalState = states.last()
        Assert.assertEquals(1, finalState.budgetList.size)
        Assert.assertEquals(budgetWithEntertainment, finalState.budgetList[0])

        job.cancel()
    }
}
