package com.udit.catchup.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.udit.catchup.CatchUpApp

/**
 * Fires once a day at the user's chosen time. Picks someone from the call rotation —
 * friends marked "can call" who haven't been contacted this round, favoring whoever
 * it's been longest since you caught up with — and nudges you to reach out.
 */
class NudgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CatchUpApp
        val dao = app.database.personDao()

        // Random pick among the 5 most-overdue, so the nudge doesn't get repetitive.
        val pick = dao.nudgeCandidates(5).randomOrNull()
        if (pick != null) {
            Notifier.showNudge(applicationContext, pick)
        } else if (dao.callableCount() > 0) {
            Notifier.showCycleComplete(applicationContext)
        }

        NudgeScheduler.scheduleNext(applicationContext)
        return Result.success()
    }
}
