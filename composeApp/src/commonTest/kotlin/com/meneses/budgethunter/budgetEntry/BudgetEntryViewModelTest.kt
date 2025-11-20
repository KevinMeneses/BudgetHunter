package com.meneses.budgethunter.budgetEntry

import com.meneses.budgethunter.budgetEntry.application.BudgetEntryEvent
import com.meneses.budgethunter.budgetEntry.application.CreateBudgetEntryFromImageUseCase
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.application.ValidateFilePathUseCase
import com.meneses.budgethunter.commons.data.FileManager
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerManager
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.platform.ShareManager
import com.meneses.budgethunter.fakes.manager.FakeCameraManager
import com.meneses.budgethunter.fakes.manager.FakeFileManager
import com.meneses.budgethunter.fakes.manager.FakeFilePickerManager
import com.meneses.budgethunter.fakes.manager.FakeNotificationManager
import com.meneses.budgethunter.fakes.manager.FakePreferencesManager
import com.meneses.budgethunter.fakes.manager.FakeShareManager
import com.meneses.budgethunter.fakes.repository.FakeBudgetEntryRepository
import com.meneses.budgethunter.fakes.usecase.FakeCreateBudgetEntryFromImageUseCase
import com.meneses.budgethunter.fakes.usecase.FakeValidateFilePathUseCase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BudgetEntryViewModelTest {

    private fun createViewModel(
        repository: BudgetEntryRepository = FakeBudgetEntryRepository(),
        createUseCase: CreateBudgetEntryFromImageUseCase = FakeCreateBudgetEntryFromImageUseCase(),
        validateUseCase: ValidateFilePathUseCase = FakeValidateFilePathUseCase(),
        preferences: PreferencesManager = FakePreferencesManager(),
        fileManager: FileManager = FakeFileManager(),
        cameraManager: CameraManager = FakeCameraManager(),
        filePickerManager: FilePickerManager = FakeFilePickerManager(),
        shareManager: ShareManager = FakeShareManager(),
        notificationManager: NotificationManager = FakeNotificationManager()
    ): BudgetEntryViewModel {
        return BudgetEntryViewModel(
            repository, createUseCase, validateUseCase, preferences,
            fileManager, cameraManager, filePickerManager, shareManager, notificationManager
        )
    }

    @Test
    fun `initial state has null budget entry`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertNull(state.budgetEntry)
        assertFalse(state.goBack)
    }

    @Test
    fun `setBudgetEntry updates state`() = runTest {
        val viewModel = createViewModel()
        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "100")

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(entry))

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals(entry, state.budgetEntry)
    }

    @Test
    fun `saveBudgetEntry creates new entry when id is negative`() = runTest {
        val repository = FakeBudgetEntryRepository()
        val viewModel = createViewModel(repository = repository)
        val entry = BudgetEntry(id = -1, budgetId = 1, amount = "100")

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(entry))
        viewModel.sendEvent(BudgetEntryEvent.SaveBudgetEntry)

        kotlinx.coroutines.delay(100)

        assertEquals(1, repository.createdEntries.size)
        assertEquals(0, repository.updatedEntries.size)
        assertTrue(viewModel.uiState.value.goBack)
    }

    @Test
    fun `saveBudgetEntry updates existing entry when id is positive`() = runTest {
        val repository = FakeBudgetEntryRepository()
        val viewModel = createViewModel(repository = repository)
        val entry = BudgetEntry(id = 5, budgetId = 1, amount = "100")

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(entry))
        viewModel.sendEvent(BudgetEntryEvent.SaveBudgetEntry)

        kotlinx.coroutines.delay(100)

        assertEquals(0, repository.createdEntries.size)
        assertEquals(1, repository.updatedEntries.size)
    }

    @Test
    fun `saveBudgetEntry shows error when amount is empty`() = runTest {
        val viewModel = createViewModel()
        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "")

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(entry))
        viewModel.sendEvent(BudgetEntryEvent.SaveBudgetEntry)

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertNotNull(state.emptyAmountError)
        assertFalse(state.goBack)
    }

    @Test
    fun `goBack sets goBack flag`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendEvent(BudgetEntryEvent.GoBack)

        val state = viewModel.uiState.value
        assertTrue(state.goBack)
    }

    @Test
    fun `validateChanges shows discard modal when entry changed`() = runTest {
        val viewModel = createViewModel()
        val originalEntry = BudgetEntry(id = 1, budgetId = 1, amount = "100")
        val modifiedEntry = BudgetEntry(id = 1, budgetId = 1, amount = "200")

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(originalEntry))
        viewModel.sendEvent(BudgetEntryEvent.ValidateChanges(modifiedEntry))

        val state = viewModel.uiState.value
        assertTrue(state.isDiscardChangesModalVisible)
    }

    @Test
    fun `validateChanges goes back when entry unchanged`() = runTest {
        val viewModel = createViewModel()
        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "100")

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(entry))
        viewModel.sendEvent(BudgetEntryEvent.ValidateChanges(entry))

        val state = viewModel.uiState.value
        assertTrue(state.goBack)
        assertFalse(state.isDiscardChangesModalVisible)
    }

    @Test
    fun `hideDiscardChangesModal hides modal`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendEvent(BudgetEntryEvent.HideDiscardChangesModal)

        assertFalse(viewModel.uiState.value.isDiscardChangesModalVisible)
    }

    @Test
    fun `toggleAttachInvoiceModal toggles visibility`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendEvent(BudgetEntryEvent.ToggleAttachInvoiceModal(true))
        assertTrue(viewModel.uiState.value.isAttachInvoiceModalVisible)

        viewModel.sendEvent(BudgetEntryEvent.ToggleAttachInvoiceModal(false))
        assertFalse(viewModel.uiState.value.isAttachInvoiceModalVisible)
    }

    @Test
    fun `toggleShowInvoiceModal toggles visibility`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendEvent(BudgetEntryEvent.ToggleShowInvoiceModal(true))
        assertTrue(viewModel.uiState.value.isShowInvoiceModalVisible)

        viewModel.sendEvent(BudgetEntryEvent.ToggleShowInvoiceModal(false))
        assertFalse(viewModel.uiState.value.isShowInvoiceModalVisible)
    }

    @Test
    fun `attachInvoice saves file and updates entry`() = runTest {
        val fileManager = FakeFileManager()
        val viewModel = createViewModel(fileManager = fileManager)
        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "100")

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(entry))
        viewModel.sendEvent(BudgetEntryEvent.AttachInvoice(byteArrayOf(1, 2, 3)))

        kotlinx.coroutines.delay(100)

        assertEquals(1, fileManager.savedFiles.size)
        val state = viewModel.uiState.value
        assertNotNull(state.budgetEntry?.invoice)
    }

    @Test
    fun `attachInvoice processes with AI when enabled`() = runTest {
        val preferences = FakePreferencesManager()
        preferences.aiEnabled = true

        val createUseCase = FakeCreateBudgetEntryFromImageUseCase()
        createUseCase.processedEntry = BudgetEntry(id = 1, budgetId = 1, amount = "150", description = "AI Result")

        val viewModel = createViewModel(preferences = preferences, createUseCase = createUseCase)
        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "100")

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(entry))
        viewModel.sendEvent(BudgetEntryEvent.AttachInvoice(byteArrayOf(1, 2, 3)))

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertEquals("AI Result", state.budgetEntry?.description)
    }

    @Test
    fun `deleteAttachedInvoice removes invoice from entry`() = runTest {
        val viewModel = createViewModel()
        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "100", invoice = "/path/to/invoice.pdf")

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(entry))
        viewModel.sendEvent(BudgetEntryEvent.DeleteAttachedInvoice)

        val state = viewModel.uiState.value
        assertNull(state.budgetEntry?.invoice)
    }

    @Test
    fun `discardChanges goes back`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendEvent(BudgetEntryEvent.DiscardChanges)

        assertTrue(viewModel.uiState.value.goBack)
    }

    @Test
    fun `takePhoto opens camera`() = runTest {
        val cameraManager = FakeCameraManager()
        val viewModel = createViewModel(cameraManager = cameraManager)

        viewModel.sendEvent(BudgetEntryEvent.TakePhoto)

        assertNotNull(cameraManager.callback)
    }

    @Test
    fun `takePhoto attaches photo when taken`() = runTest {
        val cameraManager = FakeCameraManager()
        val fileManager = FakeFileManager()
        val viewModel = createViewModel(cameraManager = cameraManager, fileManager = fileManager)

        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "100")
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(entry))
        viewModel.sendEvent(BudgetEntryEvent.TakePhoto)

        cameraManager.simulatePhotoTaken(byteArrayOf(1, 2, 3))

        kotlinx.coroutines.delay(100)

        assertEquals(1, fileManager.savedFiles.size)
    }

    @Test
    fun `pickFile opens file picker`() = runTest {
        val filePickerManager = FakeFilePickerManager()
        val viewModel = createViewModel(filePickerManager = filePickerManager)

        viewModel.sendEvent(BudgetEntryEvent.PickFile)

        assertTrue(viewModel.uiState.value.isOpeningFilePicker)
        assertNotNull(filePickerManager.callback)
    }

    @Test
    fun `pickFile attaches file when picked`() = runTest {
        val filePickerManager = FakeFilePickerManager()
        val fileManager = FakeFileManager()
        val viewModel = createViewModel(filePickerManager = filePickerManager, fileManager = fileManager)

        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "100")
        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(entry))
        viewModel.sendEvent(BudgetEntryEvent.PickFile)

        filePickerManager.simulateFilePicked(byteArrayOf(1, 2, 3))

        kotlinx.coroutines.delay(100)

        assertEquals(1, fileManager.savedFiles.size)
        assertFalse(viewModel.uiState.value.isOpeningFilePicker)
    }

    @Test
    fun `shareFile shares the file`() = runTest {
        val shareManager = FakeShareManager()
        val viewModel = createViewModel(shareManager = shareManager)

        viewModel.sendEvent(BudgetEntryEvent.ShareFile("/path/to/file.pdf"))

        kotlinx.coroutines.delay(600)

        assertEquals(listOf("/path/to/file.pdf"), shareManager.sharedFiles)
        assertFalse(viewModel.uiState.value.isSharingFile)
    }

    @Test
    fun `showNotification shows error notification`() = runTest {
        val notificationManager = FakeNotificationManager()
        val viewModel = createViewModel(notificationManager = notificationManager)

        viewModel.sendEvent(BudgetEntryEvent.ShowNotification("Error occurred", isError = true))

        assertEquals(1, notificationManager.notifications.size)
        assertEquals("Error" to "Error occurred", notificationManager.notifications[0])
    }

    @Test
    fun `showNotification shows toast for non-error`() = runTest {
        val notificationManager = FakeNotificationManager()
        val viewModel = createViewModel(notificationManager = notificationManager)

        viewModel.sendEvent(BudgetEntryEvent.ShowNotification("Success", isError = false))

        assertEquals(listOf("Success"), notificationManager.toasts)
    }

    @Test
    fun `updateInvoice shows attach modal and hides show modal`() = runTest {
        val viewModel = createViewModel()

        viewModel.sendEvent(BudgetEntryEvent.ToggleShowInvoiceModal(true))
        viewModel.sendEvent(BudgetEntryEvent.UpdateInvoice)

        val state = viewModel.uiState.value
        assertFalse(state.isShowInvoiceModalVisible)
        assertTrue(state.isAttachInvoiceModalVisible)
    }

    @Test
    fun `setBudgetEntry validates invoice file`() = runTest {
        val validateUseCase = FakeValidateFilePathUseCase()
        validateUseCase.validPath = "/validated/path.pdf"

        val viewModel = createViewModel(validateUseCase = validateUseCase)
        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "100", invoice = "/path/invoice.pdf")

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(entry))

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertTrue(state.isFileValid)
        assertEquals("/validated/path.pdf", state.validatedFilePath)
    }

    @Test
    fun `setBudgetEntry handles null invoice`() = runTest {
        val viewModel = createViewModel()
        val entry = BudgetEntry(id = 1, budgetId = 1, amount = "100", invoice = null)

        viewModel.sendEvent(BudgetEntryEvent.SetBudgetEntry(entry))

        kotlinx.coroutines.delay(100)

        val state = viewModel.uiState.value
        assertTrue(state.isFileValid)
        assertNull(state.validatedFilePath)
    }
}
