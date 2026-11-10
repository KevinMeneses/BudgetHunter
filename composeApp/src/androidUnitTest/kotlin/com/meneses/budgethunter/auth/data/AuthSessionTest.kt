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

    @Test
    fun `onSessionExpired asks for the redirect and the notice`() {
        session.onSessionExpired()

        assertTrue(session.isSignInRedirectPending.value)
        assertTrue(session.consumeSessionExpiredNotice())
    }

    @Test
    fun `the session expired notice is consumed once`() {
        session.onSessionExpired()
        session.consumeSessionExpiredNotice()

        assertFalse(session.consumeSessionExpiredNotice())
    }

    @Test
    fun `a sign out does not show the session expired notice`() {
        session.onSignedOut()

        assertFalse(session.consumeSessionExpiredNotice())
    }
}
