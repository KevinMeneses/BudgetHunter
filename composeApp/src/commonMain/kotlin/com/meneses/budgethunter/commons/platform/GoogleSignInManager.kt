package com.meneses.budgethunter.commons.platform

/**
 * Asks the platform for a Google ID token, which the backend exchanges for a session.
 *
 * Callback-based rather than `suspend` on purpose: a `suspend` function in an interface cannot
 * be implemented from Swift, and the iOS side of this is native Swift. [CameraManager] takes the
 * same shape for the same reason.
 */
interface GoogleSignInManager {
    /** False when the app was built without a Google client id, which hides the sign-in button. */
    val isAvailable: Boolean

    fun signIn(onResult: (GoogleSignInResult) -> Unit)

    /**
     * Drops the credential the platform cached for this app.
     *
     * Without it Android keeps auto-selecting the last account, so a signed-out user can never
     * switch to a different Google identity.
     */
    fun signOut()
}

sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult

    /**
     * The user dismissed the account picker. This is the most common outcome of the whole flow,
     * so it is kept apart from [Failure]: backing out is not an error and must not show one.
     */
    data object Cancelled : GoogleSignInResult

    /** No Google account is available on the device — worth a different, actionable message. */
    data object NoCredentialAvailable : GoogleSignInResult

    data class Failure(val message: String) : GoogleSignInResult
}
