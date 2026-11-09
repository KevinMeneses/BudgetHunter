package com.meneses.budgethunter.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.account
import budgethunter.composeapp.generated.resources.add_bank
import budgethunter.composeapp.generated.resources.add_bank_description
import budgethunter.composeapp.generated.resources.ai_processing
import budgethunter.composeapp.generated.resources.ai_processing_description
import budgethunter.composeapp.generated.resources.app_description
import budgethunter.composeapp.generated.resources.app_notifications_off_description
import budgethunter.composeapp.generated.resources.app_notifications_off_explanation
import budgethunter.composeapp.generated.resources.app_notifications_off_title
import budgethunter.composeapp.generated.resources.app_version
import budgethunter.composeapp.generated.resources.bank_may_repeat_movements
import budgethunter.composeapp.generated.resources.bank_reads_nothing
import budgethunter.composeapp.generated.resources.cancel
import budgethunter.composeapp.generated.resources.change_password
import budgethunter.composeapp.generated.resources.come_back
import budgethunter.composeapp.generated.resources.default_budget
import budgethunter.composeapp.generated.resources.default_budget_needed_for_reading
import budgethunter.composeapp.generated.resources.no_default_budget
import budgethunter.composeapp.generated.resources.notification_access_explanation
import budgethunter.composeapp.generated.resources.notification_access_missing
import budgethunter.composeapp.generated.resources.notification_access_title
import budgethunter.composeapp.generated.resources.notification_reader_stopped_description
import budgethunter.composeapp.generated.resources.notification_reader_stopped_explanation
import budgethunter.composeapp.generated.resources.notification_reader_stopped_title
import budgethunter.composeapp.generated.resources.reading_channel_app
import budgethunter.composeapp.generated.resources.reading_channel_email
import budgethunter.composeapp.generated.resources.reading_channel_sms
import budgethunter.composeapp.generated.resources.remove_bank
import budgethunter.composeapp.generated.resources.section_ai
import budgethunter.composeapp.generated.resources.section_automatic_entries
import budgethunter.composeapp.generated.resources.set_password
import budgethunter.composeapp.generated.resources.set_password_description
import budgethunter.composeapp.generated.resources.settings
import budgethunter.composeapp.generated.resources.sign_out
import budgethunter.composeapp.generated.resources.sign_out_confirmation
import budgethunter.composeapp.generated.resources.sign_out_unsynced_warning
import budgethunter.composeapp.generated.resources.signed_in
import budgethunter.composeapp.generated.resources.sms_permission_missing_description
import budgethunter.composeapp.generated.resources.sms_permission_missing_title
import budgethunter.composeapp.generated.resources.sms_shortcut_settings_description
import budgethunter.composeapp.generated.resources.sms_shortcut_settings_row
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.commons.ui.ConfirmationModal
import com.meneses.budgethunter.commons.ui.LoadingOverlay
import com.meneses.budgethunter.commons.util.Platform
import com.meneses.budgethunter.settings.application.SettingsIntent
import com.meneses.budgethunter.settings.application.SettingsState
import com.meneses.budgethunter.sms.domain.BankChannels
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.sms.domain.ReadingChannel
import com.meneses.budgethunter.theme.AppColors
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Settings as a grouped list: a label per section and one rounded group of rows under it, with
 * the account on top and the app's version at the foot. Each row's subtitle says what the option
 * does; the switch already says whether it is on.
 */
