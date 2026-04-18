package com.meneses.budgethunter.budgetDetail.data

import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.domain.Budget
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BudgetDetailRepositoryTest {

    private fun buildRepository(
        budgetDataSource: BudgetLocalDataSource,
        entryDataSource: BudgetEntryLocalDataSource,
        budgetEntryRepository: BudgetEntryRepository = mockk(relaxed = true),
        budgetRepository: BudgetRepository = mockk(relaxed = true),
        deleteUseCase: DeleteBudgetUseCase = mockk(relaxed = true)
    ) = BudgetDetailRepository(
        budgetDataSource,
        entryDataSource,
        budgetEntryRepository,
        budgetRepository,
        Dispatchers.Default,
        deleteUseCase
    )

    @Test
    fun `getBudgetDetailById combines budget and entries`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1"),
            BudgetEntry(id = 2, budgetId = 1, amount = "200.0", description = "Entry 2")
        )
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
        }
        val repository = buildRepository(budgetDataSource, entryDataSource)

        val result = repository.getBudgetDetailById(1).first()

        assertEquals(budget, result.budget)
        assertEquals(entries, result.entries)
    }

    @Test
    fun `getBudgetDetailById returns empty entries when none exist`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
        }
        val repository = buildRepository(budgetDataSource, entryDataSource)

        val result = repository.getBudgetDetailById(1).first()

        assertEquals(budget, result.budget)
        assertEquals(emptyList(), result.entries)
    }

    @Test
    fun `getBudgetDetailById updates cache on new data`() = runTest {
        val budget = Budget(id = 1, name = "Budget 1", amount = 1000.0)
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1")
        )
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
        }
        val repository = buildRepository(budgetDataSource, entryDataSource)

        val result = repository.getBudgetDetailById(1).first()
        val cachedDetail = repository.getCachedDetail()

        assertEquals(result, cachedDetail)
    }

    @Test
    fun `getAllFilteredBy returns filtered entries`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Food", category = BudgetEntry.Category.FOOD)
        )
        val filter = BudgetEntryFilter(category = BudgetEntry.Category.FOOD)
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
            coEvery { getAllFilteredBy(filter) } returns entries
        }
        val repository = buildRepository(budgetDataSource, entryDataSource)

        repository.getBudgetDetailById(1).first()

        val result = repository.getAllFilteredBy(filter)

        assertEquals(entries, result.entries)
        coVerify { entryDataSource.getAllFilteredBy(filter) }
    }

    @Test
    fun `getAllFilteredBy uses cached budget`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val allEntries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Food", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 2, budgetId = 1, amount = "200.0", description = "Transport", category = BudgetEntry.Category.TRANSPORTATION)
        )
        val filteredEntries = listOf(allEntries[0])
        val filter = BudgetEntryFilter(category = BudgetEntry.Category.FOOD)
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(allEntries)
            coEvery { getAllFilteredBy(filter) } returns filteredEntries
        }
        val repository = buildRepository(budgetDataSource, entryDataSource)

        repository.getBudgetDetailById(1).first()

        val result = repository.getAllFilteredBy(filter)

        assertEquals(budget, result.budget)
        assertEquals(filteredEntries, result.entries)
    }

    @Test
    fun `updateBudgetAmount updates budget via BudgetRepository`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val updatedBudget = budget.copy(amount = 1500.0, isSynced = false, lastSyncedAt = null)
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
        }
        val budgetRepository = mockk<BudgetRepository> {
            coEvery { update(updatedBudget) } returns Unit
        }
        val repository = buildRepository(budgetDataSource, entryDataSource, budgetRepository = budgetRepository)

        repository.getBudgetDetailById(1).first()

        repository.updateBudgetAmount(1500.0)

        coVerify { budgetRepository.update(updatedBudget) }
    }

    @Test
    fun `updateBudgetAmount with zero amount`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val updatedBudget = budget.copy(amount = 0.0, isSynced = false, lastSyncedAt = null)
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
        }
        val budgetRepository = mockk<BudgetRepository> {
            coEvery { update(updatedBudget) } returns Unit
        }
        val repository = buildRepository(budgetDataSource, entryDataSource, budgetRepository = budgetRepository)

        repository.getBudgetDetailById(1).first()

        repository.updateBudgetAmount(0.0)

        coVerify { budgetRepository.update(updatedBudget) }
    }

    @Test
    fun `updateBudgetAmount with large amount`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val updatedBudget = budget.copy(amount = 999999.99, isSynced = false, lastSyncedAt = null)
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
        }
        val budgetRepository = mockk<BudgetRepository> {
            coEvery { update(updatedBudget) } returns Unit
        }
        val repository = buildRepository(budgetDataSource, entryDataSource, budgetRepository = budgetRepository)

        repository.getBudgetDetailById(1).first()

        repository.updateBudgetAmount(999999.99)

        coVerify { budgetRepository.update(updatedBudget) }
    }

    @Test
    fun `updateBudgetAmount propagates exception from BudgetRepository`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val updatedBudget = budget.copy(amount = 1500.0, isSynced = false, lastSyncedAt = null)
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
        }
        val budgetRepository = mockk<BudgetRepository> {
            coEvery { update(updatedBudget) } throws IllegalStateException("Update failed")
        }
        val repository = buildRepository(budgetDataSource, entryDataSource, budgetRepository = budgetRepository)

        repository.getBudgetDetailById(1).first()

        val exception = kotlin.runCatching {
            repository.updateBudgetAmount(1500.0)
        }.exceptionOrNull()

        assertEquals("Update failed", exception?.message)
        assertEquals(IllegalStateException::class, exception?.let { it::class })
    }

    @Test
    fun `deleteBudget delegates to use case`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
        }
        val deleteUseCase = mockk<DeleteBudgetUseCase> {
            coEvery { execute(1L) } returns Unit
        }
        val repository = buildRepository(budgetDataSource, entryDataSource, deleteUseCase = deleteUseCase)

        repository.deleteBudget(1)

        coVerify { deleteUseCase.execute(1L) }
    }

    @Test
    fun `deleteBudget handles large budget ID`() = runTest {
        val largeBudgetId = Int.MAX_VALUE
        val budget = Budget(id = largeBudgetId, name = "Test Budget", amount = 1000.0)
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(largeBudgetId.toLong()) } returns flowOf(emptyList())
        }
        val deleteUseCase = mockk<DeleteBudgetUseCase> {
            coEvery { execute(largeBudgetId.toLong()) } returns Unit
        }
        val repository = buildRepository(budgetDataSource, entryDataSource, deleteUseCase = deleteUseCase)

        repository.deleteBudget(largeBudgetId)

        coVerify { deleteUseCase.execute(largeBudgetId.toLong()) }
    }

    @Test
    fun `deleteBudget propagates exception from use case`() = runTest {
        val budgetDataSource = mockk<BudgetLocalDataSource>(relaxed = true)
        val entryDataSource = mockk<BudgetEntryLocalDataSource>(relaxed = true)
        val deleteUseCase = mockk<DeleteBudgetUseCase> {
            coEvery { execute(1L) } throws RuntimeException("Delete failed")
        }
        val repository = buildRepository(budgetDataSource, entryDataSource, deleteUseCase = deleteUseCase)

        val exception = kotlin.runCatching {
            repository.deleteBudget(1)
        }.exceptionOrNull()

        assertEquals("Delete failed", exception?.message)
        assertEquals(RuntimeException::class, exception?.let { it::class })
    }

    @Test
    fun `deleteEntriesByIds delegates to BudgetEntryRepository for each entry`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val entry1 = BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1")
        val entry2 = BudgetEntry(id = 2, budgetId = 1, amount = "200.0", description = "Entry 2")
        val entry3 = BudgetEntry(id = 3, budgetId = 1, amount = "300.0", description = "Entry 3")
        val entries = listOf(entry1, entry2, entry3)

        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
        }
        val budgetEntryRepository = mockk<BudgetEntryRepository> {
            coEvery { delete(any()) } returns Unit
        }
        val repository = buildRepository(budgetDataSource, entryDataSource, budgetEntryRepository = budgetEntryRepository)

        repository.getBudgetDetailById(1).first()

        repository.deleteEntriesByIds(listOf(1, 2, 3))

        coVerify { budgetEntryRepository.delete(entry1) }
        coVerify { budgetEntryRepository.delete(entry2) }
        coVerify { budgetEntryRepository.delete(entry3) }
    }

    @Test
    fun `deleteEntriesByIds handles empty list without making any calls`() = runTest {
        val budgetDataSource = mockk<BudgetLocalDataSource>(relaxed = true)
        val entryDataSource = mockk<BudgetEntryLocalDataSource>(relaxed = true)
        val budgetEntryRepository = mockk<BudgetEntryRepository>(relaxed = true)
        val repository = buildRepository(budgetDataSource, entryDataSource, budgetEntryRepository = budgetEntryRepository)

        repository.deleteEntriesByIds(emptyList())

        coVerify(exactly = 0) { budgetEntryRepository.delete(any()) }
        coVerify(exactly = 0) { entryDataSource.deleteByIds(any()) }
    }

    @Test
    fun `deleteEntriesByIds falls back to local data source for IDs not in cache`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val entry1 = BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1")

        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(listOf(entry1))
            coEvery { deleteByIds(listOf(99L)) } returns Unit
        }
        val budgetEntryRepository = mockk<BudgetEntryRepository> {
            coEvery { delete(any()) } returns Unit
        }
        val repository = buildRepository(budgetDataSource, entryDataSource, budgetEntryRepository = budgetEntryRepository)

        repository.getBudgetDetailById(1).first()

        // entry1 is in cache; id 99 is not
        repository.deleteEntriesByIds(listOf(1, 99))

        coVerify { budgetEntryRepository.delete(entry1) }
        coVerify { entryDataSource.deleteByIds(listOf(99L)) }
    }

    @Test
    fun `deleteEntriesByIds handles large list of IDs`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val ids = (1..100).toList()
        val entries = ids.map { BudgetEntry(id = it, budgetId = 1, amount = "${it * 10}.0", description = "Entry $it") }

        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
        }
        val budgetEntryRepository = mockk<BudgetEntryRepository> {
            coEvery { delete(any()) } returns Unit
        }
        val repository = buildRepository(budgetDataSource, entryDataSource, budgetEntryRepository = budgetEntryRepository)

        repository.getBudgetDetailById(1).first()

        repository.deleteEntriesByIds(ids)

        coVerify(exactly = 100) { budgetEntryRepository.delete(any()) }
    }

    @Test
    fun `deleteEntriesByIds propagates exception from BudgetEntryRepository`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val entry1 = BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1")

        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(listOf(entry1))
        }
        val budgetEntryRepository = mockk<BudgetEntryRepository> {
            coEvery { delete(entry1) } throws IllegalStateException("Delete entries failed")
        }
        val repository = buildRepository(budgetDataSource, entryDataSource, budgetEntryRepository = budgetEntryRepository)

        repository.getBudgetDetailById(1).first()

        val exception = kotlin.runCatching {
            repository.deleteEntriesByIds(listOf(1))
        }.exceptionOrNull()

        assertEquals("Delete entries failed", exception?.message)
        assertEquals(IllegalStateException::class, exception?.let { it::class })
    }

    @Test
    fun `syncEntries uses provided budgetId and serverId`() = runTest {
        val budgetDataSource = mockk<BudgetLocalDataSource>(relaxed = true)
        val entryDataSource = mockk<BudgetEntryLocalDataSource>(relaxed = true)
        val budgetEntryRepository = mockk<BudgetEntryRepository> {
            coEvery { sync(42, 99L) } returns Result.success(Unit)
        }
        val repository = buildRepository(budgetDataSource, entryDataSource, budgetEntryRepository = budgetEntryRepository)

        val result = repository.syncEntries(budgetId = 42, serverId = 99L)

        assertTrue(result.isSuccess)
        coVerify { budgetEntryRepository.sync(42, 99L) }
    }

    @Test
    fun `syncEntries uses cached budget when no parameters provided`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0, serverId = 100L)
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
        }
        val budgetEntryRepository = mockk<BudgetEntryRepository> {
            coEvery { sync(1, 100L) } returns Result.success(Unit)
        }
        val repository = buildRepository(budgetDataSource, entryDataSource, budgetEntryRepository = budgetEntryRepository)

        repository.getBudgetDetailById(1).first()

        val result = repository.syncEntries()

        assertTrue(result.isSuccess)
        coVerify { budgetEntryRepository.sync(1, 100L) }
    }

    @Test
    fun `syncEntries returns failure when cached budget has no serverId`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0, serverId = null)
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
        }
        val repository = buildRepository(budgetDataSource, entryDataSource)

        repository.getBudgetDetailById(1).first()

        val result = repository.syncEntries()

        assertTrue(result.isFailure)
        assertEquals("Budget must sync before syncing entries", result.exceptionOrNull()?.message)
    }

    @Test
    fun `getBudgetDetailById handles multiple entries`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val entries = (1..10).map {
            BudgetEntry(id = it, budgetId = 1, amount = "${it * 100}.0", description = "Entry $it")
        }
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
        }
        val repository = buildRepository(budgetDataSource, entryDataSource)

        val result = repository.getBudgetDetailById(1).first()

        assertEquals(10, result.entries.size)
    }

    @Test
    fun `getCachedDetail returns default when no data collected`() = runTest {
        BudgetDetailRepository.clearCache()

        val budgetDataSource = mockk<BudgetLocalDataSource>(relaxed = true)
        val entryDataSource = mockk<BudgetEntryLocalDataSource>(relaxed = true)
        val repository = buildRepository(budgetDataSource, entryDataSource)

        val result = repository.getCachedDetail()

        assertEquals(BudgetDetail(), result)
    }

    @Test
    fun `getAllFilteredBy filters by description`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val filteredEntries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Grocery shopping")
        )
        val filter = BudgetEntryFilter(description = "Grocery")
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
            coEvery { getAllFilteredBy(filter) } returns filteredEntries
        }
        val repository = buildRepository(budgetDataSource, entryDataSource)

        repository.getBudgetDetailById(1).first()

        val result = repository.getAllFilteredBy(filter)

        assertEquals(1, result.entries.size)
        assertEquals("Grocery shopping", result.entries[0].description)
    }

    @Test
    fun `getAllFilteredBy filters by type`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val filteredEntries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Income", type = BudgetEntry.Type.INCOME)
        )
        val filter = BudgetEntryFilter(type = BudgetEntry.Type.INCOME)
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
            coEvery { getAllFilteredBy(filter) } returns filteredEntries
        }
        val repository = buildRepository(budgetDataSource, entryDataSource)

        repository.getBudgetDetailById(1).first()

        val result = repository.getAllFilteredBy(filter)

        assertEquals(1, result.entries.size)
        assertEquals(BudgetEntry.Type.INCOME, result.entries[0].type)
    }

    @Test
    fun `getAllFilteredBy filters by date range`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val filteredEntries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry", date = "2024-02-15")
        )
        val filter = BudgetEntryFilter(startDate = "2024-02-01", endDate = "2024-02-28")
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
            coEvery { getAllFilteredBy(filter) } returns filteredEntries
        }
        val repository = buildRepository(budgetDataSource, entryDataSource)

        repository.getBudgetDetailById(1).first()

        val result = repository.getAllFilteredBy(filter)

        assertEquals(1, result.entries.size)
        assertEquals("2024-02-15", result.entries[0].date)
    }

    @Test
    fun `getAllFilteredBy propagates exception from data source`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val filter = BudgetEntryFilter(category = BudgetEntry.Category.FOOD)
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
            coEvery { getAllFilteredBy(filter) } throws Exception("Filter error")
        }
        val repository = buildRepository(budgetDataSource, entryDataSource)

        repository.getBudgetDetailById(1).first()

        val exception = kotlin.runCatching {
            repository.getAllFilteredBy(filter)
        }.exceptionOrNull()

        assertEquals("Filter error", exception?.message)
        assertEquals(Exception::class, exception?.let { it::class })
    }

    @Test
    fun `getBudgetDetailById combines budget with entries of different categories`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Food", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 2, budgetId = 1, amount = "200.0", description = "Transport", category = BudgetEntry.Category.TRANSPORTATION)
        )
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
        }
        val repository = buildRepository(budgetDataSource, entryDataSource)

        val result = repository.getBudgetDetailById(1).first()

        assertEquals(2, result.entries.size)
        assertEquals(BudgetEntry.Category.FOOD, result.entries[0].category)
        assertEquals(BudgetEntry.Category.TRANSPORTATION, result.entries[1].category)
    }

    @Test
    fun `getCachedDetail can be called concurrently`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
        }
        val repository = buildRepository(budgetDataSource, entryDataSource)

        repository.getBudgetDetailById(1).first()

        val results = List(10) {
            async {
                repository.getCachedDetail()
            }
        }.map { it.await() }

        assertEquals(10, results.size)
        results.forEach { result ->
            assertEquals(budget, result.budget)
        }
    }

    @Test
    fun `cache is updated atomically when collecting flow`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1")
        )
        val budgetDataSource = mockk<BudgetLocalDataSource> {
            coEvery { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
        }
        val repository = buildRepository(budgetDataSource, entryDataSource)

        val result = repository.getBudgetDetailById(1).first()
        val cached = repository.getCachedDetail()

        assertEquals(result, cached)
        assertEquals(budget, cached.budget)
        assertEquals(entries, cached.entries)
    }
}
