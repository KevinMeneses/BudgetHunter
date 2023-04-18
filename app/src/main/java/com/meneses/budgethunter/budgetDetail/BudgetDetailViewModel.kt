package com.meneses.budgethunter.budgetDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetDetail.application.InsAndOutsState
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.LocalBudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.data.LocalBudgetRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetDetailViewModel(
    private val budgetEntryRepository: BudgetEntryRepository = LocalBudgetEntryRepository(),
    private val budgetRepository: BudgetRepository = LocalBudgetRepository(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsAndOutsState())
    val uiState = _uiState.asStateFlow()

    init {
        collectBudgetEntries()
    }

    private fun collectBudgetEntries() {
        viewModelScope.launch(dispatcher) {
            budgetEntryRepository.budgetEntries.collect { entries ->
                _uiState.update { state ->
                    state.copy(entries = entries)
                }
            }
        }
    }

    fun setBudget(budget: Budget) {
        _uiState.update {
            it.copy(budget = budget)
        }
    }

    fun getBudgetEntries() {
        val id = _uiState.value.budget.id
        budgetEntryRepository.getEntriesByBudgetId(id)
    }

    fun showBudgetModal() = setBudgetModalVisibility(true)

    fun hideBudgetModal() = setBudgetModalVisibility(false)

    private fun setBudgetModalVisibility(visible: Boolean) {
        _uiState.update {
            it.copy(isBudgetModalVisible = visible)
        }
    }

    fun showFilterModal() = setFilterModalVisibility(true)

    fun hideFilterModal() = setFilterModalVisibility(false)

    private fun setFilterModalVisibility(visible: Boolean) {
        _uiState.update {
            it.copy(isFilterModalVisible = visible)
        }
    }

    fun showDeleteModal() = setDeleteModalVisibility(true)

    fun hideDeleteModal() = setDeleteModalVisibility(false)

    private fun setDeleteModalVisibility(visible: Boolean) {
        _uiState.update {
            it.copy(isDeleteModalVisible = visible)
        }
    }

    fun setBudgetAmount(amount: Double) {
        _uiState.update {
            val budget = it.budget.copy(amount = amount)
            budgetRepository.updateBudget(budget)
            it.copy(budget = budget)
        }
    }

    fun filterEntries(type: BudgetEntry.Type) {
        val budgetEntry = BudgetEntry(
            budgetId = _uiState.value.budget.id,
            type = type
        )
        budgetEntryRepository.getEntriesBy(budgetEntry)
        _uiState.update {
            it.copy(filter = type)
        }
    }

    fun clearFilter() {
        getBudgetEntries()
        _uiState.update {
            it.copy(filter = null)
        }
    }

    fun deleteBudget() {
        val budget = _uiState.value.budget
        budgetRepository.deleteBudget(budget)
    }
}