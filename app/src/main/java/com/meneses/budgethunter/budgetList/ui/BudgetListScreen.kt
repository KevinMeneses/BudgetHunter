package com.meneses.budgethunter.budgetList.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetList.BudgetListViewModel
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.theme.BudgetHunterTheme
import kotlinx.serialization.Serializable

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetHunterTheme {
        BudgetListScreen.Show(
            showUserGuide = {},
            showBudgetDetail = {}
        )
    }
}

@Serializable
object BudgetListScreen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Show(
        myViewModel: BudgetListViewModel = viewModel(),
        showUserGuide: () -> Unit,
        showBudgetDetail: (Budget) -> Unit
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
                    onLeftButtonClick = showUserGuide,
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

        LaunchedEffect(key1 = uiState.navigateToBudget) {
            uiState.navigateToBudget?.let { showBudgetDetail(it) }
        }

        DisposableEffect(key1 = Unit) {
            onDispose {
                BudgetListEvent
                    .ClearNavigation
                    .run(myViewModel::sendEvent)
            }
        }
    }
}

