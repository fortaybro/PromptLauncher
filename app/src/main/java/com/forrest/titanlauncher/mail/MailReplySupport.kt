package com.forrest.titanlauncher.mail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


data class MailReplyAllResult(
    val recipients: List<String>,
    val message: String
)


class MailReplySupportRepository {

    suspend fun loadReplyAllRecipients(
        accessToken: String,
        messageId: String,
        senderEmail: String
    ): MailReplyAllResult {

        return withContext(
            Dispatchers.IO
        ) {

            if (
                accessToken.isBlank()
            ) {

                return@withContext MailReplyAllResult(
                    recipients =
                        listOfNotNull(
                            senderEmail
                                .trim()
                                .takeIf {
                                    it.isNotBlank()
                                }
                        ),
                    message =
                        "GOOGLE MAIL AUTH REQUIRED"
                )
            }

            val fallbackSender =
                senderEmail
                    .trim()

            try {

                val profileEmail =
                    loadProfileEmail(
                        accessToken
                    )

                val metadata =
                    loadRecipientMetadata(
                        accessToken =
                            accessToken,
                        messageId =
                            messageId
                    )

                val replyTo =
                    extractEmailAddresses(
                        metadata["Reply-To"]
                            .orEmpty()
                    )
                        .firstOrNull()
                        .orEmpty()

                val deliveredTo =
                    extractEmailAddresses(
                        metadata["Delivered-To"]
                            .orEmpty()
                    )

                val ownAddresses =
                    buildSet {

                        profileEmail
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }
                            ?.let {
                                add(
                                    it.lowercase()
                                )
                            }

                        deliveredTo
                            .forEach {
                                    address ->

                                add(
                                    address.lowercase()
                                )
                            }
                    }

                val candidates =
                    buildList {

                        add(
                            replyTo.ifBlank {
                                fallbackSender
                            }
                        )

                        addAll(
                            extractEmailAddresses(
                                metadata["To"]
                                    .orEmpty()
                            )
                        )

                        addAll(
                            extractEmailAddresses(
                                metadata["Cc"]
                                    .orEmpty()
                            )
                        )
                    }

                val seen =
                    mutableSetOf<String>()

                val recipients =
                    candidates
                        .map {
                            it.trim()
                        }
                        .filter {
                            it.isNotBlank()
                        }
                        .filter {
                                address ->

                            val normalized =
                                address.lowercase()

                            normalized !in ownAddresses &&
                                    seen.add(
                                        normalized
                                    )
                        }

                MailReplyAllResult(
                    recipients =
                        recipients.ifEmpty {

                            listOfNotNull(
                                fallbackSender
                                    .takeIf {
                                        it.isNotBlank()
                                    }
                            )
                        },
                    message =
                        "READY"
                )

            } catch (
                _: Exception
            ) {

                MailReplyAllResult(
                    recipients =
                        listOfNotNull(
                            fallbackSender
                                .takeIf {
                                    it.isNotBlank()
                                }
                        ),
                    message =
                        "REPLY ALL FALLBACK"
                )
            }
        }
    }


    private fun loadProfileEmail(
        accessToken: String
    ): String? {

        val connection =
            URL(
                "https://gmail.googleapis.com/gmail/v1/users/me/profile"
            )
                .openConnection() as HttpURLConnection

        return try {

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                15_000

            connection.readTimeout =
                15_000

            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            if (
                connection.responseCode !in 200..299
            ) {

                null

            } else {

                JSONObject(
                    connection
                        .inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }
                )
                    .optString(
                        "emailAddress"
                    )
                    .trim()
                    .takeIf {
                        it.isNotBlank()
                    }
            }

        } finally {

            connection.disconnect()
        }
    }


    private fun loadRecipientMetadata(
        accessToken: String,
        messageId: String
    ): Map<String, String> {

        if (
            messageId.isBlank()
        ) {

            return emptyMap()
        }

        val url =
            URL(
                "https://gmail.googleapis.com/gmail/v1/users/me/messages/" +
                        "$messageId?format=metadata" +
                        "&metadataHeaders=To" +
                        "&metadataHeaders=Cc" +
                        "&metadataHeaders=Delivered-To" +
                        "&metadataHeaders=Reply-To"
            )

        val connection =
            url.openConnection() as HttpURLConnection

        return try {

            connection.requestMethod =
                "GET"

            connection.connectTimeout =
                15_000

            connection.readTimeout =
                15_000

            connection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            connection.setRequestProperty(
                "Accept",
                "application/json"
            )

            if (
                connection.responseCode !in 200..299
            ) {

                emptyMap()

            } else {

                val root =
                    JSONObject(
                        connection
                            .inputStream
                            .bufferedReader()
                            .use {
                                it.readText()
                            }
                    )

                val headers =
                    root
                        .optJSONObject(
                            "payload"
                        )
                        ?.optJSONArray(
                            "headers"
                        )

                buildMap {

                    if (
                        headers != null
                    ) {

                        for (
                        index in 0 until headers.length()
                        ) {

                            val header =
                                headers.optJSONObject(
                                    index
                                )
                                    ?: continue

                            val name =
                                header
                                    .optString(
                                        "name"
                                    )
                                    .trim()

                            val value =
                                header
                                    .optString(
                                        "value"
                                    )
                                    .trim()

                            if (
                                name.isNotBlank() &&
                                value.isNotBlank()
                            ) {

                                put(
                                    name,
                                    value
                                )
                            }
                        }
                    }
                }
            }

        } finally {

            connection.disconnect()
        }
    }


    private fun extractEmailAddresses(
        raw: String
    ): List<String> {

        return Regex(
            "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",
            RegexOption.IGNORE_CASE
        )
            .findAll(
                raw
            )
            .map {
                it.value.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .toList()
    }
}
