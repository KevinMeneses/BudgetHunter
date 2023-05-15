package com.meneses.budgethunter.budgetList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.budgetList.application.BudgetListState
import com.meneses.budgethunter.budgetList.data.repository.BudgetLocalRepository
import com.meneses.budgethunter.budgetList.data.repository.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetListViewModel(
    private val budgetRepository: BudgetRepository = BudgetLocalRepository(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
    val uiState get() = _uiState.asStateFlow()
    private val _uiState = MutableStateFlow(BudgetListState())

    init {
        collectBudgetList()
    }

    private fun collectBudgetList() {
        viewModelScope.launch(dispatcher) {
            budgetRepository.budgets.collect { budgetList ->
                _uiState.update {
                    val updatedList = if (it.filter == null) budgetList
                    else budgetRepository.getAllFilteredBy(it.filter)
                    it.copy(budgetList = updatedList)
                }
            }
        }
    }

    fun createBudget(budget: Budget) {
        viewModelScope.launch(dispatcher) {
            val budgetSaved = budgetRepository.create(budget)
            navigateToBudget(budgetSaved)
        }
    }

    fun navigateToBudget(budget: Budget) =
        _uiState.update { it.copy(navigateToBudget = budget) }

    fun filterList(filter: BudgetFilter) {
        viewModelScope.launch(dispatcher) {
            val filteredList = budgetRepository.getAllFilteredBy(filter)
            _uiState.update { it.copy(budgetList = filteredList, filter = filter) }
        }
    }

    fun clearFilter() {
        viewModelScope.launch(dispatcher) {
            val budgetList = budgetRepository.getAll()
            _uiState.update { it.copy(budgetList = budgetList, filter = null) }
        }
    }

    fun onDispose() = _uiState.update { it.copy(navigateToBudget = null) }

    fun showAddModal() = setAddModalVisibility(true)

    fun hideAddModal() = setAddModalVisibility(false)

    fun showFilterModal() = setFilterModalVisibility(true)

    fun hideFilterModal() = setFilterModalVisibility(false)

    private fun setAddModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(addModalVisibility = visible) }

    private fun setFilterModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(filterModalVisibility = visible) }
}