package com.meneses.budgethunter.budgetMetrics

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetMetrics.application.BudgetMetricsIntent
import com.meneses.budgethunter.budgetMetrics.application.GetTotalsPerCategoryUseCase
import io.mockk.coEvery
import io.mockk.mockk
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
import kotlin.test.assertTrue

/**
 * Unit tests for the totals the metrics screen reads: the amount per category, what share of
 * the whole each one is, and the total they add up to.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetMetricsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getTotalsPerCategoryUseCase = mockk<GetTotalsPerCategoryUseCase>()

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

        val viewModel = BudgetMetricsViewModel(getTotalsPerCategoryUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1000.0, state.total)
        assertEquals(BudgetEntry.Category.FOOD, state.categoryMetrics.first().category)
        assertEquals(750.0, state.categoryMetrics.first().amount)
        assertEquals(75.0, state.categoryMetrics.first().percentage)
        assertEquals(25.0, state.categoryMetrics.last().percentage)
    }

    @Test
    fun `the order the use case returns is kept, so the biggest category comes first`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(BudgetEntry.Type.OUTCOME) } returns mapOf(
            BudgetEntry.Category.TAXES to 500.0,
            BudgetEntry.Category.FOOD to 300.0,
            BudgetEntry.Category.LEISURE to 200.0
        )

        val viewModel = BudgetMetricsViewModel(getTotalsPerCategoryUseCase)
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

        val viewModel = BudgetMetricsViewModel(getTotalsPerCategoryUseCase)
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

        val viewModel = BudgetMetricsViewModel(getTotalsPerCategoryUseCase)
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

        val viewModel = BudgetMetricsViewModel(getTotalsPerCategoryUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(BudgetEntry.Type.OUTCOME, viewModel.uiState.value.selectedType)
    }

    @Test
    fun `a total of zero does not blow up the percentages`() = runTest {
        coEvery { getTotalsPerCategoryUseCase.execute(BudgetEntry.Type.OUTCOME) } returns mapOf(
            BudgetEntry.Category.FOOD to 0.0
        )

        val viewModel = BudgetMetricsViewModel(getTotalsPerCategoryUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0.0, viewModel.uiState.value.categoryMetrics.single().percentage)
    }
}
