package com.meneses.budgethunter.budgetEntry.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.BudgetEntryViewModel
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryEvent
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.commons.ui.ConfirmationModal
import com.meneses.budgethunter.commons.utils.fakeNavigation
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
    val context = LocalContext.current

    val onBack = remember {
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
                onLeftButtonClick = onBack,
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
            onBudgetItemChanged = setBudgetEntry,
            onInvoiceFieldClick = {
                if (uiState.budgetEntry?.invoice == null) {
                    BudgetEntryEvent.ToggleAttachInvoiceModal(true)
                } else {
                    BudgetEntryEvent.ToggleShowInvoiceModal(true)
                }.run(myViewModel::sendEvent)
            }
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

    val selectFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = {
            BudgetEntryEvent
                .SaveInvoice(
                    fileToSave = it ?: return@rememberLauncherForActivityResult,
                    contentResolver = context.contentResolver,
                    internalFilesDir = context.dataDir
                ).run(myViewModel::sendEvent)
        }
    )

    ShowInvoiceModal(
        show = uiState.isShowInvoiceModalVisible,
        invoice = uiState.budgetEntry?.invoice,
        onDismiss = {
            BudgetEntryEvent
                .ToggleShowInvoiceModal(false)
                .run(myViewModel::sendEvent)
        },
        onEdit = {
            BudgetEntryEvent
                .ToggleAttachInvoiceModal(true)
                .run(myViewModel::sendEvent)
            BudgetEntryEvent
                .ToggleShowInvoiceModal(false)
                .run(myViewModel::sendEvent)
        },
        onDelete = {
            BudgetEntryEvent
                .DeleteAttachedInvoice
                .run(myViewModel::sendEvent)
        }
    )

    AttachInvoiceModal(
        show = uiState.isAttachInvoiceModalVisible,
        onDismiss = {
            BudgetEntryEvent
                .ToggleAttachInvoiceModal(false)
                .run(myViewModel::sendEvent)
        },
        onTakePhoto = {

        },
        onSelectFile = {
            selectFileLauncher
                .launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    )

    if (uiState.attachInvoiceError != null) {
        Toast.makeText(context, uiState.attachInvoiceError, Toast.LENGTH_SHORT).show()
    }

    BackHandler(enabled = true, onBack = onBack)
    if (uiState.goBack) navigator.popBackStack()
}
