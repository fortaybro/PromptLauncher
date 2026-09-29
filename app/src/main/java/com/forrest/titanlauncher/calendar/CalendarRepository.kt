package com.forrest.titanlauncher.calendar

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CalendarCreateResult(
    val success: Boolean,
    val message: String
)

data class GoogleCalendarOption(
    val id: String,
    val summary: String,
    val primary: Boolean,
    val accessRole: String
)

data class CalendarListResult(
    val success: Boolean,
    val calendars: List<GoogleCalendarOption>,
    val message: String
)


data class GoogleUpcomingEvent(
    val title: String,
    val startTime: Long,
    val allDay: Boolean
)

class CalendarRepository {

    suspend fun loadNextEvent(
        accessToken: String,
        calendarId: String
    ): GoogleUpcomingEvent? {

        return withContext(
            Dispatchers.IO
        ) {

            if (
                accessToken.isBlank() ||
                calendarId.isBlank()
            ) {

                return@withContext null
            }

            var connection:
                    HttpURLConnection? =
                null

            try {

                val encodedCalendarId =
                    Uri.encode(
                        calendarId
                    )

                val timeMin =
                    Uri.encode(
                        Instant
                            .now()
                            .toString()
                    )

                val url =
                    URL(
                        "https://www.googleapis.com/calendar/v3/calendars/$encodedCalendarId/events?timeMin=$timeMin&singleEvents=true&orderBy=startTime&maxResults=1"
                    )

                connection =
                    url
                        .openConnection() as HttpURLConnection

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

                val responseCode =
                    connection.responseCode

                if (
                    responseCode !in 200..299
                ) {

                    return@withContext null
                }

                val responseBody =
                    connection
                        .inputStream
                        .bufferedReader()
                        .use {

                            it.readText()
                        }

                val root =
                    JSONObject(
                        responseBody
                    )

                val items =
                    root.optJSONArray(
                        "items"
                    )
                        ?: return@withContext null

                if (
                    items.length() == 0
                ) {

                    return@withContext null
                }

                val event =
                    items.optJSONObject(
                        0
                    )
                        ?: return@withContext null

                val title =
                    event
                        .optString(
                            "summary"
                        )
                        .ifBlank {
                            "untitled event"
                        }

                val start =
                    event.optJSONObject(
                        "start"
                    )
                        ?: return@withContext null

                val dateTime =
                    start.optString(
                        "dateTime"
                    )

                if (
                    dateTime.isNotBlank()
                ) {

                    val startMillis =
                        java.time.OffsetDateTime
                            .parse(
                                dateTime
                            )
                            .toInstant()
                            .toEpochMilli()

                    return@withContext GoogleUpcomingEvent(
                        title =
                            title,
                        startTime =
                            startMillis,
                        allDay =
                            false
                    )
                }

                val date =
                    start.optString(
                        "date"
                    )

                if (
                    date.isBlank()
                ) {

                    return@withContext null
                }

                val startMillis =
                    java.time.LocalDate
                        .parse(
                            date
                        )
                        .atStartOfDay(
                            ZoneId.systemDefault()
                        )
                        .toInstant()
                        .toEpochMilli()

                GoogleUpcomingEvent(
                    title =
                        title,
                    startTime =
                        startMillis,
                    allDay =
                        true
                )

            } catch (
                _: Exception
            ) {

                null

            } finally {

                connection
                    ?.disconnect()
            }
        }
    }

