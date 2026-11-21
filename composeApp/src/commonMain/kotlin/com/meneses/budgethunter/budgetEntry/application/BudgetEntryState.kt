package com.meneses.budgethunter.budgetEntry.application

import com.meneses.budgethunter.billing.domain.Allowance
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
    val isSaving: Boolean = false,
    val pendingAiInvoicePath: String? = null,
    /** Whether a receipt will be read by the AI, so the form can say so before one is attached. */
    val isAiProcessingEnabled: Boolean = false,
    /**
     * This month's receipt scans as the server last counted them: read once when the screen opens and updated
     * by each scan's answer. Null when unknown (offline mode, no account, the server could not be asked).
     */
    val receiptScans: Allowance? = null
) {
    /**
     * How many receipt scans are left, when few enough to say so next to the scan action; null hides the line.
     * Only informative: scanning is never blocked here, the server decides.
     */
    fun receiptScansLeftNotice(): Int? =
        receiptScans?.takeIf { isAiProcessingEnabled && it.isRunningLow }?.remaining?.coerceAtLeast(0)

    fun shouldShowAiAutofillModal() = pendingAiInvoicePath != null

    fun shouldShowFileNotFoundModal() =
        isShowInvoiceModalVisible && budgetEntry?.invoice != null && !isFileValid

    fun shouldShowInvoiceDisplayModal() =
        isShowInvoiceModalVisible && isFileValid
}
