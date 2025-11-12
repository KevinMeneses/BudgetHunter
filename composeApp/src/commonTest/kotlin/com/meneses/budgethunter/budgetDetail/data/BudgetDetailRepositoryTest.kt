package com.meneses.budgethunter.budgetDetail.data

import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetList.domain.Budget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class BudgetDetailRepositoryTest {

    // Test helpers to track operations
    private class BudgetDetailTracker {
        val updatedBudgets = mutableListOf<Budget>()
        val deletedBudgetIds = mutableListOf<Long>()
        val deletedEntryIdsList = mutableListOf<List<Long>>()
        val filteredEntries = mutableMapOf<BudgetEntryFilter, List<BudgetEntry>>()
    }

    // Mock data sources for testing
    private class MockBudgetLocalDataSource(
        private val tracker: BudgetDetailTracker,
        private val budgetsFlow: MutableStateFlow<List<Budget>>
    ) {
        val budgets: Flow<List<Budget>> = budgetsFlow

        suspend fun update(budget: Budget) {
            tracker.updatedBudgets.add(budget)
        }
    }

    private class MockBudgetEntryLocalDataSource(
        private val tracker: BudgetDetailTracker,
        private val entriesByBudgetId: Map<Long, Flow<List<BudgetEntry>>>
    ) {
        fun selectAllByBudgetId(budgetId: Long): Flow<List<BudgetEntry>> {
            return entriesByBudgetId[budgetId] ?: flowOf(emptyList())
        }

        suspend fun getAllFilteredBy(filter: BudgetEntryFilter): List<BudgetEntry> {
            // Return filtered entries from tracker
            return tracker.filteredEntries[filter] ?: emptyList()
        }

        suspend fun deleteByIds(ids: List<Long>) {
            tracker.deletedEntryIdsList.add(ids)
        }
    }

    private class MockDeleteBudgetUseCase(
        private val tracker: BudgetDetailTracker
    ) {
        suspend fun execute(budgetId: Long) {
            tracker.deletedBudgetIds.add(budgetId)
        }
    }

    // Testable version of repository
    private class TestableBudgetDetailRepository(
        private val budgetLocalDataSource: MockBudgetLocalDataSource,
        private val entriesLocalDataSource: MockBudgetEntryLocalDataSource,
        private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher,
        private val deleteBudgetUseCase: MockDeleteBudgetUseCase
    ) {
        private val cacheMutex = Mutex()
        private var cachedBudgetDetail = BudgetDetail()

        suspend fun getCachedDetail(): BudgetDetail = cacheMutex.withLock {
            cachedBudgetDetail
        }

        fun getBudgetDetailById(budgetId: Int): Flow<BudgetDetail> =
            budgetLocalDataSource.budgets
                .mapNotNull { budgets ->
                    budgets.find { it.id == budgetId }
                }.combine(
                    entriesLocalDataSource.selectAllByBudgetId(budgetId.toLong())
                ) { budget, entries ->
                    BudgetDetail(budget, entries)
                }.onEach {
                    cacheMutex.withLock {
                        if (it != cachedBudgetDetail) {
                            cachedBudgetDetail = it
                        }
                    }
                }

        suspend fun getAllFilteredBy(filter: BudgetEntryFilter): BudgetDetail {
            val cached = getCachedDetail()
            return cached.copy(
                entries = entriesLocalDataSource.getAllFilteredBy(filter)
            )
        }

        suspend fun updateBudgetAmount(amount: Double) = kotlinx.coroutines.withContext(ioDispatcher) {
            val cached = getCachedDetail()
            val budget = cached.budget.copy(amount = amount)
            budgetLocalDataSource.update(budget)
        }

        suspend fun deleteBudget(budgetId: Int) = kotlinx.coroutines.withContext(ioDispatcher) {
            deleteBudgetUseCase.execute(budgetId.toLong())
        }

        suspend fun deleteEntriesByIds(ids: List<Int>) = kotlinx.coroutines.withContext(ioDispatcher) {
            val dbIds = ids.map { it.toLong() }
            entriesLocalDataSource.deleteByIds(dbIds)
        }

        // Extension functions needed for flow operations
        private fun <T> Flow<T>.mapNotNull(transform: suspend (T) -> T?): Flow<T> {
            return kotlinx.coroutines.flow.mapNotNull(this, transform)
        }

        private fun <T1, T2, R> Flow<T1>.combine(
            other: Flow<T2>,
            transform: suspend (T1, T2) -> R
        ): Flow<R> {
            return kotlinx.coroutines.flow.combine(this, other, transform)
        }

        private fun <T> Flow<T>.onEach(action: suspend (T) -> Unit): Flow<T> {
            return kotlinx.coroutines.flow.onEach(this, action)
        }
    }

    @Test
    fun `getCachedDetail returns cached budget detail`() = runTest {
        val tracker = BudgetDetailTracker()
        val budgetsFlow = MutableStateFlow(emptyList<Budget>())
        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, emptyMap())
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        val cached = repository.getCachedDetail()

        assertEquals(BudgetDetail(), cached)
    }

    @Test
    fun `getBudgetDetailById combines budget and entries into BudgetDetail`() = runTest {
        val tracker = BudgetDetailTracker()
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budgetsFlow = MutableStateFlow(listOf(budget))

        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1"),
            BudgetEntry(id = 2, budgetId = 1, amount = "200.0", description = "Entry 2")
        )
        val entriesMap = mapOf(1L to flowOf(entries))

        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, entriesMap)
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
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
    fun `getBudgetDetailById updates cache when detail changes`() = runTest {
        val tracker = BudgetDetailTracker()
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budgetsFlow = MutableStateFlow(listOf(budget))

        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1")
        )
        val entriesMap = mapOf(1L to flowOf(entries))

        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, entriesMap)
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // Collect the flow to trigger cache update
        repository.getBudgetDetailById(1).first()

        val cached = repository.getCachedDetail()

        assertEquals(budget, cached.budget)
        assertEquals(entries, cached.entries)
    }

    @Test
    fun `getBudgetDetailById returns empty entries when budget has no entries`() = runTest {
        val tracker = BudgetDetailTracker()
        val budget = Budget(id = 1, name = "Empty Budget", amount = 500.0)
        val budgetsFlow = MutableStateFlow(listOf(budget))

        val entriesMap = mapOf(1L to flowOf(emptyList<BudgetEntry>()))

        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, entriesMap)
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
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
    fun `getAllFilteredBy returns cached budget with filtered entries`() = runTest {
        val tracker = BudgetDetailTracker()
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budgetsFlow = MutableStateFlow(listOf(budget))

        val allEntries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1"),
            BudgetEntry(id = 2, budgetId = 1, amount = "200.0", description = "Entry 2")
        )
        val entriesMap = mapOf(1L to flowOf(allEntries))

        val filter = BudgetEntryFilter(description = "Entry 1")
        val filteredEntries = listOf(allEntries[0])
        tracker.filteredEntries[filter] = filteredEntries

        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, entriesMap)
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // First, populate the cache
        repository.getBudgetDetailById(1).first()

        // Now get filtered entries
        val result = repository.getAllFilteredBy(filter)

        assertEquals(budget, result.budget)
        assertEquals(filteredEntries, result.entries)
    }

    @Test
    fun `getAllFilteredBy handles empty filter results`() = runTest {
        val tracker = BudgetDetailTracker()
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budgetsFlow = MutableStateFlow(listOf(budget))

        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1")
        )
        val entriesMap = mapOf(1L to flowOf(entries))

        val filter = BudgetEntryFilter(description = "NonExistent")
        tracker.filteredEntries[filter] = emptyList()

        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, entriesMap)
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // Populate cache
        repository.getBudgetDetailById(1).first()

        val result = repository.getAllFilteredBy(filter)

        assertEquals(budget, result.budget)
        assertEquals(emptyList(), result.entries)
    }

    @Test
    fun `updateBudgetAmount updates budget with new amount`() = runTest {
        val tracker = BudgetDetailTracker()
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budgetsFlow = MutableStateFlow(listOf(budget))

        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1")
        )
        val entriesMap = mapOf(1L to flowOf(entries))

        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, entriesMap)
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // Populate cache
        repository.getBudgetDetailById(1).first()

        // Update amount
        repository.updateBudgetAmount(1500.0)

        assertEquals(1, tracker.updatedBudgets.size)
        assertEquals(1500.0, tracker.updatedBudgets[0].amount)
        assertEquals("Test Budget", tracker.updatedBudgets[0].name)
    }

    @Test
    fun `updateBudgetAmount preserves other budget properties`() = runTest {
        val tracker = BudgetDetailTracker()
        val budget = Budget(
            id = 42,
            name = "Complex Budget",
            amount = 1000.0,
            totalExpenses = 250.0,
            date = "2024-06-15"
        )
        val budgetsFlow = MutableStateFlow(listOf(budget))
        val entriesMap = mapOf(42L to flowOf(emptyList<BudgetEntry>()))

        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, entriesMap)
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // Populate cache
        repository.getBudgetDetailById(42).first()

        // Update amount
        repository.updateBudgetAmount(2000.0)

        val updated = tracker.updatedBudgets[0]
        assertEquals(42, updated.id)
        assertEquals("Complex Budget", updated.name)
        assertEquals(2000.0, updated.amount)
        assertEquals(250.0, updated.totalExpenses)
        assertEquals("2024-06-15", updated.date)
    }

    @Test
    fun `updateBudgetAmount handles zero amount`() = runTest {
        val tracker = BudgetDetailTracker()
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budgetsFlow = MutableStateFlow(listOf(budget))
        val entriesMap = mapOf(1L to flowOf(emptyList<BudgetEntry>()))

        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, entriesMap)
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // Populate cache
        repository.getBudgetDetailById(1).first()

        // Update to zero
        repository.updateBudgetAmount(0.0)

        assertEquals(1, tracker.updatedBudgets.size)
        assertEquals(0.0, tracker.updatedBudgets[0].amount)
    }

    @Test
    fun `deleteBudget delegates to delete use case`() = runTest {
        val tracker = BudgetDetailTracker()
        val budgetsFlow = MutableStateFlow(emptyList<Budget>())
        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, emptyMap())
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        repository.deleteBudget(42)

        assertEquals(listOf(42L), tracker.deletedBudgetIds)
    }

    @Test
    fun `deleteBudget handles multiple sequential deletions`() = runTest {
        val tracker = BudgetDetailTracker()
        val budgetsFlow = MutableStateFlow(emptyList<Budget>())
        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, emptyMap())
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        repository.deleteBudget(1)
        repository.deleteBudget(2)
        repository.deleteBudget(3)

        assertEquals(listOf(1L, 2L, 3L), tracker.deletedBudgetIds)
    }

    @Test
    fun `deleteEntriesByIds converts int ids to long and delegates to data source`() = runTest {
        val tracker = BudgetDetailTracker()
        val budgetsFlow = MutableStateFlow(emptyList<Budget>())
        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, emptyMap())
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        repository.deleteEntriesByIds(listOf(1, 2, 3))

        assertEquals(1, tracker.deletedEntryIdsList.size)
        assertEquals(listOf(1L, 2L, 3L), tracker.deletedEntryIdsList[0])
    }

    @Test
    fun `deleteEntriesByIds handles empty list`() = runTest {
        val tracker = BudgetDetailTracker()
        val budgetsFlow = MutableStateFlow(emptyList<Budget>())
        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, emptyMap())
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        repository.deleteEntriesByIds(emptyList())

        assertEquals(1, tracker.deletedEntryIdsList.size)
        assertEquals(emptyList(), tracker.deletedEntryIdsList[0])
    }

    @Test
    fun `deleteEntriesByIds handles single entry`() = runTest {
        val tracker = BudgetDetailTracker()
        val budgetsFlow = MutableStateFlow(emptyList<Budget>())
        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, emptyMap())
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        repository.deleteEntriesByIds(listOf(42))

        assertEquals(1, tracker.deletedEntryIdsList.size)
        assertEquals(listOf(42L), tracker.deletedEntryIdsList[0])
    }

    @Test
    fun `deleteEntriesByIds handles large list of entries`() = runTest {
        val tracker = BudgetDetailTracker()
        val budgetsFlow = MutableStateFlow(emptyList<Budget>())
        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, emptyMap())
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        val ids = (1..100).toList()
        repository.deleteEntriesByIds(ids)

        assertEquals(1, tracker.deletedEntryIdsList.size)
        assertEquals(ids.map { it.toLong() }, tracker.deletedEntryIdsList[0])
    }

    @Test
    fun `getBudgetDetailById does not update cache when detail is unchanged`() = runTest {
        val tracker = BudgetDetailTracker()
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Entry 1")
        )

        val budgetsFlow = MutableStateFlow(listOf(budget))
        val entriesMap = mapOf(1L to flowOf(entries))

        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, entriesMap)
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // First collection
        val first = repository.getBudgetDetailById(1).first()
        val cached1 = repository.getCachedDetail()

        // Should be equal
        assertEquals(first, cached1)
    }

    @Test
    fun `getAllFilteredBy with different filters returns different results`() = runTest {
        val tracker = BudgetDetailTracker()
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budgetsFlow = MutableStateFlow(listOf(budget))

        val allEntries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100.0", description = "Food", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 2, budgetId = 1, amount = "200.0", description = "Transport", category = BudgetEntry.Category.TRANSPORTATION)
        )
        val entriesMap = mapOf(1L to flowOf(allEntries))

        val filter1 = BudgetEntryFilter(category = BudgetEntry.Category.FOOD)
        val filter2 = BudgetEntryFilter(category = BudgetEntry.Category.TRANSPORTATION)
        tracker.filteredEntries[filter1] = listOf(allEntries[0])
        tracker.filteredEntries[filter2] = listOf(allEntries[1])

        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, entriesMap)
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // Populate cache
        repository.getBudgetDetailById(1).first()

        val result1 = repository.getAllFilteredBy(filter1)
        val result2 = repository.getAllFilteredBy(filter2)

        assertNotEquals(result1.entries, result2.entries)
        assertEquals(1, result1.entries.size)
        assertEquals(1, result2.entries.size)
        assertEquals(BudgetEntry.Category.FOOD, result1.entries[0].category)
        assertEquals(BudgetEntry.Category.TRANSPORTATION, result2.entries[0].category)
    }

    @Test
    fun `updateBudgetAmount can be called multiple times`() = runTest {
        val tracker = BudgetDetailTracker()
        val budget = Budget(id = 1, name = "Test Budget", amount = 1000.0)
        val budgetsFlow = MutableStateFlow(listOf(budget))
        val entriesMap = mapOf(1L to flowOf(emptyList<BudgetEntry>()))

        val budgetDataSource = MockBudgetLocalDataSource(tracker, budgetsFlow)
        val entryDataSource = MockBudgetEntryLocalDataSource(tracker, entriesMap)
        val deleteUseCase = MockDeleteBudgetUseCase(tracker)

        val repository = TestableBudgetDetailRepository(
            budgetDataSource,
            entryDataSource,
            Dispatchers.Default,
            deleteUseCase
        )

        // Populate cache
        repository.getBudgetDetailById(1).first()

        // Multiple updates
        repository.updateBudgetAmount(1500.0)
        repository.updateBudgetAmount(2000.0)
        repository.updateBudgetAmount(2500.0)

        assertEquals(3, tracker.updatedBudgets.size)
        assertEquals(1500.0, tracker.updatedBudgets[0].amount)
        assertEquals(2000.0, tracker.updatedBudgets[1].amount)
        assertEquals(2500.0, tracker.updatedBudgets[2].amount)
    }
}
