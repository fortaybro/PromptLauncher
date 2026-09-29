package com.forrest.titanlauncher.mail

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


data class MailMessage(
    val id: String,
    val threadId: String,
    val sender: String,
    val senderEmail: String,
    val messageIdHeader: String,
    val subject: String,
    val snippet: String,
    val body: String,
    val timestamp: Long,
    val unread: Boolean,
    val important: Boolean
)


data class MailLoadResult(
    val success: Boolean,
    val messages: List<MailMessage>,
    val message: String
)


data class MailSendResult(
    val success: Boolean,
    val message: String
)

class GmailRepository {

    suspend fun loadInbox(
        accessToken: String,
        maxResults: Int = 20
    ): MailLoadResult {

        return withContext(
            Dispatchers.IO
        ) {

            if (
                accessToken.isBlank()
            ) {

                return@withContext MailLoadResult(
                    success = false,
                    messages = emptyList(),
                    message = "GOOGLE MAIL AUTH REQUIRED"
                )
            }

            try {

                val listUrl =
                    URL(
                        "https://gmail.googleapis.com/gmail/v1/users/me/messages?labelIds=INBOX&maxResults=$maxResults"
                    )

                val listConnection =
                    listUrl.openConnection() as HttpURLConnection

                val messageIds =
                    try {

                        listConnection.requestMethod =
                            "GET"

                        listConnection.connectTimeout =
                            15_000

                        listConnection.readTimeout =
                            15_000

                        listConnection.setRequestProperty(
                            "Authorization",
                            "Bearer $accessToken"
                        )

                        listConnection.setRequestProperty(
                            "Accept",
                            "application/json"
                        )

                        val responseCode =
                            listConnection.responseCode

                        if (
                            responseCode !in 200..299
                        ) {

                            return@withContext MailLoadResult(
                                success = false,
                                messages = emptyList(),
                                message =
                                    when (
                                        responseCode
                                    ) {

                                        401 ->
                                            "GOOGLE MAIL AUTH EXPIRED"

                                        403 ->
                                            "GMAIL ACCESS DENIED"

                                        else ->
                                            "GMAIL ERROR $responseCode"
                                    }
                            )
                        }

                        val root =
                            JSONObject(
                                listConnection
                                    .inputStream
                                    .bufferedReader()
                                    .use {
                                        it.readText()
                                    }
                            )

                        val messages =
                            root.optJSONArray(
                                "messages"
                            )
                                ?: JSONArray()

                        buildList {

                            for (
                            index in 0 until messages.length()
                            ) {

                                val id =
                                    messages
                                        .optJSONObject(
                                            index
                                        )
                                        ?.optString(
                                            "id"
                                        )
                                        .orEmpty()

                                if (
                                    id.isNotBlank()
                                ) {

                                    add(
                                        id
                                    )
                                }
                            }
                        }

                    } finally {

                        listConnection.disconnect()
                    }

                val loaded =
                    messageIds
                        .mapNotNull {
                                id ->

                            loadMessage(
                                accessToken = accessToken,
                                messageId = id
                            )
                        }
                        .sortedWith(
                            compareByDescending<MailMessage> {
                                it.unread && it.important
                            }
                                .thenByDescending {
                                    it.unread
                                }
                                .thenByDescending {
                                    it.important
                                }
                                .thenByDescending {
                                    it.timestamp
                                }
                        )

                MailLoadResult(
                    success = true,
                    messages = loaded,
                    message = "MAIL LOADED"
                )

            } catch (
                _: Exception
            ) {

                MailLoadResult(
                    success = false,
                    messages = emptyList(),
                    message = "MAIL CONNECTION FAILED"
                )
            }
        }
    }

    private fun loadMessage(
        accessToken: String,
        messageId: String
    ): MailMessage? {

        val url =
            URL(
                "https://gmail.googleapis.com/gmail/v1/users/me/messages/$messageId?format=full"
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

                return null
            }

            val root =
                JSONObject(
                    connection
                        .inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }
                )

            val payload =
                root.optJSONObject(
                    "payload"
                )
                    ?: JSONObject()

            val labels =
                root.optJSONArray(
                    "labelIds"
                )
                    ?: JSONArray()

            val rawSender =
                headerValue(
                    payload,
                    "From"
                )
                    .ifBlank {
                        "Unknown sender"
                    }

            val subject =
                headerValue(
                    payload,
                    "Subject"
                )
                    .ifBlank {
                        "(no subject)"
                    }

            val body =
                extractTextBody(
                    payload
                )
                    .trim()

            val snippet =
                root.optString(
                    "snippet"
                )
                    .trim()

