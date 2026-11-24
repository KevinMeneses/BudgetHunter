package com.meneses.budgethunter.budgetList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.budget_synced_successfully
import budgethunter.composeapp.generated.resources.sync_failed_retry_online
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.application.BudgetListIntent
import com.meneses.budgethunter.budgetList.application.BudgetListState
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.application.DuplicateBudgetUseCase
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.platform.NetworkMonitor
import com.meneses.budgethunter.sms.application.IsNotificationReaderStoppedUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetListViewModel(
    private val budgetRepository: BudgetRepository,
    private val duplicateBudgetUseCase: DuplicateBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager,
    private val isNotificationReaderStopped: IsNotificationReaderStoppedUseCase,
    private val networkMonitor: NetworkMonitor,
    private val logger: Logger
) : ViewModel() {
    private val tag = "BudgetListViewModel"
    val uiState get() = _uiState.asStateFlow()
    private val _uiState = MutableStateFlow(BudgetListState())

    private val _events = Channel<BudgetListEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        collectBudgetList()
        checkAuthState()
        syncWhenOnline()
        collectGoogleLinkNotice()
    }

    private fun collectGoogleLinkNotice() {
        viewModelScope.launch {
            preferencesManager.googleLinkNotice.collect { notice ->
                _uiState.update {
                    it.copy(
                        googleLinkNotice = notice,
                        isGoogleLinkNoticeOpen = it.isGoogleLinkNoticeOpen && notice != null
                    )
                }
            }
        }
    }

    private fun openGoogleLinkNotice() {
        _uiState.update { it.copy(isGoogleLinkNoticeOpen = it.googleLinkNotice != null) }
    }

    private fun dismissGoogleLinkNotice() {
        _uiState.update { it.copy(isGoogleLinkNoticeOpen = false, googleLinkNotice = null) }
        viewModelScope.launch { preferencesManager.setGoogleLinkNotice(null) }
    }

    private var autoSync: Job? = null

    /**
     * Syncs the budgets when the app opens with a connection (the first value is true) and each time
     * the connection comes back, so what was saved offline goes up without a pull-to-refresh. This
     * screen is the root of every launch with a session and stays in the back stack, so its scope is
     * "the app in use"; an app-wide trigger would also sync when the process starts in the
     * background (SMS receiver, notification listener, workers, the iOS Shortcut).
     */
    private fun syncWhenOnline() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online -> if (online) startAutoSync() }
        }
    }

    private fun startAutoSync() {
        // A trigger while one runs is dropped. Syncs from elsewhere (pull-to-refresh, sign in, a
        // budget saved) are serialized by BudgetSyncManager.
        if (autoSync?.isActive == true) return
        autoSync = viewModelScope.launch {
            // Silent: no message and no spinner, nobody asked for it
            try {
                // Read at each trigger: offline mode can be entered and the session can end meanwhile
                if (preferencesManager.isOfflineModeEnabled() || !authRepository.isAuthenticated()) return@launch
                budgetRepository.sync().onFailure { logger.warn(tag, "Automatic sync failed", it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn(tag, "Automatic sync failed", e)
            }
        }
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val isAuthenticated = authRepository.isAuthenticated()
            // While signed in, the last account that signed in is the current one
            val accountEmail = if (isAuthenticated) preferencesManager.getLastSignedInEmail() else null
            _uiState.update { it.copy(isAuthenticated = isAuthenticated, accountEmail = accountEmail) }
        }
    }

    private fun collectBudgetList() {
        viewModelScope.launch {
            budgetRepository.budgets.collect { budgetList ->
                val currentState = _uiState.value
                val filteredList = when {
                    currentState.filter != null -> budgetRepository.getAllFilteredBy(currentState.filter)
                    currentState.searchQuery.isNotBlank() -> budgetList.filter { budget ->
                        budget.name.contains(currentState.searchQuery, ignoreCase = true)
                    }
                    else -> budgetList
                }
                _uiState.update {
                    it.copy(budgetList = filteredList, isLoading = false)
                }
            }
        }
    }

    fun sendIntent(intent: BudgetListIntent) {
        when (intent) {
            is BudgetListIntent.CreateBudget -> createBudget(intent.budget)
            is BudgetListIntent.UpdateBudget -> updateBudget(intent.budget)
            is BudgetListIntent.DuplicateBudget -> duplicateBudget(intent.budget)
            is BudgetListIntent.DeleteBudget -> deleteBudget(intent.budgetId)
            is BudgetListIntent.OpenBudget -> openBudget(intent.budget)
            is BudgetListIntent.ToggleAddModal -> setAddModalVisibility(intent.isVisible)
            is BudgetListIntent.ToggleUpdateModal -> setUpdateModalVisibility(intent.budget)
            is BudgetListIntent.ToggleSearchMode -> setSearchMode(intent.isSearchMode)
            is BudgetListIntent.UpdateSearchQuery -> updateSearchQuery(intent.query)
            is BudgetListIntent.ClearFilter -> clearFilter()
            is BudgetListIntent.SignIn -> signIn()
            is BudgetListIntent.SyncBudgets -> syncBudgets()
            is BudgetListIntent.CheckNotificationReader -> checkNotificationReader()
            is BudgetListIntent.OpenGoogleLinkNotice -> openGoogleLinkNotice()
            is BudgetListIntent.DismissGoogleLinkNotice -> dismissGoogleLinkNotice()
        }
    }

    private var notificationReaderCheck: Job? = null

    private fun checkNotificationReader() {
        // A check under way reads everything again after its grace period
        if (notificationReaderCheck?.isActive == true) return
        notificationReaderCheck = viewModelScope.launch {
            val stopped = isNotificationReaderStopped()
            _uiState.update { it.copy(isNotificationReaderStopped = stopped) }
        }
    }

    private fun syncBudgets() = viewModelScope.launch {
        _uiState.update { it.copy(isSyncing = true) }
        try {
            budgetRepository.sync()
            _events.trySend(BudgetListEvent.ShowMessage(Res.string.budget_synced_successfully))
        } catch (e: Exception) {
            logger.warn(tag, "Sync failed", e)
            _events.trySend(BudgetListEvent.ShowMessage(Res.string.sync_failed_retry_online))
        } finally {
            // Small delay to ensure PullToRefreshBox can process the state change
            delay(100)
            _uiState.update { it.copy(isSyncing = false) }
        }
    }

    private fun duplicateBudget(budget: Budget) = viewModelScope.launch {
        duplicateBudgetUseCase.execute(budget)
    }

    private fun deleteBudget(budgetId: Long) = viewModelScope.launch {
        deleteBudgetUseCase.execute(budgetId)
    }

    private fun createBudget(budget: Budget) = viewModelScope.launch {
        _uiState.update { it.copy(isCreatingBudget = true) }
        try {
            val budgetSaved = budgetRepository.create(budget)
            openBudget(budgetSaved)
        } finally {
            _uiState.update { it.copy(isCreatingBudget = false) }
        }
    }

    private fun updateBudget(budget: Budget) = viewModelScope.launch {
        _uiState.update { it.copy(isUpdatingBudget = true) }
        try {
            // Mark as unsynced so it will be pushed to server
            val budgetToUpdate = budget.copy(
                isSynced = false,
                lastSyncedAt = null
            )
            budgetRepository.update(budgetToUpdate)
        } finally {
            _uiState.update { it.copy(isUpdatingBudget = false) }
        }
    }

    private fun openBudget(budget: Budget) =
        _events.trySend(BudgetListEvent.NavigateToBudget(budget))

    private fun clearFilter() {
        viewModelScope.launch {
            val budgetList = budgetRepository.getAllCached()
            _uiState.update { it.copy(budgetList = budgetList, filter = null) }
        }
    }

    private fun setAddModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(addModalVisibility = visible) }

    private fun setUpdateModalVisibility(budget: Budget?) =
        _uiState.update { it.copy(budgetToUpdate = budget) }

    private fun setSearchMode(isSearchMode: Boolean) {
        _uiState.update {
            it.copy(
                isSearchMode = isSearchMode,
                searchQuery = if (!isSearchMode) "" else it.searchQuery
            )
        }
        // If exiting search mode, restore the full budget list
        if (!isSearchMode) {
            viewModelScope.launch {
                val currentBudgets = budgetRepository.getAllCached()
                _uiState.update { currentState ->
                    val filteredList = if (currentState.filter != null) {
                        budgetRepository.getAllFilteredBy(currentState.filter)
                    } else {
                        currentBudgets
                    }
                    currentState.copy(budgetList = filteredList)
                }
            }
        }
    }

    private fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        // Trigger filtering by refreshing the budget list
        viewModelScope.launch {
            val currentBudgets = budgetRepository.getAllCached()
            _uiState.update { currentState ->
                val filteredList = when {
                    currentState.filter != null -> budgetRepository.getAllFilteredBy(currentState.filter)
                    query.isNotBlank() -> currentBudgets.filter { budget ->
                        budget.name.contains(query, ignoreCase = true)
                    }
                    else -> currentBudgets
                }
                currentState.copy(budgetList = filteredList)
            }
        }
    }

    private fun signIn() {
        _events.trySend(BudgetListEvent.NavigateToSignIn)
    }
}
