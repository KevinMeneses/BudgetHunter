package com.meneses.budgethunter.budgetEntry

import com.meneses.budgethunter.budgetEntry.application.BudgetEntryEvent
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryState
import com.meneses.budgethunter.budgetEntry.data.repository.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

@ExperimentalCoroutinesApi
class BudgetEntryViewModelTest {

    private val repository: BudgetEntryRepository = mockk()
    private val dispatcher = StandardTestDispatcher()

    private val viewModel = BudgetEntryViewModel(
        budgetEntryRepository = repository,
        dispatcher = dispatcher
    )

    @Test
    fun sendGoBackEvent() = runTest {
        viewModel.sendEvent(BudgetEntryEvent.GoBack)
        val state = viewModel.uiState.value
        Assert.assertTrue(state.goBack)
    }

    @Test
    fun sendHideDiscardChangesModalEvent() = runTest {
        viewModel.sendEvent(BudgetEntryEvent.HideDiscardChangesModal)
        val state = viewModel.uiState.value
        Assert.assertFalse(state.isDiscardChangesModalVisible)
    }

    @Test
    fun sendSetBudgetEntryEvent() = runTest {
        val budgetEntry = BudgetEntry()
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(budgetEntry))
        val state = viewModel.uiState.value
        Assert.assertEquals(budgetEntry, state.budgetEntry)
    }

    @Test
    fun sendSaveBudgetEntryEventAmountError() = runTest(dispatcher) {
        val budgetEntry = BudgetEntry()
        val state = mutableListOf<BudgetEntryState>()
        val job = launch { viewModel.uiState.toList(state) }

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(budgetEntry))
        viewModel.sendEvent(BudgetEntryEvent.SaveBudgetEntry)
        runCurrent()

        Assert.assertNotNull(state.last().emptyAmountError)
        job.cancel()
    }

    @Test
    fun sendSaveBudgetEntryEvent() = runTest(dispatcher) {
        val budgetEntry = BudgetEntry(amount = "20")
        val state = mutableListOf<BudgetEntryState>()
        val job = launch { viewModel.uiState.toList(state) }

        every { repository.create(budgetEntry) } returns Unit

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(budgetEntry))
        viewModel.sendEvent(BudgetEntryEvent.SaveBudgetEntry)
        runCurrent()

        verify { repository.create(budgetEntry) }

        sendGoBackEvent()
        job.cancel()
    }

    @Test
    fun sendSaveBudgetEntryEventUpdate() = runTest(dispatcher) {
        val budgetEntry = BudgetEntry(id= 1, amount = "20")
        val state = mutableListOf<BudgetEntryState>()
        val job = launch { viewModel.uiState.toList(state) }

        every { repository.update(budgetEntry) } returns Unit

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(budgetEntry))
        viewModel.sendEvent(BudgetEntryEvent.SaveBudgetEntry)
        runCurrent()

        verify { repository.update(budgetEntry) }

        sendGoBackEvent()
        job.cancel()
    }

    @Test
    fun sendValidateChangesEventNoChanges() = runTest {
        val budgetEntry = BudgetEntry()
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(budgetEntry))
        viewModel.sendEvent(BudgetEntryEvent.ValidateChanges(budgetEntry))
        val state = viewModel.uiState.value
        Assert.assertTrue(state.goBack)
    }

    @Test
    fun sendValidateChangesEventChanges() = runTest {
        val budgetEntry = BudgetEntry()
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(budgetEntry))
        val updatedBudgetEntry = budgetEntry.copy(amount = "10")
        viewModel.sendEvent(BudgetEntryEvent.ValidateChanges(updatedBudgetEntry))
        val state = viewModel.uiState.value
        Assert.assertTrue(state.isDiscardChangesModalVisible)
    }
}
