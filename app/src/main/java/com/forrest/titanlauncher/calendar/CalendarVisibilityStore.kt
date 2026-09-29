package com.forrest.titanlauncher.calendar

import android.content.Context


class CalendarVisibilityStore(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )


    fun getHiddenCalendarIds(): Set<String> {

        return preferences
            .getStringSet(
                KEY_HIDDEN_CALENDAR_IDS,
                emptySet()
            )
            ?.toSet()
            ?: emptySet()
    }


    fun isVisible(
        calendarId: String
    ): Boolean {

        return calendarId !in
                getHiddenCalendarIds()
    }


    fun setVisible(
        calendarId: String,
        visible: Boolean
    ) {

        val hidden =
            getHiddenCalendarIds()
                .toMutableSet()

        if (
            visible
        ) {

            hidden.remove(
                calendarId
            )

        } else {

            hidden.add(
                calendarId
            )
        }

        preferences
            .edit()
            .putStringSet(
                KEY_HIDDEN_CALENDAR_IDS,
                hidden
            )
            .apply()
    }


    fun showAll() {

        preferences
            .edit()
            .remove(
                KEY_HIDDEN_CALENDAR_IDS
            )
            .apply()
    }


    companion object {

        private const val PREFERENCES_NAME =
            "prompt_launcher_calendar_visibility"

        private const val KEY_HIDDEN_CALENDAR_IDS =
            "hidden_calendar_ids"
    }
}
