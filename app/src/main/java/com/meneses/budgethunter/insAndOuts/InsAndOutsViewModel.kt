package com.meneses.budgethunter.insAndOuts

import androidx.lifecycle.ViewModel
import com.meneses.budgethunter.budgetItemLists
import com.meneses.budgethunter.budgetList
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.insAndOuts.application.InsAndOutsState
import com.meneses.budgethunter.insAndOuts.domain.BudgetItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class InsAndOutsViewModel: ViewModel() {

    private val _uiState = MutableStateFlow(InsAndOutsState())
    val uiState = _uiState.asStateFlow()

    fun getBudgetItems(budget: Budget) {
        val itemList = budgetItemLists.filter { it.budgetId == budget.id }
        _uiState.update {
            it.copy(
                budget = budget,
                itemList = itemList
            )
        }
    }

    fun setBudgetModalVisibility(visible: Boolean) {
        _uiState.update {
            it.copy(isBudgetModalVisible = visible)
        }
    }

    fun setFilterModalVisibility(visible: Boolean) {
        _uiState.update {
            it.copy(isFilterModalVisible = visible)
        }
    }

    fun setDeleteModalVisibility(visible: Boolean) {
        _uiState.update {
            it.copy(isDeleteModalVisible = visible)
        }
    }

    fun setBudgetAmount(amount: Double) {
        _uiState.update {
            budgetList.remove(it.budget)
            val budget = it.budget.copy(amount = amount)
            budgetList.add(budget)
            it.copy(budget = budget)
        }
    }

    fun filterList(type: BudgetItem.Type) {
        val itemList = budgetItemLists.filter { it.budgetId == _uiState.value.budget.id }
        val list = itemList.filter { it.type == type }
        _uiState.update {
            it.copy(
                itemList = list,
                filter = type
            )
        }
    }

    fun clearFilter() {
        val itemList = budgetItemLists.filter { it.budgetId == _uiState.value.budget.id }
        _uiState.update {
            it.copy(
                itemList = itemList,
                filter = null
            )
        }
    }

    fun deleteBudget() {
        val budget = _uiState.value.budget
        budgetList.remove(budget)
    }
}