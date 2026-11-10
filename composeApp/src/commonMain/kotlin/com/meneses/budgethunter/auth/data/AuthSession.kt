package com.meneses.budgethunter.auth.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/**
 * Tells navigation that the session ended and the user has to go to sign in.
 *
 * A pending flag rather than an event from the screen that signed out: the sign out outlives
 * that screen, and an event sent after the user left it was lost, leaving them inside the app
 * with no session. Navigation consumes it once, so choosing offline mode afterwards is not
 * redirected again.
 */
class AuthSession {
    private val _isSignInRedirectPending = MutableStateFlow(false)
    val isSignInRedirectPending: StateFlow<Boolean> = _isSignInRedirectPending.asStateFlow()

    private val _isSessionExpiredNoticePending = MutableStateFlow(false)

    fun onSignedOut() {
        _isSignInRedirectPending.value = true
    }

    /**
     * The server rejected the session (the refresh token expired or was replaced by a sign in on
     * another device) while the app was open. Unlike [onSignedOut] nothing on the device is
     * cleared: the unsynced entries are pushed once the same account signs in again. Without
     * the redirect the user stayed inside the app, every change "saved locally" with no way to
     * know the session was gone.
     */
    fun onSessionExpired() {
        _isSessionExpiredNoticePending.value = true
        _isSignInRedirectPending.value = true
    }

    fun consumeSignInRedirect() {
        _isSignInRedirectPending.value = false
    }

    /** True once after [onSessionExpired], so sign in can tell the user why they are there. */
    fun consumeSessionExpiredNotice(): Boolean =
        _isSessionExpiredNoticePending.getAndUpdate { false }
}
