package com.meneses.budgethunter.sms.application

import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.sms.domain.ReadingChannel
import kotlinx.coroutines.delay

/**
 * Whether the bank notification reader should be running but the system left it unbound.
 *
 * Some devices leave the listener unbound after the app's own notifications are switched off and on,
 * with the switch on and everything granted, and nothing the app can do binds it again: only a force
 * stop from the system settings does. Android binds the listener a moment after the app starts or the
 * access is granted, so it is looked at again after a grace period before saying it stopped.
 */
class IsNotificationReaderStoppedUseCase(
    private val preferencesManager: PreferencesManager,
    private val permissionsManager: PermissionsManager
) {
    suspend operator fun invoke(): Boolean {
        if (!looksStopped()) return false
        delay(CONNECT_GRACE_MS)
        return looksStopped()
    }

    private suspend fun looksStopped(): Boolean =
        // The bank apps and the bank emails are both read by the one listener
        preferencesManager.getChannelsInUse().any { it != ReadingChannel.SMS } &&
            permissionsManager.hasNotificationListenerAccess() &&
            permissionsManager.areAppNotificationsEnabled() &&
            !permissionsManager.isNotificationListenerConnected()

    private companion object {
        const val CONNECT_GRACE_MS = 3_000L
    }
}
