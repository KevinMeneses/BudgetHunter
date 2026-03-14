package com.meneses.budgethunter.budgetEntry.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.data.FileData

sealed interface BudgetEntryIntent {
    data class SetBudgetEntry(val budgetEntry: BudgetEntry) : BudgetEntryIntent
    data object SaveBudgetEntry : BudgetEntryIntent
    data class ValidateChanges(val budgetEntry: BudgetEntry) : BudgetEntryIntent
    data object DiscardChanges : BudgetEntryIntent
    data object HideDiscardChangesModal : BudgetEntryIntent
    data class ToggleAttachInvoiceModal(val show: Boolean) : BudgetEntryIntent
    data class ToggleShowInvoiceModal(val show: Boolean) : BudgetEntryIntent
    data class AttachInvoice(val fileData: FileData) : BudgetEntryIntent
    data object TakePhoto : BudgetEntryIntent
    data object PickFile : BudgetEntryIntent
    data class ShareFile(val filePath: String) : BudgetEntryIntent
    data class ShowNotification(val message: String, val isError: Boolean = false) : BudgetEntryIntent
    data object GoBack : BudgetEntryIntent
    data object DeleteAttachedInvoice : BudgetEntryIntent
    data object UpdateInvoice : BudgetEntryIntent
}
