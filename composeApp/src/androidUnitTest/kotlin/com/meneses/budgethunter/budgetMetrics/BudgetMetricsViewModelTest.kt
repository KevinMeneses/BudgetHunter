package com.meneses.budgethunter.budgetMetrics

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetMetrics.application.BudgetMetricsIntent
import com.meneses.budgethunter.budgetMetrics.application.CategorizationOutcome
import com.meneses.budgethunter.budgetMetrics.application.CategorizeEntriesUseCase
import com.meneses.budgethunter.budgetMetrics.application.GetCategorizationOfferUseCase
import com.meneses.budgethunter.budgetMetrics.application.GetTotalsPerCategoryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the totals the metrics screen reads: the amount per category, what share of
 * the whole each one is, and the total they add up to.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetMetricsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getTotalsPerCategoryUseCase = mockk<GetTotalsPerCategoryUseCase>()
    private val getCategorizationOfferUseCase = mockk<GetCategorizationOfferUseCase>(relaxed = true)
    private val categorizeEntriesUseCase = mockk<CategorizeEntriesUseCase>()
    private val budget = Budget(id = 1, serverId = 10L)

    private fun createViewModel() = BudgetMetricsViewModel(
        budget = budget,
        getTotalsPerCategoryUseCase = getTotalsPerCategoryUseCase,
        getCategorizationOfferUseCase = getCategorizationOfferUseCase,
        categorizeEntriesUseCase = categorizeEntriesUseCase
    )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `state holds the total and the share of each category`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(BudgetEntry.Type.OUTCOME) } returns mapOf(
            BudgetEntry.Category.FOOD to 750.0,
            BudgetEntry.Category.HEALTH to 250.0
        )

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1000.0, state.total)
        assertEquals(BudgetEntry.Category.FOOD, state.categoryMetrics.first().category)
        assertEquals(750.0, state.categoryMetrics.first().amount)
        assertEquals(75.0, state.categoryMetrics.first().percentage)
        assertEquals(25.0, state.categoryMetrics.last().percentage)
    }

    @Test
    fun `the screen is loading until the metrics are read, not empty`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(BudgetEntry.Type.OUTCOME) } returns emptyMap()

        val viewModel = createViewModel()
        assertTrue(viewModel.uiState.value.isLoading)

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `switching type loads again and only the last type asked for is kept`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(BudgetEntry.Type.OUTCOME) } returns mapOf(
            BudgetEntry.Category.FOOD to 100.0
        )
        coEvery { getTotalsPerCategoryUseCase.execute(BudgetEntry.Type.INCOME) } returns mapOf(
            BudgetEntry.Category.OTHER to 900.0
        )
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(BudgetMetricsIntent.SelectType(BudgetEntry.Type.INCOME))
        assertTrue(viewModel.uiState.value.isLoading)
        viewModel.sendIntent(BudgetMetricsIntent.SelectType(BudgetEntry.Type.OUTCOME))
        viewModel.sendIntent(BudgetMetricsIntent.SelectType(BudgetEntry.Type.INCOME))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(BudgetEntry.Type.INCOME, state.selectedType)
        assertEquals(900.0, state.total)
    }

    @Test
    fun `the order the use case returns is kept, so the biggest category comes first`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(BudgetEntry.Type.OUTCOME) } returns mapOf(
            BudgetEntry.Category.TAXES to 500.0,
            BudgetEntry.Category.FOOD to 300.0,
            BudgetEntry.Category.LEISURE to 200.0
        )

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            listOf(
                BudgetEntry.Category.TAXES,
                BudgetEntry.Category.FOOD,
                BudgetEntry.Category.LEISURE
            ),
            viewModel.uiState.value.categoryMetrics.map { it.category }
        )
    }

    @Test
    fun `state is empty when there are no entries`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(BudgetEntry.Type.OUTCOME) } returns emptyMap()

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.categoryMetrics.isEmpty())
        assertEquals(0.0, state.total)
    }

    @Test
    fun `selecting incomes reloads the totals for that type`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(BudgetEntry.Type.OUTCOME) } returns mapOf(
            BudgetEntry.Category.FOOD to 300.0
        )
        coEvery { getTotalsPerCategoryUseCase.execute(BudgetEntry.Type.INCOME) } returns mapOf(
            BudgetEntry.Category.OTHER to 1200.0
        )

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.sendIntent(BudgetMetricsIntent.SelectType(BudgetEntry.Type.INCOME))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(BudgetEntry.Type.INCOME, state.selectedType)
        assertEquals(1200.0, state.total)
        assertEquals(BudgetEntry.Category.OTHER, state.categoryMetrics.single().category)
    }

    @Test
    fun `expenses are the type shown when the screen opens`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(BudgetEntry.Type.OUTCOME) } returns emptyMap()

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(BudgetEntry.Type.OUTCOME, viewModel.uiState.value.selectedType)
    }

    @Test
    fun `a total of zero does not blow up the percentages`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(BudgetEntry.Type.OUTCOME) } returns mapOf(
            BudgetEntry.Category.FOOD to 0.0
        )

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0.0, viewModel.uiState.value.categoryMetrics.single().percentage)
    }

    @Test
    fun `the offer is in the state when there are entries waiting`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(any()) } returns emptyMap()
        coEvery { getCategorizationOfferUseCase.execute(budget) } returns 3

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.categorizationOffer)
    }

    @Test
    fun `there is no offer when nothing is waiting`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(any()) } returns emptyMap()
        coEvery { getCategorizationOfferUseCase.execute(budget) } returns 0

        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.categorizationOffer)
    }

    @Test
    fun `declining closes the offer and remembers how many were waiting`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(any()) } returns emptyMap()
        coEvery { getCategorizationOfferUseCase.execute(budget) } returns 4
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(BudgetMetricsIntent.DeclineCategorization)

        assertNull(viewModel.uiState.value.categorizationOffer)
        verify { getCategorizationOfferUseCase.decline(budget, 4) }
    }

    @Test
    fun `confirming categorizes, closes the offer and reloads the totals`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(BudgetEntry.Type.OUTCOME) } returnsMany listOf(
            mapOf(BudgetEntry.Category.UNCATEGORIZED to 100.0),
            mapOf(BudgetEntry.Category.FOOD to 100.0)
        )
        coEvery { getCategorizationOfferUseCase.execute(budget) } returns 2
        coEvery { categorizeEntriesUseCase.execute(budget) } returns CategorizationOutcome.Done(2, 0)
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(BudgetMetricsIntent.ConfirmCategorization)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.categorizationOffer)
        assertFalse(state.isCategorizing)
        assertEquals(CategorizationOutcome.Done(2, 0), state.categorizationOutcome)
        assertEquals(BudgetEntry.Category.FOOD, state.categoryMetrics.single().category)
        verify { getCategorizationOfferUseCase.clearDecline(budget) }
    }

    @Test
    fun `a failed run keeps the offer so it can be retried`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(any()) } returns emptyMap()
        coEvery { getCategorizationOfferUseCase.execute(budget) } returns 2
        val error = IllegalStateException("no network")
        coEvery { categorizeEntriesUseCase.execute(budget) } returns CategorizationOutcome.Failed(error)
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(BudgetMetricsIntent.ConfirmCategorization)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.categorizationOffer)
        assertEquals(CategorizationOutcome.Failed(error), state.categorizationOutcome)
    }

    @Test
    fun `a second confirmation while a run is going is ignored`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(any()) } returns emptyMap()
        coEvery { getCategorizationOfferUseCase.execute(budget) } returns 2
        coEvery { categorizeEntriesUseCase.execute(budget) } returns CategorizationOutcome.Done(2, 0)
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(BudgetMetricsIntent.ConfirmCategorization)
        viewModel.sendIntent(BudgetMetricsIntent.ConfirmCategorization)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { categorizeEntriesUseCase.execute(budget) }
    }

    @Test
    fun `showing the outcome clears it`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(any()) } returns emptyMap()
        coEvery { getCategorizationOfferUseCase.execute(budget) } returns 1
        coEvery { categorizeEntriesUseCase.execute(budget) } returns CategorizationOutcome.AlreadyRunning
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.sendIntent(BudgetMetricsIntent.ConfirmCategorization)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.sendIntent(BudgetMetricsIntent.OutcomeShown)

        assertNull(viewModel.uiState.value.categorizationOutcome)
    }
}
