package com.meneses.budgethunter.budgetMetrics

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetMetrics.application.IGetTotalsPerCategoryUseCase
import com.meneses.budgethunter.fakes.usecase.FakeGetTotalsPerCategoryUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BudgetMetricsViewModelTest {

    @Test
    fun `initial state loads metrics on initialization`() = runTest {
        val totals = mapOf(
            BudgetEntry.Category.FOOD to 100.0,
            BudgetEntry.Category.TRANSPORTATION to 50.0,
            BudgetEntry.Category.LEISURE to 25.0
        )
        val useCase: IGetTotalsPerCategoryUseCase = FakeGetTotalsPerCategoryUseCase(totals)
        val viewModel = BudgetMetricsViewModel(useCase)

        // Wait for initialization
        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals<Map<BudgetEntry.Category, Double>>(totals, state.metricsData)
    }

    @Test
    fun `calculates correct percentages from totals`() = runTest {
        val totals = mapOf(
            BudgetEntry.Category.FOOD to 100.0,
            BudgetEntry.Category.TRANSPORTATION to 50.0,
            BudgetEntry.Category.LEISURE to 50.0
        )
        val useCase: IGetTotalsPerCategoryUseCase = FakeGetTotalsPerCategoryUseCase(totals)
        val viewModel = BudgetMetricsViewModel(useCase)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        val expectedPercentages = listOf(50.0, 25.0, 25.0) // 100/200, 50/200, 50/200

        assertEquals(expectedPercentages, state.percentages)
    }

    @Test
    fun `assigns correct number of colors based on categories`() = runTest {
        val totals = mapOf(
            BudgetEntry.Category.FOOD to 100.0,
            BudgetEntry.Category.TRANSPORTATION to 50.0,
            BudgetEntry.Category.LEISURE to 25.0
        )
        val useCase: IGetTotalsPerCategoryUseCase = FakeGetTotalsPerCategoryUseCase(totals)
        val viewModel = BudgetMetricsViewModel(useCase)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals(3, state.chartColors.size)
    }

    @Test
    fun `handles empty metrics data`() = runTest {
        val useCase: IGetTotalsPerCategoryUseCase = FakeGetTotalsPerCategoryUseCase(emptyMap())
        val viewModel = BudgetMetricsViewModel(useCase)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals(emptyMap(), state.metricsData)
        assertTrue(state.percentages.isEmpty())
        assertTrue(state.chartColors.isEmpty())
    }

    @Test
    fun `handles single category`() = runTest {
        val totals = mapOf(BudgetEntry.Category.FOOD to 150.0)
        val useCase: IGetTotalsPerCategoryUseCase = FakeGetTotalsPerCategoryUseCase(totals)
        val viewModel = BudgetMetricsViewModel(useCase)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals<Map<BudgetEntry.Category, Double>>(mapOf(BudgetEntry.Category.FOOD to 150.0), state.metricsData)
        assertEquals(listOf(100.0), state.percentages)
        assertEquals(1, state.chartColors.size)
    }

    @Test
    fun `handles large number of categories`() = runTest {
        val categories = listOf(
            BudgetEntry.Category.FOOD,
            BudgetEntry.Category.GROCERIES,
            BudgetEntry.Category.SELF_CARE,
            BudgetEntry.Category.TRANSPORTATION,
            BudgetEntry.Category.HOUSEHOLD_ITEMS,
            BudgetEntry.Category.SERVICES,
            BudgetEntry.Category.EDUCATION,
            BudgetEntry.Category.HEALTH,
            BudgetEntry.Category.LEISURE,
            BudgetEntry.Category.TAXES
        )
        val totals = categories.associateWith { it.ordinal * 10.0 + 10.0 }
        val useCase: IGetTotalsPerCategoryUseCase = FakeGetTotalsPerCategoryUseCase(totals)
        val viewModel = BudgetMetricsViewModel(useCase)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals(10, state.metricsData.size)
        assertEquals(10, state.percentages.size)
        assertEquals(10, state.chartColors.size)
    }

    @Test
    fun `percentages sum to approximately 100`() = runTest {
        val totals = mapOf(
            BudgetEntry.Category.FOOD to 100.0,
            BudgetEntry.Category.TRANSPORTATION to 100.0,
            BudgetEntry.Category.LEISURE to 100.0
        )
        val useCase: IGetTotalsPerCategoryUseCase = FakeGetTotalsPerCategoryUseCase(totals)
        val viewModel = BudgetMetricsViewModel(useCase)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        val sum = state.percentages.sum()

        // Should sum to approximately 100 (allowing for floating point arithmetic)
        assertTrue(sum >= 99.9 && sum <= 100.1)
    }

    @Test
    fun `handles decimal values correctly`() = runTest {
        val totals = mapOf(
            BudgetEntry.Category.FOOD to 33.33,
            BudgetEntry.Category.TRANSPORTATION to 66.67
        )
        val useCase: IGetTotalsPerCategoryUseCase = FakeGetTotalsPerCategoryUseCase(totals)
        val viewModel = BudgetMetricsViewModel(useCase)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals(2, state.percentages.size)
        assertTrue(state.percentages.all { it > 0 })
    }

    @Test
    fun `uiState exposes state as flow`() = runTest {
        val totals = mapOf(BudgetEntry.Category.FOOD to 100.0)
        val useCase: IGetTotalsPerCategoryUseCase = FakeGetTotalsPerCategoryUseCase(totals)
        val viewModel = BudgetMetricsViewModel(useCase)

        kotlinx.coroutines.delay(100)

        val state1 = viewModel.uiState.value
        val state2 = viewModel.uiState.value

        // Both should reference the same state
        assertEquals(state1.metricsData, state2.metricsData)
    }
}
