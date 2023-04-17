package com.meneses.budgethunter.insAndOuts.ui

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meneses.budgethunter.budgetItemLists
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.destinations.DetailScreenDestination
import com.meneses.budgethunter.fakeNavigation
import com.meneses.budgethunter.insAndOuts.InsAndOutsViewModel
import com.meneses.budgethunter.insAndOuts.domain.BudgetItem
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    InsAndOutsScreen(fakeNavigation, Budget())
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun InsAndOutsScreen(
    navigator: DestinationsNavigator,
    budget: Budget,
    myViewModel: InsAndOutsViewModel = viewModel()
) {
    val uiState by myViewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (uiState.budget.id != budget.id) {
            myViewModel.setBudget(budget)
        }

        myViewModel.getBudgetItems()
    }

    ModalNavigationDrawer(
        drawerContent = {
            InsAndOutsMenu(
                onFilterClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        myViewModel.setFilterModalVisibility(true)
                    }
                },
                onDeleteClick = {
                    coroutineScope.launch {
                        drawerState.close()
                        myViewModel.setDeleteModalVisibility(true)
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
                    onLeftButtonClick = {
                        coroutineScope.launch {
                            drawerState.open()
                        }
                    },
                    onRightButtonClick = {
                        val budgetItem = BudgetItem(id = budgetItemLists.size, budgetId = budget.id)
                        val destination = DetailScreenDestination(budgetItem)
                        navigator.navigate(destination)
                    }
                )
            }
        ) { paddingValues ->
            InsAndOutsContent(
                budgetAmount = uiState.budget.amount,
                budgetItems = uiState.itemList,
                paddingValues = paddingValues,
                onBudgetClick = { myViewModel.setBudgetModalVisibility(true) },
                onItemClick = { navigator.navigate(DetailScreenDestination(it)) }
            )
        }

        BudgetModal(
            show = uiState.isBudgetModalVisible,
            budgetAmount = uiState.budget.amount,
            onDismiss = { myViewModel.setBudgetModalVisibility(false) },
            onSaveClick = { myViewModel.setBudgetAmount(it) }
        )

        FilterModal(
            show = uiState.isFilterModalVisible,
            filter = uiState.filter,
            onDismiss = { myViewModel.setFilterModalVisibility(false) },
            onClean = { myViewModel.clearFilter() },
            onApply = { myViewModel.filterList(it) }
        )

        DeleteConfirmationModal(
            show = uiState.isDeleteModalVisible,
            onDismiss = { myViewModel.setDeleteModalVisibility(false) },
            onAccept = {
                myViewModel.deleteBudget()
                navigator.popBackStack()
            }
        )
    }
}

