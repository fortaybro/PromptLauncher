package com.forrest.titanlauncher.mail

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


class MailActionRepository {

    suspend fun archiveMessage(
        accessToken: String,
        messageId: String
    ): Boolean {

        return modifyLabels(
            accessToken =
                accessToken,
            messageId =
                messageId,
            addLabelIds =
                emptyList(),
            removeLabelIds =
                listOf(
                    "INBOX"
                )
        )
    }


    suspend fun restoreToInbox(
        accessToken: String,
        messageId: String
    ): Boolean {

        return modifyLabels(
            accessToken =
                accessToken,
            messageId =
                messageId,
            addLabelIds =
                listOf(
                    "INBOX"
                ),
            removeLabelIds =
                emptyList()
        )
    }


    private suspend fun modifyLabels(
        accessToken: String,
        messageId: String,
        addLabelIds: List<String>,
        removeLabelIds: List<String>
    ): Boolean {

        return withContext(
            Dispatchers.IO
        ) {

            if (
                accessToken.isBlank() ||
                messageId.isBlank()
            ) {

                return@withContext false
            }

            val connection =
                URL(
                    "https://gmail.googleapis.com/gmail/v1/users/me/messages/$messageId/modify"
                )
                    .openConnection() as HttpURLConnection

            try {

                connection.requestMethod =
                    "POST"

                connection.connectTimeout =
                    15_000

                connection.readTimeout =
                    15_000

                connection.doOutput =
                    true

                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $accessToken"
                )

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                )

                val body =
                    JSONObject()
                        .apply {

                            put(
                                "addLabelIds",
                                addLabelIds
                            )

                            put(
                                "removeLabelIds",
                                removeLabelIds
                            )
                        }
                        .toString()

                connection
                    .outputStream
                    .bufferedWriter(
                        Charsets.UTF_8
                    )
                    .use {
                            writer ->

                        writer.write(
                            body
                        )
                    }

                connection.responseCode in 200..299

            } catch (
                _: Exception
            ) {

                false

            } finally {

                connection.disconnect()
            }
        }
    }
}


data class SnoozedMailItem(
    val messageId: String,
    val wakeAtMillis: Long
)


class MailSnoozeStore(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )


    fun snoozeFor24Hours(
        messageId: String
    ) {

        val wakeAtMillis =
            System.currentTimeMillis() +
                    TWENTY_FOUR_HOURS_MILLIS

        val updated =
            readItems()
                .filterNot {
                    it.messageId ==
                            messageId
                }
                .plus(
                    SnoozedMailItem(
                        messageId =
                            messageId,
                        wakeAtMillis =
                            wakeAtMillis
                    )
                )

        writeItems(
            updated
        )
    }


    fun dueItems(
        nowMillis: Long =
            System.currentTimeMillis()
    ): List<SnoozedMailItem> {

        return readItems()
            .filter {
                it.wakeAtMillis <=
                        nowMillis
            }
    }


    fun remove(
        messageId: String
    ) {

        writeItems(
            readItems()
                .filterNot {
                    it.messageId ==
                            messageId
                }
        )
    }


    private fun readItems(): List<SnoozedMailItem> {

        return preferences
            .getStringSet(
                KEY_ITEMS,
                emptySet()
            )
            .orEmpty()
            .mapNotNull {
                    encoded ->

                val separatorIndex =
                    encoded.lastIndexOf(
                        '|'
                    )

                if (
                    separatorIndex <= 0 ||
                    separatorIndex >=
                    encoded.lastIndex
                ) {

                    null

                } else {

                    val messageId =
                        encoded
                            .substring(
                                0,
                                separatorIndex
                            )
                            .trim()

                    val wakeAt =
                        encoded
                            .substring(
                                separatorIndex +
                                        1
                            )
                            .toLongOrNull()

                    if (
                        messageId.isBlank() ||
                        wakeAt == null
                    ) {

                        null

                    } else {

                        SnoozedMailItem(
                            messageId =
                                messageId,
                            wakeAtMillis =
                                wakeAt
                        )
                    }
                }
            }
    }


    private fun writeItems(
        items: List<SnoozedMailItem>
    ) {

        val encoded =
            items
                .map {
                        item ->

                    "${item.messageId}|${item.wakeAtMillis}"
                }
                .toSet()

        preferences
            .edit()
            .putStringSet(
                KEY_ITEMS,
                encoded
            )
            .apply()
    }


    companion object {

        private const val PREFS_NAME =
            "prompt_launcher_mail_snooze"

        private const val KEY_ITEMS =
            "snoozed_mail"

        private const val TWENTY_FOUR_HOURS_MILLIS =
            24L *
                    60L *
                    60L *
                    1000L
    }
}
