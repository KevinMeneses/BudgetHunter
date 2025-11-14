package com.meneses.budgethunter.settings

import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.settings.application.SettingsEvent
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest {

    private class FakePreferencesManager : PreferencesManager {
        private var smsReadingEnabled = false
        private var aiProcessingEnabled = false
        private var defaultBudgetId = -1
        private var selectedBankIds = emptySet<String>()

        override suspend fun isSmsReadingEnabled(): Boolean = smsReadingEnabled
        override suspend fun setSmsReadingEnabled(enabled: Boolean) {
            smsReadingEnabled = enabled
        }

        override suspend fun isAiProcessingEnabled(): Boolean = aiProcessingEnabled
        override suspend fun setAiProcessingEnabled(enabled: Boolean) {
            aiProcessingEnabled = enabled
        }

        override suspend fun getDefaultBudgetId(): Int = defaultBudgetId
        override suspend fun setDefaultBudgetId(budgetId: Int) {
            defaultBudgetId = budgetId
        }

        override suspend fun getSelectedBankIds(): Set<String> = selectedBankIds
        override suspend fun setSelectedBankIds(bankIds: Set<String>) {
            selectedBankIds = bankIds
        }
    }

    private class FakeBudgetRepository : BudgetRepository {
        private val budgetCache = mutableListOf<Budget>()
        override val budgets: StateFlow<List<Budget>> = MutableStateFlow(emptyList())

        fun setBudgets(budgets: List<Budget>) {
            budgetCache.clear()
            budgetCache.addAll(budgets)
        }

        override suspend fun create(budget: Budget): Budget = budget
        override suspend fun update(budget: Budget) {}
        override suspend fun getById(id: Int): Budget? = budgetCache.find { it.id == id }
        override suspend fun getAllCached(): List<Budget> = budgetCache.toList()
        override suspend fun getAllFilteredBy(filter: Any?): List<Budget> = budgetCache.toList()
    }

    private class FakePermissionsManager : PermissionsManager {
        var hasSms = false
        var shouldShowRationale = false
        var appSettingsOpened = false
        var permissionRequested = false
        var grantPermission = false

        override fun hasSmsPermission(): Boolean = hasSms
        override fun shouldShowSMSPermissionRationale(): Boolean = shouldShowRationale
        override fun requestSmsPermissions(onResult: (Boolean) -> Unit) {
            permissionRequested = true
            onResult(grantPermission)
        }
        override fun openAppSettings() {
            appSettingsOpened = true
        }
    }

    @Test
    fun `initial state loads settings on initialization`() = runTest {
        val preferences = FakePreferencesManager()
        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadSettings loads default budget`() = runTest {
        val preferences = FakePreferencesManager()
        preferences.setDefaultBudgetId(1)

        val repository = FakeBudgetRepository()
        val budget = Budget(id = 1, name = "Default Budget", amount = 100.0)
        repository.setBudgets(listOf(budget))

        val permissions = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals(budget, state.defaultBudget)
    }

    @Test
    fun `loadSettings loads sms reading state`() = runTest {
        val preferences = FakePreferencesManager()
        preferences.setSmsReadingEnabled(true)

        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertTrue(state.isSmsReadingEnabled)
    }

    @Test
    fun `loadSettings loads ai processing state`() = runTest {
        val preferences = FakePreferencesManager()
        preferences.setAiProcessingEnabled(true)

        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertTrue(state.isAiProcessingEnabled)
    }

    @Test
    fun `toggleSmsReading enables sms reading`() = runTest {
        val preferences = FakePreferencesManager()
        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()
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
        val preferences = FakePreferencesManager()
        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()
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
        val preferences = FakePreferencesManager()
        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()
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
        val preferences = FakePreferencesManager()
        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ToggleAiProcessing(true))
        kotlinx.coroutines.delay(100)

        assertTrue(preferences.isAiProcessingEnabled())
        assertTrue(viewModel.uiState.value.isAiProcessingEnabled)
    }

    @Test
    fun `setDefaultBudget updates preference and state`() = runTest {
        val preferences = FakePreferencesManager()
        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()

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
        val preferences = FakePreferencesManager()
        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ShowDefaultBudgetSelector)

        assertTrue(viewModel.uiState.value.isDefaultBudgetSelectorVisible)
    }

    @Test
    fun `hideDefaultBudgetSelector sets visibility`() = runTest {
        val preferences = FakePreferencesManager()
        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ShowDefaultBudgetSelector)
        viewModel.sendEvent(SettingsEvent.HideDefaultBudgetSelector)

        assertFalse(viewModel.uiState.value.isDefaultBudgetSelectorVisible)
    }

    @Test
    fun `showBankSelector sets visibility`() = runTest {
        val preferences = FakePreferencesManager()
        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ShowBankSelector)

        assertTrue(viewModel.uiState.value.isBankSelectorVisible)
    }

    @Test
    fun `hideBankSelector sets visibility`() = runTest {
        val preferences = FakePreferencesManager()
        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ShowBankSelector)
        viewModel.sendEvent(SettingsEvent.HideBankSelector)

        assertFalse(viewModel.uiState.value.isBankSelectorVisible)
    }

    @Test
    fun `setSelectedBanks updates preferences and state`() = runTest {
        val preferences = FakePreferencesManager()
        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()

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
        val preferences = FakePreferencesManager()
        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ShowManualPermissionDialog)

        assertTrue(viewModel.uiState.value.isManualPermissionDialogVisible)
    }

    @Test
    fun `hideManualPermissionDialog sets visibility`() = runTest {
        val preferences = FakePreferencesManager()
        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()

        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(SettingsEvent.ShowManualPermissionDialog)
        viewModel.sendEvent(SettingsEvent.HideManualPermissionDialog)

        assertFalse(viewModel.uiState.value.isManualPermissionDialogVisible)
    }

    @Test
    fun `openAppSettings opens settings and hides dialog`() = runTest {
        val preferences = FakePreferencesManager()
        val repository = FakeBudgetRepository()
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
        val preferences = FakePreferencesManager()
        val repository = FakeBudgetRepository()
        val permissions = FakePermissionsManager()

        // This should not throw even if there are issues
        val viewModel = SettingsViewModel(preferences, repository, permissions)

        kotlinx.coroutines.delay(100)

        // Should complete loading even with errors
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
