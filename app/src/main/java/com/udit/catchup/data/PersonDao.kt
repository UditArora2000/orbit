package com.udit.catchup.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {

    @Insert
    suspend fun insert(person: Person): Long

    @Insert
    suspend fun insertAll(people: List<Person>): List<Long>

    @Update
    suspend fun update(person: Person)

    @Query("SELECT * FROM people WHERE id = :id")
    fun observePerson(id: Long): Flow<Person?>

    @Query("SELECT * FROM people")
    suspend fun allPeople(): List<Person>

    // --- Triage queue ---

    @Query(
        "SELECT * FROM people WHERE status = 'UNTRIAGED' " +
            "ORDER BY skipCount ASC, displayName COLLATE NOCASE ASC LIMIT 1"
    )
    fun observeNextUntriaged(): Flow<Person?>

    @Query("UPDATE people SET skipCount = skipCount + 1, updatedAt = :ts WHERE id = :id")
    suspend fun skip(id: Long, ts: Long)

    @Query(
        "SELECT * FROM people WHERE status = 'UNTRIAGED' AND skipCount > 0 " +
            "ORDER BY skipCount ASC, displayName COLLATE NOCASE ASC"
    )
    fun observeSkipped(): Flow<List<Person>>

    @Query("SELECT COUNT(*) FROM people WHERE status = 'UNTRIAGED' AND skipCount > 0")
    fun observeSkippedCount(): Flow<Int>

    @Query("SELECT * FROM people WHERE status = 'ARCHIVED' ORDER BY displayName COLLATE NOCASE ASC")
    fun observeArchived(): Flow<List<Person>>

    @Query("SELECT COUNT(*) FROM people WHERE status = 'ARCHIVED'")
    fun observeArchivedCount(): Flow<Int>

    @Query("UPDATE people SET status = 'UNTRIAGED', skipCount = 0, updatedAt = :ts WHERE id = :id")
    suspend fun restore(id: Long, ts: Long)

    // Puts the person ahead of everyone in the queue (ordering is skipCount ASC).
    @Query(
        "UPDATE people SET skipCount = " +
            "(SELECT COALESCE(MIN(skipCount), 0) - 1 FROM people WHERE status = 'UNTRIAGED'), " +
            "updatedAt = :ts WHERE id = :id"
    )
    suspend fun bringToFront(id: Long, ts: Long)

    // --- Counts for the dashboard ---

    @Query("SELECT COUNT(*) FROM people WHERE status != 'ARCHIVED'")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM people WHERE status = 'UNTRIAGED'")
    fun observeUntriagedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM people WHERE status = 'TRIAGED' AND canCall = 1")
    fun observeCallableCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM people WHERE status = 'TRIAGED' AND canCall = 1")
    suspend fun callableCount(): Int

    @Query("SELECT COUNT(*) FROM people WHERE status = 'TRIAGED' AND canHangout = 1")
    fun observeHangoutCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM people WHERE status = 'TRIAGED' AND isTight = 1")
    fun observeTightCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM people WHERE status = 'TRIAGED' AND canCall = 1 " +
            "AND contactedThisCycle = 1"
    )
    fun observeCycleDoneCount(): Flow<Int>

    // --- Call rotation ---

    @Query(
        "SELECT * FROM people WHERE status = 'TRIAGED' AND canCall = 1 AND contactedThisCycle = 0 " +
            "ORDER BY (lastContactedAt IS NULL) DESC, lastContactedAt ASC, displayName COLLATE NOCASE ASC " +
            "LIMIT :limit"
    )
    suspend fun nudgeCandidates(limit: Int): List<Person>

    @Query(
        "SELECT * FROM people WHERE status = 'TRIAGED' AND canCall = 1 AND contactedThisCycle = 0 " +
            "ORDER BY (lastContactedAt IS NULL) DESC, lastContactedAt ASC, displayName COLLATE NOCASE ASC " +
            "LIMIT 1"
    )
    fun observeNextUp(): Flow<Person?>

    @Query(
        "UPDATE people SET lastContactedAt = :ts, contactedThisCycle = 1, updatedAt = :ts " +
            "WHERE id = :id"
    )
    suspend fun markContacted(id: Long, ts: Long)

    @Query("UPDATE people SET contactedThisCycle = 0")
    suspend fun resetCycle()

    // --- Friends list ---

    @Query(
        "SELECT * FROM people WHERE status = 'TRIAGED' " +
            "AND (:filter = 'ALL' OR (:filter = 'CALL' AND canCall = 1) OR (:filter = 'HANGOUT' AND canHangout = 1) " +
            "OR (:filter = 'TIGHT' AND isTight = 1)) " +
            "AND (:search = '' OR displayName LIKE '%' || :search || '%' " +
            "OR howWeKnow LIKE '%' || :search || '%' " +
            "OR instagramUsername LIKE '%' || :search || '%') " +
            "ORDER BY displayName COLLATE NOCASE ASC"
    )
    fun observeFriends(filter: String, search: String): Flow<List<Person>>

    // --- Catch-up history ---

    @Insert
    suspend fun insertLog(log: CatchUpLog)

    @Query(
        "SELECT l.id, l.personId, l.timestamp, l.method, p.displayName " +
            "FROM catchup_log l JOIN people p ON p.id = l.personId " +
            "ORDER BY l.timestamp DESC LIMIT :limit"
    )
    fun observeRecentLogs(limit: Int): Flow<List<LogWithName>>

    @Query("SELECT COUNT(*) FROM catchup_log")
    fun observeTotalCatchUps(): Flow<Int>
}
