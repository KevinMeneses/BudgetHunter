package com.meneses.budgethunter.sms.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meneses.budgethunter.sms.domain.MovementFingerprints
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The last movements registered from notifications ([MovementFingerprints]), kept on disk so a
 * notification posted again after the process died is not registered a second time.
 *
 * Kept on the device and through sign out: they say what this device already read, whoever reads next.
 * A single in Koin: its lock is what keeps two copies handled at once from both being registered.
 */
class RegisteredMovements(private val preferences: DataStore<Preferences>) {

    private val mutex = Mutex()

    /**
     * Runs [register] unless [fingerprint] is among the last registered, and remembers it once
     * [register] returns. True when it ran. A [register] that throws leaves nothing remembered, so
     * the next copy of the notification tries again instead of the movement being lost. Once it
     * returned, the movement is registered even if remembering it fails (a full disk): that only
     * risks a copy later, while failing here would report an entry that was added as an error.
     * For the same reason, a list that cannot be read is taken as empty: a copy is better than a
     * movement lost.
     */
    suspend fun registerOnce(fingerprint: String, register: suspend () -> Unit): Boolean = mutex.withLock {
        val stored = try {
            preferences.data.first()[KEY_REGISTERED_MOVEMENTS]
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (MovementFingerprints.isRemembered(stored, fingerprint)) return@withLock false

        register()
        try {
            preferences.edit { current ->
                MovementFingerprints.remember(current[KEY_REGISTERED_MOVEMENTS], fingerprint)
                    ?.let { current[KEY_REGISTERED_MOVEMENTS] = it }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Registered all the same: see above
        }
        true
    }

    private companion object {
        val KEY_REGISTERED_MOVEMENTS = stringPreferencesKey("registered_movements")
    }
}
