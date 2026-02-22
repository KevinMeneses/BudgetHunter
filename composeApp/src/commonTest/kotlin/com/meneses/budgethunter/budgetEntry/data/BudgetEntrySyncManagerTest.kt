package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.network.BudgetEntryApiService
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.network.models.BudgetEntryResponse
import com.meneses.budgethunter.commons.data.network.models.CreateBudgetEntryRequest
import com.meneses.budgethunter.commons.data.network.models.UpdateBudgetEntryRequest
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.data.sync.SyncException
import com.meneses.budgethunter.commons.data.sync.SyncResult
import com.meneses.budgethunter.commons.data.sync.SyncStats
import com.meneses.budgethunter.db.Budget_entry
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
 * Comprehensive unit tests for BudgetEntrySyncManager.
 * Tests all synchronization operations including push, pull, and full sync for budget entries.
 */
class BudgetEntrySyncManagerTest {

    // Mocks
    private val entryLocalDataSource = mockk<BudgetEntryLocalDataSource>()
    private val budgetLocalDataSource = mockk<BudgetLocalDataSource>()
    private val apiService = mockk<BudgetEntryApiService>()
    private val authRepository = mockk<AuthRepository>()
    private val logger = mockk<Logger>(relaxed = true)

    // System under test
    private lateinit var syncManager: BudgetEntrySyncManager

    @BeforeTest
    fun setup() {
        syncManager = BudgetEntrySyncManager(
            localDataSource = entryLocalDataSource,
            budgetEntryApiService = apiService,
            budgetLocalDataSource = budgetLocalDataSource,
            authRepository = authRepository,
            ioDispatcher = Dispatchers.Unconfined,
            logger = logger
        )
    }

    // ========== syncPendingEntries() Tests ==========

    @Test
    fun `syncPendingEntries returns failure when not authenticated`() = runTest {
        // Given - User is not authenticated
        coEvery { authRepository.isAuthenticated() } returns false

        // When
        val result = syncManager.syncPendingEntries(budgetId = 1)

        // Then
        assertIs<SyncResult.Failure>(result)
        assertIs<SyncException.NotAuthenticated>(result.error)
        coVerify(exactly = 0) { budgetLocalDataSource.getById(any()) }
    }

    @Test
    fun `syncPendingEntries returns failure when parent budget not synced`() = runTest {
        // Given - User is authenticated but parent budget has no serverId
        coEvery { authRepository.isAuthenticated() } returns true
        val unsyncedBudget = Budget(id = 1, name = "Budget", amount = 1000.0, serverId = null)
        coEvery { budgetLocalDataSource.getById(1) } returns unsyncedBudget

        // When
        val result = syncManager.syncPendingEntries(budgetId = 1)

        // Then
        assertIs<SyncResult.Failure>(result)
        assertIs<SyncException.ParentNotSynced>(result.error)
        assertTrue(result.error.message?.contains("Budget 1 must be synced") == true)
        verify(exactly = 0) { entryLocalDataSource.getUnsynced(any()) }
    }

    @Test
    fun `syncPendingEntries returns success with zero items when list is empty`() = runTest {
        // Given - User is authenticated and parent budget is synced
        coEvery { authRepository.isAuthenticated() } returns true
        val syncedBudget = Budget(id = 1, name = "Budget", amount = 1000.0, serverId = 101)
        coEvery { budgetLocalDataSource.getById(1) } returns syncedBudget
        every { entryLocalDataSource.getUnsynced(1) } returns emptyList()

        // When
        val result = syncManager.syncPendingEntries(budgetId = 1)

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(0, result.data.totalItems)
        assertEquals(0, result.data.syncedItems)
    }

