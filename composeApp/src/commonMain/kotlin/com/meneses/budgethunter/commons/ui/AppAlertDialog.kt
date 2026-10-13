package com.meneses.budgethunter.commons.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

/**
 * The look every two-button dialog of the app shares. It only draws: what the buttons do, and
 * when the dialog goes away, is up to the caller.
 */
@Composable
fun AppAlertDialog(
    title: String,
    confirmButtonText: String,
    cancelButtonText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismissRequest: () -> Unit,
    buttonsEnabled: Boolean = true,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = content,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = buttonsEnabled) {
                Text(
                    text = confirmButtonText,
                    fontWeight = FontWeight.Medium,
                    color = if (buttonsEnabled) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = buttonsEnabled) {
                Text(
                    text = cancelButtonText,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        properties = DialogProperties()
    )
}

/** Body text of an [AppAlertDialog]. */
@Composable
fun AppAlertDialogMessage(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
