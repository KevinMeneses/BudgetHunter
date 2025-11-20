package com.meneses.budgethunter.budgetList

import com.meneses.budgethunter.budgetList.application.BudgetListEvent
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.fakes.repository.FakeBudgetRepository
import com.meneses.budgethunter.fakes.usecase.FakeDeleteBudgetUseCase
import com.meneses.budgethunter.fakes.usecase.FakeDuplicateBudgetUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BudgetListViewModelTest {

    @Test
    fun `initial state is loading with empty budget list`() = runTest {
        val repository: IBudgetRepository = FakeBudgetRepository()
        val viewModel = BudgetListViewModel(repository, FakeDuplicateBudgetUseCase(), FakeDeleteBudgetUseCase())

        val state = viewModel.uiState.value
        assertTrue(state.budgetList.isEmpty())
    }

    @Test
    fun `collectBudgetList updates state with budgets from repository`() = runTest {
        val repository: IBudgetRepository = FakeBudgetRepository()
        val budgets = listOf(
            Budget(id = 1, name = "Budget 1", amount = 100.0),
            Budget(id = 2, name = "Budget 2", amount = 200.0)
        )
        repository.emitBudgets(budgets)

        val viewModel = BudgetListViewModel(repository, FakeDuplicateBudgetUseCase(), FakeDeleteBudgetUseCase())

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals(2, state.budgetList.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `createBudget adds new budget and navigates to it`() = runTest {
        val repository: IBudgetRepository = FakeBudgetRepository()
        val viewModel = BudgetListViewModel(repository, FakeDuplicateBudgetUseCase(), FakeDeleteBudgetUseCase())

        val newBudget = Budget(id = -1, name = "New Budget", amount = 500.0)
        viewModel.sendEvent(BudgetListEvent.CreateBudget(newBudget))

        kotlinx.coroutines.delay(100)

        assertEquals(1, repository.createdBudgets.size)
        val state = viewModel.uiState.value
        assertEquals(1, state.navigateToBudget?.id)
    }

    @Test
    fun `updateBudget calls repository update`() = runTest {
        val repository: IBudgetRepository = FakeBudgetRepository()
        val viewModel = BudgetListViewModel(repository, FakeDuplicateBudgetUseCase(), FakeDeleteBudgetUseCase())

        val budget = Budget(id = 1, name = "Updated", amount = 300.0)
        viewModel.sendEvent(BudgetListEvent.UpdateBudget(budget))

        kotlinx.coroutines.delay(100)

        assertEquals(1, repository.updatedBudgets.size)
        assertEquals("Updated", repository.updatedBudgets[0].name)
    }

    @Test
    fun `duplicateBudget calls use case`() = runTest {
        val duplicateUseCase: IDuplicateBudgetUseCase = FakeDuplicateBudgetUseCase()
        val viewModel = BudgetListViewModel(FakeBudgetRepository(), duplicateUseCase, FakeDeleteBudgetUseCase())

        val budget = Budget(id = 1, name = "Original", amount = 100.0)
        viewModel.sendEvent(BudgetListEvent.DuplicateBudget(budget))

        kotlinx.coroutines.delay(100)

        assertEquals(1, duplicateUseCase.duplicatedBudgets.size)
    }

    @Test
    fun `deleteBudget calls use case with correct id`() = runTest {
        val deleteUseCase: IDeleteBudgetUseCase = FakeDeleteBudgetUseCase()
        val viewModel = BudgetListViewModel(FakeBudgetRepository(), FakeDuplicateBudgetUseCase(), deleteUseCase)

        viewModel.sendEvent(BudgetListEvent.DeleteBudget(42L))

        kotlinx.coroutines.delay(100)

        assertEquals(listOf(42L), deleteUseCase.deletedBudgetIds)
    }

    @Test
    fun `openBudget sets navigation state`() = runTest {
        val repository: IBudgetRepository = FakeBudgetRepository()
        val viewModel = BudgetListViewModel(repository, FakeDuplicateBudgetUseCase(), FakeDeleteBudgetUseCase())

        val budget = Budget(id = 1, name = "Test", amount = 100.0)
        viewModel.sendEvent(BudgetListEvent.OpenBudget(budget))

        val state = viewModel.uiState.value
        assertEquals(budget, state.navigateToBudget)
    }

    @Test
    fun `clearNavigation resets navigation state`() = runTest {
        val repository: IBudgetRepository = FakeBudgetRepository()
        val viewModel = BudgetListViewModel(repository, FakeDuplicateBudgetUseCase(), FakeDeleteBudgetUseCase())

        val budget = Budget(id = 1, name = "Test", amount = 100.0)
        viewModel.sendEvent(BudgetListEvent.OpenBudget(budget))
        viewModel.sendEvent(BudgetListEvent.ClearNavigation)

        val state = viewModel.uiState.value
        assertNull(state.navigateToBudget)
    }

    @Test
    fun `toggleAddModal sets visibility state`() = runTest {
        val viewModel = BudgetListViewModel(FakeBudgetRepository(), FakeDuplicateBudgetUseCase(), FakeDeleteBudgetUseCase())

        viewModel.sendEvent(BudgetListEvent.ToggleAddModal(true))
        assertTrue(viewModel.uiState.value.addModalVisibility)

        viewModel.sendEvent(BudgetListEvent.ToggleAddModal(false))
        assertFalse(viewModel.uiState.value.addModalVisibility)
    }

    @Test
    fun `toggleUpdateModal sets budget to update`() = runTest {
        val viewModel = BudgetListViewModel(FakeBudgetRepository(), FakeDuplicateBudgetUseCase(), FakeDeleteBudgetUseCase())

        val budget = Budget(id = 1, name = "Test", amount = 100.0)
        viewModel.sendEvent(BudgetListEvent.ToggleUpdateModal(budget))

        assertEquals(budget, viewModel.uiState.value.budgetToUpdate)
    }

    @Test
    fun `toggleSearchMode activates search mode`() = runTest {
        val viewModel = BudgetListViewModel(FakeBudgetRepository(), FakeDuplicateBudgetUseCase(), FakeDeleteBudgetUseCase())

        viewModel.sendEvent(BudgetListEvent.ToggleSearchMode(true))

        assertTrue(viewModel.uiState.value.isSearchMode)
    }

    @Test
    fun `toggleSearchMode deactivates search mode and clears query`() = runTest {
        val viewModel = BudgetListViewModel(FakeBudgetRepository(), FakeDuplicateBudgetUseCase(), FakeDeleteBudgetUseCase())

        viewModel.sendEvent(BudgetListEvent.UpdateSearchQuery("test"))
        viewModel.sendEvent(BudgetListEvent.ToggleSearchMode(true))
        viewModel.sendEvent(BudgetListEvent.ToggleSearchMode(false))

        val state = viewModel.uiState.value
        assertFalse(state.isSearchMode)
        assertEquals("", state.searchQuery)
    }

    @Test
    fun `updateSearchQuery filters budget list by name`() = runTest {
        val repository: IBudgetRepository = FakeBudgetRepository()
        val budgets = listOf(
            Budget(id = 1, name = "Food Budget", amount = 100.0),
            Budget(id = 2, name = "Transport Budget", amount = 200.0),
            Budget(id = 3, name = "Food and Drinks", amount = 150.0)
        )
        repository.emitBudgets(budgets)

        val viewModel = BudgetListViewModel(repository, FakeDuplicateBudgetUseCase(), FakeDeleteBudgetUseCase())

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(BudgetListEvent.UpdateSearchQuery("Food"))
        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals("Food", state.searchQuery)
        assertEquals(2, state.budgetList.size)
        assertTrue(state.budgetList.all { it.name.contains("Food", ignoreCase = true) })
    }

    @Test
    fun `updateSearchQuery is case insensitive`() = runTest {
        val repository: IBudgetRepository = FakeBudgetRepository()
        val budgets = listOf(
            Budget(id = 1, name = "FOOD BUDGET", amount = 100.0),
            Budget(id = 2, name = "food budget", amount = 200.0)
        )
        repository.emitBudgets(budgets)

        val viewModel = BudgetListViewModel(repository, FakeDuplicateBudgetUseCase(), FakeDeleteBudgetUseCase())

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(BudgetListEvent.UpdateSearchQuery("FoOd"))
        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals(2, state.budgetList.size)
    }

    @Test
    fun `updateSearchQuery with empty string shows all budgets`() = runTest {
        val repository: IBudgetRepository = FakeBudgetRepository()
        val budgets = listOf(
            Budget(id = 1, name = "Budget 1", amount = 100.0),
            Budget(id = 2, name = "Budget 2", amount = 200.0)
        )
        repository.emitBudgets(budgets)

        val viewModel = BudgetListViewModel(repository, FakeDuplicateBudgetUseCase(), FakeDeleteBudgetUseCase())

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(BudgetListEvent.UpdateSearchQuery("Budget 1"))
        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(BudgetListEvent.UpdateSearchQuery(""))
        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals(2, state.budgetList.size)
    }

    @Test
    fun `clearFilter restores full budget list`() = runTest {
        val repository: IBudgetRepository = FakeBudgetRepository()
        val budgets = listOf(
            Budget(id = 1, name = "Budget 1", amount = 100.0),
            Budget(id = 2, name = "Budget 2", amount = 200.0)
        )
        repository.emitBudgets(budgets)

        val viewModel = BudgetListViewModel(repository, FakeDuplicateBudgetUseCase(), FakeDeleteBudgetUseCase())

        kotlinx.coroutines.delay(100)
        viewModel.sendEvent(BudgetListEvent.ClearFilter)
        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertNull(state.filter)
    }
}
