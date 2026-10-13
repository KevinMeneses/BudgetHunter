package com.meneses.budgethunter.budgetMetrics.application

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.PreferencesManager
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetCategorizationOfferUseCaseTest {

    private val preferencesManager = mockk<PreferencesManager>()
    private val localDataSource = mockk<BudgetEntryLocalDataSource>()
    private val budget = Budget(id = 1, serverId = 10L)
    private var entries = listOf<BudgetEntry>()

    private val useCase = GetCategorizationOfferUseCase(preferencesManager, localDataSource)

    @BeforeTest
    fun setup() {
        coEvery { preferencesManager.isAiProcessingEnabled() } returns true
        coEvery { preferencesManager.isOfflineModeEnabled() } returns false
        coEvery { localDataSource.getAllCached() } answers { entries }
        entries = waiting(2)
    }

    private fun waiting(count: Int, budgetId: Int = 1) = List(count) {
        BudgetEntry(budgetId = budgetId, category = BudgetEntry.Category.UNCATEGORIZED)
    }

    @Test
    fun `offers the entries that have no category`() = runTest {
        assertEquals(2, useCase.execute(budget))
    }

    @Test
    fun `entries that have a category are not offered`() = runTest {
        entries = waiting(1) + BudgetEntry(budgetId = 1, category = BudgetEntry.Category.OTHER) +
            BudgetEntry(budgetId = 1, category = BudgetEntry.Category.FOOD)

        assertEquals(1, useCase.execute(budget))
    }

    @Test
    fun `entries of another budget are not counted`() = runTest {
        entries = waiting(2, budgetId = 99)

        assertEquals(0, useCase.execute(budget))
    }

    @Test
    fun `nothing is offered when AI processing is off`() = runTest {
        coEvery { preferencesManager.isAiProcessingEnabled() } returns false

        assertEquals(0, useCase.execute(budget))
    }

    @Test
    fun `nothing is offered in offline mode`() = runTest {
        coEvery { preferencesManager.isOfflineModeEnabled() } returns true

        assertEquals(0, useCase.execute(budget))
    }

    @Test
    fun `nothing is offered for a budget that is not on the server`() = runTest {
        assertEquals(0, useCase.execute(budget.copy(serverId = null)))
    }

    @Test
    fun `after Not now the offer stays quiet until more entries are waiting`() = runTest {
        useCase.decline(budget, waiting = 2)

        assertEquals(0, useCase.execute(budget))

        entries = waiting(3)
        assertEquals(3, useCase.execute(budget))
    }

    @Test
    fun `declining one budget does not silence another`() = runTest {
        useCase.decline(Budget(id = 2, serverId = 20L), waiting = 2)

        assertEquals(2, useCase.execute(budget))
    }

    @Test
    fun `a finished run clears the decline`() = runTest {
        useCase.decline(budget, waiting = 2)
        useCase.clearDecline(budget)

        assertEquals(2, useCase.execute(budget))
    }
}