            MailMessage(
                id = root.optString(
                    "id"
                ),
                threadId = root.optString(
                    "threadId"
                ),
                sender = cleanSender(
                    rawSender
                ),
                senderEmail = extractEmailAddress(
                    rawSender
                ),
                messageIdHeader = headerValue(
                    payload,
                    "Message-ID"
                ),
                subject = subject,
                snippet = snippet,
                body =
                    body.ifBlank {
                        snippet
                    },
                timestamp =
                    root.optString(
                        "internalDate"
                    )
                        .toLongOrNull()
                        ?: 0L,
                unread =
                    hasLabel(
                        labels,
                        "UNREAD"
                    ),
                important =
                    hasLabel(
                        labels,
                        "IMPORTANT"
                    )
            )

        } catch (
            _: Exception
        ) {

            null

        } finally {

            connection.disconnect()
        }
    }

    suspend fun sendEmail(
        accessToken: String,
        toAddress: String,
        subject: String,
        body: String,
        threadId: String? = null,
        inReplyToMessageId: String? = null
    ): MailSendResult {

        return withContext(
            Dispatchers.IO
        ) {

            if (
                accessToken.isBlank()
            ) {

                return@withContext MailSendResult(
                    success = false,
                    message = "GOOGLE MAIL AUTH REQUIRED"
                )
            }

            val cleanTo =
                sanitizeHeaderValue(
                    toAddress
                )

            val cleanSubject =
                sanitizeHeaderValue(
                    subject
                )

            if (
                cleanTo.isBlank() ||
                !cleanTo.contains(
                    "@"
                )
            ) {

                return@withContext MailSendResult(
                    success = false,
                    message = "EMAIL ADDRESS INVALID"
                )
            }

            if (
                cleanSubject.isBlank()
            ) {

                return@withContext MailSendResult(
                    success = false,
                    message = "SUBJECT REQUIRED"
                )
            }

            if (
                body.isBlank()
            ) {

                return@withContext MailSendResult(
                    success = false,
                    message = "MESSAGE IS EMPTY"
                )
            }

            try {

                val mime =
                    buildString {

                        append(
                            "To: $cleanTo\r\n"
                        )

                        append(
                            "Subject: $cleanSubject\r\n"
                        )

                        if (
                            !inReplyToMessageId.isNullOrBlank()
                        ) {

                            val cleanMessageId =
                                sanitizeHeaderValue(
                                    inReplyToMessageId
                                )

                            if (
                                cleanMessageId.isNotBlank()
                            ) {

                                append(
                                    "In-Reply-To: $cleanMessageId\r\n"
                                )

                                append(
                                    "References: $cleanMessageId\r\n"
                                )
                            }
                        }

                        append(
                            "MIME-Version: 1.0\r\n"
                        )

                        append(
                            "Content-Type: text/plain; charset=UTF-8\r\n"
                        )

                        append(
                            "Content-Transfer-Encoding: 8bit\r\n"
                        )

                        append(
                            "\r\n"
                        )

                        append(
                            body
                        )
                    }

                val encodedRaw =
                    Base64.encodeToString(
                        mime.toByteArray(
                            Charsets.UTF_8
                        ),
                        Base64.URL_SAFE or
                                Base64.NO_WRAP or
                                Base64.NO_PADDING
                    )

                val requestBody =
                    JSONObject()
                        .apply {

                            put(
                                "raw",
                                encodedRaw
                            )

                            if (
                                !threadId.isNullOrBlank()
                            ) {

                                put(
                                    "threadId",
                                    threadId
                                )
                            }
                        }
                        .toString()

                val connection =
                    URL(
                        "https://gmail.googleapis.com/gmail/v1/users/me/messages/send"
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

                    connection.setRequestProperty(
                        "Accept",
                        "application/json"
                    )

                    connection
                        .outputStream
                        .bufferedWriter(
                            Charsets.UTF_8
                        )
                        .use {
                                writer ->

                            writer.write(
                                requestBody
                            )

                            writer.flush()
                        }

                    val responseCode =
                        connection.responseCode

                    if (
                        responseCode in 200..299
                    ) {

                        MailSendResult(
                            success = true,
                            message = "✓ EMAIL SENT"
                        )

                    } else {

                        MailSendResult(
                            success = false,
                            message =
                                when (
                                    responseCode
                                ) {

                                    401 ->
                                        "GOOGLE MAIL AUTH EXPIRED"

                                    403 ->
                                        "GMAIL SEND DENIED"

                                    else ->
                                        "EMAIL SEND FAILED $responseCode"
                                }
                        )
                    }

                } finally {

                    connection.disconnect()
                }

            } catch (
                _: Exception
            ) {

                MailSendResult(
                    success = false,
                    message = "EMAIL SEND FAILED"
                )
            }
        }
    }

    suspend fun markAsRead(
        accessToken: String,
        messageId: String
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

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                val requestBody =
                    JSONObject()
                        .apply {

                            put(
                                "removeLabelIds",
                                JSONArray()
                                    .put(
                                        "UNREAD"
                                    )
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
                            requestBody
                        )

                        writer.flush()
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

    suspend fun markAsUnread(
        accessToken: String,
        messageId: String
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

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                val requestBody =
                    JSONObject()
                        .apply {

                            put(
                                "addLabelIds",
                                JSONArray()
                                    .put(
                                        "UNREAD"
                                    )
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
                            requestBody
                        )

                        writer.flush()
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

    suspend fun trashMessage(
        accessToken: String,
        messageId: String
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
                    "https://gmail.googleapis.com/gmail/v1/users/me/messages/$messageId/trash"
                )
                    .openConnection() as HttpURLConnection

            try {

                connection.requestMethod =
                    "POST"

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

    private fun headerValue(
        payload: JSONObject,
        name: String
    ): String {

        val headers =
            payload.optJSONArray(
                "headers"
            )
                ?: return ""

        for (
        index in 0 until headers.length()
        ) {

            val header =
                headers.optJSONObject(
                    index
                )
                    ?: continue

            if (
                header.optString(
                    "name"
                ).equals(
                    name,
                    ignoreCase = true
                )
            ) {

                return header.optString(
                    "value"
                )
            }
        }

        return ""
    }

    private fun hasLabel(
        labels: JSONArray,
        target: String
    ): Boolean {

        for (
        index in 0 until labels.length()
        ) {

            if (
                labels.optString(
                    index
                ) == target
            ) {

                return true
            }
        }

        return false
    }

    private fun cleanSender(
        raw: String
    ): String {

        val name =
            raw
                .substringBefore(
                    "<"
                )
                .trim()
                .trim(
                    '"'
                )

        return name.ifBlank {

            extractEmailAddress(
                raw
            )
                .substringBefore(
                    "@"
                )
                .trim()
        }
    }

    private fun extractEmailAddress(
        raw: String
    ): String {

        val bracketed =
            Regex(
                "<([^>]+)>"
            )
                .find(
                    raw
                )
                ?.groupValues
                ?.getOrNull(
                    1
                )
                ?.trim()

        if (
            !bracketed.isNullOrBlank()
        ) {

            return bracketed
        }

        val plain =
            Regex(
                "[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}",
                RegexOption.IGNORE_CASE
            )
                .find(
                    raw
                )
                ?.value
                ?.trim()

        return plain.orEmpty()
    }

    private fun sanitizeHeaderValue(
        value: String
    ): String {

        return value
            .replace(
                "\r",
                " "
            )
            .replace(
                "\n",
                " "
            )
            .trim()
    }

    private fun extractTextBody(
        part: JSONObject
    ): String {

        val plain =
            findMimeBody(
                part = part,
                targetMimeType = "text/plain"
            )

        if (
            plain.isNotBlank()
        ) {

            return plain
        }

        val html =
            findMimeBody(
                part = part,
                targetMimeType = "text/html"
            )

        return if (
            html.isNotBlank()
        ) {

            stripHtml(
                html
            )

        } else {

            ""
        }
    }

    private fun findMimeBody(
        part: JSONObject,
        targetMimeType: String
    ): String {

        val mimeType =
            part.optString(
                "mimeType"
            )

        val bodyData =
            part
                .optJSONObject(
                    "body"
                )
                ?.optString(
                    "data"
                )
                .orEmpty()

        if (
            bodyData.isNotBlank() &&
            mimeType.equals(
                targetMimeType,
                ignoreCase = true
            )
        ) {

            return decodeBase64Url(
                bodyData
            )
        }

        val parts =
            part.optJSONArray(
                "parts"
            )
                ?: return ""

        for (
        index in 0 until parts.length()
        ) {

            val child =
                parts.optJSONObject(
                    index
                )
                    ?: continue

            val found =
                findMimeBody(
                    part = child,
                    targetMimeType = targetMimeType
                )

            if (
                found.isNotBlank()
            ) {

                return found
            }
        }

        return ""
    }

    private fun decodeBase64Url(
        value: String
    ): String {

        return try {

            val bytes =
                Base64.decode(
                    value,
                    Base64.URL_SAFE or
                            Base64.NO_WRAP or
                            Base64.NO_PADDING
                )

            bytes.toString(
                Charsets.UTF_8
            )

        } catch (
            _: Exception
        ) {

            ""
        }
    }

    private fun stripHtml(
        html: String
    ): String {

        return html
            .replace(
                Regex(
                    "<br\\s*/?>",
                    RegexOption.IGNORE_CASE
                ),
                "\n"
            )
            .replace(
                Regex(
                    "</p>",
                    RegexOption.IGNORE_CASE
                ),
                "\n\n"
            )
            .replace(
                Regex(
                    "<[^>]+>"
                ),
                ""
            )
            .replace(
                "&nbsp;",
                " "
            )
            .replace(
                "&amp;",
                "&"
            )
            .replace(
                "&lt;",
                "<"
            )
            .replace(
                "&gt;",
                ">"
            )
            .replace(
                "&quot;",
                "\""
            )
            .replace(
                Regex(
                    "\\n{3,}"
                ),
                "\n\n"
            )
            .trim()
    }
}
