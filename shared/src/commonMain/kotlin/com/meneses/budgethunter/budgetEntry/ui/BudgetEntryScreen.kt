package com.meneses.budgethunter.budgetEntry.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.BudgetEntryViewModel
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryEvent
import com.meneses.budgethunter.budgetEntry.budgetEntryViewModel
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.commons.ui.ConfirmationModal
import moe.tlaster.precompose.flow.collectAsStateWithLifecycle
import moe.tlaster.precompose.navigation.BackHandler
import moe.tlaster.precompose.viewmodel.viewModel

@Composable
fun BudgetEntryScreen(
    budgetEntry: BudgetEntry,
    onGoBack: () -> Unit,
    myViewModel: BudgetEntryViewModel = viewModel(creator = budgetEntryViewModel())
) {
    val uiState by myViewModel.uiState.collectAsStateWithLifecycle()
    val onBackPressed = remember {
        fun() {
            BudgetEntryEvent
                .ValidateChanges(budgetEntry)
                .run(myViewModel::sendEvent)
        }
    }

    val setBudgetEntry = remember {
        fun(budgetEntry: BudgetEntry) {
            BudgetEntryEvent
                .SetBudgetEntry(budgetEntry)
                .run(myViewModel::sendEvent)
        }
    }

    LaunchedEffect(Unit) {
        if (uiState.budgetEntry?.id != budgetEntry.id) {
            setBudgetEntry(budgetEntry)
        }
    }

    Scaffold(
        topBar = {
            val title = if (budgetEntry.id < 0) stringResource(id = R.string.new_registry)
            else stringResource(id = R.string.update_registry)

            AppBar(
                title = title,
                leftButtonIcon = Icons.Default.ArrowBack,
                leftButtonDescription = stringResource(R.string.come_back),
                rightButtonIcon = Icons.Default.Done,
                rightButtonDescription = stringResource(R.string.save_entry),
                onLeftButtonClick = onBackPressed,
                onRightButtonClick = {
                    BudgetEntryEvent
                        .SaveBudgetEntry
                        .run(myViewModel::sendEvent)
                }
            )
        }
    ) { paddingValues ->
        BudgetEntryForm(
            budgetEntry = uiState.budgetEntry ?: budgetEntry,
            amountError = uiState.emptyAmountError,
            paddingValues = paddingValues,
            onBudgetItemChanged = setBudgetEntry
        )
    }

    ConfirmationModal(
        show = uiState.isDiscardChangesModalVisible,
        message = stringResource(id = R.string.unsaved_changes_confirmation_message),
        confirmButtonText = stringResource(id = R.string.discard),
        cancelButtonText = stringResource(id = R.string.come_back),
        onDismiss = {
            BudgetEntryEvent
                .HideDiscardChangesModal
                .run(myViewModel::sendEvent)
        },
        onConfirm = {
            BudgetEntryEvent.GoBack
                .run(myViewModel::sendEvent)
        }
    )

    BackHandler(enabled = true, onBack = onBackPressed)
    if (uiState.goBack) onGoBack()
}
