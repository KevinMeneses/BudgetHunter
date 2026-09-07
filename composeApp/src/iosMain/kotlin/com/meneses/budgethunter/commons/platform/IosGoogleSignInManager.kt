package com.meneses.budgethunter.commons.platform

/**
 * Placeholder until the native Swift implementation lands.
 *
 * Reports itself unavailable, so the sign-in and sign-up screens simply do not offer the Google
 * button on iOS. The real one arrives with the GoogleSignIn SDK and is injected through
 * `IOSBridge`, the way [KeychainStore] already is.
 */
class IosGoogleSignInManager : GoogleSignInManager {
    override val isAvailable = false

    override fun signIn(onResult: (GoogleSignInResult) -> Unit) {
        onResult(GoogleSignInResult.Failure("Google sign in is not available on iOS yet"))
    }

    override fun signOut() = Unit
}
