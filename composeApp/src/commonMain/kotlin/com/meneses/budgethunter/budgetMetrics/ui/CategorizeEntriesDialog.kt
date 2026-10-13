package com.meneses.budgethunter.budgetMetrics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.categorize_confirm
import budgethunter.composeapp.generated.resources.categorize_dialog_message
import budgethunter.composeapp.generated.resources.categorize_dialog_progress
import budgethunter.composeapp.generated.resources.categorize_dialog_title
import budgethunter.composeapp.generated.resources.categorize_not_now
import com.meneses.budgethunter.commons.ui.AppAlertDialog
import com.meneses.budgethunter.commons.ui.AppAlertDialogMessage
import org.jetbrains.compose.resources.stringResource

/**
 * Offers to categorize the [waiting] entries that have no category. While the server works the
 * dialog stays up, with its buttons off, because the answer can take seconds on a big budget.
 */
@Composable
fun CategorizeEntriesDialog(
    waiting: Int,
    isCategorizing: Boolean,
    onConfirm: () -> Unit,
    onDecline: () -> Unit
) {
    AppAlertDialog(
        title = stringResource(Res.string.categorize_dialog_title),
        confirmButtonText = stringResource(Res.string.categorize_confirm),
        cancelButtonText = stringResource(Res.string.categorize_not_now),
        onConfirm = onConfirm,
        onCancel = onDecline,
        onDismissRequest = { if (!isCategorizing) onDecline() },
        buttonsEnabled = !isCategorizing
    ) {
        if (isCategorizing) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                AppAlertDialogMessage(stringResource(Res.string.categorize_dialog_progress))
            }
        } else {
            AppAlertDialogMessage(stringResource(Res.string.categorize_dialog_message, waiting))
        }
    }
}
