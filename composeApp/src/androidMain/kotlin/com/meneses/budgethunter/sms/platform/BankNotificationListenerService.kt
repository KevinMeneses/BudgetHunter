package com.meneses.budgethunter.sms.platform

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.sms.domain.BankNotification
import com.meneses.budgethunter.sms.domain.BankNotificationService
import com.meneses.budgethunter.sms.domain.RecentNotifications
import com.meneses.budgethunter.sms.domain.SupportedBanks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

/**
 * Reads the notifications of bank apps that report movements there instead of by SMS (Nequi, Nu).
 *
 * The system binds it once the user grants notification access in its settings; the switch in
 * Settings and the selected banks decide whether what it reads is registered. Every other app's notification is
 * dropped on the spot, before its text is even read.
 */
class BankNotificationListenerService : NotificationListenerService(), KoinComponent {

    private val bankNotificationService: BankNotificationService by inject()
    private val preferencesManager: PreferencesManager by inject()
    private val ioScope: CoroutineScope by inject(named("IOScope"))

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val posted = sbn ?: return
        val bankConfig = SupportedBanks.getNotificationBankByPackage(posted.packageName) ?: return
        val notification = posted.notification ?: return

        // The summary of a group repeats its children, an ongoing one is a progress or a session
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return

        val extras = notification.extras ?: return
        // The expanded text is the whole message; the short one may be cut by the app
        val text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)
        val bankNotification = BankNotification(
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            text = text?.toString().orEmpty()
        )
        if (bankNotification.isBlank) return

        // An update of the same notification keeps its key and its time, even when its text grows
        // (a short text first, the expanded one later), so the text is left out on purpose
        // An app that leaves `when` unset would give every movement under the same id one fingerprint,
        // so then the text tells them apart
        val postedAt = notification.`when`
        val fingerprint = if (postedAt != 0L) "${posted.key}|$postedAt" else "${posted.key}|${bankNotification.message}"
        if (!recentNotifications.markIfNew(fingerprint)) return

        ioScope.launch {
            // The shared IO scope has no exception handler: a failure here (reading the
            // preferences) must not take down the app, which runs this with no screen open
            try {
                if (!preferencesManager.isBankNotificationReadingEnabled()) return@launch
                // Like an SMS bank, an app is read only while the user has it among the selected banks
                if (bankConfig.id !in preferencesManager.getSelectedBankIds()) return@launch
                bankNotificationService.processBankNotification(bankNotification, bankConfig)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Nothing to log: the notification text holds names and amounts
            }
        }
    }

    /**
     * The system drops the binding now and then (the app's notifications switched off and on, the app
     * updated, memory pressure) and does not always bind again by itself, so the access stays granted
     * while nothing is read. Asking for it here brings the listener back.
     */
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        rebindIfGranted(this)
    }

    companion object {
        // Shared by the instances the system creates on each bind; notifications arrive on the main thread
        private val recentNotifications = RecentNotifications()

        /** Asks the system to bind the listener again, when the user granted notification access. */
        fun rebindIfGranted(context: Context) {
            if (context.packageName !in NotificationManagerCompat.getEnabledListenerPackages(context)) return
            // With the app's own notifications off Android drops the listener right after binding it,
            // and onListenerDisconnected would ask again in a loop. The next resume asks once they are on
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
            try {
                NotificationListenerService.requestRebind(ComponentName(context, BankNotificationListenerService::class.java))
            } catch (_: RuntimeException) {
                // The access was taken away in between: Settings shows it missing
            }
        }
    }
}
