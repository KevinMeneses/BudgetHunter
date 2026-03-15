package com.meneses.budgethunter.budgetDetail

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailIntent
import com.meneses.budgethunter.budgetDetail.data.BudgetDetailRepository
import com.meneses.budgethunter.budgetEntry.data.sync.RealTimeSyncManager
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.collaborator.data.CollaboratorRepository
import com.meneses.budgethunter.commons.data.network.models.UserInfo
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

    // System under test
    private lateinit var viewModel: BudgetDetailViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Default auth state: not authenticated – prevents SSE startup in init block
        coEvery { authRepository.isAuthenticated() } returns false
        // Collaborator check returns empty list so SSE is not started
        coEvery { collaboratorRepository.getCollaborators(any()) } returns Result.success(emptyList<UserInfo>())

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
}
