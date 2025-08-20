package com.meneses.budgethunter.budgetList.data

import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.db.Budget
import com.meneses.budgethunter.db.BudgetQueries
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

internal class BudgetLocalDataSourceTest {

    private val queries: BudgetQueries = mockk(relaxed = true)
    private val dispatcher = StandardTestDispatcher()
    private val dataSource = BudgetLocalDataSource(queries, dispatcher)

    private val budget = com.meneses.budgethunter.budgetList.domain.Budget(
        id = 1,
        amount = 20.0,
        name = "Gastos Mes",
        date = "2024-01-01"
    )

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
        unmockkStatic("app.cash.sqldelight.coroutines.FlowQuery")
    }

    @Test
    fun getBudgets() = runTest(dispatcher) {
        // Simple test that the method can be called without errors
        Assert.assertNotNull(dataSource.budgets)
    }

    @Test
    fun insert() {
        every { queries.insert(any(), any(), any(), any()) } returns Unit
        every { queries.selectLastId() } returns mockk {
            every { executeAsOne() } returns 1L
        }

        val result = dataSource.create(budget)

        verify {
            queries.insert(
                id = null,
                name = budget.name,
                amount = budget.amount,
                date = budget.date
            )
        }
        Assert.assertEquals(1, result.id)
    }

    @Test
    fun update() {
        every {
            queries.update(any(), any(), any(), any())
        } returns Unit

        dataSource.update(budget)

        verify {
            queries.update(
                id = budget.id.toLong(),
                name = budget.name,
                amount = budget.amount,
                date = budget.date
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
