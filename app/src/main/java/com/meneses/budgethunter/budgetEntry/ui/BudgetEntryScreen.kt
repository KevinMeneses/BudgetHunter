package com.meneses.budgethunter.budgetEntry.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
            val title = remember {
                if (budgetEntry.amount == null) "Nuevo presupuesto"
                else "Modificar presupuesto"
            }

            AppBar(
                title = title,
                leftButtonIcon = Icons.Default.ArrowBack,
                rightButtonIcon = Icons.Default.Done,
                onLeftButtonClick = onBack,
                onRightButtonClick = fun() {
                    if (uiState.budgetEntry?.amount != null) {
                        myViewModel.saveBudgetEntry()
                        navigator.popBackStack()
                    }
                }
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
        message = "Hay cambios sin guardar ¿desea descartar estos cambios?",
        confirmButtonText = "Descartar",
        cancelButtonText = "Volver",
        onDismiss = myViewModel::hideDiscardChangesModal,
        onConfirm = navigator::popBackStack
    )

    BackHandler(enabled = true, onBack = onBack)
    if (uiState.goBack) navigator.popBackStack()
}
