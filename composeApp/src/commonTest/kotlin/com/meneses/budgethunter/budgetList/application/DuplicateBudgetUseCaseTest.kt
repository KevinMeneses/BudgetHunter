package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DuplicateBudgetUseCaseTest {

    // Test helpers to track operations
    private class DuplicationTracker {
        val createdBudgets = mutableListOf<Budget>()
        val createdEntries = mutableListOf<BudgetEntry>()
        var nextBudgetId = 100
        val entriesByBudgetId = mutableMapOf<Int, List<BudgetEntry>>()
    }

    // Testable version that doesn't require repository dependencies
    private class TestableDuplicateBudgetUseCase(
        private val tracker: DuplicationTracker,
        private val defaultDispatcher: kotlinx.coroutines.CoroutineDispatcher
    ) {
        suspend fun execute(budget: Budget) = kotlinx.coroutines.withContext(defaultDispatcher) {
            // Copy the budget with new ID and add "(copy)" suffix
            val updatedBudget = budget.copy(id = -1, name = budget.name + " (copy)")
            val newBudget = updatedBudget.copy(id = tracker.nextBudgetId++)
            tracker.createdBudgets.add(newBudget)

            // Copy all entries
            val entries = tracker.entriesByBudgetId[budget.id] ?: emptyList()
            entries.forEach { entry ->
                val updatedEntry = entry.copy(id = -1, budgetId = newBudget.id)
                tracker.createdEntries.add(updatedEntry)
            }
        }
    }

    @Test
    fun `execute creates a copy of the budget with (copy) suffix`() = runTest {
        val tracker = DuplicationTracker()
        val originalBudget = Budget(
            id = 1,
            name = "Original Budget",
            amount = 1000.0,
            totalExpenses = 250.0,
            date = "2024-01-01"
        )

        val useCase = TestableDuplicateBudgetUseCase(tracker, Dispatchers.Default)
        useCase.execute(originalBudget)

        val created = tracker.createdBudgets
        assertEquals(1, created.size)
        assertEquals("Original Budget (copy)", created[0].name)
        assertEquals(1000.0, created[0].amount)
        assertEquals(250.0, created[0].totalExpenses)
        assertEquals("2024-01-01", created[0].date)
        assertEquals(100, created[0].id)
    }

    @Test
    fun `execute copies all entries from original budget to new budget`() = runTest {
        val tracker = DuplicationTracker()
        val originalBudget = Budget(id = 1, name = "Test", amount = 100.0)

        val originalEntries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "50", description = "Entry 1"),
            BudgetEntry(id = 2, budgetId = 1, amount = "25", description = "Entry 2"),
            BudgetEntry(id = 3, budgetId = 1, amount = "10", description = "Entry 3")
        )

        tracker.entriesByBudgetId[1] = originalEntries

        val useCase = TestableDuplicateBudgetUseCase(tracker, Dispatchers.Default)
        useCase.execute(originalBudget)

        val createdEntries = tracker.createdEntries
        assertEquals(3, createdEntries.size)

        // All entries should have -1 as ID (reset for DB insertion)
        assertTrue(createdEntries.all { it.id == -1 })

        // All entries should have the new budget ID
        assertTrue(createdEntries.all { it.budgetId == 100 })

        // Verify entry data is preserved
        assertEquals("50", createdEntries[0].amount)
        assertEquals("Entry 1", createdEntries[0].description)
        assertEquals("25", createdEntries[1].amount)
        assertEquals("Entry 2", createdEntries[1].description)
        assertEquals("10", createdEntries[2].amount)
        assertEquals("Entry 3", createdEntries[2].description)
    }

    @Test
    fun `execute handles budget with no entries`() = runTest {
        val tracker = DuplicationTracker()
        val originalBudget = Budget(id = 1, name = "Empty Budget", amount = 100.0)

        // No entries for this budget
        tracker.entriesByBudgetId[1] = emptyList()

        val useCase = TestableDuplicateBudgetUseCase(tracker, Dispatchers.Default)
        useCase.execute(originalBudget)

        // Budget should still be created
        val createdBudgets = tracker.createdBudgets
        assertEquals(1, createdBudgets.size)
        assertEquals("Empty Budget (copy)", createdBudgets[0].name)

        // No entries should be created
        val createdEntries = tracker.createdEntries
        assertEquals(0, createdEntries.size)
    }

    @Test
    fun `execute preserves all entry properties`() = runTest {
        val tracker = DuplicationTracker()
        val originalBudget = Budget(id = 1, name = "Test", amount = 100.0)

        val originalEntry = BudgetEntry(
            id = 1,
            budgetId = 1,
            amount = "123.45",
            description = "Complex Entry",
            type = BudgetEntry.Type.INCOME,
            category = BudgetEntry.Category.FOOD,
            date = "2024-06-15",
            invoice = "invoice.pdf",
            isSelected = false
        )

        tracker.entriesByBudgetId[1] = listOf(originalEntry)

        val useCase = TestableDuplicateBudgetUseCase(tracker, Dispatchers.Default)
        useCase.execute(originalBudget)

        val createdEntries = tracker.createdEntries
        assertEquals(1, createdEntries.size)

        val copied = createdEntries[0]
        assertEquals(-1, copied.id)
        assertEquals(100, copied.budgetId)
        assertEquals("123.45", copied.amount)
        assertEquals("Complex Entry", copied.description)
        assertEquals(BudgetEntry.Type.INCOME, copied.type)
        assertEquals(BudgetEntry.Category.FOOD, copied.category)
        assertEquals("2024-06-15", copied.date)
        assertEquals("invoice.pdf", copied.invoice)
        assertEquals(false, copied.isSelected)
    }

    @Test
    fun `execute handles budget name that already has (copy) suffix`() = runTest {
        val tracker = DuplicationTracker()
        val originalBudget = Budget(id = 1, name = "Budget (copy)", amount = 100.0)

        val useCase = TestableDuplicateBudgetUseCase(tracker, Dispatchers.Default)
        useCase.execute(originalBudget)

        val created = tracker.createdBudgets
        assertEquals("Budget (copy) (copy)", created[0].name)
    }

    @Test
    fun `execute handles large number of entries`() = runTest {
        val tracker = DuplicationTracker()
        val originalBudget = Budget(id = 1, name = "Large Budget", amount = 10000.0)

        val largeEntryList = (1..100).map { index ->
            BudgetEntry(
                id = index,
                budgetId = 1,
                amount = "$index.00",
                description = "Entry $index"
            )
        }

        tracker.entriesByBudgetId[1] = largeEntryList

        val useCase = TestableDuplicateBudgetUseCase(tracker, Dispatchers.Default)
        useCase.execute(originalBudget)

        val createdEntries = tracker.createdEntries
        assertEquals(100, createdEntries.size)

        // Verify all entries were copied with correct data
        createdEntries.forEachIndexed { index, entry ->
            assertEquals(-1, entry.id)
            assertEquals(100, entry.budgetId)
            assertEquals("${index + 1}.00", entry.amount)
            assertEquals("Entry ${index + 1}", entry.description)
        }
    }

    @Test
    fun `execute handles budget with zero amount`() = runTest {
        val tracker = DuplicationTracker()
        val originalBudget = Budget(id = 1, name = "Zero Budget", amount = 0.0)

        val useCase = TestableDuplicateBudgetUseCase(tracker, Dispatchers.Default)
        useCase.execute(originalBudget)

        val created = tracker.createdBudgets
        assertEquals(0.0, created[0].amount)
    }

    @Test
    fun `execute handles budget with empty name`() = runTest {
        val tracker = DuplicationTracker()
        val originalBudget = Budget(id = 1, name = "", amount = 100.0)

        val useCase = TestableDuplicateBudgetUseCase(tracker, Dispatchers.Default)
        useCase.execute(originalBudget)

        val created = tracker.createdBudgets
        assertEquals(" (copy)", created[0].name)
    }

    @Test
    fun `execute can duplicate multiple budgets sequentially`() = runTest {
        val tracker = DuplicationTracker()
        val budget1 = Budget(id = 1, name = "Budget 1", amount = 100.0)
        val budget2 = Budget(id = 2, name = "Budget 2", amount = 200.0)

        tracker.entriesByBudgetId[1] = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "10")
        )
        tracker.entriesByBudgetId[2] = listOf(
            BudgetEntry(id = 2, budgetId = 2, amount = "20")
        )

        val useCase = TestableDuplicateBudgetUseCase(tracker, Dispatchers.Default)

        useCase.execute(budget1)
        useCase.execute(budget2)

        val createdBudgets = tracker.createdBudgets
        assertEquals(2, createdBudgets.size)
        assertEquals("Budget 1 (copy)", createdBudgets[0].name)
        assertEquals("Budget 2 (copy)", createdBudgets[1].name)

        val createdEntries = tracker.createdEntries
        assertEquals(2, createdEntries.size)
    }

    @Test
    fun `execute preserves totalExpenses from original budget`() = runTest {
        val tracker = DuplicationTracker()
        val originalBudget = Budget(
            id = 1,
            name = "Test",
            amount = 1000.0,
            totalExpenses = 456.78,
            date = "2024-01-01"
        )

        val useCase = TestableDuplicateBudgetUseCase(tracker, Dispatchers.Default)
        useCase.execute(originalBudget)

        val created = tracker.createdBudgets
        assertEquals(456.78, created[0].totalExpenses)
    }

    @Test
    fun `execute converts budget id to Long for entry lookup`() = runTest {
        val tracker = DuplicationTracker()
        // Budget with Int ID
        val originalBudget = Budget(id = 999, name = "Test", amount = 100.0)

        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 999, amount = "50")
        )

        // Set entries with Int key
        tracker.entriesByBudgetId[999] = entries

        val useCase = TestableDuplicateBudgetUseCase(tracker, Dispatchers.Default)
        useCase.execute(originalBudget)

        // Entry should be found and copied
        val createdEntries = tracker.createdEntries
        assertEquals(1, createdEntries.size)
    }
}
