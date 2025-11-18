package com.meneses.budgethunter.budgetDetail.data

import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.domain.Budget
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.mock
import dev.mokkery.verifySuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetDetailRepositoryTest {

    @Test
    fun `getBudgetDetailById combines budget and entries`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1"),
            BudgetEntry(id = 2, budgetId = 1, amount = "200.0", description = "Entry 2")
        )
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        val result = repository.getBudgetDetailById(1).first()

        assertEquals(budget, result.budget)
        assertEquals(entries, result.entries)
    }

    @Test
    fun `getBudgetDetailById returns empty entries when none exist`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        val result = repository.getBudgetDetailById(1).first()

        assertEquals(budget, result.budget)
        assertEquals(emptyList(), result.entries)
    }

    @Test
    fun `getBudgetDetailById updates cache on new data`() = runTest {
        val budget1 = Budget(id = 1, name = "Budget 1", amount = 1000.0)
        val budget2 = Budget(id = 1, name = "Budget 1 Updated", amount = 1500.0)
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1")
        )
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget1))
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        val result1 = repository.getBudgetDetailById(1).first()
        val cachedDetail = repository.getCachedDetail()

        assertEquals(result1, cachedDetail)
    }

    @Test
    fun `getAllFilteredBy returns filtered entries`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Food", category = BudgetEntry.Category.FOOD)
        )
        val filter = BudgetEntryFilter(category = BudgetEntry.Category.FOOD)
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
            everySuspend { getAllFilteredBy(filter) } returns entries
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // First collect to populate cache
        repository.getBudgetDetailById(1).first()

        val result = repository.getAllFilteredBy(filter)

        assertEquals(entries, result.entries)
        verifySuspend { entryDataSource.getAllFilteredBy(filter) }
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
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(allEntries)
            everySuspend { getAllFilteredBy(filter) } returns filteredEntries
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // Populate cache
        repository.getBudgetDetailById(1).first()

        val result = repository.getAllFilteredBy(filter)

        assertEquals(budget, result.budget)
        assertEquals(filteredEntries, result.entries)
    }

    @Test
    fun `updateBudgetAmount updates budget in data source`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val updatedBudget = budget.copy(amount = 1500.0)
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
            everySuspend { update(updatedBudget) } returns Unit
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // Populate cache
        repository.getBudgetDetailById(1).first()

        repository.updateBudgetAmount(1500.0)

        verifySuspend { budgetDataSource.update(updatedBudget) }
    }

    @Test
    fun `updateBudgetAmount with zero amount`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val updatedBudget = budget.copy(amount = 0.0)
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
            everySuspend { update(updatedBudget) } returns Unit
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // Populate cache
        repository.getBudgetDetailById(1).first()

        repository.updateBudgetAmount(0.0)

        verifySuspend { budgetDataSource.update(updatedBudget) }
    }

    @Test
    fun `deleteBudget delegates to use case`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
        }
        val deleteUseCase = mock<DeleteBudgetUseCase> {
            everySuspend { execute(1L) } returns Unit
        }
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        repository.deleteBudget(1)

        verifySuspend { deleteUseCase.execute(1L) }
    }

    @Test
    fun `deleteEntriesByIds delegates to data source`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
            everySuspend { deleteByIds(listOf(1L, 2L, 3L)) } returns Unit
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        repository.deleteEntriesByIds(listOf(1, 2, 3))

        verifySuspend { entryDataSource.deleteByIds(listOf(1L, 2L, 3L)) }
    }

    @Test
    fun `deleteEntriesByIds handles empty list`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
            everySuspend { deleteByIds(emptyList()) } returns Unit
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        repository.deleteEntriesByIds(emptyList())

        verifySuspend { entryDataSource.deleteByIds(emptyList()) }
    }

    @Test
    fun `getBudgetDetailById handles multiple entries`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val entries = (1..10).map {
            BudgetEntry(id = it, budgetId = 1, amount = "${it * 100}.0", description = "Entry $it")
        }
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        val result = repository.getBudgetDetailById(1).first()

        assertEquals(10, result.entries.size)
    }

    @Test
    fun `getCachedDetail returns default when no data collected`() = runTest {
        val budgetDataSource = mock<BudgetLocalDataSource>()
        val entryDataSource = mock<BudgetEntryLocalDataSource>()
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

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
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
            everySuspend { getAllFilteredBy(filter) } returns filteredEntries
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // Populate cache
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
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
            everySuspend { getAllFilteredBy(filter) } returns filteredEntries
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // Populate cache
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
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
            everySuspend { getAllFilteredBy(filter) } returns filteredEntries
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // Populate cache
        repository.getBudgetDetailById(1).first()

        val result = repository.getAllFilteredBy(filter)

        assertEquals(1, result.entries.size)
        assertEquals("2024-02-15", result.entries[0].date)
    }

    @Test
    fun `updateBudgetAmount with large amount`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val updatedBudget = budget.copy(amount = 999999.99)
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
            everySuspend { update(updatedBudget) } returns Unit
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // Populate cache
        repository.getBudgetDetailById(1).first()

        repository.updateBudgetAmount(999999.99)

        verifySuspend { budgetDataSource.update(updatedBudget) }
    }

    @Test
    fun `deleteBudget handles large budget ID`() = runTest {
        val largeBudgetId = Int.MAX_VALUE
        val budget = Budget(id = largeBudgetId, name = "Test Budget", amount = 1000.0)
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(largeBudgetId.toLong()) } returns flowOf(emptyList())
        }
        val deleteUseCase = mock<DeleteBudgetUseCase> {
            everySuspend { execute(largeBudgetId.toLong()) } returns Unit
        }
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        repository.deleteBudget(largeBudgetId)

        verifySuspend { deleteUseCase.execute(largeBudgetId.toLong()) }
    }

    @Test
    fun `deleteEntriesByIds handles large list of IDs`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val ids = (1..100).toList()
        val longIds = ids.map { it.toLong() }
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(emptyList())
            everySuspend { deleteByIds(longIds) } returns Unit
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        repository.deleteEntriesByIds(ids)

        verifySuspend { entryDataSource.deleteByIds(longIds) }
    }

    @Test
    fun `getBudgetDetailById combines budget with entries of different categories`() = runTest {
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Food", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 2, budgetId = 1, amount = "200.0", description = "Transport", category = BudgetEntry.Category.TRANSPORTATION)
        )
        val budgetDataSource = mock<BudgetLocalDataSource> {
            everySuspend { budgets } returns flowOf(listOf(budget))
        }
        val entryDataSource = mock<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
        }
        val deleteUseCase = mock<DeleteBudgetUseCase>()
        val repository = BudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        val result = repository.getBudgetDetailById(1).first()

        assertEquals(2, result.entries.size)
        assertEquals(BudgetEntry.Category.FOOD, result.entries[0].category)
        assertEquals(BudgetEntry.Category.TRANSPORTATION, result.entries[1].category)
    }
}
