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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.budgets
import budgethunter.composeapp.generated.resources.create_new_budget
import budgethunter.composeapp.generated.resources.open_menu
import budgethunter.composeapp.generated.resources.search
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.application.BudgetListState
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.commons.ui.dashedBorder
import com.meneses.budgethunter.theme.AppColors
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
object BudgetListScreen {
    @Composable
    fun Show(
        uiState: BudgetListState,
        onEvent: (BudgetListEvent) -> Unit,
        showBudgetDetail: (Budget) -> Unit,
        showSettings: () -> Unit
    ) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val snackBarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalNavigationDrawer(
                drawerContent = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        BudgetListMenu(
                            onSettingsClick = {
                                coroutineScope.launch {
                                    drawerState.close()
                                    showSettings()
                                }
                            }
                        )
                    }
                },
                drawerState = drawerState
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                                    title = stringResource(Res.string.budgets),
                                    leftButtonIcon = Icons.Default.Search,
                                    rightButtonIcon = Icons.Default.Menu,
                                    leftButtonDescription = stringResource(Res.string.search),
                                    rightButtonDescription = stringResource(Res.string.open_menu),
                                    onLeftButtonClick = {
                                        BudgetListEvent
                                            .ToggleSearchMode(true)
                                            .run(onEvent)
                                    },
                                    onRightButtonClick = {
                                        coroutineScope.launch {
                                            drawerState.open()
                                        }
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
                                    contentDescription = stringResource(Res.string.create_new_budget)
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
                }
            }
        }

        LaunchedEffect(key1 = uiState.navigateToBudget) {
            uiState.navigateToBudget?.let { showBudgetDetail(it) }
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

