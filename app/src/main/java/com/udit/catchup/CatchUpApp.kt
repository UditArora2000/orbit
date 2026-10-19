package com.udit.catchup

import android.app.Application
import com.udit.catchup.data.AppDatabase
import com.udit.catchup.data.SettingsRepository
import com.udit.catchup.notify.Notifier
import com.udit.catchup.notify.NudgeScheduler
import com.udit.catchup.notify.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CatchUpApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var settings: SettingsRepository
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.build(this)
        settings = SettingsRepository(this)
        Notifier.createChannels(this)
        SyncWorker.schedulePeriodic(this)
        appScope.launch { NudgeScheduler.scheduleNext(this@CatchUpApp) }
    }
}
