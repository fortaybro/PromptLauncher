package com.forrest.titanlauncher.contacts

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract


data class EmailContact(
    val name: String,
    val emailAddress: String
)

class ContactRepository(
    private val context: Context
) {

    fun findContact(
        searchName: String
    ): Contact? {

        return findContacts(
            searchName = searchName,
            limit = 1
        ).firstOrNull()
    }

    fun findContacts(
        searchName: String,
        limit: Int = 8
    ): List<Contact> {

        val query =
            normalizeName(
                searchName
            )

        if (
            query.isBlank()
        ) {
            return emptyList()
        }

        val contacts =
            mutableListOf<Contact>()

        val projection =
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

        context.contentResolver
            .query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            ?.use {
                    cursor ->

                val nameIndex =
                    cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY
                    )

                val numberIndex =
                    cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )

                while (
                    cursor.moveToNext()
                ) {

                    if (
                        nameIndex < 0 ||
                        numberIndex < 0
                    ) {
                        continue
                    }

                    val name =
                        cursor.getString(
                            nameIndex
                        )
                            ?.trim()
                            .orEmpty()

                    val phoneNumber =
                        cursor.getString(
                            numberIndex
                        )
                            ?.trim()
                            .orEmpty()

                    if (
                        name.isBlank() ||
                        phoneNumber.isBlank()
                    ) {
                        continue
                    }

                    if (
                        nameMatches(
                            name,
                            query
                        )
                    ) {

                        contacts.add(
                            Contact(
                                name = name,
                                phoneNumber = phoneNumber
                            )
                        )
                    }
                }
            }

        return contacts
            .distinctBy {

                "${normalizeName(it.name)}|${normalizePhoneNumber(it.phoneNumber)}"
            }
            .sortedWith(
                compareBy<Contact> {

                    contactMatchRank(
                        contactName = it.name,
                        query = query
                    )
                }
                    .thenBy {

                        normalizeName(
                            it.name
                        )
                    }
            )
            .take(
                limit.coerceAtLeast(
                    1
                )
            )
    }

    fun findEmailContacts(
        searchName: String,
        limit: Int = 8
    ): List<EmailContact> {

        val query =
            normalizeName(
                searchName
            )

        if (
            query.isBlank()
        ) {
            return emptyList()
        }

        val contacts =
            mutableListOf<EmailContact>()

        val projection =
            arrayOf(
                ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Email.ADDRESS
            )

        context.contentResolver
            .query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            ?.use {
                    cursor ->

                val nameIndex =
                    cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Email.DISPLAY_NAME_PRIMARY
                    )

                val emailIndex =
                    cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Email.ADDRESS
                    )

                while (
                    cursor.moveToNext()
                ) {

                    if (
                        nameIndex < 0 ||
                        emailIndex < 0
                    ) {
                        continue
                    }

                    val name =
                        cursor.getString(
                            nameIndex
                        )
                            ?.trim()
                            .orEmpty()

                    val emailAddress =
                        cursor.getString(
                            emailIndex
                        )
                            ?.trim()
                            .orEmpty()

                    if (
                        name.isBlank() ||
                        emailAddress.isBlank()
                    ) {
                        continue
                    }

                    if (
                        nameMatches(
                            name,
                            query
                        ) ||
                        emailAddress
                            .lowercase()
                            .contains(
                                query
                            )
                    ) {

                        contacts.add(
                            EmailContact(
                                name = name,
                                emailAddress = emailAddress
                            )
                        )
                    }
                }
            }

        return contacts
            .distinctBy {

                "${normalizeName(it.name)}|${it.emailAddress.lowercase()}"
            }
            .sortedWith(
                compareBy<EmailContact> {

                    contactMatchRank(
                        contactName = it.name,
                        query = query
                    )
                }
                    .thenBy {

                        normalizeName(
                            it.name
                        )
                    }
                    .thenBy {

                        it.emailAddress.lowercase()
                    }
            )
            .take(
                limit.coerceAtLeast(
                    1
                )
            )
    }

    /*
     * Returns a content URI for the contact's photo, or null when the
     * number is unsaved or the contact has no picture.
     *
     * Deliberately mirrors findContactNameByPhoneNumber: same table,
     * same phoneNumbersMatch comparison. An earlier version used
     * PhoneLookup, whose own matching index disagreed with ours, so
     * names resolved while photos silently came back null.
     */
    fun findContactPhotoUriByPhoneNumber(
        phoneNumber: String
    ): String? {

        val target =
            normalizePhoneNumber(
                phoneNumber
            )

        if (
            target.isBlank()
        ) {
            return null
        }

        val projection =
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

        context.contentResolver
            .query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            ?.use {
                    cursor ->

                val numberIndex =
                    cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )

                val thumbnailIndex =
                    cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
                    )

                val fullIndex =
                    cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.PHOTO_URI
                    )

                while (
                    cursor.moveToNext()
                ) {

                    if (
                        numberIndex < 0
                    ) {
                        continue
                    }

                    val candidateNumber =
                        normalizePhoneNumber(
                            cursor.getString(
                                numberIndex
                            )
                                .orEmpty()
                        )

                    if (
                        !phoneNumbersMatch(
                            target,
                            candidateNumber
                        )
                    ) {
                        continue
                    }

                    /*
                     * The thumbnail is sized for a list row and much
                     * cheaper to decode than the full photo.
                     */
                    val thumbnail =
                        if (
                            thumbnailIndex >= 0
                        ) {
                            cursor.getString(
                                thumbnailIndex
                            )
                        } else {
                            null
                        }

                    val full =
                        if (
                            fullIndex >= 0
                        ) {
                            cursor.getString(
                                fullIndex
                            )
                        } else {
                            null
                        }

                    val resolved =
                        thumbnail
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?: full
                                ?.takeIf {
                                    it.isNotBlank()
                                }

                    if (
                        resolved != null
                    ) {
                        return resolved
                    }
                }
            }

        return null
    }

    /*
     * Resolves the system contact URI for a number so the launcher can
     * hand the user off to the Contacts app.
     *
     * Uses the same table and matcher as the name and photo lookups,
     * so a number that shows a name here will always resolve to a
     * contact there.
     */
    fun findContactLookupUriByPhoneNumber(
        phoneNumber: String
    ): Uri? {

        val target =
            normalizePhoneNumber(
                phoneNumber
            )

        if (
            target.isBlank()
        ) {
            return null
        }

        val projection =
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
            )

        context.contentResolver
            .query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            ?.use {
                    cursor ->

                val numberIndex =
                    cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )

                val idIndex =
                    cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                    )

                val lookupIndex =
                    cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
                    )

                while (
                    cursor.moveToNext()
                ) {

                    if (
                        numberIndex < 0 ||
                        idIndex < 0 ||
                        lookupIndex < 0
                    ) {
                        continue
                    }

                    val candidateNumber =
                        normalizePhoneNumber(
                            cursor.getString(
                                numberIndex
                            )
                                .orEmpty()
                        )

                    if (
                        !phoneNumbersMatch(
                            target,
                            candidateNumber
                        )
                    ) {
                        continue
                    }

                    val contactId =
                        cursor.getLong(
                            idIndex
                        )

                    val lookupKey =
                        cursor.getString(
                            lookupIndex
                        )
                            ?: continue

                    return ContactsContract.Contacts
                        .getLookupUri(
                            contactId,
                            lookupKey
                        )
                }
            }

        return null
    }

    fun findContactNameByPhoneNumber(
        phoneNumber: String
    ): String? {

        val target =
            normalizePhoneNumber(
                phoneNumber
            )

        if (
            target.isBlank()
        ) {
            return null
        }

        val projection =
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

        context.contentResolver
            .query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            ?.use {
                    cursor ->

                val nameIndex =
                    cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY
                    )

                val numberIndex =
                    cursor.getColumnIndex(
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )

                while (
                    cursor.moveToNext()
                ) {

                    if (
                        nameIndex < 0 ||
                        numberIndex < 0
                    ) {
                        continue
                    }

                    val candidateNumber =
                        normalizePhoneNumber(
                            cursor.getString(
                                numberIndex
                            )
                                .orEmpty()
                        )

                    if (
                        phoneNumbersMatch(
                            target,
                            candidateNumber
                        )
                    ) {

                        return cursor.getString(
                            nameIndex
                        )
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                    }
                }
            }

        return null
    }

    private fun nameMatches(
        name: String,
        query: String
    ): Boolean {

        val normalizedName =
            normalizeName(
                name
            )

        return normalizedName == query ||
                normalizedName.startsWith(
                    "$query "
                ) ||
                normalizedName
                    .split(
                        " "
                    )
                    .any {
                            part ->

                        part.startsWith(
                            query
                        )
                    } ||
                normalizedName.contains(
                    query
                )
    }

    private fun contactMatchRank(
        contactName: String,
        query: String
    ): Int {

        val normalizedName =
            normalizeName(
                contactName
            )

        val words =
            normalizedName.split(
                " "
            )

        return when {

            normalizedName == query ->
                0

            normalizedName.startsWith(
                "$query "
            ) ->
                1

            words.any {
                    word ->

                word == query
            } ->
                2

            words.any {
                    word ->

                word.startsWith(
                    query
                )
            } ->
                3

            normalizedName.contains(
                query
            ) ->
                4

            else ->
                5
        }
    }

    private fun normalizeName(
        value: String
    ): String {

        return value
            .trim()
            .lowercase()
            .replace(
                Regex(
                    "\\s+"
                ),
                " "
            )
    }

    private fun normalizePhoneNumber(
        value: String
    ): String {

        return value.filter {
            it.isDigit()
        }
    }

    private fun phoneNumbersMatch(
        first: String,
        second: String
    ): Boolean {

        if (
            first.isBlank() ||
            second.isBlank()
        ) {
            return false
        }

        if (
            first == second
        ) {
            return true
        }

        val firstLastTen =
            first.takeLast(
                10
            )

        val secondLastTen =
            second.takeLast(
                10
            )

        return firstLastTen.length == 10 &&
                secondLastTen.length == 10 &&
                firstLastTen == secondLastTen
    }
}