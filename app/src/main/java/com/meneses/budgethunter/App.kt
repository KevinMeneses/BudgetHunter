package com.meneses.budgethunter

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import com.meneses.budgethunter.budgetDetail.BudgetDetailViewModel
import com.meneses.budgethunter.budgetDetail.ui.BudgetDetailScreen
import com.meneses.budgethunter.budgetEntry.BudgetEntryViewModel
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.ui.BudgetEntryScreen
import com.meneses.budgethunter.budgetList.BudgetListViewModel
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.ui.BudgetListScreen
import com.meneses.budgethunter.budgetMetrics.BudgetMetricsViewModel
import com.meneses.budgethunter.budgetMetrics.ui.BudgetMetricsScreen
import com.meneses.budgethunter.commons.util.serializableType
import com.meneses.budgethunter.settings.SettingsViewModel
import com.meneses.budgethunter.settings.ui.SettingsScreen
import com.meneses.budgethunter.splash.SplashScreen
import com.meneses.budgethunter.splash.SplashScreenViewModel
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.BudgetHunterTheme
import org.koin.androidx.compose.koinViewModel
import kotlin.reflect.typeOf

@Composable
fun App() {
    BudgetHunterTheme {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppColors.background
        ) {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = SplashScreen
            ) {
                composable<SplashScreen> {
                    val splashScreenViewModel: SplashScreenViewModel = koinViewModel()
                    SplashScreen.Show(
                        uiState = splashScreenViewModel.uiState.collectAsStateWithLifecycle().value,
                        onEvent = splashScreenViewModel::sendEvent,
                        showBudgetList = {
                            navController.navigate(
                                route = BudgetListScreen,
                                navOptions = navOptions {
                                    popUpTo<SplashScreen> { inclusive = true }
                                }
                            )
                        }
                    )
                }

                composable<BudgetListScreen> {
                    val budgetListViewModel: BudgetListViewModel = koinViewModel()
                    BudgetListScreen.Show(
                        uiState = budgetListViewModel.uiState.collectAsStateWithLifecycle().value,
                        onEvent = budgetListViewModel::sendEvent,
                        showBudgetDetail = { budget ->
                            navController.navigate(BudgetDetailScreen(budget))
                        },
                        showSettings = { navController.navigate(SettingsScreen) }
                    )
                }

                composable<SettingsScreen> {
                    val settingsViewModel: SettingsViewModel = koinViewModel()
                    SettingsScreen.Show(
                        uiState = settingsViewModel.uiState.collectAsStateWithLifecycle().value,
                        onEvent = settingsViewModel::sendEvent,
                        goBack = navController::popBackStack
                    )
                }

                composable<BudgetDetailScreen>(
                    typeMap = mapOf(typeOf<Budget>() to serializableType<Budget>())
                ) {
                    val route = it.toRoute<BudgetDetailScreen>()
                    val budgetDetailViewModel: BudgetDetailViewModel = koinViewModel()
                    route.Show(
                        uiState = budgetDetailViewModel.uiState.collectAsStateWithLifecycle().value,
                        onEvent = budgetDetailViewModel::sendEvent,
                        goBack = navController::popBackStack,
                        showBudgetEntry = { budgetEntry ->
                            navController.navigate(BudgetEntryScreen(budgetEntry))
                        },
                        showBudgetMetrics = { budget ->
                            navController.navigate(BudgetMetricsScreen(budget))
                        },
                        showSettings = { navController.navigate(SettingsScreen) }
                    )
                }

                composable<BudgetEntryScreen>(
                    typeMap = mapOf(typeOf<BudgetEntry>() to serializableType<BudgetEntry>())
                ) {
                    val route = it.toRoute<BudgetEntryScreen>()
                    val budgetEntryViewModel: BudgetEntryViewModel = koinViewModel()
                    route.Show(
                        uiState = budgetEntryViewModel.uiState.collectAsStateWithLifecycle().value,
                        onEvent = budgetEntryViewModel::sendEvent,
                        goBack = navController::popBackStack
                    )
                }

                composable<BudgetMetricsScreen>(
                    typeMap = mapOf(typeOf<Budget>() to serializableType<Budget>())
                ) {
                    val route = it.toRoute<BudgetMetricsScreen>()
                    val budgetMetricsViewModel: BudgetMetricsViewModel = koinViewModel()
                    route.Show(
                        uiState = budgetMetricsViewModel.uiState.collectAsStateWithLifecycle().value,
                        goBack = navController::popBackStack
                    )
                }
            }
        }
    }
}
