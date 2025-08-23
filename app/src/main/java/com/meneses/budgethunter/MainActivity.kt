package com.meneses.budgethunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.meneses.budgethunter.settings.ui.SettingsScreen
import com.meneses.budgethunter.settings.SettingsViewModel
import com.meneses.budgethunter.splash.SplashScreen
import com.meneses.budgethunter.splash.SplashScreenViewModel
import com.meneses.budgethunter.theme.AppColors
import com.meneses.budgethunter.theme.BudgetHunterTheme
import com.meneses.budgethunter.userGuide.UserGuideScreen
import kotlin.reflect.typeOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
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
                            val splashScreenViewModel: SplashScreenViewModel = viewModel()
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
                            val budgetListViewModel: BudgetListViewModel = viewModel()
                            BudgetListScreen.Show(
                                uiState = budgetListViewModel.uiState.collectAsStateWithLifecycle().value,
                                onEvent = budgetListViewModel::sendEvent,
                                showUserGuide = { navController.navigate(UserGuideScreen) },
                                showBudgetDetail = { budget ->
                                    navController.navigate(BudgetDetailScreen(budget))
                                },
                                showSettings = { navController.navigate(SettingsScreen) }
                            )
                        }

                        composable<UserGuideScreen> {
                            UserGuideScreen.Show(goBack = navController::popBackStack)
                        }

                        composable<SettingsScreen> {
                            val settingsViewModel: SettingsViewModel = viewModel()
                            val context = LocalContext.current
                            LaunchedEffect(Unit) { settingsViewModel.loadSettings(context) }
                            SettingsScreen.Show(
                                uiState = settingsViewModel.uiState.collectAsStateWithLifecycle().value,
                                onEvent = settingsViewModel::sendEvent,
                                goBack = navController::popBackStack
                            )
                        }

                        composable<BudgetDetailScreen>(
                            typeMap = mapOf(typeOf<Budget>() to serializableType<Budget>())
                        ) {
                            val budgetDetailViewModel: BudgetDetailViewModel = viewModel()
                            it.toRoute<BudgetDetailScreen>().Show(
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
                            val budgetEntryViewModel: BudgetEntryViewModel = viewModel()
                            it.toRoute<BudgetEntryScreen>().Show(
                                uiState = budgetEntryViewModel.uiState.collectAsStateWithLifecycle().value,
                                onEvent = budgetEntryViewModel::sendEvent,
                                goBack = navController::popBackStack
                            )
                        }

                        composable<BudgetMetricsScreen>(
                            typeMap = mapOf(typeOf<Budget>() to serializableType<Budget>())
                        ) {
                            val budgetMetricsViewModel: BudgetMetricsViewModel = viewModel()
                            it.toRoute<BudgetMetricsScreen>().Show(
                                uiState = budgetMetricsViewModel.uiState.collectAsStateWithLifecycle().value,
                                goBack = navController::popBackStack
                            )
                        }
                    }
                }
            }
        }
    }
}
