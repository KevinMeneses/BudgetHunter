package com.meneses.budgethunter.budgetEntry.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

data class BudgetEntryState(
    val budgetEntry: BudgetEntry? = null,
    val emptyAmountError: String? = null, // Changed from @StringRes to String for KMP
    val isDiscardChangesModalVisible: Boolean = false,
    val isAttachInvoiceModalVisible: Boolean = false,
    val isShowInvoiceModalVisible: Boolean = false,
    val attachInvoiceError: String? = null,
    val goBack: Boolean = false,
    val isProcessingInvoice: Boolean = false,
    val isFileValid: Boolean = true,
    val validatedFilePath: String? = null,
    val isSharingFile: Boolean = false,
    val isOpeningFilePicker: Boolean = false
) {
    fun shouldShowFileNotFoundModal() =
        isShowInvoiceModalVisible && budgetEntry?.invoice != null && !isFileValid

    fun shouldShowInvoiceDisplayModal() =
        isShowInvoiceModalVisible && isFileValid
}
