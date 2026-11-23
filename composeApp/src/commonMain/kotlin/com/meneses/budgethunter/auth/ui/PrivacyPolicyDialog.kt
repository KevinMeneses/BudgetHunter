package com.meneses.budgethunter.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.cancel
import budgethunter.composeapp.generated.resources.privacy_policy_accept
import budgethunter.composeapp.generated.resources.privacy_policy_read
import budgethunter.composeapp.generated.resources.privacy_policy_updated_message
import budgethunter.composeapp.generated.resources.privacy_policy_updated_title
import budgethunter.composeapp.generated.resources.sign_out
import budgethunter.composeapp.generated.resources.sign_out_unsynced_warning
import com.meneses.budgethunter.auth.application.PrivacyPolicyIntent
import com.meneses.budgethunter.auth.application.PrivacyPolicyState
import com.meneses.budgethunter.theme.AppColors
import org.jetbrains.compose.resources.stringResource

/**
 * Asks the account to accept the privacy policy. It cannot be dismissed without a choice: no back,
 * no tap outside. "Sign out" first warns, as Settings does, when it would delete unsynced changes.
 */
@Composable
fun PrivacyPolicyDialog(
    uiState: PrivacyPolicyState,
    onIntent: (PrivacyPolicyIntent) -> Unit
) {
    if (!uiState.isDialogVisible) return
    val uriHandler = LocalUriHandler.current
    val enabled = !uiState.isWorking

    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(Res.string.privacy_policy_updated_title)) },
        text = {
            if (uiState.isSignOutConfirmationVisible) {
                Text(stringResource(Res.string.sign_out_unsynced_warning))
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.privacy_policy_updated_message))
                    TextButton(
                        onClick = { uriHandler.openPrivacyPolicy() },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(stringResource(Res.string.privacy_policy_read))
                    }
                }
            }
        },
        confirmButton = {
            if (uiState.isSignOutConfirmationVisible) {
                TextButton(onClick = { onIntent(PrivacyPolicyIntent.ConfirmSignOut) }, enabled = enabled) {
                    Text(stringResource(Res.string.sign_out), color = AppColors.error)
                }
            } else {
                Button(onClick = { onIntent(PrivacyPolicyIntent.Accept) }, enabled = enabled) {
                    Text(stringResource(Res.string.privacy_policy_accept))
                }
            }
        },
        dismissButton = {
            if (uiState.isSignOutConfirmationVisible) {
                TextButton(onClick = { onIntent(PrivacyPolicyIntent.CancelSignOut) }, enabled = enabled) {
                    Text(stringResource(Res.string.cancel))
                }
            } else {
                TextButton(onClick = { onIntent(PrivacyPolicyIntent.SignOut) }, enabled = enabled) {
                    Text(stringResource(Res.string.sign_out), color = AppColors.error)
                }
            }
        }
    )
}
