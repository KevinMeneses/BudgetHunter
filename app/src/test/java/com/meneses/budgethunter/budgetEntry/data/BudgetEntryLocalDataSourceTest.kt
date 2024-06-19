package com.meneses.budgethunter.budgetEntry.data

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.db.Budget
import com.meneses.budgethunter.db.BudgetEntryQueries
import com.meneses.budgethunter.db.Budget_entry
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test

class BudgetEntryLocalDataSourceTest {

    private val queries: BudgetEntryQueries = mockk()
    private val dispatcher = StandardTestDispatcher()
    private val dataSource = BudgetEntryLocalDataSource(queries, dispatcher)

    private val entry = Budget_entry(
        id = 1,
        budget_id = 1,
        amount = 20.0,
        description = "description",
        type = BudgetEntry.Type.INCOME,
        date = "date"
    )

    @Before
    fun setUp() {
        mockkStatic("app.cash.sqldelight.coroutines.FlowQuery")
        every { any<Query<Budget>>().asFlow() } returns mockk(relaxed = true)
        every { any<Flow<Query<Budget>>>().mapToList(dispatcher) } returns flowOf(listOf(mockk()))
    }

    @After
    fun tearDown() {
        unmockkStatic("app.cash.sqldelight.coroutines.FlowQuery")
    }

    @Test
    fun selectAllByBudgetId() {
        val id = 10L
        every { queries.selectAllByBudgetId(id) } returns mockk(relaxed = true)
        dataSource.selectAllByBudgetId(id)
        verify {
            queries.selectAllByBudgetId(id)
            any<Query<Budget>>().asFlow()
            any<Flow<Query<Budget>>>().mapToList(dispatcher)
        }
    }

    @Test
    fun insert() {
        every {
            queries.insert(any(), any(), any(), any(), any(), any())
        } returns Unit

        dataSource.create(entry)

        verify {
            queries.insert(
                id = null,
                budgetId = entry.budget_id,
                amount = entry.amount,
                description = entry.description,
                type = entry.type,
                date = entry.date
            )
        }
    }

    @Test
    fun update() {
        every {
            queries.update(any(), any(), any(), any(), any(), any())
        } returns Unit

        dataSource.update(entry)

        verify {
            queries.update(
                id = entry.id,
                budgetId = entry.budget_id,
                amount = entry.amount,
                description = entry.description,
                type = entry.type,
                date = entry.date
            )
        }
    }

    @Test
    fun deleteByIds() {
        val ids = listOf<Long>()
        every { queries.deleteByIds(ids) } returns Unit
        dataSource.deleteByIds(ids)
        verify { queries.deleteByIds(ids) }
    }
}
