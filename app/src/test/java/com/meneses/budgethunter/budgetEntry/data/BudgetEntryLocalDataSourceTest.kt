package com.meneses.budgethunter.budgetEntry.data

import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.db.BudgetEntryQueries
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class BudgetEntryLocalDataSourceTest {

    private val queries: BudgetEntryQueries = mockk(relaxed = true)
    private val dispatcher = StandardTestDispatcher()
    private val dataSource = BudgetEntryLocalDataSource(queries, dispatcher)

    private val entry = BudgetEntry(
        id = 1,
        budgetId = 1,
        amount = 20.0.toString(),
        description = "description",
        type = BudgetEntry.Type.INCOME,
        date = "date",
        invoice = "invoice",
        category = BudgetEntry.Category.FOOD
    )

    @Before
    fun setUp() {
    }

    @Test
    fun selectAllByBudgetId() {
        val id = 10L
        // Test verifies that the method can be called and returns a flow
        val result = dataSource.selectAllByBudgetId(id)
        Assert.assertNotNull(result)
    }

    @Test
    fun insert() {
        every {
            queries.insert(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Unit

        dataSource.create(entry)

        verify {
            queries.insert(
                id = null,
                budgetId = entry.budgetId.toLong(),
                amount = entry.amount.toDouble(),
                description = entry.description,
                type = entry.type,
                date = entry.date,
                category = entry.category,
                invoice = entry.invoice
            )
        }
    }

    @Test
    fun update() {
        every {
            queries.update(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Unit

        dataSource.update(entry)

        verify {
            queries.update(
                id = entry.id.toLong(),
                budgetId = entry.budgetId.toLong(),
                amount = entry.amount.toDouble(),
                description = entry.description,
                type = entry.type,
                date = entry.date,
                category = entry.category,
                invoice = entry.invoice
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
