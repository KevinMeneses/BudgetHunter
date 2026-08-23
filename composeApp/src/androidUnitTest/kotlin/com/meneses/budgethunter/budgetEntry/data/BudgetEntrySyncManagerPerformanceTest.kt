package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.network.BudgetEntryApiService
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.network.models.BudgetEntryResponse
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.data.sync.SyncResult
import com.meneses.budgethunter.commons.data.sync.SyncStats
import com.meneses.budgethunter.utils.TestDataFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.system.measureTimeMillis
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Performance / load tests for [BudgetEntrySyncManager].
 * Verifies that sync operations complete within acceptable time bounds
 * when processing large volumes of budget entries.
 *
 * All tests use [Dispatchers.Unconfined] to avoid introducing real threading overhead,
 * so the elapsed time measured by [measureTimeMillis] reflects business-logic cost rather
 * than thread-scheduling latency.
 */
class BudgetEntrySyncManagerPerformanceTest {

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

    // ========== syncPendingEntries() Performance Tests ==========

    @Test
    fun `syncPendingEntries with 1000 new entries syncs all successfully`() = runTest {
        // Given - User is authenticated with a synced parent budget
        coEvery { authRepository.isAuthenticated() } returns true
        val syncedBudget = Budget(id = 1, name = "Budget", amount = 1000.0, serverId = 101L)
        coEvery { budgetLocalDataSource.getById(1) } returns syncedBudget

        val entries = TestDataFactory.budgetEntries(count = 1000, budgetId = 1, synced = false)
        every { entryLocalDataSource.getUnsynced(1) } returns entries

        // All API create calls succeed with a generic successful response
        val anyResponse = BudgetEntryResponse(
            id = 201L,
            budgetId = 101L,
            amount = 50.0,
            description = "Entry",
            category = "OTHER",
            type = "OUTCOME",
            createdByEmail = "user@test.com",
            updatedByEmail = null,
            creationDate = "2024-01-01",
            modificationDate = "2024-01-01"
        )
        coEvery { apiService.createEntry(any(), any()) } returns Result.success(anyResponse)
        every { entryLocalDataSource.update(any()) } returns Unit

        // When
        var result: SyncResult<SyncStats>
        val elapsed = measureTimeMillis {
            result = syncManager.syncPendingEntries(budgetId = 1)
        }

        // Then - result correctness
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(1000, result.data.totalItems)
        assertEquals(1000, result.data.syncedItems)
        assertEquals(0, result.data.failedItems)

        // Then - performance constraint
        assertTrue(elapsed < 5000, "Expected completion in <5000ms but took ${elapsed}ms")

        // Then - verify update was called for every entry
        coVerify(exactly = 1000) { entryLocalDataSource.update(any()) }
    }

    // ========== pullEntriesFromServer() Performance Tests ==========

    @Test
    fun `pullEntriesFromServer with 1000 server entries processes all`() = runTest {
        // Given - User is authenticated
        coEvery { authRepository.isAuthenticated() } returns true

        val serverEntries = TestDataFactory.budgetEntryResponses(count = 1000, budgetServerId = 101L)
        coEvery { apiService.getEntries(101L) } returns Result.success(serverEntries)

        // All entries are new — not found by serverId or by unique fields
        coEvery { entryLocalDataSource.selectByServerId(any()) } returns null
        coEvery { entryLocalDataSource.selectByUniqueFields(any(), any(), any(), any()) } returns null
        every { entryLocalDataSource.create(any()) } returns Unit

        // When
        var result: SyncResult<SyncStats>
        val elapsed = measureTimeMillis {
            result = syncManager.pullEntriesFromServer(budgetServerId = 101L, localBudgetId = 1)
        }

        // Then - result correctness
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(1000, result.data.totalItems)
        assertEquals(1000, result.data.syncedItems)
        assertEquals(0, result.data.failedItems)

        // Then - performance constraint
        assertTrue(elapsed < 5000, "Expected completion in <5000ms but took ${elapsed}ms")

        // Then - verify a local record was created for every server entry
        coVerify(exactly = 1000) { entryLocalDataSource.create(any()) }
    }

    // ========== performFullSync() Performance Tests ==========

