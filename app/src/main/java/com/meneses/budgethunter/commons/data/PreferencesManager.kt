package com.meneses.budgethunter.commons.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class PreferencesManager(private val preferences: DataStore<Preferences>) {

    suspend fun isSmsReadingEnabled() = preferences.data
        .map { it[KEY_SMS_READING_ENABLED] }
        .firstOrNull() == true

    suspend fun setSmsReadingEnabled(value: Boolean) {
        preferences.edit { it[KEY_SMS_READING_ENABLED] = value }
    }

    suspend fun getDefaultBudgetId() = preferences.data
        .map { it[KEY_DEFAULT_BUDGET_ID] }
        .firstOrNull() ?: -1

    suspend fun setDefaultBudgetId(value: Int) {
        preferences.edit { it[KEY_DEFAULT_BUDGET_ID] = value }
    }

    suspend fun getSelectedBankIds() = preferences.data
        .map { it[KEY_SELECTED_BANK_IDS] }
        .firstOrNull() ?: emptySet()

    suspend fun setSelectedBankIds(value: Set<String>) {
        preferences.edit { it[KEY_SELECTED_BANK_IDS] = value }
    }

    suspend fun isAiProcessingEnabled() = preferences.data
        .map { it[KEY_AI_PROCESSING_ENABLED] }
        .firstOrNull() != false

    suspend fun setAiProcessingEnabled(value: Boolean) {
        preferences.edit { it[KEY_AI_PROCESSING_ENABLED] = value }
    }

    companion object {
        private val KEY_SMS_READING_ENABLED = booleanPreferencesKey("sms_reading_enabled")
        private val KEY_DEFAULT_BUDGET_ID = intPreferencesKey("default_budget_id")
        private val KEY_SELECTED_BANK_IDS = stringSetPreferencesKey("selected_bank_ids")
        private val KEY_AI_PROCESSING_ENABLED = booleanPreferencesKey("ai_processing_enabled")
    }
}
