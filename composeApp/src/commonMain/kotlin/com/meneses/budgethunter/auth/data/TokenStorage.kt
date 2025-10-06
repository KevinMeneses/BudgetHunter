package com.meneses.budgethunter.auth.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class TokenStorage(private val dataStore: DataStore<Preferences>) {

    private val authTokenKey = stringPreferencesKey("auth_token")
    private val refreshTokenKey = stringPreferencesKey("refresh_token")

    suspend fun saveAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[authTokenKey] = token
        }
    }

    suspend fun getAuthToken(): String? {
        return dataStore.data.map { preferences ->
            preferences[authTokenKey]
        }.first()
    }

    suspend fun saveRefreshToken(token: String) {
        dataStore.edit { preferences ->
            preferences[refreshTokenKey] = token
        }
    }

    suspend fun getRefreshToken(): String? {
        return dataStore.data.map { preferences ->
            preferences[refreshTokenKey]
        }.first()
    }

    suspend fun clearTokens() {
        dataStore.edit { preferences ->
            preferences.remove(authTokenKey)
            preferences.remove(refreshTokenKey)
        }
    }
}
