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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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

    /**
     * Whether this ViewModel is the one that asked [realTimeSyncManager] to listen.
     *
     * The manager is an application-scoped singleton shared by every instance of this screen, so
     * a ViewModel that never started it must not stop it: the initial [BudgetDetailState] has no
     * `serverId` yet, and tearing the stream down on that first emission would kill a healthy
     * connection owned by a previous instance and force a reconnect.
     */
    private var ownsRealTimeSync = false

    init {
        checkAuthState()
        viewModelScope.launch {
            uiState.map { state ->
                val budget = state.budgetDetail.budget
                if (budget.isSynced && budget.serverId != null) budget.serverId else null
            }.distinctUntilChanged().collect { serverId ->
                if (serverId != null) {
                    // Only start SSE if the budget has collaborators
                    checkAndStartSSE(serverId)
                } else if (ownsRealTimeSync) {
                    stopRealTimeSync()
                }
            }
        }
        realTimeSyncManager.collaboratorNotifications
            .onEach { collaboratorName ->
                _events.trySend(BudgetDetailEvent.ShowCollaboratorEntry(collaboratorName))
            }
            .launchIn(viewModelScope)

        budgetDetailRepository.backgroundSyncErrors
            .onEach { _events.trySend(BudgetDetailEvent.ShowError(Res.string.sync_failed_background)) }
            .launchIn(viewModelScope)
    }

    private suspend fun checkAndStartSSE(serverId: Long) {
        val collaboratorsResult = collaboratorRepository.getCollaborators(serverId)
        val hasCollaborators = (collaboratorsResult.getOrNull()?.size ?: 0) > 1

        if (hasCollaborators) {
            realTimeSyncManager.startListening(serverId)
            ownsRealTimeSync = true
        } else if (ownsRealTimeSync) {
            stopRealTimeSync()
        }
    }

    private fun stopRealTimeSync() {
        realTimeSyncManager.stopListening()
        ownsRealTimeSync = false
    }

    override fun onCleared() {
        super.onCleared()
        if (ownsRealTimeSync) stopRealTimeSync()
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
                orderedEntries = currentState.budgetDetail.entries.sortedByDescending { it.id }
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

        viewModelScope.launch {
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

    private fun syncEntries(budgetId: Int? = null, serverId: Long? = null, showErrors: Boolean) =
        viewModelScope.launch {
            hasTriggeredInitialSync = true
            _uiState.update {
                it.copy(
                    isSyncingEntries = showErrors,
                    isLoading = if (showErrors) it.isLoading else true
                )
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
                delay(100)
                _uiState.update {
                    it.copy(
                        isSyncingEntries = false,
                        isLoading = if (showErrors) it.isLoading else false
                    )
                }
            }
        }

    private fun checkAuthState() {
        viewModelScope.launch {
            val isAuthenticated = authRepository.isAuthenticated()
            _uiState.update { it.copy(isAuthenticated = isAuthenticated) }
        }
    }
}
