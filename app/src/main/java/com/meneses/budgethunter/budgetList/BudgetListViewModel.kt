package com.meneses.budgethunter.budgetList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.budgetList.application.BudgetListState
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.data.LocalBudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetListViewModel(
    private val budgetRepository: BudgetRepository = LocalBudgetRepository(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetListState())
    val uiState get() = _uiState.asStateFlow()

    init {
        budgetRepository.getAllBudgets()
        collectBudgetList()
    }

    private fun collectBudgetList() {
        viewModelScope.launch(dispatcher) {
            budgetRepository.budgetList.collect { budgetList ->
                _uiState.update { state ->
                    state.copy(budgetList = budgetList)
                }
            }
        }
    }

    fun setAddModalVisibility(visible: Boolean) {
        _uiState.update {
            it.copy(addModalVisibility = visible)
        }
    }

    fun setFilterModalVisibility(visible: Boolean) {
        _uiState.update {
            it.copy(filterModalVisibility = visible)
        }
    }

    fun createBudget(budget: Budget) {
        val budgetSaved = budgetRepository.createBudget(budget)
        navigateToBudget(budgetSaved)
    }

    fun navigateToBudget(budget: Budget) {
        _uiState.update {
            it.copy(navigateToBudget = budget)
        }
    }

    fun filterList(filter: Budget) {
        budgetRepository.getBudgetsBy(filter)
        _uiState.update {
            it.copy(filter = filter)
        }
    }

    fun clearFilter() {
        budgetRepository.getAllBudgets()
    }

    fun onDispose() {
        _uiState.update {
            it.copy(navigateToBudget = null)
        }
    }
}