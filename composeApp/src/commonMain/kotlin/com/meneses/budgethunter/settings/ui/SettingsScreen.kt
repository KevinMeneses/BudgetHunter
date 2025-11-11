package com.meneses.budgethunter.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.about
import budgethunter.composeapp.generated.resources.activated
import budgethunter.composeapp.generated.resources.ai_processing
import budgethunter.composeapp.generated.resources.ai_processing_description
import budgethunter.composeapp.generated.resources.app_description
import budgethunter.composeapp.generated.resources.app_version
import budgethunter.composeapp.generated.resources.bank_for_sms_notifications
import budgethunter.composeapp.generated.resources.come_back
import budgethunter.composeapp.generated.resources.deactivated
import budgethunter.composeapp.generated.resources.default_budget
import budgethunter.composeapp.generated.resources.enable_ai_processing
import budgethunter.composeapp.generated.resources.enable_sms_reading
import budgethunter.composeapp.generated.resources.no_default_budget
import budgethunter.composeapp.generated.resources.settings
import budgethunter.composeapp.generated.resources.sms_reading
import budgethunter.composeapp.generated.resources.sms_reading_description
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.commons.util.Platform
import com.meneses.budgethunter.settings.application.SettingsEvent
import com.meneses.budgethunter.settings.application.SettingsState
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource

@Serializable
object SettingsScreen {
    @Composable
    fun Show(
        uiState: SettingsState,
        onEvent: (SettingsEvent) -> Unit,
        goBack: () -> Unit
    ) {
        Scaffold(
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
            ) {
                SmsReadingSection(
                    uiState = uiState,
                    onToggleSmsReading = { enabled ->
                        onEvent(SettingsEvent.ToggleSmsReading(enabled))
                    },
                    onSelectDefaultBudget = {
                        onEvent(SettingsEvent.ShowDefaultBudgetSelector)
                    },
                    onSelectBanks = {
                        onEvent(SettingsEvent.ShowBankSelector)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))
                AiProcessingSection(
                    uiState = uiState,
                    onToggleAiProcessing = { enabled ->
                        onEvent(SettingsEvent.ToggleAiProcessing(enabled))
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()

                Spacer(modifier = Modifier.height(16.dp))
                AboutSection()
            }
        }

        // Default Budget Selector Modal
        if (uiState.isDefaultBudgetSelectorVisible) {
            DefaultBudgetSelectorModal(
                availableBudgets = uiState.allBudgets,
                currentDefaultBudget = uiState.defaultBudget,
                onDismiss = { onEvent(SettingsEvent.HideDefaultBudgetSelector) },
                onBudgetSelected = { budget ->
                    onEvent(SettingsEvent.SetDefaultBudget(budget))
                }
            )
        }

        // Bank Selector Modal
        if (uiState.isBankSelectorVisible) {
            BankSelectorModal(
                availableBanks = uiState.availableBanks,
                selectedBanks = uiState.selectedBanks,
                onDismiss = { onEvent(SettingsEvent.HideBankSelector) },
                onBanksSelected = { banks ->
                    onEvent(SettingsEvent.SetSelectedBanks(banks))
                }
            )
        }

        // Manual Permission Dialog
        if (uiState.isManualPermissionDialogVisible) {
            ManualPermissionDialog(
                onDismiss = { onEvent(SettingsEvent.HideManualPermissionDialog) },
                onOpenSettings = { onEvent(SettingsEvent.OpenAppSettings) }
            )
        }
    }

    @Composable
    private fun SmsReadingSection(
        uiState: SettingsState,
        onToggleSmsReading: (Boolean) -> Unit,
        onSelectDefaultBudget: () -> Unit,
        onSelectBanks: () -> Unit
    ) {
        // SMS Reading section - Only show on Android platform
        if (Platform.isAndroid) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(Res.string.sms_reading),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(Res.string.sms_reading_description),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // SMS Reading Toggle
                    SettingItem(
                        icon = Icons.Default.Email,
                        title = stringResource(Res.string.enable_sms_reading),
                        subtitle = if (uiState.isSmsReadingEnabled) stringResource(Res.string.activated) else stringResource(Res.string.deactivated),
                        showSwitch = true,
                        switchChecked = uiState.isSmsReadingEnabled,
                        onSwitchChange = onToggleSmsReading
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Default Budget
                    SettingItem(
                        icon = Icons.Default.AccountCircle,
                        title = stringResource(Res.string.default_budget),
                        subtitle = uiState.defaultBudget?.name ?: stringResource(Res.string.no_default_budget),
                        showButton = true,
                        onButtonClick = onSelectDefaultBudget,
                        enabled = uiState.isSmsReadingEnabled
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bank Selection
                    SettingItem(
                        icon = Icons.Default.Build,
                        title = stringResource(Res.string.bank_for_sms_notifications),
                        subtitle = if (uiState.selectedBanks.isNotEmpty()) {
                            "${uiState.selectedBanks.size} banks selected"
                        } else {
                            "No banks selected"
                        },
                        showButton = true,
                        onButtonClick = onSelectBanks,
                        enabled = uiState.isSmsReadingEnabled
                    )
                }
            }
        }
    }

    @Composable
    private fun SettingItem(
        icon: ImageVector,
        title: String,
        subtitle: String,
        showSwitch: Boolean = false,
        switchChecked: Boolean = false,
        onSwitchChange: ((Boolean) -> Unit)? = null,
        showButton: Boolean = false,
        onButtonClick: (() -> Unit)? = null,
        enabled: Boolean = true
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (enabled)
                    MaterialTheme.colorScheme.surface
                else
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            val clickableModifier = if (showButton && onButtonClick != null && enabled && !showSwitch) {
                Modifier.clickable { onButtonClick() }
            } else {
                Modifier
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(clickableModifier)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (showSwitch) {
                    Switch(
                        checked = switchChecked,
                        onCheckedChange = onSwitchChange,
                        enabled = enabled
                    )
                } else if (showButton) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    @Composable
    private fun AiProcessingSection(
        uiState: SettingsState,
        onToggleAiProcessing: (Boolean) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(Res.string.ai_processing),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(Res.string.ai_processing_description),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(20.dp))

                // AI Processing Toggle
                SettingItem(
                    icon = Icons.Default.Psychology,
                    title = stringResource(Res.string.enable_ai_processing),
                    subtitle = if (uiState.isAiProcessingEnabled) stringResource(Res.string.activated) else stringResource(Res.string.deactivated),
                    showSwitch = true,
                    switchChecked = uiState.isAiProcessingEnabled,
                    onSwitchChange = onToggleAiProcessing
                )
            }
        }
    }

    @Composable
    private fun AboutSection() {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(Res.string.about),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(Res.string.app_version),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(Res.string.app_description),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
