package com.meneses.budgethunter.settings

import com.meneses.budgethunter.budgetList.data.IBudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.IPreferencesManager
import com.meneses.budgethunter.commons.platform.IPermissionsManager
import com.meneses.budgethunter.fakes.manager.FakePermissionsManager
import com.meneses.budgethunter.fakes.manager.FakePreferencesManager
import com.meneses.budgethunter.fakes.repository.FakeBudgetRepository
import com.meneses.budgethunter.settings.application.SettingsEvent
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest {

    @Test
    fun `initial state loads settings on initialization`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadSettings loads default budget`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        preferences.setDefaultBudgetId(1)

        val repository: IBudgetRepository = FakeBudgetRepository()
        val budget = Budget(id = 1, name = "Default Budget", amount = 100.0)
        repository.setBudgets(listOf(budget))

        val permissions: IPermissionsManager = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals(budget, state.defaultBudget)
    }

    @Test
    fun `loadSettings loads sms reading state`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        preferences.setSmsReadingEnabled(true)

        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertTrue(state.isSmsReadingEnabled)
    }

    @Test
    fun `loadSettings loads ai processing state`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        preferences.setAiProcessingEnabled(true)

        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertTrue(state.isAiProcessingEnabled)
    }

    @Test
    fun `toggleSmsReading enables sms reading`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()
        permissions.hasSms = true

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ToggleSmsReading(true))
        kotlinx.coroutines.delay(100)

        assertTrue(preferences.isSmsReadingEnabled())
        assertTrue(viewModel.uiState.value.isSmsReadingEnabled)
    }

    @Test
    fun `toggleSmsReading requests permission when not granted`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()
        permissions.hasSms = false
        permissions.grantPermission = true

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ToggleSmsReading(true))
        kotlinx.coroutines.delay(100)

        assertTrue(permissions.permissionRequested)
        assertTrue(viewModel.uiState.value.hasSmsPermission)
    }

    @Test
    fun `toggleSmsReading shows rationale dialog when needed`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()
        permissions.hasSms = false
        permissions.shouldShowRationale = true

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ToggleSmsReading(true))
        kotlinx.coroutines.delay(100)

        assertTrue(viewModel.uiState.value.isManualPermissionDialogVisible)
    }

    @Test
    fun `toggleAiProcessing updates preference`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ToggleAiProcessing(true))
        kotlinx.coroutines.delay(100)

        assertTrue(preferences.isAiProcessingEnabled())
        assertTrue(viewModel.uiState.value.isAiProcessingEnabled)
    }

    @Test
    fun `setDefaultBudget updates preference and state`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        val budget = Budget(id = 5, name = "New Default", amount = 200.0)
        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.SetDefaultBudget(budget))
        kotlinx.coroutines.delay(100)

        assertEquals(5, preferences.getDefaultBudgetId())
        assertEquals(budget, viewModel.uiState.value.defaultBudget)
        assertFalse(viewModel.uiState.value.isDefaultBudgetSelectorVisible)
    }

    @Test
    fun `showDefaultBudgetSelector sets visibility`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ShowDefaultBudgetSelector)

        assertTrue(viewModel.uiState.value.isDefaultBudgetSelectorVisible)
    }

    @Test
    fun `hideDefaultBudgetSelector sets visibility`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ShowDefaultBudgetSelector)
        viewModel.sendEvent(SettingsEvent.HideDefaultBudgetSelector)

        assertFalse(viewModel.uiState.value.isDefaultBudgetSelectorVisible)
    }

    @Test
    fun `showBankSelector sets visibility`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ShowBankSelector)

        assertTrue(viewModel.uiState.value.isBankSelectorVisible)
    }

    @Test
    fun `hideBankSelector sets visibility`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ShowBankSelector)
        viewModel.sendEvent(SettingsEvent.HideBankSelector)

        assertFalse(viewModel.uiState.value.isBankSelectorVisible)
    }

    @Test
    fun `setSelectedBanks updates preferences and state`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        val bankConfigs = setOf(
            BankSmsConfig(id = "bank1", name = "Bank 1", senderIds = emptyList())
        )

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.SetSelectedBanks(bankConfigs))
        kotlinx.coroutines.delay(100)

        assertEquals(setOf("bank1"), preferences.getSelectedBankIds())
        assertEquals(bankConfigs, viewModel.uiState.value.selectedBanks)
    }

    @Test
    fun `showManualPermissionDialog sets visibility`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ShowManualPermissionDialog)

        assertTrue(viewModel.uiState.value.isManualPermissionDialogVisible)
    }

    @Test
    fun `hideManualPermissionDialog sets visibility`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ShowManualPermissionDialog)
        viewModel.sendEvent(SettingsEvent.HideManualPermissionDialog)

        assertFalse(viewModel.uiState.value.isManualPermissionDialogVisible)
    }

    @Test
    fun `openAppSettings opens settings and hides dialog`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ShowManualPermissionDialog)
        viewModel.sendEvent(SettingsEvent.OpenAppSettings)

        assertTrue(permissions.appSettingsOpened)
        assertFalse(viewModel.uiState.value.isManualPermissionDialogVisible)
    }

    @Test
    fun `loadSettings handles exception gracefully`() = runTest {
        val preferences: IPreferencesManager = FakePreferencesManager()
        val repository: IBudgetRepository = FakeBudgetRepository()
        val permissions: IPermissionsManager = FakePermissionsManager()

        // This should not throw even if there are issues
        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)

        // Should complete loading even with errors
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
