package com.meneses.budgethunter.auth.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthSessionTest {

    private val session = AuthSession()

    @Test
    fun `redirect is not pending initially`() {
        assertFalse(session.isSignInRedirectPending.value)
    }

    @Test
    fun `onSignedOut sets the redirect pending`() {
        session.onSignedOut()

        assertTrue(session.isSignInRedirectPending.value)
    }

    @Test
    fun `consumeSignInRedirect clears the pending redirect`() {
        session.onSignedOut()

        session.consumeSignInRedirect()

        assertFalse(session.isSignInRedirectPending.value)
    }
}
