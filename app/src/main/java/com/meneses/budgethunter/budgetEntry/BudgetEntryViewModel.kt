package com.meneses.budgethunter.budgetEntry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryState
import com.meneses.budgethunter.budgetEntry.data.repository.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.repository.BudgetEntryLocalRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetEntryViewModel(
    private val budgetEntryRepository: BudgetEntryRepository = BudgetEntryLocalRepository(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetEntryState())
    val uiState = _uiState.asStateFlow()

    fun setBudgetEntry(budgetEntry: BudgetEntry?) =
        _uiState.update { it.copy(budgetEntry = budgetEntry) }

    fun saveBudgetEntry() {
        viewModelScope.launch(dispatcher) {
            _uiState.value.budgetEntry?.let { entry ->
                if (entry.amount.isBlank()) return@let
                if (entry.id < 0) budgetEntryRepository.create(entry)
                else budgetEntryRepository.update(entry)
                _uiState.update { it.copy(goBack = true) }
            }
        }
    }

    fun validateChanges(budgetEntry: BudgetEntry) =
        _uiState.update {
            if (budgetEntry == it.budgetEntry) it.copy(goBack = true)
            else it.copy(isDiscardChangesModalVisible = true)
        }

    fun hideDiscardChangesModal() =
        _uiState.update { it.copy(isDiscardChangesModalVisible = false) }
}