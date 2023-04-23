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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meneses.budgethunter.budgetDetail.BudgetDetailViewModel
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.destinations.BudgetEntryScreenDestination
import com.meneses.budgethunter.fakeNavigation
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

    LaunchedEffect(Unit) {
        if (uiState.budget.id != budget.id) {
            myViewModel.setBudget(budget)
        }

        myViewModel.getBudgetEntries()
    }

    ModalNavigationDrawer(
        drawerContent = {
            BudgetDetailMenu(
                onFilterClick = fun() {
                    coroutineScope.launch {
                        drawerState.close()
                        myViewModel.showFilterModal()
                    }
                },
                onDeleteClick = fun() {
                    coroutineScope.launch {
                        drawerState.close()
                        myViewModel.showDeleteModal()
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
                    onLeftButtonClick = fun() {
                        coroutineScope.launch {
                            drawerState.open()
                        }
                    },
                    onRightButtonClick = fun() {
                        val budgetEntry = BudgetEntry(budgetId = budget.id)
                        val destination = BudgetEntryScreenDestination(budgetEntry)
                        navigator.navigate(destination)
                    }
                )
            }
        ) { paddingValues ->
            BudgetDetailContent(
                budgetAmount = uiState.budget.amount,
                budgetEntries = uiState.entries,
                paddingValues = paddingValues,
                onBudgetClick = myViewModel::showBudgetModal,
                onItemClick = fun(budgetEntry) {
                    val destination = BudgetEntryScreenDestination(budgetEntry)
                    navigator.navigate(destination)
                }
            )
        }

        BudgetModal(
            show = uiState.isBudgetModalVisible,
            budgetAmount = uiState.budget.amount,
            onDismiss = myViewModel::hideBudgetModal,
            onSaveClick = myViewModel::setBudgetAmount
        )

        FilterModal(
            show = uiState.isFilterModalVisible,
            filter = uiState.filter,
            onDismiss = myViewModel::hideFilterModal,
            onClean = myViewModel::clearFilter,
            onApply = myViewModel::filterEntries
        )

        DeleteConfirmationModal(
            show = uiState.isDeleteModalVisible,
            onDismiss = myViewModel::hideDeleteModal,
            onConfirm = fun() {
                myViewModel.deleteBudget()
                navigator.popBackStack()
            }
        )
    }
}
