package com.meneses.budgethunter.sms.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meneses.budgethunter.sms.domain.MovementFingerprints

/**
 * The last movements registered from notifications ([MovementFingerprints]), kept on disk so a
 * notification posted again after the process died is not registered a second time.
 *
 * Kept on the device and through sign out: they say what this device already read, whoever reads next.
 */
class RegisteredMovements(private val preferences: DataStore<Preferences>) {

    /**
     * True the first time [fingerprint] is seen, false while it is among the last remembered.
     * Checked and stored in one edit, which DataStore runs one at a time, so two copies of a
     * notification handled at once cannot both get through.
     */
    suspend fun markIfNew(fingerprint: String): Boolean {
        var isNew = false
        preferences.edit { stored ->
            val updated = MovementFingerprints.remember(stored[KEY_REGISTERED_MOVEMENTS], fingerprint)
            isNew = updated != null
            if (updated != null) stored[KEY_REGISTERED_MOVEMENTS] = updated
        }
        return isNew
    }

    private companion object {
        val KEY_REGISTERED_MOVEMENTS = stringPreferencesKey("registered_notification_movements")
    }
}
