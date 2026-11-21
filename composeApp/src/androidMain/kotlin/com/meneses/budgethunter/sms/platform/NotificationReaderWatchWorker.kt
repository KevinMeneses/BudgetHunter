package com.meneses.budgethunter.sms.platform

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.meneses.budgethunter.commons.data.sync.Logger
import com.meneses.budgethunter.commons.platform.AndroidNotificationManager
import com.meneses.budgethunter.sms.application.WarnIfNotificationReaderStoppedUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

/**
 * Checks every so often, with the app closed too, that the bank notification reader is still
 * bound, and warns the user when it is not ([WarnIfNotificationReaderStoppedUseCase]). Some
 * devices (seen on a Galaxy A55) leave it unbound after killing the app for memory, and then
 * every movement is missed until the user opens the app and sees the banner.
 */
class NotificationReaderWatchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val warnIfNotificationReaderStopped: WarnIfNotificationReaderStoppedUseCase by inject()
    private val logger: Logger by inject()

    override suspend fun doWork(): Result {
        // A warning nobody would see must not count as given: once it can be shown, a later run gives it
        val canNotify = AndroidNotificationManager.canShowNotifications(applicationContext)
        try {
            // The stop detection is a single meant for the main thread, like the screens that use it.
            // Its 60 s wait suspends, it does not block; a run stopped meanwhile is retried next period
            withContext(Dispatchers.Main) { warnIfNotificationReaderStopped(canNotify) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The next run checks again
            logger.warn(TAG, "Could not check the notification reader", e)
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "NotificationReaderWatch"
        private const val WORK_NAME = "notification_reader_watch"

        // The shortest period WorkManager allows; Android may stretch it while the device dozes
        private const val PERIOD_MINUTES = 15L

        /**
         * Schedules the check once; later calls keep the schedule already there. Called when the
         * app starts, which must not fail for it: without WorkManager (it is started by its own
         * initializer, which tests running the app do not run) there is just no check.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationReaderWatchWorker>(PERIOD_MINUTES, TimeUnit.MINUTES)
                .build()
            try {
                // KEEP: a change to the period or constraints needs UPDATE (or a new name) to reach
                // installs that already have it scheduled
                WorkManager.getInstance(context)
                    .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "WorkManager not initialized: the notification reader is not watched", e)
            }
        }
    }
}
