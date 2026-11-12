package com.meneses.budgethunter.budgetList.application

import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.data.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * The real [DuplicateBudgetUseCase] on a budget that is already on the server: the copy is a new
 * budget, so nothing of the original's sync state may be carried over.
 */
class DuplicateBudgetUseCaseSyncTest {

    private val budgetRepository = mockk<BudgetRepository>()
    private val entryRepository = mockk<BudgetEntryRepository>()
    private val useCase = DuplicateBudgetUseCase(budgetRepository, entryRepository, Dispatchers.Unconfined)

    private val syncedBudget = Budget(
        id = 1,
        name = "Shared",
        amount = 500.0,
        date = "2026-03-01",
        serverId = 101,
        isSynced = true,
        lastSyncedAt = "2026-03-02T10:00:00Z",
        collaboratorCount = 3
    )

    private val syncedEntry = BudgetEntry(
        id = 7, budgetId = 1, amount = "20", description = "Lunch",
        category = BudgetEntry.Category.FOOD, date = "2026-03-01",
        serverId = 201, isSynced = true, createdByEmail = "a@test.com", updatedByEmail = "b@test.com",
        creationDate = "2026-03-01T12:00:00", modificationDate = "2026-03-01T13:00:00"
    )

    @Test
    fun `the copy of a synced budget is a new budget pending to be pushed`() = runTest {
        val created = slot<Budget>()
        coEvery { budgetRepository.create(capture(created)) } answers { created.captured.copy(id = 2) }
        every { entryRepository.getAllByBudgetId(1L) } returns flowOf(emptyList())

        useCase.execute(syncedBudget)

        with(created.captured) {
            assertEquals("Shared (copy)", name)
            assertEquals(500.0, amount)
            assertNull(serverId)
            assertFalse(isSynced)
            assertNull(lastSyncedAt)
            assertEquals(1, collaboratorCount)
        }
    }

    @Test
    fun `the copies of synced entries are new entries pending to be pushed`() = runTest {
        coEvery { budgetRepository.create(any()) } answers { firstArg<Budget>().copy(id = 2) }
        every { entryRepository.getAllByBudgetId(1L) } returns flowOf(listOf(syncedEntry))
        val created = mutableListOf<BudgetEntry>()
        coEvery { entryRepository.create(capture(created)) } returns Unit

        useCase.execute(syncedBudget)

        assertEquals(1, created.size)
        with(created.single()) {
            assertEquals(-1, id)
            assertEquals(2, budgetId)
            assertEquals("20", amount)
            assertEquals("Lunch", description)
            assertEquals(BudgetEntry.Category.FOOD, category)
            assertEquals("2026-03-01", date)
            assertNull(serverId)
            assertFalse(isSynced)
            assertNull(createdByEmail)
            assertNull(updatedByEmail)
            assertNull(creationDate)
            assertNull(modificationDate)
        }
    }
}
