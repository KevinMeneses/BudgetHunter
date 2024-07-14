package com.meneses.budgethunter.budgetList

import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.application.BudgetListState
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

@ExperimentalCoroutinesApi
class BudgetListViewModelTest {

    private val budgetsMock = listOf<Budget>(mockk())

    private val repository: BudgetRepository = mockk {
        every { budgets } returns flowOf(budgetsMock)
    }

    private val dispatcher = StandardTestDispatcher()

    private val viewModel by lazy {
        BudgetListViewModel(
            budgetRepository = repository,
            dispatcher = dispatcher
        )
    }

    @Test
    fun initialize() = runTest(dispatcher) {
        val state = mutableListOf<BudgetListState>()
        val job = launch { viewModel.uiState.toList(state) }
        runCurrent()
        Assert.assertEquals(budgetsMock, state.last().budgetList)
        verify { repository.budgets }
        job.cancel()
    }

    @Test
    fun sendCreateBudgetEvent() = runTest(dispatcher) {
        val budget = Budget()
        val state = mutableListOf<BudgetListState>()
        val job = launch { viewModel.uiState.toList(state) }

        every { repository.create(budget) } returns budget
        viewModel.sendEvent(BudgetListEvent.CreateBudget(budget))
        runCurrent()

        Assert.assertEquals(budget, state.last().navigateToBudget)
        verify { repository.create(budget) }
        job.cancel()
    }

    @Test
    fun sendFilterListEvent() = runTest(dispatcher) {
        val budgetFilter = BudgetFilter()
        val state = mutableListOf<BudgetListState>()
        val job = launch { viewModel.uiState.toList(state) }

        every { repository.getAllFilteredBy(budgetFilter) } returns budgetsMock
        viewModel.sendEvent(BudgetListEvent.FilterList(budgetFilter))
        runCurrent()

        Assert.assertEquals(budgetsMock, state.last().budgetList)
        Assert.assertNull(state.first().filter)
        Assert.assertEquals(budgetFilter, state.last().filter)

        verify { repository.getAllFilteredBy(budgetFilter) }
        job.cancel()
    }

    @Test
    fun sendOpenBudgetEvent() = runTest {
        val budget = Budget()
        viewModel.sendEvent(BudgetListEvent.OpenBudget(budget))
        val state = viewModel.uiState.value
        Assert.assertEquals(budget, state.navigateToBudget)
    }

    @Test
    fun sendToggleAddModalEvent() = runTest {
        val state = viewModel.uiState.value
        Assert.assertFalse(state.addModalVisibility)
        viewModel.sendEvent(BudgetListEvent.ToggleAddModal(true))
        val state2 = viewModel.uiState.value
        Assert.assertTrue(state2.addModalVisibility)
    }

    @Test
    fun sendToggleFilterModalEvent() = runTest {
        val state = viewModel.uiState.value
        Assert.assertFalse(state.filterModalVisibility)
        viewModel.sendEvent(BudgetListEvent.ToggleFilterModal(true))
        val state2 = viewModel.uiState.value
        Assert.assertTrue(state2.filterModalVisibility)
    }

    @Test
    fun sendClearFilterEvent() = runTest(dispatcher) {
        val state = mutableListOf<BudgetListState>()
        val job = launch { viewModel.uiState.toList(state) }

        every { repository.getAllCached() } returns budgetsMock
        viewModel.sendEvent(BudgetListEvent.ClearFilter)
        runCurrent()

        Assert.assertEquals(budgetsMock, state.last().budgetList)
        Assert.assertNull(state.first().filter)

        verify { repository.getAllCached() }
        job.cancel()
    }

    @Test
    fun sendClearNavigationEvent() = runTest {
        viewModel.sendEvent(BudgetListEvent.ClearNavigation)
        val state = viewModel.uiState.value
        Assert.assertNull(state.navigateToBudget)
    }
}
