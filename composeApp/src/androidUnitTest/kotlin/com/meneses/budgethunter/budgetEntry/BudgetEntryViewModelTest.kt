package com.meneses.budgethunter.budgetEntry

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.ai_error_network
import budgethunter.composeapp.generated.resources.ai_error_not_an_invoice
import budgethunter.composeapp.generated.resources.ai_error_timeout
import budgethunter.composeapp.generated.resources.error_no_app_to_open_file
import com.meneses.budgethunter.billing.application.GetReceiptScansUseCase
import com.meneses.budgethunter.billing.domain.Allowance
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
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
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
    private val getReceiptScansUseCase = mockk<GetReceiptScansUseCase>(relaxed = true)
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
            getReceiptScansUseCase = getReceiptScansUseCase,
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

    // ========== Open file externally ==========

    @Test
    fun `OpenFile opens a pdf with the pdf mime type and shows nothing when it works`() = runTest {
        every { shareManager.openFile(any(), any()) } returns true

        viewModel.sendIntent(BudgetEntryIntent.OpenFile("/files/invoice.PDF"))
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 1) { shareManager.openFile("/files/invoice.PDF", "application/pdf") }
        assertNull(withTimeoutOrNull(100) { viewModel.events.first() })
    }

    @Test
    fun `OpenFile shows an error when no app can open the file`() = runTest {
        every { shareManager.openFile(any(), any()) } returns false

        viewModel.sendIntent(BudgetEntryIntent.OpenFile("/files/invoice.pdf"))
        testDispatcher.scheduler.advanceUntilIdle()

        val event = viewModel.events.first()
        assertIs<BudgetEntryEvent.ShowNotification>(event)
        assertEquals(Res.string.error_no_app_to_open_file, event.message)
        assertTrue(event.isError)
    }

    // ========== isAiProcessingEnabled Tests ==========

    private fun createViewModel() = BudgetEntryViewModel(
        budgetEntryRepository = budgetEntryRepository,
        createBudgetEntryFromImageUseCase = createBudgetEntryFromImageUseCase,
        getReceiptScansUseCase = getReceiptScansUseCase,
        validateFilePathUseCase = validateFilePathUseCase,
        preferencesManager = preferencesManager,
        fileManager = fileManager,
        cameraManager = cameraManager,
        filePickerManager = filePickerManager,
        shareManager = shareManager
    )

    @Test
    fun `state mirrors AI processing enabled in preferences`() = runTest {
        coEvery { preferencesManager.isAiProcessingEnabled() } returns true
        val enabledViewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(enabledViewModel.uiState.value.isAiProcessingEnabled)
    }

    @Test
    fun `state mirrors AI processing disabled in preferences`() = runTest {
        coEvery { preferencesManager.isAiProcessingEnabled() } returns false
        val disabledViewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(disabledViewModel.uiState.value.isAiProcessingEnabled)
    }

    // ========== Receipt scans left ==========

    private fun scansViewModel(scans: Allowance?): BudgetEntryViewModel {
        // The view model made in setup() reads the usage too: let it finish and forget its call
        testDispatcher.scheduler.advanceUntilIdle()
        clearMocks(getReceiptScansUseCase)
        coEvery { preferencesManager.isAiProcessingEnabled() } returns true
        coEvery { getReceiptScansUseCase.execute() } returns scans
        return createViewModel().also { testDispatcher.scheduler.advanceUntilIdle() }
    }

    @Test
    fun `scans left are shown only when few remain`() = runTest {
        assertNull(scansViewModel(Allowance(used = 6, limit = 10, remaining = 4)).uiState.value.receiptScansLeftNotice())
        assertEquals(3, scansViewModel(Allowance(used = 7, limit = 10, remaining = 3)).uiState.value.receiptScansLeftNotice())
        // A large allowance is not "few" until the same count: no nagging from 20 % on
        assertNull(scansViewModel(Allowance(used = 800, limit = 1000, remaining = 200)).uiState.value.receiptScansLeftNotice())
    }

    @Test
    fun `the usage is read once when the screen opens`() = runTest {
        val viewModel = scansViewModel(Allowance(used = 8, limit = 10, remaining = 2))

        viewModel.sendIntent(BudgetEntryIntent.SetBudgetEntry(BudgetEntry()))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { getReceiptScansUseCase.execute() }
    }

    @Test
    fun `nothing is shown when the usage cannot be read`() = runTest {
        val viewModel = scansViewModel(null)

        assertNull(viewModel.uiState.value.receiptScans)
        assertNull(viewModel.uiState.value.receiptScansLeftNotice())
    }

    @Test
    fun `nothing is shown while AI processing is off`() = runTest {
        coEvery { preferencesManager.isAiProcessingEnabled() } returns false
        coEvery { getReceiptScansUseCase.execute() } returns Allowance(used = 9, limit = 10, remaining = 1)
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.receiptScansLeftNotice())
    }

    @Test
    fun `a scan's answer updates the line without asking the server again`() = runTest {
        viewModel = scansViewModel(Allowance(used = 6, limit = 10, remaining = 4))
        assertNull(viewModel.uiState.value.receiptScansLeftNotice())

        attachWithAiResult(
            CreateBudgetEntryFromImageUseCase.Result(BudgetEntry(), scans = Allowance(used = 7, limit = 10, remaining = 3))
        )

        assertEquals(3, viewModel.uiState.value.receiptScansLeftNotice())
        coVerify(exactly = 1) { getReceiptScansUseCase.execute() }
    }

    @Test
    fun `a scan's answer shows the line even when the first read failed`() = runTest {
        viewModel = scansViewModel(null)

        attachWithAiResult(
            CreateBudgetEntryFromImageUseCase.Result(BudgetEntry(), scans = Allowance(used = 9, limit = 10, remaining = 1))
        )

        assertEquals(1, viewModel.uiState.value.receiptScansLeftNotice())
    }

    @Test
    fun `no scans left after a 402 without usage`() = runTest {
        viewModel = scansViewModel(Allowance(used = 9, limit = 10, remaining = 1))

        attachWithAiResult(CreateBudgetEntryFromImageUseCase.Result(BudgetEntry(), AiFailureReason.QUOTA_EXCEEDED))

        assertEquals(0, viewModel.uiState.value.receiptScansLeftNotice())
    }

    @Test
    fun `with no scans left the line offers Premium`() = runTest {
        val viewModel = scansViewModel(Allowance(used = 10, limit = 10, remaining = 0))
        assertEquals(0, viewModel.uiState.value.receiptScansLeftNotice())

        viewModel.sendIntent(BudgetEntryIntent.SeePremium)

        assertIs<BudgetEntryEvent.OpenPremium>(viewModel.events.first())
    }
}
