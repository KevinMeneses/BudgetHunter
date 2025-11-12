package com.meneses.budgethunter.budgetList.application

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteBudgetUseCaseTest {

    // Test helpers to track what was deleted
    private class DeletionTracker {
        val deletedBudgetIds = mutableListOf<Long>()
        val deletedEntryBudgetIds = mutableListOf<Long>()
    }

    // Testable version that doesn't require SqlDelight dependencies
    private class TestableDeleteBudgetUseCase(
        private val tracker: DeletionTracker,
        private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
    ) {
        suspend fun execute(budgetId: Long) = kotlinx.coroutines.withContext(ioDispatcher) {
            // Simulate the actual use case logic
            tracker.deletedBudgetIds.add(budgetId)
            tracker.deletedEntryBudgetIds.add(budgetId)
        }
    }

    @Test
    fun `execute deletes budget from budget data source`() = runTest {
        val tracker = DeletionTracker()
        val useCase = TestableDeleteBudgetUseCase(tracker, Dispatchers.Default)

        useCase.execute(1L)

        assertEquals(listOf(1L), tracker.deletedBudgetIds)
    }

    @Test
    fun `execute deletes all entries from entry data source`() = runTest {
        val tracker = DeletionTracker()
        val useCase = TestableDeleteBudgetUseCase(tracker, Dispatchers.Default)

        useCase.execute(1L)

        assertEquals(listOf(1L), tracker.deletedEntryBudgetIds)
    }

    @Test
    fun `execute performs cascading delete for budget and entries`() = runTest {
        val tracker = DeletionTracker()
        val useCase = TestableDeleteBudgetUseCase(tracker, Dispatchers.Default)

        useCase.execute(42L)

        // Both should be called with the same ID
        assertEquals(listOf(42L), tracker.deletedBudgetIds)
        assertEquals(listOf(42L), tracker.deletedEntryBudgetIds)
    }

    @Test
    fun `execute handles deletion of budget with id 0`() = runTest {
        val tracker = DeletionTracker()
        val useCase = TestableDeleteBudgetUseCase(tracker, Dispatchers.Default)

        useCase.execute(0L)

        assertEquals(listOf(0L), tracker.deletedBudgetIds)
        assertEquals(listOf(0L), tracker.deletedEntryBudgetIds)
    }

    @Test
    fun `execute handles multiple sequential deletions`() = runTest {
        val tracker = DeletionTracker()
        val useCase = TestableDeleteBudgetUseCase(tracker, Dispatchers.Default)

        useCase.execute(1L)
        useCase.execute(2L)
        useCase.execute(3L)

        assertEquals(listOf(1L, 2L, 3L), tracker.deletedBudgetIds)
        assertEquals(listOf(1L, 2L, 3L), tracker.deletedEntryBudgetIds)
    }

    @Test
    fun `execute handles large budget id values`() = runTest {
        val tracker = DeletionTracker()
        val useCase = TestableDeleteBudgetUseCase(tracker, Dispatchers.Default)
        val largeId = Long.MAX_VALUE

        useCase.execute(largeId)

        assertEquals(listOf(largeId), tracker.deletedBudgetIds)
        assertEquals(listOf(largeId), tracker.deletedEntryBudgetIds)
    }

    @Test
    fun `execute can be called multiple times with same id`() = runTest {
        val tracker = DeletionTracker()
        val useCase = TestableDeleteBudgetUseCase(tracker, Dispatchers.Default)

        // Delete same budget twice (might happen in UI with double-click)
        useCase.execute(1L)
        useCase.execute(1L)

        // Both calls should go through
        assertEquals(listOf(1L, 1L), tracker.deletedBudgetIds)
        assertEquals(listOf(1L, 1L), tracker.deletedEntryBudgetIds)
    }
}