    suspend fun loadWritableCalendars(
        accessToken: String
    ): CalendarListResult {

        return withContext(
            Dispatchers.IO
        ) {

            if (
                accessToken.isBlank()
            ) {

                return@withContext CalendarListResult(
                    success =
                        false,
                    calendars =
                        emptyList(),
                    message =
                        "GOOGLE AUTH REQUIRED"
                )
            }

            val allCalendars =
                mutableListOf<GoogleCalendarOption>()

            var pageToken:
                    String? =
                null

            try {

                do {

                    val endpoint =
                        buildString {

                            append(
                                "https://www.googleapis.com/calendar/v3/users/me/calendarList"
                            )

                            if (
                                !pageToken.isNullOrBlank()
                            ) {

                                append(
                                    "?pageToken="
                                )

                                append(
                                    Uri.encode(
                                        pageToken
                                    )
                                )
                            }
                        }

                    val connection =
                        URL(
                            endpoint
                        )
                            .openConnection() as HttpURLConnection

                    try {

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

                        val responseCode =
                            connection.responseCode

                        if (
                            responseCode !in 200..299
                        ) {

                            return@withContext CalendarListResult(
                                success =
                                    false,
                                calendars =
                                    emptyList(),
                                message =
                                    when (
                                        responseCode
                                    ) {

                                        401 ->
                                            "GOOGLE AUTH EXPIRED"

                                        403 ->
                                            "CALENDAR LIST ACCESS DENIED"

                                        else ->
                                            "CALENDAR LIST ERROR $responseCode"
                                    }
                            )
                        }

                        val responseBody =
                            connection
                                .inputStream
                                .bufferedReader()
                                .use {

                                    it.readText()
                                }

                        val root =
                            JSONObject(
                                responseBody
                            )

                        val items =
                            root.optJSONArray(
                                "items"
                            )
                                ?: JSONArray()

                        for (
                        index in
                        0 until items.length()
                        ) {

                            val item =
                                items.optJSONObject(
                                    index
                                )
                                    ?: continue

                            val accessRole =
                                item.optString(
                                    "accessRole"
                                )

                            val writable =
                                accessRole.equals(
                                    "owner",
                                    ignoreCase =
                                        true
                                ) ||
                                        accessRole.equals(
                                            "writer",
                                            ignoreCase =
                                                true
                                        )

                            if (
                                !writable
                            ) {

                                continue
                            }

                            val id =
                                item.optString(
                                    "id"
                                )

                            if (
                                id.isBlank()
                            ) {

                                continue
                            }

                            allCalendars.add(
                                GoogleCalendarOption(
                                    id =
                                        id,
                                    summary =
                                        item
                                            .optString(
                                                "summary"
                                            )
                                            .ifBlank {
                                                "Unnamed Calendar"
                                            },
                                    primary =
                                        item.optBoolean(
                                            "primary",
                                            false
                                        ),
                                    accessRole =
                                        accessRole
                                )
                            )
                        }

                        pageToken =
                            root
                                .optString(
                                    "nextPageToken"
                                )
                                .takeIf {

                                    it.isNotBlank()
                                }

                    } finally {

                        connection.disconnect()
                    }

                } while (
                    !pageToken.isNullOrBlank()
                )

            } catch (
                _: Exception
            ) {

                return@withContext CalendarListResult(
                    success =
                        false,
                    calendars =
                        emptyList(),
                    message =
                        "CALENDAR LIST FAILED"
                )
            }

            val sorted =
                allCalendars
                    .distinctBy {
                        it.id
                    }
                    .sortedWith(
                        compareByDescending<GoogleCalendarOption> {
                            it.primary
                        }
                            .thenBy {
                                it.summary
                                    .lowercase()
                            }
                    )

            CalendarListResult(
                success =
                    true,
                calendars =
                    sorted,
                message =
                    if (
                        sorted.isEmpty()
                    ) {

                        "NO WRITABLE CALENDARS"

                    } else {

                        "CALENDARS LOADED"
                    }
            )
        }
    }

    suspend fun createEvent(
        accessToken: String,
        calendarId: String,
        calendarLabel: String,
        draft: CalendarEventDraft
    ): CalendarCreateResult {

        return withContext(
            Dispatchers.IO
        ) {

            if (
                accessToken.isBlank()
            ) {

                return@withContext CalendarCreateResult(
                    success =
                        false,
                    message =
                        "GOOGLE AUTH REQUIRED"
                )
            }

            if (
                calendarId.isBlank()
            ) {

                return@withContext CalendarCreateResult(
                    success =
                        false,
                    message =
                        "RUN CALENDARSETUP"
                )
            }

            var connection:
                    HttpURLConnection? =
                null

            try {

                val encodedCalendarId =
                    Uri.encode(
                        calendarId
                    )

                val url =
                    URL(
                        "https://www.googleapis.com/calendar/v3/calendars/$encodedCalendarId/events"
                    )

                connection =
                    url
                        .openConnection() as HttpURLConnection

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

                val zoneId =
                    ZoneId.systemDefault()

                val formatter =
                    DateTimeFormatter
                        .ISO_OFFSET_DATE_TIME

                val startDateTime =
                    Instant
                        .ofEpochMilli(
                            draft.startMillis
                        )
                        .atZone(
                            zoneId
                        )
                        .format(
                            formatter
                        )

                val endDateTime =
                    Instant
                        .ofEpochMilli(
                            draft.endMillis
                        )
                        .atZone(
                            zoneId
                        )
                        .format(
                            formatter
                        )

                val requestBody =
                    JSONObject()
                        .apply {

                            put(
                                "summary",
                                draft.title
                            )

                            put(
                                "start",
                                JSONObject()
                                    .apply {

                                        put(
                                            "dateTime",
                                            startDateTime
                                        )

                                        put(
                                            "timeZone",
                                            zoneId.id
                                        )
                                    }
                            )

                            put(
                                "end",
                                JSONObject()
                                    .apply {

                                        put(
                                            "dateTime",
                                            endDateTime
                                        )

                                        put(
                                            "timeZone",
                                            zoneId.id
                                        )
                                    }
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

                val responseCode =
                    connection.responseCode

                if (
                    responseCode in 200..299
                ) {

                    CalendarCreateResult(
                        success =
                            true,
                        message =
                            "✓ $calendarLabel · ${draft.confirmationTime}"
                    )

                } else {

                    CalendarCreateResult(
                        success =
                            false,
                        message =
                            when (
                                responseCode
                            ) {

                                401 ->
                                    "GOOGLE AUTH EXPIRED"

                                403 ->
                                    "CALENDAR ACCESS DENIED"

                                404 ->
                                    "CALENDAR NOT FOUND"

                                429 ->
                                    "CALENDAR RATE LIMIT"

                                else ->
                                    "CALENDAR ERROR $responseCode"
                            }
                    )
                }

            } catch (
                _: Exception
            ) {

                CalendarCreateResult(
                    success =
                        false,
                    message =
                        "CALENDAR CONNECTION FAILED"
                )

            } finally {

                connection
                    ?.disconnect()
            }
        }
    }
}