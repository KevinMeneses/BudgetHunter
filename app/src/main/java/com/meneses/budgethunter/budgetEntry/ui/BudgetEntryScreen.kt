package com.meneses.budgethunter.budgetEntry.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.BudgetEntryViewModel
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryEvent
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.commons.ui.ConfirmationModal
import kotlinx.serialization.Serializable
import java.io.File


@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    BudgetEntryScreen(BudgetEntry(id = -1, budgetId = -1))
}

@Serializable
data class BudgetEntryScreen(val budgetEntry: BudgetEntry) {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Show(
        myViewModel: BudgetEntryViewModel = viewModel(),
        goBack: () -> Unit
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
                BudgetEntryEvent.DiscardChanges
                    .run(myViewModel::sendEvent)
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
            onShare = {
                val invoiceUri = FileProvider.getUriForFile(
                    /* context = */ context,
                    /* authority = */ context.packageName + ".provider",
                    /* file = */ File(uiState.budgetEntry?.invoice.orEmpty())
                )

                val shareIntent = Intent()
                    .setAction(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_STREAM, invoiceUri)
                    .setType("*/*")
                    .putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/pdf", "image/jpeg"))

                startActivity(
                    /* context = */ context,
                    /* intent = */ Intent.createChooser(shareIntent, "Share Invoice"),
                    /* options = */ null
                )
            },
            onDelete = {
                BudgetEntryEvent
                    .DeleteAttachedInvoice
                    .run(myViewModel::sendEvent)
            }
        )

        var photoUri by remember { mutableStateOf<Uri?>(null) }

        val takePhotoLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
            onResult = {
                if (it) BudgetEntryEvent.AttachInvoice(
                    fileToSave = photoUri ?: return@rememberLauncherForActivityResult,
                    contentResolver = context.contentResolver,
                    internalFilesDir = context.filesDir
                ).run(myViewModel::sendEvent)
            }
        )

        val selectFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = {
                BudgetEntryEvent
                    .AttachInvoice(
                        fileToSave = it ?: return@rememberLauncherForActivityResult,
                        contentResolver = context.contentResolver,
                        internalFilesDir = context.filesDir
                    ).run(myViewModel::sendEvent)
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
                photoUri = FileProvider.getUriForFile(
                    /* context = */ context,
                    /* authority = */ context.packageName + ".provider",
                    /* file = */ File(context.filesDir, "temp_invoice_picture")
                )
                takePhotoLauncher.launch(photoUri!!)
            },
            onSelectFile = {
                selectFileLauncher.launch(arrayOf("image/jpeg", "application/pdf"))
            }
        )

        if (uiState.attachInvoiceError != null) {
            Toast.makeText(context, uiState.attachInvoiceError, Toast.LENGTH_SHORT).show()
        }

        BackHandler(enabled = true, onBack = onBack)

        LaunchedEffect(key1 = uiState.goBack) {
            if (uiState.goBack) goBack()
        }
    }
}
