package com.meneses.budgethunter.sms.application

import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import kotlinx.coroutines.flow.filter

/**
 * Asks for the SMS and notification permissions when SMS reading is on but the device does not
 * grant them.
 *
 * Settings only asks when the switch is turned on. After a reinstall (or on a new phone) the
 * switch comes back on from the account or from the Android backup, but the permissions do not:
 * without this, the receiver never runs and the expenses are silently not recorded.
 *
 * Collects the preference instead of reading it once because the account's value is pulled in
 * the background after sign in, usually after the budget list is already on screen.
 *
 * Asks at most once per process, and never once the user has denied the SMS permission (the
 * system then wants a rationale, which is what the Settings dialog is for), so it does not nag.
 */
class RequestSmsPermissionsIfNeededUseCase(
    private val preferencesManager: PreferencesManager,
    private val permissionsManager: PermissionsManager
) {
    private var hasRequested = false

    /** Suspends while the caller is alive: call it from the main screen's scope. */
    suspend fun observe() {
        preferencesManager.smsReadingEnabled
            .filter { it }
            .collect { requestIfMissing() }
    }

    private fun requestIfMissing() {
        if (hasRequested) return
        val isMissingPermission = !permissionsManager.hasSmsPermission() ||
            !permissionsManager.hasNotificationPermission()
        if (!isMissingPermission) return
        if (permissionsManager.shouldShowSMSPermissionRationale()) return

        hasRequested = true
        permissionsManager.requestSmsPermissions { }
    }
}
