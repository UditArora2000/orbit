package com.udit.catchup.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ContactNamesTest {
    @Test fun savedNameReplacesNumericDisplayName() {
        assertEquals("Asha", resolveContactName("+1 (415) 555-0123", "Asha", null, "+14155550123"))
    }

    @Test fun preservesPreferredNameAndNonLatinNames() {
        assertEquals("Mom", resolveContactName(" Mom ", "Asha Patel", null, "5550123"))
        assertEquals("李明", resolveContactName("5550123", "李明", null, "5550123"))
    }

    @Test fun namedDuplicateCanSupplyMissingName() {
        val names = uniquePhoneNames(listOf("+1 (415) 555-0123" to "Asha", "+14155550123" to "+14155550123"))
        assertEquals("Asha", resolveContactName("+14155550123", null, names["14155550123"], "+14155550123"))
    }

    @Test fun doesNotGuessBetweenConflictingNamesOrCountryCodes() {
        val names = uniquePhoneNames(listOf("555-0123" to "Asha", "5550123" to "Sam", "+14155550123" to "Mom"))
        assertFalse(names.containsKey("5550123"))
        assertNull(names["4414155550123"])
        assertEquals("Mom", names["14155550123"])
    }

    @Test fun unnamedContactStillHasNumberAndEmptyContactIsSkipped() {
        assertEquals("5550123", resolveContactName(" ", null, null, "5550123"))
        assertNull(resolveContactName(null, " ", null, null))
    }

    @Test fun repeatedSameNameIsNotAmbiguousAndEmptyNumbersDoNotMatch() {
        val names = uniquePhoneNames(listOf("555-0123" to "Asha", "5550123" to "Asha", "" to "Sam"))
        assertEquals(mapOf("5550123" to "Asha"), names)
    }
}
