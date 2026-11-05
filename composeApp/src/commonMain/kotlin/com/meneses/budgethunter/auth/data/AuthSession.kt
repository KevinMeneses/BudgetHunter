package com.meneses.budgethunter.auth.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    fun onSignedOut() {
        _isSignInRedirectPending.value = true
    }

    fun consumeSignInRedirect() {
        _isSignInRedirectPending.value = false
    }
}
