package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter

data class BudgetListState(
    val budgetList: List<Budget> = emptyList(),
    val isLoading: Boolean = true,
    val addModalVisibility: Boolean = false,
    val budgetToUpdate: Budget? = null,
    val filter: BudgetFilter? = null,
    val isSearchMode: Boolean = false,
    val searchQuery: String = "",
    val isAuthenticated: Boolean = false,
    /** The signed-in account, whose initial the app bar shows; null in offline mode. */
    val accountEmail: String? = null,
    val isSyncing: Boolean = false,
    val isCreatingBudget: Boolean = false,
    val isUpdatingBudget: Boolean = false,
    /** The bank notification reader should run but the system left it unbound; Settings explains the fix. */
    val isNotificationReaderStopped: Boolean = false
)
