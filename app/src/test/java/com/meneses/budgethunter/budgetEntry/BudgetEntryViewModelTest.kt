package com.meneses.budgethunter.budgetEntry

import android.content.ContentResolver
import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryEvent
import com.meneses.budgethunter.budgetEntry.application.GetAIBudgetEntryFromImageUseCase
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.data.PreferencesManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException

@ExperimentalCoroutinesApi
class BudgetEntryViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()
    private val repository: BudgetEntryRepository = mockk()
    private val getAIBudgetEntryFromImageUseCase: GetAIBudgetEntryFromImageUseCase = mockk()
    private val preferencesManager: PreferencesManager = mockk()

    private lateinit var viewModel: BudgetEntryViewModel

    // Test data
    private val testBudgetEntry = BudgetEntry(
        id = 1,
        budgetId = 1,
        amount = "100.50",
        description = "Test Entry",
        type = BudgetEntry.Type.OUTCOME,
        category = BudgetEntry.Category.FOOD
    )

    private val emptyBudgetEntry = BudgetEntry(
        id = -1,
        budgetId = 1,
        amount = "",
        description = "",
        type = BudgetEntry.Type.OUTCOME,
        category = BudgetEntry.Category.OTHER
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { preferencesManager.isAiProcessingEnabled } returns true
        viewModel = BudgetEntryViewModel(
            budgetEntryRepository = repository,
            getAIBudgetEntryFromImageUseCase = getAIBudgetEntryFromImageUseCase,
            preferencesManager = preferencesManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `goBack event should set goBack flag to true`() = runTest {
        // When
        viewModel.sendEvent(BudgetEntryEvent.GoBack)

        // Then
        Assert.assertTrue(viewModel.uiState.value.goBack)
    }

    @Test
    fun `hideDiscardChangesModal event should hide modal`() = runTest {
        // When
        viewModel.sendEvent(BudgetEntryEvent.HideDiscardChangesModal)

        // Then
        Assert.assertFalse(viewModel.uiState.value.isDiscardChangesModalVisible)
    }

    @Test
    fun `setBudgetEntry event should update budget entry in state`() = runTest {
        // When
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(testBudgetEntry))

        // Then
        val state = viewModel.uiState.value
        Assert.assertEquals(testBudgetEntry, state.budgetEntry)
        Assert.assertNull(state.emptyAmountError) // Should clear any previous error
    }

    @Test
    fun `saveBudgetEntry event should show error when amount is empty`() = runTest(dispatcher) {
        // Given - budget entry with empty amount
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(emptyBudgetEntry))

        // When
        viewModel.sendEvent(BudgetEntryEvent.SaveBudgetEntry)
        runCurrent()

        // Then
        Assert.assertEquals(R.string.amount_mandatory, viewModel.uiState.value.emptyAmountError)
    }

    @Test
    fun sendSaveBudgetEntryEvent() = runTest(dispatcher) {
        val budgetEntry = BudgetEntry(amount = "20")

        coEvery { repository.create(budgetEntry) } returns Unit

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(budgetEntry))
        viewModel.sendEvent(BudgetEntryEvent.SaveBudgetEntry)
        runCurrent()

        coVerify { repository.create(budgetEntry) }

        // Should trigger goBack automatically after save
        Assert.assertTrue(viewModel.uiState.value.goBack)
    }

    @Test
    fun sendSaveBudgetEntryEventUpdate() = runTest(dispatcher) {
        val budgetEntry = BudgetEntry(id = 1, amount = "20")

        coEvery { repository.update(budgetEntry) } returns Unit

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(budgetEntry))
        viewModel.sendEvent(BudgetEntryEvent.SaveBudgetEntry)
        runCurrent()

        coVerify { repository.update(budgetEntry) }

        // Should trigger goBack automatically after save
        Assert.assertTrue(viewModel.uiState.value.goBack)
    }

    @Test
    fun sendValidateChangesEventNoChanges() = runTest {
        val budgetEntry = BudgetEntry()
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(budgetEntry))
        viewModel.sendEvent(BudgetEntryEvent.ValidateChanges(budgetEntry))
        val state = viewModel.uiState.value
        Assert.assertTrue(state.goBack)
    }

    @Test
    fun sendValidateChangesEventChanges() = runTest {
        val budgetEntry = BudgetEntry()
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(budgetEntry))
        val updatedBudgetEntry = budgetEntry.copy(amount = "10")
        viewModel.sendEvent(BudgetEntryEvent.ValidateChanges(updatedBudgetEntry))
        val state = viewModel.uiState.value
        Assert.assertTrue(state.isDiscardChangesModalVisible)
    }

    @Test
    fun `saveBudgetEntry event should create new entry when id is negative`() = runTest(dispatcher) {
        // Given - new budget entry (id < 0)
        val newEntry = testBudgetEntry.copy(id = -1)
        coEvery { repository.create(newEntry) } returns Unit

        // When
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(newEntry))
        viewModel.sendEvent(BudgetEntryEvent.SaveBudgetEntry)
        runCurrent()

        // Then
        coVerify { repository.create(newEntry) }
        Assert.assertTrue(viewModel.uiState.value.goBack)
    }

    @Test
    fun `saveBudgetEntry event should update existing entry when id is positive`() = runTest(dispatcher) {
        // Given - existing budget entry (id >= 0)
        coEvery { repository.update(testBudgetEntry) } returns Unit

        // When
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(testBudgetEntry))
        viewModel.sendEvent(BudgetEntryEvent.SaveBudgetEntry)
        runCurrent()

        // Then
        coVerify { repository.update(testBudgetEntry) }
        Assert.assertTrue(viewModel.uiState.value.goBack)
    }

    @Test
    fun `validateChanges event should go back when no changes detected`() = runTest {
        // Given - set initial budget entry
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(testBudgetEntry))

        // When - validate with same entry
        viewModel.sendEvent(BudgetEntryEvent.ValidateChanges(testBudgetEntry))

        // Then
        Assert.assertTrue(viewModel.uiState.value.goBack)
        Assert.assertFalse(viewModel.uiState.value.isDiscardChangesModalVisible)
    }

    @Test
    fun `validateChanges event should show discard modal when changes detected`() = runTest {
        // Given - set initial budget entry
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(testBudgetEntry))

        // When - validate with different entry
        val changedEntry = testBudgetEntry.copy(amount = "200.00")
        viewModel.sendEvent(BudgetEntryEvent.ValidateChanges(changedEntry))

        // Then
        Assert.assertFalse(viewModel.uiState.value.goBack)
        Assert.assertTrue(viewModel.uiState.value.isDiscardChangesModalVisible)
    }

    @Test
    fun `discardChanges event should go back`() = runTest {
        // When
        viewModel.sendEvent(BudgetEntryEvent.DiscardChanges)

        // Then
        Assert.assertTrue(viewModel.uiState.value.goBack)
    }

    @Test
    fun `toggleAttachInvoiceModal event should update modal visibility`() = runTest {
        // When - show modal
        viewModel.sendEvent(BudgetEntryEvent.ToggleAttachInvoiceModal(true))

        // Then
        Assert.assertTrue(viewModel.uiState.value.isAttachInvoiceModalVisible)

        // When - hide modal
        viewModel.sendEvent(BudgetEntryEvent.ToggleAttachInvoiceModal(false))

        // Then
        Assert.assertFalse(viewModel.uiState.value.isAttachInvoiceModalVisible)
    }

    @Test
    fun `toggleShowInvoiceModal event should update modal visibility`() = runTest {
        // When - show modal
        viewModel.sendEvent(BudgetEntryEvent.ToggleShowInvoiceModal(true))

        // Then
        Assert.assertTrue(viewModel.uiState.value.isShowInvoiceModalVisible)

        // When - hide modal
        viewModel.sendEvent(BudgetEntryEvent.ToggleShowInvoiceModal(false))

        // Then
        Assert.assertFalse(viewModel.uiState.value.isShowInvoiceModalVisible)
    }

    @Test
    fun `deleteAttachedInvoice event should remove invoice from budget entry`() = runTest {
        // Given - budget entry with invoice
        val entryWithInvoice = testBudgetEntry.copy(invoice = "/path/to/invoice.jpg")
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(entryWithInvoice))

        // When
        viewModel.sendEvent(BudgetEntryEvent.DeleteAttachedInvoice)

        // Then
        val state = viewModel.uiState.value
        Assert.assertNull(state.budgetEntry?.invoice)
        Assert.assertFalse(state.isShowInvoiceModalVisible)
    }

    @Test
    fun `attachInvoice event should process without crashing`() = runTest(dispatcher) {
        // Given - mock dependencies that will cause an error to test error handling
        val mockUri = mockk<Uri>()
        val mockContentResolver = mockk<ContentResolver>()
        val mockInternalFilesDir = mockk<File>()

        // Make contentResolver throw an exception to test error handling
        every { mockContentResolver.openInputStream(mockUri) } throws IOException("Mock file error")

        // Set initial budget entry
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(testBudgetEntry))

        // When - send attach invoice event (expect it to handle errors gracefully)
        val attachEvent = BudgetEntryEvent.AttachInvoice(
            fileToSave = mockUri,
            contentResolver = mockContentResolver,
            internalFilesDir = mockInternalFilesDir
        )

        viewModel.sendEvent(attachEvent)
        runCurrent()

        // Then - should show error message due to mocked exception
        Assert.assertEquals("Something went wrong loading file, please try again", viewModel.uiState.value.attachInvoiceError)
    }

    @Test
    fun `attachInvoice event should handle AI processing error and clear after delay`() = runTest(dispatcher) {
        // Given - mock dependencies with error
        val mockUri = mockk<Uri>()
        val mockContentResolver = mockk<ContentResolver>()
        val mockInternalFilesDir = mockk<File>()

        every { mockContentResolver.openInputStream(mockUri) } throws IOException("File read error")

        // Set initial budget entry
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(testBudgetEntry))

        // When
        val attachEvent = BudgetEntryEvent.AttachInvoice(
            fileToSave = mockUri,
            contentResolver = mockContentResolver,
            internalFilesDir = mockInternalFilesDir
        )
        viewModel.sendEvent(attachEvent)
        runCurrent()

        // Then - should show error message
        Assert.assertEquals("Something went wrong loading file, please try again", viewModel.uiState.value.attachInvoiceError)

        // After 2 seconds, error should be cleared
        advanceTimeBy(2001)
        runCurrent()

        Assert.assertNull(viewModel.uiState.value.attachInvoiceError)
    }

    @Test
    fun `budget entry state should have correct initial values`() = runTest {
        // Then - verify initial state
        val initialState = viewModel.uiState.value
        Assert.assertNull(initialState.budgetEntry)
        Assert.assertNull(initialState.emptyAmountError)
        Assert.assertFalse(initialState.isDiscardChangesModalVisible)
        Assert.assertFalse(initialState.isAttachInvoiceModalVisible)
        Assert.assertFalse(initialState.isShowInvoiceModalVisible)
        Assert.assertNull(initialState.attachInvoiceError)
        Assert.assertFalse(initialState.goBack)
    }
}
