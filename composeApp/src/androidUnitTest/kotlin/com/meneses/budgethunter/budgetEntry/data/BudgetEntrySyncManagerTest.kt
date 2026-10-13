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
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.awaitAll

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
            apiService.createEntry(101, CreateBudgetEntryRequest(50.0, "Entry 1", "OTHER", "OUTCOME", date = entry1.date))
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
            apiService.createEntry(101, CreateBudgetEntryRequest(75.0, "Entry 2", "OTHER", "OUTCOME", date = entry2.date))
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
            apiService.updateEntry(101, 201, UpdateBudgetEntryRequest(60.0, "Updated Entry", "OTHER", "OUTCOME", date = entry.date))
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
            apiService.createEntry(101, CreateBudgetEntryRequest(50.0, "Entry 1", "OTHER", "OUTCOME", date = entry1.date))
        } returns Result.success(response1)
        every { entryLocalDataSource.update(match { it.id == 1 }) } returns Unit

        // Second entry fails
        val networkError = Exception("Network error")
        coEvery {
            apiService.createEntry(101, CreateBudgetEntryRequest(75.0, "Entry 2", "OTHER", "OUTCOME", date = entry2.date))
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
            apiService.createEntry(101, CreateBudgetEntryRequest(50.0, "Local Entry", "OTHER", "OUTCOME", date = localEntry.date))
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

    // ========== Entry date field push/pull tests ==========

    @Test
    fun `pushNewEntry sends date when creating a new entry`() = runTest {
        // Given - User is authenticated with synced parent budget and a new entry
        coEvery { authRepository.isAuthenticated() } returns true
        val syncedBudget = Budget(id = 1, name = "Budget", amount = 1000.0, serverId = 101)
        coEvery { budgetLocalDataSource.getById(1) } returns syncedBudget

        val entry = BudgetEntry(
            id = 1,
            budgetId = 1,
            amount = "50.0",
            description = "Entry 1",
            date = "2026-01-15",
            serverId = null
        )
        every { entryLocalDataSource.getUnsynced(1) } returns listOf(entry)

        val response = BudgetEntryResponse(
            id = 201, budgetId = 101, amount = 50.0, description = "Entry 1",
            category = "OTHER", type = "OUTCOME", date = "2026-01-15",
            createdByEmail = "user@test.com", updatedByEmail = null,
            creationDate = "2024-01-01", modificationDate = "2024-01-01"
        )
        coEvery {
            apiService.createEntry(101, CreateBudgetEntryRequest(50.0, "Entry 1", "OTHER", "OUTCOME", date = "2026-01-15"))
        } returns Result.success(response)
        every { entryLocalDataSource.update(any()) } returns Unit

        // When
        val result = syncManager.syncPendingEntries(budgetId = 1)

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        coVerify {
            apiService.createEntry(101, CreateBudgetEntryRequest(50.0, "Entry 1", "OTHER", "OUTCOME", date = "2026-01-15"))
        }
    }

    @Test
    fun `pushUpdatedEntry sends date when updating an existing entry`() = runTest {
        // Given - User is authenticated with synced parent budget and an already-synced entry
        coEvery { authRepository.isAuthenticated() } returns true
        val syncedBudget = Budget(id = 1, name = "Budget", amount = 1000.0, serverId = 101)
        coEvery { budgetLocalDataSource.getById(1) } returns syncedBudget

        val entry = BudgetEntry(
            id = 1,
            budgetId = 1,
            amount = "60.0",
            description = "Updated Entry",
            date = "2026-02-20",
            serverId = 201
        )
        every { entryLocalDataSource.getUnsynced(1) } returns listOf(entry)

        val response = BudgetEntryResponse(
            id = 201, budgetId = 101, amount = 60.0, description = "Updated Entry",
            category = "OTHER", type = "OUTCOME", date = "2026-02-20",
            createdByEmail = "user@test.com", updatedByEmail = "user@test.com",
            creationDate = "2024-01-01", modificationDate = "2024-01-02"
        )
        coEvery {
            apiService.updateEntry(101, 201, UpdateBudgetEntryRequest(60.0, "Updated Entry", "OTHER", "OUTCOME", date = "2026-02-20"))
        } returns Result.success(response)
        every { entryLocalDataSource.update(any()) } returns Unit

        // When
        val result = syncManager.syncPendingEntries(budgetId = 1)

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        coVerify {
            apiService.updateEntry(101, 201, UpdateBudgetEntryRequest(60.0, "Updated Entry", "OTHER", "OUTCOME", date = "2026-02-20"))
        }
    }

    @Test
    fun `pullEntriesFromServer stores server date for a brand new entry`() = runTest {
        // Given - Server returns a new entry with a date
        coEvery { authRepository.isAuthenticated() } returns true

        val serverEntry = BudgetEntryResponse(
            id = 201, budgetId = 101, amount = 50.0, description = "Server Entry",
            category = "FOOD", type = "OUTCOME", date = "2026-03-10",
            createdByEmail = "user@test.com", updatedByEmail = null,
            creationDate = "2026-03-10T10:00:00.000000", modificationDate = "2026-03-10T10:00:00.000000"
        )
        coEvery { apiService.getEntries(101) } returns Result.success(listOf(serverEntry))
        coEvery { entryLocalDataSource.selectByServerId(201) } returns null
        coEvery { entryLocalDataSource.selectByUniqueFields(any(), any(), any(), any()) } returns null
        every { entryLocalDataSource.create(any()) } returns Unit

        // When
        val result = syncManager.pullEntriesFromServer(budgetServerId = 101, localBudgetId = 1)

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        verify {
            entryLocalDataSource.create(match { it.date == "2026-03-10" && it.serverId == 201L })
        }
    }

    @Test
    fun `pullEntriesFromServer derives date from creation date for a new entry with no server date`() = runTest {
        // Given - Server returns a legacy entry (older backend) with no date of its own
        coEvery { authRepository.isAuthenticated() } returns true

        val serverEntry = BudgetEntryResponse(
            id = 201, budgetId = 101, amount = 50.0, description = "Legacy Entry",
            category = "FOOD", type = "OUTCOME", date = null,
            createdByEmail = "user@test.com", updatedByEmail = null,
            creationDate = "2026-09-10T21:48:21.123456", modificationDate = "2026-09-10T21:48:21.123456"
        )
        coEvery { apiService.getEntries(101) } returns Result.success(listOf(serverEntry))
        coEvery { entryLocalDataSource.selectByServerId(201) } returns null
        coEvery { entryLocalDataSource.selectByUniqueFields(any(), any(), any(), any()) } returns null
        every { entryLocalDataSource.create(any()) } returns Unit

        // When
        val result = syncManager.pullEntriesFromServer(budgetServerId = 101, localBudgetId = 1)

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        verify {
            entryLocalDataSource.create(match { it.date == "2026-09-10" })
        }
    }

    @Test
    fun `pullEntriesFromServer adopts server date for an existing synced entry`() = runTest {
        // Given - Existing entry is already synced, so the server's date wins
        coEvery { authRepository.isAuthenticated() } returns true

        val serverEntry = BudgetEntryResponse(
            id = 201, budgetId = 101, amount = 60.0, description = "Updated Description",
            category = "FOOD", type = "OUTCOME", date = "2026-05-01",
            createdByEmail = "user@test.com", updatedByEmail = "user@test.com",
            creationDate = "2024-01-01", modificationDate = "2024-01-02"
        )
        coEvery { apiService.getEntries(101) } returns Result.success(listOf(serverEntry))

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
        verify {
            entryLocalDataSource.update(match { it.date == "2026-05-01" })
        }
    }

    @Test
    fun `pullEntriesFromServer keeps local date for a synced existing entry when server date is null`() = runTest {
        // Given - Existing, synced entry; server has no date (legacy row)
        coEvery { authRepository.isAuthenticated() } returns true

        val serverEntry = BudgetEntryResponse(
            id = 201, budgetId = 101, amount = 60.0, description = "Updated Description",
            category = "FOOD", type = "OUTCOME", date = null,
            createdByEmail = "user@test.com", updatedByEmail = "user@test.com",
            creationDate = "2024-01-01", modificationDate = "2024-01-02"
        )
        coEvery { apiService.getEntries(101) } returns Result.success(listOf(serverEntry))

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
        verify {
            entryLocalDataSource.update(match { it.date == "2024-01-01" })
        }
    }

    @Test
    fun `pullEntriesFromServer keeps local date for an unsynced existing entry even when server sends a different date`() = runTest {
        // Given - Local entry hasn't been pushed yet, so it's considered newer than the server's copy
        coEvery { authRepository.isAuthenticated() } returns true

        val serverEntry = BudgetEntryResponse(
            id = 201, budgetId = 101, amount = 60.0, description = "Updated Description",
            category = "FOOD", type = "OUTCOME", date = "2026-05-01",
            createdByEmail = "user@test.com", updatedByEmail = "user@test.com",
            creationDate = "2024-01-01", modificationDate = "2024-01-02"
        )
        coEvery { apiService.getEntries(101) } returns Result.success(listOf(serverEntry))

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
            every { is_synced } returns 0L
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
        verify {
            entryLocalDataSource.update(match { it.date == "2024-01-01" })
        }
    }

    @Test
    fun `pullEntriesFromServer merges server entries in ascending server id order`() = runTest {
        // Given - Server returns entries out of order by id
        coEvery { authRepository.isAuthenticated() } returns true

        val serverEntryHigh = BudgetEntryResponse(
            id = 203, budgetId = 101, amount = 30.0, description = "Entry C",
            category = "OTHER", type = "OUTCOME", createdByEmail = "user@test.com",
            updatedByEmail = null, creationDate = "2024-01-03", modificationDate = "2024-01-03"
        )
        val serverEntryLow = BudgetEntryResponse(
            id = 201, budgetId = 101, amount = 10.0, description = "Entry A",
            category = "OTHER", type = "OUTCOME", createdByEmail = "user@test.com",
            updatedByEmail = null, creationDate = "2024-01-01", modificationDate = "2024-01-01"
        )
        val serverEntryMid = BudgetEntryResponse(
            id = 202, budgetId = 101, amount = 20.0, description = "Entry B",
            category = "OTHER", type = "OUTCOME", createdByEmail = "user@test.com",
            updatedByEmail = null, creationDate = "2024-01-02", modificationDate = "2024-01-02"
        )
        coEvery { apiService.getEntries(101) } returns Result.success(
            listOf(serverEntryHigh, serverEntryLow, serverEntryMid)
        )
        coEvery { entryLocalDataSource.selectByServerId(any()) } returns null
        coEvery { entryLocalDataSource.selectByUniqueFields(any(), any(), any(), any()) } returns null
        every { entryLocalDataSource.create(any()) } returns Unit

        // When
        val result = syncManager.pullEntriesFromServer(budgetServerId = 101, localBudgetId = 1)

        // Then
        assertIs<SyncResult.Success<SyncStats>>(result)
        verifyOrder {
            entryLocalDataSource.create(match { it.serverId == 201L })
            entryLocalDataSource.create(match { it.serverId == 202L })
            entryLocalDataSource.create(match { it.serverId == 203L })
        }
    }

    // ========== budgetsWithFailedPush tracking ==========

    private fun givenSyncedBudget(budgetId: Int = 1, serverId: Long = 101) {
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { budgetLocalDataSource.getById(budgetId) } returns
            Budget(id = budgetId, name = "Budget", amount = 1000.0, serverId = serverId)
    }

    private fun serverResponse(id: Long, amount: Double, description: String) = BudgetEntryResponse(
        id = id, budgetId = 101, amount = amount, description = description,
        category = "OTHER", type = "OUTCOME", createdByEmail = "user@test.com",
        updatedByEmail = null, creationDate = "2024-01-01", modificationDate = "2024-01-01"
    )

    private fun givenAllPushesFail(budgetId: Int = 1) {
        val entry = BudgetEntry(id = 1, budgetId = budgetId, amount = "50.0", description = "Entry 1", serverId = null)
        every { entryLocalDataSource.getUnsynced(budgetId) } returns listOf(entry)
        coEvery { apiService.createEntry(any(), any()) } returns Result.failure(Exception("Network error"))
    }

    private fun givenAllPushesSucceed(budgetId: Int = 1) {
        val entry = BudgetEntry(id = 1, budgetId = budgetId, amount = "50.0", description = "Entry 1", serverId = null)
        every { entryLocalDataSource.getUnsynced(budgetId) } returns listOf(entry)
        coEvery { apiService.createEntry(any(), any()) } returns
            Result.success(serverResponse(201, 50.0, "Entry 1"))
        every { entryLocalDataSource.update(any()) } returns Unit
    }

    @Test
    fun `budgetsWithFailedPush starts empty`() {
        assertTrue(syncManager.budgetsWithFailedPush.value.isEmpty())
    }

    @Test
    fun `syncPendingEntries adds budget id when parent budget is not synced`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { budgetLocalDataSource.getById(1) } returns
            Budget(id = 1, name = "Budget", amount = 1000.0, serverId = null)

        val result = syncManager.syncPendingEntries(budgetId = 1)

        assertIs<SyncResult.Failure>(result)
        assertEquals(setOf(1), syncManager.budgetsWithFailedPush.value)
    }

    @Test
    fun `syncPendingEntries adds budget id when all entries fail`() = runTest {
        givenSyncedBudget()
        givenAllPushesFail()

        val result = syncManager.syncPendingEntries(budgetId = 1)

        assertIs<SyncResult.Failure>(result)
        assertEquals(setOf(1), syncManager.budgetsWithFailedPush.value)
    }

    @Test
    fun `syncPendingEntries adds budget id on partial success`() = runTest {
        givenSyncedBudget()
        val entry1 = BudgetEntry(id = 1, budgetId = 1, amount = "50.0", description = "Entry 1", serverId = null)
        val entry2 = BudgetEntry(id = 2, budgetId = 1, amount = "75.0", description = "Entry 2", serverId = null)
        every { entryLocalDataSource.getUnsynced(1) } returns listOf(entry1, entry2)
        coEvery {
            apiService.createEntry(101, CreateBudgetEntryRequest(50.0, "Entry 1", "OTHER", "OUTCOME", date = entry1.date))
        } returns Result.success(serverResponse(201, 50.0, "Entry 1"))
        coEvery {
            apiService.createEntry(101, CreateBudgetEntryRequest(75.0, "Entry 2", "OTHER", "OUTCOME", date = entry2.date))
        } returns Result.failure(Exception("Network error"))
        every { entryLocalDataSource.update(any()) } returns Unit

        val result = syncManager.syncPendingEntries(budgetId = 1)

        assertIs<SyncResult.PartialSuccess<SyncStats>>(result)
        assertEquals(setOf(1), syncManager.budgetsWithFailedPush.value)
    }

    @Test
    fun `syncPendingEntries removes budget id after a failed push is followed by a successful one`() = runTest {
        givenSyncedBudget()
        givenAllPushesFail()
        syncManager.syncPendingEntries(budgetId = 1)
        assertEquals(setOf(1), syncManager.budgetsWithFailedPush.value)
        givenAllPushesSucceed()

        val result = syncManager.syncPendingEntries(budgetId = 1)

        assertIs<SyncResult.Success<SyncStats>>(result)
        assertTrue(syncManager.budgetsWithFailedPush.value.isEmpty())
    }

    @Test
    fun `syncPendingEntries removes budget id when there are no pending entries`() = runTest {
        givenSyncedBudget()
        givenAllPushesFail()
        syncManager.syncPendingEntries(budgetId = 1)
        assertEquals(setOf(1), syncManager.budgetsWithFailedPush.value)
        every { entryLocalDataSource.getUnsynced(1) } returns emptyList()

        val result = syncManager.syncPendingEntries(budgetId = 1)

        assertIs<SyncResult.Success<SyncStats>>(result)
        assertTrue(syncManager.budgetsWithFailedPush.value.isEmpty())
    }

    @Test
    fun `syncPendingEntries leaves other budget ids untouched`() = runTest {
        givenSyncedBudget(budgetId = 1, serverId = 101)
        givenSyncedBudget(budgetId = 2, serverId = 102)
        givenAllPushesFail(budgetId = 1)
        syncManager.syncPendingEntries(budgetId = 1)
        assertEquals(setOf(1), syncManager.budgetsWithFailedPush.value)

        // A failure of budget 2 keeps budget 1 flagged
        givenAllPushesFail(budgetId = 2)
        syncManager.syncPendingEntries(budgetId = 2)
        assertEquals(setOf(1, 2), syncManager.budgetsWithFailedPush.value)

        // A success of budget 2 does not clear budget 1
        every { entryLocalDataSource.getUnsynced(2) } returns emptyList()
        syncManager.syncPendingEntries(budgetId = 2)
        assertEquals(setOf(1), syncManager.budgetsWithFailedPush.value)
    }

    @Test
    fun `performFullSync removes budget id when push succeeds even if pull fails`() = runTest {
        givenSyncedBudget()
        givenAllPushesFail()
        syncManager.syncPendingEntries(budgetId = 1)
        assertEquals(setOf(1), syncManager.budgetsWithFailedPush.value)
        givenAllPushesSucceed()
        coEvery { apiService.getEntries(101) } returns Result.failure(Exception("Server error"))

        val result = syncManager.performFullSync(budgetId = 1, budgetServerId = 101)

        assertIs<SyncResult.Failure>(result)
        assertTrue(syncManager.budgetsWithFailedPush.value.isEmpty())
    }

    @Test
    fun `pull failure does not add budget id`() = runTest {
        givenSyncedBudget()
        coEvery { apiService.getEntries(101) } returns Result.failure(Exception("Server error"))

        val result = syncManager.pullEntriesFromServer(budgetServerId = 101, localBudgetId = 1)

        assertIs<SyncResult.Failure>(result)
        assertTrue(syncManager.budgetsWithFailedPush.value.isEmpty())
    }

    @Test
    fun `syncAllBudgetsEntries adds ids of budgets whose push fails`() = runTest {
        val budget1 = Budget(id = 1, name = "Budget 1", amount = 1000.0, serverId = 101)
        val budget2 = Budget(id = 2, name = "Budget 2", amount = 2000.0, serverId = 102)
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { budgetLocalDataSource.getAllCached() } returns listOf(budget1, budget2)
        coEvery { budgetLocalDataSource.getById(1) } returns budget1
        coEvery { budgetLocalDataSource.getById(2) } returns budget2
        givenAllPushesFail(budgetId = 1)
        every { entryLocalDataSource.getUnsynced(2) } returns emptyList()
        coEvery { apiService.getEntries(102) } returns Result.success(emptyList())

        syncManager.syncAllBudgetsEntries()

        assertEquals(setOf(1), syncManager.budgetsWithFailedPush.value)
    }

    @Test
    fun `clearFailedPushes empties the set`() = runTest {
        givenSyncedBudget()
        givenAllPushesFail()
        syncManager.syncPendingEntries(budgetId = 1)
        assertEquals(setOf(1), syncManager.budgetsWithFailedPush.value)

        syncManager.clearFailedPushes()

        assertTrue(syncManager.budgetsWithFailedPush.value.isEmpty())
    }

    // ========== Push serialization ==========

    @Test
    fun `concurrent syncPendingEntries for the same budget creates the entry only once`() = runTest {
        // Given - authenticated, synced parent budget and a single unsynced entry
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { budgetLocalDataSource.getById(1) } returns
            Budget(id = 1, name = "Budget", amount = 1000.0, serverId = 101)
        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "50.0", description = "Entry 1", serverId = null)

        // Stateful local store: the entry stays unsynced until update() marks it synced
        var isEntrySynced = false
        every { entryLocalDataSource.getUnsynced(1) } answers {
            if (isEntrySynced) emptyList() else listOf(entry)
        }
        every { entryLocalDataSource.update(any()) } answers {
            isEntrySynced = true
        }

        // The create call suspends until released, so the second call can reach getUnsynced
        val createGate = CompletableDeferred<Unit>()
        val response = BudgetEntryResponse(
            id = 201, budgetId = 101, amount = 50.0, description = "Entry 1",
            category = "OTHER", type = "OUTCOME", createdByEmail = "user@test.com",
            updatedByEmail = null, creationDate = "2024-01-01", modificationDate = "2024-01-01"
        )
        coEvery { apiService.createEntry(101, any()) } coAnswers {
            createGate.await()
            Result.success(response)
        }

        // When - two concurrent pushes for the same budget
        val first = async { syncManager.syncPendingEntries(budgetId = 1) }
        val second = async { syncManager.syncPendingEntries(budgetId = 1) }
        testScheduler.runCurrent()
        createGate.complete(Unit)
        awaitAll(first, second)

        // Then
        coVerify(exactly = 1) { apiService.createEntry(any(), any()) }
    }

    @Test
    fun `pull running during a push does not duplicate the entry being pushed`() = runTest {
        // Given
        coEvery { authRepository.isAuthenticated() } returns true
        coEvery { budgetLocalDataSource.getById(1) } returns
            Budget(id = 1, name = "Budget", amount = 1000.0, serverId = 101)
        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "50.0", description = "Entry 1", serverId = null)

        val response = BudgetEntryResponse(
            id = 201, budgetId = 101, amount = 50.0, description = "Entry 1",
            category = "OTHER", type = "OUTCOME", createdByEmail = "user@test.com",
            updatedByEmail = null, creationDate = "2024-01-01", modificationDate = "2024-01-01"
        )

        // Stateful local store: the row only gets its serverId once the push writes it back
        var pushCommitted = false
        every { entryLocalDataSource.getUnsynced(1) } answers {
            if (pushCommitted) emptyList() else listOf(entry)
        }
        every { entryLocalDataSource.update(any()) } answers {
            pushCommitted = true
        }
        val dbEntry = mockk<Budget_entry> {
            every { id } returns 1
            every { budget_id } returns 1
            every { amount } returns 50.0
            every { description } returns "Entry 1"
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
        coEvery { entryLocalDataSource.selectByServerId(201) } answers { if (pushCommitted) dbEntry else null }
        coEvery { entryLocalDataSource.selectByUniqueFields(any(), any(), any(), any()) } returns null
        every { entryLocalDataSource.create(any()) } returns Unit

        // The POST is suspended until released, while the entry is already on the server
        val createGate = CompletableDeferred<Unit>()
        coEvery { apiService.createEntry(101, any()) } coAnswers {
            createGate.await()
            Result.success(response)
        }
        coEvery { apiService.getEntries(101) } returns Result.success(listOf(response))

        // When - a pull starts while the push is waiting for the server response
        val push = async { syncManager.syncPendingEntries(budgetId = 1) }
        testScheduler.runCurrent()
        val pull = async { syncManager.pullEntriesFromServer(budgetServerId = 101, localBudgetId = 1) }
        testScheduler.runCurrent()
        createGate.complete(Unit)
        awaitAll(push, pull)

        // Then - the pull ran after the push committed, so it updates instead of inserting a duplicate
        verify(exactly = 0) { entryLocalDataSource.create(any()) }
    }
}
