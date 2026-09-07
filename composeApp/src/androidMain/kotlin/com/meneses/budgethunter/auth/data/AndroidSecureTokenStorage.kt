package com.meneses.budgethunter.auth.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.meneses.budgethunter.commons.data.sync.Logger
import java.security.KeyStore

/**
 * Android implementation of [TokenStorage] backed by [EncryptedSharedPreferences].
 *
 * Tokens are encrypted at rest using AES256-GCM for values and AES256-SIV for keys,
 * protected by a MasterKey stored in the Android Keystore.
 */
class AndroidSecureTokenStorage(
    private val context: Context,
    private val logger: Logger
) : TokenStorage {

    /**
     * Opened lazily so that a device with a broken keystore fails while signing in (where the
     * error is handled) instead of while Koin is building the graph.
     */
    private val preferences: SharedPreferences by lazy { openPreferences() }

    override suspend fun saveAuthToken(token: String) {
        preferences.edit { putString(KEY_AUTH_TOKEN, token) }
    }

    override suspend fun getAuthToken(): String? =
        preferences.getString(KEY_AUTH_TOKEN, null)

    override suspend fun saveRefreshToken(token: String) {
        preferences.edit { putString(KEY_REFRESH_TOKEN, token) }
    }

    override suspend fun getRefreshToken(): String? =
        preferences.getString(KEY_REFRESH_TOKEN, null)

    override suspend fun clearTokens() {
        preferences.edit {
            remove(KEY_AUTH_TOKEN)
            remove(KEY_REFRESH_TOKEN)
        }
    }

    private fun openPreferences(): SharedPreferences =
        try {
            createEncryptedPreferences()
        } catch (e: Exception) {
            // The master key and the encrypted file have to agree with each other. They stop
            // agreeing when the keystore is reset or when app data is restored onto another
            // device, and from then on every read throws and the user can never sign in again.
            // Dropping both is the only way back: the stored session is lost, so the user signs
            // in again instead of being locked out.
            logger.warn(TAG, "Secure token storage is unreadable, recreating it", e)
            discardCorruptedStorage()
            createEncryptedPreferences()
        }

    private fun createEncryptedPreferences(): SharedPreferences =
        EncryptedSharedPreferences.create(
            /* context = */ context,
            /* fileName = */ PREFERENCES_FILE,
            /* masterKey = */ MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            /* prefKeyEncryptionScheme = */ EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            /* prefValueEncryptionScheme = */ EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

    private fun discardCorruptedStorage() {
        context.deleteSharedPreferences(PREFERENCES_FILE)
        try {
            KeyStore.getInstance(ANDROID_KEY_STORE)
                .apply { load(null) }
                .deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
        } catch (e: Exception) {
            logger.warn(TAG, "Could not delete the master key, a new one will be requested", e)
        }
    }

    private companion object {
        const val TAG = "SecureTokenStorage"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val PREFERENCES_FILE = "secure_token_storage"
        const val KEY_AUTH_TOKEN = "auth_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
