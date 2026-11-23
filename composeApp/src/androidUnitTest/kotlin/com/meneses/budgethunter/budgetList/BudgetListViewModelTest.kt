package com.meneses.budgethunter.budgetList

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.application.BudgetListIntent
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.application.DuplicateBudgetUseCase
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.data.sync.NoOpLogger
import com.meneses.budgethunter.commons.platform.NetworkMonitor
import com.meneses.budgethunter.sms.application.IsNotificationReaderStoppedUseCase
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/**
 * Unit tests for BudgetListViewModel event emissions.
 *
 * These tests verify that the ViewModel correctly emits one-shot navigation events
 * through its `events` Channel-backed Flow when specific intents are processed.
 *
 * The ViewModels are refactored to emit events via a Channel<BudgetListEvent>(Channel.BUFFERED)
 * exposed as receiveAsFlow(). Tests collect the first event emission and assert correctness.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Mocks
    private val budgetRepository = mockk<BudgetRepository>(relaxed = true)
    private val duplicateBudgetUseCase = mockk<DuplicateBudgetUseCase>(relaxed = true)
    private val deleteBudgetUseCase = mockk<DeleteBudgetUseCase>(relaxed = true)
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val isNotificationReaderStopped = mockk<IsNotificationReaderStoppedUseCase>(relaxed = true)

    // Connectivity seen by the view model each test creates; the one built in setup() gets its
    // own, always offline, so it never auto syncs behind a test's back.
    private val online = MutableStateFlow(false)

    private fun monitorOf(isOnline: StateFlow<Boolean>) = object : NetworkMonitor {
        override val isOnline: StateFlow<Boolean> = isOnline
        override fun startMonitoring() = Unit
        override fun stopMonitoring() = Unit
    }

    // System under test
    private lateinit var viewModel: BudgetListViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Default stub: emit an empty budget list so collectBudgetList does not hang
        coEvery { budgetRepository.budgets } returns flowOf(emptyList())
        coEvery { authRepository.isAuthenticated() } returns false

        viewModel = createViewModel(isOnline = MutableStateFlow(false))
    }

    private fun createViewModel(isOnline: StateFlow<Boolean> = online) = BudgetListViewModel(
        budgetRepository = budgetRepository,
        duplicateBudgetUseCase = duplicateBudgetUseCase,
        deleteBudgetUseCase = deleteBudgetUseCase,
        authRepository = authRepository,
        preferencesManager = preferencesManager,
        isNotificationReaderStopped = isNotificationReaderStopped,
        networkMonitor = monitorOf(isOnline),
        logger = NoOpLogger()
    )

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== openBudget Tests ==========

    @Test
    fun `openBudget intent emits NavigateToBudget event with correct budget`() = runTest {
        // Given
        val expectedBudget = Budget(id = 42, name = "Vacation Fund", amount = 5000.0)

        // When
        viewModel.sendIntent(BudgetListIntent.OpenBudget(expectedBudget))
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - collect the first event from the events flow
        val event = viewModel.events.first()
        assertIs<BudgetListEvent.NavigateToBudget>(event)
        assertEquals(expectedBudget, event.budget)
    }

    // ========== signIn Tests ==========

    @Test
    fun `signIn intent emits NavigateToSignIn event`() = runTest {
        // When
        viewModel.sendIntent(BudgetListIntent.SignIn)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val event = viewModel.events.first()
        assertIs<BudgetListEvent.NavigateToSignIn>(event)
    }

    // ========== syncBudgets Tests ==========

    @Test
    fun `syncBudgets success emits ShowMessage with success string resource`() = runTest {
        // Given - sync completes without throwing
        coJustRun { budgetRepository.sync() }

        // When
        viewModel.sendIntent(BudgetListIntent.SyncBudgets)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - a ShowMessage event is emitted (StringResource cannot be compared directly)
        val event = viewModel.events.first()
        assertIs<BudgetListEvent.ShowMessage>(event)
    }

    @Test
    fun `syncBudgets failure emits ShowMessage with failure string resource`() = runTest {
        // Given - sync throws a network-related exception
        coEvery { budgetRepository.sync() } throws Exception("Network error")

        // When
        viewModel.sendIntent(BudgetListIntent.SyncBudgets)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - a ShowMessage event is still emitted so the user is informed of the failure
        val event = viewModel.events.first()
        assertIs<BudgetListEvent.ShowMessage>(event)
    }

    // ========== accountEmail Tests ==========

    @Test
    fun `accountEmail is the last signed in email when authenticated`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { preferencesManager.getLastSignedInEmail() } returns "me@example.com"

        val authenticatedViewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("me@example.com", authenticatedViewModel.uiState.value.accountEmail)
    }

    @Test
    fun `accountEmail is null when not authenticated`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns false
        coEvery { preferencesManager.getLastSignedInEmail() } returns "stale@example.com"

        val offlineViewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(offlineViewModel.uiState.value.accountEmail)
    }

    @Test
    fun `coming to the front shows whether the notification reader stopped`() = runTest {
        coEvery { isNotificationReaderStopped() } returns true

        viewModel.sendIntent(BudgetListIntent.CheckNotificationReader)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isNotificationReaderStopped)

        coEvery { isNotificationReaderStopped() } returns false

        viewModel.sendIntent(BudgetListIntent.CheckNotificationReader)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isNotificationReaderStopped)
    }

    // ========== Auto sync on connection Tests ==========

    private fun stubAccount(authenticated: Boolean, offlineMode: Boolean) {
        coEvery { authRepository.isAuthenticated() } returns authenticated
        coEvery { preferencesManager.isOfflineModeEnabled() } returns offlineMode
    }

    @Test
    fun `online at start with a session syncs once and silently`() = runTest {
        // Given
        stubAccount(authenticated = true, offlineMode = false)
        coEvery { budgetRepository.sync() } returns Result.success(Unit)
        online.value = true
        val vm = createViewModel()
        val events = mutableListOf<BudgetListEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.toList(events) }

        // When
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { budgetRepository.sync() }
        assertTrue(events.isEmpty(), "auto sync must not show messages")
    }

    @Test
    fun `offline mode on never auto syncs`() = runTest {
        // Given
        stubAccount(authenticated = true, offlineMode = true)
        online.value = true
        createViewModel()

        // When
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { budgetRepository.sync() }
    }

    @Test
    fun `not signed in never auto syncs`() = runTest {
        // Given
        stubAccount(authenticated = false, offlineMode = false)
        online.value = true
        createViewModel()

        // When
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 0) { budgetRepository.sync() }
    }

    @Test
    fun `offline at start does not sync until the connection comes back`() = runTest {
        // Given
        stubAccount(authenticated = true, offlineMode = false)
        coEvery { budgetRepository.sync() } returns Result.success(Unit)
        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { budgetRepository.sync() }

        // When
        online.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { budgetRepository.sync() }
    }

    @Test
    fun `a trigger while an auto sync runs is dropped`() = runTest {
        // Given - the first sync is held open
        stubAccount(authenticated = true, offlineMode = false)
        val gate = CompletableDeferred<Unit>()
        coEvery { budgetRepository.sync() } coAnswers { gate.await(); Result.success(Unit) }
        online.value = true
        createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        // When - the connection blinks while syncing
        online.value = false
        testDispatcher.scheduler.advanceUntilIdle()
        online.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - still one call
        coVerify(exactly = 1) { budgetRepository.sync() }

        // When - it finishes and the connection returns again
        gate.complete(Unit)
        testDispatcher.scheduler.advanceUntilIdle()
        online.value = false
        testDispatcher.scheduler.advanceUntilIdle()
        online.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify(exactly = 2) { budgetRepository.sync() }
    }

    @Test
    fun `failed auto sync emits nothing and does not flag syncing`() = runTest {
        // Given
        stubAccount(authenticated = true, offlineMode = false)
        coEvery { budgetRepository.sync() } returns Result.failure(Exception("boom"))
        online.value = true
        val vm = createViewModel()
        val events = mutableListOf<BudgetListEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.events.toList(events) }

        // When
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(events.isEmpty())
        assertFalse(vm.uiState.value.isSyncing)
    }
}
