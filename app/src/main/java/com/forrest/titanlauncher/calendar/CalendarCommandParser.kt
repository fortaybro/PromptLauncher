package com.forrest.titanlauncher.calendar

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class CalendarTarget {
    PERSONAL,
    FAMILY
}

data class CalendarEventDraft(
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val confirmationTime: String,
    val target: CalendarTarget
)

object CalendarCommandParser {

    private val weekdayNames =
        listOf(
            "sunday",
            "monday",
            "tuesday",
            "wednesday",
            "thursday",
            "friday",
            "saturday"
        )

    private val monthNames =
        mapOf(
            "january" to Calendar.JANUARY,
            "jan" to Calendar.JANUARY,
            "february" to Calendar.FEBRUARY,
            "feb" to Calendar.FEBRUARY,
            "march" to Calendar.MARCH,
            "mar" to Calendar.MARCH,
            "april" to Calendar.APRIL,
            "apr" to Calendar.APRIL,
            "may" to Calendar.MAY,
            "june" to Calendar.JUNE,
            "jun" to Calendar.JUNE,
            "july" to Calendar.JULY,
            "jul" to Calendar.JULY,
            "august" to Calendar.AUGUST,
            "aug" to Calendar.AUGUST,
            "september" to Calendar.SEPTEMBER,
            "sept" to Calendar.SEPTEMBER,
            "sep" to Calendar.SEPTEMBER,
            "october" to Calendar.OCTOBER,
            "oct" to Calendar.OCTOBER,
            "november" to Calendar.NOVEMBER,
            "nov" to Calendar.NOVEMBER,
            "december" to Calendar.DECEMBER,
            "dec" to Calendar.DECEMBER
        )

    fun isCalendarCommand(
        input: String
    ): Boolean {

        return input
            .trim()
            .startsWith(
                "event ",
                ignoreCase =
                    true
            )
    }

    fun parse(
        input: String,
        nowMillis: Long =
            System.currentTimeMillis()
    ): CalendarEventDraft? {

        var original =
            input
                .trim()
                .removePrefixIgnoreCase(
                    "event"
                )
                .trim()

        if (
            original.isBlank()
        ) {

            return null
        }

        val target =
            if (
                original.startsWith(
                    "family ",
                    ignoreCase =
                        true
                )
            ) {

                original =
                    original
                        .removePrefixIgnoreCase(
                            "family"
                        )
                        .trim()

                CalendarTarget.FAMILY

            } else {

                CalendarTarget.PERSONAL
            }

        if (
            original.isBlank()
        ) {

            return null
        }

        val dateResult =
            parseDate(
                original,
                nowMillis
            )
                ?: return null

        val timeResult =
            parseTime(
                original
            )

        val start =
            dateResult.calendar

        if (
            timeResult != null
        ) {

            start.set(
                Calendar.HOUR_OF_DAY,
                timeResult.hour
            )

            start.set(
                Calendar.MINUTE,
                timeResult.minute
            )

        } else {

            start.set(
                Calendar.HOUR_OF_DAY,
                9
            )

            start.set(
                Calendar.MINUTE,
                0
            )
        }

        start.set(
            Calendar.SECOND,
            0
        )

        start.set(
            Calendar.MILLISECOND,
            0
        )

        val title =
            removeDateAndTimeText(
                original,
                dateResult.matchedText,
                timeResult?.matchedText
            )

        if (
            title.isBlank()
        ) {

            return null
        }

        val end =
            start.clone() as Calendar

        end.add(
            Calendar.HOUR_OF_DAY,
            1
        )

        val confirmation =
            SimpleDateFormat(
                "EEE h:mm a",
                Locale.getDefault()
            )
                .format(
                    start.time
                )
                .uppercase(
                    Locale.getDefault()
                )

        return CalendarEventDraft(
            title =
                title,
            startMillis =
                start.timeInMillis,
            endMillis =
                end.timeInMillis,
            confirmationTime =
                confirmation,
            target =
                target
        )
    }

    private data class ParsedDate(
        val calendar: Calendar,
        val matchedText: String
    )

    private data class ParsedTime(
        val hour: Int,
        val minute: Int,
        val matchedText: String
    )

