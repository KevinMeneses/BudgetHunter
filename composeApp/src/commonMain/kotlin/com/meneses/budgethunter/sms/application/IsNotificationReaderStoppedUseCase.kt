package com.meneses.budgethunter.sms.application

import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.PermissionsManager
import com.meneses.budgethunter.sms.domain.ReadingChannel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Whether the bank notification reader should be running but the system left it unbound.
 *
 * Some devices (seen on Samsung) leave the listener unbound for good, with everything granted: after
 * the app's own notifications are switched off and on, or after the system kills the app for memory
 * while the listener is bound. Nothing the app can do binds it again: only a force stop from the system
 * settings does.
 *
 * Android binds the listener a moment after the app starts or the access is granted, so during a grace
 * period from then an unbound reader is looked at again before saying it stopped. Past that period the
 * answer is immediate: a screen opened later (Settings, from the budget list's banner) shows the warning
 * at once instead of after a pause. Kept as a single so the period is counted once per process.
 */
class IsNotificationReaderStoppedUseCase(
    private val preferencesManager: PreferencesManager,
    private val permissionsManager: PermissionsManager,
    private val timeSource: TimeSource = TimeSource.Monotonic
) {
    // Since when the reader could have been bound: the first check that found everything granted
    private var grantedSince: TimeMark? = null

    suspend operator fun invoke(): Boolean {
        if (!looksStopped()) return false
        val remaining = CONNECT_GRACE - (grantedSince?.elapsedNow() ?: return false)
        if (!remaining.isPositive()) return true
        delay(remaining)
        return looksStopped()
    }

    private suspend fun looksStopped(): Boolean {
        // The bank apps and the bank emails are both read by the one listener
        if (preferencesManager.getChannelsInUse().none { it != ReadingChannel.SMS }) return false
        val granted = permissionsManager.hasNotificationListenerAccess() &&
            permissionsManager.areAppNotificationsEnabled()
        if (!granted) {
            // Granting it again gives the system a new moment to bind the reader
            grantedSince = null
            return false
        }
        if (grantedSince == null) grantedSince = timeSource.markNow()
        return !permissionsManager.isNotificationListenerConnected()
    }

    private companion object {
        val CONNECT_GRACE = 3_000.milliseconds
    }
}
