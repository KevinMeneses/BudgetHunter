package com.meneses.budgethunter.sms.application

import budgethunter.composeapp.generated.resources.Res
import budgethunter.composeapp.generated.resources.notification_reader_stopped_banner
import budgethunter.composeapp.generated.resources.notification_reader_stopped_title
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.commons.platform.NotificationManager
import com.meneses.budgethunter.commons.resources.StringResourceProvider
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Tells the user, with a notification that opens Settings, that the bank notification reader
 * stopped ([IsNotificationReaderStoppedUseCase]), so they learn it while the app is closed instead
 * of when an expense turns out missing. Run periodically on Android.
 *
 * A reader found stopped is checked again after [confirmationDelay]: when the system kills the app,
 * Android usually binds the listener again within seconds, and only one still unbound by then is
 * the one that stays so. The user is told once per stop: the warning is remembered until the
 * reader is found running again.
 */
class WarnIfNotificationReaderStoppedUseCase(
    private val isNotificationReaderStopped: IsNotificationReaderStoppedUseCase,
    private val preferencesManager: PreferencesManager,
    private val notificationManager: NotificationManager,
    private val stringResourceProvider: StringResourceProvider,
    private val confirmationDelay: Duration = 60.seconds
) {
    suspend operator fun invoke() {
        if (!isStoppedAfterConfirming()) {
            if (preferencesManager.isReaderStoppedNotified()) preferencesManager.setReaderStoppedNotified(false)
            return
        }
        if (preferencesManager.isReaderStoppedNotified()) return

        notificationManager.showOpenSettingsNotification(
            title = stringResourceProvider.getString(Res.string.notification_reader_stopped_title),
            message = stringResourceProvider.getString(Res.string.notification_reader_stopped_banner)
        )
        preferencesManager.setReaderStoppedNotified(true)
    }

    private suspend fun isStoppedAfterConfirming(): Boolean {
        if (!isNotificationReaderStopped()) return false
        delay(confirmationDelay)
        return isNotificationReaderStopped()
    }
}
