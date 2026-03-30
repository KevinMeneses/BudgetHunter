package com.meneses.budgethunter.utils

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.commons.data.network.models.BudgetEntryResponse
import com.meneses.budgethunter.commons.data.network.models.BudgetResponse

/**
 * Factory object for creating test data used across performance and load tests.
 * Provides consistent, parameterised builders for domain objects and API response models.
 */
object TestDataFactory {

    /**
     * Creates a list of [Budget] instances.
     *
     * @param count Number of budgets to create.
     * @param synced When true the produced budgets carry a non-null [Budget.serverId].
     */
    fun budgets(count: Int, synced: Boolean = true): List<Budget> =
        List(count) { index ->
            val i = index + 1
            Budget(
                id = i,
                name = "Budget $i",
                amount = 1000.0 + i,
                serverId = if (synced) (100 + i).toLong() else null
            )
        }

    /**
     * Creates a list of [BudgetEntry] instances.
     *
     * @param count Number of entries to create.
     * @param budgetId Local budget ID to assign to every entry.
     * @param synced When true the produced entries carry a non-null [BudgetEntry.serverId].
     */
    fun budgetEntries(
        count: Int,
        budgetId: Int = 1,
        synced: Boolean = false
    ): List<BudgetEntry> =
        List(count) { index ->
            val i = index + 1
            BudgetEntry(
                id = i,
                budgetId = budgetId,
                amount = "${50.0 + i}",
                description = "Entry $i",
                serverId = if (synced) (200 + i).toLong() else null,
                category = BudgetEntry.Category.OTHER,
                type = BudgetEntry.Type.OUTCOME
            )
        }

    /**
     * Creates a list of [BudgetEntryResponse] instances as returned by the backend.
     *
     * @param count Number of responses to create.
     * @param budgetServerId Server-side budget ID to embed in every response.
     */
    fun budgetEntryResponses(
        count: Int,
        budgetServerId: Long = 101L
    ): List<BudgetEntryResponse> =
        List(count) { index ->
            val i = index + 1
            BudgetEntryResponse(
                id = (200 + i).toLong(),
                budgetId = budgetServerId,
                amount = 50.0 + i,
                description = "Entry $i",
                category = "OTHER",
                type = "OUTCOME",
                createdByEmail = "user@test.com",
                updatedByEmail = null,
                creationDate = "2024-01-01",
                modificationDate = "2024-01-01"
            )
        }

    /**
     * Creates a list of [BudgetResponse] instances as returned by the backend.
     *
     * @param count Number of responses to create.
     */
    fun budgetResponses(count: Int): List<BudgetResponse> =
        List(count) { index ->
            val i = index + 1
            BudgetResponse(
                id = (100 + i).toLong(),
                name = "Budget $i",
                amount = 1000.0 + i
            )
        }
}
