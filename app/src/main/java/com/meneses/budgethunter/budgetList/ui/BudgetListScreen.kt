package com.meneses.budgethunter.budgetList.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.application.BudgetListState
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.theme.BudgetHunterTheme
import kotlinx.serialization.Serializable

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetHunterTheme {
        BudgetListScreen.Show(
            uiState = BudgetListState(),
            onEvent = {},
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
        uiState: BudgetListState,
        onEvent: (BudgetListEvent) -> Unit,
        showUserGuide: () -> Unit,
        showBudgetDetail: (Budget) -> Unit
    ) {
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
                            .run(onEvent)
                    },
                    animateRightButton = uiState.filter != null
                )
            }
        ) {
            BudgetListContent(
                list = uiState.budgetList,
                paddingValues = it,
                onEvent = onEvent,
                animate = uiState.budgetList.isEmpty() && uiState.filter == null
            )
        }

        NewBudgetModal(
            show = uiState.addModalVisibility,
            onEvent = onEvent
        )

        FilterListModal(
            show = uiState.filterModalVisibility,
            filter = uiState.filter,
            onEvent = onEvent
        )

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

