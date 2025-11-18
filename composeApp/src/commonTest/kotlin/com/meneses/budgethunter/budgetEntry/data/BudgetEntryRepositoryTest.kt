package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
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

class BudgetEntryRepositoryTest {

    @Test
    fun `getAllByBudgetId returns entries for specific budget`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1"),
            BudgetEntry(id = 2, budgetId = 1, amount = "200.0", description = "Entry 2")
        )
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        val result = repository.getAllByBudgetId(1L).first()

        assertEquals(entries, result)
    }

    @Test
    fun `getAllByBudgetId returns empty list when no entries found`() = runTest {
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(999L) } returns flowOf(emptyList())
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        val result = repository.getAllByBudgetId(999L).first()

        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllByBudgetId handles multiple budgets separately`() = runTest {
        val entries1 = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Budget 1 Entry")
        )
        val entries2 = listOf(
            BudgetEntry(id = 2, budgetId = 2, amount = "200.0", description = "Budget 2 Entry")
        )
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries1)
            every { selectAllByBudgetId(2L) } returns flowOf(entries2)
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        val result1 = repository.getAllByBudgetId(1L).first()
        val result2 = repository.getAllByBudgetId(2L).first()

        assertEquals(entries1, result1)
        assertEquals(entries2, result2)
    }

    @Test
    fun `create delegates to data source with IO dispatcher`() = runTest {
        val entry = BudgetEntry(id = -1, budgetId = 1, amount = "100.0", description = "New Entry")
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            coEvery { create(entry) } returns Unit
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        repository.create(entry)

        coVerify { dataSource.create(entry) }
    }

    @Test
    fun `create handles entry with zero amount`() = runTest {
        val entry = BudgetEntry(id = -1, budgetId = 1, amount = "0.0", description = "Zero Entry")
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            coEvery { create(entry) } returns Unit
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        repository.create(entry)

        coVerify { dataSource.create(entry) }
    }

    @Test
    fun `create handles entry with empty description`() = runTest {
        val entry = BudgetEntry(id = -1, budgetId = 1, amount = "100.0", description = "")
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            coEvery { create(entry) } returns Unit
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        repository.create(entry)

        coVerify { dataSource.create(entry) }
    }

    @Test
    fun `create handles entry with large amount`() = runTest {
        val entry = BudgetEntry(id = -1, budgetId = 1, amount = "999999.99", description = "Large Amount")
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            coEvery { create(entry) } returns Unit
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        repository.create(entry)

        coVerify { dataSource.create(entry) }
    }

    @Test
    fun `update delegates to data source with IO dispatcher`() = runTest {
        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "150.0", description = "Updated Entry")
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            coEvery { update(entry) } returns Unit
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        repository.update(entry)

        coVerify { dataSource.update(entry) }
    }

    @Test
    fun `update handles multiple sequential updates`() = runTest {
        val entry1 = BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1")
        val entry2 = BudgetEntry(id = 2, budgetId = 1, amount = "200.0", description = "Entry 2")
        val entry3 = BudgetEntry(id = 3, budgetId = 1, amount = "300.0", description = "Entry 3")
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            coEvery { update(entry1) } returns Unit
            coEvery { update(entry2) } returns Unit
            coEvery { update(entry3) } returns Unit
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        repository.update(entry1)
        repository.update(entry2)
        repository.update(entry3)

        verifySuspend {
            dataSource.update(entry1)
            dataSource.update(entry2)
            dataSource.update(entry3)
        }
    }

    @Test
    fun `update preserves all entry properties`() = runTest {
        val entry = BudgetEntry(
            id = 42,
            budgetId = 1,
            amount = "1234.56",
            description = "Complex Entry",
            type = BudgetEntry.Type.EXPENSE,
            category = BudgetEntry.Category.FOOD,
            date = "2024-06-15",
            invoice = "INV-001"
        )
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            coEvery { update(entry) } returns Unit
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        repository.update(entry)

        coVerify { dataSource.update(entry) }
    }

    @Test
    fun `create and update can be called sequentially`() = runTest {
        val newEntry = BudgetEntry(id = -1, budgetId = 1, amount = "100.0", description = "New")
        val updatedEntry = BudgetEntry(id = 1, budgetId = 1, amount = "200.0", description = "Updated")
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            coEvery { create(newEntry) } returns Unit
            coEvery { update(updatedEntry) } returns Unit
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        repository.create(newEntry)
        repository.update(updatedEntry)

        verifySuspend {
            dataSource.create(newEntry)
            dataSource.update(updatedEntry)
        }
    }

    @Test
    fun `getAllByBudgetId handles large budget ID`() = runTest {
        val largeBudgetId = Long.MAX_VALUE
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = largeBudgetId, amount = "100.0", description = "Entry")
        )
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(largeBudgetId) } returns flowOf(entries)
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        val result = repository.getAllByBudgetId(largeBudgetId).first()

        assertEquals(entries, result)
    }

    @Test
    fun `getAllByBudgetId returns entries with different types`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Income", type = BudgetEntry.Type.INCOME),
            BudgetEntry(id = 2, budgetId = 1, amount = "50.0", description = "Expense", type = BudgetEntry.Type.EXPENSE)
        )
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        val result = repository.getAllByBudgetId(1L).first()

        assertEquals(2, result.size)
        assertEquals(BudgetEntry.Type.INCOME, result[0].type)
        assertEquals(BudgetEntry.Type.EXPENSE, result[1].type)
    }

    @Test
    fun `getAllByBudgetId returns entries with different categories`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Food", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 2, budgetId = 1, amount = "50.0", description = "Transport", category = BudgetEntry.Category.TRANSPORTATION)
        )
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        val result = repository.getAllByBudgetId(1L).first()

        assertEquals(2, result.size)
        assertEquals(BudgetEntry.Category.FOOD, result[0].category)
        assertEquals(BudgetEntry.Category.TRANSPORTATION, result[1].category)
    }

    @Test
    fun `create handles entry with invoice`() = runTest {
        val entry = BudgetEntry(
            id = -1,
            budgetId = 1,
            amount = "100.0",
            description = "Purchase",
            invoice = "INV-12345"
        )
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            coEvery { create(entry) } returns Unit
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        repository.create(entry)

        coVerify { dataSource.create(entry) }
    }

    @Test
    fun `update handles entry changing budget`() = runTest {
        val entry = BudgetEntry(id = 1, budgetId = 2, amount = "100.0", description = "Moved Entry")
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            coEvery { update(entry) } returns Unit
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        repository.update(entry)

        coVerify { dataSource.update(entry) }
    }

    @Test
    fun `create handles entry with all fields populated`() = runTest {
        val entry = BudgetEntry(
            id = -1,
            budgetId = 1,
            amount = "250.75",
            description = "Complete Entry",
            type = BudgetEntry.Type.EXPENSE,
            category = BudgetEntry.Category.ENTERTAINMENT,
            date = "2024-07-20",
            invoice = "INV-999"
        )
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            coEvery { create(entry) } returns Unit
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        repository.create(entry)

        coVerify { dataSource.create(entry) }
    }

    @Test
    fun `getAllByBudgetId handles entries with dates`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1", date = "2024-01-01"),
            BudgetEntry(id = 2, budgetId = 1, amount = "200.0", description = "Entry 2", date = "2024-01-15")
        )
        val dataSource = mockk<BudgetEntryLocalDataSource> {
            every { selectAllByBudgetId(1L) } returns flowOf(entries)
        }
        val repository = BudgetEntryRepository(dataSource, Dispatchers.Default)

        val result = repository.getAllByBudgetId(1L).first()

        assertEquals(2, result.size)
        assertEquals("2024-01-01", result[0].date)
        assertEquals("2024-01-15", result[1].date)
    }
}
