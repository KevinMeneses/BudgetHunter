package com.meneses.budgethunter.budgetDetail.data

import androidx.lifecycle.AtomicReference
import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.data.datasource.BudgetEntryLocalDataSource
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetList.application.DeleteBudgetUseCase
import com.meneses.budgethunter.budgetList.data.datasource.BudgetLocalDataSource
import com.meneses.budgethunter.commons.data.KtorRealtimeMessagingClient
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BudgetDetailRepository(
    private val budgetLocalDataSource: BudgetLocalDataSource,
    private val entriesLocalDataSource: BudgetEntryLocalDataSource,
    private val preferencesManager: PreferencesManager,
    private val ioDispatcher: CoroutineDispatcher,
    private val deleteBudgetUseCase: DeleteBudgetUseCase,
    private val messagingClient: () -> KtorRealtimeMessagingClient
) {

    private var collaborationJob: Job? = null

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
                    if (preferencesManager.isCollaborationEnabled) {
                        sendUpdateToServer(it)
                    }
                }
            }

    private suspend fun sendUpdateToServer(detail: BudgetDetail) {
        val jsonDetail = Json.encodeToString(detail)
        messagingClient().sendUpdate(jsonDetail)
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

    suspend fun startCollaboration(): Int = withContext(ioDispatcher) {
        if (preferencesManager.isCollaborationEnabled) {
            throw CollaborationException("Collaboration already started")
        }
        val collaborationCode = messagingClient().startCollaboration()
        preferencesManager.isCollaborationEnabled = true
        messagingClient().sendUpdate(Json.encodeToString(getCachedDetail()))
        return@withContext collaborationCode
    }

    suspend fun stopCollaboration() {
        collaborationJob?.cancel()
        collaborationJob = null
        messagingClient().close()
        preferencesManager.isCollaborationEnabled = false
    }

    fun consumeCollaborationStream() {
        collaborationJob = CoroutineScope(ioDispatcher).launch {
            messagingClient().getCollaborationStream().collect { jsonDetail ->
                val detail = Json.decodeFromString<BudgetDetail>(jsonDetail)
                cachedBudgetDetail.set(detail)
                updateBudgetIfNecessary(detail)
                updateEntriesIfNecessary(detail)
            }
        }
    }

    private fun updateBudgetIfNecessary(detail: BudgetDetail) {
        val cachedBudgets = budgetLocalDataSource.getAllCached()
        val budgetIndex = cachedBudgets.indexOfFirst { it.id == detail.budget.id }
        if (budgetIndex != -1 && cachedBudgets[budgetIndex] != detail.budget) {
            budgetLocalDataSource.update(detail.budget)
        }
    }

    private fun updateEntriesIfNecessary(detail: BudgetDetail) {
        for (remoteEntry in detail.entries) {
            val cachedList = entriesLocalDataSource.getAllCached()
            val entryIndex = cachedList.indexOfFirst { it.id == remoteEntry.id }

            when {
                entryIndex == -1 -> {
                    val updatedEntries = cachedList.toMutableList()
                    updatedEntries.add(remoteEntry)
                    entriesLocalDataSource.create(remoteEntry)
                }

                cachedList[entryIndex] != remoteEntry -> {
                    val updatedEntries = cachedList.toMutableList()
                    updatedEntries[entryIndex] = remoteEntry
                    entriesLocalDataSource.update(remoteEntry)
                }
            }
        }
    }

    private companion object {
        val cachedBudgetDetail = AtomicReference(BudgetDetail())
    }
}
