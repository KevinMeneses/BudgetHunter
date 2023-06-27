package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetDetail.BudgetDetailViewModel
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.destinations.BudgetEntryScreenDestination
import com.meneses.budgethunter.commons.utils.fakeNavigation
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetDetailScreen(fakeNavigation, Budget())
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun BudgetDetailScreen(
    navigator: DestinationsNavigator,
    budget: Budget,
    myViewModel: BudgetDetailViewModel = viewModel()
) {
    val uiState by myViewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        if (uiState.budget.id != budget.id) {
            BudgetDetailEvent
                .SetBudget(budget)
                .run(myViewModel::sendEvent)
        }

        BudgetDetailEvent
            .GetBudgetEntries
            .run(myViewModel::sendEvent)

        onDispose {
            BudgetDetailEvent
                .ClearNavigation
                .run(myViewModel::sendEvent)
        }
    }

    ModalNavigationDrawer(
        drawerContent = {
            BudgetDetailMenu(
                animateFilterButton = uiState.filter != null,
                onFilterClick = fun() {
                    coroutineScope.launch {
                        drawerState.close()
                        BudgetDetailEvent
                            .ToggleFilterModal(true)
                            .run(myViewModel::sendEvent)
                    }
                },
                onDeleteClick = fun() {
                    coroutineScope.launch {
                        drawerState.close()
                        BudgetDetailEvent
                            .ToggleDeleteBudgetModal(true)
                            .run(myViewModel::sendEvent)
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
                    leftButtonDescription = stringResource(id = R.string.open_menu_button),
                    rightButtonIcon = Icons.Default.Add,
                    rightButtonDescription = stringResource(R.string.create_budget_entry),
                    onLeftButtonClick = fun() {
                        coroutineScope.launch {
                            drawerState.open()
                        }
                    },
                    onRightButtonClick = fun() {
                        val budgetEntry = BudgetEntry(budgetId = budget.id)
                        BudgetDetailEvent
                            .ShowEntry(budgetEntry)
                            .run(myViewModel::sendEvent)
                    },
                    animateLeftButton = uiState.filter != null
                )
            }
        ) { paddingValues ->
            BudgetDetailContent(
                paddingValues = paddingValues,
                uiState = uiState,
                onEvent = myViewModel::sendEvent
            )
        }

        BudgetModal(
            show = uiState.isBudgetModalVisible,
            budgetAmount = uiState.budget.amount,
            onEvent = myViewModel::sendEvent
        )

        FilterModal(
            show = uiState.isFilterModalVisible,
            filter = uiState.filter,
            onEvent = myViewModel::sendEvent
        )

        DeleteBudgetConfirmationModal(
            show = uiState.isDeleteBudgetModalVisible,
            onEvent = myViewModel::sendEvent
        )

        DeleteEntriesConfirmationModal(
            show = uiState.isDeleteEntriesModalVisible,
            onEvent = myViewModel::sendEvent
        )
    }

    if (uiState.goBack) navigator.popBackStack()

    uiState.showEntry?.let {
        val destination = BudgetEntryScreenDestination(it)
        navigator.navigate(destination)
    }
}
