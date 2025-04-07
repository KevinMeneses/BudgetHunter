package com.meneses.budgethunter.budgetEntry

import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryEvent
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryState
import com.meneses.budgethunter.budgetEntry.application.GetAIBudgetEntryFromImageUseCase
import com.meneses.budgethunter.budgetEntry.data.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class BudgetEntryViewModel(
    private val budgetEntryRepository: BudgetEntryRepository = BudgetEntryRepository(),
    private val getAIBudgetEntryFromImageUseCase: GetAIBudgetEntryFromImageUseCase = GetAIBudgetEntryFromImageUseCase()
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
            if (wasNewInvoiceAttached) deleteAttachedInvoice()
            val invoiceDir = saveInvoiceInAppInternalStorage(event)
            wasNewInvoiceAttached = true

            val aiBudgetEntry = _uiState.value.budgetEntry?.let { budgetEntry ->
                getAIBudgetEntryFromImageUseCase.execute(
                    imageUri = invoiceDir.toUri(),
                    budgetEntry = budgetEntry,
                    contentResolver = event.contentResolver
                )
            }

            _uiState.update {
                val updatedEntry = aiBudgetEntry
                    ?.copy(invoice = invoiceDir.absolutePath)
                    ?: it.budgetEntry

                it.copy(budgetEntry = updatedEntry)
            }

            toggleAttachInvoiceModal(false)
        } catch (e: Exception) {
            e
            updateInvoiceError("Something went wrong loading file, please try again")
            delay(2000)
            updateInvoiceError(null)
        }
    }

    private fun saveInvoiceInAppInternalStorage(event: BudgetEntryEvent.AttachInvoice): File {
        val invoiceToSave = event.contentResolver
            .openInputStream(event.fileToSave)
            .use { it!!.readBytes() }

        val fileFormat = if (event.fileToSave.path?.contains("image") == true) ".jpg" else ".pdf"
        val invoiceDir =
            File(event.internalFilesDir, System.currentTimeMillis().toString() + fileFormat)
        invoiceDir.outputStream().use { it.write(invoiceToSave) }

        return invoiceDir
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
        try {
            File(invoice).delete()
        } catch (e: IOException) {
            /* no-op */
        }
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
}
