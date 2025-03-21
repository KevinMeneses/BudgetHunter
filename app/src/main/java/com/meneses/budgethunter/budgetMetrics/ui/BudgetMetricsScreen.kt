package com.meneses.budgethunter.budgetMetrics.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.ui.toStringResource
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetMetrics.application.BudgetMetricsState
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.commons.ui.PieChart
import kotlinx.serialization.Serializable

@Serializable
data class BudgetMetricsScreen(val budget: Budget) {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Show(
        uiState: BudgetMetricsState,
        goBack: () -> Unit,
    ) {
        Scaffold(
            topBar = {
                AppBar(
                    title = budget.name,
                    leftButtonIcon = Icons.Default.ArrowBack,
                    leftButtonDescription = stringResource(id = R.string.come_back),
                    onLeftButtonClick = goBack
                )
            }
        ) { paddingValues ->
            BudgetMetricsContent(
                paddingValues = paddingValues,
                uiState = uiState
            )
        }
    }

    @Composable
    fun BudgetMetricsContent(
        paddingValues: PaddingValues,
        uiState: BudgetMetricsState
    ) {
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            PieChart(
                data = uiState.metricsData.mapKeys { it.key.toStringResource() },
                percentages = uiState.percentages,
                colors = uiState.chartColors,
                modifier = Modifier.padding(20.dp)
            )
        }
    }
}
