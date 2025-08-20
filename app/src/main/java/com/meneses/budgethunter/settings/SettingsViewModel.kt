package com.meneses.budgethunter.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.MyApplication
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.bank.BankSmsConfig
import com.meneses.budgethunter.commons.bank.SupportedBanks
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.settings.application.SettingsEvent
import com.meneses.budgethunter.settings.application.SettingsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesManager: PreferencesManager = MyApplication.preferencesManager,
    private val budgetRepository: BudgetRepository = BudgetRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState = _uiState.asStateFlow()

    fun sendEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ToggleSmsReading -> toggleSmsReading(event.enabled)
            is SettingsEvent.SetDefaultBudget -> setDefaultBudget(event.budget)
            is SettingsEvent.HandleSMSPermissionResult -> handleSmsPermission(event.granted)
            is SettingsEvent.ShowDefaultBudgetSelector -> showDefaultBudgetSelector()
            is SettingsEvent.HideDefaultBudgetSelector -> hideDefaultBudgetSelector()
            is SettingsEvent.ShowBankSelector -> showBankSelector()
            is SettingsEvent.HideBankSelector -> hideBankSelector()
            is SettingsEvent.SetSelectedBanks -> setSelectedBanks(event.bankConfigs)
        }
    }

    private fun setSelectedBanks(bankConfigs: Set<BankSmsConfig>) {
        preferencesManager.selectedBankIds = bankConfigs.map { it.id }.toSet()
        _uiState.update { it.copy(selectedBanks = bankConfigs) }
    }

    private fun showBankSelector() {
        _uiState.update { it.copy(isBankSelectorVisible = true) }
    }

    private fun hideBankSelector() {
        _uiState.update { it.copy(isBankSelectorVisible = false) }
    }

    fun loadSettings(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val defaultBudgetId = preferencesManager.defaultBudgetId
                val defaultBudget = if (defaultBudgetId != -1) {
                    budgetRepository.getById(defaultBudgetId)
                } else null

                // Load selected bank IDs and convert to BankSmsConfig objects
                val selectedBankIds = preferencesManager.selectedBankIds
                val selectedBanks = selectedBankIds.mapNotNull { bankId ->
                    SupportedBanks.getBankConfigById(bankId)
                }.toSet()

                _uiState.update {
                    it.copy(
                        isSmsReadingEnabled = preferencesManager.isSmsReadingEnabled,
                        defaultBudget = defaultBudget,
                        allBudgets = budgetRepository.getAllCached(),
                        hasSmsPermission = checkSmsPermission(context),
                        availableBanks = SupportedBanks.ALL_BANKS,
                        selectedBanks = selectedBanks,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun toggleSmsReading(enabled: Boolean) {
        preferencesManager.isSmsReadingEnabled = enabled
        _uiState.update { it.copy(isSmsReadingEnabled = enabled) }
    }

    private fun setDefaultBudget(budget: Budget) {
        preferencesManager.defaultBudgetId = budget.id
        _uiState.update {
            it.copy(
                defaultBudget = budget,
                isDefaultBudgetSelectorVisible = false
            )
        }
    }

    private fun handleSmsPermission(granted: Boolean) {
        _uiState.update { it.copy(hasSmsPermission = granted) }
        toggleSmsReading(granted)
    }

    private fun showDefaultBudgetSelector() {
        _uiState.update { it.copy(isDefaultBudgetSelectorVisible = true) }
    }

    private fun hideDefaultBudgetSelector() {
        _uiState.update { it.copy(isDefaultBudgetSelectorVisible = false) }
    }

    private fun checkSmsPermission(context: Context): Boolean {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        return hasPermission
    }
}
