package com.forrest.titanlauncher.notifications

import com.forrest.titanlauncher.mail.allMailPackages
import android.app.Notification
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.content.Context
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import com.forrest.titanlauncher.settings.LauncherSettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * One message from a MessagingStyle notification. Messaging apps post
 * the recent exchange this way, which is what makes a themed
 * conversation view possible without touching the app's own storage.
 */
data class NotificationMessage(
    val sender: String,
    val text: String,
    val timestamp: Long,
    val fromUser: Boolean
)


data class HubNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val contentIntent: PendingIntent?,
    val autoCancel: Boolean,

    /*
     * Present when the posting app attached a direct-reply action, the
     * same one the system shade uses. Null for notifications that
     * cannot be answered inline.
     */
    val replyAction: Notification.Action? = null,
    val replyRemoteInputKey: String? = null,

    /*
     * Recent messages, when the posting app used MessagingStyle.
     * Empty for ordinary notifications, and never a full history:
     * only what the app chose to put in this notification.
     */
    val messages: List<NotificationMessage> = emptyList()
) {

    val canReply: Boolean
        get() =
            replyAction != null &&
                    !replyRemoteInputKey.isNullOrBlank()
}

object NotificationCenter {

    private val _notifications =
        MutableStateFlow(
            emptyList<HubNotification>()
        )

    val notifications: StateFlow<List<HubNotification>> =
        _notifications.asStateFlow()

    private var listenerService:
            TitanNotificationListenerService? =
        null

    /*
     * These apps already have their own native Prompt Launcher surfaces.
     *
     * We still suppress their ordinary Android notifications, but we do not
     * add them to the generic NOTI section because that would create
     * duplicates.
     */
    private val existingPromptLauncherSources =
        setOf(
            "com.google.android.dialer",
            "com.android.dialer",
            "com.google.android.apps.messaging",
            "com.android.messaging"
        )

