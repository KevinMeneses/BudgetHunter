package com.meneses.budgethunter.budgetDetail

import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetEntry.data.repository.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.repository.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
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
class BudgetDetailViewModelTest {

    private val budgetEntryRepository: BudgetEntryRepository = mockk()
    private val budgetRepository: BudgetRepository = mockk()
    private val dispatcher = StandardTestDispatcher()

    private val viewModel = BudgetDetailViewModel(
        budgetEntryRepository = budgetEntryRepository,
        budgetRepository = budgetRepository,
        dispatcher = dispatcher
    )

    @Test
    fun setBudgetEvent() = runTest {
        val budget: Budget = mockk()
        val event = BudgetDetailEvent.SetBudget(budget)
        viewModel.sendEvent(event)
        val state = viewModel.uiState.value
        Assert.assertEquals(budget, state.budget)
    }

    @Test
    fun getBudgetEntriesEvent() = runTest(dispatcher) {
        val event = BudgetDetailEvent.GetBudgetEntries
        val events = mutableListOf<BudgetDetailState>()
        val budgetEntries: List<BudgetEntry> = listOf(BudgetEntry())

        every { budgetEntryRepository.getAllByBudgetId(any()) } returns flowOf(budgetEntries)
        val job = launch { viewModel.uiState.toList(events) }
        viewModel.sendEvent(event)
        runCurrent()

        Assert.assertEquals(budgetEntries, events.last().entries)
        verify { budgetEntryRepository.getAllByBudgetId(any()) }
        job.cancel()
    }

    @Test
    fun updateBudgetAmountEvent() = runTest(dispatcher) {
        val amount = 20.0
        val event = BudgetDetailEvent.UpdateBudgetAmount(amount)
        val events = mutableListOf<BudgetDetailState>()

        every { budgetRepository.update(any()) } returns Unit
        val job = launch { viewModel.uiState.toList(events) }
        viewModel.sendEvent(event)
        runCurrent()

        Assert.assertEquals(amount, events.last().budget.amount, 0.0)
        verify { budgetRepository.update(any()) }
        job.cancel()
    }

    @Test
    fun filterEntriesEvent() = runTest(dispatcher) {
        val event = BudgetDetailEvent.FilterEntries(mockk())
        val events = mutableListOf<BudgetDetailState>()
        val budgetEntries: List<BudgetEntry> = listOf(mockk())

        every { budgetEntryRepository.getAllFilteredBy(any()) } returns budgetEntries
        val job = launch { viewModel.uiState.toList(events) }
        viewModel.sendEvent(event)
        runCurrent()

        Assert.assertEquals(budgetEntries, events.last().entries)
        verify { budgetEntryRepository.getAllFilteredBy(any()) }
        job.cancel()
    }

    @Test
    fun clearFilterEvent() = runTest(dispatcher) {
        val event = BudgetDetailEvent.ClearFilter
        val events = mutableListOf<BudgetDetailState>()
        val budgetEntries: List<BudgetEntry> = listOf(mockk())

        every { budgetEntryRepository.getAll() } returns budgetEntries
        val job = launch { viewModel.uiState.toList(events) }
        viewModel.sendEvent(event)
        runCurrent()

        Assert.assertEquals(budgetEntries, events.last().entries)
        verify { budgetEntryRepository.getAll() }
        job.cancel()
    }

    @Test
    fun deleteBudgetEvent() = runTest(dispatcher) {
        val event = BudgetDetailEvent.DeleteBudget
        val events = mutableListOf<BudgetDetailState>()

        every { budgetRepository.delete(any()) } returns Unit
        val job = launch { viewModel.uiState.toList(events) }
        viewModel.sendEvent(event)
        runCurrent()

        Assert.assertTrue(events.last().goBack)
        verify { budgetRepository.delete(any()) }
        job.cancel()
    }

    @Test
    fun deleteSelectedEntriesEvent() = runTest(dispatcher) {
        val event = BudgetDetailEvent.DeleteSelectedEntries
        val events = mutableListOf<BudgetDetailState>()

        every { budgetEntryRepository.deleteByIds(any()) } returns Unit
        val job = launch { viewModel.uiState.toList(events) }
        viewModel.sendEvent(event)
        runCurrent()

        Assert.assertFalse(events.last().isSelectionActive)
        verify { budgetEntryRepository.deleteByIds(any()) }
        job.cancel()
    }

    @Test
    fun showEntryEvent() = runTest {
        val entry: BudgetEntry = mockk()
        val event = BudgetDetailEvent.ShowEntry(entry)
        viewModel.sendEvent(event)
        val state = viewModel.uiState.value
        Assert.assertEquals(entry, state.showEntry)
    }

    @Test
    fun toggleBudgetModalEvent() = runTest {
        val event = BudgetDetailEvent.ToggleBudgetModal(true)
        viewModel.sendEvent(event)
        val state = viewModel.uiState.value
        Assert.assertTrue(state.isBudgetModalVisible)
    }

    @Test
    fun toggleDeleteBudgetModalEvent() = runTest {
        val event = BudgetDetailEvent.ToggleDeleteBudgetModal(true)
        viewModel.sendEvent(event)
        val state = viewModel.uiState.value
        Assert.assertTrue(state.isDeleteBudgetModalVisible)
    }

    @Test
    fun toggleDeleteEntriesModalEvent() = runTest {
        val event = BudgetDetailEvent.ToggleDeleteEntriesModal(true)
        viewModel.sendEvent(event)
        val state = viewModel.uiState.value
        Assert.assertTrue(state.isDeleteEntriesModalVisible)
    }

    @Test
    fun toggleFilterModalEvent() = runTest {
        val event = BudgetDetailEvent.ToggleFilterModal(true)
        viewModel.sendEvent(event)
        val state = viewModel.uiState.value
        Assert.assertTrue(state.isFilterModalVisible)
    }

    @Test
    fun activateSelectionStateEvent() = runTest {
        val event = BudgetDetailEvent.ToggleSelectionState(true)
        viewModel.sendEvent(event)
        val state = viewModel.uiState.value
        Assert.assertTrue(state.isSelectionActive)
    }

    @Test
    fun deactivateSelectionStateEvent() = runTest {
        getBudgetEntriesEvent()
        val event = BudgetDetailEvent.ToggleSelectionState(false)
        viewModel.sendEvent(event)
        val state = viewModel.uiState.value
        Assert.assertFalse(state.isSelectionActive)
        Assert.assertTrue(state.entries.none { it.isSelected })
    }

    @Test
    fun toggleAllEntriesSelectionEvent() = runTest {
        getBudgetEntriesEvent()
        val event = BudgetDetailEvent.ToggleAllEntriesSelection(true)
        viewModel.sendEvent(event)
        val state = viewModel.uiState.value
        Assert.assertTrue(state.entries.all { it.isSelected })
    }

    @Test
    fun toggleEntrySelectionEvent() = runTest {
        getBudgetEntriesEvent()
        val event = BudgetDetailEvent.ToggleSelectEntry(0, true)
        viewModel.sendEvent(event)
        val state = viewModel.uiState.value
        Assert.assertTrue(state.entries.first().isSelected)
    }

    @Test
    fun clearNavigationEvent() = runTest {
        val event = BudgetDetailEvent.ClearNavigation
        viewModel.sendEvent(event)
        val state = viewModel.uiState.value
        Assert.assertNull(state.showEntry)
    }
}
