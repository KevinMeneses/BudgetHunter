package com.meneses.budgethunter.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.delete_account_failed
import budgethunter.composeapp.generated.resources.error_current_password_incorrect
import budgethunter.composeapp.generated.resources.error_password_too_short
import budgethunter.composeapp.generated.resources.sign_out_failed
import com.meneses.budgethunter.auth.application.DeleteAccountUseCase
import com.meneses.budgethunter.auth.application.SignOutUseCase
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.settings.application.SettingsIntent
import com.meneses.budgethunter.settings.application.SettingsState
import com.meneses.budgethunter.settings.application.SettingsState.SubscriptionNotice
import com.meneses.budgethunter.settings.application.SyncUserPreferencesUseCase
import com.meneses.budgethunter.sms.application.IsNotificationReaderStoppedUseCase
import com.meneses.budgethunter.sms.domain.BankChannels
import com.meneses.budgethunter.sms.domain.BankSmsConfig
import com.meneses.budgethunter.sms.domain.ReadingChannel
import com.meneses.budgethunter.sms.domain.SupportedBanks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val budgetRepository: BudgetRepository,
    private val permissionsManager: PermissionsManager,
    private val authRepository: AuthRepository,
    private val syncUserPreferences: SyncUserPreferencesUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val isNotificationReaderStopped: IsNotificationReaderStoppedUseCase,
    // Pushes outlive the screen: leaving Settings right after a toggle must not drop the save.
    private val applicationScope: CoroutineScope
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())

    private var notificationReaderCheck: Job? = null

    // The dialog is shown once per visit; the warning row stays while the reader is stopped
    private var notificationReaderStoppedDialogShown = false
    val uiState = _uiState.asStateFlow()

    init {
        loadSettings()
        loadAccount()
    }

    fun sendIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.AddBank -> addBank(intent.bank)
            is SettingsIntent.RemoveBank -> removeBank(intent.bank)
            is SettingsIntent.SetBankChannel -> setBankChannel(intent.bank, intent.channel, intent.enabled)
            is SettingsIntent.RequestSmsPermission -> requestSmsPermission()
            is SettingsIntent.OpenNotificationAccessSettings -> openNotificationAccessSettings()
            is SettingsIntent.HideNotificationAccessDialog -> hideNotificationAccessDialog()
            is SettingsIntent.OpenAppNotificationSettings -> openAppNotificationSettings()
            is SettingsIntent.HideAppNotificationsDialog -> hideAppNotificationsDialog()
            is SettingsIntent.OpenAppInfoToRestartReader -> openAppInfoToRestartReader()
            is SettingsIntent.HideNotificationReaderStoppedDialog -> hideNotificationReaderStoppedDialog()
            is SettingsIntent.RefreshPermissions -> refreshPermissions()
            is SettingsIntent.SetDefaultBudget -> setDefaultBudget(intent.budget)
            is SettingsIntent.ShowDefaultBudgetSelector -> showDefaultBudgetSelector()
            is SettingsIntent.HideDefaultBudgetSelector -> hideDefaultBudgetSelector()
            is SettingsIntent.ShowBankSelector -> showBankSelector()
            is SettingsIntent.HideBankSelector -> hideBankSelector()
            is SettingsIntent.ToggleAiProcessing -> toggleAiProcessing(intent.enabled)
            is SettingsIntent.ShowManualPermissionDialog -> showManualPermissionDialog()
            is SettingsIntent.HideManualPermissionDialog -> hideManualPermissionDialog()
            is SettingsIntent.OpenAppSettings -> openAppSettings()
            is SettingsIntent.ShowPasswordDialog -> showPasswordDialog()
            is SettingsIntent.HidePasswordDialog -> hidePasswordDialog()
            is SettingsIntent.SavePassword -> savePassword(intent.currentPassword, intent.newPassword)
            is SettingsIntent.ToggleSignOutConfirmation -> toggleSignOutConfirmation(intent.show)
            is SettingsIntent.SignOut -> signOut()
            is SettingsIntent.DismissSignOutError -> _uiState.update { it.copy(signOutError = null) }
            is SettingsIntent.ToggleDeleteAccountConfirmation -> toggleDeleteAccountConfirmation(intent.show)
            is SettingsIntent.DeleteAccount -> deleteAccount()
            is SettingsIntent.DismissDeleteAccountError -> _uiState.update { it.copy(deleteAccountError = null) }
        }
    }

    // A cancel tapped while the check is still running must not see the dialog reopen afterwards
    private var signOutConfirmationJob: Job? = null

    private fun toggleSignOutConfirmation(show: Boolean) {
        signOutConfirmationJob?.cancel()
        if (!show) {
            _uiState.update { it.copy(isSignOutConfirmationVisible = false) }
            return
        }
        signOutConfirmationJob = viewModelScope.launch {
            // Signing out deletes the device's data, so say so when some of it never reached the
            // server. Unable to tell, warn anyway: a needless warning costs less than lost work.
            val hasUnsyncedChanges = try {
                signOutUseCase.hasUnsyncedChanges()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                true
            }
            _uiState.update {
                it.copy(isSignOutConfirmationVisible = true, signOutLosesUnsyncedChanges = hasUnsyncedChanges)
            }
        }
    }

    // Outlives the screen, like the preference pushes: leaving Settings half way must not stop
    // the wipe of the local data and the session with only part of it done. There is nothing to
    // send on success: navigation follows AuthSession, so it happens even if this screen is gone.
    private fun signOut() = applicationScope.launch {
        _uiState.update {
            it.copy(isSignOutConfirmationVisible = false, isSigningOut = true, signOutError = null)
        }
        try {
            signOutUseCase.execute()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // The session could not be ended and nothing was deleted: the user can try again
            _uiState.update { it.copy(signOutError = Res.string.sign_out_failed) }
        } finally {
            _uiState.update { it.copy(isSigningOut = false) }
        }
    }

    // Like the sign out one: a cancel tapped while the plan is asked for must not reopen the dialog
    private var deleteAccountConfirmationJob: Job? = null
    private var deleteAccountJob: Job? = null

    private fun toggleDeleteAccountConfirmation(show: Boolean) {
        // Nothing to confirm while the account is already being deleted
        if (show && deleteAccountJob?.isActive == true) return
        deleteAccountConfirmationJob?.cancel()
        if (!show) {
            _uiState.update { it.copy(isDeleteAccountConfirmationVisible = false, isCheckingPlanForDeletion = false) }
            return
        }
        _uiState.update { it.copy(isCheckingPlanForDeletion = true, deleteAccountError = null) }
        deleteAccountConfirmationJob = viewModelScope.launch {
            // Deleting the account does not cancel a store subscription. Unable to tell whether
            // there is one, say it might be: a needless line costs less than months of charges.
            val notice = when (deleteAccountUseCase.isPremium()) {
                true -> SubscriptionNotice.PREMIUM
                false -> SubscriptionNotice.NONE
                null -> SubscriptionNotice.UNKNOWN
            }
            _uiState.update {
                it.copy(
                    isCheckingPlanForDeletion = false,
                    isDeleteAccountConfirmationVisible = true,
                    deleteAccountSubscriptionNotice = notice
                )
            }
        }
    }

    // Outlives the screen like the sign out: once the server deleted the account, leaving Settings
    // must not stop the device from being cleared. Navigation follows AuthSession on success and
    // on a refused session, so only a failure has something to show here.
    private fun deleteAccount() {
        // One request at a time: a second tap while the first is under way is ignored
        if (deleteAccountJob?.isActive == true) return
        _uiState.update {
            it.copy(isDeleteAccountConfirmationVisible = false, isDeletingAccount = true, deleteAccountError = null)
        }
        deleteAccountJob = applicationScope.launch {
            try {
                if (deleteAccountUseCase.execute() == DeleteAccountUseCase.Result.FAILED) {
                    _uiState.update { it.copy(deleteAccountError = Res.string.delete_account_failed) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(deleteAccountError = Res.string.delete_account_failed) }
            } finally {
                _uiState.update { it.copy(isDeletingAccount = false) }
            }
        }
    }

    private fun addBank(bank: BankSmsConfig) {
        val channels = BankChannels.default(bank)
        saveReading { state ->
            state.copy(
                selectedBanks = state.selectedBanks + bank,
                bankChannels = state.bankChannels + (bank.id to channels),
                isBankSelectorVisible = false
            )
        }
        askForPermissions(channels)
    }

    private fun removeBank(bank: BankSmsConfig) {
        saveReading { state ->
            state.copy(selectedBanks = state.selectedBanks - bank, bankChannels = state.bankChannels - bank.id)
        }
    }

    private fun setBankChannel(bank: BankSmsConfig, channel: ReadingChannel, enabled: Boolean) {
        saveReading { state ->
            val current = state.channelsOf(bank)
            val channels = if (enabled) current + channel else current - channel
            state.copy(bankChannels = state.bankChannels + (bank.id to channels))
        }
        if (enabled) askForPermissions(setOf(channel))
    }

    /**
     * Applies a change to the banks read and their channels, and stores it. The selection is kept on
     * the account; the channels only on the device. The SMS switch the account still keeps follows
     * the channels.
     */
    private fun saveReading(change: (SettingsState) -> SettingsState) {
        val state = _uiState.updateAndGet(change)
        val selectedBankIds = state.selectedBanks.map { it.id }.toSet()
        val bankChannels = state.bankChannels.filterKeys { it in selectedBankIds }
        viewModelScope.launch {
            preferencesManager.setSelectedBankIds(selectedBankIds)
            preferencesManager.setBankChannels(bankChannels)
            preferencesManager.setSmsReadingEnabled(ReadingChannel.SMS in state.channelsInUse)
            pushPreferences()
            checkNotificationReader()
        }
    }

    /** Each channel needs its own permission: SMS a runtime one, apps and emails the notification access. */
    private fun askForPermissions(channels: Set<ReadingChannel>) {
        if (ReadingChannel.SMS in channels && !permissionsManager.hasSmsPermission()) requestSmsPermission()
        if (ReadingChannel.APP in channels || ReadingChannel.EMAIL in channels) askForNotificationReader()
    }

    private fun pushPreferences() {
        applicationScope.launch { syncUserPreferences.push() }
    }

    private fun showBankSelector() {
        _uiState.update { it.copy(isBankSelectorVisible = true) }
    }

    private fun hideBankSelector() {
        _uiState.update { it.copy(isBankSelectorVisible = false) }
    }

    /**
     * Shows what is on the device right away and refreshes from the account afterwards, so the
     * screen never waits on the network. The refresh only fills in what the account changed.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val preferences = readPreferences()
                _uiState.update {
                    it.copy(
                        defaultBudget = preferences.defaultBudget,
                        allBudgets = budgetRepository.getAllCached(),
                        hasSmsPermission = permissionsManager.hasSmsPermission(),
                        hasNotificationListenerAccess = permissionsManager.hasNotificationListenerAccess(),
                        areAppNotificationsEnabled = permissionsManager.areAppNotificationsEnabled(),
                        availableBanks = SupportedBanks.SELECTABLE_BANKS,
                        selectedBanks = preferences.selectedBanks,
                        bankChannels = preferences.bankChannels,
                        isAiProcessingEnabled = preferences.isAiProcessingEnabled,
                        isLoading = false
                    )
                }
                checkNotificationReader()
            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }

            refreshFromAccount()
        }
    }

    private suspend fun refreshFromAccount() {
        try {
            syncUserPreferences.pull()
            val preferences = readPreferences()
            _uiState.update {
                it.copy(
                    defaultBudget = preferences.defaultBudget,
                    allBudgets = budgetRepository.getAllCached(),
                    // The account may bring banks this device never read: they start from their defaults
                    selectedBanks = preferences.selectedBanks,
                    bankChannels = preferences.bankChannels,
                    isAiProcessingEnabled = preferences.isAiProcessingEnabled
                )
            }
        } catch (_: Exception) {
            // Keep showing the device's values; the pull logs its own failures.
        }
    }

    private suspend fun readPreferences(): StoredPreferences {
        val defaultBudgetId = preferencesManager.getDefaultBudgetId()
        return StoredPreferences(
            isAiProcessingEnabled = preferencesManager.isAiProcessingEnabled(),
            defaultBudget = if (defaultBudgetId != -1) budgetRepository.getById(defaultBudgetId) else null,
            selectedBanks = preferencesManager.getSelectedBankIds()
                .mapNotNull { SupportedBanks.getSelectableBankById(it) }
                .toSet(),
            bankChannels = preferencesManager.getBankChannels()
        )
    }

    private class StoredPreferences(
        val isAiProcessingEnabled: Boolean,
        val defaultBudget: Budget?,
        val selectedBanks: Set<BankSmsConfig>,
        val bankChannels: Map<String, Set<ReadingChannel>>
    )

    private fun requestSmsPermission() {
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

    /**
     * Notification access is not a runtime permission: it is granted in a system screen, after a
     * warning of its own. The dialog says first what is read and why, then leads there.
     */
    private fun askForNotificationReader() {
        val hasAccess = permissionsManager.hasNotificationListenerAccess()
        val appNotificationsEnabled = permissionsManager.areAppNotificationsEnabled()
        _uiState.update {
            it.copy(
                hasNotificationListenerAccess = hasAccess,
                areAppNotificationsEnabled = appNotificationsEnabled,
                isNotificationAccessDialogVisible = !hasAccess,
                // One step at a time: the access first, the app's own notifications once it is granted
                isAppNotificationsDialogVisible = hasAccess && !appNotificationsEnabled
            )
        }
        checkNotificationReader()
    }

    /** The reader can stop with everything granted; then the user is asked for a force stop. */
    private fun checkNotificationReader() {
        // A check under way reads everything again after its grace period: restarting it on every
        // resume would only push the warning further away
        if (notificationReaderCheck?.isActive == true) return
        notificationReaderCheck = viewModelScope.launch {
            val stopped = isNotificationReaderStopped()
            val showDialog = stopped && !notificationReaderStoppedDialogShown
            if (showDialog) notificationReaderStoppedDialogShown = true
            _uiState.update {
                it.copy(
                    isNotificationReaderStopped = stopped,
                    isNotificationReaderStoppedDialogVisible = showDialog || (stopped && it.isNotificationReaderStoppedDialogVisible)
                )
            }
        }
    }

    private fun openAppInfoToRestartReader() {
        hideNotificationReaderStoppedDialog()
        permissionsManager.openAppSettings()
    }

    private fun hideNotificationReaderStoppedDialog() {
        _uiState.update { it.copy(isNotificationReaderStoppedDialogVisible = false) }
    }

    private fun openAppNotificationSettings() {
        hideAppNotificationsDialog()
        permissionsManager.openAppNotificationSettings()
    }

    private fun hideAppNotificationsDialog() {
        _uiState.update { it.copy(isAppNotificationsDialogVisible = false) }
    }

    private fun openNotificationAccessSettings() {
        hideNotificationAccessDialog()
        permissionsManager.openNotificationListenerSettings()
    }

    private fun hideNotificationAccessDialog() {
        _uiState.update { it.copy(isNotificationAccessDialogVisible = false) }
    }

    private fun refreshPermissions() {
        _uiState.update {
            it.copy(
                hasSmsPermission = permissionsManager.hasSmsPermission(),
                hasNotificationListenerAccess = permissionsManager.hasNotificationListenerAccess(),
                areAppNotificationsEnabled = permissionsManager.areAppNotificationsEnabled()
            )
        }
        checkNotificationReader()
    }

    private fun toggleAiProcessing(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setAiProcessingEnabled(enabled)
        _uiState.update { it.copy(isAiProcessingEnabled = enabled) }
        pushPreferences()
    }

    private fun setDefaultBudget(budget: Budget) = viewModelScope.launch {
        preferencesManager.setDefaultBudgetId(budget.id)
        _uiState.update {
            it.copy(
                defaultBudget = budget,
                isDefaultBudgetSelectorVisible = false
            )
        }
        pushPreferences()
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
                _uiState.update {
                    it.copy(isSignedIn = true, hasPassword = user.hasPassword, email = user.email)
                }
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
