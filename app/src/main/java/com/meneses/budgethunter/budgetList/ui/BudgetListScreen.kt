package com.meneses.budgethunter.budgetList.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.application.BudgetListState
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.commons.ui.dashedBorder
import com.meneses.budgethunter.theme.AppColors
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
            showBudgetDetail = {},
            showSettings = {}
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
        showBudgetDetail: (Budget) -> Unit,
        showSettings: () -> Unit
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
                    },
                    onSettingsClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            showSettings()
                        }
                    }
                )
            },
            drawerState = drawerState
        ) {
            Scaffold(
                topBar = {
                    if (uiState.isSearchMode) {
                        SearchAppBar(
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = { query ->
                                BudgetListEvent
                                    .UpdateSearchQuery(query)
                                    .run(onEvent)
                            },
                            onBackClick = {
                                BudgetListEvent
                                    .ToggleSearchMode(false)
                                    .run(onEvent)
                            }
                        )
                    } else {
                        AppBar(
                            title = stringResource(id = R.string.budgets),
                            leftButtonIcon = Icons.Default.Menu,
                            rightButtonIcon = Icons.Default.Search,
                            leftButtonDescription = stringResource(id = R.string.open_menu_button),
                            rightButtonDescription = stringResource(id = R.string.search),
                            onLeftButtonClick = {
                                coroutineScope.launch {
                                    drawerState.open()
                                }
                            },
                            onRightButtonClick = {
                                BudgetListEvent
                                    .ToggleSearchMode(true)
                                    .run(onEvent)
                            },
                            animateRightButton = uiState.filter != null
                        )
                    }
                },
                snackbarHost = {
                    SnackbarHost(hostState = snackBarHostState)
                },
                floatingActionButton = {
                    FloatingActionButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .dashedBorder(
                                width = 1.dp,
                                color = AppColors.onSecondaryContainer,
                                shape = AbsoluteRoundedCornerShape(15.dp),
                                on = 10.dp,
                                off = 8.dp
                            ),
                        elevation = FloatingActionButtonDefaults.elevation(5.dp),
                        onClick = {
                            BudgetListEvent
                                .ToggleAddModal(true)
                                .run(onEvent)
                        }
                    ) {
                        Icon(
                            modifier = Modifier.padding(20.dp),
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.create_new_budget)
                        )
                    }
                },
                floatingActionButtonPosition = FabPosition.Center
            ) { paddingValues ->
                BudgetListContent(
                    list = uiState.budgetList,
                    isLoading = uiState.isLoading,
                    paddingValues = paddingValues,
                    onEvent = onEvent
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
