package com.meneses.budgethunter.budgetEntry.data.sync

import com.meneses.budgethunter.budgetEntry.data.BudgetEntrySyncManager
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.commons.data.network.models.BudgetEntryAction
import com.meneses.budgethunter.commons.data.network.models.BudgetEntryEvent
import com.meneses.budgethunter.commons.data.network.models.UserInfo
import com.meneses.budgethunter.commons.data.network.services.SseClient
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.data.sync.SyncResult
import com.meneses.budgethunter.commons.data.sync.SyncStats
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the SSE connection lifecycle handled by RealTimeSyncManager.
 */
class RealTimeSyncManagerTest {

    private val sseClient = mockk<SseClient>()
    private val syncManager = mockk<BudgetEntrySyncManager>(relaxed = true)
    private val localDataSource = mockk<BudgetEntryLocalDataSource>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)

    private val event = BudgetEntryEvent(
        budgetId = BUDGET_SERVER_ID,
        entryId = 42L,
        action = BudgetEntryAction.CREATED,
        userInfo = UserInfo(email = "b@b.com", name = "b")
    )

    /**
     * Regression test: a dropped stream used to end the flow for good, so real-time updates
     * stayed dead for the rest of the session and the user had to refresh manually.
     */
    @Test
    fun `reconnects after the stream fails`() = runTest {
        var subscriptions = 0
        every { sseClient.subscribeToBudgetEntries(BUDGET_SERVER_ID) } returns flow {
            subscriptions++
            if (subscriptions == 1) throw ConnectionDropped()
            emit(event)
        }
        coEvery { syncManager.pullEntriesFromServer(BUDGET_SERVER_ID) } returns
            SyncResult.Success(SyncStats(totalItems = 1, syncedItems = 1))

        val manager = createManager()
        manager.startListening(BUDGET_SERVER_ID, owner = this)

        // First attempt fails; the retry only happens once the backoff delay elapses
        advanceTimeBy(FIRST_BACKOFF_MS)
        advanceUntilIdle()

        assertEquals(2, subscriptions)
        coVerify(exactly = 1) { syncManager.pullEntriesFromServer(BUDGET_SERVER_ID) }

        manager.stopListening()
    }

    /**
     * Cancellation must not be treated as a transient failure, otherwise stopListening could
     * never end the stream and it would resubscribe forever.
     */
    @Test
    fun `stopListening does not trigger a reconnection`() = runTest {
        var subscriptions = 0
        every { sseClient.subscribeToBudgetEntries(BUDGET_SERVER_ID) } returns flow {
            subscriptions++
            awaitCancellation()
        }

        val manager = createManager()
        manager.startListening(BUDGET_SERVER_ID, owner = this)
        advanceUntilIdle()

        manager.stopListening()
        advanceTimeBy(FIRST_BACKOFF_MS * 4)
        advanceUntilIdle()

        assertEquals(1, subscriptions)
    }

    /**
     * A budget screen being removed from the back stack while a newer one for the same budget
     * already took over the stream (e.g. the default budget opened at launch) must not close it.
     */
    @Test
    fun `stopListening by a replaced owner keeps the stream open`() = runTest {
        var subscriptions = 0
        var cancellations = 0
        every { sseClient.subscribeToBudgetEntries(BUDGET_SERVER_ID) } returns flow {
            subscriptions++
            try {
                awaitCancellation()
            } finally {
                cancellations++
            }
        }
        val oldScreen = Any()
        val newScreen = Any()

        val manager = createManager()
        manager.startListening(BUDGET_SERVER_ID, owner = oldScreen)
        manager.startListening(BUDGET_SERVER_ID, owner = newScreen)
        manager.stopListening(owner = oldScreen)
        advanceUntilIdle()

        assertEquals(1, subscriptions)
        assertEquals(0, cancellations)

        manager.stopListening(owner = newScreen)
        advanceUntilIdle()

        assertEquals(1, cancellations)
    }

    @Test
    fun `stopListening by a replaced owner keeps the stream of another budget open`() = runTest {
        var cancellations = 0
        every { sseClient.subscribeToBudgetEntries(any()) } returns flow {
            try {
                awaitCancellation()
            } finally {
                cancellations++
            }
        }
        val oldScreen = Any()
        val newScreen = Any()

        val manager = createManager()
        manager.startListening(BUDGET_SERVER_ID, owner = oldScreen)
        manager.startListening(OTHER_BUDGET_SERVER_ID, owner = newScreen)
        advanceUntilIdle()
        // Switching budgets closes the first stream
        assertEquals(1, cancellations)

        manager.stopListening(owner = oldScreen)
        advanceUntilIdle()

        assertEquals(1, cancellations)
        manager.stopListening()
    }

    /**
     * A new screen whose collaborators lookup failed takes over the stream an older screen opened,
     * so the older one being cleared does not close it.
     */
    @Test
    fun `claimIfListening takes over an open stream for the same budget`() = runTest {
        var subscriptions = 0
        var cancellations = 0
        every { sseClient.subscribeToBudgetEntries(BUDGET_SERVER_ID) } returns flow {
            subscriptions++
            try {
                awaitCancellation()
            } finally {
                cancellations++
            }
        }
        val oldScreen = Any()
        val newScreen = Any()

        val manager = createManager()
        manager.startListening(BUDGET_SERVER_ID, owner = oldScreen)
        manager.claimIfListening(BUDGET_SERVER_ID, owner = newScreen)
        manager.stopListening(owner = oldScreen)
        advanceUntilIdle()

        assertEquals(1, subscriptions)
        assertEquals(0, cancellations)
        manager.stopListening()
    }

    @Test
    fun `claimIfListening opens nothing when not listening to that budget`() = runTest {
        var subscriptions = 0
        every { sseClient.subscribeToBudgetEntries(any()) } returns flow {
            subscriptions++
            awaitCancellation()
        }

        val manager = createManager()
        manager.claimIfListening(BUDGET_SERVER_ID, owner = this)
        advanceUntilIdle()
        assertEquals(0, subscriptions)

        // Nor takes over the stream of another budget
        val otherScreen = Any()
        manager.startListening(OTHER_BUDGET_SERVER_ID, owner = otherScreen)
        manager.claimIfListening(BUDGET_SERVER_ID, owner = this)
        manager.stopListening(owner = this)
        advanceUntilIdle()
        assertEquals(1, subscriptions)
        manager.stopListening(owner = otherScreen)
    }

    /**
     * The manager collects on [scope], so it must run eagerly for the subscription to happen as
     * soon as startListening is called. Delays still go through the test scheduler, which is what
     * lets the backoff be skipped with advanceTimeBy.
     */
    private fun kotlinx.coroutines.test.TestScope.createManager(): RealTimeSyncManager {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return RealTimeSyncManager(
            sseClient = sseClient,
            syncManager = syncManager,
            localDataSource = localDataSource,
            ioDispatcher = dispatcher,
            scope = CoroutineScope(dispatcher),
            logger = logger
        )
    }

    private class ConnectionDropped : Exception("stream dropped")

    private companion object {
        const val BUDGET_SERVER_ID = 3L
        const val OTHER_BUDGET_SERVER_ID = 4L
        const val FIRST_BACKOFF_MS = 1_000L
    }
}
