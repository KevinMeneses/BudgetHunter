package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.lang.reflect.Proxy
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BudgetEntryRepositoryTest {

    private var lastInsertArgs: Array<out Any?>? = null
    private var lastUpdateArgs: Array<out Any?>? = null

    @BeforeTest
    fun setUp() {
        lastInsertArgs = null
        lastUpdateArgs = null
    }

    @Test
    fun `create delegates to local data source with normalized payload`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = createRepository(dispatcher)
        val entry = BudgetEntry(
            id = 0,
            budgetId = 7,
            amount = "25.50",
            description = "Groceries",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.GROCERIES,
            date = "2024-05-01",
            invoice = "ABC123"
        )

        repository.create(entry)

        val args = lastInsertArgs
        assertNotNull(args)
        assertEquals(listOf(null, 7L, 25.50, "Groceries", BudgetEntry.Type.OUTCOME, "2024-05-01", "ABC123", BudgetEntry.Category.GROCERIES), args.toList())
    }

    @Test
    fun `update forwards entry data to local source`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = createRepository(dispatcher)
        val entry = BudgetEntry(
            id = 9,
            budgetId = 3,
            amount = "12.00",
            description = "Coffee",
            type = BudgetEntry.Type.OUTCOME,
            category = BudgetEntry.Category.LEISURE,
            date = "2024-04-15",
            invoice = null
        )

        repository.update(entry)

        val args = lastUpdateArgs
        assertNotNull(args)
        assertEquals(listOf(9L, 3L, 12.0, "Coffee", BudgetEntry.Type.OUTCOME, "2024-04-15", null, BudgetEntry.Category.LEISURE), args.toList())
    }

    private fun createRepository(dispatcher: CoroutineDispatcher): BudgetEntryRepository {
        val dataSource = createDataSource(dispatcher)
        return BudgetEntryRepository(dataSource, dispatcher)
    }

    private fun createDataSource(dispatcher: CoroutineDispatcher): BudgetEntryLocalDataSource {
        val queriesClass = Class.forName("com.meneses.budgethunter.db.BudgetEntryQueries")
        val constructor = BudgetEntryLocalDataSource::class.java.getConstructor(queriesClass, CoroutineDispatcher::class.java)
        val queries = Proxy.newProxyInstance(
            queriesClass.classLoader,
            arrayOf(queriesClass)
        ) { _, method, args ->
            when (method.name) {
                "insert" -> {
                    lastInsertArgs = args ?: emptyArray()
                    Unit
                }

                "update" -> {
                    lastUpdateArgs = args ?: emptyArray()
                    Unit
                }

                "selectAllByBudgetId" -> createQueryProxy()
                "deleteByIds", "deleteAllByBudgetId" -> Unit
                else -> defaultReturn(method.returnType)
            }
        }
        return constructor.newInstance(queries, dispatcher)
    }

    private fun createQueryProxy(): Any? {
        val queryClass = Class.forName("app.cash.sqldelight.Query")
        return Proxy.newProxyInstance(
            queryClass.classLoader,
            arrayOf(queryClass)
        ) { _, method, _ ->
            when (method.name) {
                "executeAsList" -> emptyList<Any>()
                "addListener", "removeListener" -> Unit
                "executeAsOne" -> null
                else -> defaultReturn(method.returnType)
            }
        }
    }

    private fun defaultReturn(returnType: Class<*>): Any? = when (returnType) {
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
