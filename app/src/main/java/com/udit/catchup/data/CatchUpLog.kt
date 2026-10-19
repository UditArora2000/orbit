package com.udit.catchup.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "catchup_log")
data class CatchUpLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val method: String = "CALL",
)

/** A log row joined with the person's name, for the dashboard history list. */
data class LogWithName(
    val id: Long,
    val personId: Long,
    val timestamp: Long,
    val method: String,
    val displayName: String,
)
