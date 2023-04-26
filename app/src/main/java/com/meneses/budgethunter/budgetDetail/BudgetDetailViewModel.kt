package com.meneses.budgethunter.budgetDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetEntry.data.repository.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.repository.BudgetEntryLocalRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.repository.BudgetRepository
import com.meneses.budgethunter.budgetList.data.repository.BudgetLocalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetDetailViewModel(
    private val budgetEntryRepository: BudgetEntryRepository = BudgetEntryLocalRepository(),
    private val budgetRepository: BudgetRepository = BudgetLocalRepository(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetDetailState())
    val uiState = _uiState.asStateFlow()

    fun setBudget(budget: Budget) =
        _uiState.update { it.copy(budget = budget) }

    fun getBudgetEntries() {
        viewModelScope.launch(dispatcher) {
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
    }

    fun clearNavigation() =
        _uiState.update { it.copy(showEntry = null) }

    fun sendEvent(event: BudgetDetailEvent) {
        when(event) {
            is BudgetDetailEvent.UpdateBudgetAmount -> updateBudgetAmount(event.amount)
            is BudgetDetailEvent.FilterEntries -> filterEntries(event.filter)
            is BudgetDetailEvent.ClearFilter -> clearFilter()
            is BudgetDetailEvent.DeleteBudget -> deleteBudget()
            is BudgetDetailEvent.HideBudgetModal -> setBudgetModalVisibility(false)
            is BudgetDetailEvent.HideDeleteModal -> setDeleteModalVisibility(false)
            is BudgetDetailEvent.HideFilterModal -> setFilterModalVisibility(false)
            is BudgetDetailEvent.ShowBudgetModal -> setBudgetModalVisibility(true)
            is BudgetDetailEvent.ShowDeleteModal -> setDeleteModalVisibility(true)
            is BudgetDetailEvent.ShowFilterModal -> setFilterModalVisibility(true)
            is BudgetDetailEvent.ActivateSelection -> toggleSelection(true)
            is BudgetDetailEvent.DeactivateSelection -> toggleSelection(false)
            is BudgetDetailEvent.SelectEntry -> toggleEntrySelection(event.index, true)
            is BudgetDetailEvent.UnselectEntry -> toggleEntrySelection(event.index, false)
            is BudgetDetailEvent.ShowEntry -> showEntry(event.budgetItem)
            is BudgetDetailEvent.SelectAllEntries -> toggleAllEntriesSelection(true)
            is BudgetDetailEvent.UnselectAllEntries -> toggleAllEntriesSelection(false)
        }
    }

    private fun toggleEntrySelection(index: Int, isSelected: Boolean) {
        _uiState.update { state ->
            val updatedEntry = state.entries[index].copy(isSelected = isSelected)
            val updatedList = state.entries
                .toMutableList()
                .apply { set(index = index, element = updatedEntry) }

            state.copy(entries = updatedList)
        }
    }

    private fun showEntry(budgetItem: BudgetEntry) =
        _uiState.update { it.copy(showEntry = budgetItem) }

    private fun updateBudgetAmount(amount: Double) {
        viewModelScope.launch(dispatcher) {
            _uiState.update {
                val budget = it.budget.copy(amount = amount)
                budgetRepository.update(budget)
                it.copy(budget = budget)
            }
        }
    }

    private fun filterEntries(filter: BudgetEntry) {
        viewModelScope.launch(dispatcher) {
            val entryFilter = filter.copy(budgetId = _uiState.value.budget.id)
            val filteredEntries = budgetEntryRepository.getAllFilteredBy(entryFilter)
            _uiState.update { it.copy(entries = filteredEntries, filter = entryFilter) }
        }
    }

    private fun clearFilter() {
        viewModelScope.launch(dispatcher) {
            val entries = budgetEntryRepository.getAll()
            _uiState.update { it.copy(entries = entries, filter = null) }
        }
    }

    private fun deleteBudget() {
        viewModelScope.launch(dispatcher) {
            val budget = _uiState.value.budget
            budgetRepository.delete(budget)
        }
    }

    private fun setFilterModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(isFilterModalVisible = visible) }

    private fun setDeleteModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(isDeleteModalVisible = visible) }

    private fun setBudgetModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(isBudgetModalVisible = visible) }

    private fun toggleSelection(isActivated: Boolean) =
        _uiState.update { it.copy(isSelectionActive = isActivated) }
            .also { if (!isActivated) toggleAllEntriesSelection(false) }

    private fun toggleAllEntriesSelection(isSelected: Boolean) =
        _uiState.update { state ->
            val updatedEntries = state.entries.map { it.copy(isSelected = isSelected) }
            state.copy(entries = updatedEntries)
        }
}