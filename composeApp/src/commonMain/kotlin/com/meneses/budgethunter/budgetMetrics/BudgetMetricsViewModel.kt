package com.meneses.budgethunter.budgetMetrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetMetrics.application.BudgetMetricsIntent
import com.meneses.budgethunter.budgetMetrics.application.BudgetMetricsState
import com.meneses.budgethunter.budgetMetrics.application.GetTotalsPerCategoryUseCase
import com.meneses.budgethunter.budgetMetrics.domain.CategoryMetric
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetMetricsViewModel(
    private val getTotalsPerCategoryUseCase: GetTotalsPerCategoryUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(BudgetMetricsState())
    val uiState = _uiState.asStateFlow()

    init {
        getMetrics(_uiState.value.selectedType)
    }

    fun sendIntent(intent: BudgetMetricsIntent) {
        when (intent) {
            is BudgetMetricsIntent.SelectType -> selectType(intent.type)
        }
    }

    private fun selectType(type: BudgetEntry.Type) {
        if (type == _uiState.value.selectedType) return
        _uiState.update { it.copy(selectedType = type) }
        getMetrics(type)
    }

    private fun getMetrics(type: BudgetEntry.Type) {
        viewModelScope.launch {
            val totalsPerCategory = getTotalsPerCategoryUseCase.execute(type)
            val total = totalsPerCategory.values.sum()

            val categoryMetrics = totalsPerCategory.map { (category, amount) ->
                CategoryMetric(
                    category = category,
                    amount = amount,
                    percentage = if (total == 0.0) 0.0 else (amount * 100) / total
                )
            }

            _uiState.update {
                it.copy(
                    categoryMetrics = categoryMetrics,
                    total = total
                )
            }
        }
    }
}
