package com.meneses.budgethunter.budgetMetrics.application

import com.meneses.budgethunter.budgetEntry.data.BudgetEntrySyncManager
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.network.BudgetEntryApiService
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.network.ApiError
import com.meneses.budgethunter.commons.data.network.models.CategorizeEntriesResponse
import com.meneses.budgethunter.commons.data.sync.SyncResult
import com.meneses.budgethunter.commons.data.sync.SyncStats
import com.meneses.budgethunter.settings.application.SyncUserPreferencesUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CategorizeEntriesUseCaseTest {

    private val entrySyncManager = mockk<BudgetEntrySyncManager>()
    private val apiService = mockk<BudgetEntryApiService>()
    private val localDataSource = mockk<BudgetEntryLocalDataSource>()
    private val syncUserPreferences = mockk<SyncUserPreferencesUseCase>(relaxed = true)
    private val budget = Budget(id = 1, serverId = 10L)

    private val useCase = CategorizeEntriesUseCase(
        entrySyncManager = entrySyncManager,
        budgetEntryApiService = apiService,
        budgetEntryLocalDataSource = localDataSource,
        syncUserPreferences = syncUserPreferences
    )

    @BeforeTest
    fun setup() {
        coEvery { entrySyncManager.syncPendingEntries(1) } returns SyncResult.Success(SyncStats(0))
        coEvery { entrySyncManager.pullEntriesFromServer(10L, 1) } returns SyncResult.Success(SyncStats(0))
        coEvery { localDataSource.selectAllByBudgetId(1L) } returns flowOf(emptyList())
        coEvery { apiService.categorizeEntries(10L) } returns
            Result.success(CategorizeEntriesResponse(categorized = 3, pending = 1))
    }

    @Test
    fun `pushes first, then categorizes, then pulls`() = runTest {
        val outcome = useCase.execute(budget)

        assertEquals(CategorizationOutcome.Done(categorized = 3, pending = 1), outcome)
        coVerifyOrder {
            entrySyncManager.syncPendingEntries(1)
            apiService.categorizeEntries(10L)
            entrySyncManager.pullEntriesFromServer(10L, 1)
        }
    }

    @Test
    fun `a push that fails stops before the server is called`() = runTest {
        val error = IllegalStateException("offline")
        coEvery { entrySyncManager.syncPendingEntries(1) } returns SyncResult.Failure(error)

        assertEquals(CategorizationOutcome.Failed(error), useCase.execute(budget))
        coVerify(exactly = 0) { apiService.categorizeEntries(any()) }
    }

    @Test
    fun `some entries failing to push does not stop the run`() = runTest {
        coEvery { entrySyncManager.syncPendingEntries(1) } returns
            SyncResult.PartialSuccess(SyncStats(2, 1, 1), succeeded = 1, failed = 1, errors = emptyList())

        assertIs<CategorizationOutcome.Done>(useCase.execute(budget))
    }

    @Test
    fun `a budget that is not on the server fails without any call`() = runTest {
        val outcome = useCase.execute(Budget(id = 1, serverId = null))

        assertIs<CategorizationOutcome.Failed>(outcome)
        coVerify(exactly = 0) { entrySyncManager.syncPendingEntries(any()) }
    }

    @Test
    fun `a run already in flight is reported as such`() = runTest {
        coEvery { apiService.categorizeEntries(10L) } returns Result.failure(ApiError.Conflict)

        assertEquals(CategorizationOutcome.AlreadyRunning, useCase.execute(budget))
        coVerify(exactly = 0) { entrySyncManager.pullEntriesFromServer(any(), any()) }
    }

    @Test
    fun `a 403 saves the preferences and tries once more`() = runTest {
        coEvery { apiService.categorizeEntries(10L) } returnsMany listOf(
            Result.failure(ApiError.Forbidden),
            Result.success(CategorizeEntriesResponse(categorized = 2, pending = 0))
        )

        assertEquals(CategorizationOutcome.Done(2, 0), useCase.execute(budget))
        coVerify(exactly = 1) { syncUserPreferences.push() }
        coVerify(exactly = 2) { apiService.categorizeEntries(10L) }
    }

    @Test
    fun `a 403 that persists means AI processing is off for the account`() = runTest {
        coEvery { apiService.categorizeEntries(10L) } returns Result.failure(ApiError.Forbidden)

        assertEquals(CategorizationOutcome.AiOff, useCase.execute(budget))
        coVerify(exactly = 1) { syncUserPreferences.push() }
        coVerify(exactly = 2) { apiService.categorizeEntries(10L) }
    }

    @Test
    fun `any other error is reported as a failure`() = runTest {
        coEvery { apiService.categorizeEntries(10L) } returns Result.failure(ApiError.Timeout)

        assertEquals(CategorizationOutcome.Failed(ApiError.Timeout), useCase.execute(budget))
        coVerify(exactly = 0) { syncUserPreferences.push() }
    }

    @Test
    fun `a pull that fails after categorizing is a failure`() = runTest {
        val error = IllegalStateException("lost connection")
        coEvery { entrySyncManager.pullEntriesFromServer(10L, 1) } returns SyncResult.Failure(error)

        assertEquals(CategorizationOutcome.Failed(error), useCase.execute(budget))
    }
}
