package com.forrest.titanlauncher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat


/*
 * INCOMING MESSAGE NOTIFICATIONS
 *
 * As the default SMS app, Prompt Launcher owns this. Nothing else on
 * the device will post for an incoming text, which is why messages
 * used to arrive silently with no entry in the shade.
 *
 * The sound is attached to the channel rather than the notification,
 * because that is the only thing Android honours from Android 8
 * onward. A channel's sound is fixed once created, so the id carries
 * a version: changing the sound means bumping it so a new channel is
 * created rather than silently keeping the old one.
 */

/*
 * A channel's sound is fixed when it is created and cannot be changed
 * afterwards, so a channel made before sms_notify.wav existed stays
 * silent forever. Bumping the version creates a fresh one.
 */
private const val MessageChannelId =
    "sms_messages_v2"

private const val MessageChannelName =
    "Text messages"


object MessageNotifications {

    /*
     * Set while a conversation is on screen. An arriving message for
     * that same thread plays the in-app sound instead, so the user
     * does not get both at once.
     */
    @Volatile
    var activeConversationNumber: String? = null

    fun ensureChannel(
        context: Context
    ) {

        val manager =
            context.getSystemService(
                NotificationManager::class.java
            )
                ?: return

        /*
         * Remove superseded channels so the user is not left with a
         * dead "Text messages" entry in system settings.
         */
        runCatching {
            manager.deleteNotificationChannel(
                "sms_messages_v1"
            )
        }

        if (
            manager.getNotificationChannel(
                MessageChannelId
            ) != null
        ) {
            return
        }

        val soundUri =
            Uri.parse(
                "android.resource://" +
                        context.packageName +
                        "/" +
                        R.raw.sms_notify
            )

        val channel =
            NotificationChannel(
                MessageChannelId,
                MessageChannelName,
                NotificationManager.IMPORTANCE_HIGH
            )
                .apply {

                    description =
                        "Incoming text messages"

                    setSound(
                        soundUri,
                        AudioAttributes.Builder()
                            .setUsage(
                                AudioAttributes.USAGE_NOTIFICATION
                            )
                            .setContentType(
                                AudioAttributes.CONTENT_TYPE_SONIFICATION
                            )
                            .build()
                    )

                    enableVibration(
                        true
                    )
                }

        manager.createNotificationChannel(
            channel
        )
    }

    fun notifyIncoming(
        context: Context,
        phoneNumber: String,
        displayName: String,
        body: String
    ) {

        if (
            isThreadOnScreen(
                phoneNumber
            )
        ) {
            return
        }

        ensureChannel(
            context
        )

        val manager =
            context.getSystemService(
                NotificationManager::class.java
            )
                ?: return

        val launchIntent =
            context
                .packageManager
                .getLaunchIntentForPackage(
                    context.packageName
                )
                ?.apply {

                    flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

        val pendingIntent =
            if (
                launchIntent == null
            ) {
                null
            } else {
                PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                            PendingIntent.FLAG_IMMUTABLE
                )
            }

        val notification =
            NotificationCompat.Builder(
                context,
                MessageChannelId
            )
                /*
                 * A launcher has no monochrome notification icon of
                 * its own yet, so this borrows a system one. Swap in
                 * a real drawable when there is one.
                 */
                .setSmallIcon(
                    android.R.drawable.ic_dialog_email
                )
                .setContentTitle(
                    displayName
                )
                .setContentText(
                    body
                )
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            body
                        )
                )
                .setCategory(
                    Notification.CATEGORY_MESSAGE
                )
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setAutoCancel(
                    true
                )
                .apply {

                    if (
                        pendingIntent != null
                    ) {
                        setContentIntent(
                            pendingIntent
                        )
                    }
                }
                .build()

        runCatching {
            manager.notify(
                notificationIdFor(
                    phoneNumber
                ),
                notification
            )
        }
    }

    fun clearThread(
        context: Context,
        phoneNumber: String
    ) {

        runCatching {
            context
                .getSystemService(
                    NotificationManager::class.java
                )
                ?.cancel(
                    notificationIdFor(
                        phoneNumber
                    )
                )
        }
    }

    /*
     * One notification per conversation, replaced as new messages
     * arrive, rather than a growing stack from the same person.
     */
    private fun notificationIdFor(
        phoneNumber: String
    ): Int {

        return normalizePhoneNumber(
            phoneNumber
        )
            .hashCode()
    }

    private fun isThreadOnScreen(
        phoneNumber: String
    ): Boolean {

        val open =
            activeConversationNumber
                ?: return false

        return normalizePhoneNumber(
            open
        ) ==
                normalizePhoneNumber(
                    phoneNumber
                )
    }
}