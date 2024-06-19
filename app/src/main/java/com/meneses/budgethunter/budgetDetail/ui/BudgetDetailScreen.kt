package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.ui.AppBar
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetDetailScreen(Budget()).Show(
        uiState = BudgetDetailState(),
        onEvent = {},
        goBack = {},
        showBudgetEntry = {}
    )
}

@Serializable
data class BudgetDetailScreen(val budget: Budget) {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Show(
        uiState: BudgetDetailState,
        onEvent: (BudgetDetailEvent) -> Unit,
        goBack: () -> Unit,
        showBudgetEntry: (BudgetEntry) -> Unit
    ) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val snackBarHostState = remember { SnackbarHostState() }
        val coroutineScope = rememberCoroutineScope()

        DisposableEffect(Unit) {
            if (uiState.budget.id != budget.id) {
                BudgetDetailEvent
                    .SetBudget(budget)
                    .run(onEvent)
            }

            BudgetDetailEvent
                .GetBudgetEntries
                .run(onEvent)

            onDispose {
                BudgetDetailEvent
                    .ClearNavigation
                    .run(onEvent)
            }
        }

        ModalNavigationDrawer(
            drawerContent = {
                BudgetDetailMenu(
                    animateFilterButton = uiState.filter != null,
                    animateCollaborateButton = uiState.isCollaborationActive,
                    onFilterClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            BudgetDetailEvent
                                .ToggleFilterModal(true)
                                .run(onEvent)
                        }
                    },
                    onCollaborateClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            if (uiState.isCollaborationActive) {
                                BudgetDetailEvent
                                    .StopCollaboration
                                    .run(onEvent)
                            } else {
                                BudgetDetailEvent
                                    .ToggleCollaborateModal(true)
                                    .run(onEvent)
                            }
                        }
                    },
                    onDeleteClick = fun() {
                        coroutineScope.launch {
                            drawerState.close()
                            BudgetDetailEvent
                                .ToggleDeleteBudgetModal(true)
                                .run(onEvent)
                        }
                    }
                )
            },
            drawerState = drawerState
        ) {
            Scaffold(
                topBar = {
                    AppBar(
                        title = budget.name,
                        leftButtonIcon = Icons.Default.Menu,
                        rightButtonIcon = Icons.Default.Add,
                        leftButtonDescription = stringResource(id = R.string.open_menu_button),
                        rightButtonDescription = stringResource(R.string.create_budget_entry),
                        onLeftButtonClick = {
                            coroutineScope.launch {
                                drawerState.open()
                            }
                        },
                        onRightButtonClick = {
                            val budgetEntry = BudgetEntry(budgetId = budget.id)
                            BudgetDetailEvent
                                .ShowEntry(budgetEntry)
                                .run(onEvent)
                        },
                        animateLeftButton = uiState.filter != null
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
                budgetAmount = uiState.budget.amount,
                onEvent = onEvent
            )

            FilterModal(
                show = uiState.isFilterModalVisible,
                filter = uiState.filter,
                onEvent = onEvent
            )

            CollaborateModal(
                show = uiState.isCollaborateModalVisible,
                onEvent = onEvent
            )

            CodeModal(
                code = uiState.collaborationCode,
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

        LaunchedEffect(key1 = uiState.goBack) {
            if (uiState.goBack) goBack()
        }

        LaunchedEffect(key1 = uiState.collaborationError) {
            uiState.collaborationError?.let { snackBarHostState.showSnackbar(it) }
        }

        LaunchedEffect(key1 = uiState.showEntry) {
            uiState.showEntry?.let { showBudgetEntry(it) }
        }
    }
}
