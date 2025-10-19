package com.meneses.budgethunter.budgetEntry.data.datasource

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.lang.reflect.Proxy
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetEntryLocalDataSourceTest {

    private lateinit var dataSource: BudgetEntryLocalDataSource
    private lateinit var dispatcher: CoroutineDispatcher

    @BeforeTest
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        dataSource = createDataSource()
    }

    @Test
    fun `filters cached entries by description type category and date range`() = runTest {
        val matching = entry(
            description = "Grocery market",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.GROCERIES,
            date = "2024-01-15"
        )
        val entries = listOf(
            matching,
            entry(description = "Market refund", type = BudgetEntry.Type.INCOME, category = BudgetEntry.Category.GROCERIES, date = "2024-01-16"),
            entry(description = "Grocery market", type = BudgetEntry.Type.OUTCOME, category = BudgetEntry.Category.FOOD, date = "2024-02-01"),
            entry(description = "Grocery market", type = BudgetEntry.Type.OUTCOME, category = BudgetEntry.Category.GROCERIES, date = "2023-12-31")
        )
        setCachedEntries(entries)

        val filter = BudgetEntryFilter(
            description = "market",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.GROCERIES,
            startDate = "2024-01-01",
            endDate = "2024-01-31"
        )

        val result = dataSource.getAllFilteredBy(filter)

        assertEquals(listOf(matching), result)
    }

    private fun entry(
        description: String,
        type: BudgetEntry.Type,
        category: BudgetEntry.Category,
        date: String
    ) = BudgetEntry(
        description = description,
        type = type,
        category = category,
        date = date
    )

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
