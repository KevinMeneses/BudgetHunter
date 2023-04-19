package com.meneses.budgethunter.budgetEntry

import androidx.lifecycle.ViewModel
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryState
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.LocalBudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BudgetEntryViewModel(
    private val budgetEntryRepository: BudgetEntryRepository = LocalBudgetEntryRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(BudgetEntryState())
    val uiState = _uiState.asStateFlow()

    fun setBudgetEntry(budgetEntry: BudgetEntry?) {
        _uiState.update {
            it.copy(budgetEntry = budgetEntry)
        }
    }

    fun saveBudgetEntry() {
        _uiState.value.budgetEntry?.let {
            budgetEntryRepository.putEntry(it)
        }
    }

    fun validateChanges(budgetEntry: BudgetEntry) {
        _uiState.update {
            if (budgetEntry == _uiState.value.budgetEntry) {
                it.copy(goBack = true)
            } else {
                it.copy(isDiscardChangesModalVisible = true)
            }
        }
    }

    fun hideDiscardChangesModal() {
        _uiState.update {
            it.copy(isDiscardChangesModalVisible = false)
        }
    }
}