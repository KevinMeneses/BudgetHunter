package com.meneses.budgethunter.fakes.manager

import com.meneses.budgethunter.commons.data.PreferencesManager

class FakePreferencesManager : PreferencesManager {
    private var smsReadingEnabled = false
    private var aiProcessingEnabled = false
    private var defaultBudgetId = -1
    private var selectedBankIds = emptySet<String>()

    var aiEnabled: Boolean
        get() = aiProcessingEnabled
        set(value) { aiProcessingEnabled = value }

    override suspend fun isSmsReadingEnabled(): Boolean = smsReadingEnabled
    override suspend fun setSmsReadingEnabled(enabled: Boolean) {
        smsReadingEnabled = enabled
    }

    override suspend fun isAiProcessingEnabled(): Boolean = aiProcessingEnabled
    override suspend fun setAiProcessingEnabled(enabled: Boolean) {
        aiProcessingEnabled = enabled
    }

    override suspend fun getDefaultBudgetId(): Int = defaultBudgetId
    override suspend fun setDefaultBudgetId(budgetId: Int) {
        defaultBudgetId = budgetId
    }

    override suspend fun getSelectedBankIds(): Set<String> = selectedBankIds
    override suspend fun setSelectedBankIds(bankIds: Set<String>) {
        selectedBankIds = bankIds
    }
}
