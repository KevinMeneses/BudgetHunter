package com.meneses.budgethunter.sms.platform

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.meneses.budgethunter.commons.data.PreferencesManager
import com.meneses.budgethunter.sms.domain.BankEmail
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
 * Reads the notifications of bank apps that report movements there instead of by SMS (Nequi, Nu),
 * and those of email apps announcing an email from a selected bank.
 *
 * The system binds it once the user grants notification access in its settings; the switches in
 * Settings and the selected banks decide whether what it reads is registered. Every other app's notification is
 * dropped on the spot, before its text is even read; of an email, the sender is read first and
 * the rest only when a selected bank wrote it.
 */
class BankNotificationListenerService : NotificationListenerService(), KoinComponent {

    private val bankNotificationService: BankNotificationService by inject()
    private val preferencesManager: PreferencesManager by inject()
    private val ioScope: CoroutineScope by inject(named("IOScope"))

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val posted = sbn ?: return
        if (BankEmail.isEmailApp(posted.packageName)) {
            onEmailNotificationPosted(posted)
            return
        }
        val bankConfig = SupportedBanks.getNotificationBankByPackage(posted.packageName) ?: return
        val notification = posted.notification ?: return
        if (!notification.isSingleMessage()) return

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
     * An email app announces an email: when a selected bank sent it, the subject and the start
     * of the body are read like a bank app notification. Any other email is dropped as soon as
     * its sender is known.
     */
    private fun onEmailNotificationPosted(posted: StatusBarNotification) {
        val notification = posted.notification ?: return
        // A group summary ("3 new emails") lists several emails at once; each one has its own
        if (!notification.isSingleMessage()) return

        val extras = notification.extras ?: return
        val sender = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        if (sender.isBlank()) return

        ioScope.launch {
            try {
                if (!preferencesManager.isBankEmailReadingEnabled()) return@launch
                val selectedBanks = preferencesManager.getSelectedBankIds()
                    .mapNotNull { SupportedBanks.getSelectableBankById(it) }
                val bankConfig = BankEmail.findSender(sender, selectedBanks) ?: return@launch

                val email = BankEmail.fromNotification(
                    title = sender,
                    text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
                    bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                )
                if (email.isBlank) return@launch

                // Same identity as a bank app notification: an update keeps its key and its time
                val postedAt = notification.`when`
                val fingerprint = if (postedAt != 0L) "${posted.key}|$postedAt" else "${posted.key}|${email.body}"
                // Marked off the main thread, unlike the bank apps' ones, so these have their own lock
                if (!synchronized(recentEmails) { recentEmails.markIfNew(fingerprint) }) return@launch

                bankNotificationService.processBankEmail(email, bankConfig)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Nothing to log: the email holds names and amounts
            }
        }
    }

    /** The summary of a group repeats its children, an ongoing one is a progress or a session. */
    private fun Notification.isSingleMessage(): Boolean =
        flags and Notification.FLAG_GROUP_SUMMARY == 0 && flags and Notification.FLAG_ONGOING_EVENT == 0

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
    }

    companion object {
        // Shared by the instances the system creates on each bind; notifications arrive on the main thread
        private val recentNotifications = RecentNotifications()

        // Only the emails of selected banks get here, marked from the IO scope
        private val recentEmails = RecentNotifications()

        /**
         * Whether the system has the listener bound in this process. After the app's own notifications
         * are switched off and on, some devices leave it unbound for good: the access stays granted
         * while nothing is read, and neither requestRebind nor anything else the app can do brings it
         * back. Only a force stop from the system settings does, so Settings asks the user for it.
         */
        @Volatile
        var isConnected = false
            private set
    }
}
