package com.meneses.budgethunter.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.error_current_password_incorrect
import budgethunter.composeapp.generated.resources.error_password_too_short
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.settings.application.SettingsIntent
import com.meneses.budgethunter.settings.application.SettingsState
import com.meneses.budgethunter.sms.domain.SupportedBanks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val budgetRepository: BudgetRepository,
    private val permissionsManager: PermissionsManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSettings()
        loadAccount()
    }

    fun sendIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.ToggleSmsReading -> toggleSmsReading(intent.enabled)
            is SettingsIntent.SetDefaultBudget -> setDefaultBudget(intent.budget)
            is SettingsIntent.ShowDefaultBudgetSelector -> showDefaultBudgetSelector()
            is SettingsIntent.HideDefaultBudgetSelector -> hideDefaultBudgetSelector()
            is SettingsIntent.ShowBankSelector -> showBankSelector()
            is SettingsIntent.HideBankSelector -> hideBankSelector()
            is SettingsIntent.SetSelectedBanks -> setSelectedBanks(intent.bankConfigs)
            is SettingsIntent.ToggleAiProcessing -> toggleAiProcessing(intent.enabled)
            is SettingsIntent.ShowManualPermissionDialog -> showManualPermissionDialog()
            is SettingsIntent.HideManualPermissionDialog -> hideManualPermissionDialog()
            is SettingsIntent.OpenAppSettings -> openAppSettings()
            is SettingsIntent.ShowPasswordDialog -> showPasswordDialog()
            is SettingsIntent.HidePasswordDialog -> hidePasswordDialog()
            is SettingsIntent.SavePassword -> savePassword(intent.currentPassword, intent.newPassword)
        }
    }

    private fun setSelectedBanks(bankConfigs: Set<com.meneses.budgethunter.sms.domain.BankSmsConfig>) = viewModelScope.launch {
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
            } catch (_: Exception) {
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

    /**
     * Asks the server whether this account has a password.
     *
     * Deliberately a live call rather than something cached at sign in: the answer can change on
     * another device, and a stale "no password" would offer to set one that already exists.
     */
    private fun loadAccount() {
        viewModelScope.launch {
            if (!authRepository.isAuthenticated()) return@launch

            authRepository.getCurrentUser().onSuccess { user ->
                _uiState.update { it.copy(isSignedIn = true, hasPassword = user.hasPassword) }
            }
        }
    }

    private fun showPasswordDialog() {
        _uiState.update { it.copy(isPasswordDialogVisible = true, passwordError = null) }
    }

    private fun hidePasswordDialog() {
        _uiState.update {
            it.copy(isPasswordDialogVisible = false, passwordError = null, isSavingPassword = false)
        }
    }

    private fun savePassword(currentPassword: String, newPassword: String) {
        if (newPassword.length < MIN_PASSWORD_LENGTH) {
            _uiState.update { it.copy(passwordError = Res.string.error_password_too_short) }
            return
        }

        _uiState.update { it.copy(isSavingPassword = true, passwordError = null) }

        viewModelScope.launch {
            authRepository.setPassword(
                // An account without one has no current password to send, and the server does not
                // ask for it in that case.
                currentPassword = currentPassword.takeIf { _uiState.value.hasPassword },
                newPassword = newPassword
            ).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isPasswordDialogVisible = false,
                            isSavingPassword = false,
                            hasPassword = true
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(
                            isSavingPassword = false,
                            passwordError = Res.string.error_current_password_incorrect
                        )
                    }
                }
            )
        }
    }

    private fun openAppSettings() {
        permissionsManager.openAppSettings()
        hideManualPermissionDialog()
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 6
    }
}
