package com.meneses.budgethunter.budgetList.data.sync

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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
            apiService.createBudget(CreateBudgetRequest("Budget 1", 1000.0))
        } returns Result.success(BudgetResponse(id = 101, name = "Budget 1", amount = 1000.0))

        coEvery {
            apiService.createBudget(CreateBudgetRequest("Budget 2", 2000.0))
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
            apiService.createBudget(CreateBudgetRequest("Budget 1", 1000.0))
        } returns Result.success(BudgetResponse(id = 101, name = "Budget 1", amount = 1000.0))
        every { localDataSource.markAsSynced(1, 101, any()) } returns Unit

        // Second budget fails
        val networkError = Exception("Network error")
        coEvery {
            apiService.createBudget(CreateBudgetRequest("Budget 2", 2000.0))
        } returns Result.failure(networkError)

        // Third budget succeeds
        coEvery {
            apiService.createBudget(CreateBudgetRequest("Budget 3", 3000.0))
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

    // ========== performFullSync() Tests ==========

    @Test
    fun `performFullSync successfully completes push and pull operations`() = runTest {
        // Given - User is authenticated
        coEvery { authRepository.isAuthenticated() } returns true

        // Setup push phase
        val localBudget = Budget(id = 1, name = "Local Budget", amount = 1000.0)
        every { localDataSource.getUnsynced() } returns listOf(localBudget)
        coEvery {
            apiService.createBudget(CreateBudgetRequest("Local Budget", 1000.0))
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
            apiService.createBudget(CreateBudgetRequest("Budget 1", 1000.0))
        } returns Result.success(BudgetResponse(id = 101, name = "Budget 1", amount = 1000.0))
        every { localDataSource.markAsSynced(1, 101, any()) } returns Unit

        val networkError = Exception("Network error")
        coEvery {
            apiService.createBudget(CreateBudgetRequest("Budget 2", 2000.0))
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
}