@Serializable
object SettingsScreen {
    @Composable
    fun Show(
        uiState: SettingsState,
        onIntent: (SettingsIntent) -> Unit,
        goBack: () -> Unit,
        showSmsShortcutTutorial: () -> Unit
    ) {
        val snackbarHostState = remember { SnackbarHostState() }
        // Notification access is granted in the system settings, so it is read again on the way back
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
            onIntent(SettingsIntent.RefreshPermissions)
        }
        uiState.signOutError?.let { error ->
            val message = stringResource(error)
            LaunchedEffect(error) {
                snackbarHostState.showSnackbar(message)
                onIntent(SettingsIntent.DismissSignOutError)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    AppBar(
                        title = stringResource(Res.string.settings),
                        leftButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
                        onLeftButtonClick = goBack,
                        leftButtonDescription = stringResource(Res.string.come_back)
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val email = uiState.email
                    if (uiState.isSignedIn && email != null) {
                        AccountHeader(email = email)
                    }

                    SectionLabel(stringResource(Res.string.section_automatic_entries))
                    if (Platform.isAndroid) {
                        SmsReadingGroup(uiState = uiState, onIntent = onIntent)
                    } else {
                        SmsShortcutGroup(
                            uiState = uiState,
                            onOpenTutorial = showSmsShortcutTutorial,
                            onIntent = onIntent
                        )
                    }

                    SectionLabel(stringResource(Res.string.section_ai))
                    SettingsGroup {
                        SettingRow(
                            icon = Icons.Default.AutoAwesome,
                            title = stringResource(Res.string.ai_processing),
                            subtitle = stringResource(Res.string.ai_processing_description),
                            switchChecked = uiState.isAiProcessingEnabled,
                            onSwitchChange = { onIntent(SettingsIntent.ToggleAiProcessing(it)) }
                        )
                    }

                    if (uiState.isSignedIn) {
                        SectionLabel(stringResource(Res.string.account))
                        SettingsGroup {
                            SettingRow(
                                icon = Icons.Default.Lock,
                                title = stringResource(
                                    if (uiState.hasPassword) Res.string.change_password else Res.string.set_password
                                ),
                                subtitle = stringResource(Res.string.set_password_description),
                                onClick = { onIntent(SettingsIntent.ShowPasswordDialog) }
                            )
                            GroupDivider()
                            SettingRow(
                                icon = Icons.AutoMirrored.Filled.Logout,
                                title = stringResource(Res.string.sign_out),
                                contentColor = AppColors.error,
                                showChevron = false,
                                onClick = { onIntent(SettingsIntent.ToggleSignOutConfirmation(true)) }
                            )
                        }
                    }

                    AboutFooter()
                }
            }

            // Covers the first frames, until the device's values are read, so the screen never
            // flashes its empty defaults. The account refresh is not waited on.
            if (uiState.isLoading || uiState.isSigningOut) {
                LoadingOverlay()
            }
        }

        if (uiState.isDefaultBudgetSelectorVisible) {
            DefaultBudgetSelectorModal(
                availableBudgets = uiState.allBudgets,
                currentDefaultBudget = uiState.defaultBudget,
                onDismiss = { onIntent(SettingsIntent.HideDefaultBudgetSelector) },
                onBudgetSelected = { budget ->
                    onIntent(SettingsIntent.SetDefaultBudget(budget))
                }
            )
        }

        if (uiState.isBankSelectorVisible) {
            AddBankModal(
                banks = uiState.unselectedBanks,
                onDismiss = { onIntent(SettingsIntent.HideBankSelector) },
                onBankSelected = { bank -> onIntent(SettingsIntent.AddBank(bank)) }
            )
        }

        if (uiState.isPasswordDialogVisible) {
            PasswordModal(
                hasPassword = uiState.hasPassword,
                isSaving = uiState.isSavingPassword,
                error = uiState.passwordError,
                onDismiss = { onIntent(SettingsIntent.HidePasswordDialog) },
                onSave = { current, new ->
                    onIntent(SettingsIntent.SavePassword(current, new))
                }
            )
        }

        if (uiState.isNotificationAccessDialogVisible) {
            ManualPermissionDialog(
                title = stringResource(Res.string.notification_access_title),
                explanation = stringResource(Res.string.notification_access_explanation),
                onDismiss = { onIntent(SettingsIntent.HideNotificationAccessDialog) },
                onOpenSettings = { onIntent(SettingsIntent.OpenNotificationAccessSettings) }
            )
        }

        if (uiState.isAppNotificationsDialogVisible) {
            ManualPermissionDialog(
                title = stringResource(Res.string.app_notifications_off_title),
                explanation = stringResource(Res.string.app_notifications_off_explanation),
                onDismiss = { onIntent(SettingsIntent.HideAppNotificationsDialog) },
                onOpenSettings = { onIntent(SettingsIntent.OpenAppNotificationSettings) }
            )
        }

        if (uiState.isNotificationReaderStoppedDialogVisible) {
            ManualPermissionDialog(
                title = stringResource(Res.string.notification_reader_stopped_title),
                explanation = stringResource(Res.string.notification_reader_stopped_explanation),
                onDismiss = { onIntent(SettingsIntent.HideNotificationReaderStoppedDialog) },
                onOpenSettings = { onIntent(SettingsIntent.OpenAppInfoToRestartReader) }
            )
        }

        if (uiState.isManualPermissionDialogVisible) {
            ManualPermissionDialog(
                onDismiss = { onIntent(SettingsIntent.HideManualPermissionDialog) },
                onOpenSettings = { onIntent(SettingsIntent.OpenAppSettings) }
            )
        }

