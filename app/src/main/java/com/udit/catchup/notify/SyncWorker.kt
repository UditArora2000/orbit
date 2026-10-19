package com.udit.catchup.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.udit.catchup.data.ContactsSyncer
import java.util.concurrent.TimeUnit

/**
 * Periodically re-reads the phone's contacts in the background. When new contacts
 * appear (e.g. you saved someone's number), it notifies you to file their details.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val result = ContactsSyncer.sync(applicationContext)
        if (result.newCount > 0) {
            Notifier.showNewContacts(applicationContext, result.newCount)
        }
        return Result.success()
    }

    companion object {
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "contact_sync", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
