package com.forrest.titanlauncher.mail

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope

class GoogleMailAuthManager(
    activity: Activity
) {

    companion object {
        const val GMAIL_MODIFY_SCOPE =
            "https://www.googleapis.com/auth/gmail.modify"
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
                            GMAIL_MODIFY_SCOPE
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
                            "GOOGLE MAIL AUTH REQUIRED"
                        )
                    }

                } else {

                    val accessToken =
                        result.accessToken

                    if (
                        accessToken.isNullOrBlank()
                    ) {

                        onError(
                            "GOOGLE MAIL TOKEN MISSING"
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
                    "GOOGLE MAIL AUTH FAILED"
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
