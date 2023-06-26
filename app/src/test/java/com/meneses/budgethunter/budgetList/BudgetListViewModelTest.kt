package com.meneses.budgethunter.budgetList

import com.meneses.budgethunter.budgetList.application.BudgetListState
import com.meneses.budgethunter.budgetList.data.repository.BudgetRepository
import com.meneses.budgethunter.budgetList.domain.Budget
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

@ExperimentalCoroutinesApi
class BudgetListViewModelTest {

    private val repository: BudgetRepository = mockk()
    private val dispatcher = StandardTestDispatcher()

    private val viewModel by lazy {
        BudgetListViewModel(
            budgetRepository = repository,
            dispatcher = dispatcher
        )
    }

    @Test
    fun initialize() = runTest(dispatcher) {
        val budgets = listOf<Budget>(mockk())
        val state = mutableListOf<BudgetListState>()

        every { repository.budgets } returns flowOf(budgets)
        val job = launch { viewModel.uiState.toList(state) }
        runCurrent()

        Assert.assertEquals(budgets, state.last().budgetList)
        verify { repository.budgets }
        job.cancel()
    }

    @Test
    fun sendEvent() {
    }
}
