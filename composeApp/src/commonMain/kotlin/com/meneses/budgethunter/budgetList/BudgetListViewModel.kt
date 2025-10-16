package com.meneses.budgethunter.budgetList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.application.BudgetListState
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.application.DuplicateBudgetUseCase
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetListViewModel(
    private val budgetRepository: BudgetRepository,
    private val duplicateBudgetUseCase: DuplicateBudgetUseCase,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    val uiState get() = _uiState.asStateFlow()
    private val _uiState = MutableStateFlow(BudgetListState())

    init {
        collectBudgetList()
        checkAuthState()
    }

    private fun checkAuthState() {
        viewModelScope.launch {
            val isAuthenticated = authRepository.isAuthenticated()
            _uiState.update { it.copy(isAuthenticated = isAuthenticated) }
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

    fun sendEvent(event: BudgetListEvent) {
        when (event) {
            is BudgetListEvent.CreateBudget -> createBudget(event.budget)
            is BudgetListEvent.UpdateBudget -> updateBudget(event.budget)
            is BudgetListEvent.DuplicateBudget -> duplicateBudget(event.budget)
            is BudgetListEvent.DeleteBudget -> deleteBudget(event.budgetId)
            is BudgetListEvent.OpenBudget -> openBudget(event.budget)
            is BudgetListEvent.ToggleAddModal -> setAddModalVisibility(event.isVisible)
            is BudgetListEvent.ToggleUpdateModal -> setUpdateModalVisibility(event.budget)
            is BudgetListEvent.ToggleSearchMode -> setSearchMode(event.isSearchMode)
            is BudgetListEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is BudgetListEvent.ClearFilter -> clearFilter()
            is BudgetListEvent.ClearNavigation -> clearNavigation()
            is BudgetListEvent.SignOut -> signOut()
            is BudgetListEvent.SignIn -> signIn()
            is BudgetListEvent.ClearSignInNavigation -> clearSignInNavigation()
            is BudgetListEvent.SyncBudgets -> syncBudgets()
        }
    }

    private fun syncBudgets() = viewModelScope.launch {
        _uiState.update { it.copy(isSyncing = true) }
        try {
            budgetRepository.sync()
        } catch (e: Exception) {
            // Log sync errors silently - user is likely not authenticated
            println("BudgetListViewModel: Sync failed - ${e.message}")
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
        val budgetSaved = budgetRepository.create(budget)
        openBudget(budgetSaved)
    }

    private fun updateBudget(budget: Budget) = viewModelScope.launch {
        budgetRepository.update(budget)
    }

    private fun openBudget(budget: Budget) =
        _uiState.update { it.copy(navigateToBudget = budget) }

    private fun clearFilter() {
        viewModelScope.launch {
            val budgetList = budgetRepository.getAllCached()
            _uiState.update { it.copy(budgetList = budgetList, filter = null) }
        }
    }

    private fun clearNavigation() =
        _uiState.update { it.copy(navigateToBudget = null) }

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

    private fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            preferencesManager.setOfflineModeEnabled(false)
            _uiState.update { it.copy(navigateToSignIn = true, isAuthenticated = false) }
        }
    }

    private fun signIn() {
        _uiState.update { it.copy(navigateToSignIn = true) }
    }

    private fun clearSignInNavigation() {
        _uiState.update { it.copy(navigateToSignIn = false) }
    }
}