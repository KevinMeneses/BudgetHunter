package com.meneses.budgethunter.budgetList.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetList.BudgetListViewModel
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.destinations.BudgetDetailScreenDestination
import com.meneses.budgethunter.destinations.UserGuideScreenDestination
import com.meneses.budgethunter.fakeNavigation
import com.meneses.budgethunter.theme.BudgetHunterTheme
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
    val uiState by myViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppBar(
                title = stringResource(id = R.string.budgets),
                leftButtonIcon = Icons.Outlined.Info,
                rightButtonIcon = Icons.Default.Search,
                leftButtonDescription = stringResource(id = R.string.user_guide),
                rightButtonDescription = stringResource(id = R.string.filter),
                onLeftButtonClick = { navigator.navigate(UserGuideScreenDestination) },
                onRightButtonClick = fun() {
                    BudgetListEvent
                        .ToggleFilterModal(true)
                        .run(myViewModel::sendEvent)
                },
                animateRightButton = uiState.filter != null
            )
        }
    ) {
        BudgetListContent(
            list = uiState.budgetList,
            paddingValues = it,
            onEvent = myViewModel::sendEvent,
            animate = uiState.budgetList.isEmpty() && uiState.filter == null
        )
    }

    NewBudgetModal(
        show = uiState.addModalVisibility,
        onEvent = myViewModel::sendEvent
    )

    FilterListModal(
        show = uiState.filterModalVisibility,
        filter = uiState.filter,
        onEvent = myViewModel::sendEvent
    )

    uiState.navigateToBudget?.let {
        val destination = BudgetDetailScreenDestination(it)
        navigator.navigate(destination)
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            BudgetListEvent
                .ClearNavigation
                .run(myViewModel::sendEvent)
        }
    }
}
