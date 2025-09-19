package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.back_content_description
import budgethunter.composeapp.generated.resources.open_menu
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.ui.AppBar
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
data class BudgetDetailScreen(val budget: Budget) {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Show(
        uiState: BudgetDetailState,
        onEvent: (BudgetDetailEvent) -> Unit,
        goBack: () -> Unit,
        showBudgetEntry: (BudgetEntry) -> Unit,
        showBudgetMetrics: (Budget) -> Unit,
        showSettings: () -> Unit
    ) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val snackBarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        DisposableEffect(Unit) {
            if (uiState.budgetDetail.budget.id != budget.id) {
                BudgetDetailEvent
                    .SetBudget(budget)
                    .run(onEvent)
            }

            BudgetDetailEvent
                .GetBudgetDetail
                .run(onEvent)

            onDispose {
                BudgetDetailEvent
                    .ClearNavigation
                    .run(onEvent)
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalNavigationDrawer(
                drawerContent = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        BudgetDetailMenu(
                            animateFilterButton = uiState.filter != null,
                            onFilterClick = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    BudgetDetailEvent
                                        .ToggleFilterModal(true)
                                        .run(onEvent)
                                }
                            },
                            onMetricsClick = {
                                showBudgetMetrics(budget)
                            },
                            onDeleteClick = fun() {
                                coroutineScope.launch {
                                    drawerState.close()
                                    BudgetDetailEvent
                                        .ToggleDeleteBudgetModal(true)
                                        .run(onEvent)
                                }
                            },
                            onSettingsClick = {
                                coroutineScope.launch {
                                    drawerState.close()
                                }
                                showSettings()
                            }
                        )
                    }
                },
                drawerState = drawerState
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Scaffold(
                        topBar = {
                            AppBar(
                                title = uiState.budgetDetail.budget.name,
                                leftButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
                                leftButtonDescription = stringResource(Res.string.back_content_description),
                                rightButtonIcon = Icons.Default.Menu,
                                rightButtonDescription = stringResource(Res.string.open_menu),
                                onLeftButtonClick = goBack,
                                onRightButtonClick = {
                                    coroutineScope.launch {
                                        drawerState.open()
                                    }
                                },
                                animateRightButton = uiState.filter != null
                            )
                        },
                        snackbarHost = {
                            SnackbarHost(hostState = snackBarHostState)
                        }
                    ) { paddingValues ->
                        BudgetDetailContent(
                            paddingValues = paddingValues,
                            uiState = uiState,
                            onEvent = onEvent
                        )
                    }

                    BudgetModal(
                        show = uiState.isBudgetModalVisible,
                        budgetAmount = uiState.budgetDetail.budget.amount,
                        onEvent = onEvent
                    )

                    FilterModal(
                        show = uiState.isFilterModalVisible,
                        filter = uiState.filter,
                        onEvent = onEvent
                    )

                    DeleteBudgetConfirmationModal(
                        show = uiState.isDeleteBudgetModalVisible,
                        onEvent = onEvent
                    )

                    DeleteEntriesConfirmationModal(
                        show = uiState.isDeleteEntriesModalVisible,
                        onEvent = onEvent
                    )
                }
            }
        }

        LaunchedEffect(key1 = uiState.goBack) {
            if (uiState.goBack) goBack()
        }

        LaunchedEffect(key1 = uiState.showEntry) {
            uiState.showEntry?.let { showBudgetEntry(it) }
        }
    }
}