        ConfirmationModal(
            show = uiState.isSignOutConfirmationVisible,
            message = stringResource(
                if (uiState.signOutLosesUnsyncedChanges) Res.string.sign_out_unsynced_warning
                else Res.string.sign_out_confirmation
            ),
            confirmButtonText = stringResource(Res.string.sign_out),
            cancelButtonText = stringResource(Res.string.cancel),
            onDismiss = { onIntent(SettingsIntent.ToggleSignOutConfirmation(false)) },
            onConfirm = { onIntent(SettingsIntent.SignOut) }
        )
    }

    /**
     * Automatic reading on Android: the banks read, each with the channels it is read from. The
     * channels are the one place where reading is decided; the permissions they need are asked for
     * when a channel is picked and warned about below while missing.
     */
    @Composable
    private fun SmsReadingGroup(
        uiState: SettingsState,
        onIntent: (SettingsIntent) -> Unit
    ) {
        SettingsGroup {
            uiState.sortedSelectedBanks.forEach { bank ->
                BankReadingRow(
                    bank = bank,
                    channels = uiState.channelsOf(bank),
                    onChannelChange = { channel, enabled ->
                        onIntent(SettingsIntent.SetBankChannel(bank, channel, enabled))
                    },
                    onRemove = { onIntent(SettingsIntent.RemoveBank(bank)) }
                )
                GroupDivider()
            }
            SettingRow(
                icon = Icons.Default.Add,
                title = stringResource(Res.string.add_bank),
                subtitle = if (uiState.selectedBanks.isEmpty()) {
                    stringResource(Res.string.add_bank_description)
                } else {
                    null
                },
                showChevron = false,
                enabled = uiState.unselectedBanks.isNotEmpty(),
                onClick = { onIntent(SettingsIntent.ShowBankSelector) }
            )
            // What the picked channels need and is still missing, most basic first
            if (uiState.isSmsPermissionMissing) {
                GroupDivider()
                SettingRow(
                    icon = Icons.Default.Warning,
                    title = stringResource(Res.string.sms_permission_missing_title),
                    subtitle = stringResource(Res.string.sms_permission_missing_description),
                    contentColor = AppColors.error,
                    onClick = { onIntent(SettingsIntent.RequestSmsPermission) }
                )
            }
            // The bank apps and the bank emails use the one notification reader: nothing is read
            // from either until the system grants access
            if (uiState.isNotificationReaderNeeded && !uiState.hasNotificationListenerAccess) {
                GroupDivider()
                SettingRow(
                    icon = Icons.Default.Warning,
                    title = stringResource(Res.string.notification_access_title),
                    subtitle = stringResource(Res.string.notification_access_missing),
                    contentColor = AppColors.error,
                    onClick = { onIntent(SettingsIntent.OpenNotificationAccessSettings) }
                )
            } else if (uiState.isNotificationReaderNeeded && !uiState.areAppNotificationsEnabled) {
                // With the app's own notifications off, Android stops handing over the banks' ones
                GroupDivider()
                SettingRow(
                    icon = Icons.Default.Warning,
                    title = stringResource(Res.string.app_notifications_off_title),
                    subtitle = stringResource(Res.string.app_notifications_off_description),
                    contentColor = AppColors.error,
                    onClick = { onIntent(SettingsIntent.OpenAppNotificationSettings) }
                )
            } else if (uiState.isNotificationReaderNeeded && uiState.isNotificationReaderStopped) {
                // Everything granted, yet the system left the reader unbound: only a force stop helps
                GroupDivider()
                SettingRow(
                    icon = Icons.Default.Warning,
                    title = stringResource(Res.string.notification_reader_stopped_title),
                    subtitle = stringResource(Res.string.notification_reader_stopped_description),
                    contentColor = AppColors.error,
                    onClick = { onIntent(SettingsIntent.OpenAppInfoToRestartReader) }
                )
            }
            GroupDivider()
            // Every channel stores the movements in this budget
            SettingRow(
                icon = Icons.Default.AccountBalanceWallet,
                title = stringResource(Res.string.default_budget),
                subtitle = uiState.defaultBudget?.name ?: stringResource(Res.string.no_default_budget),
                // Without it every movement read is dropped (only a notification says so, once it is lost)
                warning = if (uiState.isAnyReadingEnabled && uiState.defaultBudget == null) {
                    stringResource(Res.string.default_budget_needed_for_reading)
                } else {
                    null
                },
                enabled = uiState.isAnyReadingEnabled,
                onClick = { onIntent(SettingsIntent.ShowDefaultBudgetSelector) }
            )
        }
    }

    /**
     * A bank read automatically: a chip per channel it can be read from, and what its choice means
     * when it reads nothing or may register a movement twice.
     */
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun BankReadingRow(
        bank: BankSmsConfig,
        channels: Set<ReadingChannel>,
        onChannelChange: (ReadingChannel, Boolean) -> Unit,
        onRemove: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalance,
                contentDescription = null,
                tint = AppColors.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp).size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = bank.displayName, fontSize = 16.sp, color = AppColors.onSurface)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BankChannels.available(bank).forEach { channel ->
                        val picked = channel in channels
                        FilterChip(
                            selected = picked,
                            onClick = { onChannelChange(channel, !picked) },
                            label = { Text(stringResource(channel.label())) }
                        )
                    }
                }
                val warning = when {
                    channels.isEmpty() -> stringResource(Res.string.bank_reads_nothing)
                    // Nothing tells apart the same movement read twice
                    channels.size > 1 -> stringResource(Res.string.bank_may_repeat_movements)
                    else -> null
                }
                if (warning != null) {
                    Text(
                        text = warning,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = AppColors.error
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.remove_bank, bank.displayName),
                    tint = AppColors.onSurfaceVariant
                )
            }
        }
    }

    private fun ReadingChannel.label(): StringResource = when (this) {
        ReadingChannel.SMS -> Res.string.reading_channel_sms
        ReadingChannel.APP -> Res.string.reading_channel_app
        ReadingChannel.EMAIL -> Res.string.reading_channel_email
    }

    /**
     * iOS cannot read SMS, but a Shortcuts automation can hand them to the app. This only leads
     * to the guide; the automation itself is the switch, so there is no toggle here.
     */
    @Composable
    private fun SmsShortcutGroup(
        uiState: SettingsState,
        onOpenTutorial: () -> Unit,
        onIntent: (SettingsIntent) -> Unit
    ) {
        SettingsGroup {
            SettingRow(
                icon = Icons.Default.Sms,
                title = stringResource(Res.string.sms_shortcut_settings_row),
                subtitle = stringResource(Res.string.sms_shortcut_settings_description),
                onClick = onOpenTutorial
            )
            GroupDivider()
            // The shortcut stores the movements in this budget, as the Android receiver does
            SettingRow(
                icon = Icons.Default.AccountBalanceWallet,
                title = stringResource(Res.string.default_budget),
                subtitle = uiState.defaultBudget?.name ?: stringResource(Res.string.no_default_budget),
                onClick = { onIntent(SettingsIntent.ShowDefaultBudgetSelector) }
            )
        }
    }

    @Composable
    private fun AccountHeader(email: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(AppColors.surfaceContainer)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AppColors.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = email.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.onTertiaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = email,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(Res.string.signed_in),
                    fontSize = 13.sp,
                    color = AppColors.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun SectionLabel(text: String) {
        Text(
            text = text.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
            color = AppColors.primary,
            modifier = Modifier.padding(start = 4.dp, top = 16.dp)
        )
    }

    @Composable
    private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(AppColors.surfaceContainer),
            content = content
        )
    }

    @Composable
    private fun GroupDivider() {
        HorizontalDivider(
            modifier = Modifier.padding(start = 56.dp),
            color = AppColors.outlineVariant.copy(alpha = 0.6f)
        )
    }

    /**
     * One option. With [onSwitchChange] it carries a switch and toggles from the whole row;
     * with [onClick] it opens something and shows a chevron.
     */
    @Composable
    private fun SettingRow(
        icon: ImageVector,
        title: String,
        subtitle: String? = null,
        warning: String? = null,
        enabled: Boolean = true,
        contentColor: Color = AppColors.onSurface,
        switchChecked: Boolean? = null,
        onSwitchChange: ((Boolean) -> Unit)? = null,
        showChevron: Boolean = true,
        onClick: (() -> Unit)? = null
    ) {
        val rowAction: (() -> Unit)? = when {
            !enabled -> null
            switchChecked != null && onSwitchChange != null -> { { onSwitchChange(!switchChecked) } }
            else -> onClick
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (rowAction != null) Modifier.clickable(onClick = rowAction) else Modifier)
                .alpha(if (enabled) 1f else 0.5f)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (contentColor == AppColors.onSurface) AppColors.onSurfaceVariant else contentColor,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    color = contentColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = AppColors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (warning != null) {
                    Text(
                        text = warning,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = AppColors.error,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (switchChecked != null) {
                Switch(
                    checked = switchChecked,
                    onCheckedChange = onSwitchChange,
                    enabled = enabled
                )
            } else if (onClick != null && showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AppColors.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun AboutFooter() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.app_version),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.app_description),
                fontSize = 12.sp,
                color = AppColors.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
