package com.meneses.budgethunter.budgetList.data.sync

import com.meneses.budgethunter.budgetEntry.data.BudgetEntrySyncManager
import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.data.network.BudgetApiService
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.network.models.BudgetResponse
import com.meneses.budgethunter.commons.data.network.models.CreateBudgetRequest
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.data.sync.SyncException
import com.meneses.budgethunter.commons.data.sync.SyncResult
import com.meneses.budgethunter.commons.data.sync.SyncStats
import com.meneses.budgethunter.commons.util.today
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.justRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for BudgetSyncManager.
 * Tests all synchronization operations including push, pull, and full sync.
 */
class BudgetSyncManagerTest {

    // Mocks
    private val localDataSource = mockk<BudgetLocalDataSource>()
    private val apiService = mockk<BudgetApiService>()
    private val authRepository = mockk<AuthRepository>()
    private val logger = mockk<Logger>(relaxed = true)

    // System under test
    private val entrySyncManager = mockk<BudgetEntrySyncManager>()
    private lateinit var syncManager: BudgetSyncManager

    @BeforeTest
    fun setup() {
        coEvery { entrySyncManager.syncPendingEntries(any()) } returns SyncResult.Success(SyncStats(totalItems = 0))
        // Nothing synced locally by default, so the pull has nothing to prune.
        every { localDataSource.getSyncedServerIds() } returns emptySet()
        syncManager = BudgetSyncManager(
            localDataSource = localDataSource,
            budgetApiService = apiService,
            entrySyncManager = entrySyncManager,
            authRepository = authRepository,
            ioDispatcher = Dispatchers.Unconfined,
            logger = logger
        )
    }

    // ========== syncPendingBudgets() Tests ==========

    @Test
    fun `syncPendingBudgets returns failure when not authenticated`() = runTest {
        // Given - User is not authenticated
        coEvery { authRepository.isAuthenticated() } returns false

        // When
        val result = syncManager.syncPendingBudgets()

        // Then
        assertIs<SyncResult.Failure>(result)
        assertIs<SyncException.NotAuthenticated>(result.error)
        verify(exactly = 0) { localDataSource.getUnsynced() }
    }

    @Test
    fun `syncPendingBudgets returns success with zero items when list is empty`() = runTest {
        // Given - User is authenticated and no unsynced budgets
        coEvery { authRepository.isAuthenticated() } returns true
        every { localDataSource.getUnsynced() } returns emptyList()

        // When
        val result = syncManager.syncPendingBudgets()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(0, result.data.totalItems)
        assertEquals(0, result.data.syncedItems)
        assertEquals(0, result.data.failedItems)
    }

    @Test
    fun `syncPendingBudgets successfully syncs all budgets`() = runTest {
        // Given - User is authenticated with unsynced budgets
        coEvery { authRepository.isAuthenticated() } returns true

        val budget1 = Budget(id = 1, name = "Budget 1", amount = 1000.0)
        val budget2 = Budget(id = 2, name = "Budget 2", amount = 2000.0)
        every { localDataSource.getUnsynced() } returns listOf(budget1, budget2)

        // API calls succeed
        coEvery {
            apiService.createBudget(CreateBudgetRequest("Budget 1", 1000.0, date = budget1.date))
        } returns Result.success(BudgetResponse(id = 101, name = "Budget 1", amount = 1000.0))

        coEvery {
            apiService.createBudget(CreateBudgetRequest("Budget 2", 2000.0, date = budget2.date))
        } returns Result.success(BudgetResponse(id = 102, name = "Budget 2", amount = 2000.0))

        // Local updates succeed
        every { localDataSource.markAsSynced(1, 101, any()) } returns Unit
        every { localDataSource.markAsSynced(2, 102, any()) } returns Unit

        // When
        val result = syncManager.syncPendingBudgets()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(2, result.data.totalItems)
        assertEquals(2, result.data.syncedItems)
        assertEquals(0, result.data.failedItems)
        assertTrue(result.data.errors.isEmpty())

        // Verify both budgets were marked as synced
        verify { localDataSource.markAsSynced(1, 101, any()) }
        verify { localDataSource.markAsSynced(2, 102, any()) }
    }

