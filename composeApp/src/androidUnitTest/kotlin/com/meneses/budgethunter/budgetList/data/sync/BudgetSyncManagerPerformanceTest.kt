package com.meneses.budgethunter.budgetList.data.sync

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.data.network.BudgetApiService
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.network.models.BudgetResponse
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.data.sync.SyncResult
import com.meneses.budgethunter.commons.data.sync.SyncStats
import com.meneses.budgethunter.utils.TestDataFactory
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.system.measureTimeMillis
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Performance / load tests for [BudgetSyncManager].
 * Verifies that sync operations complete within acceptable time bounds
 * when processing large volumes of budgets.
 *
 * All tests use [Dispatchers.Unconfined] to avoid introducing real threading overhead,
 * so the elapsed time measured by [measureTimeMillis] reflects business-logic cost rather
 * than thread-scheduling latency.
 */
class BudgetSyncManagerPerformanceTest {

    // Mocks
    private val localDataSource = mockk<BudgetLocalDataSource>()
    private val apiService = mockk<BudgetApiService>()
    private val authRepository = mockk<AuthRepository>()
    private val logger = mockk<Logger>(relaxed = true)

    // System under test
    private lateinit var syncManager: BudgetSyncManager

    @BeforeTest
    fun setup() {
        syncManager = BudgetSyncManager(
            localDataSource = localDataSource,
            budgetApiService = apiService,
            authRepository = authRepository,
            ioDispatcher = Dispatchers.Unconfined,
            logger = logger
        )
    }

    // ========== syncPendingBudgets() Performance Tests ==========

    @Test
    fun `syncPendingBudgets with 100 unsynced budgets pushes all successfully`() = runTest {
        // Given - User is authenticated with 100 unsynced budgets
        coEvery { authRepository.isAuthenticated() } returns true

        val unsyncedBudgets = TestDataFactory.budgets(count = 100, synced = false)
        every { localDataSource.getUnsynced() } returns unsyncedBudgets

        // All API create calls succeed with a representative response
        coEvery { apiService.createBudget(any()) } returns Result.success(
            BudgetResponse(id = 101L, name = "Budget", amount = 1000.0)
        )
        every { localDataSource.markAsSynced(any(), any(), any()) } returns Unit

        // When
        var result: SyncResult<SyncStats>
        val elapsed = measureTimeMillis {
            result = syncManager.syncPendingBudgets()
        }

        // Then - result correctness
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(100, result.data.totalItems)
        assertEquals(100, result.data.syncedItems)
        assertEquals(0, result.data.failedItems)

        // Then - performance constraint
        assertTrue(elapsed < 3000, "Expected completion in <3000ms but took ${elapsed}ms")

        // Then - every budget was marked as synced
        verify(exactly = 100) { localDataSource.markAsSynced(any(), any(), any()) }
    }

    // ========== pullBudgetsFromServer() Performance Tests ==========

    @Test
    fun `pullBudgetsFromServer with 100 server budgets creates all locally`() = runTest {
        // Given - User is authenticated; server returns 100 budgets, all new locally
        coEvery { authRepository.isAuthenticated() } returns true

        val serverBudgets = TestDataFactory.budgetResponses(count = 100)
        coEvery { apiService.getBudgets() } returns Result.success(serverBudgets)

        every { localDataSource.getByServerId(any()) } returns null
        every { localDataSource.create(any()) } returns Budget()

        // When
        var result: SyncResult<SyncStats>
        val elapsed = measureTimeMillis {
            result = syncManager.pullBudgetsFromServer()
        }

        // Then - result correctness
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(100, result.data.totalItems)
        assertEquals(100, result.data.syncedItems)
        assertEquals(0, result.data.failedItems)

        // Then - performance constraint
        assertTrue(elapsed < 3000, "Expected completion in <3000ms but took ${elapsed}ms")

        // Then - a local record was created for every server budget
        verify(exactly = 100) { localDataSource.create(any()) }
    }

    // ========== performFullSync() Performance Tests ==========

    @Test
    fun `performFullSync with 50 local plus 50 server budgets accounts for all 100`() = runTest {
        // Given - User is authenticated
        coEvery { authRepository.isAuthenticated() } returns true

        // Push side: 50 unsynced local budgets
        val unsyncedBudgets = TestDataFactory.budgets(count = 50, synced = false)
        every { localDataSource.getUnsynced() } returns unsyncedBudgets

        // All API create calls succeed with a representative response
        coEvery { apiService.createBudget(any()) } returns Result.success(
            BudgetResponse(id = 101L, name = "Budget", amount = 1000.0)
        )
        every { localDataSource.markAsSynced(any(), any(), any()) } returns Unit

        // Pull side: 50 new server budgets
        val serverBudgets = TestDataFactory.budgetResponses(count = 50)
        coEvery { apiService.getBudgets() } returns Result.success(serverBudgets)
        every { localDataSource.getByServerId(any()) } returns null
        every { localDataSource.create(any()) } returns Budget()

        // When
        var result: SyncResult<SyncStats>
        val elapsed = measureTimeMillis {
            result = syncManager.performFullSync()
        }

        // Then - result correctness (50 pushed + 50 pulled = 100 total)
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(100, result.data.totalItems)
        assertEquals(100, result.data.syncedItems)
        assertEquals(0, result.data.failedItems)

        // Then - performance constraint
        assertTrue(elapsed < 5000, "Expected completion in <5000ms but took ${elapsed}ms")
    }
}
