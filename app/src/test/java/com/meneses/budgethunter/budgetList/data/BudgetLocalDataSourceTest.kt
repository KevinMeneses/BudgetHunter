package com.meneses.budgethunter.budgetList.data

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.db.Budget
import com.meneses.budgethunter.db.BudgetQueries
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

internal class BudgetLocalDataSourceTest {

    private val queries: BudgetQueries = mockk(relaxed = true)
    private val dispatcher = StandardTestDispatcher()
    private val dataSource by lazy { BudgetLocalDataSource(queries, dispatcher) }

    private val budget = com.meneses.budgethunter.budgetList.domain.Budget(
        id = 1,
        amount = 20.0,
        name = "Gastos Mes",
    )

    @Before
    fun setUp() {
        mockkStatic("app.cash.sqldelight.coroutines.FlowQuery")
        every { queries.selectAll() } returns mockk(relaxed = true)
        every { any<Query<Budget>>().asFlow() } returns mockk(relaxed = true)
        every { any<Flow<Query<Budget>>>().mapToList(dispatcher) } returns flowOf(listOf(mockk()))
    }

    @After
    fun tearDown() {
        unmockkStatic("app.cash.sqldelight.coroutines.FlowQuery")
    }

    @Test
    fun getBudgets() = runTest(dispatcher) {
        dataSource.budgets.collect {
            Assert.assertTrue(it.isNotEmpty())
        }

        verify {
            queries.selectAll()
            any<Query<Budget>>().asFlow()
            any<Flow<Query<Budget>>>().mapToList(dispatcher)
        }
    }

    @Test
    fun insert() {
        every { queries.insert(any(), any(), any()) } returns Unit

        dataSource.create(budget)

        verify {
            queries.insert(
                id = null,
                name = budget.name,
                amount = budget.amount
            )
        }
    }

    @Test
    fun update() {
        every {
            queries.update(any(), any(), any())
        } returns Unit

        dataSource.update(budget)

        verify {
            queries.update(
                id = budget.id.toLong(),
                name = budget.name,
                amount = budget.amount
            )
        }
    }

    @Test
    fun delete() {
        val id = 10L
        every { queries.delete(any()) } returns Unit
        dataSource.delete(id)
        verify { queries.delete(id) }
    }
}
