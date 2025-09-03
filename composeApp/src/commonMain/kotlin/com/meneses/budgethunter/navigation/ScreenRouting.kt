package com.meneses.budgethunter.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.meneses.budgethunter.budgetList.BudgetListViewModel
import com.meneses.budgethunter.budgetList.ui.BudgetListScreen
import org.koin.compose.koinInject

@Composable
fun BudgetHunterNavigation() {
    // Temporarily simplified - just show BudgetListScreen directly
    // TODO: Implement proper navigation once iOS build issues are resolved
    val viewModel: BudgetListViewModel = koinInject()
    val uiState by viewModel.uiState.collectAsState()
    
    BudgetListScreen(
        uiState = uiState,
        onEvent = viewModel::sendEvent,
        showBudgetDetail = { budget ->
            // TODO: Navigate to budget detail
        },
        showSettings = {
            // TODO: Navigate to settings  
        }
    )
}
