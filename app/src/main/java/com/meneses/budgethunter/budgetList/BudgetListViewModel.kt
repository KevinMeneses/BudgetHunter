package com.meneses.budgethunter.budgetList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.application.BudgetListState
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.application.DuplicateBudgetUseCase
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetListViewModel(
    private val budgetRepository: BudgetRepository = BudgetRepository(),
    private val duplicateBudgetUseCase: DuplicateBudgetUseCase = DuplicateBudgetUseCase(budgetRepository),
    private val deleteBudgetUseCase: DeleteBudgetUseCase = DeleteBudgetUseCase()
) : ViewModel() {
    val uiState get() = _uiState.asStateFlow()
    private val _uiState = MutableStateFlow(BudgetListState())

    init {
        collectBudgetList()
    }

    private fun collectBudgetList() {
        viewModelScope.launch {
            budgetRepository.budgets.collect { budgetList ->
                _uiState.update {
                    val updatedList = if (it.filter == null) budgetList
                    else budgetRepository.getAllFilteredBy(it.filter)
                    it.copy(budgetList = updatedList, isLoading = false)
                }
            }
        }
    }

    fun sendEvent(event: BudgetListEvent) {
        when (event) {
            is BudgetListEvent.CreateBudget -> createBudget(event.budget)
            is BudgetListEvent.UpdateBudget -> updateBudget(event.budget)
            is BudgetListEvent.DuplicateBudget -> duplicateBudget(event.budget)
            is BudgetListEvent.DeleteBudget -> deleteBudget(event.budgetId)
            is BudgetListEvent.FilterList -> filterList(event.filter)
            is BudgetListEvent.OpenBudget -> openBudget(event.budget)
            is BudgetListEvent.ToggleAddModal -> setAddModalVisibility(event.isVisible)
            is BudgetListEvent.ToggleUpdateModal -> setUpdateModalVisibility(event.budget)
            is BudgetListEvent.ToggleFilterModal -> setFilterModalVisibility(event.isVisible)
            is BudgetListEvent.ClearFilter -> clearFilter()
            is BudgetListEvent.ClearNavigation -> clearNavigation()
            is BudgetListEvent.JoinCollaboration -> joinCollaboration(event.collaborationCode)
            is BudgetListEvent.ToggleJoinCollaborationModal ->
                setJoinCollaborationModalVisibility(event.isVisible)
        }
    }

    private fun duplicateBudget(budget: Budget) = viewModelScope.launch {
        duplicateBudgetUseCase.execute(budget)
    }

    private fun deleteBudget(budgetId: Long) = viewModelScope.launch {
        deleteBudgetUseCase.execute(budgetId)
    }

    private fun setJoinCollaborationModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(joinCollaborationModalVisibility = visible) }

    private fun joinCollaboration(collaborationCode: String) {
        viewModelScope.launch {
            try {
                budgetRepository.joinCollaboration(collaborationCode.toInt())
                // TODO: navigate to detail and consume collaboration stream there
            } catch (e: Exception) {
                collaborationError()
            }
        }
    }

    private suspend fun collaborationError() {
        val errorMessage = "An error occurred trying to collaborate, please try again later"
        _uiState.update { it.copy(collaborationError = errorMessage) }
        delay(3000)
        _uiState.update { it.copy(collaborationError = null) }
    }

    private fun createBudget(budget: Budget) = viewModelScope.launch {
        val budgetSaved = budgetRepository.create(budget)
        openBudget(budgetSaved)
    }

    private fun updateBudget(budget: Budget) = viewModelScope.launch {
        budgetRepository.update(budget)
    }

    private fun openBudget(budget: Budget) =
        _uiState.update { it.copy(navigateToBudget = budget) }

    private fun filterList(filter: BudgetFilter) {
        viewModelScope.launch {
            val filteredList = budgetRepository.getAllFilteredBy(filter)
            _uiState.update { it.copy(budgetList = filteredList, filter = filter) }
        }
    }

    private fun clearFilter() {
        viewModelScope.launch {
            val budgetList = budgetRepository.getAllCached()
            _uiState.update { it.copy(budgetList = budgetList, filter = null) }
        }
    }

    private fun clearNavigation() =
        _uiState.update { it.copy(navigateToBudget = null) }

    private fun setAddModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(addModalVisibility = visible) }

    private fun setUpdateModalVisibility(budget: Budget?) =
        _uiState.update { it.copy(budgetToUpdate = budget) }

    private fun setFilterModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(filterModalVisibility = visible) }
}
