package com.meneses.budgethunter.budgetMetrics.application

import com.meneses.budgethunter.budgetEntry.data.BudgetEntrySyncManager
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.network.BudgetEntryApiService
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.network.ApiError
import com.meneses.budgethunter.commons.data.network.models.CategorizeEntriesResponse
import com.meneses.budgethunter.commons.data.sync.SyncResult
import com.meneses.budgethunter.settings.application.SyncUserPreferencesUseCase
import kotlinx.coroutines.flow.first

/**
 * Asks the server to categorize the budget's waiting entries and brings the result back.
 *
 * The order matters: the server can only categorize what it has, so the budget's unsynced entries
 * go up first; and the entries are pulled afterwards, so the screen shows what the server decided.
 */
class CategorizeEntriesUseCase(
    private val entrySyncManager: BudgetEntrySyncManager,
    private val budgetEntryApiService: BudgetEntryApiService,
    private val budgetEntryLocalDataSource: BudgetEntryLocalDataSource,
    private val syncUserPreferences: SyncUserPreferencesUseCase
) {
    suspend fun execute(budget: Budget): CategorizationOutcome {
        val serverId = budget.serverId
            ?: return CategorizationOutcome.Failed(IllegalStateException("Budget ${budget.id} is not on the server"))

        val push = entrySyncManager.syncPendingEntries(budget.id)
        if (push is SyncResult.Failure) return CategorizationOutcome.Failed(push.error)

        val response = categorize(serverId).getOrElse { return it.toOutcome() }

        val pull = entrySyncManager.pullEntriesFromServer(serverId, localBudgetId = budget.id)
        if (pull is SyncResult.Failure) return CategorizationOutcome.Failed(pull.error)

        // The pull writes to the database; reading it back here refreshes the entries the totals
        // are computed from, which would otherwise catch up on their own a moment later.
        budgetEntryLocalDataSource.selectAllByBudgetId(budget.id.toLong()).first()

        return CategorizationOutcome.Done(response.categorized, response.pending)
    }

    /**
     * A 403 may only mean the server has not heard yet that AI processing is on (the preference
     * was changed while offline, or the push failed), so the preferences are saved and the call is
     * retried once before giving up.
     */
    private suspend fun categorize(serverId: Long): Result<CategorizeEntriesResponse> {
        val first = budgetEntryApiService.categorizeEntries(serverId)
        if (first.exceptionOrNull() != ApiError.Forbidden) return first

        syncUserPreferences.push()
        return budgetEntryApiService.categorizeEntries(serverId)
    }

    private fun Throwable.toOutcome() = when (this) {
        ApiError.Conflict -> CategorizationOutcome.AlreadyRunning
        ApiError.Forbidden -> CategorizationOutcome.AiOff
        else -> CategorizationOutcome.Failed(this)
    }
}
