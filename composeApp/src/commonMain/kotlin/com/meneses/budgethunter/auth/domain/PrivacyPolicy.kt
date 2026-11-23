package com.meneses.budgethunter.auth.domain

import com.meneses.budgethunter.commons.data.network.getBaseUrl

/**
 * The privacy policy the backend serves at `/privacidad`.
 *
 * [VERSION] is the one sign up and Google sign in accept. It is a constant rather than asked from
 * the server because nothing can be asked before there is an account (`GET /api/users/me` needs a
 * session, and the backend has no public endpoint for it), and a request at that moment would be
 * one more way for sign up to fail. If the backend's policy is newer than this build, the server
 * ignores the stale version (it never rejects it), and the check that runs right after signing in
 * ([com.meneses.budgethunter.auth.application.CheckPrivacyPolicyUseCase]) asks for the current one,
 * which it reads from the server. Keep it equal to the backend's `PRIVACY_POLICY_VERSION`.
 */
object PrivacyPolicy {
    const val VERSION = "2026-11-23"

    /** The page, on the same server the app talks to. */
    val url: String get() = getBaseUrl().trimEnd('/') + "/privacidad"
}
