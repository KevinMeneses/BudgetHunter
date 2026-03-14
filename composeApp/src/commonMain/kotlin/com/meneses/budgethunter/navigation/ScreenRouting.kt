package com.meneses.budgethunter.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import com.meneses.budgethunter.auth.SignInViewModel
import com.meneses.budgethunter.auth.SignUpViewModel
import com.meneses.budgethunter.auth.ui.SignInScreen
import com.meneses.budgethunter.auth.ui.SignUpScreen
import com.meneses.budgethunter.budgetDetail.BudgetDetailViewModel
import com.meneses.budgethunter.budgetDetail.ui.BudgetDetailScreen
import com.meneses.budgethunter.budgetEntry.BudgetEntryViewModel
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.ui.BudgetEntryScreen
import com.meneses.budgethunter.budgetList.BudgetListViewModel
import com.meneses.budgethunter.budgetList.application.BudgetListIntent
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.ui.BudgetListScreen
import com.meneses.budgethunter.budgetMetrics.BudgetMetricsViewModel
import com.meneses.budgethunter.budgetMetrics.ui.BudgetMetricsScreen
import com.meneses.budgethunter.commons.platform.NetworkMonitor
import com.meneses.budgethunter.commons.util.serializableType
import com.meneses.budgethunter.collaborator.CollaboratorsViewModel
import com.meneses.budgethunter.collaborator.ui.CollaboratorsScreen
import com.meneses.budgethunter.settings.SettingsViewModel
import com.meneses.budgethunter.settings.ui.SettingsScreen
import com.meneses.budgethunter.splash.SplashScreenViewModel
import com.meneses.budgethunter.splash.ui.SplashScreen
import androidx.compose.material3.MaterialTheme
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.reflect.typeOf

