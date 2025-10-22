package com.meneses.budgethunter.budgetDetail.data

import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.lang.reflect.Proxy
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BudgetDetailRepositoryTest {

    private var deletedBudgetId: Long? = null
    private var deletedEntryIds: List<Long>? = null
    private val clearedBudgetIds = mutableListOf<Long>()

    @BeforeTest
    fun setUp() {
        seedCachedDetail()
        deletedBudgetId = null
        deletedEntryIds = null
        clearedBudgetIds.clear()
    }

    @Test
    fun `deleteEntriesByIds converts to database identifiers`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = createRepository(dispatcher)
        repository.deleteEntriesByIds(listOf(3, 5, 9))

        assertEquals(listOf(3L, 5L, 9L), deletedEntryIds)
    }

    @Test
    fun `deleteBudget invokes cascading cleanup`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = createRepository(dispatcher)
        repository.deleteBudget(11)

        assertEquals(11L, deletedBudgetId)
        assertEquals(listOf(11L), clearedBudgetIds)
    }

    private fun createRepository(dispatcher: CoroutineDispatcher): BudgetDetailRepository {
        val budgetLocalDataSource = createBudgetLocalDataSource(dispatcher)
        val entryLocalDataSource = createEntryLocalDataSource(dispatcher)
        val deleteBudgetUseCase = DeleteBudgetUseCase(budgetLocalDataSource, entryLocalDataSource, dispatcher)
        return BudgetDetailRepository(
            budgetLocalDataSource = budgetLocalDataSource,
            entriesLocalDataSource = entryLocalDataSource,
            ioDispatcher = dispatcher,
            deleteBudgetUseCase = deleteBudgetUseCase
        )
    }

    private fun seedCachedDetail() {
        val field = BudgetDetailRepository::class.java.getDeclaredField("cachedBudgetDetail")
        field.isAccessible = true
        field.set(null, BudgetDetail())
    }

    private fun createBudgetLocalDataSource(dispatcher: CoroutineDispatcher): BudgetLocalDataSource {
        val queriesClass = Class.forName("com.meneses.budgethunter.db.BudgetQueries")
        val constructor = BudgetLocalDataSource::class.java.getConstructor(queriesClass, CoroutineDispatcher::class.java)
        val queries = Proxy.newProxyInstance(
            queriesClass.classLoader,
            arrayOf(queriesClass)
        ) { _, method, args ->
            when (method.name) {
                "delete" -> {
                    deletedBudgetId = args?.getOrNull(0) as? Long
                    Unit
                }

                "selectAll" -> createEmptyListQuery()
                "insert", "update", "selectLastId" -> defaultReturn(method.returnType)
                "transaction" -> defaultReturn(method.returnType)
                else -> defaultReturn(method.returnType)
            }
        }
        return constructor.newInstance(queries, dispatcher)
    }

    private fun createEntryLocalDataSource(dispatcher: CoroutineDispatcher): BudgetEntryLocalDataSource {
        val queriesClass = Class.forName("com.meneses.budgethunter.db.BudgetEntryQueries")
        val constructor = BudgetEntryLocalDataSource::class.java.getConstructor(queriesClass, CoroutineDispatcher::class.java)
        val queries = Proxy.newProxyInstance(
            queriesClass.classLoader,
            arrayOf(queriesClass)
        ) { _, method, args ->
            when (method.name) {
                "deleteByIds" -> {
                    @Suppress("UNCHECKED_CAST")
                    val ids = args?.getOrNull(0) as? List<Long>
                    deletedEntryIds = ids
                    Unit
                }

                "deleteAllByBudgetId" -> {
                    val id = args?.getOrNull(0) as? Long
                    if (id != null) {
                        clearedBudgetIds += id
                    }
                    Unit
                }

                "selectAllByBudgetId" -> createEmptyListQuery()
                "insert", "update" -> defaultReturn(method.returnType)
                else -> defaultReturn(method.returnType)
            }
        }
        return constructor.newInstance(queries, dispatcher)
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
