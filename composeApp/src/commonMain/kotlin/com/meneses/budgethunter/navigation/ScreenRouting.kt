package com.meneses.budgethunter.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.meneses.budgethunter.budgetList.BudgetListViewModel
import com.meneses.budgethunter.budgetList.ui.BudgetListScreen
import com.meneses.budgethunter.theme.AppColors
import org.koin.compose.koinInject

@Composable
fun BudgetHunterNavigation() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppColors.background
    ) {
        // Simplified navigation - just show BudgetListScreen for now
        // Full navigation will be implemented once all ViewModels are migrated
        val budgetListViewModel: BudgetListViewModel = koinInject()
        val uiState by budgetListViewModel.uiState.collectAsState()
        
        BudgetListScreen(
            uiState = uiState,
            onEvent = budgetListViewModel::sendEvent,
            showBudgetDetail = { budget ->
                // TODO: Navigate to budget detail once BudgetDetailViewModel is migrated
            },
            showSettings = { 
                // TODO: Navigate to settings once SettingsScreen is migrated
            }
        )
    }
}
