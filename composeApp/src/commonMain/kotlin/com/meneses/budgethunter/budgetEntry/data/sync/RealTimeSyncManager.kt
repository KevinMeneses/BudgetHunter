package com.meneses.budgethunter.budgetEntry.data.sync

import com.meneses.budgethunter.budgetEntry.data.BudgetEntrySyncManager
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.commons.data.network.models.BudgetEntryAction
import com.meneses.budgethunter.commons.data.network.models.BudgetEntryEvent
import com.meneses.budgethunter.commons.data.network.services.SseClient
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.data.sync.SyncResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
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
 * @property logger Logger for real-time sync operations
 */
class RealTimeSyncManager(
    private val sseClient: SseClient,
    private val syncManager: BudgetEntrySyncManager,
    private val localDataSource: BudgetEntryLocalDataSource,
    private val ioDispatcher: CoroutineDispatcher,
    private val scope: CoroutineScope,
    private val logger: Logger
) {
    private val tag = "RealTimeSyncManager"
    private var currentJob: Job? = null
    private var currentBudgetServerId: Long? = null

    private val _collaboratorNotifications = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val collaboratorNotifications: SharedFlow<String> = _collaboratorNotifications.asSharedFlow()

    /**
     * Start listening for real-time budget entry updates from a specific budget.
     *
     * Establishes an SSE connection and updates local database when events arrive.
     * If already listening to a different budget, stops the current connection first.
     *
     * The connection is re-established automatically with exponential backoff whenever the
     * stream fails, so a dropped connection does not silently disable real-time updates for
     * the rest of the session. Only cancelling the job (see [stopListening]) ends it.
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
            .retryWhen { cause, attempt -> reconnectAfterBackoff(cause, attempt) }
            .catch { logger.error(tag, "SSE stream error", it) }
            .launchIn(scope)
    }

    /**
     * Decides whether a failed SSE stream should be re-established, waiting first.
     *
     * Cancellation must propagate untouched, otherwise [stopListening] would be unable to end
     * the stream. Any other failure is transient from the client's point of view (server
     * restart, connectivity loss, proxy dropping the connection), so it is retried forever with
     * a delay that doubles up to [MAX_RECONNECT_DELAY_MS].
     *
     * @return true to resubscribe, false to let the failure reach the collector
     */
    private suspend fun reconnectAfterBackoff(cause: Throwable, attempt: Long): Boolean {
        if (cause is CancellationException) return false

        val backoffMultiplier = 1L shl attempt.coerceAtMost(MAX_BACKOFF_EXPONENT).toInt()
        val delayMillis = (INITIAL_RECONNECT_DELAY_MS * backoffMultiplier)
            .coerceAtMost(MAX_RECONNECT_DELAY_MS)

        logger.warn(tag, "SSE stream dropped, reconnecting in ${delayMillis}ms (attempt ${attempt + 1})", cause)
        delay(delayMillis)
        return true
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
        withContext(ioDispatcher) {
            when (event.action) {
                BudgetEntryAction.CREATED, BudgetEntryAction.UPDATED -> {
                    val result = syncManager.pullEntriesFromServer(event.budgetId)
                    when (result) {
                        is SyncResult.Failure -> {
                            logger.warn(tag, "Failed to pull entries after SSE event", result.error)
                        }

                        is SyncResult.PartialSuccess -> {
                            logger.warn(
                                tag = tag,
                                message = "Partial sync after SSE event: ${result.succeeded} succeeded, ${result.failed} failed"
                            )
                        }

                        is SyncResult.Success -> {
                            logger.debug(tag, "Successfully synced entries after SSE event")
                            _collaboratorNotifications.emit(event.userInfo.name)
                        }
                    }
                }

                BudgetEntryAction.DELETED -> {
                    val existingEntry = localDataSource.selectByServerId(event.entryId)
                    if (existingEntry != null) localDataSource.delete(existingEntry.id)
                }
            }
        }
    }

    /**
     * Stop listening for real-time updates and cleanup resources.
     * Cancels the collection job, which terminates the SSE connection.
     */
    fun stopListening() {
        currentJob?.cancel()
        currentJob = null
        currentBudgetServerId = null
    }

    private companion object {
        const val INITIAL_RECONNECT_DELAY_MS = 1_000L
        const val MAX_RECONNECT_DELAY_MS = 60_000L
        const val MAX_BACKOFF_EXPONENT = 6L
    }
}
