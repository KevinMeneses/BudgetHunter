package com.meneses.budgethunter.budgetMetrics.application

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.lang.reflect.Proxy
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetTotalsPerCategoryUseCaseTest {

    private lateinit var dispatcher: CoroutineDispatcher
    private lateinit var dataSource: BudgetEntryLocalDataSource

    @BeforeTest
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        dataSource = createDataSource()
    }

    @Test
    fun `sums categories ignoring invalid amounts and preserves descending order`() = runTest {
        val entries = listOf(
            BudgetEntry(amount = "10", category = BudgetEntry.Category.FOOD),
            BudgetEntry(amount = "5.5", category = BudgetEntry.Category.FOOD),
            BudgetEntry(amount = "20", category = BudgetEntry.Category.SERVICES),
            BudgetEntry(amount = "invalid", category = BudgetEntry.Category.SERVICES),
            BudgetEntry(amount = "0", category = BudgetEntry.Category.OTHER)
        )
        setCachedEntries(entries)
        val useCase = GetTotalsPerCategoryUseCase(dataSource, dispatcher)

        val result = useCase.execute()

        assertEquals(listOf(BudgetEntry.Category.SERVICES, BudgetEntry.Category.FOOD), result.keys.toList())
        assertEquals(20.0, result[BudgetEntry.Category.SERVICES])
        assertEquals(15.5, result[BudgetEntry.Category.FOOD])
    }

    private fun createDataSource(): BudgetEntryLocalDataSource {
        val queriesClass = Class.forName("com.meneses.budgethunter.db.BudgetEntryQueries")
        val constructor = BudgetEntryLocalDataSource::class.java.getConstructor(queriesClass, CoroutineDispatcher::class.java)
        val proxy = Proxy.newProxyInstance(
            queriesClass.classLoader,
            arrayOf(queriesClass)
        ) { _, method, _ ->
            when (method.returnType) {
                java.lang.Boolean.TYPE -> false
                java.lang.Long.TYPE -> 0L
                java.lang.Integer.TYPE -> 0
                java.lang.Double.TYPE -> 0.0
                java.lang.Float.TYPE -> 0f
                java.lang.Short.TYPE -> 0.toShort()
                java.lang.Byte.TYPE -> 0.toByte()
                java.lang.Character.TYPE -> 0.toChar()
                Void.TYPE -> null
                else -> null
            }
        }
        return constructor.newInstance(proxy, dispatcher)
    }

    private fun setCachedEntries(entries: List<BudgetEntry>) {
        val field = BudgetEntryLocalDataSource::class.java.getDeclaredField("cachedEntries")
        field.isAccessible = true
        field.set(dataSource, entries)
    }
}
