package com.meneses.budgethunter.budgetDetail

import com.meneses.budgethunter.budgetDetail.application.BudgetDetailEvent
import com.meneses.budgethunter.budgetDetail.application.BudgetDetailState
import com.meneses.budgethunter.budgetDetail.data.IBudgetDetailRepository
import com.meneses.budgethunter.budgetDetail.domain.BudgetDetail
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntryFilter
import com.meneses.budgethunter.budgetList.domain.Budget
import com.meneses.budgethunter.fakes.repository.FakeBudgetDetailRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BudgetDetailViewModelTest {

    @Test
    fun `initial state is loading`() = runTest {
        val repository: IBudgetDetailRepository = FakeBudgetDetailRepository()
        val viewModel = BudgetDetailViewModel(repository)

        val state = viewModel.uiState.value
        assertTrue(state.isLoading)
    }

    @Test
    fun `setBudget updates budget in state`() = runTest {
        val repository: IBudgetDetailRepository = FakeBudgetDetailRepository()
        val viewModel = BudgetDetailViewModel(repository)

        val budget = Budget(id = 1, name = "Test Budget", amount = 100.0)
        viewModel.sendEvent(BudgetDetailEvent.SetBudget(budget))

        val state = viewModel.uiState.value
        assertEquals(budget, state.budgetDetail.budget)
    }

    @Test
    fun `getBudgetDetail loads detail from repository`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "50"),
            BudgetEntry(id = 2, budgetId = 1, amount = "30")
        )
        val budget = Budget(id = 1, name = "Test", amount = 100.0)
        val detail = BudgetDetail(budget = budget, entries = entries)

        val fakeRepository = FakeBudgetDetailRepository()
        fakeRepository.cachedDetail = detail

        val viewModel = BudgetDetailViewModel(fakeRepository)
        viewModel.sendEvent(BudgetDetailEvent.SetBudget(budget))
        viewModel.sendEvent(BudgetDetailEvent.GetBudgetDetail)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.budgetDetail.entries.size)
    }

    @Test
    fun `updateBudgetAmount calls repository`() = runTest {
        val fakeRepository = FakeBudgetDetailRepository()
        val viewModel = BudgetDetailViewModel(fakeRepository)

        viewModel.sendEvent(BudgetDetailEvent.UpdateBudgetAmount(500.0))

        kotlinx.coroutines.delay(100)

        assertEquals(listOf(500.0), fakeRepository.updatedAmounts)
    }

    @Test
    fun `filterEntries applies filter to entries`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "50", type = BudgetEntry.Type.INCOME),
            BudgetEntry(id = 2, budgetId = 1, amount = "30", type = BudgetEntry.Type.OUTCOME)
        )
        val detail = BudgetDetail(entries = entries)
        val fakeRepository = FakeBudgetDetailRepository()
        fakeRepository.cachedDetail = detail

        val viewModel = BudgetDetailViewModel(fakeRepository)
        val filter = BudgetEntryFilter.ByType(BudgetEntry.Type.INCOME)
        viewModel.sendEvent(BudgetDetailEvent.FilterEntries(filter))

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals(filter, state.filter)
    }

    @Test
    fun `clearFilter removes filter from state`() = runTest {
        val fakeRepository = FakeBudgetDetailRepository()
        fakeRepository.cachedDetail = BudgetDetail(entries = emptyList())

        val viewModel = BudgetDetailViewModel(fakeRepository)
        viewModel.sendEvent(BudgetDetailEvent.ClearFilter)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertNull(state.filter)
    }

    @Test
    fun `deleteBudget calls repository and sets goBack`() = runTest {
        val budget = Budget(id = 42, name = "Test", amount = 100.0)
        val fakeRepository = FakeBudgetDetailRepository()
        fakeRepository.cachedDetail = BudgetDetail(budget = budget)

        val viewModel = BudgetDetailViewModel(fakeRepository)
        viewModel.sendEvent(BudgetDetailEvent.SetBudget(budget))
        viewModel.sendEvent(BudgetDetailEvent.DeleteBudget)

        kotlinx.coroutines.delay(100)

        assertEquals(listOf(42), fakeRepository.deletedBudgetIds)
        assertTrue(viewModel.uiState.value.goBack)
    }

    @Test
    fun `deleteSelectedEntries deletes only selected entries`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "50", isSelected = true),
            BudgetEntry(id = 2, budgetId = 1, amount = "30", isSelected = false),
            BudgetEntry(id = 3, budgetId = 1, amount = "20", isSelected = true)
        )
        val fakeRepository = FakeBudgetDetailRepository()
        fakeRepository.cachedDetail = BudgetDetail(entries = entries)

        val viewModel = BudgetDetailViewModel(fakeRepository)
        viewModel.sendEvent(BudgetDetailEvent.DeleteSelectedEntries)

        kotlinx.coroutines.delay(100)

        assertEquals(1, fakeRepository.deletedEntryIds.size)
        assertEquals(listOf(1, 3), fakeRepository.deletedEntryIds[0])
    }

    @Test
    fun `showEntry sets entry in state`() = runTest {
        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "50")
        val repository: IBudgetDetailRepository = FakeBudgetDetailRepository()
        val viewModel = BudgetDetailViewModel(repository)

        viewModel.sendEvent(BudgetDetailEvent.ShowEntry(entry))

        assertEquals(entry, viewModel.uiState.value.showEntry)
    }

    @Test
    fun `clearNavigation resets showEntry`() = runTest {
        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "50")
        val repository: IBudgetDetailRepository = FakeBudgetDetailRepository()
        val viewModel = BudgetDetailViewModel(repository)

        viewModel.sendEvent(BudgetDetailEvent.ShowEntry(entry))
        viewModel.sendEvent(BudgetDetailEvent.ClearNavigation)

        assertNull(viewModel.uiState.value.showEntry)
    }

    @Test
    fun `toggleBudgetModal sets modal visibility`() = runTest {
        val repository: IBudgetDetailRepository = FakeBudgetDetailRepository()
        val viewModel = BudgetDetailViewModel(repository)

        viewModel.sendEvent(BudgetDetailEvent.ToggleBudgetModal(true))
        assertTrue(viewModel.uiState.value.isBudgetModalVisible)

        viewModel.sendEvent(BudgetDetailEvent.ToggleBudgetModal(false))
        assertFalse(viewModel.uiState.value.isBudgetModalVisible)
    }

    @Test
    fun `toggleDeleteBudgetModal sets modal visibility`() = runTest {
        val repository: IBudgetDetailRepository = FakeBudgetDetailRepository()
        val viewModel = BudgetDetailViewModel(repository)

        viewModel.sendEvent(BudgetDetailEvent.ToggleDeleteBudgetModal(true))
        assertTrue(viewModel.uiState.value.isDeleteBudgetModalVisible)

        viewModel.sendEvent(BudgetDetailEvent.ToggleDeleteBudgetModal(false))
        assertFalse(viewModel.uiState.value.isDeleteBudgetModalVisible)
    }

    @Test
    fun `toggleDeleteEntriesModal sets modal visibility`() = runTest {
        val repository: IBudgetDetailRepository = FakeBudgetDetailRepository()
        val viewModel = BudgetDetailViewModel(repository)

        viewModel.sendEvent(BudgetDetailEvent.ToggleDeleteEntriesModal(true))
        assertTrue(viewModel.uiState.value.isDeleteEntriesModalVisible)

        viewModel.sendEvent(BudgetDetailEvent.ToggleDeleteEntriesModal(false))
        assertFalse(viewModel.uiState.value.isDeleteEntriesModalVisible)
    }

    @Test
    fun `toggleFilterModal sets modal visibility`() = runTest {
        val repository: IBudgetDetailRepository = FakeBudgetDetailRepository()
        val viewModel = BudgetDetailViewModel(repository)

        viewModel.sendEvent(BudgetDetailEvent.ToggleFilterModal(true))
        assertTrue(viewModel.uiState.value.isFilterModalVisible)

        viewModel.sendEvent(BudgetDetailEvent.ToggleFilterModal(false))
        assertFalse(viewModel.uiState.value.isFilterModalVisible)
    }

    @Test
    fun `toggleSelectionState activates selection mode`() = runTest {
        val repository: IBudgetDetailRepository = FakeBudgetDetailRepository()
        val viewModel = BudgetDetailViewModel(repository)

        viewModel.sendEvent(BudgetDetailEvent.ToggleSelectionState(true))
        assertTrue(viewModel.uiState.value.isSelectionActive)

        viewModel.sendEvent(BudgetDetailEvent.ToggleSelectionState(false))
        assertFalse(viewModel.uiState.value.isSelectionActive)
    }

    @Test
    fun `toggleAllEntriesSelection selects all entries`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "50", isSelected = false),
            BudgetEntry(id = 2, budgetId = 1, amount = "30", isSelected = false)
        )
        val fakeRepository = FakeBudgetDetailRepository()
        fakeRepository.cachedDetail = BudgetDetail(entries = entries)

        val viewModel = BudgetDetailViewModel(fakeRepository)
        viewModel.sendEvent(BudgetDetailEvent.GetBudgetDetail)
        kotlinx.coroutines.delay(100)

        viewModel.sendEvent(BudgetDetailEvent.ToggleAllEntriesSelection(true))

        val state = viewModel.uiState.value
        assertTrue(state.budgetDetail.entries.all { it.isSelected })
    }

    @Test
    fun `toggleAllEntriesSelection deselects all entries`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "50", isSelected = true),
            BudgetEntry(id = 2, budgetId = 1, amount = "30", isSelected = true)
        )
        val fakeRepository = FakeBudgetDetailRepository()
        fakeRepository.cachedDetail = BudgetDetail(entries = entries)

        val viewModel = BudgetDetailViewModel(fakeRepository)
        viewModel.sendEvent(BudgetDetailEvent.GetBudgetDetail)
        kotlinx.coroutines.delay(100)

        viewModel.sendEvent(BudgetDetailEvent.ToggleAllEntriesSelection(false))

        val state = viewModel.uiState.value
        assertTrue(state.budgetDetail.entries.none { it.isSelected })
    }

    @Test
    fun `toggleSelectEntry toggles individual entry selection`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "50", isSelected = false),
            BudgetEntry(id = 2, budgetId = 1, amount = "30", isSelected = false)
        )
        val fakeRepository = FakeBudgetDetailRepository()
        fakeRepository.cachedDetail = BudgetDetail(entries = entries)

        val viewModel = BudgetDetailViewModel(fakeRepository)
        viewModel.sendEvent(BudgetDetailEvent.GetBudgetDetail)
        kotlinx.coroutines.delay(100)

        viewModel.sendEvent(BudgetDetailEvent.ToggleSelectEntry(index = 0, isSelected = true))

        val state = viewModel.uiState.value
        assertTrue(state.budgetDetail.entries[0].isSelected)
        assertFalse(state.budgetDetail.entries[1].isSelected)
    }

    @Test
    fun `sortList cycles through sort orders`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 3, budgetId = 1, amount = "100", type = BudgetEntry.Type.INCOME),
            BudgetEntry(id = 2, budgetId = 1, amount = "50", type = BudgetEntry.Type.OUTCOME),
            BudgetEntry(id = 1, budgetId = 1, amount = "75", type = BudgetEntry.Type.INCOME)
        )
        val fakeRepository = FakeBudgetDetailRepository()
        fakeRepository.cachedDetail = BudgetDetail(entries = entries)

        val viewModel = BudgetDetailViewModel(fakeRepository)
        viewModel.sendEvent(BudgetDetailEvent.GetBudgetDetail)
        kotlinx.coroutines.delay(100)

        // First sort: AMOUNT_ASCENDANT
        viewModel.sendEvent(BudgetDetailEvent.SortList)
        assertEquals(BudgetDetailState.ListOrder.AMOUNT_ASCENDANT, viewModel.uiState.value.listOrder)

        // Second sort: AMOUNT_DESCENDANT
        viewModel.sendEvent(BudgetDetailEvent.SortList)
        assertEquals(BudgetDetailState.ListOrder.AMOUNT_DESCENDANT, viewModel.uiState.value.listOrder)

        // Third sort: DEFAULT
        viewModel.sendEvent(BudgetDetailEvent.SortList)
        assertEquals(BudgetDetailState.ListOrder.DEFAULT, viewModel.uiState.value.listOrder)
    }

    @Test
    fun `deactivating selection deselects all entries`() = runTest {
        val entries = listOf(
            BudgetEntry(id = 1, budgetId = 1, amount = "50", isSelected = true),
            BudgetEntry(id = 2, budgetId = 1, amount = "30", isSelected = true)
        )
        val fakeRepository = FakeBudgetDetailRepository()
        fakeRepository.cachedDetail = BudgetDetail(entries = entries)

        val viewModel = BudgetDetailViewModel(fakeRepository)
        viewModel.sendEvent(BudgetDetailEvent.GetBudgetDetail)
        kotlinx.coroutines.delay(100)

        viewModel.sendEvent(BudgetDetailEvent.ToggleSelectionState(false))

        val state = viewModel.uiState.value
        assertTrue(state.budgetDetail.entries.none { it.isSelected })
    }
}
