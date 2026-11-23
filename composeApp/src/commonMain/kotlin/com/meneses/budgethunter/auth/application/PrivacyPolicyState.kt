package com.meneses.budgethunter.auth.application

data class PrivacyPolicyState(
    /** The server's current version while the dialog asks for it; null = no dialog. */
    val pendingVersion: String? = null,
    /** Accepting or signing out: the buttons wait. */
    val isWorking: Boolean = false,
    /** Signing out would delete changes the server does not have: the dialog says so first. */
    val isSignOutConfirmationVisible: Boolean = false
) {
    val isDialogVisible get() = pendingVersion != null
}

sealed interface PrivacyPolicyIntent {
    /** After a sign in, and when the app opens with a session. */
    data object Check : PrivacyPolicyIntent
    data object Accept : PrivacyPolicyIntent
    data object SignOut : PrivacyPolicyIntent
    data object ConfirmSignOut : PrivacyPolicyIntent
    data object CancelSignOut : PrivacyPolicyIntent
}
