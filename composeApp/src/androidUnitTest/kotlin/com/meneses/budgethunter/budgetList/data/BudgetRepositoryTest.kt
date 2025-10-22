package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.budgetList.domain.BudgetFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.lang.reflect.Proxy
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BudgetRepositoryTest {

    private var lastInsertArgs: Array<out Any?>? = null
    private var lastUpdateArgs: Array<out Any?>? = null
    private var lastInsertId: Long = 17L

    @BeforeTest
    fun setUp() {
        lastInsertArgs = null
        lastUpdateArgs = null
        lastInsertId = 17L
    }

    @Test
    fun `getById reads cached budgets`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val localDataSource = createDataSource(dispatcher)
        val repository = BudgetRepository(localDataSource, dispatcher)
        val cached = listOf(
            Budget(id = 1, amount = 100.0, name = "Home", date = "2024-01-01"),
            Budget(id = 2, amount = 50.0, name = "Groceries", date = "2024-02-02")
        )
        setCachedBudgets(localDataSource, cached)

        val result = repository.getById(2)

        assertEquals(cached[1], result)
    }

    @Test
    fun `getAllFilteredBy applies name filter ignoring case`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val localDataSource = createDataSource(dispatcher)
        val repository = BudgetRepository(localDataSource, dispatcher)
        val cached = listOf(
            Budget(id = 1, amount = 100.0, name = "Vacation", date = "2024-03-03"),
            Budget(id = 2, amount = 80.0, name = "Vacation Plans", date = "2024-03-10"),
            Budget(id = 3, amount = 40.0, name = "Groceries", date = "2024-03-05")
        )
        setCachedBudgets(localDataSource, cached)

        val result = repository.getAllFilteredBy(BudgetFilter(name = "vacation"))

        assertEquals(cached.take(2), result)
    }

    @Test
    fun `create returns budget with generated identifier`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val localDataSource = createDataSource(dispatcher)
        val repository = BudgetRepository(localDataSource, dispatcher)
        val budget = Budget(
            id = -1,
            amount = 245.0,
            name = "Renovation",
            date = "2024-06-01"
        )

        val saved = repository.create(budget)

        assertEquals(lastInsertId.toInt(), saved.id)
        val args = lastInsertArgs
        assertNotNull(args)
        assertEquals(listOf(245.0, "Renovation", "2024-06-01"), args.toList())
    }

    @Test
    fun `update forwards values to local data source`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val localDataSource = createDataSource(dispatcher)
        val repository = BudgetRepository(localDataSource, dispatcher)
        val budget = Budget(
            id = 9,
            amount = 510.0,
            name = "Savings",
            date = "2024-05-20"
        )

        repository.update(budget)

        val args = lastUpdateArgs
        assertNotNull(args)
        assertEquals(listOf(9L, 510.0, "Savings", "2024-05-20"), args.toList())
    }

    private fun createDataSource(dispatcher: CoroutineDispatcher): BudgetLocalDataSource {
        val queriesClass = Class.forName("com.meneses.budgethunter.db.BudgetQueries")
        val constructor = BudgetLocalDataSource::class.java.getConstructor(queriesClass, CoroutineDispatcher::class.java)
        val queries = Proxy.newProxyInstance(
            queriesClass.classLoader,
            arrayOf(queriesClass)
        ) { _, method, args ->
            when (method.name) {
                "insert" -> {
                    lastInsertArgs = args ?: emptyArray()
                    Unit
                }

                "selectLastId" -> createQueryProxy(lastInsertId)

                "update" -> {
                    lastUpdateArgs = args ?: emptyArray()
                    Unit
                }

                "transaction" -> {
                    invokeTransaction(args)
                    Unit
                }

                "selectAll" -> createEmptyListQuery()
                "delete" -> Unit
                else -> defaultReturn(method.returnType)
            }
        }
        return constructor.newInstance(queries, dispatcher)
    }

    private fun setCachedBudgets(dataSource: BudgetLocalDataSource, budgets: List<Budget>) {
        val field = BudgetLocalDataSource::class.java.getDeclaredField("cachedList")
        field.isAccessible = true
        field.set(dataSource, budgets)
    }

    private fun createQueryProxy(value: Long): Any {
        val queryClass = Class.forName("app.cash.sqldelight.Query")
        return Proxy.newProxyInstance(
            queryClass.classLoader,
            arrayOf(queryClass)
        ) { _, method, _ ->
            when (method.name) {
                "executeAsOne" -> value
                "executeAsList" -> listOf(value)
                "addListener", "removeListener" -> Unit
                else -> defaultReturn(method.returnType)
            }
        }
    }

    private fun createEmptyListQuery(): Any {
        val queryClass = Class.forName("app.cash.sqldelight.Query")
        return Proxy.newProxyInstance(
            queryClass.classLoader,
            arrayOf(queryClass)
        ) { _, method, _ ->
            when (method.name) {
                "executeAsList" -> emptyList<Any>()
                "addListener", "removeListener" -> Unit
                "executeAsOne" -> emptyList<Any>()
                else -> defaultReturn(method.returnType)
            }
        }
    }

    private fun invokeTransaction(args: Array<out Any?>?) {
        if (args.isNullOrEmpty()) return
        val lambda = args.last()
        val invokeMethod = lambda?.javaClass?.methods?.firstOrNull { it.name == "invoke" }
        if (invokeMethod != null) {
            if (invokeMethod.parameterCount == 0) {
                invokeMethod.invoke(lambda)
            } else {
                val transactionClass = Class.forName("app.cash.sqldelight.Transacter$Transaction")
                val transaction = Proxy.newProxyInstance(
                    transactionClass.classLoader,
                    arrayOf(transactionClass)
                ) { _, _, _ -> null }
                invokeMethod.invoke(lambda, transaction)
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
