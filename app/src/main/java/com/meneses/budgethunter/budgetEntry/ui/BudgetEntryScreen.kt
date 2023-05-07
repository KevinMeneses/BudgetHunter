package com.meneses.budgethunter.budgetEntry.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meneses.budgethunter.R
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.budgetEntry.BudgetEntryViewModel
import com.meneses.budgethunter.fakeNavigation
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.ui.ConfirmationModal
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetEntryScreen(fakeNavigation, BudgetEntry(id = -1, budgetId = -1))
}

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun BudgetEntryScreen(
    navigator: DestinationsNavigator,
    budgetEntry: BudgetEntry,
    myViewModel: BudgetEntryViewModel = viewModel()
) {
    val uiState by myViewModel.uiState.collectAsStateWithLifecycle()
    val onBack = remember {
        fun() { myViewModel.validateChanges(budgetEntry) }
    }

    LaunchedEffect(Unit) {
        if (uiState.budgetEntry?.id != budgetEntry.id) {
            myViewModel.setBudgetEntry(budgetEntry)
        }
    }

    Scaffold(
        topBar = {
            val title = if (budgetEntry.id < 0) stringResource(id = R.string.new_registry)
                else stringResource(id = R.string.update_registry)

            AppBar(
                title = title,
                leftButtonIcon = Icons.Default.ArrowBack,
                rightButtonIcon = Icons.Default.Done,
                onLeftButtonClick = onBack,
                onRightButtonClick = myViewModel::saveBudgetEntry
            )
        }
    ) { paddingValues ->
        BudgetEntryForm(
            budgetEntry = uiState.budgetEntry ?: budgetEntry,
            paddingValues = paddingValues,
            onBudgetItemChanged = myViewModel::setBudgetEntry
        )
    }

    ConfirmationModal(
        show = uiState.isDiscardChangesModalVisible,
        message = stringResource(id = R.string.unsaved_changes_confirmation_message),
        confirmButtonText = stringResource(id = R.string.discard),
        cancelButtonText = stringResource(id = R.string.come_back),
        onDismiss = myViewModel::hideDiscardChangesModal,
        onConfirm = navigator::popBackStack
    )

    BackHandler(enabled = true, onBack = onBack)
    if (uiState.goBack) navigator.popBackStack()
}
