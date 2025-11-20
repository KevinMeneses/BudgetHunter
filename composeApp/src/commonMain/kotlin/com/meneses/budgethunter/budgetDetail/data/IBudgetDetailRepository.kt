package com.meneses.budgethunter.budgetDetail.data

import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import kotlinx.coroutines.flow.Flow

interface IBudgetDetailRepository {
    suspend fun getCachedDetail(): BudgetDetail
    fun getBudgetDetailById(budgetId: Int): Flow<BudgetDetail>
    suspend fun getAllFilteredBy(filter: BudgetEntryFilter): BudgetDetail
    suspend fun updateBudgetAmount(amount: Double)
    suspend fun deleteBudget(budgetId: Int)
    suspend fun deleteEntriesByIds(ids: List<Int>)
}