    @Test
    fun `syncPendingBudgets returns partial success when some budgets fail`() = runTest {
        // Given - User is authenticated with unsynced budgets
        coEvery { authRepository.isAuthenticated() } returns true

        val budget1 = Budget(id = 1, name = "Budget 1", amount = 1000.0)
        val budget2 = Budget(id = 2, name = "Budget 2", amount = 2000.0)
        val budget3 = Budget(id = 3, name = "Budget 3", amount = 3000.0)
        every { localDataSource.getUnsynced() } returns listOf(budget1, budget2, budget3)

        // First budget succeeds
        coEvery {
            apiService.createBudget(CreateBudgetRequest("Budget 1", 1000.0, date = budget1.date))
        } returns Result.success(BudgetResponse(id = 101, name = "Budget 1", amount = 1000.0))
        every { localDataSource.markAsSynced(1, 101, any()) } returns Unit

        // Second budget fails
        val networkError = Exception("Network error")
        coEvery {
            apiService.createBudget(CreateBudgetRequest("Budget 2", 2000.0, date = budget2.date))
        } returns Result.failure(networkError)

        // Third budget succeeds
        coEvery {
            apiService.createBudget(CreateBudgetRequest("Budget 3", 3000.0, date = budget3.date))
        } returns Result.success(BudgetResponse(id = 103, name = "Budget 3", amount = 3000.0))
        every { localDataSource.markAsSynced(3, 103, any()) } returns Unit

        // When
        val result = syncManager.syncPendingBudgets()

        // Then
        assertIs<SyncResult.PartialSuccess<SyncStats>>(result)
        assertEquals(3, result.data.totalItems)
        assertEquals(2, result.data.syncedItems)
        assertEquals(1, result.data.failedItems)
        assertEquals(2, result.succeeded)
        assertEquals(1, result.failed)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors.first().itemIdentifier.contains("Budget 2"))
    }

    @Test
    fun `syncPendingBudgets returns failure when all budgets fail`() = runTest {
        // Given - User is authenticated with unsynced budgets
        coEvery { authRepository.isAuthenticated() } returns true

        val budget1 = Budget(id = 1, name = "Budget 1", amount = 1000.0)
        val budget2 = Budget(id = 2, name = "Budget 2", amount = 2000.0)
        every { localDataSource.getUnsynced() } returns listOf(budget1, budget2)

        // All API calls fail
        val networkError = Exception("Network error")
        coEvery {
            apiService.createBudget(any())
        } returns Result.failure(networkError)

        // When
        val result = syncManager.syncPendingBudgets()

        // Then
        assertIs<SyncResult.Failure>(result)
        assertTrue(result.error.message?.contains("Failed to sync all items") == true)

        // Verify no budgets were marked as synced
        verify(exactly = 0) { localDataSource.markAsSynced(any(), any(), any()) }
    }

    // ========== pullBudgetsFromServer() Tests ==========

    @Test
    fun `pullBudgetsFromServer returns failure when not authenticated`() = runTest {
        // Given - User is not authenticated
        coEvery { authRepository.isAuthenticated() } returns false

        // When
        val result = syncManager.pullBudgetsFromServer()

        // Then
        assertIs<SyncResult.Failure>(result)
        assertIs<SyncException.NotAuthenticated>(result.error)
        coVerify(exactly = 0) { apiService.getBudgets() }
    }

    @Test
    fun `pullBudgetsFromServer returns success with zero items when server returns empty list`() = runTest {
        // Given - User is authenticated and server returns no budgets
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { apiService.getBudgets() } returns Result.success(emptyList())

        // When
        val result = syncManager.pullBudgetsFromServer()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(0, result.data.totalItems)
        assertEquals(0, result.data.syncedItems)
    }

    @Test
    fun `pullBudgetsFromServer successfully pulls and merges new budgets`() = runTest {
        // Given - User is authenticated
        coEvery { authRepository.isAuthenticated() } returns true

        val serverBudget1 = BudgetResponse(id = 101, name = "Server Budget 1", amount = 1500.0)
        val serverBudget2 = BudgetResponse(id = 102, name = "Server Budget 2", amount = 2500.0)
        coEvery { apiService.getBudgets() } returns Result.success(listOf(serverBudget1, serverBudget2))

        // Both budgets don't exist locally
        every { localDataSource.getByServerId(101) } returns null
        every { localDataSource.getByServerId(102) } returns null

        // Creating new budgets succeeds
        every { localDataSource.create(any()) } returns Budget()

        // When
        val result = syncManager.pullBudgetsFromServer()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(2, result.data.totalItems)
        assertEquals(2, result.data.syncedItems)
        assertEquals(0, result.data.failedItems)

        // Verify budgets were created
        verify(exactly = 2) { localDataSource.create(any()) }
    }

    @Test
    fun `pullBudgetsFromServer updates existing budgets`() = runTest {
        // Given - User is authenticated
        coEvery { authRepository.isAuthenticated() } returns true

        val serverBudget = BudgetResponse(id = 101, name = "Updated Name", amount = 1500.0)
        coEvery { apiService.getBudgets() } returns Result.success(listOf(serverBudget))

        // Budget exists locally with different values
        val existingBudget = Budget(
            id = 1,
            name = "Old Name",
            amount = 1000.0,
            serverId = 101,
            isSynced = true
        )
        every { localDataSource.getByServerId(101) } returns existingBudget

        // Updating budget succeeds
        every { localDataSource.update(any()) } returns Unit

        // When
        val result = syncManager.pullBudgetsFromServer()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(1, result.data.totalItems)
        assertEquals(1, result.data.syncedItems)

        // Verify budget was updated with new values
        verify {
            localDataSource.update(
                match { budget ->
                    budget.id == 1 &&
                        budget.name == "Updated Name" &&
                        budget.amount == 1500.0 &&
                        budget.serverId == 101L &&
                        budget.isSynced
                }
            )
        }
    }

    @Test
    fun `pullBudgetsFromServer returns failure when API call fails`() = runTest {
        // Given - User is authenticated but API fails
        coEvery { authRepository.isAuthenticated() } returns true
        val serverError = Exception("Server error")
        coEvery { apiService.getBudgets() } returns Result.failure(serverError)

        // When
        val result = syncManager.pullBudgetsFromServer()

        // Then
        assertIs<SyncResult.Failure>(result)
        assertEquals("Server error", result.error.message)

        // Verify no local operations were performed
        verify(exactly = 0) { localDataSource.create(any()) }
        verify(exactly = 0) { localDataSource.update(any()) }
    }

    // ========== pullBudgetsFromServer() pruning Tests ==========

    @Test
    fun `pullBudgetsFromServer deletes local synced budgets the server no longer returns`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns true
        every { localDataSource.getSyncedServerIds() } returns setOf(1L, 2L, 3L)
        coEvery { apiService.getBudgets() } returns
            Result.success(listOf(BudgetResponse(id = 2, name = "B", amount = 1.0)))
        every { localDataSource.getByServerId(2) } returns
            Budget(id = 5, name = "B", amount = 1.0, serverId = 2, isSynced = true)
        justRun { localDataSource.update(any()) }
        justRun { localDataSource.deleteByServerIds(any()) }

        syncManager.pullBudgetsFromServer()

        verify(exactly = 1) { localDataSource.deleteByServerIds(setOf(1L, 3L)) }
    }

    @Test
    fun `pullBudgetsFromServer does not prune when the server returns an empty list`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns true
        every { localDataSource.getSyncedServerIds() } returns setOf(1L, 2L)
        coEvery { apiService.getBudgets() } returns Result.success(emptyList())
        justRun { localDataSource.deleteByServerIds(any()) }

        val result = syncManager.pullBudgetsFromServer()

        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(0, result.data.totalItems)
        verify(exactly = 0) { localDataSource.deleteByServerIds(any()) }
    }

    @Test
    fun `pullBudgetsFromServer does not delete when the server returns every local synced budget`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns true
        every { localDataSource.getSyncedServerIds() } returns setOf(1L)
        coEvery { apiService.getBudgets() } returns
            Result.success(listOf(BudgetResponse(id = 1, name = "A", amount = 1.0)))
        every { localDataSource.getByServerId(1) } returns
            Budget(id = 5, name = "A", amount = 1.0, serverId = 1, isSynced = true)
        justRun { localDataSource.update(any()) }

        syncManager.pullBudgetsFromServer()

        verify(exactly = 0) { localDataSource.deleteByServerIds(any()) }
    }

    @Test
    fun `pullBudgetsFromServer does not delete when there are no local synced budgets`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns true
        every { localDataSource.getSyncedServerIds() } returns emptySet()
        coEvery { apiService.getBudgets() } returns Result.success(emptyList())

        syncManager.pullBudgetsFromServer()

        verify(exactly = 0) { localDataSource.deleteByServerIds(any()) }
    }

    @Test
    fun `pullBudgetsFromServer does not delete anything when fetching fails`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns true
        every { localDataSource.getSyncedServerIds() } returns setOf(1L, 2L)
        coEvery { apiService.getBudgets() } returns Result.failure(Exception("Server error"))

        val result = syncManager.pullBudgetsFromServer()

        assertIs<SyncResult.Failure>(result)
        verify(exactly = 0) { localDataSource.deleteByServerIds(any()) }
    }

    @Test
    fun `pullBudgetsFromServer does not delete anything when not authenticated`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns false

        syncManager.pullBudgetsFromServer()

        verify(exactly = 0) { localDataSource.deleteByServerIds(any()) }
        verify(exactly = 0) { localDataSource.deleteSynced() }
    }

    @Test
    fun `pullBudgetsFromServer snapshots synced ids before fetching from the server`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns true
        every { localDataSource.getSyncedServerIds() } returns setOf(1L, 2L)
        coEvery { apiService.getBudgets() } returns
            Result.success(listOf(BudgetResponse(id = 2, name = "B", amount = 1.0)))
        every { localDataSource.getByServerId(2) } returns
            Budget(id = 5, name = "B", amount = 1.0, serverId = 2, isSynced = true)
        justRun { localDataSource.update(any()) }
        justRun { localDataSource.deleteByServerIds(any()) }

        syncManager.pullBudgetsFromServer()

        coVerifyOrder {
            localDataSource.getSyncedServerIds()
            apiService.getBudgets()
            localDataSource.deleteByServerIds(setOf(1L))
        }
    }

    @Test
    fun `pullBudgetsFromServer keeps a budget pushed after the snapshot was taken`() = runTest {
        // Given - budget 7 got its server id while the fetch was in flight, so it is not in the snapshot
        coEvery { authRepository.isAuthenticated() } returns true
        every { localDataSource.getSyncedServerIds() } returns setOf(1L, 2L)
        coEvery { apiService.getBudgets() } returns
            Result.success(listOf(BudgetResponse(id = 2, name = "B", amount = 1.0)))
        every { localDataSource.getByServerId(2) } returns
            Budget(id = 5, name = "B", amount = 1.0, serverId = 2, isSynced = true)
        justRun { localDataSource.update(any()) }
        justRun { localDataSource.deleteByServerIds(any()) }

        syncManager.pullBudgetsFromServer()

        // Then - only snapshot ids are candidates
        verify(exactly = 0) { localDataSource.deleteByServerIds(match { 7L in it }) }
        verify(exactly = 1) { localDataSource.deleteByServerIds(setOf(1L)) }
    }

    @Test
    fun `pullBudgetsFromServer still merges returned budgets while pruning`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns true
        every { localDataSource.getSyncedServerIds() } returns setOf(1L)
        coEvery { apiService.getBudgets() } returns
            Result.success(listOf(BudgetResponse(id = 101, name = "New", amount = 5.0)))
        every { localDataSource.getByServerId(101) } returns null
        every { localDataSource.create(any()) } returns Budget()
        justRun { localDataSource.deleteByServerIds(any()) }

        val result = syncManager.pullBudgetsFromServer()

        assertIs<SyncResult.Success<SyncStats>>(result)
        verify(exactly = 1) { localDataSource.create(any()) }
        verify(exactly = 1) { localDataSource.deleteByServerIds(setOf(1L)) }
    }

    // ========== performFullSync() Tests ==========

    @Test
    fun `performFullSync successfully completes push and pull operations`() = runTest {
        // Given - User is authenticated
        coEvery { authRepository.isAuthenticated() } returns true

        // Setup push phase
        val localBudget = Budget(id = 1, name = "Local Budget", amount = 1000.0)
        every { localDataSource.getUnsynced() } returns listOf(localBudget)
        coEvery {
            apiService.createBudget(CreateBudgetRequest("Local Budget", 1000.0, date = localBudget.date))
        } returns Result.success(BudgetResponse(id = 101, name = "Local Budget", amount = 1000.0))
        every { localDataSource.markAsSynced(1, 101, any()) } returns Unit

        // Setup pull phase
        val serverBudget = BudgetResponse(id = 102, name = "Server Budget", amount = 2000.0)
        coEvery { apiService.getBudgets() } returns Result.success(listOf(serverBudget))
        every { localDataSource.getByServerId(102) } returns null
        every { localDataSource.create(any()) } returns Budget()

        // When
        val result = syncManager.performFullSync()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(2, result.data.totalItems) // 1 pushed + 1 pulled
        assertEquals(2, result.data.syncedItems)
        assertEquals(0, result.data.failedItems)

        // Verify both operations occurred
        verify { localDataSource.markAsSynced(1, 101, any()) }
        verify { localDataSource.create(any()) }
    }

    @Test
    fun `performFullSync returns failure when push phase fails completely`() = runTest {
        // Given - User is authenticated
        coEvery { authRepository.isAuthenticated() } returns false

        // When
        val result = syncManager.performFullSync()

        // Then
        assertIs<SyncResult.Failure>(result)
        assertIs<SyncException.NotAuthenticated>(result.error)

        // Verify pull phase was not attempted
        coVerify(exactly = 0) { apiService.getBudgets() }
    }

    @Test
    fun `performFullSync returns failure when pull phase fails completely`() = runTest {
        // Given - User is authenticated and push succeeds
        coEvery { authRepository.isAuthenticated() } returns true

        // Push phase succeeds (no items to sync)
        every { localDataSource.getUnsynced() } returns emptyList()

        // Pull phase fails
        val serverError = Exception("Server error")
        coEvery { apiService.getBudgets() } returns Result.failure(serverError)

        // When
        val result = syncManager.performFullSync()

        // Then
        assertIs<SyncResult.Failure>(result)
        assertEquals("Server error", result.error.message)
    }

    @Test
    fun `performFullSync aggregates stats from partial successes`() = runTest {
        // Given - User is authenticated
        coEvery { authRepository.isAuthenticated() } returns true

        // Push phase: partial success (1 succeeds, 1 fails)
        val budget1 = Budget(id = 1, name = "Budget 1", amount = 1000.0)
        val budget2 = Budget(id = 2, name = "Budget 2", amount = 2000.0)
        every { localDataSource.getUnsynced() } returns listOf(budget1, budget2)

        coEvery {
            apiService.createBudget(CreateBudgetRequest("Budget 1", 1000.0, date = budget1.date))
        } returns Result.success(BudgetResponse(id = 101, name = "Budget 1", amount = 1000.0))
        every { localDataSource.markAsSynced(1, 101, any()) } returns Unit

        val networkError = Exception("Network error")
        coEvery {
            apiService.createBudget(CreateBudgetRequest("Budget 2", 2000.0, date = budget2.date))
        } returns Result.failure(networkError)

        // Pull phase: success
        val serverBudget = BudgetResponse(id = 102, name = "Server Budget", amount = 3000.0)
        coEvery { apiService.getBudgets() } returns Result.success(listOf(serverBudget))
        every { localDataSource.getByServerId(102) } returns null
        every { localDataSource.create(any()) } returns Budget()

        // When
        val result = syncManager.performFullSync()

        // Then
        assertIs<SyncResult.PartialSuccess<SyncStats>>(result)
        assertEquals(3, result.data.totalItems) // 2 pushed + 1 pulled
        assertEquals(2, result.data.syncedItems) // 1 pushed + 1 pulled
        assertEquals(1, result.data.failedItems) // 1 failed push
        assertEquals(1, result.data.errors.size)
    }

    // ========== Budget date field push/pull tests ==========

    @Test
    fun `syncSingleBudget sends date when creating a new budget`() = runTest {
        // Given - User is authenticated with an unsynced new budget
        coEvery { authRepository.isAuthenticated() } returns true

        val budget = Budget(id = 1, name = "Budget 1", amount = 1000.0, date = "2026-01-15")
        every { localDataSource.getUnsynced() } returns listOf(budget)

        coEvery {
            apiService.createBudget(CreateBudgetRequest("Budget 1", 1000.0, date = "2026-01-15"))
        } returns Result.success(BudgetResponse(id = 101, name = "Budget 1", amount = 1000.0, date = "2026-01-15"))
        every { localDataSource.markAsSynced(1, 101, any()) } returns Unit

        // When
        val result = syncManager.syncPendingBudgets()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        coVerify {
            apiService.createBudget(CreateBudgetRequest("Budget 1", 1000.0, date = "2026-01-15"))
        }
    }

    @Test
    fun `syncSingleBudget sends date when updating an existing budget`() = runTest {
        // Given - User is authenticated with an unsynced budget that already has a serverId
        coEvery { authRepository.isAuthenticated() } returns true

        val budget = Budget(
            id = 1,
            name = "Budget 1",
            amount = 1000.0,
            date = "2026-02-20",
            serverId = 101
        )
        every { localDataSource.getUnsynced() } returns listOf(budget)

        coEvery {
            apiService.updateBudget(101, CreateBudgetRequest("Budget 1", 1000.0, date = "2026-02-20"))
        } returns Result.success(BudgetResponse(id = 101, name = "Budget 1", amount = 1000.0, date = "2026-02-20"))
        every { localDataSource.markAsSynced(1, 101, any()) } returns Unit

        // When
        val result = syncManager.syncPendingBudgets()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        coVerify {
            apiService.updateBudget(101, CreateBudgetRequest("Budget 1", 1000.0, date = "2026-02-20"))
        }
    }

    @Test
    fun `pullBudgetsFromServer stores server date for a brand new budget`() = runTest {
        // Given - Server returns a budget with a date, not present locally
        coEvery { authRepository.isAuthenticated() } returns true

        val serverBudget = BudgetResponse(id = 101, name = "Server Budget", amount = 1500.0, date = "2026-03-10")
        coEvery { apiService.getBudgets() } returns Result.success(listOf(serverBudget))
        every { localDataSource.getByServerId(101) } returns null
        every { localDataSource.create(any()) } returns Budget()

        // When
        val result = syncManager.pullBudgetsFromServer()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        verify {
            localDataSource.create(match { it.date == "2026-03-10" && it.serverId == 101L })
        }
    }

    @Test
    fun `pullBudgetsFromServer falls back to today for a new budget with no server date`() = runTest {
        // Given - Server returns a legacy budget with no date (older backend)
        coEvery { authRepository.isAuthenticated() } returns true

        val serverBudget = BudgetResponse(id = 101, name = "Legacy Budget", amount = 1500.0, date = null)
        coEvery { apiService.getBudgets() } returns Result.success(listOf(serverBudget))
        every { localDataSource.getByServerId(101) } returns null
        every { localDataSource.create(any()) } returns Budget()

        // When
        val result = syncManager.pullBudgetsFromServer()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        verify {
            localDataSource.create(match { it.date == today() })
        }
    }

    @Test
    fun `pullBudgetsFromServer adopts server date for an existing synced budget`() = runTest {
        // Given - Existing local budget is already synced, so the server's date wins
        coEvery { authRepository.isAuthenticated() } returns true

        val serverBudget = BudgetResponse(id = 101, name = "Updated Name", amount = 1500.0, date = "2026-05-01")
        coEvery { apiService.getBudgets() } returns Result.success(listOf(serverBudget))

        val existingBudget = Budget(
            id = 1,
            name = "Old Name",
            amount = 1000.0,
            date = "2026-01-01",
            serverId = 101,
            isSynced = true
        )
        every { localDataSource.getByServerId(101) } returns existingBudget
        every { localDataSource.update(any()) } returns Unit

        // When
        val result = syncManager.pullBudgetsFromServer()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        verify {
            localDataSource.update(match { it.date == "2026-05-01" })
        }
    }

    @Test
    fun `pullBudgetsFromServer keeps local date for a synced existing budget when server date is null`() = runTest {
        // Given - Existing, synced local budget; server has no date (legacy row)
        coEvery { authRepository.isAuthenticated() } returns true

        val serverBudget = BudgetResponse(id = 101, name = "Updated Name", amount = 1500.0, date = null)
        coEvery { apiService.getBudgets() } returns Result.success(listOf(serverBudget))

        val existingBudget = Budget(
            id = 1,
            name = "Old Name",
            amount = 1000.0,
            date = "2026-01-01",
            serverId = 101,
            isSynced = true
        )
        every { localDataSource.getByServerId(101) } returns existingBudget
        every { localDataSource.update(any()) } returns Unit

        // When
        val result = syncManager.pullBudgetsFromServer()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        verify {
            localDataSource.update(match { it.date == "2026-01-01" })
        }
    }

    @Test
    fun `pullBudgetsFromServer keeps local date for an unsynced existing budget even when server sends a different date`() = runTest {
        // Given - Local budget hasn't been pushed yet, so it's considered newer than the server's copy
        coEvery { authRepository.isAuthenticated() } returns true

        val serverBudget = BudgetResponse(id = 101, name = "Updated Name", amount = 1500.0, date = "2026-05-01")
        coEvery { apiService.getBudgets() } returns Result.success(listOf(serverBudget))

        val existingBudget = Budget(
            id = 1,
            name = "Old Name",
            amount = 1000.0,
            date = "2026-01-01",
            serverId = 101,
            isSynced = false
        )
        every { localDataSource.getByServerId(101) } returns existingBudget
        every { localDataSource.update(any()) } returns Unit

        // When
        val result = syncManager.pullBudgetsFromServer()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        verify {
            localDataSource.update(match { it.date == "2026-01-01" })
        }
    }

    @Test
    fun `pullBudgetsFromServer merges server budgets in ascending server id order`() = runTest {
        // Given - Server returns budgets out of order by id
        coEvery { authRepository.isAuthenticated() } returns true

        val serverBudgetHigh = BudgetResponse(id = 103, name = "Budget C", amount = 3000.0)
        val serverBudgetLow = BudgetResponse(id = 101, name = "Budget A", amount = 1000.0)
        val serverBudgetMid = BudgetResponse(id = 102, name = "Budget B", amount = 2000.0)
        coEvery { apiService.getBudgets() } returns Result.success(
            listOf(serverBudgetHigh, serverBudgetLow, serverBudgetMid)
        )

        every { localDataSource.getByServerId(any()) } returns null
        every { localDataSource.create(any()) } returns Budget()

        // When
        val result = syncManager.pullBudgetsFromServer()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        verifyOrder {
            localDataSource.create(match { it.serverId == 101L })
            localDataSource.create(match { it.serverId == 102L })
            localDataSource.create(match { it.serverId == 103L })
        }
    }

    // ========== Entry push after budget create ==========

    private val entryBudgetResponse = BudgetResponse(id = 101, name = "Budget 1", amount = 1000.0)

    @Test
    fun `syncSingleBudget pushes pending entries after creating a budget on the server`() = runTest {
        // Given
        coEvery { authRepository.isAuthenticated() } returns true
        val budget = Budget(id = 1, name = "Budget 1", amount = 1000.0)
        every { localDataSource.getUnsynced() } returns listOf(budget)
        coEvery { apiService.createBudget(any()) } returns Result.success(entryBudgetResponse)
        every { localDataSource.markAsSynced(any(), any(), any()) } returns Unit

        // When
        syncManager.syncPendingBudgets()

        // Then - entries are pushed after the budget got its serverId
        coVerify(exactly = 1) { entrySyncManager.syncPendingEntries(1) }
        io.mockk.coVerifyOrder {
            localDataSource.markAsSynced(1, 101, any())
            entrySyncManager.syncPendingEntries(1)
        }
    }

    @Test
    fun `syncSingleBudget does not push entries when updating a budget that already has a serverId`() = runTest {
        // Given
        coEvery { authRepository.isAuthenticated() } returns true
        val budget = Budget(id = 1, name = "Budget 1", amount = 1000.0, serverId = 101)
        every { localDataSource.getUnsynced() } returns listOf(budget)
        coEvery { apiService.updateBudget(101, any()) } returns Result.success(entryBudgetResponse)
        every { localDataSource.markAsSynced(any(), any(), any()) } returns Unit

        // When
        syncManager.syncPendingBudgets()

        // Then
        coVerify(exactly = 0) { entrySyncManager.syncPendingEntries(any()) }
    }

    @Test
    fun `syncSingleBudget does not push entries when the create call fails`() = runTest {
        // Given
        coEvery { authRepository.isAuthenticated() } returns true
        val budget = Budget(id = 1, name = "Budget 1", amount = 1000.0)
        every { localDataSource.getUnsynced() } returns listOf(budget)
        coEvery { apiService.createBudget(any()) } returns Result.failure(Exception("Network error"))

        // When
        syncManager.syncPendingBudgets()

        // Then
        coVerify(exactly = 0) { entrySyncManager.syncPendingEntries(any()) }
    }

    @Test
    fun `syncSingleBudget still succeeds when the entry push returns failure`() = runTest {
        // Given
        coEvery { authRepository.isAuthenticated() } returns true
        val budget = Budget(id = 1, name = "Budget 1", amount = 1000.0)
        every { localDataSource.getUnsynced() } returns listOf(budget)
        coEvery { apiService.createBudget(any()) } returns Result.success(entryBudgetResponse)
        every { localDataSource.markAsSynced(any(), any(), any()) } returns Unit
        coEvery { entrySyncManager.syncPendingEntries(1) } returns SyncResult.Failure(Exception("boom"))

        // When
        val result = syncManager.syncPendingBudgets()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(1, result.data.syncedItems)
        assertEquals(0, result.data.failedItems)
    }

    @Test
    fun `syncSingleBudget still succeeds when the entry push throws`() = runTest {
        // Given
        coEvery { authRepository.isAuthenticated() } returns true
        val budget = Budget(id = 1, name = "Budget 1", amount = 1000.0)
        every { localDataSource.getUnsynced() } returns listOf(budget)
        coEvery { apiService.createBudget(any()) } returns Result.success(entryBudgetResponse)
        every { localDataSource.markAsSynced(any(), any(), any()) } returns Unit
        coEvery { entrySyncManager.syncPendingEntries(1) } throws RuntimeException("boom")

        // When
        val result = syncManager.syncPendingBudgets()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(1, result.data.syncedItems)
        assertEquals(0, result.data.failedItems)
    }
}
