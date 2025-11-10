package com.meneses.budgethunter.budgetList.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.AbsoluteRoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        val snackBarHostState = remember { SnackbarHostState() }
        var dropdownExpanded by remember { mutableStateOf(false) }

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
                        rightButtonIcon = Icons.Default.MoreVert,
                        leftButtonDescription = stringResource(Res.string.search),
                        rightButtonDescription = stringResource(Res.string.open_menu),
                        onLeftButtonClick = {
                            BudgetListEvent
                                .ToggleSearchMode(true)
                                .run(onEvent)
                        },
                        onRightButtonClick = {
                            dropdownExpanded = true
                        },
                        animateRightButton = uiState.filter != null,
                        rightButtonDropdownContent = {
                            BudgetListMenu(
                                expanded = dropdownExpanded,
                                onDismiss = { dropdownExpanded = false },
                                onSettingsClick = showSettings
                            )
                        }
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
                            shape = AbsoluteRoundedCornerShape(10.dp),
                            on = 10.dp,
                            off = 8.dp
                        ),
                    shape = AbsoluteRoundedCornerShape(10.dp),
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

