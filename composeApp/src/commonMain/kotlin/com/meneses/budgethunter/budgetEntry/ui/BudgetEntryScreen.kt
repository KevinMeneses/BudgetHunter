package com.meneses.budgethunter.budgetEntry.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.come_back
import budgethunter.composeapp.generated.resources.discard
import budgethunter.composeapp.generated.resources.new_registry
import budgethunter.composeapp.generated.resources.save_entry
import budgethunter.composeapp.generated.resources.unsaved_changes_confirmation_message
import budgethunter.composeapp.generated.resources.update_registry
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryIntent
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
        onIntent: (BudgetEntryIntent) -> Unit,
        snackbarHostState: SnackbarHostState
    ) {
        val onBack = remember {
            fun() {
                BudgetEntryIntent
                    .ValidateChanges(budgetEntry)
                    .run(onIntent)
            }
        }

        val setBudgetEntry = remember {
            fun(budgetEntry: BudgetEntry) {
                BudgetEntryIntent
                    .SetBudgetEntry(budgetEntry)
                    .run(onIntent)
            }
        }

        LaunchedEffect(budgetEntry.id) {
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
                        BudgetEntryIntent
                            .SaveBudgetEntry
                            .run(onIntent)
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { paddingValues ->
            BudgetEntryForm(
                budgetEntry = uiState.budgetEntry ?: budgetEntry,
                amountError = uiState.emptyAmountError?.let { stringResource(it) },
                isFileValid = uiState.isFileValid,
                paddingValues = paddingValues,
                onBudgetItemChanged = setBudgetEntry,
                onInvoiceFieldClick = {
                    if (uiState.budgetEntry?.invoice == null) {
                        BudgetEntryIntent.ToggleAttachInvoiceModal(true)
                    } else {
                        BudgetEntryIntent.ToggleShowInvoiceModal(true)
                    }.run(onIntent)
                }
            )
        }

        if (uiState.isProcessingInvoice) {
            LoadingOverlay()
        }

        if (uiState.isSharingFile) {
            LoadingOverlay()
        }

        if (uiState.isOpeningFilePicker) {
            LoadingOverlay()
        }

        if (uiState.isSaving) {
            LoadingOverlay()
        }

        ConfirmationModal(
            show = uiState.isDiscardChangesModalVisible,
            message = stringResource(Res.string.unsaved_changes_confirmation_message),
            confirmButtonText = stringResource(Res.string.discard),
            cancelButtonText = stringResource(Res.string.come_back),
            onDismiss = {
                BudgetEntryIntent
                    .HideDiscardChangesModal
                    .run(onIntent)
            },
            onConfirm = {
                BudgetEntryIntent.DiscardChanges
                    .run(onIntent)
            }
        )

        FileNotFoundModal(
            show = uiState.shouldShowFileNotFoundModal(),
            onDismiss = {
                BudgetEntryIntent
                    .ToggleShowInvoiceModal(false)
                    .run(onIntent)
            },
            onReattach = {
                BudgetEntryIntent
                    .UpdateInvoice
                    .run(onIntent)
            }
        )

        uiState.validatedFilePath?.let { validatedPath ->
            var isFileLoadable by remember {
                mutableStateOf(true)
            }

            InvoiceDisplayModal(
                show = uiState.shouldShowInvoiceDisplayModal(),
                validatedFilePath = validatedPath,
                onDismiss = {
                    BudgetEntryIntent
                        .ToggleShowInvoiceModal(false)
                        .run(onIntent)
                },
                onEdit = {
                    BudgetEntryIntent
                        .UpdateInvoice
                        .run(onIntent)
                },
                onShare = {
                    BudgetEntryIntent
                        .ShareFile(validatedPath)
                        .run(onIntent)
                },
                onDelete = {
                    BudgetEntryIntent
                        .DeleteAttachedInvoice
                        .run(onIntent)
                },
                onError = {
                    isFileLoadable = false
                }
            )

            FileNotLoadableModal(
                show = !isFileLoadable,
                onDismiss = {
                    isFileLoadable = true
                },
                onReplace = {
                    BudgetEntryIntent
                        .UpdateInvoice
                        .run(onIntent)
                }
            )
        }

        AttachInvoiceModal(
            show = uiState.isAttachInvoiceModalVisible,
            onDismiss = {
                BudgetEntryIntent
                    .ToggleAttachInvoiceModal(false)
                    .run(onIntent)
            },
            onTakePhoto = {
                BudgetEntryIntent.TakePhoto.run(onIntent)
            },
            onSelectFile = {
                BudgetEntryIntent.PickFile.run(onIntent)
            }
        )

        PlatformBackHandler(enabled = true, onBack = onBack)
    }
}
