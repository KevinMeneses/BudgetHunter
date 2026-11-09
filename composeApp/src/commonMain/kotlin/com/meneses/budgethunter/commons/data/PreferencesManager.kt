package com.meneses.budgethunter.commons.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.meneses.budgethunter.sms.domain.BankChannels
import com.meneses.budgethunter.sms.domain.ReadingChannel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class PreferencesManager(private val preferences: DataStore<Preferences>) {

    suspend fun isSmsReadingEnabled() = preferences.data
        .map { it[KEY_SMS_READING_ENABLED] }
        .firstOrNull() == true

    suspend fun setSmsReadingEnabled(value: Boolean) {
        preferences.edit { it[KEY_SMS_READING_ENABLED] = value }
    }

    /** Reading the notifications of bank apps, Android only. Kept on this device, not on the account. */
    suspend fun isBankNotificationReadingEnabled() = preferences.data
        .map { it[KEY_BANK_NOTIFICATION_READING_ENABLED] }
        .firstOrNull() == true

    suspend fun setBankNotificationReadingEnabled(value: Boolean) {
        preferences.edit { it[KEY_BANK_NOTIFICATION_READING_ENABLED] = value }
    }

    /**
     * Reading the notifications of email apps for the emails of the selected banks, Android only.
     * Kept on this device, not on the account.
     */
    suspend fun isBankEmailReadingEnabled() = preferences.data
        .map { it[KEY_BANK_EMAIL_READING_ENABLED] }
        .firstOrNull() == true

    suspend fun setBankEmailReadingEnabled(value: Boolean) {
        preferences.edit { it[KEY_BANK_EMAIL_READING_ENABLED] = value }
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

    /**
     * The channels picked for each bank ([BankChannels]). Kept on this device, not on the account:
     * the channels are read by this device's switches.
     */
    suspend fun getBankChannels(): Map<String, Set<ReadingChannel>> = preferences.data
        .map { it[KEY_BANK_CHANNELS] }
        .firstOrNull()
        ?.let { BankChannels.decode(it) }
        ?: emptyMap()

    suspend fun setBankChannels(value: Map<String, Set<ReadingChannel>>) {
        preferences.edit { it[KEY_BANK_CHANNELS] = BankChannels.encode(value) }
    }

    suspend fun isAiProcessingEnabled() = preferences.data
        .map { it[KEY_AI_PROCESSING_ENABLED] }
        .firstOrNull() != false

    suspend fun setAiProcessingEnabled(value: Boolean) {
        preferences.edit { it[KEY_AI_PROCESSING_ENABLED] = value }
    }

    suspend fun isOfflineModeEnabled() = preferences.data
        .map { it[KEY_OFFLINE_MODE_ENABLED] }
        .firstOrNull() == true

    suspend fun setOfflineModeEnabled(value: Boolean) {
        preferences.edit { it[KEY_OFFLINE_MODE_ENABLED] = value }
    }

    /**
     * Set when a sign out could not delete the device's budgets and entries; whoever uses the
     * app next finishes the job (`PrepareDataForAccountUseCase.finishPendingWipe`). Like the last
     * email, it survives [clearUserPreferences].
     */
    suspend fun isLocalWipePending() = preferences.data
        .map { it[KEY_LOCAL_WIPE_PENDING] }
        .firstOrNull() == true

    suspend fun setLocalWipePending(value: Boolean) {
        preferences.edit { it[KEY_LOCAL_WIPE_PENDING] = value }
    }

    /**
     * Email of the last account that signed in on this device. Deliberately left alone by sign out
     * and [clearUserPreferences]: it is what tells the next sign in whether the account changed.
     */
    suspend fun getLastSignedInEmail(): String? = preferences.data
        .map { it[KEY_LAST_SIGNED_IN_EMAIL] }
        .firstOrNull()

    suspend fun setLastSignedInEmail(value: String) {
        preferences.edit { it[KEY_LAST_SIGNED_IN_EMAIL] = value }
    }

    /**
     * Forgets everything the settings screen saves. Used on sign out so the next account to sign
     * in on this device does not inherit the previous one's choices.
     */
    suspend fun clearUserPreferences() {
        preferences.edit {
            it.remove(KEY_SMS_READING_ENABLED)
            it.remove(KEY_BANK_NOTIFICATION_READING_ENABLED)
            it.remove(KEY_BANK_EMAIL_READING_ENABLED)
            it.remove(KEY_DEFAULT_BUDGET_ID)
            it.remove(KEY_SELECTED_BANK_IDS)
            it.remove(KEY_BANK_CHANNELS)
            it.remove(KEY_AI_PROCESSING_ENABLED)
        }
    }

    companion object {
        private val KEY_SMS_READING_ENABLED = booleanPreferencesKey("sms_reading_enabled")
        private val KEY_BANK_NOTIFICATION_READING_ENABLED = booleanPreferencesKey("bank_notification_reading_enabled")
        private val KEY_BANK_EMAIL_READING_ENABLED = booleanPreferencesKey("bank_email_reading_enabled")
        private val KEY_DEFAULT_BUDGET_ID = intPreferencesKey("default_budget_id")
        private val KEY_SELECTED_BANK_IDS = stringSetPreferencesKey("selected_bank_ids")
        private val KEY_BANK_CHANNELS = stringSetPreferencesKey("bank_reading_channels")
        private val KEY_AI_PROCESSING_ENABLED = booleanPreferencesKey("ai_processing_enabled")
        private val KEY_OFFLINE_MODE_ENABLED = booleanPreferencesKey("offline_mode_enabled")
        private val KEY_LAST_SIGNED_IN_EMAIL = stringPreferencesKey("last_signed_in_email")
        private val KEY_LOCAL_WIPE_PENDING = booleanPreferencesKey("local_wipe_pending")
    }
}
