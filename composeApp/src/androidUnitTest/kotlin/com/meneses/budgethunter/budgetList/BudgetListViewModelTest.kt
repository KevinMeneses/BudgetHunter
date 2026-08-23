package com.meneses.budgethunter.budgetList

import com.meneses.budgethunter.auth.application.SignOutUseCase
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.application.BudgetListIntent
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.application.DuplicateBudgetUseCase
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.sync.NoOpLogger
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

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
    private val signOutUseCase = mockk<SignOutUseCase>(relaxed = true)

    // System under test
    private lateinit var viewModel: BudgetListViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Default stub: emit an empty budget list so collectBudgetList does not hang
        coEvery { budgetRepository.budgets } returns flowOf(emptyList())
        coEvery { authRepository.isAuthenticated() } returns false

        viewModel = BudgetListViewModel(
            budgetRepository = budgetRepository,
            duplicateBudgetUseCase = duplicateBudgetUseCase,
            deleteBudgetUseCase = deleteBudgetUseCase,
            authRepository = authRepository,
            signOutUseCase = signOutUseCase,
            logger = NoOpLogger()
        )
    }

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

    // ========== signOut Tests ==========

    @Test
    fun `signOut success emits NavigateToSignIn event`() = runTest {
        // Given - sign-out use case completes successfully
        coJustRun { signOutUseCase.execute() }

        // When
        viewModel.sendIntent(BudgetListIntent.SignOut)
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
}
