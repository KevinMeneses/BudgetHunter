package com.meneses.budgethunter.budgetMetrics.application

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Decides whether the metrics screen should offer to categorize a budget's entries, and
 * remembers when the user said "Not now".
 */
class GetCategorizationOfferUseCase(
    private val preferencesManager: PreferencesManager,
    private val budgetEntryLocalDataSource: BudgetEntryLocalDataSource
) {
    // Budget (local id) -> how many entries were waiting when the user declined. Kept in memory
    // on purpose: the offer comes back when the app restarts.
    private val declined = MutableStateFlow<Map<Int, Int>>(emptyMap())

    /**
     * How many entries the dialog would offer to categorize, or 0 when it must not appear: AI
     * processing is off, offline mode is on, the budget is not on the server yet, nothing is
     * waiting, or the user already declined for this many (or more) entries.
     */
    suspend fun execute(budget: Budget): Int {
        if (budget.serverId == null) return 0
        if (!preferencesManager.isAiProcessingEnabled()) return 0
        if (preferencesManager.isOfflineModeEnabled()) return 0

        val waiting = countWaiting(budget)
        val declinedAt = declined.value[budget.id] ?: return waiting
        return if (waiting > declinedAt) waiting else 0
    }

    /** "Not now": do not ask again until more entries than [waiting] are waiting. */
    fun decline(budget: Budget, waiting: Int) = declined.update { it + (budget.id to waiting) }

    fun clearDecline(budget: Budget) = declined.update { it - budget.id }

    private suspend fun countWaiting(budget: Budget) = budgetEntryLocalDataSource
        .getAllCached()
        .count { it.budgetId == budget.id && it.category == BudgetEntry.Category.UNCATEGORIZED }
}
