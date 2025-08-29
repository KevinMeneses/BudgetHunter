package com.meneses.budgethunter.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.bank.BankSmsConfig
import com.meneses.budgethunter.commons.bank.SupportedBanks
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.LifecycleDelegate
import com.meneses.budgethunter.commons.platform.LifecycleManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.settings.application.SettingsEvent
import com.meneses.budgethunter.settings.application.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val budgetRepository: BudgetRepository,
    private val permissionsManager: PermissionsManager,
    lifecycleManager: LifecycleManager
) : ViewModel(), LifecycleDelegate {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSettings()
        lifecycleManager.setLifecycleDelegate(this)
    }

    fun sendEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ToggleSmsReading -> toggleSmsReading(event.enabled)
            is SettingsEvent.SetDefaultBudget -> setDefaultBudget(event.budget)
            is SettingsEvent.ShowDefaultBudgetSelector -> showDefaultBudgetSelector()
            is SettingsEvent.HideDefaultBudgetSelector -> hideDefaultBudgetSelector()
            is SettingsEvent.ShowBankSelector -> showBankSelector()
            is SettingsEvent.HideBankSelector -> hideBankSelector()
            is SettingsEvent.SetSelectedBanks -> setSelectedBanks(event.bankConfigs)
            is SettingsEvent.ToggleAiProcessing -> toggleAiProcessing(event.enabled)
            is SettingsEvent.ShowManualPermissionDialog -> showManualPermissionDialog()
            is SettingsEvent.HideManualPermissionDialog -> hideManualPermissionDialog()
            is SettingsEvent.OpenAppSettings -> openAppSettings()
        }
    }

    override fun onStart() {
        loadSettings()
    }

    private fun setSelectedBanks(bankConfigs: Set<BankSmsConfig>) = viewModelScope.launch {
        val selectedBankIds = bankConfigs.map { it.id }.toSet()
        preferencesManager.setSelectedBankIds(selectedBankIds)
        _uiState.update { it.copy(selectedBanks = bankConfigs) }
    }

    private fun showBankSelector() {
        _uiState.update { it.copy(isBankSelectorVisible = true) }
    }

    private fun hideBankSelector() {
        _uiState.update { it.copy(isBankSelectorVisible = false) }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val defaultBudgetId = preferencesManager.getDefaultBudgetId()
                val defaultBudget = if (defaultBudgetId != -1) {
                    budgetRepository.getById(defaultBudgetId)
                } else null

                val selectedBankIds = preferencesManager.getSelectedBankIds()
                val selectedBanks = selectedBankIds.mapNotNull { bankId ->
                    SupportedBanks.getBankConfigById(bankId)
                }.toSet()

                _uiState.update {
                    it.copy(
                        isSmsReadingEnabled = preferencesManager.isSmsReadingEnabled(),
                        defaultBudget = defaultBudget,
                        allBudgets = budgetRepository.getAllCached(),
                        hasSmsPermission = permissionsManager.hasSmsPermission(),
                        availableBanks = SupportedBanks.ALL_BANKS,
                        selectedBanks = selectedBanks,
                        isAiProcessingEnabled = preferencesManager.isAiProcessingEnabled(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun toggleSmsReading(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setSmsReadingEnabled(enabled)
        _uiState.update { it.copy(isSmsReadingEnabled = enabled) }
        if (!enabled) return@launch

        when {
            !permissionsManager.hasSmsPermission() && !permissionsManager.shouldShowSMSPermissionRationale() -> {
                permissionsManager.requestSmsPermissions { granted ->
                    _uiState.update { it.copy(hasSmsPermission = granted) }
                }
            }

            permissionsManager.shouldShowSMSPermissionRationale() -> {
                showManualPermissionDialog()
            }

            else -> {
                _uiState.update { it.copy(hasSmsPermission = true) }
            }
        }
    }

    private fun toggleAiProcessing(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setAiProcessingEnabled(enabled)
        _uiState.update { it.copy(isAiProcessingEnabled = enabled) }
    }

    private fun setDefaultBudget(budget: Budget) = viewModelScope.launch {
        preferencesManager.setDefaultBudgetId(budget.id)
        _uiState.update {
            it.copy(
                defaultBudget = budget,
                isDefaultBudgetSelectorVisible = false
            )
        }
    }


    private fun showDefaultBudgetSelector() {
        _uiState.update { it.copy(isDefaultBudgetSelectorVisible = true) }
    }

    private fun hideDefaultBudgetSelector() {
        _uiState.update { it.copy(isDefaultBudgetSelectorVisible = false) }
    }

    private fun showManualPermissionDialog() {
        _uiState.update { it.copy(isManualPermissionDialogVisible = true) }
    }

    private fun hideManualPermissionDialog() {
        _uiState.update { it.copy(isManualPermissionDialogVisible = false) }
    }

    private fun openAppSettings() {
        permissionsManager.openAppSettings()
        hideManualPermissionDialog()
    }
}
