package com.meneses.budgethunter.budgetEntry.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.come_back
import budgethunter.composeapp.generated.resources.discard
import budgethunter.composeapp.generated.resources.new_registry
import budgethunter.composeapp.generated.resources.save_entry
import budgethunter.composeapp.generated.resources.unsaved_changes_confirmation_message
import budgethunter.composeapp.generated.resources.update_registry
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryEvent
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryState
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.commons.ui.ConfirmationModal
import com.meneses.budgethunter.commons.ui.LoadingOverlay
import com.meneses.budgethunter.commons.ui.PlatformBackHandler
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
data class BudgetEntryScreen(val budgetEntry: BudgetEntry) {
    @Composable
    fun Show(
        uiState: BudgetEntryState,
        onEvent: (BudgetEntryEvent) -> Unit,
        goBack: () -> Unit
    ) {
        val onBack = remember {
            fun() {
                BudgetEntryEvent
                    .ValidateChanges(budgetEntry)
                    .run(onEvent)
            }
        }

        val setBudgetEntry = remember {
            fun(budgetEntry: BudgetEntry) {
                BudgetEntryEvent
                    .SetBudgetEntry(budgetEntry)
                    .run(onEvent)
            }
        }

        LaunchedEffect(Unit) {
            if (uiState.budgetEntry?.id != budgetEntry.id) {
                setBudgetEntry(budgetEntry)
            }
        }

        Scaffold(
            topBar = {
                val title = if (budgetEntry.id < 0) stringResource(Res.string.new_registry)
                else stringResource(Res.string.update_registry)

                AppBar(
                    title = title,
                    leftButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    leftButtonDescription = stringResource(Res.string.come_back),
                    rightButtonIcon = Icons.Default.Done,
                    rightButtonDescription = stringResource(Res.string.save_entry),
                    onLeftButtonClick = onBack,
                    onRightButtonClick = {
                        BudgetEntryEvent
                            .SaveBudgetEntry
                            .run(onEvent)
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
                    }.run(onEvent)
                }
            )
        }

        if (uiState.isProcessingInvoice) {
            LoadingOverlay()
        }

        ConfirmationModal(
            show = uiState.isDiscardChangesModalVisible,
            message = stringResource(Res.string.unsaved_changes_confirmation_message),
            confirmButtonText = stringResource(Res.string.discard),
            cancelButtonText = stringResource(Res.string.come_back),
            onDismiss = {
                BudgetEntryEvent
                    .HideDiscardChangesModal
                    .run(onEvent)
            },
            onConfirm = {
                BudgetEntryEvent.DiscardChanges
                    .run(onEvent)
            }
        )

        ShowInvoiceModal(
            show = uiState.isShowInvoiceModalVisible,
            invoice = uiState.budgetEntry?.invoice,
            onDismiss = {
                BudgetEntryEvent
                    .ToggleShowInvoiceModal(false)
                    .run(onEvent)
            },
            onEdit = {
                BudgetEntryEvent
                    .ToggleAttachInvoiceModal(true)
                    .run(onEvent)
                BudgetEntryEvent
                    .ToggleShowInvoiceModal(false)
                    .run(onEvent)
            },
            onShare = {
                uiState.budgetEntry?.invoice?.let { filePath ->
                    BudgetEntryEvent.ShareFile(filePath).run(onEvent)
                }
            },
            onDelete = {
                BudgetEntryEvent
                    .DeleteAttachedInvoice
                    .run(onEvent)
            }
        )

        AttachInvoiceModal(
            show = uiState.isAttachInvoiceModalVisible,
            onDismiss = {
                BudgetEntryEvent
                    .ToggleAttachInvoiceModal(false)
                    .run(onEvent)
            },
            onTakePhoto = {
                BudgetEntryEvent.TakePhoto.run(onEvent)
            },
            onSelectFile = {
                BudgetEntryEvent.PickFile.run(onEvent)
            }
        )

        if (uiState.attachInvoiceError != null) {
            LaunchedEffect(uiState.attachInvoiceError) {
                BudgetEntryEvent.ShowNotification(uiState.attachInvoiceError, isError = true).run(onEvent)
            }
        }

        PlatformBackHandler(enabled = true, onBack = onBack)

        LaunchedEffect(key1 = uiState.goBack) {
            if (uiState.goBack) goBack()
        }
    }
}