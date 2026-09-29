package com.forrest.titanlauncher.messages

import android.content.Context
import android.provider.Telephony
import com.forrest.titanlauncher.normalizePhoneNumber


/*
 * SMS HISTORY IMPORT
 *
 * Android keeps every SMS in a shared system database that any app
 * holding READ_SMS can read. Google Messages has been writing to it
 * for as long as it was the default app, so that store holds the
 * user's whole history.
 *
 * Prompt Launcher keeps its own Room database and does not write to
 * the system store, so on first becoming the default SMS app its
 * threads start empty. This copies the history across once.
 *
 * Deliberately one-way and one-time. It does not keep the two stores
 * in sync — messages received while Prompt Launcher is default exist
 * only in Room, which is a separate piece of work.
 */


private const val ImportPrefsName =
    "prompt_launcher_sms_import"

private const val ImportedKey =
    "history_imported"

/*
 * Enough to cover years of normal use without turning first launch
 * into a long wait.
 */
private const val ImportLimit =
    5000


object SmsHistoryImporter {

    fun hasImported(
        context: Context
    ): Boolean {

        return context
            .getSharedPreferences(
                ImportPrefsName,
                Context.MODE_PRIVATE
            )
            .getBoolean(
                ImportedKey,
                false
            )
    }

    private fun markImported(
        context: Context
    ) {

        context
            .getSharedPreferences(
                ImportPrefsName,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                ImportedKey,
                true
            )
            .apply()
    }

    /*
     * Returns how many messages were copied in, or null when there
     * was nothing to do. Safe to call repeatedly: the marker stops a
     * second run, and the identity check below stops duplicates even
     * if the marker is cleared.
     */
    suspend fun importIfNeeded(
        context: Context,
        dao: SmsDao,
        force: Boolean = false
    ): Int? {

        if (
            !force &&
            hasImported(
                context
            )
        ) {
            return null
        }

        val existing =
            runCatching {
                dao.getAllOnce()
            }
                .getOrDefault(
                    emptyList()
                )

        /*
         * A message is the same message if it came from the same
         * number at the same moment with the same text. Room's own
         * ids are no help here, since the system store has its own.
         */
        val seen =
            existing
                .map {
                    identityOf(
                        it.phoneNumber,
                        it.timestamp,
                        it.body
                    )
                }
                .toMutableSet()

        val projection =
            arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.READ
            )

        var imported = 0

        val result =
            runCatching {

                context.contentResolver
                    .query(
                        Telephony.Sms.CONTENT_URI,
                        projection,
                        null,
                        null,
                        Telephony.Sms.DATE + " DESC"
                    )
                    ?.use { cursor ->

                        val addressIndex =
                            cursor.getColumnIndex(
                                Telephony.Sms.ADDRESS
                            )

                        val bodyIndex =
                            cursor.getColumnIndex(
                                Telephony.Sms.BODY
                            )

                        val dateIndex =
                            cursor.getColumnIndex(
                                Telephony.Sms.DATE
                            )

                        val typeIndex =
                            cursor.getColumnIndex(
                                Telephony.Sms.TYPE
                            )

                        val readIndex =
                            cursor.getColumnIndex(
                                Telephony.Sms.READ
                            )

                        while (
                            cursor.moveToNext() &&
                            imported < ImportLimit
                        ) {

                            if (
                                addressIndex < 0 ||
                                bodyIndex < 0 ||
                                dateIndex < 0
                            ) {
                                continue
                            }

                            val address =
                                cursor
                                    .getString(
                                        addressIndex
                                    )
                                    .orEmpty()
                                    .trim()

                            val body =
                                cursor
                                    .getString(
                                        bodyIndex
                                    )
                                    .orEmpty()

                            if (
                                address.isBlank() ||
                                body.isBlank()
                            ) {
                                continue
                            }

                            val timestamp =
                                cursor.getLong(
                                    dateIndex
                                )

                            val identity =
                                identityOf(
                                    address,
                                    timestamp,
                                    body
                                )

                            if (
                                !seen.add(
                                    identity
                                )
                            ) {
                                continue
                            }

                            val type =
                                if (
                                    typeIndex >= 0
                                ) {
                                    cursor.getInt(
                                        typeIndex
                                    )
                                } else {
                                    Telephony.Sms.MESSAGE_TYPE_INBOX
                                }

                            /*
                             * Drafts, queued and failed messages are
                             * not part of the conversation as far as
                             * a reader is concerned.
                             */
                            val incoming =
                                when (
                                    type
                                ) {
                                    Telephony.Sms.MESSAGE_TYPE_INBOX ->
                                        true

                                    Telephony.Sms.MESSAGE_TYPE_SENT ->
                                        false

                                    else ->
                                        continue
                                }

                            val isRead =
                                if (
                                    readIndex >= 0
                                ) {
                                    cursor.getInt(
                                        readIndex
                                    ) != 0
                                } else {
                                    true
                                }

                            dao.insert(
                                SmsMessage(
                                    phoneNumber =
                                        address,
                                    body =
                                        body,
                                    timestamp =
                                        timestamp,
                                    incoming =
                                        incoming,
                                    isRead =
                                        !incoming ||
                                                isRead,
                                    threadKey =
                                        threadKeyFor(
                                            listOf(
                                                address
                                            )
                                        ),
                                    senderNumber =
                                        if (
                                            incoming
                                        ) {
                                            address
                                        } else {
                                            null
                                        }
                                )
                            )

                            imported += 1
                        }
                    }

                imported
            }
                .onFailure { error ->

                }
                .getOrNull()

        if (
            result == null
        ) {
            return null
        }

        markImported(
            context
        )


        return imported
    }

    private fun identityOf(
        address: String,
        timestamp: Long,
        body: String
    ): String {

        return normalizePhoneNumber(
            address
        ) +
                "|" +
                timestamp +
                "|" +
                body.hashCode()
    }
}