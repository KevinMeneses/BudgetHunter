package com.meneses.budgethunter.budgetEntry.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import org.jetbrains.compose.resources.StringResource

data class BudgetEntryState(
    val budgetEntry: BudgetEntry? = null,
    val emptyAmountError: StringResource? = null,
    val isDiscardChangesModalVisible: Boolean = false,
    val isAttachInvoiceModalVisible: Boolean = false,
    val isShowInvoiceModalVisible: Boolean = false,
    val isProcessingInvoice: Boolean = false,
    val isFileValid: Boolean = true,
    val validatedFilePath: String? = null,
    val isSharingFile: Boolean = false,
    val isOpeningFilePicker: Boolean = false,
    val isSaving: Boolean = false
) {
    fun shouldShowFileNotFoundModal() =
        isShowInvoiceModalVisible && budgetEntry?.invoice != null && !isFileValid

    fun shouldShowInvoiceDisplayModal() =
        isShowInvoiceModalVisible && isFileValid
}
