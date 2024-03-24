package com.meneses.budgethunter.budgetEntry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meneses.budgethunter.R
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryEvent
import com.meneses.budgethunter.budgetEntry.application.BudgetEntryState
import com.meneses.budgethunter.budgetEntry.data.repository.BudgetEntryLocalRepository
import com.meneses.budgethunter.budgetEntry.data.repository.BudgetEntryRepository
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class BudgetEntryViewModel(
    private val budgetEntryRepository: BudgetEntryRepository = BudgetEntryLocalRepository(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetEntryState())
    val uiState = _uiState.asStateFlow()

    private var invoiceToDelete: String? = null

    fun sendEvent(event: BudgetEntryEvent) {
        when (event) {
            is BudgetEntryEvent.GoBack -> goBack()
            is BudgetEntryEvent.HideDiscardChangesModal -> hideDiscardChangesModal()
            is BudgetEntryEvent.SaveBudgetEntry -> saveBudgetEntry()
            is BudgetEntryEvent.SetBudgetEntry -> setBudgetEntry(event.budgetEntry)
            is BudgetEntryEvent.ValidateChanges -> validateChanges(event.budgetEntry)
            is BudgetEntryEvent.SaveInvoice -> saveInvoice(event)
            is BudgetEntryEvent.ToggleAttachInvoiceModal -> toggleAttachInvoiceModal(event.show)
            is BudgetEntryEvent.ToggleShowInvoiceModal -> toggleShowInvoiceModal(event.show)
            is BudgetEntryEvent.DeleteAttachedInvoice -> removeAttachedInvoice()
        }
    }

    private fun removeAttachedInvoice() {
        invoiceToDelete = _uiState.value.budgetEntry?.invoice
        _uiState.update {
            val updatedBudgetEntry = it.budgetEntry?.copy(invoice = null)
            it.copy(
                budgetEntry = updatedBudgetEntry,
                isShowInvoiceModalVisible = false
            )
        }
    }

    private fun saveInvoice(event: BudgetEntryEvent.SaveInvoice) {
        viewModelScope.launch(dispatcher) {
            try {
                val invoiceToSave = event.contentResolver
                    .openInputStream(event.fileToSave)
                    .use { it!!.readBytes() }

                val invoiceDir = File(event.internalFilesDir, System.currentTimeMillis().toString())
                invoiceDir.outputStream().use { it.write(invoiceToSave) }

                _uiState.update {
                    val updatedEntry = it.budgetEntry?.copy(invoice = invoiceDir.absolutePath)
                    it.copy(budgetEntry = updatedEntry)
                }

                toggleAttachInvoiceModal(false)
            } catch (e: Exception) {
                updateInvoiceError("Something went wrong loading file, please try again")
                delay(2000)
                updateInvoiceError(null)
            }
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

    private fun saveBudgetEntry() {
        viewModelScope.launch(dispatcher) {
            _uiState.value.budgetEntry?.let { entry ->
                if (entry.amount.isBlank()) {
                    showAmountError()
                    return@launch
                }

                deleteInvoiceIfNecessary()
                if (entry.id < 0) budgetEntryRepository.create(entry)
                else budgetEntryRepository.update(entry)
                goBack()
            }
        }
    }

    private fun deleteInvoiceIfNecessary() {
        val invoice = invoiceToDelete ?: return
        try {
            File(invoice).delete()
        } catch (e: IOException) {
            return
        }
    }

    private fun showAmountError() {
        _uiState.update {
            it.copy(emptyAmountError = R.string.amount_mandatory)
        }
    }

    private fun goBack() =
        _uiState.update { it.copy(goBack = true) }

    private fun validateChanges(budgetEntry: BudgetEntry) =
        _uiState.update {
            if (budgetEntry == it.budgetEntry) it.copy(goBack = true)
            else it.copy(isDiscardChangesModalVisible = true)
        }

    private fun hideDiscardChangesModal() =
        _uiState.update { it.copy(isDiscardChangesModalVisible = false) }
}
