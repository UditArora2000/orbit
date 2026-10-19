package com.udit.catchup.data

import android.content.Context
import android.net.Uri
import com.udit.catchup.CatchUpApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Imports the accounts you follow from Instagram's "Download your information" export
 * (the `following.json` file). Instagram shut down API access to personal follow lists,
 * so the data export file is the only reliable way in.
 *
 * Usernames are fuzzy-matched against contact names (letters only, lowercased) so
 * "john_doe99" links to the contact "John Doe". Unmatched accounts become new people
 * in the triage queue.
 */
object InstagramImporter {

    data class ImportResult(val linked: Int, val created: Int, val skipped: Int)

    suspend fun import(context: Context, uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader().readText()
        } ?: return@withContext ImportResult(0, 0, 0)

        val usernames = parseFollowing(text)
        val dao = (context.applicationContext as CatchUpApp).database.personDao()
        val people = dao.allPeople()

        val alreadyLinked = people.mapNotNull { it.instagramUsername?.lowercase() }.toHashSet()
        val byName = HashMap<String, Person>()
        for (p in people) byName.putIfAbsent(normalizeName(p.displayName), p)

        var linked = 0
        var created = 0
        var skipped = 0
        val now = System.currentTimeMillis()
        val toInsert = ArrayList<Person>()

        for (username in usernames) {
            if (username.lowercase() in alreadyLinked) {
                skipped++
                continue
            }
            val match = byName[normalizeName(username)]
            when {
                match != null && match.instagramUsername == null -> {
                    dao.update(match.copy(instagramUsername = username, updatedAt = now))
                    linked++
                }
                match != null -> skipped++
                else -> {
                    toInsert += Person(
                        displayName = username,
                        instagramUsername = username,
                        source = Person.SOURCE_INSTAGRAM,
                    )
                    created++
                }
            }
        }

        if (toInsert.isNotEmpty()) dao.insertAll(toInsert)
        ImportResult(linked, created, skipped)
    }

    /** Handles both the object form ({"relationships_following": [...]}) and a bare array. */
    fun parseFollowing(text: String): List<String> {
        val root = try {
            Json.parseToJsonElement(text)
        } catch (e: Exception) {
            return emptyList()
        }
        val arr: JsonArray = when {
            root is JsonArray -> root
            root is JsonObject ->
                root["relationships_following"] as? JsonArray
                    ?: root.values.firstOrNull { it is JsonArray } as? JsonArray
                    ?: return emptyList()
            else -> return emptyList()
        }
        return arr.mapNotNull { el ->
            val obj = el as? JsonObject ?: return@mapNotNull null
            val fromList = (obj["string_list_data"] as? JsonArray)
                ?.firstOrNull()?.let { it as? JsonObject }
                ?.get("value")?.let { it as? JsonPrimitive }?.contentOrNull
            val title = (obj["title"] as? JsonPrimitive)?.contentOrNull
            (fromList ?: title)?.trim()?.takeIf { it.isNotEmpty() }
        }.distinct()
    }
}
