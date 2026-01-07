package com.meneses.budgethunter.budgetEntry.data.sync

import com.meneses.budgethunter.budgetEntry.data.BudgetEntrySyncManager
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.commons.data.network.models.BudgetEntryAction
import com.meneses.budgethunter.commons.data.network.models.BudgetEntryEvent
import com.meneses.budgethunter.commons.data.network.services.SseClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

/**
 * Manages real-time synchronization of budget entries from collaborators.
 *
 * Listens to SSE events for lightweight notifications about budget entry changes.
 * When a notification arrives, triggers a full refresh from the server to get the
 * latest state and updates the local database accordingly.
 *
 * This notification-based approach reduces bandwidth usage by ~90% compared to
 * sending full entry data in SSE events.
 *
 * @property sseClient Client for establishing SSE connection to backend
 * @property syncManager Sync manager for pulling entries from server
 * @property localDataSource Local database access for budget entries
 * @property ioDispatcher Dispatcher for database operations
 * @property scope Coroutine scope for launching collection jobs
 */
class RealTimeSyncManager(
    private val sseClient: SseClient,
    private val syncManager: BudgetEntrySyncManager,
    private val localDataSource: BudgetEntryLocalDataSource,
    private val ioDispatcher: CoroutineDispatcher,
    private val scope: CoroutineScope
) {
    private var currentJob: Job? = null
    private var currentBudgetServerId: Long? = null

    /**
     * Start listening for real-time budget entry updates from a specific budget.
     *
     * Establishes an SSE connection and updates local database when events arrive.
     * If already listening to a different budget, stops the current connection first.
     *
     * @param budgetServerId Server-side ID of the budget to listen for updates
     */
    fun startListening(budgetServerId: Long) {
        // Stop existing listener if it's for a different budget
        if (currentBudgetServerId != budgetServerId) {
            stopListening()
        }

        if (currentJob != null) {
            return
        }

        currentBudgetServerId = budgetServerId

        currentJob = sseClient.subscribeToBudgetEntries(budgetServerId)
            .onEach { event -> handleBudgetEntryEvent(event) }
            .catch { it.printStackTrace() }
            .launchIn(scope)
    }

    /**
     * Handle a real-time budget entry notification.
     *
     * Based on the action type:
     * - CREATED/UPDATED: Triggers a full refresh from server to get latest state
     * - DELETED: Removes the entry from local database
     *
     * @param event The notification event received from SSE
     */
    private suspend fun handleBudgetEntryEvent(event: BudgetEntryEvent) {
        try {
            withContext(ioDispatcher) {
                when (event.action) {
                    BudgetEntryAction.CREATED, BudgetEntryAction.UPDATED -> {
                        syncManager.pullEntriesFromServer(event.budgetId)
                    }

                    BudgetEntryAction.DELETED -> {
                        val existingEntry = localDataSource.selectByServerId(event.entryId)
                        if (existingEntry != null) localDataSource.delete(existingEntry.id)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Stop listening for real-time updates and cleanup resources.
     */
    fun stopListening() {
        currentJob?.cancel()
        currentJob = null
        currentBudgetServerId = null
    }
}
