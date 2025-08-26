package com.meneses.budgethunter.budgetDetail.data

import java.util.concurrent.atomic.AtomicReference
import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class BudgetDetailRepository(
    private val budgetLocalDataSource: BudgetLocalDataSource,
    private val entriesLocalDataSource: BudgetEntryLocalDataSource,
    private val ioDispatcher: CoroutineDispatcher,
    private val deleteBudgetUseCase: DeleteBudgetUseCase
) {

    fun getCachedDetail(): BudgetDetail = cachedBudgetDetail.get()

    fun getBudgetDetailById(budgetId: Int): Flow<BudgetDetail> =
        budgetLocalDataSource.budgets
            .mapNotNull { budgets ->
                budgets.find { it.id == budgetId }
            }.combine(
                entriesLocalDataSource
                    .selectAllByBudgetId(budgetId.toLong())
            ) { budget, entries ->
                BudgetDetail(budget, entries)
            }.onEach {
                if (it != getCachedDetail()) {
                    cachedBudgetDetail.set(it)
                }
            }

    fun getAllFilteredBy(filter: BudgetEntryFilter) =
        getCachedDetail().copy(
            entries = entriesLocalDataSource.getAllFilteredBy(filter)
        )

    suspend fun updateBudgetAmount(amount: Double) = withContext(ioDispatcher) {
        val budget = getCachedDetail().budget.copy(amount = amount)
        budgetLocalDataSource.update(budget)
    }

    suspend fun deleteBudget() = withContext(ioDispatcher) {
        val budgetId = getCachedDetail().budget.id.toLong()
        deleteBudgetUseCase.execute(budgetId)
    }

    suspend fun deleteEntriesByIds(ids: List<Int>) = withContext(ioDispatcher) {
        val dbIds = ids.map { it.toLong() }
        entriesLocalDataSource.deleteByIds(dbIds)
    }

    private companion object {
        val cachedBudgetDetail = AtomicReference(BudgetDetail())
    }
}
