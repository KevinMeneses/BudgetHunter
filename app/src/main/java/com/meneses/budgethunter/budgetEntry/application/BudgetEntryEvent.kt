package com.meneses.budgethunter.budgetEntry.application

import android.content.ContentResolver
import android.net.Uri
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import java.io.File

sealed interface BudgetEntryEvent {
    data class SetBudgetEntry(val budgetEntry: BudgetEntry) : BudgetEntryEvent
    data object SaveBudgetEntry : BudgetEntryEvent
    data class ValidateChanges(val budgetEntry: BudgetEntry) : BudgetEntryEvent
    data object DiscardChanges : BudgetEntryEvent
    data object HideDiscardChangesModal : BudgetEntryEvent
    data class ToggleAttachInvoiceModal(val show: Boolean) : BudgetEntryEvent
    data class ToggleShowInvoiceModal(val show: Boolean) : BudgetEntryEvent

    data class AttachInvoice(
        val fileToSave: Uri,
        val contentResolver: ContentResolver,
        val internalFilesDir: File?
    ) : BudgetEntryEvent

    data object GoBack : BudgetEntryEvent
    data object DeleteAttachedInvoice : BudgetEntryEvent
}
