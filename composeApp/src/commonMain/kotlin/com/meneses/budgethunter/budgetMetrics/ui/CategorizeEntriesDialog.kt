package com.meneses.budgethunter.budgetMetrics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    AlertDialog(
        onDismissRequest = { if (!isCategorizing) onDecline() },
        title = { Text(stringResource(Res.string.categorize_dialog_title)) },
        text = {
            if (isCategorizing) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text(stringResource(Res.string.categorize_dialog_progress))
                }
            } else {
                Text(stringResource(Res.string.categorize_dialog_message, waiting))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isCategorizing) {
                Text(stringResource(Res.string.categorize_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline, enabled = !isCategorizing) {
                Text(stringResource(Res.string.categorize_not_now))
            }
        }
    )
}
