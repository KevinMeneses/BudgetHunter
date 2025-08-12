package com.meneses.budgethunter.settings.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meneses.budgethunter.R
import com.meneses.budgethunter.commons.ui.AppBar
import com.meneses.budgethunter.settings.application.SettingsEvent
import com.meneses.budgethunter.settings.application.SettingsState
import kotlinx.serialization.Serializable

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    SettingsScreen.Show(
        uiState = SettingsState(),
        onEvent = { },
        goBack = { }
    )
}

@Serializable
object SettingsScreen {
    @Composable
    fun Show(
        uiState: SettingsState,
        onEvent: (SettingsEvent) -> Unit,
        goBack: () -> Unit
    ) {
        val permissionsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) {
            val receiveSmsGranted = it[Manifest.permission.RECEIVE_SMS] ?: false
            onEvent(SettingsEvent.HandleSMSPermissionResult(receiveSmsGranted))
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(id = R.string.settings),
                    leftButtonIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onLeftButtonClick = goBack,
                    leftButtonDescription = stringResource(id = R.string.come_back)
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
                        if (enabled && !uiState.hasSmsPermission) {
                            val permissions = arrayOf(Manifest.permission.RECEIVE_SMS, "")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissions[1] = Manifest.permission.POST_NOTIFICATIONS
                            }
                            permissionsLauncher.launch(permissions)
                        } else {
                            onEvent(SettingsEvent.ToggleSmsReading(enabled))
                        }
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
    }

    @Composable
    private fun SmsReadingSection(
        uiState: SettingsState,
        onToggleSmsReading: (Boolean) -> Unit,
        onSelectDefaultBudget: () -> Unit,
        onSelectBanks: () -> Unit
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
                        imageVector = Icons.Default.Email,
                        contentDescription = stringResource(R.string.sms_reading_icon_description),
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.sms_reading),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(id = R.string.sms_reading_description),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Settings Items with better spacing
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // SMS Reading Toggle
                    SettingItem(
                        icon = Icons.Default.Notifications,
                        title = stringResource(id = R.string.enable_sms_reading),
                        subtitle = if (uiState.isSmsReadingEnabled) "Activado" else "Desactivado",
                        showSwitch = true,
                        switchChecked = uiState.isSmsReadingEnabled,
                        onSwitchChange = onToggleSmsReading,
                        showError = !uiState.hasSmsPermission && uiState.isSmsReadingEnabled,
                        errorMessage = stringResource(id = R.string.sms_permission_required)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Default Budget Selection
                    SettingItem(
                        icon = Icons.Default.AccountCircle,
                        title = stringResource(id = R.string.default_budget),
                        subtitle = uiState.defaultBudget?.name ?: stringResource(id = R.string.no_default_budget),
                        showButton = true,
                        onButtonClick = onSelectDefaultBudget,
                        enabled = uiState.isSmsReadingEnabled
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bank Selection
                    SettingItem(
                        icon = Icons.Default.Build,
                        title = stringResource(id = R.string.bank_for_sms_notifications),
                        subtitle = if (uiState.selectedBanks.isEmpty()) {
                            stringResource(id = R.string.no_banks_selected)
                        } else {
                            stringResource(id = R.string.banks_selected, uiState.selectedBanks.size)
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
        showError: Boolean = false,
        errorMessage: String = "",
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
                    contentDescription = stringResource(R.string.default_budget_icon_description),
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
                    if (showError) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                        contentDescription = stringResource(R.string.bank_selection_icon_description),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                        contentDescription = stringResource(R.string.about_icon_description),
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.about),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.app_version),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.app_description),
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