@Composable
fun BudgetHunterNavigation() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = SplashScreen,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                )
            }
        ) {
            composable<SplashScreen> {
                val splashScreenViewModel: SplashScreenViewModel = koinInject()
                val uiState by splashScreenViewModel.uiState.collectAsStateWithLifecycle()

                SplashScreen.Show(
                    uiState = uiState,
                    onIntent = splashScreenViewModel::sendIntent,
                    navigateToSignIn = {
                        navController.navigate(
                            route = SignInScreen,
                            navOptions = navOptions {
                                popUpTo<SplashScreen> { inclusive = true }
                            }
                        )
                    },
                    navigateToBudgetList = {
                        navController.navigate(
                            route = BudgetListScreen,
                            navOptions = navOptions {
                                popUpTo<SplashScreen> { inclusive = true }
                            }
                        )
                    }
                )
            }

            composable<SignInScreen> {
                val signInViewModel: SignInViewModel = koinInject()
                val uiState by signInViewModel.uiState.collectAsStateWithLifecycle()

                // Track if we came directly from splash (initial entry = false)
                // This will be saved across recompositions but reset when the screen is recreated
                val cameFromSignUp = rememberSaveable {
                    navController.previousBackStackEntry != null
                }

                SignInScreen.Show(
                    uiState = uiState,
                    onIntent = signInViewModel::sendIntent,
                    navigateToSignUp = {
                        navController.navigate(SignUpScreen)
                    },
                    navigateToBudgetList = {
                        navController.navigate(
                            route = BudgetListScreen,
                            navOptions = navOptions {
                                popUpTo<SignInScreen> { inclusive = true }
                            }
                        )
                    },
                    canNavigateBack = cameFromSignUp,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<SignUpScreen> {
                val signUpViewModel: SignUpViewModel = koinInject()
                val uiState by signUpViewModel.uiState.collectAsStateWithLifecycle()

                SignUpScreen.Show(
                    uiState = uiState,
                    onIntent = signUpViewModel::sendIntent,
                    navigateToSignIn = {
                        navController.popBackStack()
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable<BudgetListScreen> {
                val budgetListViewModel: BudgetListViewModel = koinInject()
                val uiState by budgetListViewModel.uiState.collectAsStateWithLifecycle()
                val networkMonitor: NetworkMonitor = koinInject()

                // Handle navigation to sign in
                LaunchedEffect(uiState.navigateToSignIn) {
                    if (uiState.navigateToSignIn) {
                        navController.navigate(
                            route = SignInScreen,
                            navOptions = navOptions {
                                popUpTo<BudgetListScreen> { inclusive = true }
                            }
                        )
                        budgetListViewModel.sendIntent(BudgetListIntent.ClearSignInNavigation)
                    }
                }

                BudgetListScreen.Show(
                    uiState = uiState,
                    onIntent = budgetListViewModel::sendIntent,
                    showBudgetDetail = { budget ->
                        navController.navigate(BudgetDetailScreen(budget))
                    },
                    showSettings = {
                        navController.navigate(SettingsScreen)
                    },
                    networkMonitor = networkMonitor
                )
            }

            composable<SettingsScreen> {
                val settingsViewModel: SettingsViewModel = koinInject()
                val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

                SettingsScreen.Show(
                    uiState = uiState,
                    onIntent = settingsViewModel::sendIntent,
                    goBack = { navController.popBackStack() }
                )
            }

            composable<BudgetDetailScreen>(
                typeMap = mapOf(typeOf<Budget>() to serializableType<Budget>())
            ) { backStackEntry ->
                val budgetDetailRoute = backStackEntry.toRoute<BudgetDetailScreen>()
                val budgetDetailViewModel: BudgetDetailViewModel = koinInject()
                val uiState by budgetDetailViewModel.uiState.collectAsStateWithLifecycle()
                val networkMonitor: NetworkMonitor = koinInject()

                budgetDetailRoute.Show(
                    uiState = uiState,
                    onIntent = budgetDetailViewModel::sendIntent,
                    goBack = { navController.popBackStack() },
                    showBudgetEntry = { budgetEntry ->
                        navController.navigate(BudgetEntryScreen(budgetEntry))
                    },
                    showBudgetMetrics = { budget ->
                        navController.navigate(BudgetMetricsScreen(budget))
                    },
                    showSettings = {
                        navController.navigate(SettingsScreen)
                    },
                    showCollaborators = { serverId, budgetName ->
                        navController.navigate(CollaboratorsScreen(serverId, budgetName))
                    },
                    networkMonitor = networkMonitor
                )
            }

            composable<BudgetEntryScreen>(
                typeMap = mapOf(typeOf<BudgetEntry>() to serializableType<BudgetEntry>())
            ) { backStackEntry ->
                val budgetEntryRoute = backStackEntry.toRoute<BudgetEntryScreen>()
                val budgetEntryViewModel: BudgetEntryViewModel = koinInject()
                val uiState by budgetEntryViewModel.uiState.collectAsStateWithLifecycle()

                budgetEntryRoute.Show(
                    uiState = uiState,
                    onIntent = budgetEntryViewModel::sendIntent,
                    goBack = { navController.popBackStack() }
                )
            }

            composable<BudgetMetricsScreen>(
                typeMap = mapOf(typeOf<Budget>() to serializableType<Budget>())
            ) { backStackEntry ->
                val budgetMetricsRoute = backStackEntry.toRoute<BudgetMetricsScreen>()
                val budgetMetricsViewModel: BudgetMetricsViewModel = koinInject()
                val uiState by budgetMetricsViewModel.uiState.collectAsStateWithLifecycle()

                budgetMetricsRoute.Show(
                    uiState = uiState,
                    goBack = { navController.popBackStack() }
                )
            }

            composable<CollaboratorsScreen> { backStackEntry ->
                val collaboratorsRoute = backStackEntry.toRoute<CollaboratorsScreen>()
                val collaboratorsViewModel: CollaboratorsViewModel = koinInject(
                    parameters = { parametersOf(collaboratorsRoute.budgetServerId) }
                )
                val uiState by collaboratorsViewModel.uiState.collectAsStateWithLifecycle()

                collaboratorsRoute.Show(
                    uiState = uiState,
                    onIntent = collaboratorsViewModel::sendIntent,
                    goBack = { navController.popBackStack() }
                )
            }
        }
    }
}