    @Test
    fun `performFullSync with 500 local entries plus 500 server entries accounts for all`() = runTest {
        // Given - User is authenticated with a synced parent budget
        coEvery { authRepository.isAuthenticated() } returns true
        val syncedBudget = Budget(id = 1, name = "Budget", amount = 1000.0, serverId = 101L)
        coEvery { budgetLocalDataSource.getById(1) } returns syncedBudget

        // Push side: 500 unsynced local entries
        val localEntries = TestDataFactory.budgetEntries(count = 500, budgetId = 1, synced = false)
        every { entryLocalDataSource.getUnsynced(1) } returns localEntries

        val anyResponse = BudgetEntryResponse(
            id = 201L,
            budgetId = 101L,
            amount = 50.0,
            description = "Entry",
            category = "OTHER",
            type = "OUTCOME",
            createdByEmail = "user@test.com",
            updatedByEmail = null,
            creationDate = "2024-01-01",
            modificationDate = "2024-01-01"
        )
        coEvery { apiService.createEntry(any(), any()) } returns Result.success(anyResponse)
        every { entryLocalDataSource.update(any()) } returns Unit

        // Pull side: 500 new server entries
        val serverEntries = TestDataFactory.budgetEntryResponses(count = 500, budgetServerId = 101L)
        coEvery { apiService.getEntries(101L) } returns Result.success(serverEntries)
        coEvery { entryLocalDataSource.selectByServerId(any()) } returns null
        coEvery { entryLocalDataSource.selectByUniqueFields(any(), any(), any(), any()) } returns null
        every { entryLocalDataSource.create(any()) } returns Unit

        // When
        var result: SyncResult<SyncStats>
        val elapsed = measureTimeMillis {
            result = syncManager.performFullSync(budgetId = 1, budgetServerId = 101L)
        }

        // Then - result correctness
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(1000, result.data.totalItems)
        assertEquals(1000, result.data.syncedItems)
        assertEquals(0, result.data.failedItems)

        // Then - performance constraint
        assertTrue(elapsed < 10000, "Expected completion in <10000ms but took ${elapsed}ms")
    }

    // ========== syncAllBudgetsEntries() Performance Tests ==========

    @Test
    fun `syncAllBudgetsEntries across 10 budgets with 100 entries each processes 1000 entries total`() = runTest {
        // Given - User is authenticated with 10 synced budgets
        coEvery { authRepository.isAuthenticated() } returns true

        val budgets = TestDataFactory.budgets(count = 10, synced = true)
        coEvery { budgetLocalDataSource.getAllCached() } returns budgets

        // For each budget: push 100 unsynced entries, pull returns empty list
        budgets.forEach { budget ->
            coEvery { budgetLocalDataSource.getById(budget.id) } returns budget

            val entriesForBudget = TestDataFactory.budgetEntries(
                count = 100,
                budgetId = budget.id,
                synced = false
            )
            every { entryLocalDataSource.getUnsynced(budget.id) } returns entriesForBudget

            val anyResponse = BudgetEntryResponse(
                id = 201L,
                budgetId = budget.serverId!!,
                amount = 50.0,
                description = "Entry",
                category = "OTHER",
                type = "OUTCOME",
                createdByEmail = "user@test.com",
                updatedByEmail = null,
                creationDate = "2024-01-01",
                modificationDate = "2024-01-01"
            )
            coEvery { apiService.createEntry(budget.serverId, any()) } returns Result.success(anyResponse)

            // Pull returns empty list so we focus entirely on the push side
            coEvery { apiService.getEntries(budget.serverId) } returns Result.success(emptyList())
        }

        every { entryLocalDataSource.update(any()) } returns Unit

        // When
        var result: SyncResult<SyncStats>
        val elapsed = measureTimeMillis {
            result = syncManager.syncAllBudgetsEntries()
        }

        // Then - result correctness (10 budgets × 100 entries = 1000 total)
        assertIs<SyncResult.Success<SyncStats>>(result)
        assertEquals(1000, result.data.totalItems)
        assertEquals(1000, result.data.syncedItems)
        assertEquals(0, result.data.failedItems)

        // Then - performance constraint
        assertTrue(elapsed < 15000, "Expected completion in <15000ms but took ${elapsed}ms")
    }
}
