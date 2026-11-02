package com.meneses.budgethunter.budgetDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.entries_synced_successfully
import budgethunter.composeapp.generated.resources.sync_failed_background
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailIntent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetDetail.data.BudgetDetailRepository
import com.meneses.budgethunter.budgetEntry.data.sync.RealTimeSyncManager
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.network.ApiError
import com.meneses.budgethunter.commons.data.network.toApiError
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetDetailViewModel(
    private val budgetDetailRepository: BudgetDetailRepository,
    private val realTimeSyncManager: RealTimeSyncManager,
    private val authRepository: AuthRepository,
    private val collaboratorRepository: com.meneses.budgethunter.collaborator.data.CollaboratorRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetDetailState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<BudgetDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var hasTriggeredInitialSync = false

    private var syncJob: Job? = null

    /** Collects the budget detail; restarted rather than duplicated when the screen comes back. */
    private var detailJob: Job? = null

    private var realTimeSyncCheckJob: Job? = null

    init {
        checkAuthState()
        viewModelScope.launch {
            uiState.map { it.budgetDetail.budget.syncedServerId() }
                .distinctUntilChanged()
                .collect { serverId -> refreshRealTimeSync(serverId) }
        }
        realTimeSyncManager.collaboratorNotifications
            .onEach { collaboratorName ->
                _events.trySend(BudgetDetailEvent.ShowCollaboratorEntry(collaboratorName))
            }
            .launchIn(viewModelScope)

        uiState.map { it.budgetDetail.budget.id }
            .distinctUntilChanged()
            .combine(budgetDetailRepository.budgetsWithFailedSync) { budgetId, failedBudgetIds ->
                budgetId in failedBudgetIds
            }
            .distinctUntilChanged()
            .filter { hasFailedSync -> hasFailedSync }
            .onEach { _events.trySend(BudgetDetailEvent.ShowError(Res.string.sync_failed_background)) }
            .launchIn(viewModelScope)
    }

    /**
     * Listens to the budget in real time only while it is on the server and has collaborators.
     *
     * Runs when the budget's server id changes and every time the screen comes back (see
     * [resumeSync]): the ViewModel outlives the trips to other screens, so a collaborator added
     * or removed in the collaborators screen would otherwise not start or stop the stream.
     *
     * [realTimeSyncManager] is an application-scoped singleton shared by every instance of this
     * screen, so it is stopped with this ViewModel as owner: an instance that never started the
     * stream, or that was replaced by a newer one, leaves it alone.
     */
    private fun refreshRealTimeSync(serverId: Long?) {
        realTimeSyncCheckJob?.cancel()
        realTimeSyncCheckJob = viewModelScope.launch {
            if (serverId == null) {
                realTimeSyncManager.stopListening(owner = this@BudgetDetailViewModel)
                return@launch
            }

            val collaboratorsResult = collaboratorRepository.getCollaborators(serverId)
            // A failed lookup says nothing about the collaborators: keep the stream as it is, but
            // take it over, or a previous instance being cleared would close it under us
            val collaborators = collaboratorsResult.getOrNull() ?: run {
                realTimeSyncManager.claimIfListening(serverId, owner = this@BudgetDetailViewModel)
                return@launch
            }

            if (collaborators.size > 1) {
                realTimeSyncManager.startListening(serverId, owner = this@BudgetDetailViewModel)
            } else {
                realTimeSyncManager.stopListening(owner = this@BudgetDetailViewModel)
            }
        }
    }

    private fun Budget.syncedServerId(): Long? = serverId?.takeIf { isSynced }

    override fun onCleared() {
        super.onCleared()
        realTimeSyncManager.stopListening(owner = this)
    }

    fun sendIntent(intent: BudgetDetailIntent) {
        when (intent) {
            is BudgetDetailIntent.SetBudget -> setBudget(intent.budget)
            is BudgetDetailIntent.GetBudgetDetail -> getBudgetDetail()
            is BudgetDetailIntent.UpdateBudgetAmount -> updateBudgetAmount(intent.amount)
            is BudgetDetailIntent.FilterEntries -> filterEntries(intent.filter)
            is BudgetDetailIntent.ClearFilter -> clearFilter()
            is BudgetDetailIntent.DeleteBudget -> deleteBudget()
            is BudgetDetailIntent.DeleteSelectedEntries -> deleteSelectedEntries()
            is BudgetDetailIntent.ShowEntry -> showEntry(intent.budgetItem)
            is BudgetDetailIntent.ToggleBudgetModal -> setBudgetModalVisibility(intent.isVisible)
            is BudgetDetailIntent.ToggleDeleteBudgetModal -> setDeleteBudgetModalVisibility(intent.isVisible)
            is BudgetDetailIntent.ToggleDeleteEntriesModal -> setDeleteEntriesModalVisibility(intent.isVisible)
            is BudgetDetailIntent.ToggleFilterModal -> setFilterModalVisibility(intent.isVisible)
            is BudgetDetailIntent.ToggleSelectionState -> toggleSelectionState(intent.isActivated)
            is BudgetDetailIntent.ToggleAllEntriesSelection -> toggleAllEntriesSelection(intent.isSelected)
            is BudgetDetailIntent.ToggleSelectEntry -> toggleEntrySelection(intent)
            is BudgetDetailIntent.SortList -> orderList()
            is BudgetDetailIntent.SyncEntries -> {
                val budget = _uiState.value.budgetDetail.budget
                syncEntries(budgetId = budget.id, serverId = budget.serverId, showErrors = true)
            }
            is BudgetDetailIntent.ResumeSync -> resumeSync()
        }
    }

    private fun orderList() = _uiState.update { currentState ->
        val newOrder: BudgetDetailState.ListOrder
        val orderedEntries: List<BudgetEntry>

        when (currentState.listOrder) {
            BudgetDetailState.ListOrder.DEFAULT -> {
                newOrder = BudgetDetailState.ListOrder.AMOUNT_ASCENDANT
                orderedEntries = currentState.budgetDetail.entries.sortedBy {
                    val isPositive = it.type == BudgetEntry.Type.INCOME
                    if (isPositive) it.amount
                    else "-" + it.amount
                }
            }

            BudgetDetailState.ListOrder.AMOUNT_ASCENDANT -> {
                newOrder = BudgetDetailState.ListOrder.AMOUNT_DESCENDANT
                orderedEntries = currentState.budgetDetail.entries.sortedByDescending {
                    val isPositive = it.type == BudgetEntry.Type.INCOME
                    if (isPositive) it.amount
                    else "-" + it.amount
                }
            }

            BudgetDetailState.ListOrder.AMOUNT_DESCENDANT -> {
                newOrder = BudgetDetailState.ListOrder.DEFAULT
                orderedEntries = currentState.budgetDetail.entries.sortedWith(
                    compareByDescending<BudgetEntry> { it.date }.thenByDescending { it.id }
                )
            }
        }

        currentState.copy(
            budgetDetail = currentState.budgetDetail.copy(entries = orderedEntries),
            listOrder = newOrder
        )
    }

    private fun setBudget(budget: Budget) =
        _uiState.update {
            if (it.budgetDetail.budget.id != budget.id) {
                hasTriggeredInitialSync = false
            }
            it.copy(
                budgetDetail = it.budgetDetail
                    .copy(budget = budget)
            )
        }

    private fun getBudgetDetail() {
        val budgetId = _uiState.value.budgetDetail.budget.id

        // The screen asks again each time it comes back from another screen, and the ViewModel
        // keeps living meanwhile: restart the collection instead of adding a second one.
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            // Trigger auto-sync ONCE before starting to collect, not on every emission
            if (!hasTriggeredInitialSync) {
                val currentServerId = _uiState.value.budgetDetail.budget.serverId
                if (currentServerId != null) {
                    hasTriggeredInitialSync = true
                    viewModelScope.launch {
                        syncEntries(
                            budgetId = budgetId,
                            serverId = currentServerId,
                            showErrors = false
                        )
                    }
                }
            }

            budgetDetailRepository
                .getBudgetDetailById(budgetId)
                .collect { detail ->
                    val currentFilter = _uiState.value.filter
                    val updatedDetail = if (currentFilter == null) detail
                    else budgetDetailRepository.getAllFilteredBy(currentFilter)
                    _uiState.update {
                        it.copy(budgetDetail = updatedDetail, isLoading = false)
                    }
                }
        }
    }

    private fun deleteSelectedEntries() = viewModelScope.launch {
        val entriesToDeleteIds = _uiState.value.budgetDetail.entries
            .filter { it.isSelected }
            .map { it.id }

        budgetDetailRepository.deleteEntriesByIds(entriesToDeleteIds)
        toggleSelectionState(false)
    }

    private fun toggleEntrySelection(intent: BudgetDetailIntent.ToggleSelectEntry) {
        _uiState.update { state ->
            val updatedEntry = state.budgetDetail
                .entries[intent.index]
                .copy(isSelected = intent.isSelected)

            val updatedList = state.budgetDetail
                .entries
                .toMutableList()
                .apply { set(index = intent.index, element = updatedEntry) }

            state.copy(
                budgetDetail = state.budgetDetail
                    .copy(entries = updatedList)
            )
        }
    }

    private fun showEntry(budgetItem: BudgetEntry) =
        _events.trySend(BudgetDetailEvent.ShowEntry(budgetItem))

    private fun updateBudgetAmount(amount: Double) = viewModelScope.launch {
        budgetDetailRepository.updateBudgetAmount(amount)
    }

    private fun filterEntries(filter: BudgetEntryFilter) = viewModelScope.launch {
        val filteredDetail = budgetDetailRepository.getAllFilteredBy(filter)
        _uiState.update { it.copy(budgetDetail = filteredDetail, filter = filter) }
    }

    private fun clearFilter() = viewModelScope.launch {
        val detail = budgetDetailRepository.getCachedDetail()
        _uiState.update { it.copy(budgetDetail = detail, filter = null) }
    }

    private fun deleteBudget() = viewModelScope.launch {
        val budgetId = _uiState.value.budgetDetail.budget.id
        budgetDetailRepository.deleteBudget(budgetId)
        _events.trySend(BudgetDetailEvent.NavigateBack)
    }

    private fun setFilterModalVisibility(visible: Boolean) =
        _uiState.update {
            it.copy(modal = if (visible) BudgetDetailState.ModalState.Filter else BudgetDetailState.ModalState.None)
        }

    private fun setDeleteBudgetModalVisibility(visible: Boolean) =
        _uiState.update {
            it.copy(modal = if (visible) BudgetDetailState.ModalState.DeleteBudget else BudgetDetailState.ModalState.None)
        }

    private fun setDeleteEntriesModalVisibility(visible: Boolean) =
        _uiState.update {
            it.copy(modal = if (visible) BudgetDetailState.ModalState.DeleteEntries else BudgetDetailState.ModalState.None)
        }

    private fun setBudgetModalVisibility(visible: Boolean) =
        _uiState.update {
            it.copy(modal = if (visible) BudgetDetailState.ModalState.Budget else BudgetDetailState.ModalState.None)
        }

    private fun toggleSelectionState(isActivated: Boolean) =
        _uiState.update { it.copy(isSelectionActive = isActivated) }
            .also { if (!isActivated) toggleAllEntriesSelection(false) }

    private fun toggleAllEntriesSelection(isSelected: Boolean) =
        _uiState.update { state ->
            val updatedEntries = state.budgetDetail.entries
                .map { it.copy(isSelected = isSelected) }
            state.copy(
                budgetDetail = state.budgetDetail
                    .copy(entries = updatedEntries)
            )
        }

    /**
     * Retries pushing entries that were saved while the app was in background (e.g. from an SMS)
     * and whose own push failed. Runs silently: [BudgetDetailState.isLoading] would replace the
     * whole screen with a loading indicator every time the app comes back to the foreground.
     *
     * Also runs when returning from another screen of the app, so it re-checks the real-time
     * stream too (collaborators may have changed there).
     */
    private fun resumeSync() {
        refreshRealTimeSync(_uiState.value.budgetDetail.budget.syncedServerId())
        val budget = _uiState.value.budgetDetail.budget
        val serverId = budget.serverId ?: return
        if (syncJob?.isActive == true) return
        syncEntries(budgetId = budget.id, serverId = serverId, showErrors = false, showProgress = false)
    }

    private fun syncEntries(
        budgetId: Int? = null,
        serverId: Long? = null,
        showErrors: Boolean,
        showProgress: Boolean = true
    ) = viewModelScope.launch {
        hasTriggeredInitialSync = true
        if (showProgress) {
            _uiState.update {
                it.copy(
                    isSyncingEntries = showErrors,
                    isLoading = if (showErrors) it.isLoading else true
                )
            }
        }
        try {
            val result = budgetDetailRepository.syncEntries(budgetId, serverId)
            if (result.isSuccess && showErrors) {
                _events.trySend(BudgetDetailEvent.ShowSuccess(Res.string.entries_synced_successfully))
            } else if (result.isFailure && showErrors) {
                val error = result.exceptionOrNull()
                val apiError = error?.toApiError() ?: ApiError.Unknown
                _events.trySend(BudgetDetailEvent.ShowError(apiError.messageResource))
            }
        } catch (e: Exception) {
            if (showErrors) {
                val apiError = e.toApiError()
                _events.trySend(BudgetDetailEvent.ShowError(apiError.messageResource))
            }
        } finally {
            if (showProgress) {
                delay(100)
                _uiState.update {
                    it.copy(
                        isSyncingEntries = false,
                        isLoading = if (showErrors) it.isLoading else false
                    )
                }
            }
        }
    }.also { syncJob = it }

    private fun checkAuthState() {
        viewModelScope.launch {
            val isAuthenticated = authRepository.isAuthenticated()
            _uiState.update { it.copy(isAuthenticated = isAuthenticated) }
        }
    }
}
