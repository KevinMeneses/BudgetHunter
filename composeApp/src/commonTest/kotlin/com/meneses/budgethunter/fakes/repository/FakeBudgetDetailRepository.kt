package com.meneses.budgethunter.fakes.repository

import com.meneses.budgethunter.budgetDetail.data.IBudgetDetailRepository
import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeBudgetDetailRepository : IBudgetDetailRepository {
    var cachedDetail = BudgetDetail()
    val deletedBudgetIds = mutableListOf<Int>()
    val deletedEntryIds = mutableListOf<List<Int>>()
    val updatedAmounts = mutableListOf<Double>()

    override fun getBudgetDetailById(budgetId: Int): Flow<BudgetDetail> {
        return flowOf(cachedDetail)
    }

    override suspend fun getCachedDetail(): BudgetDetail = cachedDetail

    override suspend fun getAllFilteredBy(filter: BudgetEntryFilter): BudgetDetail {
        return cachedDetail.copy(
            entries = cachedDetail.entries.filter {
                when {
                    filter.category != null -> it.category == filter.category
                    filter.type != null -> it.type == filter.type
                    else -> true
                }
            }
        )
    }

    override suspend fun deleteBudget(budgetId: Int) {
        deletedBudgetIds.add(budgetId)
    }

    override suspend fun deleteEntriesByIds(ids: List<Int>) {
        deletedEntryIds.add(ids)
    }

    override suspend fun updateBudgetAmount(amount: Double) {
        updatedAmounts.add(amount)
    }
}
