package com.forrest.titanlauncher.mail

import android.content.Context
import android.content.Intent
import android.net.Uri

/*
 * MAIL APPS
 *
 * Prompt Launcher reads Gmail directly. Every other mail app (Spark,
 * Samsung Email, Outlook, ...) is reached through Android instead:
 * its notifications show in the hub's email tab, tapping one opens
 * the email in that app, and composing can hand off to it.
 */

/*
 * Mail apps recognised by package even if they do not advertise a
 * mailto: handler. Gmail is left out: Prompt already shows Gmail
 * itself, so its notifications would only be duplicates.
 */
internal val KnownMailAppPackages =
    setOf(
        "com.readdle.spark",
        "com.samsung.android.email.provider",
        "com.microsoft.office.outlook",
        "com.yahoo.mobile.client.android.mail",
        "ch.protonmail.android",
        "me.bluemail.mail",
        "com.fsck.k9",
        "net.thunderbird.android",
        "com.fastmail.app",
        "com.easilydo.mail",
        "com.aol.mobile.aolapp"
    )

internal data class MailAppEntry(
    val packageName: String,
    val label: String
)

/*
 * Installed mail apps: anything that handles mailto: links, plus the
 * known list above. Gmail and Prompt Launcher itself are excluded.
 */
internal fun findMailApps(
    context: Context
): List<MailAppEntry> {

    val packageManager =
        context.packageManager

    val excluded =
        setOf(
            context.packageName,
            "com.google.android.gm"
        )

    val mailtoHandlers =
        runCatching {
            @Suppress("DEPRECATION")
            packageManager
                .queryIntentActivities(
                    Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse(
                            "mailto:"
                        )
                    ),
                    0
                )
                .mapNotNull {
                    it.activityInfo?.packageName
                }
        }
            .getOrDefault(
                emptyList()
            )

    val installedKnown =
        KnownMailAppPackages.filter { packageName ->
            runCatching {
                packageManager.getLaunchIntentForPackage(
                    packageName
                ) != null
            }
                .getOrDefault(
                    false
                )
        }

    return (mailtoHandlers + installedKnown)
        .distinct()
        .filterNot {
            it in excluded
        }
        .mapNotNull { packageName ->
            runCatching {
                val info =
                    packageManager.getApplicationInfo(
                        packageName,
                        0
                    )

                MailAppEntry(
                    packageName =
                        packageName,
                    label =
                        packageManager
                            .getApplicationLabel(
                                info
                            )
                            .toString()
                )
            }
                .getOrNull()
        }
        .sortedBy {
            it.label.lowercase()
        }
}

/*
 * Package names whose notifications count as email in the hub.
 */
internal fun mailAppPackageSet(
    context: Context
): Set<String> =
    findMailApps(
        context
    )
        .map {
            it.packageName
        }
        .toSet() +
            KnownMailAppPackages

/*
 * Opens a new email to this address in the chosen mail app. With no
 * app chosen, or if that app is gone, Android asks which app to use.
 * Returns false only when nothing could handle it.
 */
internal fun composeInMailApp(
    context: Context,
    packageName: String?,
    toAddress: String
): Boolean {

    val base =
        Intent(
            Intent.ACTION_SENDTO,
            Uri.parse(
                "mailto:" + toAddress.trim()
            )
        )

    if (
        !packageName.isNullOrBlank()
    ) {
        val opened =
            runCatching {
                context.startActivity(
                    Intent(base).setPackage(
                        packageName
                    )
                )
                true
            }
                .getOrDefault(
                    false
                )

        if (
            opened
        ) {
            return true
        }
    }

    return runCatching {
        context.startActivity(
            Intent.createChooser(
                base,
                "Send email with"
            )
        )
        true
    }
        .getOrDefault(
            false
        )
}

internal const val GmailPackage =
    "com.google.android.gm"

/*
 * Every installed email app, Gmail included, for choosing providers in
 * onboarding and settings.
 */
internal fun findEmailProviders(
    context: Context
): List<MailAppEntry> {

    val gmail =
        runCatching {
            val packageManager =
                context.packageManager

            if (
                packageManager.getLaunchIntentForPackage(
                    GmailPackage
                ) == null
            ) {
                null
            } else {
                MailAppEntry(
                    packageName =
                        GmailPackage,
                    label =
                        packageManager
                            .getApplicationLabel(
                                packageManager.getApplicationInfo(
                                    GmailPackage,
                                    0
                                )
                            )
                            .toString()
                )
            }
        }
            .getOrNull()

    return (
            listOfNotNull(
                gmail
            ) +
                    findMailApps(
                        context
                    )
            )
        .distinctBy {
            it.packageName
        }
}

/*
 * Packages treated as email everywhere: their notifications are left
 * in the shade and filed under email, like texts.
 */
internal fun allMailPackages(
    context: Context
): Set<String> =
    mailAppPackageSet(
        context
    ) +
            GmailPackage

/*
 * Opens the user's email app: the first chosen provider that is still
 * installed, otherwise Android's default email app, otherwise Gmail.
 */
internal fun openMailApp(
    context: Context,
    preferredPackages: List<String>
): Boolean {

    val packageManager =
        context.packageManager

    val launchIntent =
        preferredPackages
            .asSequence()
            .mapNotNull { packageName ->
                runCatching {
                    packageManager.getLaunchIntentForPackage(
                        packageName
                    )
                }
                    .getOrNull()
            }
            .firstOrNull()
            ?: runCatching {
                Intent.makeMainSelectorActivity(
                    Intent.ACTION_MAIN,
                    Intent.CATEGORY_APP_EMAIL
                )
            }
                .getOrNull()
            ?: runCatching {
                packageManager.getLaunchIntentForPackage(
                    GmailPackage
                )
            }
                .getOrNull()
            ?: return false

    return runCatching {
        context.startActivity(
            launchIntent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )
        )
        true
    }
        .getOrDefault(
            false
        )
}
