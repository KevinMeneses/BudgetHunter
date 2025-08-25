package com.meneses.budgethunter.commons.data

interface PreferencesManager {
    var isSmsReadingEnabled: Boolean
    var defaultBudgetId: Int
    var selectedBankIds: Set<String>
    var isAiProcessingEnabled: Boolean
}
