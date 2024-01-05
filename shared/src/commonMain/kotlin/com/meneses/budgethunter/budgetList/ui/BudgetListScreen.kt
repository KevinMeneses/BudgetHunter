package com.meneses.budgethunter.budgetList.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetList.BudgetListViewModel
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.budgetListViewModel
import com.meneses.budgethunter.commons.ui.AppBar
import moe.tlaster.precompose.flow.collectAsStateWithLifecycle
import moe.tlaster.precompose.viewmodel.viewModel

@Composable
fun BudgetListScreen(
    myViewModel: BudgetListViewModel = viewModel(creator = budgetListViewModel()),
    onHelpClick: () -> Unit,
    onBudgetClick: (Budget) -> Unit
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
                onLeftButtonClick = onHelpClick,
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

    uiState.navigateToBudget?.let(onBudgetClick)

    DisposableEffect(key1 = Unit) {
        onDispose {
            BudgetListEvent
                .ClearNavigation
                .run(myViewModel::sendEvent)
        }
    }
}
