package com.meneses.budgethunter.budgetEntry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryEvent
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryState
import com.meneses.budgethunter.budgetEntry.application.ICreateBudgetEntryFromImageUseCase
import com.meneses.budgethunter.budgetEntry.data.IBudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.application.IValidateFilePathUseCase
import com.meneses.budgethunter.commons.data.IFileManager
import com.meneses.budgethunter.commons.data.IPreferencesManager
import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerManager
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.platform.ShareManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * KMP BudgetEntryViewModel migrated from Android version.
 * Maintains all the complex business logic, state management, and platform service integrations
 * while being cross-platform compatible.
 */
class BudgetEntryViewModel(
    private val budgetEntryRepository: IBudgetEntryRepository,
    private val createBudgetEntryFromImageUseCase: ICreateBudgetEntryFromImageUseCase,
    private val validateFilePathUseCase: IValidateFilePathUseCase,
    private val preferencesManager: IPreferencesManager,
    private val fileManager: IFileManager,
    private val cameraManager: CameraManager,
    private val filePickerManager: FilePickerManager,
    private val shareManager: ShareManager,
    private val notificationManager: NotificationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetEntryState())
    val uiState = _uiState.asStateFlow()

    private var wasNewInvoiceAttached: Boolean = false
    private var invoiceToDelete: String? = null

    fun sendEvent(event: BudgetEntryEvent) {
        when (event) {
            is BudgetEntryEvent.GoBack -> goBack()
            is BudgetEntryEvent.HideDiscardChangesModal -> hideDiscardChangesModal()
            is BudgetEntryEvent.SaveBudgetEntry -> saveBudgetEntry()
            is BudgetEntryEvent.SetBudgetEntry -> setBudgetEntry(event.budgetEntry)
            is BudgetEntryEvent.ValidateChanges -> validateChanges(event.budgetEntry)
            is BudgetEntryEvent.AttachInvoice -> attachInvoice(event)
            is BudgetEntryEvent.ToggleAttachInvoiceModal -> toggleAttachInvoiceModal(event.show)
            is BudgetEntryEvent.ToggleShowInvoiceModal -> toggleShowInvoiceModal(event.show)
            is BudgetEntryEvent.DeleteAttachedInvoice -> removeAttachedInvoice()
            is BudgetEntryEvent.DiscardChanges -> discardChanges()
            is BudgetEntryEvent.TakePhoto -> takePhoto()
            is BudgetEntryEvent.PickFile -> pickFile()
            is BudgetEntryEvent.ShareFile -> shareFile(event.filePath)
            is BudgetEntryEvent.ShowNotification -> showNotification(event.message, event.isError)
            is BudgetEntryEvent.UpdateInvoice -> updateInvoice()
        }
    }

    private fun discardChanges() {
        if (wasNewInvoiceAttached) deleteAttachedInvoice()
        goBack()
    }

    private fun removeAttachedInvoice() {
        invoiceToDelete = _uiState.value.budgetEntry?.invoice
        wasNewInvoiceAttached = false
        _uiState.update {
            val updatedBudgetEntry = it.budgetEntry?.copy(invoice = null)
            it.copy(
                budgetEntry = updatedBudgetEntry,
                isShowInvoiceModalVisible = false
            )
        }
    }

    private fun attachInvoice(event: BudgetEntryEvent.AttachInvoice) = viewModelScope.launch {
        try {
            _uiState.update { it.copy(isProcessingInvoice = true) }
            toggleAttachInvoiceModal(false)
            if (wasNewInvoiceAttached) deleteAttachedInvoice()
            val invoicePath = fileManager.saveFile(event.fileData)
            wasNewInvoiceAttached = true

            val aiBudgetEntry = if (preferencesManager.isAiProcessingEnabled()) {
                _uiState.value.budgetEntry?.let { budgetEntry ->
                    createBudgetEntryFromImageUseCase.execute(
                        imageUri = fileManager.createUri(invoicePath),
                        budgetEntry = budgetEntry
                    )
                }
            } else {
                _uiState.value.budgetEntry
            }

            _uiState.update {
                val updatedEntry = aiBudgetEntry
                    ?.copy(invoice = invoicePath)
                    ?: it.budgetEntry

                it.copy(
                    budgetEntry = updatedEntry,
                    isProcessingInvoice = false
                )
            }

            // Validate the newly attached invoice file
            validateInvoiceFile(invoicePath)
        } catch (_: Exception) {
            _uiState.update { it.copy(isProcessingInvoice = false) }
            updateInvoiceError("Something went wrong loading file, please try again")
            delay(2000)
            updateInvoiceError(null)
        }
    }

    private fun updateInvoiceError(message: String?) =
        _uiState.update { it.copy(attachInvoiceError = message) }

    private fun toggleAttachInvoiceModal(show: Boolean) =
        _uiState.update { it.copy(isAttachInvoiceModalVisible = show) }

    private fun toggleShowInvoiceModal(show: Boolean) =
        _uiState.update { it.copy(isShowInvoiceModalVisible = show) }

    private fun updateInvoice() {
        _uiState.update {
            it.copy(
                isShowInvoiceModalVisible = false,
                isAttachInvoiceModalVisible = true
            )
        }
    }

    private fun setBudgetEntry(budgetEntry: BudgetEntry) {
        _uiState.update {
            it.copy(
                budgetEntry = budgetEntry,
                emptyAmountError = null
            )
        }

        // Validate file path when budget entry changes
        validateInvoiceFile(budgetEntry.invoice)
    }

    private fun saveBudgetEntry() = viewModelScope.launch {
        _uiState.value.budgetEntry?.let { entry ->
            if (entry.amount.isBlank()) {
                showAmountError()
                return@launch
            }

            if (invoiceToDelete != null) {
                deleteDetachedInvoice()
            }

            if (entry.id < 0) {
                budgetEntryRepository.create(entry)
            } else {
                budgetEntryRepository.update(entry)
            }

            goBack()
        }
    }

    private fun deleteAttachedInvoice() {
        val invoice = _uiState.value.budgetEntry?.invoice ?: return
        deleteFile(invoice)
    }

    private fun deleteDetachedInvoice() {
        val invoice = invoiceToDelete ?: return
        deleteFile(invoice)
    }

    private fun deleteFile(invoice: String) {
        fileManager.deleteFile(invoice)
    }

    private fun showAmountError() {
        _uiState.update {
            it.copy(emptyAmountError = "Amount is mandatory") // KMP: Using string instead of R.string
        }
    }

    private fun goBack() {
        _uiState.update { it.copy(goBack = true) }
    }

    private fun validateChanges(budgetEntry: BudgetEntry) =
        _uiState.update {
            if (budgetEntry == it.budgetEntry) it.copy(goBack = true)
            else it.copy(isDiscardChangesModalVisible = true)
        }

    private fun hideDiscardChangesModal() =
        _uiState.update { it.copy(isDiscardChangesModalVisible = false) }

    private fun takePhoto() {
        cameraManager.takePhoto { fileData ->
            fileData?.let {
                sendEvent(BudgetEntryEvent.AttachInvoice(it))
            }
        }
    }

    private fun pickFile() {
        _uiState.update { it.copy(isOpeningFilePicker = true) }
        filePickerManager.pickFile { fileData ->
            _uiState.update { it.copy(isOpeningFilePicker = false) }
            fileData?.let {
                sendEvent(BudgetEntryEvent.AttachInvoice(it))
            }
        }
    }

    private fun shareFile(filePath: String) = viewModelScope.launch {
        _uiState.update { it.copy(isSharingFile = true) }
        try {
            shareManager.shareFile(filePath)
        } finally {
            // Small delay to ensure share sheet is presented before hiding loading
            delay(500)
            _uiState.update { it.copy(isSharingFile = false) }
        }
    }

    private fun showNotification(message: String, isError: Boolean) {
        if (isError) {
            notificationManager.showNotification(title = "Error", message = message)
        } else {
            notificationManager.showToast(message)
        }
    }

    private fun validateInvoiceFile(invoice: String?) = viewModelScope.launch {
        if (invoice == null) {
            _uiState.update {
                it.copy(
                    isFileValid = true,
                    validatedFilePath = null
                )
            }
            return@launch
        }

        val validatedPath = validateFilePathUseCase.execute(invoice)

        _uiState.update {
            it.copy(
                isFileValid = validatedPath != null,
                validatedFilePath = validatedPath
            )
        }
    }
}
