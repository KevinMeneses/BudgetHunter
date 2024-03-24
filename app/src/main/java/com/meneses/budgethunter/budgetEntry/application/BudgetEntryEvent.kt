package com.meneses.budgethunter.budgetEntry.application

import android.content.ContentResolver
import android.net.Uri
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry
import java.io.File

sealed interface BudgetEntryEvent {
    data class SetBudgetEntry(val budgetEntry: BudgetEntry) : BudgetEntryEvent
    object SaveBudgetEntry : BudgetEntryEvent
    data class ValidateChanges(val budgetEntry: BudgetEntry) : BudgetEntryEvent
    object HideDiscardChangesModal : BudgetEntryEvent
    data class ToggleAttachInvoiceModal(val show: Boolean) : BudgetEntryEvent
    data class ToggleShowInvoiceModal(val show: Boolean) : BudgetEntryEvent

    data class SaveInvoice(
        val fileToSave: Uri,
        val contentResolver: ContentResolver,
        val internalFilesDir: File?
    ) : BudgetEntryEvent

    object GoBack : BudgetEntryEvent
    object DeleteAttachedInvoice : BudgetEntryEvent
}
