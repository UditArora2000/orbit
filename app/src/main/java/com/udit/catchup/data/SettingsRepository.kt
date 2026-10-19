package com.udit.catchup.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val nudgeEnabled: Boolean = true,
    val nudgeHour: Int = 18,
    val nudgeMinute: Int = 0,
    val cycleNumber: Int = 1,
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val NUDGE_ENABLED = booleanPreferencesKey("nudge_enabled")
        val NUDGE_HOUR = intPreferencesKey("nudge_hour")
        val NUDGE_MINUTE = intPreferencesKey("nudge_minute")
        val CYCLE_NUMBER = intPreferencesKey("cycle_number")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            nudgeEnabled = p[Keys.NUDGE_ENABLED] ?: true,
            nudgeHour = p[Keys.NUDGE_HOUR] ?: 18,
            nudgeMinute = p[Keys.NUDGE_MINUTE] ?: 0,
            cycleNumber = p[Keys.CYCLE_NUMBER] ?: 1,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setNudgeEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NUDGE_ENABLED] = enabled }
    }

    suspend fun setNudgeTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.NUDGE_HOUR] = hour
            it[Keys.NUDGE_MINUTE] = minute
        }
    }

    suspend fun incrementCycle() {
        context.dataStore.edit { it[Keys.CYCLE_NUMBER] = (it[Keys.CYCLE_NUMBER] ?: 1) + 1 }
    }
}
