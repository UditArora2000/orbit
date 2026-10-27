package com.udit.catchup.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "people", indices = [Index("lookupKey")])
data class Person(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    // Link back to the Android contact, when this person came from the phone book.
    val lookupKey: String? = null,
    val contactId: Long? = null,
    val displayName: String,
    val phoneNumber: String? = null,
    val photoUri: String? = null,
    val instagramUsername: String? = null,
    val source: String = SOURCE_CONTACTS,
    val howWeKnow: String = "",
    val relationship: String = "",
    val notes: String = "",
    val canHangout: Boolean = false,
    val canCall: Boolean = false,
    val isTight: Boolean = false,
    val status: String = STATUS_UNTRIAGED,
    val skipCount: Int = 0,
    val lastContactedAt: Long? = null,
    val contactedThisCycle: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val SOURCE_CONTACTS = "CONTACTS"
        const val SOURCE_INSTAGRAM = "INSTAGRAM"

        const val STATUS_UNTRIAGED = "UNTRIAGED"
        const val STATUS_TRIAGED = "TRIAGED"
        const val STATUS_ARCHIVED = "ARCHIVED"

        val RELATIONSHIPS = listOf(
            "Friend", "Family", "Work", "School", "Neighbor", "Acquaintance", "Other"
        )
    }
}

/** Lowercased letters only, used to fuzzy-match Instagram usernames against contact names. */
fun normalizeName(name: String): String = name.lowercase().filter { it.isLetter() }
