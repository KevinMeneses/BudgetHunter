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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

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

    /** Reconnection state of the open stream, see [Connection]. */
    private var currentConnection: Connection? = null

    /**
     * Last caller of [startListening]; only it may end the stream with [stopListening].
     *
     * Like [currentJob], [currentBudgetServerId] and [currentConnection], it is not synchronized: the manager is only
     * called from the main thread (the budget detail ViewModel).
     */
    private var currentOwner: Any? = null

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
     * the rest of the session. The backoff starts over once a connection succeeds, and
     * [reconnectNow] skips the wait. Only cancelling the job (see [stopListening]) ends it.
     *
     * The caller becomes the owner of the stream even when it is already open for the same
     * budget, so a screen that is being destroyed cannot close the stream a newer screen needs:
     * see [stopListening] with an owner.
     *
     * @param budgetServerId Server-side ID of the budget to listen for updates
     * @param owner Who asks for the stream; pass the same object to [stopListening]
     */
    fun startListening(budgetServerId: Long, owner: Any) {
        // Stop existing listener if it's for a different budget
        if (currentBudgetServerId != budgetServerId) {
            stopListening()
        }

        currentOwner = owner

        if (currentJob != null) {
            return
        }

        currentBudgetServerId = budgetServerId
        val connection = Connection()
        currentConnection = connection

        currentJob = sseClient
            .subscribeToBudgetEntries(budgetServerId, onConnected = connection::onConnected)
            .onEach { event -> handleBudgetEntryEvent(event) }
            // The server closing the stream cleanly (a deploy, a proxy timeout) ends the flow
            // without an error, which retryWhen would not see: treat it as a drop too
            .onCompletion { cause -> if (cause == null) throw StreamEndedException() }
            .retryWhen { cause, _ -> reconnectAfterBackoff(cause, connection) }
            .catch { logger.error(tag, "SSE stream error", it) }
            .launchIn(scope)
    }

    /**
     * Decides whether a failed SSE stream should be re-established, waiting first.
     *
     * Cancellation must propagate untouched, otherwise [stopListening] would be unable to end
     * the stream. Any other failure is transient from the client's point of view (server
     * restart, connectivity loss, proxy dropping the connection), so it is retried forever with
     * a delay that doubles with each consecutive failure up to [MAX_RECONNECT_DELAY_MS].
     *
     * The wait ends early when [reconnectNow] is called, and the failures count from zero again.
     *
     * @return true to resubscribe, false to let the failure reach the collector
     */
    private suspend fun reconnectAfterBackoff(cause: Throwable, connection: Connection): Boolean {
        if (cause is CancellationException) return false

        val failures = connection.consecutiveFailures++
        val backoffMultiplier = 1L shl failures.coerceAtMost(MAX_BACKOFF_EXPONENT)
        val delayMillis = (INITIAL_RECONNECT_DELAY_MS * backoffMultiplier)
            .coerceAtMost(MAX_RECONNECT_DELAY_MS)

        logger.warn(tag, "SSE stream dropped, reconnecting in ${delayMillis}ms (attempt ${failures + 1})", cause)
        val woken = withTimeoutOrNull(delayMillis) { connection.wakeUp.receive() } != null
        if (woken) {
            logger.debug(tag, "Reconnecting now instead of waiting")
            connection.consecutiveFailures = 0
        }
        return true
    }

    /**
     * Reconnects right away if the stream is waiting to reconnect, with the backoff started over.
     *
     * For when the app comes back to the foreground: Android cuts the network of background apps,
     * so the stream usually dropped meanwhile and could otherwise still be waiting up to
     * [MAX_RECONNECT_DELAY_MS]. Does nothing when not listening; when the stream is connected, the
     * next drop reconnects without waiting once.
     */
    fun reconnectNow() {
        currentConnection?.wakeUp?.trySend(Unit)
    }

    /**
     * Handle a real-time budget entry notification.
     *
     * Based on the action type:
     * - CREATED/UPDATED: Triggers a full refresh from server to get latest state
     * - DELETED: Removes the entry from local database, unless it holds a change still to be pushed
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
                    // An entry with a change of its own still to be pushed stays: its push finds
                    // it gone and creates it again, as for a deletion seen by a pull
                    localDataSource.deleteSyncedByServerId(event.entryId)
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
        currentConnection = null
        currentBudgetServerId = null
        currentOwner = null
    }

    /**
     * Makes [owner] the owner of the stream if it is already open for [budgetServerId], without
     * opening one otherwise.
     *
     * For a screen that cannot tell right now whether the budget is still shared (the
     * collaborators lookup failed) but must not lose a stream a previous screen opened for it.
     */
    fun claimIfListening(budgetServerId: Long, owner: Any) {
        if (currentJob != null && currentBudgetServerId == budgetServerId) currentOwner = owner
    }

    /**
     * Stops listening only if [owner] is the last caller of [startListening].
     *
     * When the user opens a budget screen while another one is being removed from the back
     * stack (for example the default budget opened at launch), the new screen may start the
     * stream before the old one is cleared; the old one must then leave the stream alone.
     */
    fun stopListening(owner: Any) {
        if (currentOwner === owner) stopListening()
    }

    /** The server closed the stream without an error; reconnected like any other drop. */
    private class StreamEndedException : Exception("SSE stream closed by the server")

    /**
     * Reconnection state of one stream.
     *
     * [consecutiveFailures] is only touched from the stream's collecting coroutine
     * ([onConnected] runs there too). [wakeUp] is the only part [reconnectNow] uses, from the main
     * thread; it is conflated so a call made while the stream is failing is not lost, and drained
     * on connection so a call made while connected only skips one wait.
     */
    private class Connection {
        var consecutiveFailures = 0
        val wakeUp = Channel<Unit>(Channel.CONFLATED)

        fun onConnected() {
            consecutiveFailures = 0
            wakeUp.tryReceive()
        }
    }

    private companion object {
        const val INITIAL_RECONNECT_DELAY_MS = 1_000L
        const val MAX_RECONNECT_DELAY_MS = 60_000L
        const val MAX_BACKOFF_EXPONENT = 6
    }
}
