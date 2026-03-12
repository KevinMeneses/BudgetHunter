package com.meneses.budgethunter.budgetDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetDetail.data.BudgetDetailRepository
import com.meneses.budgethunter.budgetEntry.data.sync.RealTimeSyncManager
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.network.ApiError
import com.meneses.budgethunter.commons.data.network.toApiError
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
    private var hasTriggeredInitialSync = false

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
                } else {
                    realTimeSyncManager.stopListening()
                }
            }
        }
    }

    private suspend fun checkAndStartSSE(serverId: Long) {
        val collaboratorsResult = collaboratorRepository.getCollaborators(serverId)
        val hasCollaborators = (collaboratorsResult.getOrNull()?.size ?: 0) > 1

        if (hasCollaborators) {
            realTimeSyncManager.startListening(serverId)
        } else {
            realTimeSyncManager.stopListening()
        }
    }

    override fun onCleared() {
        super.onCleared()
        realTimeSyncManager.stopListening()
    }

    fun sendEvent(event: BudgetDetailEvent) {
        when (event) {
            is BudgetDetailEvent.SetBudget -> setBudget(event.budget)
            is BudgetDetailEvent.GetBudgetDetail -> getBudgetDetail()
            is BudgetDetailEvent.UpdateBudgetAmount -> updateBudgetAmount(event.amount)
            is BudgetDetailEvent.FilterEntries -> filterEntries(event.filter)
            is BudgetDetailEvent.ClearFilter -> clearFilter()
            is BudgetDetailEvent.DeleteBudget -> deleteBudget()
            is BudgetDetailEvent.DeleteSelectedEntries -> deleteSelectedEntries()
            is BudgetDetailEvent.ShowEntry -> showEntry(event.budgetItem)
            is BudgetDetailEvent.ToggleBudgetModal -> setBudgetModalVisibility(event.isVisible)
            is BudgetDetailEvent.ToggleDeleteBudgetModal -> setDeleteBudgetModalVisibility(event.isVisible)
            is BudgetDetailEvent.ToggleDeleteEntriesModal -> setDeleteEntriesModalVisibility(event.isVisible)
            is BudgetDetailEvent.ToggleFilterModal -> setFilterModalVisibility(event.isVisible)
            is BudgetDetailEvent.ToggleSelectionState -> toggleSelectionState(event.isActivated)
            is BudgetDetailEvent.ToggleAllEntriesSelection -> toggleAllEntriesSelection(event.isSelected)
            is BudgetDetailEvent.ToggleSelectEntry -> toggleEntrySelection(event)
            is BudgetDetailEvent.ClearNavigation -> clearNavigation()
            is BudgetDetailEvent.SortList -> orderList()
            is BudgetDetailEvent.SyncEntries -> {
                val budget = _uiState.value.budgetDetail.budget
                syncEntries(budgetId = budget.id, serverId = budget.serverId, showErrors = true)
            }

            is BudgetDetailEvent.ClearSyncError -> clearSyncError()
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

    private fun clearNavigation() =
        _uiState.update { it.copy(showEntry = null) }

    private fun deleteSelectedEntries() = viewModelScope.launch {
        val entriesToDeleteIds = _uiState.value.budgetDetail.entries
            .filter { it.isSelected }
            .map { it.id }

        budgetDetailRepository.deleteEntriesByIds(entriesToDeleteIds)
        toggleSelectionState(false)
    }

    private fun toggleEntrySelection(event: BudgetDetailEvent.ToggleSelectEntry) {
        _uiState.update { state ->
            val updatedEntry = state.budgetDetail
                .entries[event.index]
                .copy(isSelected = event.isSelected)

            val updatedList = state.budgetDetail
                .entries
                .toMutableList()
                .apply { set(index = event.index, element = updatedEntry) }

            state.copy(
                budgetDetail = state.budgetDetail
                    .copy(entries = updatedList)
            )
        }
    }

    private fun showEntry(budgetItem: BudgetEntry) =
        _uiState.update { it.copy(showEntry = budgetItem) }

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
        _uiState.update {
            budgetDetailRepository.deleteBudget(budgetId)
            it.copy(goBack = true)
        }
    }

    private fun setFilterModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(isFilterModalVisible = visible) }

    private fun setDeleteBudgetModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(isDeleteBudgetModalVisible = visible) }

    private fun setDeleteEntriesModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(isDeleteEntriesModalVisible = visible) }

    private fun setBudgetModalVisibility(visible: Boolean) =
        _uiState.update { it.copy(isBudgetModalVisible = visible) }

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
                    isLoading = if (showErrors) it.isLoading else true,
                    syncError = if (showErrors) null else it.syncError
                )
            }
            try {
                val result = budgetDetailRepository.syncEntries(budgetId, serverId)
                if (result.isFailure && showErrors) {
                    val error = result.exceptionOrNull()
                    val apiError = error?.toApiError() ?: ApiError.Unknown
                    _uiState.update { it.copy(syncError = apiError.messageResource) }
                }
            } catch (e: Exception) {
                if (showErrors) {
                    val apiError = e.toApiError()
                    _uiState.update { it.copy(syncError = apiError.messageResource) }
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

    private fun clearSyncError() {
        _uiState.update { it.copy(syncError = null) }
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val isAuthenticated = authRepository.isAuthenticated()
            _uiState.update { it.copy(isAuthenticated = isAuthenticated) }
        }
    }
}