    /*
     * Notifications from these packages are left to Android.
     *
     * These are system/security/emergency components rather than ordinary
     * user apps. Prompt Launcher should never silently swallow them.
     */
    private val protectedSystemPackages =
        setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.cellbroadcastreceiver",
            "com.google.android.cellbroadcastreceiver"
        )

    /*
     * The messaging apps are only a duplicate source while Prompt
     * Launcher owns SMS. When another app holds the role, Prompt
     * receives no texts of its own, and that app's notifications are
     * the only way messages can appear here at all — so they have to
     * be let through.
     *
     * Mail and the dialers are deliberately not treated this way;
     * they are separate questions with their own answers.
     */
    /*
     * Installed email apps, refreshed every ten minutes so a newly
     * installed one is picked up without a restart.
     */
    private var cachedMailPackages: Set<String> =
        emptySet()

    private var cachedMailPackagesAt =
        0L

    private fun isMailPackage(
        context: Context,
        packageName: String
    ): Boolean {

        val now =
            System.currentTimeMillis()

        if (
            now - cachedMailPackagesAt >
            10L * 60L * 1000L
        ) {
            cachedMailPackages =
                runCatching {
                    allMailPackages(
                        context
                    )
                }
                    .getOrDefault(
                        cachedMailPackages
                    )

            cachedMailPackagesAt =
                now
        }

        return packageName in
                cachedMailPackages
    }

    private val messagingSources =
        setOf(
            "com.google.android.apps.messaging",
            "com.android.messaging"
        )

    /*
     * Other texting apps people use as their default. Their
     * notifications are treated like Google Messages': left in the
     * shade, and dropped here once the app clears them.
     */
    private val otherMessagingApps =
        setOf(
            "com.samsung.android.messaging",
            "com.textra",
            "com.moez.QKSMS",
            "xyz.klinker.messenger",
            "org.fossify.messages",
            "com.simplemobiletools.smsmessenger",
            "com.verizon.messaging.vzmsgs",
            "com.motorola.messaging",
            "com.oneplus.mms"
        )

    private var cachedDefaultSms: String? =
        null

    private var cachedDefaultSmsAt =
        0L

    /*
     * True for any app that handles texts: the known ones above, plus
     * whatever app is currently the default for SMS.
     */
    private fun isMessagingPackage(
        context: Context,
        packageName: String
    ): Boolean {

        if (
            packageName in messagingSources ||
            packageName in otherMessagingApps
        ) {
            return true
        }

        /*
         * The hub refreshes every second, so the answer is cached for
         * half a minute rather than asking Android each time.
         */
        val now =
            System.currentTimeMillis()

        if (
            now - cachedDefaultSmsAt > 30_000L
        ) {
            cachedDefaultSms =
                runCatching {
                    Telephony.Sms
                        .getDefaultSmsPackage(
                            context
                        )
                }
                    .getOrNull()

            cachedDefaultSmsAt =
                now
        }

        val defaultSms =
            cachedDefaultSms

        return defaultSms != null &&
                defaultSms != context.packageName &&
                defaultSms == packageName
    }

    /*
     * A MessagingStyle notification usually carries only the messages
     * that are currently unread — often one or two. Reading it alone
     * can never show more than the app chose to include.
     *
     * So each conversation's messages are accumulated here as they
     * arrive, keyed by app and conversation title, and merged back in
     * when the notification is rebuilt. That gives a real backlog for
     * a conversation that has been active, while still holding
     * nothing that did not arrive as a notification.
     */
    private const val ConversationHistoryLimit =
        20

    /*
     * Conversations opened inside Prompt Launcher. The notification
     * itself stays posted until the messaging app clears it, so this
     * is what lets a thread stop looking unread once it has been
     * read here.
     *
     * A key is dropped again when a newer message arrives for it, so
     * a fresh message always reads as unread.
     */
    private val _locallyRead =
        MutableStateFlow<Set<String>>(
            emptySet()
        )

    val locallyRead: StateFlow<Set<String>> =
        _locallyRead.asStateFlow()

    fun markConversationRead(
        key: String
    ) {

        _locallyRead.value =
            _locallyRead.value + key
    }

    private fun markConversationUnread(
        key: String
    ) {

        if (
            key in
            _locallyRead.value
        ) {
            _locallyRead.value =
                _locallyRead.value - key
        }
    }

    private val conversationHistory =
        mutableMapOf<String, MutableList<NotificationMessage>>()

    private fun mergeConversationHistory(
        packageName: String,
        title: String,
        incoming: List<NotificationMessage>
    ): List<NotificationMessage> {

        if (
            incoming.isEmpty()
        ) {
            return emptyList()
        }

        val historyKey =
            packageName + "|" + title

        val stored =
            conversationHistory
                .getOrPut(
                    historyKey
                ) {
                    mutableListOf()
                }

        incoming.forEach { message ->

            val alreadyStored =
                stored.any {
                    it.timestamp ==
                            message.timestamp &&
                            it.text ==
                            message.text
                }

            if (
                !alreadyStored
            ) {
                stored.add(
                    message
                )
            }
        }

        stored.sortBy {
            it.timestamp
        }

        while (
            stored.size >
            ConversationHistoryLimit
        ) {
            stored.removeAt(
                0
            )
        }

        return stored.toList()
    }

    private fun isDuplicateOfPromptLauncher(
        context: Context,
        packageName: String
    ): Boolean {

        if (
            packageName !in
            existingPromptLauncherSources
        ) {
            return false
        }

        if (
            packageName in
            messagingSources
        ) {

            val promptOwnsSms =
                runCatching {
                    Telephony.Sms
                        .getDefaultSmsPackage(
                            context
                        ) ==
                            context.packageName
                }
                    .getOrDefault(
                        true
                    )

            return promptOwnsSms
        }

        return true
    }

    internal fun attach(
        service: TitanNotificationListenerService
    ) {
        listenerService =
            service

        service.publishActiveNotifications()
    }

    internal fun detach(
        service: TitanNotificationListenerService
    ) {
        if (
            listenerService ===
            service
        ) {
            listenerService =
                null
        }
    }

    /*
     * Called whenever Android gives Prompt Launcher a notification.
     *
     * The order here matters:
     *
     * 1. Capture it for the Prompt Launcher Hub.
     * 2. Remove the Android notification.
     *
     * That lets Prompt Launcher become the notification surface while still
     * retaining the notification information internally.
     */
    internal fun handleNotificationPosted(
        context: Context,
        service: TitanNotificationListenerService,
        sbn: StatusBarNotification
    ) {
        if (
            !shouldRouteThroughPromptLauncher(
                context =
                    context,
                sbn =
                    sbn
            )
        ) {
            return
        }

        val packageName =
            sbn.packageName
                .orEmpty()

        /*
         * Apps listed as OFF under Settings → Notifications are completely
         * muted.
         *
         * Apps listed as ON are stored in the Hub.
         *
         * Gmail / Messages / Dialer are handled by Prompt Launcher's native
         * Mail, Messages, and Calls surfaces, so they are not duplicated in
         * NOTI.
         */
        val mutedApps =
            LauncherSettingsStore(
                context
            )
                .load()
                .mutedNotificationApps

        val shouldShowInGenericHub =
            !isDuplicateOfPromptLauncher(
                context = context,
                packageName = packageName
            ) &&
                    packageName !in
                    mutedApps

        if (
            shouldShowInGenericHub
        ) {
            buildHubNotification(
                context =
                    context,
                sbn =
                    sbn
            )
                ?.let {
                        notification ->

                    upsertNotification(
                        notification
                    )
                }
        }

        /*
         * Prompt Launcher takes ownership of an alert by cancelling
         * the system copy, so the shade does not show the same thing
         * twice.
         *
         * That is wrong for the app that owns messaging. Cancelling
         * its notification makes Android report the message as
         * handled, which is why a new text appeared for an instant
         * and then vanished from the bolt and the info card while
         * still sitting unread in the messaging app. Its notification
         * is the only record of that message, so it is left alone.
         */
        val ownsMessaging =
            isMessagingPackage(
                context,
                packageName
            )

        /*
         * Email is treated the same way as texts: the email app's
         * notification stays in the shade, so opening it there or
         * in the app keeps read state in step.
         */
        val ownsMail =
            isMailPackage(
                service,
                packageName
            )

        if (
            !ownsMessaging &&
            !ownsMail
        ) {

            runCatching {
                service.cancelNotification(
                    sbn.key
                )
            }
        }
    }

    /*
     * Called when the notification listener first connects or when the Hub
     * requests a refresh.
     *
     * This also cleans up ordinary notifications that were already sitting
     * in Android's notification shade before the listener connected.
     */
    internal fun handleActiveNotifications(
        context: Context,
        service: TitanNotificationListenerService,
        activeNotifications: Array<StatusBarNotification>?
    ) {
        activeNotifications
            ?.forEach {
                    sbn ->

                handleNotificationPosted(
                    context =
                        context,
                    service =
                        service,
                    sbn =
                        sbn
                )
            }

        /*
         * Null means Android could not give us the list right now, so
         * there is nothing safe to compare against.
         */
        if (
            activeNotifications == null
        ) {
            return
        }

        /*
         * READ ELSEWHERE
         *
         * Texting and email apps keep their notification posted only
         * while something is unread, and clear it once you open the
         * conversation. Prompt normally hears that removal, but not
         * if Android had paused the listener at the time. So on every
         * refresh, any text or email Prompt still holds whose
         * notification is gone from the shade has been read, and is
         * dropped from quick reply and the hub.
         *
         * Other apps are skipped: Prompt cancels their system copy on
         * purpose, so their absence from the shade means nothing.
         */
        val stillPosted =
            activeNotifications
                .map {
                    it.key
                }
                .toSet()

        val readElsewhere =
            _notifications
                .value
                .filter { notification ->

                    notification.key !in stillPosted &&
                            (
                                    isMessagingPackage(
                                        context,
                                        notification.packageName
                                    ) ||
                                            isMailPackage(
                                                context,
                                                notification.packageName
                                            )
                                    )
                }
                .map {
                    it.key
                }
                .toSet()

        if (
            readElsewhere.isNotEmpty()
        ) {
            _notifications.value =
                _notifications
                    .value
                    .filterNot {
                        it.key in readElsewhere
                    }
        }
    }

    private fun shouldRouteThroughPromptLauncher(
        context: Context,
        sbn: StatusBarNotification
    ): Boolean {
        val packageName =
            sbn.packageName
                .orEmpty()

        if (
            packageName.isBlank()
        ) {
            return false
        }

        /*
         * Never consume Prompt Launcher's own notifications.
         */
        if (
            packageName ==
            context.packageName
        ) {
            return false
        }

        /*
         * Leave critical Android/system notifications alone.
         */
        if (
            packageName in
            protectedSystemPackages
        ) {
            return false
        }

        /*
         * If Android considers the notification non-clearable, Prompt
         * Launcher should not attempt to take ownership of it.
         *
         * This protects many foreground services and persistent system
         * states automatically.
         */
        if (
            !sbn.isClearable
        ) {
            return false
        }

        val notification =
            sbn.notification
                ?: return false

        /*
         * Explicitly preserve important live/critical notification types.
         */
        if (
            isProtectedNotification(
                notification
            )
        ) {
            return false
        }

        /*
         * Only take over notifications belonging to actual user-facing
         * applications.
         *
         * This prevents Prompt Launcher from swallowing obscure Android
         * background-service notifications.
         */
        val hasLauncherEntry =
            runCatching {
                context
                    .packageManager
                    .getLaunchIntentForPackage(
                        packageName
                    ) != null
            }
                .getOrDefault(
                    false
                )

        if (
            !hasLauncherEntry &&
            !isDuplicateOfPromptLauncher(
                context = context,
                packageName = packageName
            )
        ) {
            return false
        }

        return true
    }

    private fun isProtectedNotification(
        notification: Notification
    ): Boolean {

        /*
         * Persistent / foreground work should remain visible to Android.
         */
        if (
            notification.flags and
            Notification.FLAG_ONGOING_EVENT !=
            0
        ) {
            return true
        }

        if (
            notification.flags and
            Notification.FLAG_FOREGROUND_SERVICE !=
            0
        ) {
            return true
        }

        if (
            notification.flags and
            Notification.FLAG_NO_CLEAR !=
            0
        ) {
            return true
        }

        /*
         * Preserve live functionality where removing the notification could
         * make the phone confusing or unsafe.
         */
        return when (
            notification.category
        ) {

            Notification.CATEGORY_ALARM,
            Notification.CATEGORY_CALL,
            Notification.CATEGORY_NAVIGATION,
            Notification.CATEGORY_TRANSPORT,
            Notification.CATEGORY_SERVICE,
            Notification.CATEGORY_SYSTEM,
            Notification.CATEGORY_STATUS,
            Notification.CATEGORY_PROGRESS ->
                true

            else ->
                false
        }
    }

    private fun buildHubNotification(
        context: Context,
        sbn: StatusBarNotification
    ): HubNotification? {
        val packageName =
            sbn.packageName
                .orEmpty()

        if (
            packageName.isBlank()
        ) {
            return null
        }

        val notification =
            sbn.notification
                ?: return null

        /*
         * Child notifications contain the useful information. Group summary
         * notifications usually just create duplicates such as
         * "3 new notifications."
         */
        if (
            notification.flags and
            Notification.FLAG_GROUP_SUMMARY !=
            0
        ) {
            return null
        }

        val extras =
            notification.extras

        val title =
            extras
                ?.getCharSequence(
                    Notification.EXTRA_TITLE
                )
                ?.toString()
                ?.trim()
                .orEmpty()

        val bigText =
            extras
                ?.getCharSequence(
                    Notification.EXTRA_BIG_TEXT
                )
                ?.toString()
                ?.trim()
                .orEmpty()

        val shortText =
            extras
                ?.getCharSequence(
                    Notification.EXTRA_TEXT
                )
                ?.toString()
                ?.trim()
                .orEmpty()

        val text =
            bigText
                .ifBlank {
                    shortText
                }

        /*
         * MessagingStyle carries the recent exchange as structured
         * messages rather than one flattened line. A null person on a
         * message means the user sent it, which is how the style
         * distinguishes sides.
         */
        val conversation =
            runCatching {

                androidx.core.app.NotificationCompat.MessagingStyle
                    .extractMessagingStyleFromNotification(
                        notification
                    )
                    ?.messages
                    ?.map { message ->

                        NotificationMessage(
                            sender =
                                message
                                    .person
                                    ?.name
                                    ?.toString()
                                    .orEmpty(),
                            text =
                                message
                                    .text
                                    ?.toString()
                                    .orEmpty(),
                            timestamp =
                                message.timestamp,
                            fromUser =
                                message.person == null
                        )
                    }
                    ?.filter {
                        it.text.isNotBlank()
                    }
                    .orEmpty()
            }
                .getOrDefault(
                    emptyList()
                )

        /*
         * Don't populate the Hub with blank framework notifications.
         */
        if (
            title.isBlank() &&
            text.isBlank()
        ) {
            return null
        }

        val appName =
            try {
                val appInfo =
                    context
                        .packageManager
                        .getApplicationInfo(
                            packageName,
                            0
                        )

                context
                    .packageManager
                    .getApplicationLabel(
                        appInfo
                    )
                    .toString()
                    .ifBlank {
                        packageName
                    }

            } catch (
                _: Exception
            ) {
                packageName
            }

        /*
         * The first action carrying a free-form RemoteInput is the
         * direct-reply one. Messaging apps put it first; anything
         * without one simply cannot be replied to from here.
         */
        val replyAction =
            notification
                .actions
                ?.firstOrNull { action ->

                    action
                        .remoteInputs
                        ?.any {
                            it.allowFreeFormInput
                        } == true
                }

        val replyRemoteInput =
            replyAction
                ?.remoteInputs
                ?.firstOrNull {
                    it.allowFreeFormInput
                }

        return HubNotification(
            key =
                sbn.key,
            packageName =
                packageName,
            appName =
                appName,
            title =
                title,
            text =
                text,
            timestamp =
                sbn.postTime,
            contentIntent =
                notification.contentIntent,
            autoCancel =
                notification.flags and
                        Notification.FLAG_AUTO_CANCEL !=
                        0,
            replyAction =
                replyAction,
            replyRemoteInputKey =
                replyRemoteInput
                    ?.resultKey,
            messages =
                mergeConversationHistory(
                    packageName =
                        packageName,
                    title =
                        title,
                    incoming =
                        conversation
                )
        )
    }

    private fun upsertNotification(
        notification: HubNotification
    ) {

        /*
         * A newer message makes the thread unread again, however it
         * was left. Compared by message count, since that is what
         * grows when something arrives.
         */
        val previousCount =
            _notifications
                .value
                .firstOrNull {
                    it.key ==
                            notification.key
                }
                ?.messages
                ?.size
                ?: 0

        if (
            notification.messages.size >
            previousCount
        ) {
            markConversationUnread(
                notification.key
            )
        }

        val updated =
            _notifications
                .value
                .filterNot {
                    it.key ==
                            notification.key
                }
                .plus(
                    notification
                )
                .sortedByDescending {
                    it.timestamp
                }
                .take(
                    80
                )

        _notifications.value =
            updated
    }

    internal fun removeNotification(
        key: String
    ) {
        _notifications.value =
            _notifications
                .value
                .filterNot {
                    it.key ==
                            key
                }
    }

    /*
     * Fires the notification's own direct-reply action with the given
     * text, exactly as the system shade would. Returns false when the
     * notification has no reply action or the app rejects the intent.
     */
    fun sendReply(
        context: Context,
        key: String,
        message: String
    ): Boolean {

        if (
            message.isBlank()
        ) {
            return false
        }

        val notification =
            _notifications
                .value
                .firstOrNull {
                    it.key == key
                }
                ?: return false

        val action =
            notification.replyAction
                ?: return false

        val resultKey =
            notification.replyRemoteInputKey
                ?: return false

        return runCatching {

            val intent =
                Intent()

            val results =
                Bundle()
                    .apply {
                        putCharSequence(
                            resultKey,
                            message
                        )
                    }

            RemoteInput.addResultsToIntent(
                action.remoteInputs,
                intent,
                results
            )

            action
                .actionIntent
                .send(
                    context,
                    0,
                    intent
                )

            true
        }
            .getOrDefault(
                false
            )
    }

    fun requestRefresh() {
        listenerService
            ?.publishActiveNotifications()
    }

    /*
     * Takes a context so the fallback always has one. The listener
     * service is not guaranteed to be alive — Android unbinds it
     * freely — and relying on it meant the fallback silently did
     * nothing exactly when it was needed.
     */
    fun openNotification(
        key: String,
        context: Context? = null
    ) {
        val notification =
            _notifications
                .value
                .firstOrNull {
                    it.key ==
                            key
                }

        if (
            notification == null
        ) {

            android.util.Log.w(
                "TitanNoti",
                "open: key not found " + key
            )

            return
        }

        android.util.Log.d(
            "TitanNoti",
            "open: " + notification.packageName +
                    " hasIntent=" + (notification.contentIntent != null)
        )

        /*
         * The notification's own content intent is the deep link into
         * that conversation, so it is tried first. It can be absent,
         * or cancelled by the posting app between being posted and
         * being tapped, in which case falling back to launching the
         * app is better than doing nothing at all.
         */
        /*
         * From Android 14 the sender of a PendingIntent must opt in
         * before it may start an activity. Without these options the
         * send succeeds silently and nothing appears, which is
         * exactly what a dead "go to full message" looks like.
         */
        val startOptions =
            runCatching {

                android.app.ActivityOptions
                    .makeBasic()
                    .apply {

                        if (
                            android.os.Build.VERSION.SDK_INT >=
                            android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                        ) {
                            setPendingIntentBackgroundActivityStartMode(
                                android.app.ActivityOptions
                                    .MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                            )
                        }
                    }
                    .toBundle()
            }
                .getOrNull()

        val opened =
            runCatching {

                val intent =
                    notification.contentIntent

                if (
                    intent == null
                ) {
                    false
                } else {

                    intent.send(
                        context,
                        0,
                        null,
                        null,
                        null,
                        null,
                        startOptions
                    )

                    true
                }
            }
                .onFailure { error ->

                    android.util.Log.w(
                        "TitanNoti",
                        "open: send failed " + error
                    )
                }
                .getOrDefault(
                    false
                )

        android.util.Log.d(
            "TitanNoti",
            "open: sent=" + opened
        )

        if (
            !opened
        ) {

            runCatching {

                android.util.Log.d(
                    "TitanNoti",
                    "open: falling back to app launch, service=" +
                            (listenerService != null)
                )

                val launchContext =
                    context
                        ?: listenerService

                launchContext
                    ?.packageManager
                    ?.getLaunchIntentForPackage(
                        notification.packageName
                    )
                    ?.apply {

                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                        )
                    }
                    ?.let { intent ->

                        launchContext
                            .startActivity(
                                intent
                            )
                    }
            }
        }

        /*
         * Opening it from Prompt Launcher counts as handling it, so remove
         * the Hub copy.
         */
        removeNotification(
            key
        )

        /*
         * This is harmless if the Android copy has already been removed.
         */
        if (
            notification.autoCancel
        ) {
            runCatching {
                listenerService
                    ?.cancelNotification(
                        key
                    )
            }
        }
    }

    fun dismissNotification(
        key: String
    ) {
        /*
         * Remove any remaining Android copy.
         */
        runCatching {
            listenerService
                ?.cancelNotification(
                    key
                )
        }

        /*
         * Most importantly, remove Prompt Launcher's retained Hub copy.
         */
        removeNotification(
            key
        )
    }
}