    @Test
    fun `syncPendingEntries successfully syncs all new entries`() = runTest {
        // Given - User is authenticated with synced parent budget
        coEvery { authRepository.isAuthenticated() } returns true
        val syncedBudget = Budget(id = 1, name = "Budget", amount = 1000.0, serverId = 101)
        coEvery { budgetLocalDataSource.getById(1) } returns syncedBudget

        val entry1 = BudgetEntry(
            id = 1,
            budgetId = 1,
            amount = "50.0",
            description = "Entry 1",
            serverId = null
        )
        val entry2 = BudgetEntry(
            id = 2,
            budgetId = 1,
            amount = "75.0",
            description = "Entry 2",
            serverId = null
        )
        every { entryLocalDataSource.getUnsynced(1) } returns listOf(entry1, entry2)

        // API calls succeed for creating new entries
        val response1 = BudgetEntryResponse(
            id = 201,
            budgetId = 101,
            amount = 50.0,
            description = "Entry 1",
            category = "OTHER",
            type = "OUTCOME",
            createdByEmail = "user@test.com",
            updatedByEmail = null,
            creationDate = "2024-01-01",
            modificationDate = "2024-01-01"
        )
        coEvery {
            apiService.createEntry(101, CreateBudgetEntryRequest(50.0, "Entry 1", "OTHER", "OUTCOME"))
        } returns Result.success(response1)

        val response2 = BudgetEntryResponse(
            id = 202,
            budgetId = 101,
            amount = 75.0,
            description = "Entry 2",
            category = "OTHER",
            type = "OUTCOME",
            createdByEmail = "user@test.com",
            updatedByEmail = null,
            creationDate = "2024-01-01",
            modificationDate = "2024-01-01"
        )
        coEvery {
            apiService.createEntry(101, CreateBudgetEntryRequest(75.0, "Entry 2", "OTHER", "OUTCOME"))
        } returns Result.success(response2)

        every { entryLocalDataSource.update(any()) } returns Unit

        // When
        val result = syncManager.syncPendingEntries(budgetId = 1)

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(2, result.data.totalItems)
        assertEquals(2, result.data.syncedItems)
        assertEquals(0, result.data.failedItems)

        // Verify entries were updated with server IDs
        verify(exactly = 2) { entryLocalDataSource.update(any()) }
    }

    @Test
    fun `syncPendingEntries successfully updates existing entries`() = runTest {
        // Given - User is authenticated with synced parent budget
        coEvery { authRepository.isAuthenticated() } returns true
        val syncedBudget = Budget(id = 1, name = "Budget", amount = 1000.0, serverId = 101)
        coEvery { budgetLocalDataSource.getById(1) } returns syncedBudget

        val entry = BudgetEntry(
            id = 1,
            budgetId = 1,
            amount = "60.0",
            description = "Updated Entry",
            serverId = 201 // Already has serverId (existing entry)
        )
        every { entryLocalDataSource.getUnsynced(1) } returns listOf(entry)

        // API call succeeds for updating entry
        val response = BudgetEntryResponse(
            id = 201,
            budgetId = 101,
            amount = 60.0,
            description = "Updated Entry",
            category = "OTHER",
            type = "OUTCOME",
            createdByEmail = "user@test.com",
            updatedByEmail = "user@test.com",
            creationDate = "2024-01-01",
            modificationDate = "2024-01-02"
        )
        coEvery {
            apiService.updateEntry(101, 201, UpdateBudgetEntryRequest(60.0, "Updated Entry", "OTHER", "OUTCOME"))
        } returns Result.success(response)

        every { entryLocalDataSource.update(any()) } returns Unit

        // When
        val result = syncManager.syncPendingEntries(budgetId = 1)

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(1, result.data.totalItems)
        assertEquals(1, result.data.syncedItems)
        verify { entryLocalDataSource.update(any()) }
    }

    @Test
    fun `syncPendingEntries returns partial success when some entries fail`() = runTest {
        // Given - User is authenticated with synced parent budget
        coEvery { authRepository.isAuthenticated() } returns true
        val syncedBudget = Budget(id = 1, name = "Budget", amount = 1000.0, serverId = 101)
        coEvery { budgetLocalDataSource.getById(1) } returns syncedBudget

        val entry1 = BudgetEntry(id = 1, budgetId = 1, amount = "50.0", description = "Entry 1", serverId = null)
        val entry2 = BudgetEntry(id = 2, budgetId = 1, amount = "75.0", description = "Entry 2", serverId = null)
        every { entryLocalDataSource.getUnsynced(1) } returns listOf(entry1, entry2)

        // First entry succeeds
        val response1 = BudgetEntryResponse(
            id = 201, budgetId = 101, amount = 50.0, description = "Entry 1",
            category = "OTHER", type = "OUTCOME", createdByEmail = "user@test.com",
            updatedByEmail = null, creationDate = "2024-01-01", modificationDate = "2024-01-01"
        )
        coEvery {
            apiService.createEntry(101, CreateBudgetEntryRequest(50.0, "Entry 1", "OTHER", "OUTCOME"))
        } returns Result.success(response1)
        every { entryLocalDataSource.update(match { it.id == 1 }) } returns Unit

        // Second entry fails
        val networkError = Exception("Network error")
        coEvery {
            apiService.createEntry(101, CreateBudgetEntryRequest(75.0, "Entry 2", "OTHER", "OUTCOME"))
        } returns Result.failure(networkError)

        // When
        val result = syncManager.syncPendingEntries(budgetId = 1)

        // Then
        assertIs<SyncResult.PartialSuccess<SyncStats>>(result)
        assertEquals(2, result.data.totalItems)
        assertEquals(1, result.data.syncedItems)
        assertEquals(1, result.data.failedItems)
        assertEquals(1, result.errors.size)
        assertTrue(result.errors.first().itemIdentifier.contains("Entry 2"))
    }