    private fun parseDate(
        text: String,
        nowMillis: Long
    ): ParsedDate? {

        val lower =
            text.lowercase(
                Locale.getDefault()
            )

        val base =
            Calendar
                .getInstance()
                .apply {

                    timeInMillis =
                        nowMillis
                }

        if (
            Regex(
                """\btoday\b""",
                RegexOption.IGNORE_CASE
            )
                .containsMatchIn(
                    lower
                )
        ) {

            return ParsedDate(
                calendar =
                    base,
                matchedText =
                    "today"
            )
        }

        if (
            Regex(
                """\btomorrow\b""",
                RegexOption.IGNORE_CASE
            )
                .containsMatchIn(
                    lower
                )
        ) {

            base.add(
                Calendar.DAY_OF_YEAR,
                1
            )

            return ParsedDate(
                calendar =
                    base,
                matchedText =
                    "tomorrow"
            )
        }

        val weekdayRegex =
            Regex(
                """\b(next\s+)?(monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b""",
                RegexOption.IGNORE_CASE
            )

        val weekdayMatch =
            weekdayRegex.find(
                lower
            )

        if (
            weekdayMatch != null
        ) {

            val nextRequested =
                weekdayMatch
                    .groupValues[1]
                    .isNotBlank()

            val name =
                weekdayMatch
                    .groupValues[2]
                    .lowercase(
                        Locale.getDefault()
                    )

            val targetDay =
                weekdayNames
                    .indexOf(
                        name
                    ) + 1

            var daysAhead =
                targetDay -
                        base.get(
                            Calendar.DAY_OF_WEEK
                        )

            if (
                daysAhead < 0
            ) {

                daysAhead +=
                    7
            }

            if (
                daysAhead == 0
            ) {

                daysAhead =
                    if (
                        nextRequested
                    ) {
                        7
                    } else {
                        0
                    }

            } else if (
                nextRequested
            ) {

                daysAhead +=
                    7
            }

            base.add(
                Calendar.DAY_OF_YEAR,
                daysAhead
            )

            return ParsedDate(
                calendar =
                    base,
                matchedText =
                    weekdayMatch.value
            )
        }

        val monthRegex =
            Regex(
                """\b(january|jan|february|feb|march|mar|april|apr|may|june|jun|july|jul|august|aug|september|sept|sep|october|oct|november|nov|december|dec)\s+(\d{1,2})(?:st|nd|rd|th)?\b""",
                RegexOption.IGNORE_CASE
            )

        val monthMatch =
            monthRegex.find(
                lower
            )

        if (
            monthMatch != null
        ) {

            val monthName =
                monthMatch
                    .groupValues[1]
                    .lowercase(
                        Locale.getDefault()
                    )

            val day =
                monthMatch
                    .groupValues[2]
                    .toIntOrNull()
                    ?: return null

            val month =
                monthNames[monthName]
                    ?: return null

            val currentYear =
                base.get(
                    Calendar.YEAR
                )

            base.set(
                Calendar.YEAR,
                currentYear
            )

            base.set(
                Calendar.MONTH,
                month
            )

            base.set(
                Calendar.DAY_OF_MONTH,
                day
            )

            if (
                base.timeInMillis <
                nowMillis
            ) {

                base.add(
                    Calendar.YEAR,
                    1
                )
            }

            return ParsedDate(
                calendar =
                    base,
                matchedText =
                    monthMatch.value
            )
        }

        return null
    }

    private fun parseTime(
        text: String
    ): ParsedTime? {

        val noonMatch =
            Regex(
                """\b(?:at\s+)?noon\b""",
                RegexOption.IGNORE_CASE
            )
                .find(
                    text
                )

        if (
            noonMatch != null
        ) {

            return ParsedTime(
                hour =
                    12,
                minute =
                    0,
                matchedText =
                    noonMatch.value
            )
        }

        val midnightMatch =
            Regex(
                """\b(?:at\s+)?midnight\b""",
                RegexOption.IGNORE_CASE
            )
                .find(
                    text
                )

        if (
            midnightMatch != null
        ) {

            return ParsedTime(
                hour =
                    0,
                minute =
                    0,
                matchedText =
                    midnightMatch.value
            )
        }

        val timeRegex =
            Regex(
                """\b(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b""",
                RegexOption.IGNORE_CASE
            )

        val match =
            timeRegex.find(
                text
            )
                ?: return null

        var hour =
            match
                .groupValues[1]
                .toIntOrNull()
                ?: return null

        val minute =
            match
                .groupValues[2]
                .takeIf {
                    it.isNotBlank()
                }
                ?.toIntOrNull()
                ?: 0

        val amPm =
            match
                .groupValues[3]
                .lowercase(
                    Locale.getDefault()
                )

        if (
            hour !in 1..12 ||
            minute !in 0..59
        ) {

            return null
        }

        if (
            amPm == "pm" &&
            hour != 12
        ) {

            hour +=
                12
        }

        if (
            amPm == "am" &&
            hour == 12
        ) {

            hour =
                0
        }

        return ParsedTime(
            hour =
                hour,
            minute =
                minute,
            matchedText =
                match.value
        )
    }

    private fun removeDateAndTimeText(
        text: String,
        dateText: String,
        timeText: String?
    ): String {

        var result =
            text

        if (
            !timeText.isNullOrBlank()
        ) {

            result =
                result.replaceFirst(
                    timeText,
                    "",
                    ignoreCase =
                        true
                )
        }

        result =
            result.replaceFirst(
                dateText,
                "",
                ignoreCase =
                    true
            )

        return result
            .replace(
                Regex(
                    """\s+"""
                ),
                " "
            )
            .trim()
            .trim(
                '-',
                ',',
                ':'
            )
            .trim()
    }

    private fun String.removePrefixIgnoreCase(
        prefix: String
    ): String {

        return if (
            startsWith(
                prefix,
                ignoreCase =
                    true
            )
        ) {

            substring(
                prefix.length
            )

        } else {

            this
        }
    }
}