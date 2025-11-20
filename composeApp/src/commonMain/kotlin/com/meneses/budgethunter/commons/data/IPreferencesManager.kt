package com.meneses.budgethunter.commons.data

interface IPreferencesManager {
    suspend fun isSmsReadingEnabled(): Boolean
    suspend fun setSmsReadingEnabled(value: Boolean)
    suspend fun getDefaultBudgetId(): Int
    suspend fun setDefaultBudgetId(value: Int)
    suspend fun getSelectedBankIds(): Set<String>
    suspend fun setSelectedBankIds(value: Set<String>)
    suspend fun isAiProcessingEnabled(): Boolean
    suspend fun setAiProcessingEnabled(value: Boolean)
}
