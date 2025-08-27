package com.meneses.budgethunter.budgetEntry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryEvent
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryState
import com.meneses.budgethunter.budgetEntry.application.CreateBudgetEntryFromImageUseCase
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.data.FileManager
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.CameraManager
import com.meneses.budgethunter.commons.platform.FilePickerManager
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.platform.ShareManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetEntryViewModel(
    private val budgetEntryRepository: BudgetEntryRepository,
    private val createBudgetEntryFromImageUseCase: CreateBudgetEntryFromImageUseCase,
    private val preferencesManager: PreferencesManager,
    private val fileManager: FileManager,
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
        } catch (e: Exception) {
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

    private fun setBudgetEntry(budgetEntry: BudgetEntry?) =
        _uiState.update {
            it.copy(
                budgetEntry = budgetEntry,
                emptyAmountError = null
            )
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
            it.copy(emptyAmountError = R.string.amount_mandatory)
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
        filePickerManager.pickFile { fileData ->
            fileData?.let {
                sendEvent(BudgetEntryEvent.AttachInvoice(it))
            }
        }
    }

    private fun shareFile(filePath: String) {
        shareManager.shareFile(filePath)
    }

    private fun showNotification(message: String, isError: Boolean) {
        if (isError) {
            notificationManager.showError(message)
        } else {
            notificationManager.showToast(message)
        }
    }
}
