package com.meneses.budgethunter.settings.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.meneses.budgethunter.R
import com.meneses.budgethunter.commons.bank.BankSmsConfig
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
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            onEvent(SettingsEvent.HandleSMSPermissionResult(isGranted))
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
                            permissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                        } else {
                            onEvent(SettingsEvent.ToggleSmsReading(enabled))
                        }
                    },
                    onSelectDefaultBudget = {
                        onEvent(SettingsEvent.ShowDefaultBudgetSelector)
                    },
                    onBankSelected = { bank ->
                        onEvent(SettingsEvent.SetSelectedBank(bank))
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
    }

    @Composable
    private fun SmsReadingSection(
        uiState: SettingsState,
        onToggleSmsReading: (Boolean) -> Unit,
        onSelectDefaultBudget: () -> Unit,
        onBankSelected: (BankSmsConfig) -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.sms_reading),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(id = R.string.sms_reading_description),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // SMS Reading Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.enable_sms_reading),
                        fontSize = 16.sp
                    )
                    Switch(
                        checked = uiState.isSmsReadingEnabled,
                        onCheckedChange = onToggleSmsReading
                    )
                }

                if (!uiState.hasSmsPermission) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.sms_permission_required),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Default Budget Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(0.65f)) {
                        Text(
                            text = stringResource(id = R.string.default_budget),
                            fontSize = 16.sp
                        )
                        Text(
                            text = uiState.defaultBudget?.name ?: stringResource(id = R.string.no_default_budget),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(
                        modifier = Modifier.weight(0.3f),
                        onClick = onSelectDefaultBudget
                    ) {
                        Text(
                            text = stringResource(id = R.string.select_default_budget),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 2
                        )
                    }
                }
            }

            BankSelectionSection(
                availableBanks = uiState.availableBanks,
                selectedBank = uiState.selectedBank,
                onBankSelected = onBankSelected
            )
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
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Acerca de",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Cazador de Presupuestos v1.0.0",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Una aplicación para gestionar tus presupuestos de manera inteligente",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class) // Para ExposedDropdownMenuBox
    @Composable
    fun BankSelectionSection(
        availableBanks: List<BankSmsConfig>,
        selectedBank: BankSmsConfig?,
        onBankSelected: (BankSmsConfig) -> Unit,
        modifier: Modifier = Modifier
    ) {
        var expandedBankDropdown by remember { mutableStateOf(false) }

            Column(modifier = modifier.padding(16.dp)) {
                Text(
                    text = stringResource(id = R.string.bank_for_sms_notifications),
                    modifier = Modifier.padding(bottom = 8.dp),
                    fontSize = 16.sp
                )
                ExposedDropdownMenuBox(
                    expanded = expandedBankDropdown,
                    onExpandedChange = { expandedBankDropdown = !expandedBankDropdown },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedBank?.displayName ?: stringResource(id = R.string.select_a_bank),
                        onValueChange = {}, // No se cambia directamente, solo por selección
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBankDropdown) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedBankDropdown,
                        onDismissRequest = { expandedBankDropdown = false }
                    ) {
                        if (availableBanks.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.no_banks_configured)) },
                                onClick = { expandedBankDropdown = false }, // Cierra el menú
                                enabled = false // Deshabilita el ítem
                            )
                        }
                        availableBanks.forEach { bank ->
                            DropdownMenuItem(
                                text = { Text(bank.displayName) },
                                onClick = {
                                    onBankSelected(bank)
                                    expandedBankDropdown =
                                        false // Cierra el menú después de la selección
                                }
                            )
                        }
                    }
                }
            }
    }
}
