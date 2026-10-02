package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.data.network.BudgetEntryApiService
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.data.sync.SyncResult
import com.meneses.budgethunter.commons.data.sync.SyncStats
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for BudgetEntryRepository. The failed-push state lives in BudgetEntrySyncManager;
 * the repository only exposes it, triggers pushes when authenticated, and clears it on sign-out.
 */
class BudgetEntryRepositoryTest {

    private val localDataSource = mockk<BudgetEntryLocalDataSource>(relaxed = true)
    private val syncManager = mockk<BudgetEntrySyncManager>()
    private val authRepository = mockk<AuthRepository>()
    private val budgetEntryApiService = mockk<BudgetEntryApiService>(relaxed = true)
    private val budgetLocalDataSource = mockk<BudgetLocalDataSource>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)

    private val failedPushes = MutableStateFlow<Set<Int>>(emptySet())

    private lateinit var repository: BudgetEntryRepository

    private val entry = BudgetEntry(id = 1, budgetId = 7, amount = "10.0", description = "Entry")
    private val success = SyncResult.Success(SyncStats(totalItems = 1, syncedItems = 1))

    @BeforeTest
    fun setup() {
        every { localDataSource.create(any()) } returns Unit
        every { localDataSource.update(any()) } returns Unit
        every { syncManager.budgetsWithFailedPush } returns failedPushes
        every { syncManager.clearFailedPushes() } just runs
        coEvery { syncManager.syncPendingEntries(any()) } returns success
        repository = BudgetEntryRepository(
            localDataSource = localDataSource,
            syncManager = syncManager,
            authRepository = authRepository,
            budgetEntryApiService = budgetEntryApiService,
            budgetLocalDataSource = budgetLocalDataSource,
            ioDispatcher = Dispatchers.Unconfined,
            logger = logger
        )
    }

    // ========== create() ==========

    @Test
    fun `create pushes pending entries of the budget when authenticated`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns true

        repository.create(entry)

        verify(exactly = 1) { localDataSource.create(entry) }
        coVerify(exactly = 1) { syncManager.syncPendingEntries(7) }
    }

    @Test
    fun `create does not push and logs debug when not authenticated`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns false

        repository.create(entry)

        verify(exactly = 1) { localDataSource.create(entry) }
        coVerify(exactly = 0) { syncManager.syncPendingEntries(any()) }
        verify(atLeast = 1) { logger.debug(any(), any()) }
    }

    // ========== update() ==========

    @Test
    fun `update pushes pending entries of the budget when authenticated`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns true

        repository.update(entry)

        verify(exactly = 1) { localDataSource.update(entry.copy(isSynced = false)) }
        coVerify(exactly = 1) { syncManager.syncPendingEntries(7) }
    }

    @Test
    fun `update does not push and logs debug when not authenticated`() = runTest {
        coEvery { authRepository.isAuthenticated() } returns false

        repository.update(entry)

        verify(exactly = 1) { localDataSource.update(entry.copy(isSynced = false)) }
        coVerify(exactly = 0) { syncManager.syncPendingEntries(any()) }
        verify(atLeast = 1) { logger.debug(any(), any()) }
    }

    // ========== budgetsWithFailedSync ==========

    @Test
    fun `budgetsWithFailedSync exposes the sync manager failed push state`() = runTest {
        assertEquals(emptySet(), repository.budgetsWithFailedSync.value)

        failedPushes.value = setOf(7, 9)

        assertEquals(setOf(7, 9), repository.budgetsWithFailedSync.value)
    }

    // ========== clearAllData() ==========

    @Test
    fun `clearAllData clears local entries and the failed push state`() = runTest {
        repository.clearAllData()

        verify(exactly = 1) { localDataSource.clearAllData() }
        verify(exactly = 1) { syncManager.clearFailedPushes() }
    }
}
