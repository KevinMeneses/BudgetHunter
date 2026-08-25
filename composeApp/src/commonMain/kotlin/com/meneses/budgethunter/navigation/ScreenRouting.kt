package com.meneses.budgethunter.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.new_entry_from_collaborator
import budgethunter.composeapp.generated.resources.signed_in_as
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import androidx.navigation.toRoute
import com.meneses.budgethunter.auth.SignInViewModel
import com.meneses.budgethunter.auth.SignUpViewModel
import com.meneses.budgethunter.auth.application.SignInEvent
import com.meneses.budgethunter.auth.application.SignUpEvent
import com.meneses.budgethunter.auth.ui.SignInScreen
import com.meneses.budgethunter.auth.ui.SignUpScreen
import com.meneses.budgethunter.budgetDetail.BudgetDetailViewModel
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetDetail.ui.BudgetDetailScreen
import com.meneses.budgethunter.budgetEntry.BudgetEntryViewModel
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryEvent
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.ui.BudgetEntryScreen
import com.meneses.budgethunter.budgetList.BudgetListViewModel
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.ui.BudgetListScreen
import com.meneses.budgethunter.budgetMetrics.BudgetMetricsViewModel
import com.meneses.budgethunter.budgetMetrics.ui.BudgetMetricsScreen
import com.meneses.budgethunter.collaborator.CollaboratorsViewModel
import com.meneses.budgethunter.collaborator.application.CollaboratorsEvent
import com.meneses.budgethunter.collaborator.ui.CollaboratorsScreen
import com.meneses.budgethunter.commons.platform.NetworkMonitor
import com.meneses.budgethunter.commons.util.serializableType
import com.meneses.budgethunter.settings.SettingsViewModel
import com.meneses.budgethunter.settings.ui.SettingsScreen
import com.meneses.budgethunter.splash.SplashScreenViewModel
import com.meneses.budgethunter.splash.application.SplashEvent
import com.meneses.budgethunter.splash.ui.SplashScreen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
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
        var signedInEmail by remember { mutableStateOf<String?>(null) }

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

                LaunchedEffect(Unit) {
                    splashScreenViewModel.events.collect { event ->
                        when (event) {
                            is SplashEvent.NavigateToSignIn -> navController.navigate(
                                route = SignInScreen,
                                navOptions = navOptions {
                                    popUpTo<SplashScreen> { inclusive = true }
                                }
                            )
                            is SplashEvent.NavigateToBudgetList -> navController.navigate(
                                route = BudgetListScreen,
                                navOptions = navOptions {
                                    popUpTo<SplashScreen> { inclusive = true }
                                }
                            )
                        }
                    }
                }

                SplashScreen.Show(
                    uiState = uiState,
                    onIntent = splashScreenViewModel::sendIntent
                )
            }

            composable<SignInScreen> { backStackEntry ->
                val signInViewModel: SignInViewModel = koinInject()
                val uiState by signInViewModel.uiState.collectAsStateWithLifecycle()

                val cameFromSignUp = rememberSaveable {
                    navController.previousBackStackEntry != null
                }

                LaunchedEffect(Unit) {
                    signInViewModel.events.collect { event ->
                        when (event) {
                            is SignInEvent.NavigateToBudgetList -> {
                                signedInEmail = event.email
                                navController.navigate(
                                    route = BudgetListScreen,
                                    navOptions = navOptions {
                                        popUpTo<SignInScreen> { inclusive = true }
                                    }
                                )
                            }
                        }
                    }
                }

                SignInScreen.Show(
                    uiState = uiState,
                    onIntent = signInViewModel::sendIntent,
                    navigateToSignUp = { navController.navigateOnce(SignUpScreen) },
                    canNavigateBack = cameFromSignUp,
                    onNavigateBack = { navController.popBackStackOnce(backStackEntry) }
                )
            }

            composable<SignUpScreen> { backStackEntry ->
                val signUpViewModel: SignUpViewModel = koinInject()
                val uiState by signUpViewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    signUpViewModel.events.collect { event ->
                        when (event) {
                            is SignUpEvent.NavigateToSignIn -> navController.popBackStackOnce(backStackEntry)
                        }
                    }
                }

                SignUpScreen.Show(
                    uiState = uiState,
                    onIntent = signUpViewModel::sendIntent
                )
            }

            composable<BudgetListScreen> {
                val budgetListViewModel: BudgetListViewModel = koinInject()
                val uiState by budgetListViewModel.uiState.collectAsStateWithLifecycle()
                val networkMonitor: NetworkMonitor = koinInject()
                val snackbarHostState = remember { SnackbarHostState() }
                var messageResource by remember { mutableStateOf<StringResource?>(null) }

                LaunchedEffect(Unit) {
                    budgetListViewModel.events.collect { event ->
                        when (event) {
                            is BudgetListEvent.NavigateToBudget -> navController.navigateOnce(
                                BudgetDetailScreen(event.budget)
                            )
                            is BudgetListEvent.NavigateToSignIn -> navController.navigate(
                                route = SignInScreen,
                                navOptions = navOptions {
                                    popUpTo<BudgetListScreen> { inclusive = true }
                                }
                            )
                            is BudgetListEvent.ShowMessage -> messageResource = event.message
                        }
                    }
                }

                messageResource?.let { res ->
                    val message = stringResource(res)
                    LaunchedEffect(res) {
                        snackbarHostState.showSnackbar(message)
                        messageResource = null
                    }
                }

                signedInEmail?.let { email ->
                    if (email.isNotBlank()) {
                        val message = stringResource(Res.string.signed_in_as, email)
                        LaunchedEffect(email) {
                            snackbarHostState.showSnackbar(message)
                            signedInEmail = null
                        }
                    } else {
                        signedInEmail = null
                    }
                }

                BudgetListScreen.Show(
                    uiState = uiState,
                    onIntent = budgetListViewModel::sendIntent,
                    showSettings = { navController.navigateOnce(SettingsScreen) },
                    networkMonitor = networkMonitor,
                    snackbarHostState = snackbarHostState
                )
            }

            composable<SettingsScreen> { backStackEntry ->
                val settingsViewModel: SettingsViewModel = koinInject()
                val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()

                SettingsScreen.Show(
                    uiState = uiState,
                    onIntent = settingsViewModel::sendIntent,
                    goBack = { navController.popBackStackOnce(backStackEntry) }
                )
            }

            composable<BudgetDetailScreen>(
                typeMap = mapOf(typeOf<Budget>() to serializableType<Budget>())
            ) { backStackEntry ->
                val budgetDetailRoute = backStackEntry.toRoute<BudgetDetailScreen>()
                val budgetDetailViewModel: BudgetDetailViewModel = koinInject()
                val uiState by budgetDetailViewModel.uiState.collectAsStateWithLifecycle()
                val networkMonitor: NetworkMonitor = koinInject()
                val snackbarHostState = remember { SnackbarHostState() }
                var errorResource by remember { mutableStateOf<StringResource?>(null) }
                var successResource by remember { mutableStateOf<StringResource?>(null) }
                var pendingCollaboratorName by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    budgetDetailViewModel.events.collect { event ->
                        when (event) {
                            is BudgetDetailEvent.NavigateBack -> navController.popBackStackOnce(backStackEntry)
                            is BudgetDetailEvent.ShowEntry -> navController.navigateOnce(
                                BudgetEntryScreen(event.entry)
                            )
                            is BudgetDetailEvent.ShowError -> errorResource = event.message
                            is BudgetDetailEvent.ShowSuccess -> successResource = event.message
                            is BudgetDetailEvent.ShowCollaboratorEntry -> pendingCollaboratorName = event.collaboratorName
                        }
                    }
                }

                errorResource?.let { res ->
                    val message = stringResource(res)
                    LaunchedEffect(res) {
                        snackbarHostState.showSnackbar(message)
                        errorResource = null
                    }
                }

                successResource?.let { res ->
                    val message = stringResource(res)
                    LaunchedEffect(res) {
                        snackbarHostState.showSnackbar(message)
                        successResource = null
                    }
                }

                pendingCollaboratorName?.let { name ->
                    val message = stringResource(Res.string.new_entry_from_collaborator, name)
                    LaunchedEffect(name) {
                        snackbarHostState.showSnackbar(message)
                        pendingCollaboratorName = null
                    }
                }

                budgetDetailRoute.Show(
                    uiState = uiState,
                    onIntent = budgetDetailViewModel::sendIntent,
                    snackbarHostState = snackbarHostState,
                    goBack = { navController.popBackStackOnce(backStackEntry) },
                    showBudgetMetrics = { budget ->
                        navController.navigateOnce(BudgetMetricsScreen(budget))
                    },
                    showSettings = { navController.navigateOnce(SettingsScreen) },
                    showCollaborators = { serverId, budgetName ->
                        navController.navigateOnce(CollaboratorsScreen(serverId, budgetName))
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
                val snackbarHostState = remember { SnackbarHostState() }
                var pendingNotification by remember { mutableStateOf<StringResource?>(null) }

                LaunchedEffect(Unit) {
                    budgetEntryViewModel.events.collect { event ->
                        when (event) {
                            is BudgetEntryEvent.NavigateBack -> navController.popBackStackOnce(backStackEntry)
                            is BudgetEntryEvent.ShowNotification -> pendingNotification = event.message
                        }
                    }
                }

                pendingNotification?.let { res ->
                    val message = stringResource(res)
                    LaunchedEffect(res) {
                        snackbarHostState.showSnackbar(message)
                        pendingNotification = null
                    }
                }

                budgetEntryRoute.Show(
                    uiState = uiState,
                    onIntent = budgetEntryViewModel::sendIntent,
                    snackbarHostState = snackbarHostState
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
                    goBack = { navController.popBackStackOnce(backStackEntry) }
                )
            }

            composable<CollaboratorsScreen> { backStackEntry ->
                val collaboratorsRoute = backStackEntry.toRoute<CollaboratorsScreen>()
                val collaboratorsViewModel: CollaboratorsViewModel = koinInject(
                    parameters = { parametersOf(collaboratorsRoute.budgetServerId) }
                )
                val uiState by collaboratorsViewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(Unit) {
                    collaboratorsViewModel.events.collect { event ->
                        when (event) {
                            is CollaboratorsEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                            is CollaboratorsEvent.ShowSuccess -> snackbarHostState.showSnackbar(event.message)
                        }
                    }
                }

                collaboratorsRoute.Show(
                    uiState = uiState,
                    onIntent = collaboratorsViewModel::sendIntent,
                    snackbarHostState = snackbarHostState,
                    goBack = { navController.popBackStackOnce(backStackEntry) }
                )
            }
        }
    }
}
