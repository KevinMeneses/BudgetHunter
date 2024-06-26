package com.meneses.budgethunter.budgetDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetDetailViewModel(
    private val budgetEntryRepository: BudgetEntryRepository = BudgetEntryRepository(),
    private val budgetRepository: BudgetRepository = BudgetRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetDetailState())
    val uiState = _uiState.asStateFlow()

    fun sendEvent(event: BudgetDetailEvent) {
        when (event) {
            is BudgetDetailEvent.SetBudget -> setBudget(event.budget)
            is BudgetDetailEvent.GetBudgetEntries -> getBudgetEntries()
            is BudgetDetailEvent.UpdateBudgetAmount -> updateBudgetAmount(event.amount)
            is BudgetDetailEvent.FilterEntries -> filterEntries(event.filter)
            is BudgetDetailEvent.ClearFilter -> clearFilter()
            is BudgetDetailEvent.DeleteBudget -> deleteBudget()
            is BudgetDetailEvent.DeleteSelectedEntries -> deleteSelectedEntries()
            is BudgetDetailEvent.ShowEntry -> showEntry(event.budgetItem)
            is BudgetDetailEvent.ToggleBudgetModal -> setBudgetModalVisibility(event.isVisible)
            is BudgetDetailEvent.ToggleDeleteBudgetModal -> setDeleteBudgetModalVisibility(event.isVisible)
            is BudgetDetailEvent.ToggleDeleteEntriesModal -> setDeleteEntriesModalVisibility(event.isVisible)
            is BudgetDetailEvent.ToggleFilterModal -> setFilterModalVisibility(event.isVisible)
            is BudgetDetailEvent.ToggleSelectionState -> toggleSelectionState(event.isActivated)
            is BudgetDetailEvent.ToggleAllEntriesSelection -> toggleAllEntriesSelection(event.isSelected)
            is BudgetDetailEvent.ToggleSelectEntry -> toggleEntrySelection(event)
            is BudgetDetailEvent.ClearNavigation -> clearNavigation()
            is BudgetDetailEvent.StartCollaboration -> startCollaboration()
            is BudgetDetailEvent.ToggleCollaborateModal -> toggleCollaborationModal(event.isVisible)
            BudgetDetailEvent.StopCollaboration -> stopCollaboration()
            BudgetDetailEvent.HideCodeModal -> _uiState.update { it.copy(collaborationCode = null) }
        }
    }

    private fun stopCollaboration() = viewModelScope.launch {
        budgetRepository.stopCollaboration()
        _uiState.update { it.copy(isCollaborationActive = false) }
    }

    private fun startCollaboration() = viewModelScope.launch {
        try {
            val collaborationCode = budgetRepository.startCollaboration()
            if (collaborationCode != -1) {
                budgetRepository.joinCollaboration(collaborationCode)
                budgetRepository.consumeCollaborationStream()
                budgetEntryRepository.consumeCollaborationStream()
                _uiState.update {
                    it.copy(
                        isCollaborationActive = true,
                        collaborationCode = collaborationCode
                    )
                }
            } else {
                collaborationError()
            }
        } catch (e: Exception) {
            collaborationError()
        }
    }

    private suspend fun collaborationError() {
        val errorMessage = "An error occurred trying to collaborate, please try again later"
        _uiState.update { it.copy(collaborationError = errorMessage) }
        delay(3000)
        _uiState.update { it.copy(collaborationError = null) }
    }

    private fun toggleCollaborationModal(visible: Boolean) =
        _uiState.update { it.copy(isCollaborateModalVisible = visible) }

    private fun setBudget(budget: Budget) =
        _uiState.update { it.copy(budget = budget) }

    private fun getBudgetEntries() = viewModelScope.launch {
        budgetEntryRepository
            .getAllByBudgetId(_uiState.value.budget.id)
            .collect { entries ->
                _uiState.update {
                    val updatedEntries = if (it.filter == null) entries
                    else budgetEntryRepository.getAllFilteredBy(it.filter)
                    it.copy(entries = updatedEntries)
                }
            }
    }

    private fun clearNavigation() =
        _uiState.update { it.copy(showEntry = null) }

    private fun deleteSelectedEntries() = viewModelScope.launch {
        val entriesToDeleteIds = _uiState.value.entries
            .filter { it.isSelected }
            .map { it.id }

        budgetEntryRepository.deleteByIds(entriesToDeleteIds)
        toggleSelectionState(false)
    }

    private fun toggleEntrySelection(event: BudgetDetailEvent.ToggleSelectEntry) {
        _uiState.update { state ->
            val updatedEntry = state.entries[event.index].copy(isSelected = event.isSelected)
            val updatedList = state.entries
                .toMutableList()
                .apply { set(index = event.index, element = updatedEntry) }

            state.copy(entries = updatedList)
        }
    }

    private fun showEntry(budgetItem: BudgetEntry) =
        _uiState.update { it.copy(showEntry = budgetItem) }

    private fun updateBudgetAmount(amount: Double) = viewModelScope.launch {
        _uiState.update {
            val budget = it.budget.copy(amount = amount)
            budgetRepository.update(budget)
            it.copy(budget = budget)
        }
    }

    private fun filterEntries(filter: BudgetEntryFilter) = viewModelScope.launch {
        val filteredEntries = budgetEntryRepository.getAllFilteredBy(filter)
        _uiState.update { it.copy(entries = filteredEntries, filter = filter) }
    }

    private fun clearFilter() = viewModelScope.launch {
        val entries = budgetEntryRepository.getAll()
        _uiState.update { it.copy(entries = entries, filter = null) }
    }

    private fun deleteBudget() = viewModelScope.launch {
        _uiState.update {
            budgetRepository.delete(it.budget)
            it.copy(goBack = true)
        }
    }

    private fun setFilterModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(isFilterModalVisible = visible) }

    private fun setDeleteBudgetModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(isDeleteBudgetModalVisible = visible) }

    private fun setDeleteEntriesModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(isDeleteEntriesModalVisible = visible) }

    private fun setBudgetModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(isBudgetModalVisible = visible) }

    private fun toggleSelectionState(isActivated: Boolean) =
        _uiState.update { it.copy(isSelectionActive = isActivated) }
            .also { if (!isActivated) toggleAllEntriesSelection(false) }

    private fun toggleAllEntriesSelection(isSelected: Boolean) =
        _uiState.update { state ->
            val updatedEntries = state.entries.map { it.copy(isSelected = isSelected) }
            state.copy(entries = updatedEntries)
        }
}
