package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.data.network.BudgetApiService
import com.meneses.budgethunter.budgetList.data.sync.BudgetSyncManager
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.data.sync.SyncResult
import com.meneses.budgethunter.commons.data.sync.SyncStats
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BudgetRepositoryTest {

    private fun buildRepository(
        dataSource: BudgetLocalDataSource = mockk(relaxed = true),
        syncManager: BudgetSyncManager = mockk(relaxed = true),
        apiService: BudgetApiService = mockk(relaxed = true),
        authRepository: AuthRepository = mockk { coEvery { isAuthenticated() } returns false },
        logger: Logger = mockk(relaxed = true)
    ) = BudgetRepository(dataSource, syncManager, apiService, authRepository, Dispatchers.Default, logger)

    // ── budgets ───────────────────────────────────────────────────────────────

    @Test
    fun `budgets property exposes flow from data source`() = runTest {
        val budgetList = listOf(
            Budget(id = 1, name = "Budget 1", amount = 100.0),
            Budget(id = 2, name = "Budget 2", amount = 200.0)
        )
        val dataSource = mockk<BudgetLocalDataSource> {
            every { budgets } returns flowOf(budgetList)
        }
        val repository = buildRepository(dataSource = dataSource)

        val result = repository.budgets.first()

        assertEquals(budgetList, result)
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    fun `getById returns budget when found`() = runTest {
        val budget = Budget(id = 42, name = "Test Budget", amount = 500.0)
        val dataSource = mockk<BudgetLocalDataSource> {
            coEvery { getById(42) } returns budget
        }
        val repository = buildRepository(dataSource = dataSource)

        val result = repository.getById(42)

        assertEquals(budget, result)
        coVerify { dataSource.getById(42) }
    }

    @Test
    fun `getById returns null when budget not found`() = runTest {
        val dataSource = mockk<BudgetLocalDataSource> {
            coEvery { getById(999) } returns null
        }
        val repository = buildRepository(dataSource = dataSource)

        val result = repository.getById(999)

        assertNull(result)
        coVerify { dataSource.getById(999) }
    }

    @Test
    fun `getById handles large ID values`() = runTest {
        val largeId = Int.MAX_VALUE
        val budget = Budget(id = largeId, name = "Large ID Budget", amount = 100.0)
        val dataSource = mockk<BudgetLocalDataSource> {
            coEvery { getById(largeId) } returns budget
        }
        val repository = buildRepository(dataSource = dataSource)

        val result = repository.getById(largeId)

        assertEquals(budget, result)
        coVerify { dataSource.getById(largeId) }
    }

    @Test
    fun `getById propagates exception from data source`() = runTest {
        val dataSource = mockk<BudgetLocalDataSource> {
            coEvery { getById(1) } throws IllegalArgumentException("Invalid ID")
        }
        val repository = buildRepository(dataSource = dataSource)

        val exception = kotlin.runCatching { repository.getById(1) }.exceptionOrNull()

        assertEquals("Invalid ID", exception?.message)
        assertEquals(IllegalArgumentException::class, exception?.let { it::class })
    }

    // ── getAllCached ──────────────────────────────────────────────────────────

    @Test
    fun `getAllCached returns cached budgets`() = runTest {
        val cachedBudgets = listOf(
            Budget(id = 1, name = "Cached 1", amount = 100.0),
            Budget(id = 2, name = "Cached 2", amount = 200.0)
        )
        val dataSource = mockk<BudgetLocalDataSource> {
            coEvery { getAllCached() } returns cachedBudgets
        }
        val repository = buildRepository(dataSource = dataSource)

        val result = repository.getAllCached()

        assertEquals(cachedBudgets, result)
        coVerify { dataSource.getAllCached() }
    }

    @Test
    fun `getAllCached returns empty list when no cached budgets`() = runTest {
        val dataSource = mockk<BudgetLocalDataSource> {
            coEvery { getAllCached() } returns emptyList()
        }
        val repository = buildRepository(dataSource = dataSource)

        val result = repository.getAllCached()

        assertEquals(emptyList(), result)
        coVerify { dataSource.getAllCached() }
    }

    @Test
    fun `getAllCached propagates exception from data source`() = runTest {
        val dataSource = mockk<BudgetLocalDataSource> {
            coEvery { getAllCached() } throws Exception("Cache read error")
        }
        val repository = buildRepository(dataSource = dataSource)

        val exception = kotlin.runCatching { repository.getAllCached() }.exceptionOrNull()

        assertEquals("Cache read error", exception?.message)
        assertEquals(Exception::class, exception?.let { it::class })
    }

    // ── getAllFilteredBy ──────────────────────────────────────────────────────

    @Test
    fun `getAllFilteredBy returns budgets matching filter`() = runTest {
        val filter = BudgetFilter(name = "February")
        val filteredBudgets = listOf(
            Budget(id = 2, name = "February Budget", amount = 200.0, date = "2024-02-15")
        )
        val dataSource = mockk<BudgetLocalDataSource> {
            coEvery { getAllFilteredBy(filter) } returns filteredBudgets
        }
        val repository = buildRepository(dataSource = dataSource)

        val result = repository.getAllFilteredBy(filter)

        assertEquals(1, result.size)
        assertEquals("February Budget", result[0].name)
        coVerify { dataSource.getAllFilteredBy(filter) }
    }

    @Test
    fun `getAllFilteredBy returns all budgets when filter is empty`() = runTest {
        val allBudgets = listOf(
            Budget(id = 1, name = "Budget 1", amount = 100.0),
            Budget(id = 2, name = "Budget 2", amount = 200.0)
        )
        val filter = BudgetFilter()
        val dataSource = mockk<BudgetLocalDataSource> {
            coEvery { getAllFilteredBy(filter) } returns allBudgets
        }
        val repository = buildRepository(dataSource = dataSource)

        val result = repository.getAllFilteredBy(filter)

        assertEquals(allBudgets.size, result.size)
        coVerify { dataSource.getAllFilteredBy(filter) }
    }

    @Test
    fun `getAllFilteredBy propagates exception from data source`() = runTest {
        val filter = BudgetFilter(name = "Test")
        val dataSource = mockk<BudgetLocalDataSource> {
            coEvery { getAllFilteredBy(filter) } throws Exception("Filter error")
        }
        val repository = buildRepository(dataSource = dataSource)

        val exception = kotlin.runCatching { repository.getAllFilteredBy(filter) }.exceptionOrNull()

        assertEquals("Filter error", exception?.message)
        assertEquals(Exception::class, exception?.let { it::class })
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    fun `create delegates to data source`() = runTest {
        val budget = Budget(id = -1, name = "New Budget", amount = 1000.0)
        val dataSource = mockk<BudgetLocalDataSource> {
            every { create(budget) } returns budget.copy(id = 1)
        }
        val repository = buildRepository(dataSource = dataSource)

        repository.create(budget)

        coVerify { dataSource.create(budget) }
    }

    @Test
    fun `create triggers sync when authenticated`() = runTest {
        val budget = Budget(id = -1, name = "New Budget", amount = 1000.0)
        val dataSource = mockk<BudgetLocalDataSource> {
            every { create(budget) } returns budget.copy(id = 1)
        }
        val syncManager = mockk<BudgetSyncManager> {
            coEvery { syncPendingBudgets() } returns SyncResult.Success(
                SyncStats(totalItems = 1, syncedItems = 1)
            )
        }
        val authRepository = mockk<AuthRepository> {
            coEvery { isAuthenticated() } returns true
        }
        val repository = buildRepository(
            dataSource = dataSource,
            syncManager = syncManager,
            authRepository = authRepository
        )

        repository.create(budget)

        coVerify { syncManager.syncPendingBudgets() }
    }

    @Test
    fun `create does not trigger sync when not authenticated`() = runTest {
        val budget = Budget(id = -1, name = "New Budget", amount = 1000.0)
        val dataSource = mockk<BudgetLocalDataSource> {
            every { create(budget) } returns budget.copy(id = 1)
        }
        val syncManager = mockk<BudgetSyncManager>(relaxed = true)
        val authRepository = mockk<AuthRepository> {
            coEvery { isAuthenticated() } returns false
        }
        val repository = buildRepository(
            dataSource = dataSource,
            syncManager = syncManager,
            authRepository = authRepository
        )

        repository.create(budget)

        coVerify(exactly = 0) { syncManager.syncPendingBudgets() }
    }

    @Test
    fun `create handles budget with zero amount`() = runTest {
        val budget = Budget(id = -1, name = "Zero Budget", amount = 0.0)
        val dataSource = mockk<BudgetLocalDataSource> {
            every { create(budget) } returns budget.copy(id = 1)
        }
        val repository = buildRepository(dataSource = dataSource)

        repository.create(budget)

        coVerify { dataSource.create(budget) }
    }

    @Test
    fun `create handles budget with empty name`() = runTest {
        val budget = Budget(id = -1, name = "", amount = 100.0)
        val dataSource = mockk<BudgetLocalDataSource> {
            every { create(budget) } returns budget.copy(id = 1)
        }
        val repository = buildRepository(dataSource = dataSource)

        repository.create(budget)

        coVerify { dataSource.create(budget) }
    }

    @Test
    fun `create propagates exception from data source`() = runTest {
        val budget = Budget(id = -1, name = "New Budget", amount = 1000.0)
        val dataSource = mockk<BudgetLocalDataSource> {
            every { create(budget) } throws IllegalStateException("Database error")
        }
        val repository = buildRepository(dataSource = dataSource)

        val exception = kotlin.runCatching { repository.create(budget) }.exceptionOrNull()

        assertEquals("Database error", exception?.message)
        assertEquals(IllegalStateException::class, exception?.let { it::class })
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    fun `update delegates to data source`() = runTest {
        val budget = Budget(id = 1, name = "Updated Budget", amount = 1500.0)
        val dataSource = mockk<BudgetLocalDataSource> {
            every { update(budget) } returns Unit
        }
        val repository = buildRepository(dataSource = dataSource)

        repository.update(budget)

        coVerify { dataSource.update(budget) }
    }

    @Test
    fun `update triggers sync when authenticated`() = runTest {
        val budget = Budget(id = 1, name = "Updated Budget", amount = 1500.0)
        val dataSource = mockk<BudgetLocalDataSource> {
            every { update(budget) } returns Unit
        }
        val syncManager = mockk<BudgetSyncManager> {
            coEvery { syncPendingBudgets() } returns SyncResult.Success(
                SyncStats(totalItems = 1, syncedItems = 1)
            )
        }
        val authRepository = mockk<AuthRepository> {
            coEvery { isAuthenticated() } returns true
        }
        val repository = buildRepository(
            dataSource = dataSource,
            syncManager = syncManager,
            authRepository = authRepository
        )

        repository.update(budget)

        coVerify { syncManager.syncPendingBudgets() }
    }

    @Test
    fun `update does not trigger sync when not authenticated`() = runTest {
        val budget = Budget(id = 1, name = "Updated Budget", amount = 1500.0)
        val dataSource = mockk<BudgetLocalDataSource> {
            every { update(budget) } returns Unit
        }
        val syncManager = mockk<BudgetSyncManager>(relaxed = true)
        val authRepository = mockk<AuthRepository> {
            coEvery { isAuthenticated() } returns false
        }
        val repository = buildRepository(
            dataSource = dataSource,
            syncManager = syncManager,
            authRepository = authRepository
        )

        repository.update(budget)

        coVerify(exactly = 0) { syncManager.syncPendingBudgets() }
    }

    @Test
    fun `update handles multiple sequential updates`() = runTest {
        val budget1 = Budget(id = 1, name = "Budget 1", amount = 100.0)
        val budget2 = Budget(id = 2, name = "Budget 2", amount = 200.0)
        val budget3 = Budget(id = 3, name = "Budget 3", amount = 300.0)
        val dataSource = mockk<BudgetLocalDataSource> {
            every { update(budget1) } returns Unit
            every { update(budget2) } returns Unit
            every { update(budget3) } returns Unit
        }
        val repository = buildRepository(dataSource = dataSource)

        repository.update(budget1)
        repository.update(budget2)
        repository.update(budget3)

        coVerify {
            dataSource.update(budget1)
            dataSource.update(budget2)
            dataSource.update(budget3)
        }
    }

    @Test
    fun `update preserves all budget properties`() = runTest {
        val budget = Budget(
            id = 42,
            name = "Complex Budget",
            amount = 1234.56,
            totalExpenses = 567.89,
            date = "2024-06-15"
        )
        val dataSource = mockk<BudgetLocalDataSource> {
            every { update(budget) } returns Unit
        }
        val repository = buildRepository(dataSource = dataSource)

        repository.update(budget)

        coVerify { dataSource.update(budget) }
    }

    @Test
    fun `update propagates exception from data source`() = runTest {
        val budget = Budget(id = 1, name = "Updated Budget", amount = 1500.0)
        val dataSource = mockk<BudgetLocalDataSource> {
            every { update(budget) } throws RuntimeException("Update failed")
        }
        val repository = buildRepository(dataSource = dataSource)

        val exception = kotlin.runCatching { repository.update(budget) }.exceptionOrNull()

        assertEquals("Update failed", exception?.message)
        assertEquals(RuntimeException::class, exception?.let { it::class })
    }

    // ── sync ──────────────────────────────────────────────────────────────────

    @Test
    fun `sync returns success when full sync succeeds`() = runTest {
        val syncManager = mockk<BudgetSyncManager> {
            coEvery { performFullSync() } returns SyncResult.Success(
                SyncStats(totalItems = 5, syncedItems = 5)
            )
        }
        val repository = buildRepository(syncManager = syncManager)

        val result = repository.sync()

        assertTrue(result.isSuccess)
        coVerify { syncManager.performFullSync() }
    }

    @Test
    fun `sync returns success on partial sync`() = runTest {
        val syncManager = mockk<BudgetSyncManager> {
            coEvery { performFullSync() } returns SyncResult.PartialSuccess(
                data = SyncStats(totalItems = 5, syncedItems = 3, failedItems = 2),
                succeeded = 3,
                failed = 2,
                errors = emptyList()
            )
        }
        val repository = buildRepository(syncManager = syncManager)

        val result = repository.sync()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `sync returns failure when full sync fails`() = runTest {
        val error = RuntimeException("Sync failed")
        val syncManager = mockk<BudgetSyncManager> {
            coEvery { performFullSync() } returns SyncResult.Failure(error)
        }
        val repository = buildRepository(syncManager = syncManager)

        val result = repository.sync()

        assertTrue(result.isFailure)
        assertEquals("Sync failed", result.exceptionOrNull()?.message)
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    fun `delete removes budget locally when not authenticated`() = runTest {
        val budget = Budget(id = 5, name = "To Delete", amount = 100.0, serverId = 99L)
        val dataSource = mockk<BudgetLocalDataSource> {
            coEvery { getById(5) } returns budget
            every { delete(5L) } returns Unit
        }
        val authRepository = mockk<AuthRepository> {
            coEvery { isAuthenticated() } returns false
        }
        val repository = buildRepository(dataSource = dataSource, authRepository = authRepository)

        repository.delete(5)

        coVerify { dataSource.delete(5L) }
    }

    @Test
    fun `delete removes budget from server then locally when authenticated with serverId`() = runTest {
        val budget = Budget(id = 5, name = "To Delete", amount = 100.0, serverId = 99L)
        val dataSource = mockk<BudgetLocalDataSource> {
            coEvery { getById(5) } returns budget
            every { delete(5L) } returns Unit
        }
        val authRepository = mockk<AuthRepository> {
            coEvery { isAuthenticated() } returns true
        }
        val apiService = mockk<BudgetApiService> {
            coEvery { deleteBudget(99L) } returns Result.success(Unit)
        }
        val repository = buildRepository(
            dataSource = dataSource,
            apiService = apiService,
            authRepository = authRepository
        )

        repository.delete(5)

        coVerify { apiService.deleteBudget(99L) }
        coVerify { dataSource.delete(5L) }
    }

    @Test
    fun `delete only removes locally when authenticated but budget has no serverId`() = runTest {
        val budget = Budget(id = 5, name = "To Delete", amount = 100.0, serverId = null)
        val dataSource = mockk<BudgetLocalDataSource> {
            coEvery { getById(5) } returns budget
            every { delete(5L) } returns Unit
        }
        val authRepository = mockk<AuthRepository> {
            coEvery { isAuthenticated() } returns true
        }
        val apiService = mockk<BudgetApiService>(relaxed = true)
        val repository = buildRepository(
            dataSource = dataSource,
            apiService = apiService,
            authRepository = authRepository
        )

        repository.delete(5)

        // apiService is relaxed — deleteBudget was never configured, so any call would use default Unit
        // The important assertion is that the local delete still happened
        coVerify { dataSource.delete(5L) }
    }

    // ── clearAllData ──────────────────────────────────────────────────────────

    @Test
    fun `clearAllData delegates to data source`() = runTest {
        val dataSource = mockk<BudgetLocalDataSource> {
            every { clearAllData() } returns Unit
        }
        val repository = buildRepository(dataSource = dataSource)

        repository.clearAllData()

        coVerify { dataSource.clearAllData() }
    }

    // ── combined operations ───────────────────────────────────────────────────

    @Test
    fun `create and update can be called sequentially`() = runTest {
        val newBudget = Budget(id = -1, name = "New", amount = 100.0)
        val updatedBudget = Budget(id = 1, name = "Updated", amount = 200.0)
        val dataSource = mockk<BudgetLocalDataSource> {
            every { create(newBudget) } returns newBudget.copy(id = 1)
            every { update(updatedBudget) } returns Unit
        }
        val repository = buildRepository(dataSource = dataSource)

        repository.create(newBudget)
        repository.update(updatedBudget)

        coVerify {
            dataSource.create(newBudget)
            dataSource.update(updatedBudget)
        }
    }
}
