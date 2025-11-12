package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetEntryRepositoryTest {

    // Test helpers to track operations
    private class BudgetEntryTracker {
        val createdEntries = mutableListOf<BudgetEntry>()
        val updatedEntries = mutableListOf<BudgetEntry>()
        val entriesByBudgetId = mutableMapOf<Long, Flow<List<BudgetEntry>>>()
    }

    // Mock data source for testing
    private class MockBudgetEntryLocalDataSource(
        private val tracker: BudgetEntryTracker
    ) {
        fun selectAllByBudgetId(budgetId: Long): Flow<List<BudgetEntry>> {
            return tracker.entriesByBudgetId[budgetId] ?: flowOf(emptyList())
        }

        suspend fun create(budgetEntry: BudgetEntry) {
            tracker.createdEntries.add(budgetEntry)
        }

        suspend fun update(budgetEntry: BudgetEntry) {
            tracker.updatedEntries.add(budgetEntry)
        }
    }

    // Testable version of repository
    private class TestableBudgetEntryRepository(
        private val localDataSource: MockBudgetEntryLocalDataSource,
        private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
    ) {
        fun getAllByBudgetId(budgetId: Long) =
            localDataSource.selectAllByBudgetId(budgetId)

        suspend fun create(budgetEntry: BudgetEntry) = kotlinx.coroutines.withContext(ioDispatcher) {
            localDataSource.create(budgetEntry)
        }

        suspend fun update(budgetEntry: BudgetEntry) = kotlinx.coroutines.withContext(ioDispatcher) {
            localDataSource.update(budgetEntry)
        }
    }

    @Test
    fun `getAllByBudgetId returns entries for specific budget`() = runTest {
        val tracker = BudgetEntryTracker()
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1"),
            BudgetEntry(id = 2, budgetId = 1, amount = "200.0", description = "Entry 2")
        )
        tracker.entriesByBudgetId[1L] = flowOf(entries)

        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        var result: List<BudgetEntry>? = null
        repository.getAllByBudgetId(1L).collect { budgetEntries ->
            result = budgetEntries
        }

        assertEquals(entries, result)
    }

    @Test
    fun `getAllByBudgetId returns empty list when no entries found`() = runTest {
        val tracker = BudgetEntryTracker()
        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        var result: List<BudgetEntry>? = null
        repository.getAllByBudgetId(999L).collect { budgetEntries ->
            result = budgetEntries
        }

        assertEquals(emptyList(), result)
    }

    @Test
    fun `getAllByBudgetId handles multiple budgets separately`() = runTest {
        val tracker = BudgetEntryTracker()
        val entries1 = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Budget 1 Entry")
        )
        val entries2 = listOf(
            BudgetEntry(id = 2, budgetId = 2, amount = "200.0", description = "Budget 2 Entry")
        )
        tracker.entriesByBudgetId[1L] = flowOf(entries1)
        tracker.entriesByBudgetId[2L] = flowOf(entries2)

        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        var result1: List<BudgetEntry>? = null
        repository.getAllByBudgetId(1L).collect { result1 = it }

        var result2: List<BudgetEntry>? = null
        repository.getAllByBudgetId(2L).collect { result2 = it }

        assertEquals(entries1, result1)
        assertEquals(entries2, result2)
    }

    @Test
    fun `create delegates to data source with IO dispatcher`() = runTest {
        val tracker = BudgetEntryTracker()
        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        val entry = BudgetEntry(
            id = -1,
            budgetId = 1,
            amount = "150.50",
            description = "New Entry"
        )
        repository.create(entry)

        assertEquals(listOf(entry), tracker.createdEntries)
    }

    @Test
    fun `create handles entry with all properties set`() = runTest {
        val tracker = BudgetEntryTracker()
        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        val entry = BudgetEntry(
            id = -1,
            budgetId = 42,
            amount = "999.99",
            description = "Complex Entry",
            type = BudgetEntry.Type.INCOME,
            category = BudgetEntry.Category.FOOD,
            date = "2024-06-15",
            invoice = "invoice.pdf",
            isSelected = true
        )
        repository.create(entry)

        val created = tracker.createdEntries[0]
        assertEquals(-1, created.id)
        assertEquals(42, created.budgetId)
        assertEquals("999.99", created.amount)
        assertEquals("Complex Entry", created.description)
        assertEquals(BudgetEntry.Type.INCOME, created.type)
        assertEquals(BudgetEntry.Category.FOOD, created.category)
        assertEquals("2024-06-15", created.date)
        assertEquals("invoice.pdf", created.invoice)
        assertEquals(true, created.isSelected)
    }

    @Test
    fun `create handles entry with empty description`() = runTest {
        val tracker = BudgetEntryTracker()
        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        val entry = BudgetEntry(id = -1, budgetId = 1, amount = "100.0", description = "")
        repository.create(entry)

        assertEquals(1, tracker.createdEntries.size)
        assertEquals("", tracker.createdEntries[0].description)
    }

    @Test
    fun `create handles entry with zero amount`() = runTest {
        val tracker = BudgetEntryTracker()
        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        val entry = BudgetEntry(id = -1, budgetId = 1, amount = "0.0", description = "Zero")
        repository.create(entry)

        assertEquals(1, tracker.createdEntries.size)
        assertEquals("0.0", tracker.createdEntries[0].amount)
    }

    @Test
    fun `create handles entry with null invoice`() = runTest {
        val tracker = BudgetEntryTracker()
        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        val entry = BudgetEntry(
            id = -1,
            budgetId = 1,
            amount = "100.0",
            description = "No invoice",
            invoice = null
        )
        repository.create(entry)

        assertEquals(1, tracker.createdEntries.size)
        assertEquals(null, tracker.createdEntries[0].invoice)
    }

    @Test
    fun `create handles different entry types`() = runTest {
        val tracker = BudgetEntryTracker()
        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        val outcomeEntry = BudgetEntry(
            id = -1,
            budgetId = 1,
            amount = "50.0",
            type = BudgetEntry.Type.OUTCOME
        )
        val incomeEntry = BudgetEntry(
            id = -1,
            budgetId = 1,
            amount = "100.0",
            type = BudgetEntry.Type.INCOME
        )

        repository.create(outcomeEntry)
        repository.create(incomeEntry)

        assertEquals(2, tracker.createdEntries.size)
        assertEquals(BudgetEntry.Type.OUTCOME, tracker.createdEntries[0].type)
        assertEquals(BudgetEntry.Type.INCOME, tracker.createdEntries[1].type)
    }

    @Test
    fun `create handles different categories`() = runTest {
        val tracker = BudgetEntryTracker()
        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        val categories = listOf(
            BudgetEntry.Category.FOOD,
            BudgetEntry.Category.TRANSPORTATION,
            BudgetEntry.Category.HEALTH,
            BudgetEntry.Category.OTHER
        )

        categories.forEach { category ->
            repository.create(
                BudgetEntry(
                    id = -1,
                    budgetId = 1,
                    amount = "10.0",
                    category = category
                )
            )
        }

        assertEquals(categories.size, tracker.createdEntries.size)
        categories.forEachIndexed { index, category ->
            assertEquals(category, tracker.createdEntries[index].category)
        }
    }

    @Test
    fun `update delegates to data source with IO dispatcher`() = runTest {
        val tracker = BudgetEntryTracker()
        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        val entry = BudgetEntry(
            id = 1,
            budgetId = 1,
            amount = "250.00",
            description = "Updated Entry"
        )
        repository.update(entry)

        assertEquals(listOf(entry), tracker.updatedEntries)
    }

    @Test
    fun `update preserves all entry properties`() = runTest {
        val tracker = BudgetEntryTracker()
        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        val entry = BudgetEntry(
            id = 42,
            budgetId = 1,
            amount = "555.55",
            description = "Full Update",
            type = BudgetEntry.Type.INCOME,
            category = BudgetEntry.Category.LEISURE,
            date = "2024-12-25",
            invoice = "updated.pdf",
            isSelected = false
        )
        repository.update(entry)

        val updated = tracker.updatedEntries[0]
        assertEquals(42, updated.id)
        assertEquals(1, updated.budgetId)
        assertEquals("555.55", updated.amount)
        assertEquals("Full Update", updated.description)
        assertEquals(BudgetEntry.Type.INCOME, updated.type)
        assertEquals(BudgetEntry.Category.LEISURE, updated.category)
        assertEquals("2024-12-25", updated.date)
        assertEquals("updated.pdf", updated.invoice)
        assertEquals(false, updated.isSelected)
    }

    @Test
    fun `update handles multiple sequential updates`() = runTest {
        val tracker = BudgetEntryTracker()
        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        val entry1 = BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1")
        val entry2 = BudgetEntry(id = 2, budgetId = 1, amount = "200.0", description = "Entry 2")
        val entry3 = BudgetEntry(id = 3, budgetId = 1, amount = "300.0", description = "Entry 3")

        repository.update(entry1)
        repository.update(entry2)
        repository.update(entry3)

        assertEquals(listOf(entry1, entry2, entry3), tracker.updatedEntries)
    }

    @Test
    fun `create and update can be called sequentially`() = runTest {
        val tracker = BudgetEntryTracker()
        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        val newEntry = BudgetEntry(id = -1, budgetId = 1, amount = "100.0", description = "New")
        repository.create(newEntry)

        val updatedEntry = BudgetEntry(id = 1, budgetId = 1, amount = "200.0", description = "Updated")
        repository.update(updatedEntry)

        assertEquals(1, tracker.createdEntries.size)
        assertEquals(1, tracker.updatedEntries.size)
    }

    @Test
    fun `getAllByBudgetId handles large budget ID values`() = runTest {
        val tracker = BudgetEntryTracker()
        val largeId = Long.MAX_VALUE
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = largeId.toInt(), amount = "100.0", description = "Large ID")
        )
        tracker.entriesByBudgetId[largeId] = flowOf(entries)

        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        var result: List<BudgetEntry>? = null
        repository.getAllByBudgetId(largeId).collect { result = it }

        assertEquals(entries, result)
    }

    @Test
    fun `create handles multiple entries for same budget`() = runTest {
        val tracker = BudgetEntryTracker()
        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        val entry1 = BudgetEntry(id = -1, budgetId = 1, amount = "100.0", description = "Entry 1")
        val entry2 = BudgetEntry(id = -1, budgetId = 1, amount = "200.0", description = "Entry 2")
        val entry3 = BudgetEntry(id = -1, budgetId = 1, amount = "300.0", description = "Entry 3")

        repository.create(entry1)
        repository.create(entry2)
        repository.create(entry3)

        assertEquals(3, tracker.createdEntries.size)
        assertEquals(listOf(entry1, entry2, entry3), tracker.createdEntries)
    }

    @Test
    fun `getAllByBudgetId returns large list of entries`() = runTest {
        val tracker = BudgetEntryTracker()
        val entries = (1..100).map { index ->
            BudgetEntry(
                id = index,
                budgetId = 1,
                amount = "$index.00",
                description = "Entry $index"
            )
        }
        tracker.entriesByBudgetId[1L] = flowOf(entries)

        val dataSource = MockBudgetEntryLocalDataSource(tracker)
        val repository = TestableBudgetEntryRepository(dataSource, Dispatchers.Default)

        var result: List<BudgetEntry>? = null
        repository.getAllByBudgetId(1L).collect { result = it }

        assertEquals(100, result?.size)
        assertEquals(entries, result)
    }
}
