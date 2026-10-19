package com.udit.catchup.notify

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.udit.catchup.CatchUpApp
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules a one-shot worker for the next occurrence of the user's chosen nudge time.
 * The worker re-schedules itself after firing, so the nudge repeats daily.
 */
object NudgeScheduler {

    private const val WORK_NAME = "daily_nudge"

    suspend fun scheduleNext(context: Context) {
        val app = context.applicationContext as CatchUpApp
        val settings = app.settings.current()
        val workManager = WorkManager.getInstance(context)
        if (!settings.nudgeEnabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = OneTimeWorkRequestBuilder<NudgeWorker>()
            .setInitialDelay(delayUntil(settings.nudgeHour, settings.nudgeMinute), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun delayUntil(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val next = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis - now.timeInMillis
    }
}
