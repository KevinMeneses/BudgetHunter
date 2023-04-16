package com.meneses.budgethunter.budgetList.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meneses.budgethunter.budgetList.BudgetListViewModel
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.theme.BudgetHunterTheme
import com.meneses.budgethunter.destinations.InsAndOutsScreenDestination
import com.meneses.budgethunter.fakeNavigation
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetHunterTheme {
        BudgetListScreen(fakeNavigation)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Destination
fun BudgetListScreen(
    navigator: DestinationsNavigator,
    myViewModel: BudgetListViewModel = viewModel()
) {
    val uiState by myViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppBar(
                title = "Presupuestos",
                rightButtonIcon = Icons.Default.Search,
                onRightButtonClick = { myViewModel.setFilterModalVisibility(true) }
            )
        }
    ) {
        BudgetListContent(
            list = uiState.budgetList,
            paddingValues = it,
            onAddBudgetClick = { myViewModel.setAddModalVisibility(true) },
            onBudgetClick = { index ->
                navigator.navigate(
                    InsAndOutsScreenDestination(uiState.budgetList[index])
                )
            }
        )
    }

    NewBudgetModal(
        show = uiState.addModalVisibility,
        onDismiss = { myViewModel.setAddModalVisibility(false) },
        onCreateClick = {
            val savedBudget = myViewModel.createBudget(it)
            navigator.navigate(InsAndOutsScreenDestination(savedBudget))
        }
    )

    FilterListModal(
        show = uiState.filterModalVisibility,
        filter = uiState.filter,
        onDismiss = { myViewModel.setFilterModalVisibility(false) },
        onClear = { myViewModel.clearFilter() },
        onApplyClick = { myViewModel.filterList(it) }
    )
}