package com.meneses.budgethunter.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.apply
import budgethunter.composeapp.generated.resources.bank_channels_hint
import budgethunter.composeapp.generated.resources.bank_source_notifications
import budgethunter.composeapp.generated.resources.bank_source_sms
import budgethunter.composeapp.generated.resources.bank_source_sms_and_notifications
import budgethunter.composeapp.generated.resources.cancel
import budgethunter.composeapp.generated.resources.change_password
import budgethunter.composeapp.generated.resources.current_password
import budgethunter.composeapp.generated.resources.hide_password
import budgethunter.composeapp.generated.resources.new_password
import budgethunter.composeapp.generated.resources.reading_channel_app
import budgethunter.composeapp.generated.resources.reading_channel_email
import budgethunter.composeapp.generated.resources.reading_channel_sms
import budgethunter.composeapp.generated.resources.save
import budgethunter.composeapp.generated.resources.select_banks
import budgethunter.composeapp.generated.resources.set_password
import budgethunter.composeapp.generated.resources.set_password_description
import budgethunter.composeapp.generated.resources.settings
import budgethunter.composeapp.generated.resources.show_password
import budgethunter.composeapp.generated.resources.sms_permission_explanation
import budgethunter.composeapp.generated.resources.sms_permission_required
import com.meneses.budgethunter.sms.domain.BankChannels
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.sms.domain.ReadingChannel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun BankSelectorModal(
    availableBanks: List<BankSmsConfig>,
    selectedBanks: Set<BankSmsConfig>,
    bankChannels: Map<String, Set<ReadingChannel>>,
    onDismiss: () -> Unit,
    onBanksSelected: (Set<BankSmsConfig>, Map<String, Set<ReadingChannel>>) -> Unit
) {
    var tempSelectedBanks by remember { mutableStateOf(selectedBanks) }
    // Each selected bank starts from the channels it is read from now, its defaults when never picked
    var tempChannels by remember {
        mutableStateOf(selectedBanks.associate { it.id to BankChannels.of(it, bankChannels) })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(Res.string.select_banks),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(360.dp)
            ) {
                items(availableBanks) { bank ->
                    val isSelected = tempSelectedBanks.contains(bank)
                    BankOptionItem(
                        bank = bank,
                        isSelected = isSelected,
                        channels = tempChannels[bank.id] ?: BankChannels.default(bank),
                        onToggle = { select ->
                            if (select) {
                                tempSelectedBanks = tempSelectedBanks + bank
                                tempChannels = tempChannels + (bank.id to BankChannels.default(bank))
                            } else {
                                tempSelectedBanks = tempSelectedBanks - bank
                                tempChannels = tempChannels - bank.id
                            }
                        },
                        onChannelsChange = { channels -> tempChannels = tempChannels + (bank.id to channels) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onBanksSelected(tempSelectedBanks, tempChannels)
                    onDismiss()
                }
            ) {
                Text(stringResource(Res.string.apply))
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

@Composable
private fun BankOptionItem(
    bank: BankSmsConfig,
    isSelected: Boolean,
    channels: Set<ReadingChannel>,
    onToggle: (Boolean) -> Unit,
    onChannelsChange: (Set<ReadingChannel>) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onToggle(!isSelected) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onToggle
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = bank.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isSelected) {
                    // A bank may send some alerts by one channel and others by another, so any
                    // of them can be picked, at least one
                    Text(
                        text = stringResource(Res.string.bank_channels_hint),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        BankChannels.available(bank).forEach { channel ->
                            val picked = channel in channels
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    val updated = if (picked) channels - channel else channels + channel
                                    if (updated.isNotEmpty()) onChannelsChange(updated)
                                },
                                label = { Text(stringResource(channel.label())) }
                            )
                        }
                    }
                } else {
                    // Which switch reads this bank, so the user knows what selecting it does
                    val source = when {
                        bank.readsSms && bank.readsNotifications -> Res.string.bank_source_sms_and_notifications
                        bank.readsNotifications -> Res.string.bank_source_notifications
                        else -> Res.string.bank_source_sms
                    }
                    Text(
                        text = stringResource(source),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun ReadingChannel.label(): StringResource = when (this) {
    ReadingChannel.SMS -> Res.string.reading_channel_sms
    ReadingChannel.APP -> Res.string.reading_channel_app
    ReadingChannel.EMAIL -> Res.string.reading_channel_email
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
