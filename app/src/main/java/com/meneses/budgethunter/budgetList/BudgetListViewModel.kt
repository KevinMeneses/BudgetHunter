package com.meneses.budgethunter.budgetList

import androidx.lifecycle.ViewModel
import com.meneses.budgethunter.budgetList
import com.meneses.budgethunter.budgetList.application.BudgetListState
import com.meneses.budgethunter.budgetList.domain.Budget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BudgetListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetListState())
    val uiState get() = _uiState.asStateFlow()

    init {
        getAllBudgets()
    }

    private fun getAllBudgets() {
        _uiState.update {
            it.copy(budgetList = budgetList)
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
        budgetList.add(budget)
        getAllBudgets()
    }

    fun filterList(filter: Budget) {
        val list = budgetList
            .filter {
                if (filter.name.isBlank()) true
                else it.name == filter.name
            }
            .filter {
                if (filter.frequency == null) true
                else it.frequency == filter.frequency
            }

        _uiState.update {
            it.copy(
                budgetList = list,
                filter = filter
            )
        }
    }

    fun clearFilter() {
        _uiState.update {
            it.copy(
                budgetList = budgetList,
                filter = null
            )
        }
    }
}