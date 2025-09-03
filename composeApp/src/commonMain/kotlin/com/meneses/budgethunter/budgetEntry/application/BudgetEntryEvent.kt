package com.meneses.budgethunter.budgetEntry.application

import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import com.meneses.budgethunter.commons.data.FileData

sealed interface BudgetEntryEvent {
    data class SetBudgetEntry(val budgetEntry: BudgetEntry) : BudgetEntryEvent
    data object SaveBudgetEntry : BudgetEntryEvent
    data class ValidateChanges(val budgetEntry: BudgetEntry) : BudgetEntryEvent
    data object DiscardChanges : BudgetEntryEvent
    data object HideDiscardChangesModal : BudgetEntryEvent
    data class ToggleAttachInvoiceModal(val show: Boolean) : BudgetEntryEvent
    data class ToggleShowInvoiceModal(val show: Boolean) : BudgetEntryEvent
    data class AttachInvoice(val fileData: FileData) : BudgetEntryEvent
    data object TakePhoto : BudgetEntryEvent
    data object PickFile : BudgetEntryEvent
    data class ShareFile(val filePath: String) : BudgetEntryEvent
    data class ShowNotification(val message: String, val isError: Boolean = false) : BudgetEntryEvent
    data object GoBack : BudgetEntryEvent
    data object DeleteAttachedInvoice : BudgetEntryEvent
}