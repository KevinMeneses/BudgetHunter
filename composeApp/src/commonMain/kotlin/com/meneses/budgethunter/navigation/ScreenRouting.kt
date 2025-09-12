package com.meneses.budgethunter.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.meneses.budgethunter.budgetList.BudgetListViewModel
import com.meneses.budgethunter.budgetList.ui.BudgetListScreen
import com.meneses.budgethunter.settings.SettingsViewModel
import com.meneses.budgethunter.settings.ui.SettingsScreen
import com.meneses.budgethunter.theme.AppColors
import org.koin.compose.koinInject

@Composable
fun BudgetHunterNavigation() {
    val navController = rememberNavController()
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppColors.background
    ) {
        NavHost(
            navController = navController,
            startDestination = "budget_list"
        ) {
            composable("budget_list") {
                val budgetListViewModel: BudgetListViewModel = koinInject()
                val uiState by budgetListViewModel.uiState.collectAsState()
                
                BudgetListScreen(
                    uiState = uiState,
                    onEvent = budgetListViewModel::sendEvent,
                    showBudgetDetail = { budget ->
                        // TODO: Navigate to budget detail once BudgetDetailViewModel is migrated
                    },
                    showSettings = { 
                        navController.navigate("settings")
                    }
                )
            }
            
            composable("settings") {
                val settingsViewModel: SettingsViewModel = koinInject()
                val uiState by settingsViewModel.uiState.collectAsState()
                
                SettingsScreen.Show(
                    uiState = uiState,
                    onEvent = settingsViewModel::sendEvent,
                    goBack = { navController.popBackStack() }
                )
            }
        }
    }
}
