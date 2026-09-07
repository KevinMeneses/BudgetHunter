package com.meneses.budgethunter.auth.data

import com.meneses.budgethunter.commons.platform.KeychainStore

/**
 * iOS implementation of [TokenStorage] backed by the system Keychain.
 *
 * The actual Keychain access is implemented natively in Swift ([KeychainStore], see
 * `IOSKeychainStore.swift`) and injected via the IOSBridge. This avoids Kotlin/Native's
 * fragile ObjC↔CoreFoundation toll-free bridging casts against the Security framework,
 * which silently failed at runtime.
 *
 * Keychain operations are synchronous and fast, so the suspend functions delegate directly.
 */
class IosSecureTokenStorage(
    private val keychain: KeychainStore
) : TokenStorage {

    override suspend fun saveAuthToken(token: String) = keychain.save(KEY_AUTH_TOKEN, token)

    override suspend fun getAuthToken(): String? = keychain.read(KEY_AUTH_TOKEN)

    override suspend fun saveRefreshToken(token: String) = keychain.save(KEY_REFRESH_TOKEN, token)

    override suspend fun getRefreshToken(): String? = keychain.read(KEY_REFRESH_TOKEN)

    override suspend fun clearTokens() {
        keychain.delete(KEY_AUTH_TOKEN)
        keychain.delete(KEY_REFRESH_TOKEN)
    }

    private companion object {
        const val KEY_AUTH_TOKEN = "auth_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
