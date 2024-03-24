package com.meneses.budgethunter.budgetEntry.application

import androidx.annotation.StringRes
import com.meneses.budgethunter.budgetEntry.domain.BudgetEntry

data class BudgetEntryState(
    val budgetEntry: BudgetEntry? = null,
    @StringRes val emptyAmountError: Int? = null,
    val isDiscardChangesModalVisible: Boolean = false,
    val isAttachInvoiceModalVisible: Boolean = false,
    val isShowInvoiceModalVisible: Boolean = false,
    val attachInvoiceError: String? = null,
    val goBack: Boolean = false
)
