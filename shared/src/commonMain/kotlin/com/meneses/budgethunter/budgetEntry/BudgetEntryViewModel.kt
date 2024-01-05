package com.meneses.budgethunter.budgetEntry

import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryEvent
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryState
import com.meneses.budgethunter.budgetEntry.data.repository.BudgetEntryLocalRepository
import com.meneses.budgethunter.budgetEntry.data.repository.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moe.tlaster.precompose.stateholder.SavedStateHolder
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope


fun budgetEntryViewModel(): (SavedStateHolder) -> BudgetEntryViewModel =
    { BudgetEntryViewModel() }

class BudgetEntryViewModel(
    private val budgetEntryRepository: BudgetEntryRepository = BudgetEntryLocalRepository(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetEntryState())
    val uiState = _uiState.asStateFlow()

    fun sendEvent(event: BudgetEntryEvent) {
        when (event) {
            is BudgetEntryEvent.GoBack -> goBack()
            is BudgetEntryEvent.HideDiscardChangesModal -> hideDiscardChangesModal()
            is BudgetEntryEvent.SaveBudgetEntry -> saveBudgetEntry()
            is BudgetEntryEvent.SetBudgetEntry -> setBudgetEntry(event.budgetEntry)
            is BudgetEntryEvent.ValidateChanges -> validateChanges(event.budgetEntry)
        }
    }

    private fun setBudgetEntry(budgetEntry: BudgetEntry?) =
        _uiState.update {
            it.copy(
                budgetEntry = budgetEntry,
                emptyAmountError = null
            )
        }

    private fun saveBudgetEntry() {
        viewModelScope.launch(dispatcher) {
            _uiState.value.budgetEntry?.let { entry ->
                if (entry.amount.isBlank()) {
                    showAmountError()
                    return@launch
                }
                if (entry.id < 0) budgetEntryRepository.create(entry)
                else budgetEntryRepository.update(entry)
                goBack()
            }
        }
    }

    private fun showAmountError() {
        _uiState.update {
            it.copy(emptyAmountError = R.string.amount_mandatory)
        }
    }

    private fun goBack() =
        _uiState.update { it.copy(goBack = true) }

    private fun validateChanges(budgetEntry: BudgetEntry) =
        _uiState.update {
            if (budgetEntry == it.budgetEntry) it.copy(goBack = true)
            else it.copy(isDiscardChangesModalVisible = true)
        }

    private fun hideDiscardChangesModal() =
        _uiState.update { it.copy(isDiscardChangesModalVisible = false) }
}
