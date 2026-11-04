package com.meneses.budgethunter.budgetDetail.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.back_content_description
import budgethunter.composeapp.generated.resources.new_registry
import budgethunter.composeapp.generated.resources.open_menu
import budgethunter.composeapp.generated.resources.scan_receipt
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailIntent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.platform.NetworkMonitor
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.theme.AppColors
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

        // Retry pushing entries saved while the app was in background (e.g. from an SMS) whose own
        // push failed; the effect above only runs once per ViewModel, so returning to the
        // foreground would otherwise never sync them.
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            onIntent(BudgetDetailIntent.ResumeSync)
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = currentBudget.name,
                    leftButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    leftButtonDescription = stringResource(Res.string.back_content_description),
                    rightButtonIcon = Icons.Default.MoreVert,
                    rightButtonDescription = stringResource(Res.string.open_menu),
                    onLeftButtonClick = goBack,
                    onRightButtonClick = {
                        dropdownExpanded = true
                    },
                    rightButtonDropdownContent = {
                        BudgetDetailMenu(
                            expanded = dropdownExpanded,
                            onDismiss = { dropdownExpanded = false },
                            onDeleteClick = {
                                BudgetDetailIntent
                                    .ToggleDeleteBudgetModal(true)
                                    .run(onIntent)
                            },
                            onSettingsClick = showSettings
                        )
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            floatingActionButton = {
                // Out of the way while selecting: the delete button takes the bottom then
                if (!uiState.isSelectionActive) {
                    NewEntryButtons(
                        onNewEntry = {
                            BudgetDetailIntent
                                .ShowEntry(BudgetEntry(budgetId = currentBudget.id))
                                .run(onIntent)
                        },
                        onScanReceipt = {
                            BudgetDetailIntent
                                .ShowEntry(BudgetEntry(budgetId = currentBudget.id), scanReceipt = true)
                                .run(onIntent)
                        }
                    )
                }
            }
        ) { paddingValues ->
            BudgetDetailContent(
                paddingValues = paddingValues,
                isOnline = if (uiState.isAuthenticated) isOnline else true,
                uiState = uiState,
                onIntent = onIntent,
                onMetricsClick = { showBudgetMetrics(currentBudget) },
                onCollaboratorsClick = currentBudget.serverId
                    ?.takeIf { isBudgetSynced }
                    ?.let { serverId -> { showCollaborators(serverId, currentBudget.name) } }
            )
        }

        BudgetModal(
            show = uiState.modal == BudgetDetailState.ModalState.Budget,
            budgetAmount = currentBudget.amount,
            onIntent = onIntent
        )

        DeleteBudgetConfirmationModal(
            show = uiState.modal == BudgetDetailState.ModalState.DeleteBudget,
            onIntent = onIntent
        )

        DeleteEntriesConfirmationModal(
            show = uiState.modal == BudgetDetailState.ModalState.DeleteEntries,
            onIntent = onIntent
        )
    }
}

/**
 * A new entry is the main action; scanning a receipt, which opens the same form with the receipt
 * picker already showing, sits right above it.
 */
@Composable
private fun NewEntryButtons(
    onNewEntry: () -> Unit,
    onScanReceipt: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmallFloatingActionButton(
            onClick = onScanReceipt,
            containerColor = AppColors.tertiaryContainer,
            contentColor = AppColors.onTertiaryContainer
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = stringResource(Res.string.scan_receipt)
            )
        }
        ExtendedFloatingActionButton(
            onClick = onNewEntry,
            containerColor = AppColors.primaryContainer,
            contentColor = AppColors.onPrimaryContainer,
            icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
            text = { Text(text = stringResource(Res.string.new_registry)) }
        )
    }
}
