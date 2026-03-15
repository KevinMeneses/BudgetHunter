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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.add_transaction
import budgethunter.composeapp.generated.resources.back_content_description
import budgethunter.composeapp.generated.resources.open_menu
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailIntent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.platform.NetworkMonitor
import com.meneses.budgethunter.commons.ui.AppBar
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
data class BudgetDetailScreen(val budget: Budget) {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Show(
        uiState: BudgetDetailState,
        onIntent: (BudgetDetailIntent) -> Unit,
        snackbarHostState: SnackbarHostState,
        goBack: () -> Unit,
        showBudgetMetrics: (Budget) -> Unit,
        showSettings: () -> Unit,
        showCollaborators: (Long, String) -> Unit,
        networkMonitor: NetworkMonitor
    ) {
        var dropdownExpanded by remember { mutableStateOf(false) }
        val currentBudget = uiState.budgetDetail.budget
        val isBudgetSynced = currentBudget.serverId != null
        val isOnline by networkMonitor.isOnline.collectAsState()

        DisposableEffect(Unit) {
            if (uiState.budgetDetail.budget.id != budget.id) {
                BudgetDetailIntent
                    .SetBudget(budget)
                    .run(onIntent)
            }

            BudgetDetailIntent
                .GetBudgetDetail
                .run(onIntent)

            onDispose { }
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = currentBudget.name,
                    leftButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    leftButtonDescription = stringResource(Res.string.back_content_description),
                    secondRightButtonIcon = Icons.Default.Add,
                    secondRightButtonDescription = stringResource(Res.string.add_transaction),
                    rightButtonIcon = Icons.Default.MoreVert,
                    rightButtonDescription = stringResource(Res.string.open_menu),
                    onLeftButtonClick = goBack,
                    onSecondRightButtonClick = {
                        val budgetEntry = BudgetEntry(budgetId = currentBudget.id)
                        BudgetDetailIntent
                            .ShowEntry(budgetEntry)
                            .run(onIntent)
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
                                BudgetDetailIntent
                                    .ToggleFilterModal(true)
                                    .run(onIntent)
                            },
                            onMetricsClick = { showBudgetMetrics(currentBudget) },
                            onDeleteClick = {
                                BudgetDetailIntent
                                    .ToggleDeleteBudgetModal(true)
                                    .run(onIntent)
                            },
                            onSettingsClick = showSettings,
                            showCollaboratorsOption = isBudgetSynced,
                            onCollaboratorsClick = {
                                currentBudget.serverId?.let { serverId ->
                                    showCollaborators(serverId, currentBudget.name)
                                }
                            }
                        )
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { paddingValues ->
            BudgetDetailContent(
                paddingValues = paddingValues,
                isOnline = if (uiState.isAuthenticated) isOnline else true,
                uiState = uiState,
                onIntent = onIntent
            )
        }

        BudgetModal(
            show = uiState.isBudgetModalVisible,
            budgetAmount = currentBudget.amount,
            onIntent = onIntent
        )

        FilterModal(
            show = uiState.isFilterModalVisible,
            filter = uiState.filter,
            onIntent = onIntent
        )

        DeleteBudgetConfirmationModal(
            show = uiState.isDeleteBudgetModalVisible,
            onIntent = onIntent
        )

        DeleteEntriesConfirmationModal(
            show = uiState.isDeleteEntriesModalVisible,
            onIntent = onIntent
        )
    }
}
