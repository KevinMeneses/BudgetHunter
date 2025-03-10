package com.meneses.budgethunter.budgetMetrics.application

import androidx.compose.ui.graphics.Color
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

data class BudgetMetricsState(
    val metricsData: Map<BudgetEntry.Category, Double> = emptyMap(),
    val percentages: List<Double> = listOf(),
    val chartColors: List<Color> = emptyList()
)
