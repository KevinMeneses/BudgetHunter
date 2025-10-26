package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
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
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.add_transaction
import budgethunter.composeapp.generated.resources.back_content_description
import budgethunter.composeapp.generated.resources.open_menu
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.ui.AppBar
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
data class BudgetDetailScreen(val budget: Budget) {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Show(
        uiState: BudgetDetailState,
        onEvent: (BudgetDetailEvent) -> Unit,
        goBack: () -> Unit,
        showBudgetEntry: (BudgetEntry) -> Unit,
        showBudgetMetrics: (Budget) -> Unit,
        showSettings: () -> Unit
    ) {
        val snackBarHostState = remember { SnackbarHostState() }
        var dropdownExpanded by remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
            if (uiState.budgetDetail.budget.id != budget.id) {
                BudgetDetailEvent
                    .SetBudget(budget)
                    .run(onEvent)
            }

            BudgetDetailEvent
                .GetBudgetDetail
                .run(onEvent)

            onDispose {
                BudgetDetailEvent
                    .ClearNavigation
                    .run(onEvent)
            }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = uiState.budgetDetail.budget.name,
                    leftButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    leftButtonDescription = stringResource(Res.string.back_content_description),
                    secondRightButtonIcon = Icons.Default.Add,
                    secondRightButtonDescription = stringResource(Res.string.add_transaction),
                    rightButtonIcon = Icons.Default.MoreVert,
                    rightButtonDescription = stringResource(Res.string.open_menu),
                    onLeftButtonClick = goBack,
                    onSecondRightButtonClick = {
                        val budgetEntry = BudgetEntry(budgetId = uiState.budgetDetail.budget.id)
                        BudgetDetailEvent
                            .ShowEntry(budgetEntry)
                            .run(onEvent)
                    },
                    onRightButtonClick = {
                        dropdownExpanded = true
                    },
                    animateRightButton = uiState.filter != null,
                    rightButtonDropdownContent = {
                        BudgetDetailMenu(
                            expanded = dropdownExpanded,
                            onDismiss = { dropdownExpanded = false },
                            onFilterClick = {
                                BudgetDetailEvent
                                    .ToggleFilterModal(true)
                                    .run(onEvent)
                            },
                            onMetricsClick = { showBudgetMetrics(budget) },
                            onDeleteClick = {
                                BudgetDetailEvent
                                    .ToggleDeleteBudgetModal(true)
                                    .run(onEvent)
                            },
                            onSettingsClick = showSettings
                        )
                    }
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
            budgetAmount = uiState.budgetDetail.budget.amount,
            onEvent = onEvent
        )

        FilterModal(
            show = uiState.isFilterModalVisible,
            filter = uiState.filter,
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

        LaunchedEffect(key1 = uiState.goBack) {
            if (uiState.goBack) goBack()
        }

        LaunchedEffect(key1 = uiState.showEntry) {
            uiState.showEntry?.let { showBudgetEntry(it) }
        }

        LaunchedEffect(key1 = uiState.syncError) {
            uiState.syncError?.let { message ->
                snackBarHostState.showSnackbar(message)
                BudgetDetailEvent.ClearSyncError.run(onEvent)
            }
        }
    }
}
