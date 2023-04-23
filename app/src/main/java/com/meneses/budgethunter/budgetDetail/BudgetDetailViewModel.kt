package com.meneses.budgethunter.budgetDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetDetail.application.InsAndOutsState
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

    private val _uiState = MutableStateFlow(InsAndOutsState())
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

    fun setBudgetAmount(amount: Double) {
        viewModelScope.launch(dispatcher) {
            _uiState.update {
                val budget = it.budget.copy(amount = amount)
                budgetRepository.update(budget)
                it.copy(budget = budget)
            }
        }
    }

    fun filterEntries(filter: BudgetEntry) {
        viewModelScope.launch(dispatcher) {
            val entryFilter = filter.copy(budgetId = _uiState.value.budget.id)
            val filteredEntries = budgetEntryRepository.getAllFilteredBy(entryFilter)
            _uiState.update { it.copy(entries = filteredEntries, filter = entryFilter) }
        }
    }

    fun clearFilter() {
        viewModelScope.launch(dispatcher) {
            val entries = budgetEntryRepository.getAll()
            _uiState.update { it.copy(entries = entries, filter = null) }
        }
    }

    fun deleteBudget() {
        viewModelScope.launch(dispatcher) {
            val budget = _uiState.value.budget
            budgetRepository.delete(budget)
        }
    }

    fun showBudgetModal() = setBudgetModalVisibility(true)

    fun hideBudgetModal() = setBudgetModalVisibility(false)

    fun showFilterModal() = setFilterModalVisibility(true)

    fun hideFilterModal() = setFilterModalVisibility(false)

    fun showDeleteModal() = setDeleteModalVisibility(true)

    fun hideDeleteModal() = setDeleteModalVisibility(false)

    private fun setFilterModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(isFilterModalVisible = visible) }

    private fun setDeleteModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(isDeleteModalVisible = visible) }

    private fun setBudgetModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(isBudgetModalVisible = visible) }
}