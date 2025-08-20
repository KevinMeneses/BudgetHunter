package com.meneses.budgethunter.commons.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var isCollaborationEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_COLLABORATION_ENABLED, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_COLLABORATION_ENABLED, value) }

    var isSmsReadingEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_SMS_READING_ENABLED, false)
        set(value) = sharedPreferences.edit { putBoolean(KEY_SMS_READING_ENABLED, value) }

    var defaultBudgetId: Int
        get() = sharedPreferences.getInt(KEY_DEFAULT_BUDGET_ID, -1)
        set(value) = sharedPreferences.edit { putInt(KEY_DEFAULT_BUDGET_ID, value) }

    // Store multiple selected bank IDs as a comma-separated string
    var selectedBankIds: Set<String>
        get() {
            val bankIdsString = sharedPreferences.getString(KEY_SELECTED_BANK_IDS, "") ?: ""
            return if (bankIdsString.isNotEmpty()) {
                bankIdsString.split(",").toSet()
            } else {
                emptySet()
            }
        }
        set(value) = sharedPreferences.edit {
            putString(KEY_SELECTED_BANK_IDS, value.joinToString(","))
        }

    companion object {
        private const val PREF_NAME = "budget_hunter_preferences"
        private const val KEY_COLLABORATION_ENABLED = "collaboration_enabled"
        private const val KEY_SMS_READING_ENABLED = "sms_reading_enabled"
        private const val KEY_DEFAULT_BUDGET_ID = "default_budget_id"
        private const val KEY_SELECTED_BANK_IDS = "selected_bank_ids"
    }
}
