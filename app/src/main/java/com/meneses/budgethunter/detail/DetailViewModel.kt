package com.meneses.budgethunter.detail

import androidx.lifecycle.ViewModel
import com.meneses.budgethunter.budgetItemLists
import com.meneses.budgethunter.detail.application.DetailState
import com.meneses.budgethunter.insAndOuts.domain.BudgetItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DetailViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(DetailState())
    val uiState = _uiState.asStateFlow()

    fun setDetail(budgetItem: BudgetItem?) {
        _uiState.update {
            it.copy(detail = budgetItem)
        }
    }

    fun saveBudgetDetail() {
        budgetItemLists.removeIf {
            it.id == _uiState.value.detail?.id
        }
        _uiState.value.detail?.let { budgetItemLists.add(it) }
    }
}