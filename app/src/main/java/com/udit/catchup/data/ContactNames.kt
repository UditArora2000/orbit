package com.udit.catchup.data

/** A phone-number fallback is not a saved contact name. Supports non-Latin names too. */
internal fun String.isContactName(): Boolean = any { it.isLetter() }

internal fun resolveContactName(
    displayName: String?,
    structuredName: String?,
    matchingPhoneName: String?,
    phoneNumber: String?,
): String? {
    val names = listOf(displayName, structuredName, matchingPhoneName)
        .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
    return names.firstOrNull { it.isContactName() }
        ?: names.firstOrNull()
        ?: phoneNumber?.trim()?.takeIf(String::isNotEmpty)
}

/** Only use an exact number match, and never guess when multiple saved names share it. */
internal fun uniquePhoneNames(entries: List<Pair<String, String>>): Map<String, String> =
    entries.groupBy({ it.first.filter(Char::isDigit) }, { it.second.trim() })
        .filterKeys { it.isNotEmpty() }
        .mapNotNull { (number, names) ->
            names.filter { it.isContactName() }.distinct().singleOrNull()?.let { number to it }
        }.toMap()
