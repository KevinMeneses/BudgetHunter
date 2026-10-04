package com.meneses.budgethunter.budgetEntry

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.ai_error_network
import budgethunter.composeapp.generated.resources.ai_error_not_an_invoice
import budgethunter.composeapp.generated.resources.ai_error_timeout
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryEvent
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryIntent
import com.meneses.budgethunter.budgetEntry.application.CreateBudgetEntryFromImageUseCase
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.AiFailureReason
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.application.ValidateFilePathUseCase
import com.meneses.budgethunter.commons.data.FileData
import com.meneses.budgethunter.commons.data.FileManager
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerManager
import com.meneses.budgethunter.commons.platform.ShareManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for BudgetEntryViewModel event emissions.
 *
 * Focuses on the simplest navigation event: GoBack intent triggers a NavigateBack
 * one-shot event through the Channel-backed `events` Flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetEntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    // Mocks
    private val budgetEntryRepository = mockk<BudgetEntryRepository>(relaxed = true)
    private val createBudgetEntryFromImageUseCase = mockk<CreateBudgetEntryFromImageUseCase>(relaxed = true)
    private val validateFilePathUseCase = mockk<ValidateFilePathUseCase>(relaxed = true)
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val fileManager = mockk<FileManager>(relaxed = true)
    private val cameraManager = mockk<CameraManager>(relaxed = true)
    private val filePickerManager = mockk<FilePickerManager>(relaxed = true)
    private val shareManager = mockk<ShareManager>(relaxed = true)

    // System under test
    private lateinit var viewModel: BudgetEntryViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = BudgetEntryViewModel(
            budgetEntryRepository = budgetEntryRepository,
            createBudgetEntryFromImageUseCase = createBudgetEntryFromImageUseCase,
            validateFilePathUseCase = validateFilePathUseCase,
            preferencesManager = preferencesManager,
            fileManager = fileManager,
            cameraManager = cameraManager,
            filePickerManager = filePickerManager,
            shareManager = shareManager
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== GoBack Tests ==========

    @Test
    fun `GoBack intent emits NavigateBack event`() = runTest {
        // When
        viewModel.sendIntent(BudgetEntryIntent.GoBack)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - the ViewModel should emit a NavigateBack event instead of mutating goBack state
        val event = viewModel.events.first()
        assertIs<BudgetEntryEvent.NavigateBack>(event)
    }

    // ========== AI failure feedback ==========

    private fun attachWithAiResult(result: CreateBudgetEntryFromImageUseCase.Result) {
        coEvery { preferencesManager.isAiProcessingEnabled() } returns true
        every { fileManager.saveFile(any()) } returns "/files/invoice.pdf"
        every { fileManager.createUri(any()) } returns "file:///files/invoice.pdf"
        coEvery { createBudgetEntryFromImageUseCase.execute(any(), any()) } returns result

        viewModel.sendIntent(BudgetEntryIntent.SetBudgetEntry(BudgetEntry()))
        viewModel.sendIntent(
            BudgetEntryIntent.AttachInvoice(FileData(byteArrayOf(1), "invoice.pdf", "application/pdf", "/files"))
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `AI network failure shows an error notification and keeps the invoice`() = runTest {
        attachWithAiResult(CreateBudgetEntryFromImageUseCase.Result(BudgetEntry(), AiFailureReason.NETWORK))

        val event = viewModel.events.first()

        assertIs<BudgetEntryEvent.ShowNotification>(event)
        assertEquals(Res.string.ai_error_network, event.message)
        assertTrue(event.isError)
        assertEquals("/files/invoice.pdf", viewModel.uiState.value.budgetEntry?.invoice)
    }

    @Test
    fun `not an invoice shows an informative notification`() = runTest {
        attachWithAiResult(CreateBudgetEntryFromImageUseCase.Result(BudgetEntry(), AiFailureReason.NOT_AN_INVOICE))

        val event = viewModel.events.first()

        assertIs<BudgetEntryEvent.ShowNotification>(event)
        assertEquals(Res.string.ai_error_not_an_invoice, event.message)
        assertFalse(event.isError)
    }

    @Test
    fun `AI timeout shows its own message`() = runTest {
        attachWithAiResult(CreateBudgetEntryFromImageUseCase.Result(BudgetEntry(), AiFailureReason.TIMEOUT))

        val event = viewModel.events.first()

        assertIs<BudgetEntryEvent.ShowNotification>(event)
        assertEquals(Res.string.ai_error_timeout, event.message)
        assertTrue(event.isError)
    }
}
