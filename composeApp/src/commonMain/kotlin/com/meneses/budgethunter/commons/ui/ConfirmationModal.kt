package com.meneses.budgethunter.commons.ui

import androidx.compose.runtime.Composable
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.confirmation
import org.jetbrains.compose.resources.stringResource

@Composable
fun ConfirmationModal(
    show: Boolean,
    message: String,
    confirmButtonText: String,
    cancelButtonText: String,
    title: String? = null, // Make title configurable for KMP
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (show) {
        AppAlertDialog(
            title = title ?: stringResource(Res.string.confirmation),
            confirmButtonText = confirmButtonText,
            cancelButtonText = cancelButtonText,
            onConfirm = {
                onConfirm()
                onDismiss()
            },
            onCancel = onDismiss,
            onDismissRequest = onDismiss
        ) {
            AppAlertDialogMessage(message)
        }
    }
}
