package com.meneses.budgethunter.budgetDetail

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.sync_failed_background
import androidx.lifecycle.ViewModelStore
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailIntent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetDetail.data.BudgetDetailRepository
import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.data.sync.RealTimeSyncManager
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.collaborator.data.CollaboratorRepository
import com.meneses.budgethunter.commons.data.network.models.UserInfo
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.verify
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

    private val budget = Budget(id = 10, name = "Budget", amount = 100.0)

    private fun entry(id: Int, description: String, amount: String = "1") =
        BudgetEntry(id = id, budgetId = budget.id, description = description, amount = amount, date = "2026-03-10")

    /** Opens [budget] with [entries], in the database order (newest first). */
    private fun showDetail(entries: List<BudgetEntry>) {
        every { budgetDetailRepository.getBudgetDetailById(budget.id) } returns flowOf(BudgetDetail(budget, entries))
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(budget))
        viewModel.sendIntent(BudgetDetailIntent.GetBudgetDetail)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    private fun shownDescriptions() = viewModel.uiState.value.budgetDetail.entries.map { it.description }

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

    // ========== Lifetime across navigation ==========

    /**
     * The ViewModel lives on while an entry is opened, and the screen asks for the detail again
     * when it comes back: the filter must still be applied to what the collection emits.
     */
    @Test
    fun `GetBudgetDetail again keeps the filter`() = runTest {
        val filter = BudgetEntryFilter(description = "merc")
        showDetail(listOf(entry(2, "Mercado"), entry(1, "Gasolina")))
        viewModel.sendIntent(BudgetDetailIntent.FilterEntries(filter))

        // When - back from an entry
        viewModel.sendIntent(BudgetDetailIntent.GetBudgetDetail)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(filter, viewModel.uiState.value.filter)
        assertEquals(listOf("Mercado"), shownDescriptions())
    }

    // ========== Filter ==========

    @Test
    fun `filtering applies right away, for every keystroke`() = runTest {
        showDetail(listOf(entry(3, "Mercado"), entry(2, "Mecánico"), entry(1, "Gasolina")))

        // No dispatcher advance: the search field relies on the state being updated synchronously
        viewModel.sendIntent(BudgetDetailIntent.FilterEntries(BudgetEntryFilter(description = "me")))
        assertEquals(listOf("Mercado", "Mecánico"), shownDescriptions())

        viewModel.sendIntent(BudgetDetailIntent.FilterEntries(BudgetEntryFilter(description = "mer")))
        assertEquals(listOf("Mercado"), shownDescriptions())
    }

    @Test
    fun `the total count is every entry of the budget, whatever the filter`() = runTest {
        showDetail(listOf(entry(2, "Mercado"), entry(1, "Gasolina")))

        viewModel.sendIntent(BudgetDetailIntent.FilterEntries(BudgetEntryFilter(description = "merc")))

        assertEquals(2, viewModel.uiState.value.totalEntryCount)
        assertEquals(1, viewModel.uiState.value.budgetDetail.entries.size)
    }

    @Test
    fun `the budget totals cover every entry, whatever the filter`() = runTest {
        showDetail(
            listOf(
                entry(3, "Mercado", amount = "100"),
                entry(2, "Gasolina", amount = "40"),
                entry(1, "Salario", amount = "500").copy(type = BudgetEntry.Type.INCOME)
            )
        )

        viewModel.sendIntent(BudgetDetailIntent.FilterEntries(BudgetEntryFilter(description = "merc")))

        // The summary describes the budget; only the list is narrowed
        assertEquals(140.0, viewModel.uiState.value.budgetExpenses)
        assertEquals(500.0, viewModel.uiState.value.budgetIncomes)
        assertEquals(listOf("Mercado"), shownDescriptions())
    }

    @Test
    fun `an entry arriving while filtering is filtered too`() = runTest {
        val source = MutableStateFlow(BudgetDetail(budget, listOf(entry(1, "Mercado"))))
        every { budgetDetailRepository.getBudgetDetailById(budget.id) } returns source
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(budget))
        viewModel.sendIntent(BudgetDetailIntent.GetBudgetDetail)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.sendIntent(BudgetDetailIntent.FilterEntries(BudgetEntryFilter(description = "merc")))

        // When - a collaborator adds two entries (real-time sync writes them to the database)
        source.value = BudgetDetail(
            budget,
            listOf(entry(3, "Mercado 2"), entry(2, "Gasolina"), entry(1, "Mercado"))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(listOf("Mercado 2", "Mercado"), shownDescriptions())
        assertEquals(3, viewModel.uiState.value.totalEntryCount)
    }

    @Test
    fun `the filter bar starts hidden, and hiding it keeps the filter`() = runTest {
        showDetail(listOf(entry(2, "Mercado"), entry(1, "Gasolina")))
        assertFalse(viewModel.uiState.value.isFilterBarVisible)

        viewModel.sendIntent(BudgetDetailIntent.ToggleFilterBar)
        assertTrue(viewModel.uiState.value.isFilterBarVisible)
        viewModel.sendIntent(BudgetDetailIntent.FilterEntries(BudgetEntryFilter(description = "merc")))

        viewModel.sendIntent(BudgetDetailIntent.ToggleFilterBar)

        assertFalse(viewModel.uiState.value.isFilterBarVisible)
        assertEquals(listOf("Mercado"), shownDescriptions())
    }

    @Test
    fun `ClearFilter shows every entry again`() = runTest {
        showDetail(listOf(entry(2, "Mercado"), entry(1, "Gasolina")))
        viewModel.sendIntent(BudgetDetailIntent.FilterEntries(BudgetEntryFilter(type = BudgetEntry.Type.INCOME)))
        assertEquals(emptyList(), shownDescriptions())

        viewModel.sendIntent(BudgetDetailIntent.ClearFilter)

        assertFalse(viewModel.uiState.value.filter.isActive)
        assertEquals(listOf("Mercado", "Gasolina"), shownDescriptions())
    }

    @Test
    fun `the chosen order survives a filter change and new entries`() = runTest {
        val source = MutableStateFlow(
            BudgetDetail(budget, listOf(entry(2, "Mercado", amount = "5"), entry(1, "Mecánico", amount = "1")))
        )
        every { budgetDetailRepository.getBudgetDetailById(budget.id) } returns source
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(budget))
        viewModel.sendIntent(BudgetDetailIntent.GetBudgetDetail)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(BudgetDetailIntent.SetListOrder(BudgetDetailState.ListOrder.AMOUNT_DESCENDANT))
        viewModel.sendIntent(BudgetDetailIntent.FilterEntries(BudgetEntryFilter(description = "me")))
        source.value = BudgetDetail(
            budget,
            listOf(
                entry(3, "Medias", amount = "3"),
                entry(2, "Mercado", amount = "5"),
                entry(1, "Mecánico", amount = "1")
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(BudgetDetailState.ListOrder.AMOUNT_DESCENDANT, viewModel.uiState.value.listOrder)
        assertEquals(listOf("Mercado", "Medias", "Mecánico"), shownDescriptions())
    }

    @Test
    fun `amounts are sorted as numbers, whatever the type`() = runTest {
        showDetail(
            listOf(
                entry(3, "Salario", amount = "20").copy(type = BudgetEntry.Type.INCOME),
                entry(2, "Arriendo", amount = "100"),
                entry(1, "Café", amount = "3.5")
            )
        )

        viewModel.sendIntent(BudgetDetailIntent.SetListOrder(BudgetDetailState.ListOrder.AMOUNT_DESCENDANT))
        assertEquals(listOf("Arriendo", "Salario", "Café"), shownDescriptions())

        viewModel.sendIntent(BudgetDetailIntent.SetListOrder(BudgetDetailState.ListOrder.AMOUNT_ASCENDANT))
        assertEquals(listOf("Café", "Salario", "Arriendo"), shownDescriptions())

        viewModel.sendIntent(BudgetDetailIntent.SetListOrder(BudgetDetailState.ListOrder.DEFAULT))
        assertEquals(listOf("Salario", "Arriendo", "Café"), shownDescriptions())
    }

    @Test
    fun `sorting while selecting keeps the selected entries`() = runTest {
        showDetail(listOf(entry(2, "Mercado", amount = "5"), entry(1, "Gasolina", amount = "1")))
        viewModel.sendIntent(BudgetDetailIntent.ToggleSelectionState(true))
        viewModel.sendIntent(BudgetDetailIntent.ToggleSelectEntry(index = 0, isSelected = true))

        viewModel.sendIntent(BudgetDetailIntent.SetListOrder(BudgetDetailState.ListOrder.AMOUNT_ASCENDANT))

        val selected = viewModel.uiState.value.budgetDetail.entries.filter { it.isSelected }
        assertEquals(listOf("Mercado"), selected.map { it.description })
    }

    @Test
    fun `selecting an entry that is no longer shown is ignored`() = runTest {
        showDetail(listOf(entry(1, "Mercado")))

        viewModel.sendIntent(BudgetDetailIntent.ToggleSelectEntry(index = 5, isSelected = true))

        assertTrue(viewModel.uiState.value.budgetDetail.entries.none { it.isSelected })
    }

    @Test
    fun `GetBudgetDetail again replaces the previous collection instead of adding one`() = runTest {
        val budget = Budget(id = 10, name = "Budget", amount = 100.0)
        val source = MutableSharedFlow<BudgetDetail>()
        every { budgetDetailRepository.getBudgetDetailById(10) } returns source

        viewModel.sendIntent(BudgetDetailIntent.SetBudget(budget))
        viewModel.sendIntent(BudgetDetailIntent.GetBudgetDetail)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.sendIntent(BudgetDetailIntent.GetBudgetDetail)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, source.subscriptionCount.value)
    }

    // ========== Real-time sync ==========

    private val sharedBudget = Budget(id = 10, name = "Shared", amount = 1.0, serverId = 42L, isSynced = true)
    private val twoCollaborators = listOf(
        UserInfo(email = "a@a.com", name = "a"),
        UserInfo(email = "b@b.com", name = "b")
    )

    @Test
    fun `starts real-time sync as owner when the budget has collaborators`() = runTest {
        coEvery { collaboratorRepository.getCollaborators(42L) } returns Result.success(twoCollaborators)

        viewModel.sendIntent(BudgetDetailIntent.SetBudget(sharedBudget))
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { realTimeSyncManager.startListening(42L, viewModel) }
    }

    @Test
    fun `ResumeSync starts real-time sync once a collaborator was added`() = runTest {
        // Given - opened without collaborators
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(sharedBudget))
        testDispatcher.scheduler.advanceUntilIdle()
        verify(exactly = 0) { realTimeSyncManager.startListening(any(), any()) }

        // When - back from the collaborators screen after sharing the budget
        coEvery { collaboratorRepository.getCollaborators(42L) } returns Result.success(twoCollaborators)
        viewModel.sendIntent(BudgetDetailIntent.ResumeSync)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        verify(exactly = 1) { realTimeSyncManager.startListening(42L, viewModel) }
    }

    @Test
    fun `ResumeSync asks the real-time stream to reconnect now`() = runTest {
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(sharedBudget))

        viewModel.sendIntent(BudgetDetailIntent.ResumeSync)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { realTimeSyncManager.reconnectNow() }
    }

    @Test
    fun `ResumeSync stops real-time sync once the collaborators are gone`() = runTest {
        coEvery { collaboratorRepository.getCollaborators(42L) } returns Result.success(twoCollaborators)
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(sharedBudget))
        testDispatcher.scheduler.advanceUntilIdle()

        forgetRealTimeSyncCalls()
        coEvery { collaboratorRepository.getCollaborators(42L) } returns Result.success(twoCollaborators.take(1))
        viewModel.sendIntent(BudgetDetailIntent.ResumeSync)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { realTimeSyncManager.stopListening(owner = viewModel) }
    }

    @Test
    fun `a failed collaborator lookup leaves real-time sync running`() = runTest {
        coEvery { collaboratorRepository.getCollaborators(42L) } returns Result.success(twoCollaborators)
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(sharedBudget))
        testDispatcher.scheduler.advanceUntilIdle()

        forgetRealTimeSyncCalls()
        coEvery { collaboratorRepository.getCollaborators(42L) } returns Result.failure(Exception("offline"))
        viewModel.sendIntent(BudgetDetailIntent.ResumeSync)
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { realTimeSyncManager.stopListening(any<Any>()) }
        verify(exactly = 0) { realTimeSyncManager.stopListening() }
        verify(exactly = 1) { realTimeSyncManager.claimIfListening(42L, viewModel) }
    }

    @Test
    fun `never stops real-time sync without naming itself as owner`() = runTest {
        coEvery { collaboratorRepository.getCollaborators(42L) } returns Result.success(twoCollaborators)
        viewModel.sendIntent(BudgetDetailIntent.SetBudget(sharedBudget))
        viewModel.sendIntent(BudgetDetailIntent.ResumeSync)
        testDispatcher.scheduler.advanceUntilIdle()

        forgetRealTimeSyncCalls()

        clearViewModel()

        verify(exactly = 0) { realTimeSyncManager.stopListening() }
        verify(exactly = 1) { realTimeSyncManager.stopListening(owner = viewModel) }
    }

    /** Drops the calls recorded so far (e.g. the stop for the initial state without server id). */
    private fun forgetRealTimeSyncCalls() = clearMocks(
        realTimeSyncManager,
        answers = false,
        recordedCalls = true,
        childMocks = false,
        verificationMarks = true,
        exclusionRules = false
    )

    /** Clears the ViewModel the way the navigation back stack does when the screen is left. */
    private fun clearViewModel() {
        val store = ViewModelStore()
        store.put("detail", viewModel)
        store.clear()
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

    // ========== showEntry scanReceipt Tests ==========

    @Test
    fun `showEntry with scanReceipt true emits ShowEntry event asking to scan the receipt`() = runTest {
        val entry = BudgetEntry(id = 5, budgetId = 10)

        viewModel.sendIntent(BudgetDetailIntent.ShowEntry(entry, scanReceipt = true))
        testDispatcher.scheduler.advanceUntilIdle()

        val event = viewModel.events.first()
        assertIs<BudgetDetailEvent.ShowEntry>(event)
        assertEquals(entry, event.entry)
        assertTrue(event.scanReceipt)
    }

    @Test
    fun `showEntry without scanReceipt emits ShowEntry event that does not scan the receipt`() = runTest {
        viewModel.sendIntent(BudgetDetailIntent.ShowEntry(BudgetEntry(id = 5, budgetId = 10)))
        testDispatcher.scheduler.advanceUntilIdle()

        val event = viewModel.events.first()
        assertIs<BudgetDetailEvent.ShowEntry>(event)
        assertFalse(event.scanReceipt)
    }
}
