package com.meneses.budgethunter.budgetDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetDetail.data.BudgetDetailRepository
import com.meneses.budgethunter.budgetDetail.data.CollaborationException
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetList.domain.Budget
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetDetailViewModel(
    private val budgetDetailRepository: BudgetDetailRepository = BudgetDetailRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetDetailState())
    val uiState = _uiState.asStateFlow()

    fun sendEvent(event: BudgetDetailEvent) {
        when (event) {
            is BudgetDetailEvent.SetBudget -> setBudget(event.budget)
            is BudgetDetailEvent.GetBudgetDetail -> getBudgetDetail()
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
            BudgetDetailEvent.SortList -> orderList()
        }
    }

    private fun orderList() = _uiState.update { currentState ->
        val newOrder: BudgetDetailState.ListOrder
        val orderedEntries: List<BudgetEntry>

        when (currentState.listOrder) {
            BudgetDetailState.ListOrder.DEFAULT -> {
                newOrder = BudgetDetailState.ListOrder.AMOUNT_ASCENDANT
                orderedEntries = currentState.budgetDetail.entries.sortedBy {
                    val isPositive = it.type == BudgetEntry.Type.INCOME
                    if (isPositive) it.amount
                    else "-" + it.amount
                }
            }

            BudgetDetailState.ListOrder.AMOUNT_ASCENDANT -> {
                newOrder = BudgetDetailState.ListOrder.AMOUNT_DESCENDANT
                orderedEntries = currentState.budgetDetail.entries.sortedByDescending {
                    val isPositive = it.type == BudgetEntry.Type.INCOME
                    if (isPositive) it.amount
                    else "-" + it.amount
                }
            }

            BudgetDetailState.ListOrder.AMOUNT_DESCENDANT -> {
                newOrder = BudgetDetailState.ListOrder.DEFAULT
                orderedEntries = currentState.budgetDetail.entries.sortedByDescending { it.id }
            }
        }

        currentState.copy(
            budgetDetail = currentState.budgetDetail.copy(entries = orderedEntries),
            listOrder = newOrder
        )
    }

    private fun stopCollaboration() = viewModelScope.launch {
        budgetDetailRepository.stopCollaboration()
        _uiState.update { it.copy(isCollaborationActive = false) }
    }

    private fun startCollaboration() = viewModelScope.launch {
        try {
            val collaborationCode = budgetDetailRepository.startCollaboration()
            budgetDetailRepository.consumeCollaborationStream()
            _uiState.update {
                it.copy(
                    isCollaborationActive = true,
                    collaborationCode = collaborationCode
                )
            }
        } catch (e: CollaborationException) {
            collaborationError(e.message.orEmpty())
        } catch (e: Exception) {
            val errorMessage = "An error occurred trying to collaborate, please try again later"
            collaborationError(errorMessage)
        }
    }

    private suspend fun collaborationError(errorMessage: String) {
        _uiState.update { it.copy(collaborationError = errorMessage) }
        delay(3000)
        _uiState.update { it.copy(collaborationError = null) }
    }

    private fun toggleCollaborationModal(visible: Boolean) =
        _uiState.update { it.copy(isCollaborateModalVisible = visible) }

    private fun setBudget(budget: Budget) =
        _uiState.update {
            it.copy(
                budgetDetail = it.budgetDetail
                    .copy(budget = budget)
            )
        }

    private fun getBudgetDetail() = viewModelScope.launch {
        val budgetId = _uiState.value.budgetDetail.budget.id
        budgetDetailRepository
            .getBudgetDetailById(budgetId)
            .collect { detail ->
                _uiState.update {
                    val updatedDetail = if (it.filter == null) detail
                    else budgetDetailRepository.getAllFilteredBy(it.filter)
                    it.copy(budgetDetail = updatedDetail)
                }
            }
    }

    private fun clearNavigation() =
        _uiState.update { it.copy(showEntry = null) }

    private fun deleteSelectedEntries() = viewModelScope.launch {
        val entriesToDeleteIds = _uiState.value.budgetDetail.entries
            .filter { it.isSelected }
            .map { it.id }

        budgetDetailRepository.deleteEntriesByIds(entriesToDeleteIds)
        toggleSelectionState(false)
    }

    private fun toggleEntrySelection(event: BudgetDetailEvent.ToggleSelectEntry) {
        _uiState.update { state ->
            val updatedEntry = state.budgetDetail
                .entries[event.index]
                .copy(isSelected = event.isSelected)

            val updatedList = state.budgetDetail
                .entries
                .toMutableList()
                .apply { set(index = event.index, element = updatedEntry) }

            state.copy(
                budgetDetail = state.budgetDetail
                    .copy(entries = updatedList)
            )
        }
    }

    private fun showEntry(budgetItem: BudgetEntry) =
        _uiState.update { it.copy(showEntry = budgetItem) }

    private fun updateBudgetAmount(amount: Double) = viewModelScope.launch {
        budgetDetailRepository.updateBudgetAmount(amount)
    }

    private fun filterEntries(filter: BudgetEntryFilter) = viewModelScope.launch {
        val filteredDetail = budgetDetailRepository.getAllFilteredBy(filter)
        _uiState.update { it.copy(budgetDetail = filteredDetail, filter = filter) }
    }

    private fun clearFilter() = viewModelScope.launch {
        val detail = budgetDetailRepository.getCachedDetail()
        _uiState.update { it.copy(budgetDetail = detail, filter = null) }
    }

    private fun deleteBudget() = viewModelScope.launch {
        _uiState.update {
            budgetDetailRepository.deleteBudget()
            budgetDetailRepository.stopCollaboration()
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
            val updatedEntries = state.budgetDetail.entries
                .map { it.copy(isSelected = isSelected) }
            state.copy(
                budgetDetail = state.budgetDetail
                    .copy(entries = updatedEntries)
            )
        }
}
