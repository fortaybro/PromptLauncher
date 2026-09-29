package com.forrest.titanlauncher.calendar

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


data class CalendarAssistCalendar(
    val id: String,
    val summary: String,
    val primary: Boolean
)


data class CalendarAssistEvent(
    val title: String,
    val startTime: Long,
    val allDay: Boolean,
    val location: String
)


data class CalendarAssistSnapshot(
    val nextEvent: CalendarAssistEvent?,
    val agendaEvents: List<CalendarAssistEvent>
)


class CalendarAssistRepository {

    suspend fun loadAllCalendarsSnapshot(
        accessToken: String,
        hiddenCalendarIds: Set<String> = emptySet(),
        agendaDays: Long = 8,
        nextEventDays: Long = 30
    ): CalendarAssistSnapshot {

        return withContext(
            Dispatchers.IO
        ) {

            if (
                accessToken.isBlank()
            ) {

                return@withContext CalendarAssistSnapshot(
                    nextEvent =
                        null,
                    agendaEvents =
                        emptyList()
                )
            }

            val visibleCalendarIds =
                loadReadableCalendarsInternal(
                    accessToken
                )
                    .map {
                        it.id
                    }
                    .filterNot {
                        it in
                                hiddenCalendarIds
                    }

            if (
                visibleCalendarIds.isEmpty()
            ) {

                return@withContext CalendarAssistSnapshot(
                    nextEvent =
                        null,
                    agendaEvents =
                        emptyList()
                )
            }

            val zone =
                ZoneId.systemDefault()

            val today =
                LocalDate.now(
                    zone
                )

            val startMillis =
                today
                    .atStartOfDay(
                        zone
                    )
                    .toInstant()
                    .toEpochMilli()

            val agendaEndMillis =
                today
                    .plusDays(
                        agendaDays
                    )
                    .atStartOfDay(
                        zone
                    )
                    .toInstant()
                    .toEpochMilli()

            val nextEventEndMillis =
                today
                    .plusDays(
                        nextEventDays
                    )
                    .atStartOfDay(
                        zone
                    )
                    .toInstant()
                    .toEpochMilli()

            val endMillis =
                maxOf(
                    agendaEndMillis,
                    nextEventEndMillis
                )

            val allEvents =
                loadEventsBetweenInternal(
                    accessToken =
                        accessToken,
                    calendarIds =
                        visibleCalendarIds,
                    startMillis =
                        startMillis,
                    endMillis =
                        endMillis
                )

            val now =
                System.currentTimeMillis()

            val nextEvent =
                allEvents
                    .firstOrNull {
                            event ->

                        if (
                            event.allDay
                        ) {

                            eventLocalDate(
                                event =
                                    event,
                                zone =
                                    zone
                            ) >=
                                    today

                        } else {

                            event.startTime >=
                                    now
                        }
                    }

            val agendaEvents =
                allEvents
                    .filter {
                            event ->

                        event.startTime <
                                agendaEndMillis
                    }

            CalendarAssistSnapshot(
                nextEvent =
                    nextEvent,
                agendaEvents =
                    agendaEvents
            )
        }
    }


    suspend fun loadReadableCalendars(
        accessToken: String
    ): List<CalendarAssistCalendar> {

        return withContext(
            Dispatchers.IO
        ) {

            loadReadableCalendarsInternal(
                accessToken
            )
        }
    }


    suspend fun loadNextEvent(
        accessToken: String,
        calendarId: String
    ): CalendarAssistEvent? {

        return loadEventsBetween(
            accessToken =
                accessToken,
            calendarIds =
                listOf(
                    calendarId
                ),
            startMillis =
                System.currentTimeMillis(),
            endMillis =
                LocalDate
                    .now()
                    .plusDays(
                        31
                    )
                    .atStartOfDay(
                        ZoneId.systemDefault()
                    )
                    .toInstant()
                    .toEpochMilli()
        )
            .firstOrNull()
    }


