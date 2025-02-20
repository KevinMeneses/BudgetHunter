package com.meneses.budgethunter.budgetMetrics

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.budgetMetrics.application.BudgetMetricsState
import com.meneses.budgethunter.budgetMetrics.application.GetTotalsPerCategoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetMetricsViewModel(
    private val getTotalsPerCategoryUseCase: GetTotalsPerCategoryUseCase = GetTotalsPerCategoryUseCase()
): ViewModel() {
    private val _uiState = MutableStateFlow(BudgetMetricsState())
    val uiState = _uiState.asStateFlow()

    init {
        getMetrics()
    }

    private fun getMetrics() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    metricsData = getTotalsPerCategoryUseCase.execute(),
                    chartColors = listOf(
                        Color.Black,
                        Color.LightGray,
                        Color.Red,
                        Color.Blue,
                        Color.Cyan,
                        Color.Gray,
                        Color.Green,
                        Color.Magenta,
                        Color.White,
                        Color.Yellow,
                        Color.DarkGray
                    )
                )
            }
        }
    }
}
