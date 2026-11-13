package com.meneses.budgethunter.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.add_bank
import budgethunter.composeapp.generated.resources.cancel
import budgethunter.composeapp.generated.resources.change_password
import budgethunter.composeapp.generated.resources.current_password
import budgethunter.composeapp.generated.resources.delete_account
import budgethunter.composeapp.generated.resources.delete_account_device
import budgethunter.composeapp.generated.resources.delete_account_other_budgets
import budgethunter.composeapp.generated.resources.delete_account_owned_budgets
import budgethunter.composeapp.generated.resources.delete_account_permanent
import budgethunter.composeapp.generated.resources.delete_account_subscription_premium
import budgethunter.composeapp.generated.resources.delete_account_subscription_unknown
import budgethunter.composeapp.generated.resources.delete_account_title
import budgethunter.composeapp.generated.resources.hide_password
import budgethunter.composeapp.generated.resources.manage_subscription
import budgethunter.composeapp.generated.resources.new_password
import budgethunter.composeapp.generated.resources.save
import budgethunter.composeapp.generated.resources.set_password
import budgethunter.composeapp.generated.resources.set_password_description
import budgethunter.composeapp.generated.resources.settings
import budgethunter.composeapp.generated.resources.show_password
import budgethunter.composeapp.generated.resources.sms_permission_explanation
import budgethunter.composeapp.generated.resources.sms_permission_required
import com.meneses.budgethunter.commons.ui.AppAlertDialog
import com.meneses.budgethunter.commons.ui.AppAlertDialogMessage
import com.meneses.budgethunter.commons.util.Platform
import com.meneses.budgethunter.settings.application.SettingsState.SubscriptionNotice
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The banks that can still be read automatically; tapping one adds it with its default channels. */
@Composable
fun AddBankModal(
    banks: List<BankSmsConfig>,
    onDismiss: () -> Unit,
    onBankSelected: (BankSmsConfig) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.add_bank),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(banks) { bank ->
                    // Its channels are picked on its row once added
                    Text(
                        text = bank.displayName,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBankSelected(bank) }
                            .padding(vertical = 14.dp)
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}

@Composable
fun ManualPermissionDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    title: String = stringResource(Res.string.sms_permission_required),
    explanation: String = stringResource(Res.string.sms_permission_explanation)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Text(
                text = explanation,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(stringResource(Res.string.settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    )
}

/**
 * Sets a first password, or replaces an existing one.
 *
 * [hasPassword] decides both the wording and whether the current-password field appears: an
 * account created through Google has none, and asking for one it never had would be a dead end.
 */
@Composable
fun PasswordModal(
    hasPassword: Boolean,
    isSaving: Boolean,
    error: StringResource?,
    onDismiss: () -> Unit,
    onSave: (currentPassword: String, newPassword: String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (hasPassword) Res.string.change_password else Res.string.set_password
                )
            )
        },
        text = {
            Column {
                if (!hasPassword) {
                    Text(
                        text = stringResource(Res.string.set_password_description),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (hasPassword) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text(stringResource(Res.string.current_password)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(Res.string.new_password)) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                },
                                contentDescription = stringResource(
                                    if (passwordVisible) Res.string.hide_password else Res.string.show_password
                                )
                            )
                        }
                    },
                    singleLine = true
                )

                error?.let {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(currentPassword, newPassword) },
                enabled = !isSaving &&
                    newPassword.isNotBlank() &&
                    (!hasPassword || currentPassword.isNotBlank())
            ) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}

/**
 * Confirms deleting the account, saying in plain words what happens to the user's budgets and,
 * when the account is or may be premium, that the store subscription has to be cancelled apart.
 */
@Composable
fun DeleteAccountDialog(
    subscriptionNotice: SubscriptionNotice,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AppAlertDialog(
        title = stringResource(Res.string.delete_account_title),
        confirmButtonText = stringResource(Res.string.delete_account),
        cancelButtonText = stringResource(Res.string.cancel),
        onConfirm = onConfirm,
        onCancel = onDismiss,
        onDismissRequest = onDismiss,
        destructive = true
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppAlertDialogMessage(stringResource(Res.string.delete_account_permanent))
            AppAlertDialogMessage(stringResource(Res.string.delete_account_owned_budgets))
            AppAlertDialogMessage(stringResource(Res.string.delete_account_other_budgets))
            AppAlertDialogMessage(stringResource(Res.string.delete_account_device))
            if (subscriptionNotice != SubscriptionNotice.NONE) {
                SubscriptionWarning(premium = subscriptionNotice == SubscriptionNotice.PREMIUM)
            }
        }
    }
}

@Composable
private fun SubscriptionWarning(premium: Boolean) {
    val store = if (Platform.isAndroid) GOOGLE_PLAY else APP_STORE
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = stringResource(
                if (premium) Res.string.delete_account_subscription_premium
                else Res.string.delete_account_subscription_unknown,
                store
            ),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.error
        )
        TextButton(
            onClick = {
                // No browser or store app to open it: the text above still says where to go
                runCatching {
                    uriHandler.openUri(if (Platform.isAndroid) PLAY_SUBSCRIPTIONS_URL else APP_STORE_SUBSCRIPTIONS_URL)
                }
            },
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(stringResource(Res.string.manage_subscription, store))
        }
    }
}

private const val GOOGLE_PLAY = "Google Play"
private const val APP_STORE = "App Store"
private const val PLAY_SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"
private const val APP_STORE_SUBSCRIPTIONS_URL = "https://apps.apple.com/account/subscriptions"