    @Test
    fun `syncPendingEntries handles complete failure`() = runTest {
        // Given - User is authenticated with synced parent budget
        coEvery { authRepository.isAuthenticated() } returns true
        val syncedBudget = Budget(id = 1, name = "Budget", amount = 1000.0, serverId = 101)
        coEvery { budgetLocalDataSource.getById(1) } returns syncedBudget

        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "50.0", description = "Entry 1", serverId = null)
        every { entryLocalDataSource.getUnsynced(1) } returns listOf(entry)

        // API call fails
        val networkError = Exception("Network error")
        coEvery { apiService.createEntry(any(), any()) } returns Result.failure(networkError)

        // When
        val result = syncManager.syncPendingEntries(budgetId = 1)

        // Then
        assertIs<SyncResult.Failure>(result)
        assertTrue(result.error.message?.contains("Failed to sync all items") == true)
        verify(exactly = 0) { entryLocalDataSource.update(any()) }
    }

    // ========== pullEntriesFromServer() Tests ==========

    @Test
    fun `pullEntriesFromServer returns failure when not authenticated`() = runTest {
        // Given - User is not authenticated
        coEvery { authRepository.isAuthenticated() } returns false

        // When
        val result = syncManager.pullEntriesFromServer(budgetServerId = 101)

        // Then
        assertIs<SyncResult.Failure>(result)
        assertIs<SyncException.NotAuthenticated>(result.error)
        coVerify(exactly = 0) { apiService.getEntries(any()) }
    }

    @Test
    fun `pullEntriesFromServer returns failure when local budget not found`() = runTest {
        // Given - User is authenticated but local budget doesn't exist
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { budgetLocalDataSource.getAllCached() } returns emptyList()

        // When
        val result = syncManager.pullEntriesFromServer(budgetServerId = 101)

        // Then
        assertIs<SyncResult.Failure>(result)
        assertTrue(result.error.message?.contains("Local budget not found for server ID 101") == true)
    }

    @Test
    fun `pullEntriesFromServer returns success with zero items when server returns empty list`() = runTest {
        // Given - User is authenticated and local budget exists
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { apiService.getEntries(101) } returns Result.success(emptyList())

        // When
        val result = syncManager.pullEntriesFromServer(budgetServerId = 101, localBudgetId = 1)

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(0, result.data.totalItems)
    }

    @Test
    fun `pullEntriesFromServer successfully pulls and creates new entries`() = runTest {
        // Given - User is authenticated
        coEvery { authRepository.isAuthenticated() } returns true

        val serverEntry1 = BudgetEntryResponse(
            id = 201, budgetId = 101, amount = 50.0, description = "Server Entry 1",
            category = "FOOD", type = "OUTCOME", createdByEmail = "user@test.com",
            updatedByEmail = null, creationDate = "2024-01-01", modificationDate = "2024-01-01"
        )
        val serverEntry2 = BudgetEntryResponse(
            id = 202, budgetId = 101, amount = 75.0, description = "Server Entry 2",
            category = "TRANSPORTATION", type = "OUTCOME", createdByEmail = "user@test.com",
            updatedByEmail = null, creationDate = "2024-01-02", modificationDate = "2024-01-02"
        )
        coEvery { apiService.getEntries(101) } returns Result.success(listOf(serverEntry1, serverEntry2))

        // Entries don't exist locally
        coEvery { entryLocalDataSource.selectByServerId(201) } returns null
        coEvery { entryLocalDataSource.selectByServerId(202) } returns null
        coEvery { entryLocalDataSource.selectByUniqueFields(any(), any(), any(), any()) } returns null

        every { entryLocalDataSource.create(any()) } returns Unit

        // When
        val result = syncManager.pullEntriesFromServer(budgetServerId = 101, localBudgetId = 1)

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(2, result.data.totalItems)
        assertEquals(2, result.data.syncedItems)
        verify(exactly = 2) { entryLocalDataSource.create(any()) }
    }

    @Test
    fun `pullEntriesFromServer successfully updates existing entries`() = runTest {
        // Given - User is authenticated
        coEvery { authRepository.isAuthenticated() } returns true

        val serverEntry = BudgetEntryResponse(
            id = 201, budgetId = 101, amount = 60.0, description = "Updated Description",
            category = "FOOD", type = "OUTCOME", createdByEmail = "user@test.com",
            updatedByEmail = "user@test.com", creationDate = "2024-01-01", modificationDate = "2024-01-02"
        )
        coEvery { apiService.getEntries(101) } returns Result.success(listOf(serverEntry))

        // Entry exists locally - mock the database entry with all required properties
        val existingDbEntry = mockk<Budget_entry> {
            every { id } returns 1
            every { budget_id } returns 1
            every { amount } returns 50.0
            every { description } returns "Old Description"
            every { type } returns BudgetEntry.Type.OUTCOME
            every { category } returns BudgetEntry.Category.OTHER
            every { date } returns "2024-01-01"
            every { invoice } returns null
            every { server_id } returns 201
            every { is_synced } returns 1L
            every { created_by_email } returns "user@test.com"
            every { updated_by_email } returns null
            every { creation_date } returns "2024-01-01"
            every { modification_date } returns "2024-01-01"
        }
        coEvery { entryLocalDataSource.selectByServerId(201) } returns existingDbEntry

        every { entryLocalDataSource.update(any()) } returns Unit

        // When
        val result = syncManager.pullEntriesFromServer(budgetServerId = 101, localBudgetId = 1)

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(1, result.data.totalItems)
        assertEquals(1, result.data.syncedItems)

        // Verify entry was updated
        verify {
            entryLocalDataSource.update(
                match { entry ->
                    entry.amount.toDouble() == 60.0 && entry.description == "Updated Description"
                }
            )
        }
    }

    @Test
    fun `pullEntriesFromServer returns failure when API call fails`() = runTest {
        // Given - User is authenticated but API fails
        coEvery { authRepository.isAuthenticated() } returns true
        val serverError = Exception("Server error")
        coEvery { apiService.getEntries(101) } returns Result.failure(serverError)

        // When
        val result = syncManager.pullEntriesFromServer(budgetServerId = 101, localBudgetId = 1)

        // Then
        assertIs<SyncResult.Failure>(result)
        assertEquals("Server error", result.error.message)
        verify(exactly = 0) { entryLocalDataSource.create(any()) }
        verify(exactly = 0) { entryLocalDataSource.update(any()) }
    }

    // ========== performFullSync() Tests ==========

    @Test
    fun `performFullSync successfully completes push and pull operations`() = runTest {
        // Given - User is authenticated
        coEvery { authRepository.isAuthenticated() } returns true

        // Setup push phase
        val syncedBudget = Budget(id = 1, name = "Budget", amount = 1000.0, serverId = 101)
        coEvery { budgetLocalDataSource.getById(1) } returns syncedBudget

        val localEntry = BudgetEntry(id = 1, budgetId = 1, amount = "50.0", description = "Local Entry", serverId = null)
        every { entryLocalDataSource.getUnsynced(1) } returns listOf(localEntry)

        val createResponse = BudgetEntryResponse(
            id = 201, budgetId = 101, amount = 50.0, description = "Local Entry",
            category = "OTHER", type = "OUTCOME", createdByEmail = "user@test.com",
            updatedByEmail = null, creationDate = "2024-01-01", modificationDate = "2024-01-01"
        )
        coEvery {
            apiService.createEntry(101, CreateBudgetEntryRequest(50.0, "Local Entry", "OTHER", "OUTCOME"))
        } returns Result.success(createResponse)
        every { entryLocalDataSource.update(any()) } returns Unit

        // Setup pull phase
        val serverEntry = BudgetEntryResponse(
            id = 202, budgetId = 101, amount = 75.0, description = "Server Entry",
            category = "FOOD", type = "OUTCOME", createdByEmail = "user@test.com",
            updatedByEmail = null, creationDate = "2024-01-02", modificationDate = "2024-01-02"
        )
        coEvery { apiService.getEntries(101) } returns Result.success(listOf(serverEntry))
        coEvery { entryLocalDataSource.selectByServerId(202) } returns null
        coEvery { entryLocalDataSource.selectByUniqueFields(any(), any(), any(), any()) } returns null
        every { entryLocalDataSource.create(any()) } returns Unit

        // When
        val result = syncManager.performFullSync(budgetId = 1, budgetServerId = 101)

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(2, result.data.totalItems) // 1 pushed + 1 pulled
        assertEquals(2, result.data.syncedItems)
        assertEquals(0, result.data.failedItems)

        // Verify both operations occurred
        verify { entryLocalDataSource.update(any()) } // Push
        verify { entryLocalDataSource.create(any()) } // Pull
    }

    @Test
    fun `performFullSync returns failure when push phase fails`() = runTest {
        // Given - User is authenticated but parent budget not synced
        coEvery { authRepository.isAuthenticated() } returns true
        val unsyncedBudget = Budget(id = 1, name = "Budget", amount = 1000.0, serverId = null)
        coEvery { budgetLocalDataSource.getById(1) } returns unsyncedBudget

        // When
        val result = syncManager.performFullSync(budgetId = 1, budgetServerId = 101)

        // Then
        assertIs<SyncResult.Failure>(result)
        assertIs<SyncException.ParentNotSynced>(result.error)

        // Verify pull phase was not attempted
        coVerify(exactly = 0) { apiService.getEntries(any()) }
    }

    // ========== syncAllBudgetsEntries() Tests ==========

    @Test
    fun `syncAllBudgetsEntries returns failure when not authenticated`() = runTest {
        // Given - User is not authenticated
        coEvery { authRepository.isAuthenticated() } returns false

        // When
        val result = syncManager.syncAllBudgetsEntries()

        // Then
        assertIs<SyncResult.Failure>(result)
        assertIs<SyncException.NotAuthenticated>(result.error)
    }

    @Test
    fun `syncAllBudgetsEntries returns success with zero items when no synced budgets`() = runTest {
        // Given - User is authenticated but no synced budgets
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { budgetLocalDataSource.getAllCached() } returns emptyList()

        // When
        val result = syncManager.syncAllBudgetsEntries()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(0, result.data.totalItems)
    }

    @Test
    fun `syncAllBudgetsEntries syncs entries for all budgets`() = runTest {
        // Given - User is authenticated with multiple synced budgets
        coEvery { authRepository.isAuthenticated() } returns true

        val budget1 = Budget(id = 1, name = "Budget 1", amount = 1000.0, serverId = 101)
        val budget2 = Budget(id = 2, name = "Budget 2", amount = 2000.0, serverId = 102)
        coEvery { budgetLocalDataSource.getAllCached() } returns listOf(budget1, budget2)

        // Budget 1 push/pull
        coEvery { budgetLocalDataSource.getById(1) } returns budget1
        every { entryLocalDataSource.getUnsynced(1) } returns emptyList()
        coEvery { apiService.getEntries(101) } returns Result.success(emptyList())

        // Budget 2 push/pull
        coEvery { budgetLocalDataSource.getById(2) } returns budget2
        every { entryLocalDataSource.getUnsynced(2) } returns emptyList()
        coEvery { apiService.getEntries(102) } returns Result.success(emptyList())

        // When
        val result = syncManager.syncAllBudgetsEntries()

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(0, result.data.totalItems) // No entries in this test
        assertEquals(0, result.data.syncedItems)

        // Verify sync was attempted for both budgets
        coVerify { apiService.getEntries(101) }
        coVerify { apiService.getEntries(102) }
    }

    @Test
    fun `syncAllBudgetsEntries continues on partial failures`() = runTest {
        // Given - User is authenticated with multiple budgets
        coEvery { authRepository.isAuthenticated() } returns true

        val budget1 = Budget(id = 1, name = "Budget 1", amount = 1000.0, serverId = 101)
        val budget2 = Budget(id = 2, name = "Budget 2", amount = 2000.0, serverId = 102)
        coEvery { budgetLocalDataSource.getAllCached() } returns listOf(budget1, budget2)

        // Budget 1 fails during push (parent not synced, shouldn't happen but testing)
        coEvery { budgetLocalDataSource.getById(1) } returns budget1.copy(serverId = null)

        // Budget 2 succeeds
        coEvery { budgetLocalDataSource.getById(2) } returns budget2
        every { entryLocalDataSource.getUnsynced(2) } returns emptyList()
        coEvery { apiService.getEntries(102) } returns Result.success(emptyList())

        // When
        val result = syncManager.syncAllBudgetsEntries()

        // Then - Should still succeed with budget 2's results
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(0, result.data.totalItems)

        // Verify budget 2 was synced even though budget 1 failed
        coVerify { apiService.getEntries(102) }
    }
}
