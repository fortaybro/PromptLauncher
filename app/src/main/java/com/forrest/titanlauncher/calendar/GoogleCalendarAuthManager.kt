package com.forrest.titanlauncher.calendar

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope

class GoogleCalendarAuthManager(
    private val activity: Activity
) {

    companion object {

        const val EVENTS_SCOPE =
            "https://www.googleapis.com/auth/calendar.events"

        const val CALENDAR_LIST_SCOPE =
            "https://www.googleapis.com/auth/calendar.calendarlist.readonly"
    }

    private val authorizationClient =
        Identity.getAuthorizationClient(
            activity
        )

    fun authorize(
        onAuthorized: (String) -> Unit,
        onNeedsUserConsent: (PendingIntent) -> Unit,
        onError: (String) -> Unit
    ) {

        val request =
            AuthorizationRequest
                .builder()
                .setRequestedScopes(
                    listOf(
                        Scope(
                            EVENTS_SCOPE
                        ),
                        Scope(
                            CALENDAR_LIST_SCOPE
                        )
                    )
                )
                .build()

        authorizationClient
            .authorize(
                request
            )
            .addOnSuccessListener {
                    result ->

                if (
                    result.hasResolution()
                ) {

                    val pendingIntent =
                        result.pendingIntent

                    if (
                        pendingIntent != null
                    ) {

                        onNeedsUserConsent(
                            pendingIntent
                        )

                    } else {

                        onError(
                            "GOOGLE AUTH REQUIRED"
                        )
                    }

                } else {

                    val accessToken =
                        result.accessToken

                    if (
                        accessToken.isNullOrBlank()
                    ) {

                        onError(
                            "GOOGLE TOKEN MISSING"
                        )

                    } else {

                        onAuthorized(
                            accessToken
                        )
                    }
                }
            }
            .addOnFailureListener {

                onError(
                    "GOOGLE AUTH FAILED"
                )
            }
    }

    fun getAccessTokenFromResult(
        data: Intent?
    ): String? {

        if (
            data == null
        ) {

            return null
        }

        return try {

            authorizationClient
                .getAuthorizationResultFromIntent(
                    data
                )
                .accessToken

        } catch (
            _: Exception
        ) {

            null
        }
    }
}