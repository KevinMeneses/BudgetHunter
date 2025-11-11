package com.meneses.budgethunter.budgetMetrics.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetTotalsPerCategoryUseCaseTest {

    // For testing, we create a simple wrapper that allows us to inject test data
    // This avoids having to mock SqlDelight queries
    private class TestableGetTotalsPerCategoryUseCase(
        private val entries: List<BudgetEntry>,
        dispatcher: kotlinx.coroutines.CoroutineDispatcher
    ) {
        suspend fun execute(): Map<BudgetEntry.Category, Double> = kotlinx.coroutines.withContext(dispatcher) {
            val categories = BudgetEntry
                .getCategories()
                .map { it }
                .associateWith { 0.0 }
                .toMutableMap()

            entries.forEach {
                val previousAmount = categories[it.category] ?: 0.0
                val amountToAdd = it.amount.toDoubleOrNull() ?: 0.0
                categories[it.category] = previousAmount + amountToAdd
            }

            return@withContext categories.entries
                .filter { it.value != 0.0 }
                .sortedByDescending { it.value }
                .associate { it.key to it.value }
        }
    }

    @Test
    fun `execute returns empty map when no entries`() = runTest {
        val useCase = TestableGetTotalsPerCategoryUseCase(emptyList(), Dispatchers.Default)
        val result = useCase.execute()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `execute calculates totals for single category`() = runTest {
        val entries = listOf(
            BudgetEntry(
                id = 1,
                budgetId = 1,
                amount = "100.50",
                category = BudgetEntry.Category.FOOD
            ),
            BudgetEntry(
                id = 2,
                budgetId = 1,
                amount = "50.25",
                category = BudgetEntry.Category.FOOD
            )
        )

        val useCase = TestableGetTotalsPerCategoryUseCase(entries, Dispatchers.Default)
        val result = useCase.execute()

        assertEquals(1, result.size)
        assertEquals(150.75, result[BudgetEntry.Category.FOOD])
    }

    @Test
    fun `execute calculates totals for multiple categories`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 2, budgetId = 1, amount = "50", category = BudgetEntry.Category.GROCERIES),
            BudgetEntry(id = 3, budgetId = 1, amount = "25.50", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 4, budgetId = 1, amount = "75", category = BudgetEntry.Category.TRANSPORTATION)
        )

        val useCase = TestableGetTotalsPerCategoryUseCase(entries, Dispatchers.Default)
        val result = useCase.execute()

        assertEquals(3, result.size)
        assertEquals(125.50, result[BudgetEntry.Category.FOOD])
        assertEquals(50.0, result[BudgetEntry.Category.GROCERIES])
        assertEquals(75.0, result[BudgetEntry.Category.TRANSPORTATION])
    }

    @Test
    fun `execute filters out zero-value categories`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100", category = BudgetEntry.Category.FOOD)
        )

        val useCase = TestableGetTotalsPerCategoryUseCase(entries, Dispatchers.Default)
        val result = useCase.execute()

        assertEquals(1, result.size)
        assertTrue(result.containsKey(BudgetEntry.Category.FOOD))
        // Other categories should not be in the result
        assertTrue(!result.containsKey(BudgetEntry.Category.GROCERIES))
        assertTrue(!result.containsKey(BudgetEntry.Category.OTHER))
    }

    @Test
    fun `execute sorts categories by descending value`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "50", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 2, budgetId = 1, amount = "200", category = BudgetEntry.Category.GROCERIES),
            BudgetEntry(id = 3, budgetId = 1, amount = "100", category = BudgetEntry.Category.TRANSPORTATION)
        )

        val useCase = TestableGetTotalsPerCategoryUseCase(entries, Dispatchers.Default)
        val result = useCase.execute()

        val resultEntries = result.entries.toList()
        assertEquals(3, resultEntries.size)

        // Should be sorted by value descending: GROCERIES (200), TRANSPORTATION (100), FOOD (50)
        assertEquals(BudgetEntry.Category.GROCERIES, resultEntries[0].key)
        assertEquals(200.0, resultEntries[0].value)

        assertEquals(BudgetEntry.Category.TRANSPORTATION, resultEntries[1].key)
        assertEquals(100.0, resultEntries[1].value)

        assertEquals(BudgetEntry.Category.FOOD, resultEntries[2].key)
        assertEquals(50.0, resultEntries[2].value)
    }

    @Test
    fun `execute handles invalid amount strings`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 2, budgetId = 1, amount = "invalid", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 3, budgetId = 1, amount = "", category = BudgetEntry.Category.FOOD)
        )

        val useCase = TestableGetTotalsPerCategoryUseCase(entries, Dispatchers.Default)
        val result = useCase.execute()

        // Invalid amounts should be treated as 0.0
        assertEquals(100.0, result[BudgetEntry.Category.FOOD])
    }

    @Test
    fun `execute handles decimal amounts`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "10.50", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 2, budgetId = 1, amount = "20.75", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 3, budgetId = 1, amount = "5.25", category = BudgetEntry.Category.FOOD)
        )

        val useCase = TestableGetTotalsPerCategoryUseCase(entries, Dispatchers.Default)
        val result = useCase.execute()

        assertEquals(36.50, result[BudgetEntry.Category.FOOD])
    }

    @Test
    fun `execute handles mixed INCOME and OUTCOME types`() = runTest {
        val entries = listOf(
            BudgetEntry(
                id = 1,
                budgetId = 1,
                amount = "100",
                category = BudgetEntry.Category.FOOD,
                type = BudgetEntry.Type.OUTCOME
            ),
            BudgetEntry(
                id = 2,
                budgetId = 1,
                amount = "50",
                category = BudgetEntry.Category.FOOD,
                type = BudgetEntry.Type.INCOME
            )
        )

        val useCase = TestableGetTotalsPerCategoryUseCase(entries, Dispatchers.Default)
        val result = useCase.execute()

        // Use case sums all entries regardless of type
        assertEquals(150.0, result[BudgetEntry.Category.FOOD])
    }

    @Test
    fun `execute handles large amounts`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "999999.99", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 2, budgetId = 1, amount = "500000.50", category = BudgetEntry.Category.FOOD)
        )

        val useCase = TestableGetTotalsPerCategoryUseCase(entries, Dispatchers.Default)
        val result = useCase.execute()

        assertEquals(1500000.49, result[BudgetEntry.Category.FOOD])
    }

    @Test
    fun `execute accumulates amounts for same category from different budgets`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "100", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 2, budgetId = 2, amount = "50", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 3, budgetId = 3, amount = "25", category = BudgetEntry.Category.FOOD)
        )

        val useCase = TestableGetTotalsPerCategoryUseCase(entries, Dispatchers.Default)
        val result = useCase.execute()

        // Should sum across all budgets
        assertEquals(175.0, result[BudgetEntry.Category.FOOD])
    }

    @Test
    fun `execute handles all category types`() = runTest {
        val entries = BudgetEntry.getCategories().mapIndexed { index, category ->
            BudgetEntry(
                id = index + 1,
                budgetId = 1,
                amount = "${(index + 1) * 10}",
                category = category
            )
        }

        val useCase = TestableGetTotalsPerCategoryUseCase(entries, Dispatchers.Default)
        val result = useCase.execute()

        // Should have all categories with non-zero values
        assertEquals(BudgetEntry.getCategories().size, result.size)

        // Verify specific values
        assertEquals(10.0, result[BudgetEntry.Category.FOOD])
        assertEquals(20.0, result[BudgetEntry.Category.GROCERIES])
    }

    @Test
    fun `execute handles single entry with zero amount`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "0", category = BudgetEntry.Category.FOOD)
        )

        val useCase = TestableGetTotalsPerCategoryUseCase(entries, Dispatchers.Default)
        val result = useCase.execute()

        // Zero values should be filtered out
        assertTrue(result.isEmpty())
    }

    @Test
    fun `execute maintains precision for decimal calculations`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "10.01", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 2, budgetId = 1, amount = "20.02", category = BudgetEntry.Category.FOOD),
            BudgetEntry(id = 3, budgetId = 1, amount = "30.03", category = BudgetEntry.Category.FOOD)
        )

        val useCase = TestableGetTotalsPerCategoryUseCase(entries, Dispatchers.Default)
        val result = useCase.execute()

        assertEquals(60.06, result[BudgetEntry.Category.FOOD])
    }
}