class TitanNotificationListenerService :
    NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()

        NotificationCenter
            .attach(
                this
            )
    }

    override fun onListenerDisconnected() {
        NotificationCenter
            .detach(
                this
            )

        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(
        sbn: StatusBarNotification?
    ) {
        if (
            sbn == null
        ) {
            return
        }

        NotificationCenter
            .handleNotificationPosted(
                context =
                    this,
                service =
                    this,
                sbn =
                    sbn
            )
    }

    /*
     * We intentionally do NOT automatically remove the Hub copy here.
     *
     * Prompt Launcher itself is normally the reason the Android copy was
     * dismissed, so removing it here would make the notification vanish
     * from the Hub immediately after capture.
     */
    /*
     * Messaging apps clear their own notification once the
     * conversation has been read elsewhere, so following removals is
     * what keeps Prompt Launcher in step with them: read a text in
     * Google Messages and it stops showing as waiting here.
     *
     * This was previously ignored so an alert stayed until it was
     * handled inside Prompt Launcher. That made sense when Prompt
     * owned messaging; with another app holding the role it just
     * means the list shows things already dealt with.
     */
    override fun onNotificationRemoved(
        sbn: StatusBarNotification?
    ) {
        /*
         * Handled by the overload below, which says why it went. The
         * reason matters: apps cancel and repost a notification to
         * update it, and treating that as "the user dealt with it"
         * made new messages appear and vanish immediately.
         */
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int
    ) {

        val key =
            sbn
                ?.key
                ?: return

        /*
         * Only removals that mean the alert was actually dealt with.
         * REASON_APP_CANCEL is the one that matters for messaging:
         * it is what an app sends when the conversation has been read
         * elsewhere. Updates, group summary churn and channel changes
         * are deliberately ignored.
         */
        val handled =
            reason ==
                    REASON_CLICK ||
                    reason ==
                    REASON_CANCEL ||
                    reason ==
                    REASON_CANCEL_ALL ||
                    reason ==
                    REASON_APP_CANCEL ||
                    reason ==
                    REASON_APP_CANCEL_ALL ||
                    reason ==
                    REASON_LISTENER_CANCEL ||
                    reason ==
                    REASON_LISTENER_CANCEL_ALL

        android.util.Log.d(
            "TitanNoti",
            "removed reason=" + reason +
                    " handled=" + handled +
                    " key=" + key
        )

        if (
            !handled
        ) {
            return
        }

        NotificationCenter
            .removeNotification(
                key
            )
    }

    internal fun publishActiveNotifications() {
        NotificationCenter
            .handleActiveNotifications(
                context =
                    this,
                service =
                    this,
                activeNotifications =
                    runCatching {
                        activeNotifications
                    }
                        .getOrNull()
            )
    }
}