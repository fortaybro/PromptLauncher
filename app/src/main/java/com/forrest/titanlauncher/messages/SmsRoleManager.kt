package com.forrest.titanlauncher.messages

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony

class SmsRoleManager(
    private val context: Context
) {

    fun isSmsRoleAvailable(): Boolean {

        /*
         * Android 10+ uses RoleManager for the
         * official default SMS app role.
         */
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            val roleManager =
                context.getSystemService(
                    RoleManager::class.java
                )
                    ?: return false

            return runCatching {

                roleManager.isRoleAvailable(
                    RoleManager.ROLE_SMS
                )

            }.getOrDefault(
                false
            )
        }

        /*
         * Older Android versions do not expose
         * RoleManager. SMS handling is still
         * supported through the legacy default
         * SMS package mechanism.
         */
        return true
    }

    fun isDefaultSmsApp(): Boolean {

        /*
         * On Android 10+, RoleManager is the
         * authoritative source of truth.
         *
         * Do NOT additionally require
         * Telephony.Sms.getDefaultSmsPackage()
         * to match. Some devices/emulators can
         * temporarily report stale information
         * there even after Android has correctly
         * assigned ROLE_SMS.
         */
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            val roleManager =
                context.getSystemService(
                    RoleManager::class.java
                )
                    ?: return false

            return runCatching {

                roleManager.isRoleHeld(
                    RoleManager.ROLE_SMS
                )

            }.getOrDefault(
                false
            )
        }

        /*
         * Legacy fallback for Android 9
         * and earlier.
         */
        return runCatching {

            Telephony.Sms
                .getDefaultSmsPackage(
                    context
                ) ==
                    context.packageName

        }.getOrDefault(
            false
        )
    }

    fun createSmsRoleRequestIntent(): Intent {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.Q
        ) {

            throw IllegalStateException(
                "SMS role requires Android 10 or newer."
            )
        }

        val roleManager =
            context.getSystemService(
                RoleManager::class.java
            )
                ?: throw IllegalStateException(
                    "RoleManager is unavailable."
                )

        if (
            !roleManager.isRoleAvailable(
                RoleManager.ROLE_SMS
            )
        ) {

            throw IllegalStateException(
                "SMS role is unavailable on this device."
            )
        }

        return roleManager
            .createRequestRoleIntent(
                RoleManager.ROLE_SMS
            )
    }
}