package com.meneses.budgethunter.auth.application

import com.meneses.budgethunter.auth.data.AuthRepository
import com.meneses.budgethunter.commons.data.PreferencesManager
import kotlinx.coroutines.CancellationException

/**
 * Whether the signed-in account has to accept the privacy policy: never accepted (accounts from
 * before acceptances were recorded) or accepted an older version than the server's.
 *
 * Never blocks the app: offline mode (no account, nothing sent to the server), no session, no
 * connection, an error or a server that does not report the policy all answer null, and the next
 * sign in or app start asks again.
 */
class CheckPrivacyPolicyUseCase(
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager
) {
    /** @return the server's current version when it has to be accepted, else null. */
    suspend fun pendingVersion(): String? =
        try {
            if (preferencesManager.isOfflineModeEnabled() || !authRepository.isAuthenticated()) {
                null
            } else {
                val status = authRepository.getCurrentUser().getOrNull()?.privacyPolicy
                val current = status?.currentVersion
                // Signed out while the answer was on its way: there is nobody left to ask
                if (current.isNullOrBlank() || current == status?.acceptedVersion ||
                    !authRepository.isAuthenticated()
                ) {
                    null
                } else {
                    current
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
}
