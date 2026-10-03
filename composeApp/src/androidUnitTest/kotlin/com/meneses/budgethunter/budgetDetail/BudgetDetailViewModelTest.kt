package com.meneses.budgethunter.budgetDetail

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.sync_failed_background
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailIntent
import com.meneses.budgethunter.budgetDetail.data.BudgetDetailRepository
import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.data.sync.RealTimeSyncManager
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.collaborator.data.CollaboratorRepository
import com.meneses.budgethunter.commons.data.network.models.UserInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for BudgetDetailViewModel event emissions.
 *
 * Verifies that the ViewModel emits the correct one-shot events via its Channel-backed
 * `events` Flow for navigation and error-notification scenarios.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetDetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Mocks
    private val budgetDetailRepository = mockk<BudgetDetailRepository>(relaxed = true)
    private val realTimeSyncManager = mockk<RealTimeSyncManager>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val collaboratorRepository = mockk<CollaboratorRepository>(relaxed = true)

    // Controllable flow for collaborator notifications – shared across all tests
    private val collaboratorNotificationsFlow = MutableSharedFlow<String>(extraBufferCapacity = 8)

    // Controllable set of budgets whose background sync failed
    private val failedSyncFlow = MutableStateFlow<Set<Int>>(emptySet())

    // System under test
    private lateinit var viewModel: BudgetDetailViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Default auth state: not authenticated – prevents SSE startup in init block
        coEvery { authRepository.isAuthenticated() } returns false
        // Collaborator check returns empty list so SSE is not started
        coEvery { collaboratorRepository.getCollaborators(any()) } returns Result.success(emptyList<UserInfo>())
        every { budgetDetailRepository.budgetsWithFailedSync } returns failedSyncFlow
        // Wire the controllable shared flow so the ViewModel can collect from it
        every { realTimeSyncManager.collaboratorNotifications } returns collaboratorNotificationsFlow

        viewModel = BudgetDetailViewModel(
            budgetDetailRepository = budgetDetailRepository,
            realTimeSyncManager = realTimeSyncManager,
            authRepository = authRepository,
            collaboratorRepository = collaboratorRepository
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== deleteBudget Tests ==========

    @Test
    fun `deleteBudget emits NavigateBack event`() = runTest {
        // Given - a budget is set in the ViewModel state
        val budget = Budget(id = 10, name = "Monthly Budget", amount = 3000.0)
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(budget))
        coJustRun { budgetDetailRepository.deleteBudget(any()) }

        // When
        viewModel.sendIntent(BudgetDetailIntent.DeleteBudget)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val event = viewModel.events.first()
        assertIs<BudgetDetailEvent.NavigateBack>(event)
    }

    // ========== showEntry Tests ==========

    @Test
    fun `showEntry intent emits ShowEntry event with correct entry`() = runTest {
        // Given
        val expectedEntry = BudgetEntry(
            id = 99,
            budgetId = 10,
            amount = "250.00",
            description = "Groceries"
        )

        // When
        viewModel.sendIntent(BudgetDetailIntent.ShowEntry(expectedEntry))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val event = viewModel.events.first()
        assertIs<BudgetDetailEvent.ShowEntry>(event)
        assertEquals(expectedEntry, event.entry)
    }

    // ========== syncEntries failure Tests ==========

    @Test
    fun `syncEntries failure emits ShowError event`() = runTest {
        // Given - a synced budget with a server ID is set in state
        val syncedBudget = Budget(
            id = 10,
            name = "Synced Budget",
            amount = 3000.0,
            serverId = 42L,
            isSynced = true
        )
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(syncedBudget))

        // Sync fails with an exception that maps to a known API error
        coEvery {
            budgetDetailRepository.syncEntries(any(), any())
        } returns Result.failure(Exception("Network error"))

        // When - request sync with showErrors = true (SyncEntries intent)
        viewModel.sendIntent(BudgetDetailIntent.SyncEntries)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val event = viewModel.events.first()
        assertIs<BudgetDetailEvent.ShowError>(event)
    }

    // ========== syncEntries success Tests ==========

    @Test
    fun `syncEntries success with showErrors=true emits ShowSuccess event`() = runTest {
        // Given - a synced budget with a server ID so SyncEntries intent forwards both IDs
        val syncedBudget = Budget(
            id = 10,
            name = "Synced Budget",
            amount = 3000.0,
            serverId = 42L,
            isSynced = true
        )
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(syncedBudget))

        // Repository returns success so the ViewModel should emit ShowSuccess
        coEvery {
            budgetDetailRepository.syncEntries(any(), any())
        } returns Result.success(Unit)

        // When - SyncEntries intent always passes showErrors = true
        viewModel.sendIntent(BudgetDetailIntent.SyncEntries)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - ShowSuccess is emitted to signal the user that sync completed
        val event = viewModel.events.first()
        assertIs<BudgetDetailEvent.ShowSuccess>(event)
    }

    // ========== collaborator notification Tests ==========

    @Test
    fun `collaborator notification emits ShowCollaboratorEntry event`() = runTest {
        // Given - the ViewModel is already collecting collaboratorNotifications in its init block.
        // Advance the dispatcher so the collection coroutine is active before we emit.
        testDispatcher.scheduler.advanceUntilIdle()
        val collaboratorName = "Alice"

        // When - a collaborator name is emitted by the RealTimeSyncManager
        collaboratorNotificationsFlow.emit(collaboratorName)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - the ViewModel forwards it as a ShowCollaboratorEntry event
        val event = viewModel.events.first()
        assertIs<BudgetDetailEvent.ShowCollaboratorEntry>(event)
        assertEquals(collaboratorName, event.collaboratorName)
    }

    // ========== ResumeSync Tests ==========

    private val resumeBudget = Budget(
        id = 10,
        name = "Synced Budget",
        amount = 3000.0,
        serverId = 42L,
        isSynced = true
    )

    @Test
    fun `ResumeSync with serverId calls syncEntries with state budget ids`() = runTest {
        // Given
        coEvery { budgetDetailRepository.syncEntries(any(), any()) } returns Result.success(Unit)
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(resumeBudget))

        // When
        viewModel.sendIntent(BudgetDetailIntent.ResumeSync)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { budgetDetailRepository.syncEntries(10, 42L) }
    }

    @Test
    fun `ResumeSync runs again after the initial auto-sync already happened`() = runTest {
        // Given - initial auto-sync triggered by GetBudgetDetail and finished
        coEvery { budgetDetailRepository.syncEntries(any(), any()) } returns Result.success(Unit)
        every { budgetDetailRepository.getBudgetDetailById(any()) } returns emptyFlow()
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(resumeBudget))
        viewModel.sendIntent(BudgetDetailIntent.GetBudgetDetail)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { budgetDetailRepository.syncEntries(10, 42L) }

        // When
        viewModel.sendIntent(BudgetDetailIntent.ResumeSync)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { budgetDetailRepository.syncEntries(10, 42L) }
    }

    @Test
    fun `ResumeSync without serverId does not call syncEntries`() = runTest {
        // Given
        viewModel.sendIntent(
            BudgetDetailIntent.SetBudget(Budget(id = 10, name = "Local", amount = 1.0, serverId = null))
        )

        // When
        viewModel.sendIntent(BudgetDetailIntent.ResumeSync)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { budgetDetailRepository.syncEntries(any(), any()) }
    }

    @Test
    fun `ResumeSync does not start another sync while one is in flight`() = runTest {
        // Given - a sync that never completes until released
        val gate = CompletableDeferred<Result<Unit>>()
        coEvery { budgetDetailRepository.syncEntries(any(), any()) } coAnswers { gate.await() }
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(resumeBudget))

        // When
        viewModel.sendIntent(BudgetDetailIntent.ResumeSync)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.sendIntent(BudgetDetailIntent.ResumeSync)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { budgetDetailRepository.syncEntries(any(), any()) }

        gate.complete(Result.success(Unit))
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `ResumeSync right after first composition does not duplicate the initial auto-sync`() = runTest {
        // Given - the initial auto-sync from GetBudgetDetail is still in flight
        val gate = CompletableDeferred<Result<Unit>>()
        coEvery { budgetDetailRepository.syncEntries(any(), any()) } coAnswers { gate.await() }
        every { budgetDetailRepository.getBudgetDetailById(any()) } returns emptyFlow()
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(resumeBudget))
        viewModel.sendIntent(BudgetDetailIntent.GetBudgetDetail)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - ON_RESUME fires on first composition
        viewModel.sendIntent(BudgetDetailIntent.ResumeSync)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { budgetDetailRepository.syncEntries(any(), any()) }

        gate.complete(Result.success(Unit))
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `ResumeSync is silent and never shows loading or syncing state`() = runTest {
        // Given
        val gate = CompletableDeferred<Result<Unit>>()
        coEvery { budgetDetailRepository.syncEntries(any(), any()) } coAnswers { gate.await() }
        // Screen already loaded (isLoading defaults to true until the detail is collected). The
        // budget starts without serverId so the initial auto-sync is skipped, then the loaded
        // detail brings it in.
        every { budgetDetailRepository.getBudgetDetailById(resumeBudget.id) } returns flowOf(BudgetDetail(budget = resumeBudget))
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(resumeBudget.copy(serverId = null)))
        viewModel.sendIntent(BudgetDetailIntent.GetBudgetDetail)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading, "precondition: screen loaded")

        // When - sync is in flight
        viewModel.sendIntent(BudgetDetailIntent.ResumeSync)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { budgetDetailRepository.syncEntries(any(), any()) }
        assertFalse(viewModel.uiState.value.isLoading, "isLoading must stay false")
        assertFalse(viewModel.uiState.value.isSyncingEntries, "isSyncingEntries must stay false")

        gate.complete(Result.success(Unit))
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isSyncingEntries)
    }

    @Test
    fun `ResumeSync emits no events on success`() = runTest {
        // Given
        coEvery { budgetDetailRepository.syncEntries(any(), any()) } returns Result.success(Unit)
        val received = mutableListOf<BudgetDetailEvent>()
        backgroundScope.launch(Dispatchers.Unconfined) { viewModel.events.collect { received.add(it) } }
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(resumeBudget))

        // When
        viewModel.sendIntent(BudgetDetailIntent.ResumeSync)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { budgetDetailRepository.syncEntries(10, 42L) }
        assertTrue(received.isEmpty(), "Expected no events but got $received")
    }

    @Test
    fun `ResumeSync emits no events on failure`() = runTest {
        // Given
        coEvery {
            budgetDetailRepository.syncEntries(any(), any())
        } returns Result.failure(Exception("Network error"))
        val received = mutableListOf<BudgetDetailEvent>()
        backgroundScope.launch(Dispatchers.Unconfined) { viewModel.events.collect { received.add(it) } }
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(resumeBudget))

        // When
        viewModel.sendIntent(BudgetDetailIntent.ResumeSync)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { budgetDetailRepository.syncEntries(10, 42L) }
        assertTrue(received.isEmpty(), "Expected no events but got $received")
    }

    // ========== Background sync failure Tests ==========

    private fun TestScope.collectEvents(): MutableList<BudgetDetailEvent> {
        val received = mutableListOf<BudgetDetailEvent>()
        backgroundScope.launch(Dispatchers.Unconfined) { viewModel.events.collect { received.add(it) } }
        return received
    }

    @Test
    fun `emits ShowError when budget id is already in failed sync set at the time it is set`() = runTest {
        // Given - the failure happened while nobody was collecting
        failedSyncFlow.value = setOf(10)
        val received = collectEvents()

        // When
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(Budget(id = 10, name = "B", amount = 100.0)))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(listOf<BudgetDetailEvent>(BudgetDetailEvent.ShowError(Res.string.sync_failed_background)), received)
    }

    @Test
    fun `does not emit ShowError when failed sync set only has other budget ids`() = runTest {
        // Given
        failedSyncFlow.value = setOf(11, 12)
        val received = collectEvents()

        // When
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(Budget(id = 10, name = "B", amount = 100.0)))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(received.isEmpty(), "Expected no events but got $received")
    }

    @Test
    fun `emits ShowError only once while budget id stays in failed sync set`() = runTest {
        // Given
        val received = collectEvents()
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(Budget(id = 10, name = "B", amount = 100.0)))
        testDispatcher.scheduler.advanceUntilIdle()

        // When - the budget fails, then another budget is added to the set
        failedSyncFlow.value = setOf(10)
        testDispatcher.scheduler.advanceUntilIdle()
        failedSyncFlow.value = setOf(10, 11)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(1, received.count { it is BudgetDetailEvent.ShowError }, "Got $received")
    }

    @Test
    fun `emits ShowError again after budget id is removed and re-added`() = runTest {
        // Given
        val received = collectEvents()
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(Budget(id = 10, name = "B", amount = 100.0)))
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        failedSyncFlow.value = setOf(10)
        testDispatcher.scheduler.advanceUntilIdle()
        failedSyncFlow.value = emptySet()
        testDispatcher.scheduler.advanceUntilIdle()
        failedSyncFlow.value = setOf(10)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(2, received.count { it is BudgetDetailEvent.ShowError }, "Got $received")
    }
}
