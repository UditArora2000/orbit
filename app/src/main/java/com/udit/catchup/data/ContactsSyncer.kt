package com.udit.catchup.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.udit.catchup.CatchUpApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Pulls the phone's contacts into the local database. New contacts land in the triage queue;
 * existing ones get their name/number/photo refreshed. If an Instagram-only person matches a
 * new contact by name, the two records are merged instead of creating a duplicate.
 */
object ContactsSyncer {
    private val syncMutex = Mutex()

    data class SyncResult(val newCount: Int, val updatedCount: Int)

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun sync(context: Context): SyncResult = withContext(Dispatchers.IO) {
        if (!hasPermission(context)) return@withContext SyncResult(0, 0)
        syncMutex.withLock {
            val dao = (context.applicationContext as CatchUpApp).database.personDao()
            val resolver = context.contentResolver

            data class ContactRow(val contactId: Long, val name: String, val photoUri: String?)

            val contacts = LinkedHashMap<String, ContactRow>()
            resolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.LOOKUP_KEY,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.PHOTO_URI,
                ),
                null, null,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            )?.use { c ->
                while (c.moveToNext()) {
                    val key = c.getString(1) ?: continue
                    val name = c.getString(2)?.trim().orEmpty()
                    contacts[key] = ContactRow(c.getLong(0), name, c.getString(3))
                }
            }

            // Read saved names from the underlying records as well: an aggregate's
            // display name can be a phone number supplied by another account.
            val savedNames = HashMap<Long, String>()
            resolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Data.CONTACT_ID,
                    ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                ),
                "${ContactsContract.Data.MIMETYPE} = ?",
                arrayOf(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE),
                "${ContactsContract.Data.IS_SUPER_PRIMARY} DESC, ${ContactsContract.Data.IS_PRIMARY} DESC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val name = c.getString(1)?.trim().orEmpty()
                    if (name.isContactName()) savedNames.putIfAbsent(c.getLong(0), name)
                }
            }

            // Join by aggregate ID, and prefer the user's default phone number.
            val phones = HashMap<Long, String>()
            val allPhones = ArrayList<Pair<Long, String>>()
            val phoneNames = ArrayList<Pair<String, String>>()
            val contactsById = contacts.values.associateBy { it.contactId }
            resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ),
                null, null,
                "${ContactsContract.CommonDataKinds.Phone.IS_SUPER_PRIMARY} DESC, " +
                    "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val contactId = c.getLong(0)
                    val number = c.getString(1) ?: continue
                    if (number.isBlank()) continue
                    phones.putIfAbsent(contactId, number)
                    allPhones += contactId to number
                    val name = resolveContactName(
                        contactsById[contactId]?.name, savedNames[contactId], c.getString(2), number,
                    )
                    if (name != null) phoneNames += number to name
                }
            }
            val namesByNumber = uniquePhoneNames(phoneNames)
            val matchingNames = allPhones.groupBy({ it.first }, { it.second })
                .mapValues { (_, numbers) ->
                    numbers.mapNotNull { namesByNumber[it.filter(Char::isDigit)] }.distinct().singleOrNull()
                }

            val existingByKey = HashMap<String, Person>()
            val instagramByName = HashMap<String, Person>()
            for (p in dao.allPeople()) {
                if (p.lookupKey != null) existingByKey[p.lookupKey] = p
                else instagramByName.putIfAbsent(normalizeName(p.displayName), p)
            }

            var newCount = 0
            var updatedCount = 0
            val now = System.currentTimeMillis()
            val toInsert = ArrayList<Person>()

            for ((key, row) in contacts) {
                val phone = phones[row.contactId]
                val name = resolveContactName(row.name, savedNames[row.contactId], matchingNames[row.contactId], phone)
                    ?: continue
                val existing = existingByKey[key]
                if (existing != null) {
                    val refreshed = existing.copy(
                        contactId = row.contactId,
                        displayName = name,
                        phoneNumber = phone ?: existing.phoneNumber,
                        photoUri = row.photoUri ?: existing.photoUri,
                    )
                    if (refreshed != existing) {
                        dao.update(refreshed.copy(updatedAt = now))
                        updatedCount++
                    }
                    continue
                }
                val instagramMatch = normalizeName(name).takeIf { it.isNotEmpty() }?.let(instagramByName::remove)
                if (instagramMatch != null) {
                    dao.update(
                        instagramMatch.copy(
                            lookupKey = key,
                            contactId = row.contactId,
                            displayName = name,
                            phoneNumber = phone,
                            photoUri = row.photoUri,
                            source = Person.SOURCE_CONTACTS,
                            updatedAt = now,
                        )
                    )
                    updatedCount++
                } else {
                    toInsert += Person(
                        lookupKey = key,
                        contactId = row.contactId,
                        displayName = name,
                        phoneNumber = phone,
                        photoUri = row.photoUri,
                    )
                    newCount++
                }
            }

            if (toInsert.isNotEmpty()) dao.insertAll(toInsert)
            SyncResult(newCount, updatedCount)
        }
    }
}
