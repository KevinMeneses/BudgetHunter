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
 * the app's own notifications are switched off and on, or after the system kills the app for memory while
 * the listener is bound. Nothing the app can do binds it again: only a force stop from the system
 * settings does.
 *
 * Android binds the listener a moment after the app starts or the access is granted, so a reader found
 * unbound is given a grace period, counted from the first check that found it so, before saying it
 * stopped. Past that period the answer is immediate: a screen opened later (Settings, from the budget
 * list's banner) shows the warning at once instead of after a pause. Any check that finds the reader
 * connected or not needed starts the count again, so a reader that was bound and drops (the access
 * revoked and granted again, the notifications switched off and on) gets its own grace period.
 *
 * Kept as a single so the count spans the screens. It is only called from view models, on the main
 * thread, so the mark needs no lock.
 */
class IsNotificationReaderStoppedUseCase(
    private val preferencesManager: PreferencesManager,
    private val permissionsManager: PermissionsManager,
    private val timeSource: TimeSource = TimeSource.Monotonic
) {
    // Since when the reader has been needed, granted and still unbound
    private var unboundSince: TimeMark? = null

    suspend operator fun invoke(): Boolean {
        if (!looksStopped()) return false
        val remaining = CONNECT_GRACE - (unboundSince?.elapsedNow() ?: return false)
        if (!remaining.isPositive()) return true
        delay(remaining)
        // It may have connected meanwhile, which also clears the mark
        return looksStopped()
    }

    private suspend fun looksStopped(): Boolean {
        // The bank apps and the bank emails are both read by the one listener
        val stopped = preferencesManager.getChannelsInUse().any { it != ReadingChannel.SMS } &&
            permissionsManager.hasNotificationListenerAccess() &&
            permissionsManager.areAppNotificationsEnabled() &&
            !permissionsManager.isNotificationListenerConnected()
        unboundSince = if (stopped) unboundSince ?: timeSource.markNow() else null
        return stopped
    }

    private companion object {
        val CONNECT_GRACE = 3_000.milliseconds
    }
}
