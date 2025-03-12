package com.meneses.budgethunter.budgetList.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.application.BudgetListState
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.theme.BudgetHunterTheme
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetHunterTheme {
        BudgetListScreen.Show(
            uiState = BudgetListState(
                budgetList = listOf(
                    Budget(name = "Test")
                )
            ),
            onEvent = {},
            showUserGuide = {},
            showBudgetDetail = {}
        )
    }
}

@Serializable
object BudgetListScreen {
    @Composable
    fun Show(
        uiState: BudgetListState,
        onEvent: (BudgetListEvent) -> Unit,
        showUserGuide: () -> Unit,
        showBudgetDetail: (Budget) -> Unit
    ) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val snackBarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        ModalNavigationDrawer(
            drawerContent = {
                BudgetListMenu(
                    isCollaborationActive = uiState.isCollaborationActive,
                    onCollaborateClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            BudgetListEvent
                                .ToggleJoinCollaborationModal(true)
                                .run(onEvent)
                        }
                    },
                    onUserGuideClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            showUserGuide()
                        }
                    }
                )
            },
            drawerState = drawerState
        ) {
            Scaffold(
                topBar = {
                    AppBar(
                        title = stringResource(id = R.string.budgets),
                        leftButtonIcon = Icons.Default.Menu,
                        rightButtonIcon = Icons.Default.Search,
                        leftButtonDescription = stringResource(id = R.string.open_menu_button),
                        rightButtonDescription = stringResource(id = R.string.filter),
                        onLeftButtonClick = {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        },
                        onRightButtonClick = fun() {
                            BudgetListEvent
                                .ToggleFilterModal(true)
                                .run(onEvent)
                        },
                        animateRightButton = uiState.filter != null
                    )
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackBarHostState)
                }
            ) {
                BudgetListContent(
                    list = uiState.budgetList,
                    isLoading = uiState.isLoading,
                    paddingValues = it,
                    onEvent = onEvent,
                    animate = uiState.budgetList.isEmpty() && uiState.filter == null
                )
            }

            NewBudgetModal(
                show = uiState.addModalVisibility,
                onEvent = onEvent
            )

            UpdateBudgetModal(
                budget = uiState.budgetToUpdate,
                onEvent = onEvent
            )

            FilterListModal(
                show = uiState.filterModalVisibility,
                filter = uiState.filter,
                onEvent = onEvent
            )

            JoinCollaborationModal(
                show = uiState.joinCollaborationModalVisibility,
                onEvent = onEvent
            )
        }

        LaunchedEffect(key1 = uiState.navigateToBudget) {
            uiState.navigateToBudget?.let { showBudgetDetail(it) }
        }

        LaunchedEffect(key1 = uiState.collaborationError) {
            uiState.collaborationError?.let { snackBarHostState.showSnackbar(it) }
        }

        DisposableEffect(key1 = Unit) {
            onDispose {
                BudgetListEvent
                    .ClearNavigation
                    .run(onEvent)
            }
        }
    }
}