    suspend fun loadUpcomingAgenda(
        accessToken: String,
        calendarIds: List<String>,
        days: Long = 8
    ): List<CalendarAssistEvent> {

        val zone =
            ZoneId.systemDefault()

        val startMillis =
            LocalDate
                .now(
                    zone
                )
                .atStartOfDay(
                    zone
                )
                .toInstant()
                .toEpochMilli()

        val endMillis =
            LocalDate
                .now(
                    zone
                )
                .plusDays(
                    days
                )
                .atStartOfDay(
                    zone
                )
                .toInstant()
                .toEpochMilli()

        return loadEventsBetween(
            accessToken =
                accessToken,
            calendarIds =
                calendarIds,
            startMillis =
                startMillis,
            endMillis =
                endMillis
        )
    }


    suspend fun loadEventsBetween(
        accessToken: String,
        calendarIds: List<String>,
        startMillis: Long,
        endMillis: Long
    ): List<CalendarAssistEvent> {

        return withContext(
            Dispatchers.IO
        ) {

            loadEventsBetweenInternal(
                accessToken =
                    accessToken,
                calendarIds =
                    calendarIds,
                startMillis =
                    startMillis,
                endMillis =
                    endMillis
            )
        }
    }


    private fun loadReadableCalendarsInternal(
        accessToken: String
    ): List<CalendarAssistCalendar> {

        val calendars =
            mutableListOf<CalendarAssistCalendar>()

        var pageToken:
                String? =
            null

        do {

            var connection:
                    HttpURLConnection? =
                null

            try {

                val pageTokenParameter =
                    pageToken
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                                token ->

                            "&pageToken=${
                                Uri.encode(
                                    token
                                )
                            }"
                        }
                        .orEmpty()

                val url =
                    URL(
                        "https://www.googleapis.com/calendar/v3/users/me/calendarList" +
                                "?showHidden=true" +
                                "&minAccessRole=reader" +
                                "&maxResults=250" +
                                pageTokenParameter
                    )

                connection =
                    url.openConnection() as HttpURLConnection

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
                    connection.responseCode !in
                    200..299
                ) {

                    break
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

                val items =
                    root.optJSONArray(
                        "items"
                    )

                if (
                    items !=
                    null
                ) {

                    for (
                    index in 0 until items.length()
                    ) {

                        val item =
                            items.optJSONObject(
                                index
                            )
                                ?: continue

                        val id =
                            item
                                .optString(
                                    "id"
                                )
                                .trim()

                        if (
                            id.isBlank()
                        ) {

                            continue
                        }

                        val summary =
                            item
                                .optString(
                                    "summary"
                                )
                                .trim()
                                .ifBlank {
                                    id
                                }

                        calendars +=
                            CalendarAssistCalendar(
                                id =
                                    id,
                                summary =
                                    summary,
                                primary =
                                    item.optBoolean(
                                        "primary",
                                        false
                                    )
                            )
                    }
                }

                pageToken =
                    root
                        .optString(
                            "nextPageToken"
                        )
                        .takeIf {
                            it.isNotBlank()
                        }

            } catch (
                _: Exception
            ) {

                break

            } finally {

                connection
                    ?.disconnect()
            }

        } while (
            pageToken !=
            null
        )

        return calendars
            .distinctBy {
                it.id
            }
            .sortedWith(
                compareByDescending<CalendarAssistCalendar> {
                    it.primary
                }
                    .thenBy {
                        it.summary
                            .lowercase()
                    }
            )
    }


    private fun loadEventsBetweenInternal(
        accessToken: String,
        calendarIds: List<String>,
        startMillis: Long,
        endMillis: Long
    ): List<CalendarAssistEvent> {

        if (
            accessToken.isBlank() ||
            calendarIds.isEmpty()
        ) {

            return emptyList()
        }

        val results =
            mutableListOf<CalendarAssistEvent>()

        calendarIds
            .filter {
                it.isNotBlank()
            }
            .distinct()
            .forEach {
                    calendarId ->

                results +=
                    loadCalendarEventsBetween(
                        accessToken =
                            accessToken,
                        calendarId =
                            calendarId,
                        startMillis =
                            startMillis,
                        endMillis =
                            endMillis
                    )
            }

        return results
            .distinctBy {
                Triple(
                    it.title,
                    it.startTime,
                    it.location
                )
            }
            .sortedBy {
                it.startTime
            }
    }


    private fun loadCalendarEventsBetween(
        accessToken: String,
        calendarId: String,
        startMillis: Long,
        endMillis: Long
    ): List<CalendarAssistEvent> {

        var pageToken:
                String? =
            null

        val events =
            mutableListOf<CalendarAssistEvent>()

        do {

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
                            .ofEpochMilli(
                                startMillis
                            )
                            .toString()
                    )

                val timeMax =
                    Uri.encode(
                        Instant
                            .ofEpochMilli(
                                endMillis
                            )
                            .toString()
                    )

                val pageTokenParameter =
                    pageToken
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                                token ->

                            "&pageToken=${
                                Uri.encode(
                                    token
                                )
                            }"
                        }
                        .orEmpty()

                val url =
                    URL(
                        "https://www.googleapis.com/calendar/v3/calendars/" +
                                "$encodedCalendarId/events" +
                                "?timeMin=$timeMin" +
                                "&timeMax=$timeMax" +
                                "&singleEvents=true" +
                                "&orderBy=startTime" +
                                "&maxResults=250" +
                                pageTokenParameter
                    )

                connection =
                    url.openConnection() as HttpURLConnection

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
                    connection.responseCode !in
                    200..299
                ) {

                    break
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

                val items =
                    root.optJSONArray(
                        "items"
                    )

                if (
                    items !=
                    null
                ) {

                    for (
                    index in 0 until items.length()
                    ) {

                        val event =
                            items.optJSONObject(
                                index
                            )
                                ?: continue

                        if (
                            event.optString(
                                "status"
                            )
                                .equals(
                                    "cancelled",
                                    ignoreCase =
                                        true
                                )
                        ) {

                            continue
                        }

                        val title =
                            event
                                .optString(
                                    "summary"
                                )
                                .ifBlank {
                                    "untitled event"
                                }

                        val location =
                            event
                                .optString(
                                    "location"
                                )
                                .trim()

                        val start =
                            event.optJSONObject(
                                "start"
                            )
                                ?: continue

                        val dateTime =
                            start.optString(
                                "dateTime"
                            )

                        if (
                            dateTime.isNotBlank()
                        ) {

                            val eventStartMillis =
                                runCatching {

                                    java.time.OffsetDateTime
                                        .parse(
                                            dateTime
                                        )
                                        .toInstant()
                                        .toEpochMilli()
                                }
                                    .getOrNull()
                                    ?: continue

                            events +=
                                CalendarAssistEvent(
                                    title =
                                        title,
                                    startTime =
                                        eventStartMillis,
                                    allDay =
                                        false,
                                    location =
                                        location
                                )

                            continue
                        }

                        val date =
                            start.optString(
                                "date"
                            )

                        if (
                            date.isBlank()
                        ) {

                            continue
                        }

                        val eventStartMillis =
                            runCatching {

                                LocalDate
                                    .parse(
                                        date
                                    )
                                    .atStartOfDay(
                                        ZoneId.systemDefault()
                                    )
                                    .toInstant()
                                    .toEpochMilli()
                            }
                                .getOrNull()
                                ?: continue

                        events +=
                            CalendarAssistEvent(
                                title =
                                    title,
                                startTime =
                                    eventStartMillis,
                                allDay =
                                    true,
                                location =
                                    location
                            )
                    }
                }

                pageToken =
                    root
                        .optString(
                            "nextPageToken"
                        )
                        .takeIf {
                            it.isNotBlank()
                        }

            } catch (
                _: Exception
            ) {

                break

            } finally {

                connection
                    ?.disconnect()
            }

        } while (
            pageToken !=
            null
        )

        return events
    }


    private fun eventLocalDate(
        event: CalendarAssistEvent,
        zone: ZoneId
    ): LocalDate {

        return Instant
            .ofEpochMilli(
                event.startTime
            )
            .atZone(
                zone
            )
            .toLocalDate()
    }
}
