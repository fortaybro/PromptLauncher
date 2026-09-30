package com.forrest.titanlauncher

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.LauncherApps
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Drafts
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.forrest.titanlauncher.ai.GeminiApiKeyStore
import com.forrest.titanlauncher.ai.GeminiRepository
import com.forrest.titanlauncher.calendar.CalendarCommandParser
import com.forrest.titanlauncher.calendar.CalendarEventDraft
import com.forrest.titanlauncher.calendar.CalendarRepository
import com.forrest.titanlauncher.calendar.CalendarSelectionStore
import com.forrest.titanlauncher.calendar.CalendarTarget
import com.forrest.titanlauncher.calendar.GoogleCalendarAuthManager
import com.forrest.titanlauncher.calendar.GoogleCalendarOption
import com.forrest.titanlauncher.calls.CallLogEntry
import com.forrest.titanlauncher.calls.CallLogRepository
import com.forrest.titanlauncher.calls.CallType
import com.forrest.titanlauncher.commands.Command
import com.forrest.titanlauncher.commands.CommandParser
import com.forrest.titanlauncher.contacts.Contact
import com.forrest.titanlauncher.contacts.ContactRepository
import com.forrest.titanlauncher.contacts.EmailContact
import com.forrest.titanlauncher.messages.SmsDatabase
import com.forrest.titanlauncher.messages.SmsMessage
import com.forrest.titanlauncher.messages.SmsRoleManager
import com.forrest.titanlauncher.messages.SmsSender
import com.forrest.titanlauncher.mail.GmailRepository
import com.forrest.titanlauncher.mail.GoogleMailAuthManager
import com.forrest.titanlauncher.mail.MailMessage
import com.forrest.titanlauncher.mail.allMailPackages
import com.forrest.titanlauncher.notes.NoteCategory
import com.forrest.titanlauncher.notes.NoteItem
import com.forrest.titanlauncher.notes.NoteStyleRange
import com.forrest.titanlauncher.notes.NoteTextStyle
import com.forrest.titanlauncher.notes.NoteStore
import com.forrest.titanlauncher.notifications.HubNotification
import com.forrest.titanlauncher.notifications.NotificationCenter
import com.forrest.titanlauncher.settings.CommandSurfaceTone
import com.forrest.titanlauncher.settings.HomeCornerStyle
import com.forrest.titanlauncher.settings.HomeElementSize
import com.forrest.titanlauncher.settings.HomeSurfaceTone
import com.forrest.titanlauncher.settings.LauncherAccent
import com.forrest.titanlauncher.settings.LauncherInterfaceFont
import com.forrest.titanlauncher.settings.LauncherSettings
import com.forrest.titanlauncher.settings.LauncherSettingsStore
import com.forrest.titanlauncher.settings.ReadabilityContrast
import com.forrest.titanlauncher.settings.ReadabilityMotion
import com.forrest.titanlauncher.settings.ReadabilityTextSize
import com.forrest.titanlauncher.settings.ReadabilityTextWeight
import com.forrest.titanlauncher.settings.LauncherThemeMode
import com.forrest.titanlauncher.todoist.TodoistRepository
import com.forrest.titanlauncher.todoist.TodoistTokenStore
import com.forrest.titanlauncher.usage.CurrentHourUsageDiagnostics
import com.forrest.titanlauncher.usage.HourProductivityStatus
import com.forrest.titanlauncher.usage.UsageStatsRepository
import com.forrest.titanlauncher.weather.WeatherRepository
import com.forrest.titanlauncher.weather.WeatherSnapshot
import com.forrest.titanlauncher.ui.theme.TitanLauncherTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun HubScreen(
    smsDatabase: SmsDatabase,
    contactRepository: ContactRepository,
    hasContactsPermission: Boolean,
    mailMessages: List<MailMessage>,
    callLogEntries: List<CallLogEntry>,
    mutedNotificationApps: Set<String>,
    hasCallLogPermission: Boolean,
    selectedCategory: HubCategory,
    onSelectedCategoryChange: (HubCategory) -> Unit,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    onRequestCallLogPermission: () -> Unit,
    onOpenConversation: (Contact) -> Unit,
    onOpenThread: (ConversationThread, Contact) -> Unit = { _, _ -> },
    onOpenMail: (MailMessage) -> Unit,
    onDial: (String) -> Unit,
    mailNeedsAuthorization: Boolean = false,
    onConnectMail: () -> Unit = {},
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    /*
     * Set when a call row is opened. Drives the call action sheet.
     */
    var callActionsItem by remember {
        mutableStateOf<HubDisplayItem?>(null)
    }

    val hubClearedStore =
        remember {
            HubClearedStore(
                context
            )
        }

    var clearedIds by remember {
        mutableStateOf(
            hubClearedStore.load()
        )
    }

    val appNotifications by
    NotificationCenter
        .notifications
        .collectAsStateWithLifecycle()

    var notificationAccessEnabled by remember {
        mutableStateOf(
            hasNotificationListenerAccess(
                context
            )
        )
    }

    val focusRequester =
        remember {
            FocusRequester()
        }

    val listState =
        rememberLazyListState()

    LaunchedEffect(Unit) {
        while (
            true
        ) {
            notificationAccessEnabled =
                hasNotificationListenerAccess(
                    context
                )

            if (
                notificationAccessEnabled
            ) {
                NotificationCenter
                    .requestRefresh()
            }

            delay(
                1000
            )
        }
    }

    val allMessages by
    smsDatabase
        .smsDao()
        .observeAllMessages()
        .collectAsStateWithLifecycle(
            initialValue =
                emptyList()
        )

    val hubItems by
    produceState(
        initialValue =
            emptyList<HubDisplayItem>(),
        key1 =
            allMessages,
        key2 =
            mailMessages,
        key3 =
            listOf(
                callLogEntries,
                hasContactsPermission,
                appNotifications,
                mutedNotificationApps
            )
    ) {

        value =
            withContext(
                Dispatchers.IO
            ) {

                val ownNumber =
                    OwnNumberStore(
                        context
                    )
                        .resolve(
                            allMessages
                        )

                val messageItems =
                    allMessages
                        .groupBy {

                            /*
                             * Grouped by thread, so a group shows as
                             * one row rather than separate entries
                             * from each member. Rows written before
                             * the migration fall back to their number.
                             */
                            it.threadKey
                                .ifBlank {
                                    normalizePhoneNumber(
                                        it.phoneNumber
                                    )
                                }
                        }
                        .values
                        .mapNotNull {
                                thread ->

                            val latest =
                                thread
                                    .maxByOrNull {
                                        it.id
                                    }
                                    ?: return@mapNotNull null

                            val contactName =
                                if (
                                    hasContactsPermission
                                ) {

                                    contactRepository
                                        .findContactNameByPhoneNumber(
                                            latest.phoneNumber
                                        )

                                } else {

                                    null
                                }

                            val contactPhotoUri =
                                if (
                                    hasContactsPermission
                                ) {

                                    contactRepository
                                        .findContactPhotoUriByPhoneNumber(
                                            latest.phoneNumber
                                        )

                                } else {

                                    null
                                }

                            val isGroup =
                                thread.any {
                                    it.isGroup
                                }

                            val threadKey =
                                latest.threadKey
                                    .ifBlank {
                                        normalizePhoneNumber(
                                            latest.phoneNumber
                                        )
                                    }

                            /*
                             * A group is named by its members, the
                             * same way the messages inbox names it.
                             */
                            val title =
                                if (
                                    isGroup
                                ) {

                                    groupTitleFor(
                                        participants =
                                            threadKey
                                                .split(",")
                                                .filter {
                                                    it.isNotBlank()
                                                },
                                        ownNumber =
                                            ownNumber
                                    ) { number ->

                                        if (
                                            hasContactsPermission
                                        ) {
                                            contactRepository
                                                .findContactNameByPhoneNumber(
                                                    number
                                                )
                                        } else {
                                            null
                                        }
                                    }

                                } else {

                                    contactName
                                        ?: latest.phoneNumber
                                }

                            HubDisplayItem(
                                id =
                                    "sms:" + threadKey + ":" + latest.id,
                                type =
                                    HubItemType.MESSAGE,
                                title =
                                    title,
                                preview =
                                    if (
                                        latest.incoming
                                    ) {
                                        latest.body
                                    } else {
                                        "you: ${latest.body}"
                                    },
                                timestamp =
                                    latest.timestamp,
                                unread =
                                    thread.any {
                                            message ->

                                        message.incoming &&
                                                !message.isRead
                                    },
                                phoneNumber =
                                    latest.phoneNumber,
                                photoUri =
                                    if (
                                        isGroup
                                    ) {
                                        null
                                    } else {
                                        contactPhotoUri
                                    },
                                threadKey =
                                    threadKey,
                                isGroup =
                                    isGroup
                            )
                        }

                /*
                 * Email now works like texting: rows come from the
                 * email apps' own notifications (below), and tapping
                 * one opens it in that app. Gmail read through the API
                 * is no longer listed here, which would only duplicate
                 * Gmail's notifications.
                 */
                val emailItems =
                    emptyList<MailMessage>().map {
                            message ->

                        HubDisplayItem(
                            id =
                                "mail:${message.id}",
                            type =
                                HubItemType.EMAIL,
                            title =
                                message.sender,
                            preview =
                                message.subject
                                    .ifBlank {
                                        message.snippet
                                    },
                            timestamp =
                                message.timestamp,
                            unread =
                                message.unread,
                            mailMessage =
                                message
                        )
                    }

                val callItems =
                    callLogEntries.map {
                            call ->

                        val contactName =
                            if (
                                hasContactsPermission
                            ) {

                                contactRepository
                                    .findContactNameByPhoneNumber(
                                        call.phoneNumber
                                    )

                            } else {

                                null
                            }

                        val contactPhotoUri =
                            if (
                                hasContactsPermission
                            ) {

                                contactRepository
                                    .findContactPhotoUriByPhoneNumber(
                                        call.phoneNumber
                                    )

                            } else {

                                null
                            }

                        val callLabel =
                            when (
                                call.type
                            ) {

                                CallType.MISSED ->
                                    "missed call"

                                CallType.INCOMING ->
                                    if (
                                        call.durationSeconds > 0
                                    ) {
                                        "incoming · ${formatCallDuration(call.durationSeconds)}"
                                    } else {
                                        "incoming call"
                                    }

                                CallType.OUTGOING ->
                                    if (
                                        call.durationSeconds > 0
                                    ) {
                                        "outgoing · ${formatCallDuration(call.durationSeconds)}"
                                    } else {
                                        "outgoing call"
                                    }

                                CallType.REJECTED ->
                                    "declined call"

                                CallType.OTHER ->
                                    "call"
                            }

                        HubDisplayItem(
                            id =
                                "call:${call.id}",
                            type =
                                HubItemType.CALL,
                            title =
                                contactName
                                    ?: call.phoneNumber,
                            preview =
                                callLabel,
                            timestamp =
                                call.timestamp,
                            missed =
                                call.type ==
                                        CallType.MISSED,
                            phoneNumber =
                                call.phoneNumber,
                            photoUri =
                                contactPhotoUri
                        )
                    }

                /*
                 * Spark, Samsung Email, Outlook and other mail apps:
                 * their notifications are filed as email, not noti.
                 */
                val mailPackages =
                    if (
                        EnabledMailApps.isNotEmpty()
                    ) {
                        EnabledMailApps.toSet()
                    } else {
                        allMailPackages(
                            context
                        )
                    }

                val notificationItems =
                    appNotifications
                        .filter { notification ->
                            notification.packageName !in
                                    mutedNotificationApps
                        }
                        .map {
                                notification ->

                            val preview =
                                listOf(
                                    notification.title,
                                    notification.text
                                )
                                    .filter {
                                        it.isNotBlank()
                                    }
                                    .distinct()
                                    .joinToString(
                                        separator = " · "
                                    )

                            /*
                             * A notification carrying MessagingStyle
                             * messages is a conversation, so it is
                             * filed under messages with the @ glyph
                             * rather than sitting in noti.
                             */
                            val isConversation =
                                notification.messages.isNotEmpty()

                            val newestMessage =
                                notification
                                    .messages
                                    .lastOrNull()

                            val isMailApp =
                                !isConversation &&
                                        notification.packageName in
                                        mailPackages

                            if (
                                isMailApp
                            ) {
                                /*
                                 * Sender as the title, the app name
                                 * and subject/preview underneath.
                                 */
                                return@map HubDisplayItem(
                                    id =
                                        "notification:${notification.key}",
                                    type =
                                        HubItemType.EMAIL,
                                    title =
                                        notification.title
                                            .ifBlank {
                                                notification.appName
                                            },
                                    preview =
                                        listOf(
                                            notification.appName,
                                            notification.text
                                        )
                                            .filter {
                                                it.isNotBlank()
                                            }
                                            .joinToString(
                                                separator = " · "
                                            ),
                                    timestamp =
                                        notification.timestamp,
                                    unread =
                                        true,
                                    notificationKey =
                                        notification.key
                                )
                            }

                            HubDisplayItem(
                                id =
                                    "notification:${notification.key}",
                                type =
                                    if (
                                        isConversation
                                    ) {
                                        HubItemType.MESSAGE
                                    } else {
                                        HubItemType.NOTIFICATION
                                    },
                                title =
                                    if (
                                        isConversation &&
                                        notification.title.isNotBlank()
                                    ) {
                                        notification.title
                                    } else {
                                        notification.appName
                                    },
                                preview =
                                    if (
                                        isConversation &&
                                        newestMessage != null
                                    ) {

                                        if (
                                            newestMessage.fromUser
                                        ) {
                                            "you: " + newestMessage.text
                                        } else {
                                            newestMessage.text
                                        }

                                    } else {
                                        preview
                                    },
                                timestamp =
                                    notification.timestamp,
                                unread =
                                    true,
                                notificationKey =
                                    notification.key
                            )
                        }

                (
                        messageItems +
                                emailItems +
                                callItems +
                                notificationItems
                        )
                    .sortedByDescending {
                        it.timestamp
                    }
                    .take(
                        60
                    )
            }
    }

    /*
     * Dismissed rows are removed before anything else, so the category
     * counts and the empty state agree with what is on screen.
     */
    val visibleItems =
        hubItems.filterNot {
            it.id in clearedIds
        }

    fun clearItems(
        items: List<HubDisplayItem>
    ) {

        /*
         * Notifications are real system state, so clearing one here
         * dismisses it for the device too rather than only hiding it.
         */
        items.forEach { item ->

            if (
                item.type ==
                HubItemType.NOTIFICATION ||
                item.type ==
                HubItemType.EMAIL
            ) {

                item.notificationKey
                    ?.let {
                        NotificationCenter
                            .dismissNotification(
                                it
                            )
                    }
            }
        }

        clearedIds =
            hubClearedStore.clear(
                items.map {
                    it.id
                }
            )
    }

    val filteredItems =
        when (
            selectedCategory
        ) {

            HubCategory.ALL ->
                visibleItems

            HubCategory.MESSAGES ->
                visibleItems.filter {
                    it.type ==
                            HubItemType.MESSAGE
                }

            HubCategory.CALLS ->
                visibleItems.filter {
                    it.type ==
                            HubItemType.CALL
                }

            HubCategory.EMAIL ->
                visibleItems.filter {
                    it.type ==
                            HubItemType.EMAIL
                }

            HubCategory.NOTIFICATIONS ->
                visibleItems.filter {
                    it.type ==
                            HubItemType.NOTIFICATION
                }
        }

    val newCount =
        hubItems.count {
            it.unread
        }

    LaunchedEffect(Unit) {

        focusRequester
            .requestFocus()
    }

    LaunchedEffect(
        selectedCategory,
        filteredItems.size
    ) {

        onSelectedIndexChange(
            selectedIndex
                .coerceIn(
                    0,
                    filteredItems
                        .lastIndex
                        .coerceAtLeast(
                            0
                        )
                )
        )
    }

    LaunchedEffect(
        selectedIndex,
        filteredItems.size
    ) {

        if (
            filteredItems.isNotEmpty()
        ) {

            listState
                .animateScrollToItem(
                    selectedIndex
                )
        }
    }

    fun openItem(
        item: HubDisplayItem
    ) {

        when (
            item.type
        ) {

            HubItemType.MESSAGE -> {

                /*
                 * A text that arrived as a notification lives in the
                 * messaging app, so tapping it goes there rather than
                 * opening a copy inside the launcher.
                 */
                val notificationKey =
                    item.notificationKey

                if (
                    notificationKey != null
                ) {

                    NotificationCenter
                        .openNotification(
                            key =
                                notificationKey,
                            context =
                                context
                        )

                    return
                }

                val phoneNumber =
                    item.phoneNumber
                        ?: return

                val contact =
                    Contact(
                        name =
                            item.title,
                        phoneNumber =
                            phoneNumber
                    )

                /*
                 * When another app owns SMS, open the conversation
                 * there. For a group, every participant except this
                 * phone goes into the recipient list.
                 */
                val handedOff =
                    if (
                        item.isGroup
                    ) {
                        val ownNumber =
                            OwnNumberStore(
                                context
                            )
                                .get()
                                ?.let {
                                    normalizePhoneNumber(
                                        it
                                    )
                                }

                        val others =
                            item.threadKey
                                .split(",")
                                .map {
                                    it.trim()
                                }
                                .filter {
                                    it.isNotBlank() &&
                                            normalizePhoneNumber(
                                                it
                                            ) != ownNumber
                                }

                        others.isNotEmpty() &&
                                openThreadInMessagingAppIfNotDefault(
                                    context,
                                    others.joinToString(
                                        ";"
                                    )
                                )
                    } else {
                        openThreadInMessagingAppIfNotDefault(
                            context,
                            phoneNumber
                        )
                    }

                if (
                    handedOff
                ) {
                    return
                }

                /*
                 * A group carries its whole participant list through,
                 * so the conversation screen can show who said what
                 * and reply to everyone.
                 */
                if (
                    item.isGroup
                ) {

                    onOpenThread(
                        ConversationThread(
                            threadKey =
                                item.threadKey,
                            title =
                                item.title,
                            participants =
                                item.threadKey
                                    .split(",")
                                    .filter {
                                        it.isNotBlank()
                                    },
                            isGroup =
                                true
                        ),
                        contact
                    )

                } else {

                    onOpenConversation(
                        contact
                    )
                }
            }

            HubItemType.EMAIL -> {

                val mailMessage =
                    item.mailMessage

                if (
                    mailMessage != null
                ) {
                    onOpenMail(
                        mailMessage
                    )
                } else {
                    /*
                     * An email from another mail app: open it there.
                     */
                    item.notificationKey
                        ?.let {
                            NotificationCenter
                                .openNotification(
                                    key =
                                        it,
                                    context =
                                        context
                                )
                        }
                }
            }

            HubItemType.CALL -> {

                /*
                 * Calls open an action sheet rather than dialling
                 * straight away, so a missed call can be answered with
                 * a text just as easily as a callback.
                 */
                if (
                    item.phoneNumber != null
                ) {
                    callActionsItem =
                        item
                }
            }

            HubItemType.NOTIFICATION -> {

                item.notificationKey
                    ?.let {
                        NotificationCenter
                            .openNotification(
                                it
                            )
                    }
            }
        }
    }

    /*
     * Used by the keyboard path, where the selected index is already
     * settled. Clicks pass their own item instead, because calling
     * onSelectedIndexChange and then reading selectedIndex in the same
     * lambda reads the previous value — state has not recomposed yet.
     * That was opening whichever row had been selected before rather
     * than the one just tapped.
     */
    fun openSelectedItem() {

        filteredItems
            .getOrNull(
                selectedIndex
            )
            ?.let {
                openItem(
                    it
                )
            }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        BackgroundBlack
                    )
                    .padding(
                        horizontal =
                            10.dp,
                        vertical =
                            7.dp
                    )
                    .focusRequester(
                        focusRequester
                    )
                    .focusable()
                    .onPreviewKeyEvent {
                            event ->

                        if (
                            event.type !=
                            KeyEventType.KeyDown
                        ) {

                            false

                        } else {

                            when (
                                event.key
                            ) {

                                Key.DirectionDown -> {

                                    if (
                                        filteredItems.isNotEmpty()
                                    ) {

                                        onSelectedIndexChange(
                                            (
                                                    selectedIndex + 1
                                                    )
                                                .coerceAtMost(
                                                    filteredItems.lastIndex
                                                )
                                        )
                                    }

                                    true
                                }

                                Key.DirectionUp -> {

                                    onSelectedIndexChange(
                                        (
                                                selectedIndex - 1
                                                )
                                            .coerceAtLeast(
                                                0
                                            )
                                    )

                                    true
                                }

                                Key.DirectionRight -> {

                                    val categories =
                                        HubCategory.entries

                                    val index =
                                        categories.indexOf(
                                            selectedCategory
                                        )

                                    onSelectedCategoryChange(
                                        categories[
                                            (
                                                    index + 1
                                                    )
                                                .coerceAtMost(
                                                    categories.lastIndex
                                                )
                                        ]
                                    )

                                    onSelectedIndexChange(
                                        0
                                    )

                                    true
                                }

                                Key.DirectionLeft -> {

                                    val categories =
                                        HubCategory.entries

                                    val index =
                                        categories.indexOf(
                                            selectedCategory
                                        )

                                    onSelectedCategoryChange(
                                        categories[
                                            (
                                                    index - 1
                                                    )
                                                .coerceAtLeast(
                                                    0
                                                )
                                        ]
                                    )

                                    onSelectedIndexChange(
                                        0
                                    )

                                    true
                                }

                                Key.Enter -> {

                                    openSelectedItem()
                                    true
                                }

                                Key.Escape -> {

                                    onBack()
                                    true
                                }

                                else ->
                                    false
                            }
                        }
                    }
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = CameraSafeStartPadding),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text =
                        "hub",
                    color =
                        PrimaryText,
                    fontSize =
                        34.sp,
                    fontFamily =
                        InterfaceFont,
                    fontWeight =
                        FontWeight.Bold,
                    textDecoration =
                        TextDecoration.Underline
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            8.dp
                        )
                )

                if (
                    newCount > 0
                ) {
                    Text(
                        text =
                            "$newCount new",
                        color =
                            AccentOrange,
                        fontSize =
                            8.sp,
                        fontFamily =
                            InterfaceFont
                    )
                }

                Spacer(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                )

                /*
                 * Clears whatever the current tab is showing rather
                 * than the whole hub, so it is predictable from
                 * wherever you happen to be.
                 */
                if (
                    filteredItems.isNotEmpty()
                ) {

                    Text(
                        text =
                            "clear all",
                        color =
                            SecondaryText,
                        fontSize =
                            8.5.sp,
                        fontFamily =
                            InterfaceFont,
                        modifier =
                            Modifier
                                .clickable {
                                    clearItems(
                                        filteredItems
                                    )
                                }
                                .padding(
                                    horizontal = 4.dp,
                                    vertical = 3.dp
                                )
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                HubCategory.entries.forEach {
                        category ->

                    val selected =
                        category ==
                                selectedCategory

                    val categoryLabel =
                        when (
                            category
                        ) {

                            HubCategory.ALL ->
                                "all"

                            HubCategory.MESSAGES ->
                                "messages"

                            HubCategory.CALLS ->
                                "calls"

                            HubCategory.EMAIL ->
                                "email"

                            HubCategory.NOTIFICATIONS ->
                                "noti"
                        }

                    Box(
                        modifier =
                            Modifier.clickable {

                                onSelectedCategoryChange(
                                    category
                                )

                                onSelectedIndexChange(
                                    0
                                )
                            },
                        contentAlignment =
                            Alignment.Center
                    ) {
                        Text(
                            text =
                                categoryLabel,
                            color =
                                if (selected) AccentOrange
                                else TertiaryText,
                            fontSize =
                                9.5.sp,
                            lineHeight =
                                9.8.sp,
                            fontFamily =
                                InterfaceFont,
                            fontWeight =
                                if (selected) FontWeight.Bold
                                else FontWeight.Normal
                        )

                        if (selected) {
                            Box(
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = 3.dp)
                                        .height(1.dp)
                                        .width(22.dp)
                                        .background(AccentOrange)
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )

            MinimalDivider()

            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )

            if (
                !notificationAccessEnabled
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                34.dp
                            )
                            .background(
                                SurfaceBlack,
                                RoundedCornerShape(
                                    8.dp
                                )
                            )
                            .border(
                                0.75.dp,
                                BorderGray,
                                RoundedCornerShape(
                                    8.dp
                                )
                            )
                            .clickable {
                                try {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
                                        )
                                    )
                                } catch (
                                    _: Exception
                                ) {
                                }
                            }
                            .padding(
                                horizontal =
                                    8.dp
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "noti hidden · tap to allow notification access",
                        color =
                            AccentOrange,
                        fontSize =
                            7.sp,
                        fontFamily =
                            InterfaceFont
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )
            }

            if (
                !hasCallLogPermission
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                34.dp
                            )
                            .background(
                                SurfaceBlack,
                                RoundedCornerShape(
                                    8.dp
                                )
                            )
                            .border(
                                0.75.dp,
                                BorderGray,
                                RoundedCornerShape(
                                    8.dp
                                )
                            )
                            .clickable {

                                onRequestCallLogPermission()
                            }
                            .padding(
                                horizontal =
                                    8.dp
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "calls hidden · tap to allow call history",
                        color =
                            AccentOrange,
                        fontSize =
                            7.sp,
                        fontFamily =
                            InterfaceFont
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )
            }

            if (
                filteredItems.isEmpty()
            ) {

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(
                                1f
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {

                    /*
                     * An empty email list usually means mail was never
                     * authorized rather than that the inbox is clear,
                     * so say which and offer the way out.
                     */
                    val mailTabNeedsConnecting =
                        mailNeedsAuthorization &&
                                (
                                        selectedCategory ==
                                                HubCategory.EMAIL ||
                                                selectedCategory ==
                                                HubCategory.ALL
                                        )

                    if (
                        mailTabNeedsConnecting
                    ) {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text =
                                    "mail not connected",
                                color =
                                    TertiaryText,
                                fontSize =
                                    9.sp,
                                fontFamily =
                                    InterfaceFont
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(
                                        7.dp
                                    )
                            )

                            Text(
                                text =
                                    "sign in",
                                color =
                                    AccentOrange,
                                fontSize =
                                    10.sp,
                                fontFamily =
                                    InterfaceFont,
                                fontWeight =
                                    FontWeight.Medium,
                                modifier =
                                    Modifier
                                        .border(
                                            0.75.dp,
                                            AccentOrange,
                                            RoundedCornerShape(
                                                8.dp
                                            )
                                        )
                                        .clickable {
                                            onConnectMail()
                                        }
                                        .padding(
                                            horizontal = 12.dp,
                                            vertical = 5.dp
                                        )
                            )
                        }

                    } else {

                        Text(
                            text =
                                "nothing here",
                            color =
                                TertiaryText,
                            fontSize =
                                9.sp,
                            fontFamily =
                                InterfaceFont
                        )
                    }
                }

            } else {

                LazyColumn(
                    state =
                        listState,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(
                                1f
                            ),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            2.dp
                        )
                ) {

                    itemsIndexed(
                        items =
                            filteredItems,
                        key = {
                                _,
                                item ->

                            item.id
                        }
                    ) {
                            index,
                            item ->

                        HubRow(
                            item =
                                item,
                            selected =
                                index ==
                                        selectedIndex,
                            onClick = {

                                onSelectedIndexChange(
                                    index
                                )

                                openItem(
                                    item
                                )
                            },
                            onDismiss = {
                                clearItems(
                                    listOf(
                                        item
                                    )
                                )
                            }
                        )
                    }
                }
            }

        }

        val activeCallItem =
            callActionsItem

        if (
            activeCallItem?.phoneNumber != null
        ) {

            CallActionsOverlay(
                displayName =
                    activeCallItem.title,
                phoneNumber =
                    activeCallItem.phoneNumber,
                onText = {

                    callActionsItem =
                        null

                    /*
                     * When another app owns SMS, reply in that app's
                     * thread; Prompt's own thread is only used when
                     * Prompt is the default SMS app.
                     */
                    if (
                        !openThreadInMessagingAppIfNotDefault(
                            context,
                            activeCallItem.phoneNumber
                        )
                    ) {
                        onOpenConversation(
                            Contact(
                                name =
                                    activeCallItem.title,
                                phoneNumber =
                                    activeCallItem.phoneNumber
                            )
                        )
                    }
                },
                onCall = {

                    callActionsItem =
                        null

                    onDial(
                        activeCallItem.phoneNumber
                    )
                },
                onDismiss = {
                    callActionsItem =
                        null
                }
            )
        }
    }
}

/*
 * Decoded contact thumbnails, keyed by photo URI.
 *
 * The hub rebuilds its item list whenever messages, calls or
 * notifications change, so without this the same faces would be
 * decoded from the content provider on every pass and again on every
 * scroll. Thumbnails are small and the number of correspondents is
 * bounded, so holding them for the process lifetime is cheap.
 */
/*
 * Diagnostic for contact photos. With this on, message and call rows
 * lacking an avatar show "n" when no photo URI resolved and "d" when
 * a URI resolved but the image would not decode. Left in place
 * because it costs nothing off and answers the question instantly.
 */
private const val DebugContactPhotos =
    false

private val contactPhotoCache =
    mutableMapOf<String, ImageBitmap?>()

@Composable
internal fun rememberContactPhoto(
    photoUri: String?
): ImageBitmap? {

    val context =
        LocalContext.current

    if (
        photoUri.isNullOrBlank()
    ) {
        return null
    }

    return remember(
        photoUri
    ) {

        contactPhotoCache.getOrPut(
            photoUri
        ) {

            runCatching {

                context.contentResolver
                    .openInputStream(
                        Uri.parse(
                            photoUri
                        )
                    )
                    ?.use { stream ->

                        BitmapFactory
                            .decodeStream(
                                stream
                            )
                            ?.asImageBitmap()
                    }
            }
                .getOrNull()
        }
    }
}

@Composable
internal fun HubRow(
    item: HubDisplayItem,
    selected: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .background(
                if (selected) SurfaceBlack else BackgroundBlack,
                RoundedCornerShape(8.dp)
            )
            .border(
                width = if (selected) 0.75.dp else 0.dp,
                color = if (selected) BorderGray else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable {
                onClick()
            }
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        /*
         * Leading indicator.
         *
         * Mail and calls use vector icons, messages and notifications
         * use glyphs that carry unread state through weight. Both sit
         * in a fixed width box so every row's text starts at the same
         * x position regardless of which kind it is.
         */
        val indicatorColor =
            if (item.unread || item.missed) AccentOrange else SecondaryText

        val contactPhoto =
            rememberContactPhoto(
                item.photoUri
            )

        Box(
            modifier = Modifier.width(14.dp),
            contentAlignment = Alignment.Center
        ) {
            if (contactPhoto != null) {

                /*
                 * A saved contact's picture stands in for the glyph.
                 * Unread and missed rows get an accent ring, since the
                 * photo cannot carry weight the way @ and * do.
                 */
                Image(
                    bitmap = contactPhoto,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (item.unread || item.missed) 1.dp else 0.dp,
                            color = if (item.unread || item.missed) AccentOrange else Color.Transparent,
                            shape = CircleShape
                        )
                )

            } else if (
                DebugContactPhotos &&
                (item.type == HubItemType.MESSAGE ||
                        item.type == HubItemType.CALL)
            ) {

                Text(
                    text =
                        if (item.photoUri.isNullOrBlank()) "n" else "d",
                    color = AccentOrange,
                    fontSize = 11.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = FontWeight.Bold
                )

            } else when (item.type) {

                HubItemType.EMAIL ->
                    Icon(
                        imageVector =
                            if (item.unread) {
                                Icons.Outlined.Email
                            } else {
                                Icons.Outlined.Drafts
                            },
                        contentDescription =
                            if (item.unread) "unread email" else "read email",
                        tint = indicatorColor,
                        modifier = Modifier.size(11.dp)
                    )

                HubItemType.CALL ->
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = "call",
                        tint = indicatorColor,
                        modifier = Modifier.size(11.dp)
                    )

                HubItemType.MESSAGE ->
                    Text(
                        text = "@",
                        color = indicatorColor,
                        fontSize = 11.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = if (item.unread) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )

                HubItemType.NOTIFICATION ->
                    Text(
                        text = "*",
                        color = indicatorColor,
                        fontSize = 13.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = if (item.unread) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        }
                    )
            }
        }

        Spacer(Modifier.width(7.dp))

        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text =
                        if (
                            item.type == HubItemType.MESSAGE ||
                            item.type == HubItemType.CALL
                        ) {
                            item.title.lowercase()
                        } else {
                            item.title
                        },
                    color = PrimaryText,
                    fontSize = 8.5.sp,
                    lineHeight = 8.5.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = if (item.unread || item.missed) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatInboxTime(item.timestamp),
                    color = TertiaryText,
                    fontSize = 6.sp,
                    fontFamily = InterfaceFont
                )

            }

            /*
             * Both line heights already match their font sizes, so
             * the space that was left is the font's own built-in
             * leading, which lineHeight does not remove. Lifting the
             * preview closes it without touching either line box.
             */
            Text(
                text = item.preview,
                color = SecondaryText,
                fontSize = 7.sp,
                lineHeight = 7.sp,
                fontFamily = InterfaceFont,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.offset(y = (-3).dp)
            )
        }

        Spacer(Modifier.width(5.dp))

        /*
         * Sits outside the text column so its tap target cannot
         * inflate the title line and push the preview away from it.
         * Its own clickable consumes the tap, so the row underneath
         * does not open.
         */
        Box(
            modifier = Modifier
                .align(Alignment.Top)
                .size(14.dp)
                .clickable {
                    onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "clear",
                tint = TertiaryText,
                modifier = Modifier.size(9.dp)
            )
        }
    }
}

fun formatCallDuration(
    seconds: Long
): String {

    val minutes =
        seconds / 60

    val remainingSeconds =
        seconds % 60

    return if (
        minutes > 0
    ) {
        "${minutes}m ${remainingSeconds}s"
    } else {
        "${remainingSeconds}s"
    }
}

@Composable
fun MailInboxScreen(
    messages: List<MailMessage>,
    loading: Boolean,
    onOpenMessage: (MailMessage) -> Unit,
    onTrashMessage: (MailMessage) -> Unit,
    onMarkUnread: (MailMessage) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {

    val focusRequester =
        remember {
            FocusRequester()
        }

    val listState =
        rememberLazyListState()

    var selectedIndex by remember {
        mutableIntStateOf(
            0
        )
    }

    LaunchedEffect(Unit) {

        focusRequester
            .requestFocus()
    }

    LaunchedEffect(
        messages.size
    ) {

        if (
            messages.isEmpty()
        ) {

            selectedIndex =
                0

        } else if (
            selectedIndex >
            messages.lastIndex
        ) {

            selectedIndex =
                messages.lastIndex
        }
    }

    LaunchedEffect(
        selectedIndex
    ) {

        if (
            messages.isNotEmpty()
        ) {

            listState
                .animateScrollToItem(
                    selectedIndex
                )
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .padding(
                    horizontal =
                        10.dp,
                    vertical =
                        7.dp
                )
                .focusRequester(
                    focusRequester
                )
                .focusable()
                .onPreviewKeyEvent {
                        event ->

                    if (
                        event.type !=
                        KeyEventType.KeyDown
                    ) {

                        false

                    } else {

                        when (
                            event.key
                        ) {

                            Key.DirectionDown -> {

                                if (
                                    messages.isNotEmpty()
                                ) {

                                    selectedIndex =
                                        (
                                                selectedIndex + 1
                                                )
                                            .coerceAtMost(
                                                messages.lastIndex
                                            )
                                }

                                true
                            }

                            Key.DirectionUp -> {

                                selectedIndex =
                                    (
                                            selectedIndex - 1
                                            )
                                        .coerceAtLeast(
                                            0
                                        )

                                true
                            }

                            Key.Enter -> {

                                messages
                                    .getOrNull(
                                        selectedIndex
                                    )
                                    ?.let {

                                        onOpenMessage(
                                            it
                                        )
                                    }

                                true
                            }

                            Key.Escape -> {

                                onBack()

                                true
                            }

                            else -> {

                                if (
                                    event.key ==
                                    Key.R
                                ) {

                                    onRefresh()

                                    true

                                } else {

                                    false
                                }
                            }
                        }
                    }
                }
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = CameraSafeStartPadding),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            LauncherBackButton(onBack = onBack)

            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )

            Text(
                text =
                    "mail",
                color =
                    PrimaryText,
                fontSize =
                    29.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    5.dp
                )
        )

        MinimalDivider()

        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )

        if (
            messages.isEmpty()
        ) {

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(
                            1f
                        ),
                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text =
                        if (
                            loading
                        ) {
                            "loading mail"
                        } else {
                            "no inbox mail"
                        },
                    color =
                        TertiaryText,
                    fontSize =
                        10.sp,
                    fontFamily =
                        InterfaceFont
                )
            }

        } else {

            LazyColumn(
                state =
                    listState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(
                            1f
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        2.dp
                    )
            ) {

                itemsIndexed(
                    items =
                        messages,
                    key = {
                            _,
                            message ->

                        message.id
                    }
                ) {
                        index,
                        message ->

                    MailInboxRow(
                        message =
                            message,
                        selected =
                            index ==
                                    selectedIndex,
                        onClick = {

                            selectedIndex =
                                index

                            onOpenMessage(
                                message
                            )
                        },
                        onTrash = {

                            onTrashMessage(
                                message
                            )
                        },
                        onMarkUnread = {

                            onMarkUnread(
                                message
                            )
                        }
                    )
                }
            }
        }

    }
}

@Composable
fun MailInboxRow(
    message: MailMessage,
    selected: Boolean,
    onClick: () -> Unit,
    onTrash: () -> Unit,
    onMarkUnread: () -> Unit
) {

    var horizontalOffset by remember(
        message.id
    ) {
        mutableStateOf(
            0f
        )
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 54.dp)
                .background(
                    SurfaceBlack,
                    RoundedCornerShape(
                        8.dp
                    )
                )
    ) {

        Text(
            text =
                "UNREAD",
            color =
                AccentOrange,
            fontSize =
                8.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.Bold,
            modifier =
                Modifier
                    .align(
                        Alignment.CenterStart
                    )
                    .padding(
                        start =
                            12.dp
                    )
        )

        Text(
            text =
                "TRASH",
            color =
                AccentOrange,
            fontSize =
                8.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.Bold,
            modifier =
                Modifier
                    .align(
                        Alignment.CenterEnd
                    )
                    .padding(
                        end =
                            12.dp
                    )
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 54.dp)
                    .offset {
                        IntOffset(
                            horizontalOffset
                                .roundToInt(),
                            0
                        )
                    }
                    .background(
                        if (
                            selected
                        ) {
                            SurfaceBlack
                        } else {
                            BackgroundBlack
                        },
                        RoundedCornerShape(
                            8.dp
                        )
                    )
                    .border(
                        width =
                            if (
                                selected
                            ) {
                                0.75.dp
                            } else {
                                0.dp
                            },
                        color =
                            if (
                                selected
                            ) {
                                BorderGray
                            } else {
                                Color.Transparent
                            },
                        shape =
                            RoundedCornerShape(
                                8.dp
                            )
                    )
                    .pointerInput(
                        message.id
                    ) {

                        detectHorizontalDragGestures(
                            onHorizontalDrag = {
                                    _,
                                    dragAmount ->

                                horizontalOffset =
                                    (
                                            horizontalOffset +
                                                    dragAmount
                                            )
                                        .coerceIn(
                                            -220f,
                                            220f
                                        )
                            },
                            onDragEnd = {

                                if (
                                    horizontalOffset <=
                                    -110f
                                ) {

                                    onTrash()

                                } else if (
                                    horizontalOffset >=
                                    110f
                                ) {

                                    onMarkUnread()
                                }

                                horizontalOffset =
                                    0f
                            },
                            onDragCancel = {

                                horizontalOffset =
                                    0f
                            }
                        )
                    }
                    .clickable {

                        onClick()
                    }
                    .padding(
                        horizontal =
                            8.dp,
                        vertical =
                            4.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    if (
                        message.unread
                    ) {
                        "•"
                    } else {
                        " "
                    },
                color =
                    AccentOrange,
                fontSize =
                    10.sp,
                lineHeight =
                    10.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.width(
                        5.dp
                    )
            )

            Column(
                modifier =
                    Modifier.weight(
                        1f
                    )
            ) {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            message.sender.uppercase(),
                        color =
                            if (
                                message.unread
                            ) {
                                PrimaryText
                            } else {
                                SecondaryText
                            },
                        fontSize =
                            9.5.sp,
                        lineHeight =
                            9.8.sp,
                        fontFamily =
                            InterfaceFont,
                        fontWeight =
                            if (
                                message.unread
                            ) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            },
                        maxLines =
                            1,
                        overflow =
                            TextOverflow.Ellipsis,
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                6.dp
                            )
                    )

                    Text(
                        text =
                            formatInboxTime(
                                message.timestamp
                            ),
                        color =
                            TertiaryText,
                        fontSize =
                            6.5.sp,
                        fontFamily =
                            InterfaceFont,
                        maxLines =
                            1
                    )
                }
                Text(
                    text =
                        message.snippet,
                    color =
                        TertiaryText,
                    fontSize =
                        7.5.sp,
                    lineHeight =
                        7.8.sp,
                    fontFamily =
                        InterfaceFont,
                    maxLines =
                        1,
                    overflow =
                        TextOverflow.Ellipsis
                )
            }

            if (
                message.important
            ) {

                Spacer(
                    modifier =
                        Modifier.width(
                            5.dp
                        )
                )

                Text(
                    text =
                        "!",
                    color =
                        AccentOrange,
                    fontSize =
                        10.sp,
                    fontFamily =
                        InterfaceFont,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MailDetailScreen(
    message: MailMessage,
    onReply: (MailMessage) -> Unit,
    onReplyAll: (MailMessage) -> Unit,
    onForward: (MailMessage) -> Unit,
    onArchive: (MailMessage) -> Unit,
    onSnooze24Hours: (MailMessage) -> Unit,
    onTrash: (MailMessage) -> Unit,
    onBack: () -> Unit,
    floatingMode: Boolean = false
) {

    val focusRequester =
        remember {
            FocusRequester()
        }

    var showMoreMenu by
    remember {
        mutableStateOf(
            false
        )
    }

    LaunchedEffect(Unit) {

        focusRequester
            .requestFocus()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .padding(
                    horizontal =
                        if (
                            floatingMode
                        ) {
                            12.dp
                        } else {
                            10.dp
                        },
                    vertical =
                        if (
                            floatingMode
                        ) {
                            10.dp
                        } else {
                            7.dp
                        }
                )
                .focusRequester(
                    focusRequester
                )
                .focusable()
                .onPreviewKeyEvent {
                        event ->

                    if (
                        event.type !=
                        KeyEventType.KeyDown
                    ) {

                        false

                    } else {

                        when (
                            event.key
                        ) {

                            Key.Escape -> {

                                onBack()
                                true
                            }

                            Key.R -> {

                                onReply(
                                    message
                                )
                                true
                            }

                            else ->
                                false
                        }
                    }
                }
    ) {

        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start =
                                if (
                                    floatingMode
                                ) {
                                    0.dp
                                } else {
                                    CameraSafeStartPadding
                                }
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                LauncherBackButton(
                    onBack =
                        onBack
                )

                Spacer(
                    modifier =
                        Modifier.width(
                            8.dp
                        )
                )

                Column(
                    modifier =
                        Modifier.weight(
                            1f
                        )
                ) {

                    Text(
                        text =
                            message.sender.uppercase(),
                        color =
                            PrimaryText,
                        fontSize =
                            if (
                                floatingMode
                            ) {
                                14.5.sp
                            } else {
                                13.5.sp
                            },
                        fontFamily =
                            InterfaceFont,
                        fontWeight =
                            FontWeight.Medium,
                        maxLines =
                            1
                    )

                    Text(
                        text =
                            formatMailDate(
                                message.timestamp
                            ),
                        color =
                            TertiaryText,
                        fontSize =
                            if (
                                floatingMode
                            ) {
                                7.6.sp
                            } else {
                                7.sp
                            },
                        fontFamily =
                            InterfaceFont,
                        maxLines =
                            1
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .width(
                                36.dp
                            )
                            .height(
                                30.dp
                            )
                            .clickable {

                                showMoreMenu =
                                    !showMoreMenu
                            },
                    contentAlignment =
                        Alignment.Center
                ) {

                    Text(
                        text =
                            "...",
                        color =
                            AccentOrange,
                        fontSize =
                            13.sp,
                        lineHeight =
                            13.sp,
                        fontFamily =
                            InterfaceFont,
                        fontWeight =
                            FontWeight.Medium,
                        maxLines =
                            1,
                        softWrap =
                            false
                    )
                }
            }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(
                            1f
                        )
            ) {

                item {

                    Text(
                        text =
                            message.subject,
                        color =
                            PrimaryText,
                        fontSize =
                            if (
                                floatingMode
                            ) {
                                13.sp
                            } else {
                                12.sp
                            },
                        lineHeight =
                            if (
                                floatingMode
                            ) {
                                16.sp
                            } else {
                                15.sp
                            },
                        fontFamily =
                            InterfaceFont,
                        fontWeight =
                            FontWeight.Medium
                    )

                    if (
                        message.important ||
                        message.unread
                    ) {

                        Spacer(
                            modifier =
                                Modifier.height(
                                    3.dp
                                )
                        )

                        Text(
                            text =
                                buildList {

                                    if (
                                        message.important
                                    ) {
                                        add(
                                            "IMPORTANT"
                                        )
                                    }

                                    if (
                                        message.unread
                                    ) {
                                        add(
                                            "UNREAD"
                                        )
                                    }
                                }
                                    .joinToString(
                                        " · "
                                    ),
                            color =
                                AccentOrange,
                            fontSize =
                                7.sp,
                            fontFamily =
                                InterfaceFont
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                8.dp
                            )
                    )

                    Text(
                        text =
                            message.body,
                        color =
                            PrimaryText,
                        fontSize =
                            if (
                                floatingMode
                            ) {
                                11.4.sp
                            } else {
                                10.5.sp
                            },
                        lineHeight =
                            if (
                                floatingMode
                            ) {
                                16.2.sp
                            } else {
                                15.sp
                            },
                        fontFamily =
                            InterfaceFont
                    )
                }
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            if (
                                floatingMode
                            ) {
                                36.dp
                            } else {
                                32.dp
                            }
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                MailDetailAction(
                    text =
                        "reply",
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    onClick = {
                        onReply(
                            message
                        )
                    }
                )

                MailDetailAction(
                    text =
                        "reply all",
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    onClick = {
                        onReplyAll(
                            message
                        )
                    }
                )

                MailDetailAction(
                    text =
                        "forward",
                    modifier =
                        Modifier.weight(
                            1f
                        ),
                    onClick = {
                        onForward(
                            message
                        )
                    }
                )
            }
        }

        if (
            showMoreMenu
        ) {

            /*
             * Transparent dismiss layer behind the small overflow menu.
             * Any tap outside the menu closes it immediately.
             */
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .pointerInput(
                            showMoreMenu
                        ) {

                            detectTapGestures {

                                showMoreMenu =
                                    false
                            }
                        }
            )

            Box(
                modifier =
                    Modifier
                        .align(
                            Alignment.TopEnd
                        )
                        .padding(
                            top =
                                30.dp,
                            end =
                                1.dp
                        )
                        .width(
                            124.dp
                        )
                        .background(
                            SurfaceBlack,
                            RoundedCornerShape(
                                10.dp
                            )
                        )
                        .border(
                            0.8.dp,
                            BorderGray,
                            RoundedCornerShape(
                                10.dp
                            )
                        )
                        .padding(
                            vertical =
                                4.dp
                        )
            ) {

                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    MailMoreMenuAction(
                        text =
                            "archive",
                        onClick = {

                            showMoreMenu =
                                false

                            onArchive(
                                message
                            )
                        }
                    )

                    MailMoreMenuAction(
                        text =
                            "snooze 24hrs",
                        onClick = {

                            showMoreMenu =
                                false

                            onSnooze24Hours(
                                message
                            )
                        }
                    )

                    MailMoreMenuAction(
                        text =
                            "trash",
                        onClick = {

                            showMoreMenu =
                                false

                            onTrash(
                                message
                            )
                        }
                    )
                }
            }
        }
    }
}


@Composable
private fun MailMoreMenuAction(
    text: String,
    onClick: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    30.dp
                )
                .clickable(
                    onClick =
                        onClick
                )
                .padding(
                    horizontal =
                        10.dp
                ),
        contentAlignment =
            Alignment.CenterStart
    ) {

        Text(
            text =
                text,
            color =
                PrimaryText,
            fontSize =
                8.5.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.Medium
        )
    }
}


@Composable
private fun MailDetailAction(
    text: String,
    modifier: Modifier =
        Modifier,
    onClick: () -> Unit
) {

    Box(
        modifier =
            modifier
                .fillMaxHeight()
                .clickable(
                    onClick =
                        onClick
                ),
        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                text,
            color =
                AccentOrange,
            fontSize =
                8.5.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.Medium
        )
    }
}


fun formatMailDate(
    timestamp: Long
): String {

    return SimpleDateFormat(
        "MMM d · h:mm a",
        Locale.getDefault()
    )
        .format(
            Date(
                timestamp
            )
        )
        .lowercase()
}


@Composable
fun MailComposeScreen(
    draft: MailComposeDraft,
    statusText: String,
    onSend: (
        String,
        String,
        String
    ) -> Unit,
    onBack: () -> Unit
) {

    var toAddress by remember(
        draft
    ) {
        mutableStateOf(
            draft.toAddress
        )
    }

    var subject by remember(
        draft
    ) {
        mutableStateOf(
            draft.subject
        )
    }

    var body by remember(
        draft
    ) {
        mutableStateOf(
            draft.body
        )
    }

    val recipientFocusRequester =
        remember {
            FocusRequester()
        }

    val subjectFocusRequester =
        remember {
            FocusRequester()
        }

    val bodyFocusRequester =
        remember {
            FocusRequester()
        }

    val sendFocusRequester =
        remember {
            FocusRequester()
        }

    LaunchedEffect(
        draft
    ) {

        when {

            draft.recipientEditable -> {

                recipientFocusRequester
                    .requestFocus()
            }

            draft.isReply -> {

                bodyFocusRequester
                    .requestFocus()
            }

            else -> {

                subjectFocusRequester
                    .requestFocus()
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 7.dp
                )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = CameraSafeStartPadding),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            LauncherBackButton(onBack = onBack)

            Spacer(
                modifier = Modifier.width(8.dp)
            )

            Text(
                text =
                    when {

                        draft.isForward ->
                            "FORWARD"

                        draft.isReplyAll ->
                            "REPLY ALL"

                        draft.isReply ->
                            "REPLY"

                        else ->
                            "NEW MAIL"
                    },
                color = PrimaryText,
                fontSize = 14.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        MinimalDivider()

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        Text(
            text = "TO",
            color = TertiaryText,
            fontSize = 7.sp,
            fontFamily = InterfaceFont
        )

        if (
            draft.recipientEditable
        ) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            32.dp
                        )
                        .background(
                            InputSurface,
                            RoundedCornerShape(
                                8.dp
                            )
                        )
                        .border(
                            0.75.dp,
                            BorderGray,
                            RoundedCornerShape(
                                8.dp
                            )
                        )
                        .padding(
                            horizontal =
                                8.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                BasicTextField(
                    value =
                        toAddress,
                    onValueChange = {

                        toAddress =
                            it.replace(
                                "\n",
                                " "
                            )
                    },
                    singleLine =
                        true,
                    textStyle =
                        TextStyle(
                            color =
                                PrimaryText,
                            fontSize =
                                10.5.sp,
                            fontFamily =
                                InterfaceFont
                        ),
                    cursorBrush =
                        SolidColor(
                            AccentOrange
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(
                                recipientFocusRequester
                            )
                            .onPreviewKeyEvent {
                                    event ->

                                if (
                                    event.type !=
                                    KeyEventType.KeyDown
                                ) {

                                    false

                                } else {

                                    when (
                                        event.key
                                    ) {

                                        Key.Escape -> {

                                            onBack()
                                            true
                                        }

                                        Key.Enter,
                                        Key.DirectionDown -> {

                                            bodyFocusRequester
                                                .requestFocus()
                                            true
                                        }

                                        Key.Tab -> {

                                            subjectFocusRequester
                                                .requestFocus()
                                            true
                                        }

                                        else ->
                                            false
                                    }
                                }
                            }
                )
            }

        } else {

            Text(
                text =
                    if (
                        draft.toLabel.equals(
                            draft.toAddress,
                            ignoreCase =
                                true
                        )
                    ) {
                        draft.toAddress
                    } else {
                        "${draft.toLabel} · ${draft.toAddress}"
                    },
                color =
                    PrimaryText,
                fontSize =
                    9.5.sp,
                fontFamily =
                    InterfaceFont,
                maxLines =
                    1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        Text(
            text = "SUBJECT",
            color = TertiaryText,
            fontSize = 7.sp,
            fontFamily = InterfaceFont
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .background(
                        InputSurface,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        0.75.dp,
                        BorderGray,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(
                        horizontal = 8.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            BasicTextField(
                value = subject,
                onValueChange = {

                    subject =
                        it.replace(
                            "\n",
                            " "
                        )
                },
                singleLine = true,
                textStyle =
                    TextStyle(
                        color = PrimaryText,
                        fontSize = 10.5.sp,
                        fontFamily = InterfaceFont
                    ),
                cursorBrush =
                    SolidColor(
                        PrimaryText
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(
                            subjectFocusRequester
                        )
                        .onPreviewKeyEvent {
                                event ->

                            if (
                                event.type != KeyEventType.KeyDown
                            ) {

                                false

                            } else {

                                when (
                                    event.key
                                ) {

                                    Key.Escape -> {

                                        onBack()
                                        true
                                    }

                                    Key.Enter,
                                    Key.DirectionDown,
                                    Key.Tab -> {

                                        bodyFocusRequester
                                            .requestFocus()
                                        true
                                    }

                                    else -> false
                                }
                            }
                        }
            )
        }

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    "MESSAGE",
                color =
                    TertiaryText,
                fontSize =
                    7.sp,
                fontFamily =
                    InterfaceFont
            )

            if (
                draft.isForward &&
                draft.forwardedBody.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier.width(
                            6.dp
                        )
                )

                Text(
                    text =
                        "original included",
                    color =
                        TertiaryText,
                    fontSize =
                        6.5.sp,
                    fontFamily =
                        InterfaceFont
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        InputSurface,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        0.75.dp,
                        BorderGray,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
        ) {

            BasicTextField(
                value = body,
                onValueChange = {

                    body = it
                },
                textStyle =
                    TextStyle(
                        color = PrimaryText,
                        fontSize = 10.sp,
                        fontFamily = InterfaceFont,
                        lineHeight = 14.sp
                    ),
                cursorBrush =
                    SolidColor(
                        PrimaryText
                    ),
                modifier =
                    Modifier
                        .fillMaxSize()
                        .focusRequester(
                            bodyFocusRequester
                        )
                        .onPreviewKeyEvent {
                                event ->

                            if (
                                event.type != KeyEventType.KeyDown
                            ) {

                                false

                            } else {

                                when (
                                    event.key
                                ) {

                                    Key.Escape -> {

                                        onBack()
                                        true
                                    }

                                    Key.Tab,
                                    Key.DirectionDown -> {

                                        sendFocusRequester
                                            .requestFocus()
                                        true
                                    }

                                    else -> false
                                }
                            }
                        }
            )
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(
                        SurfaceBlack,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        0.75.dp,
                        BorderGray,
                        RoundedCornerShape(8.dp)
                    )
                    .focusRequester(
                        sendFocusRequester
                    )
                    .focusable()
                    .onPreviewKeyEvent {
                            event ->

                        if (
                            event.type != KeyEventType.KeyDown
                        ) {

                            false

                        } else {

                            when (
                                event.key
                            ) {

                                Key.Enter -> {

                                    onSend(
                                        toAddress,
                                        subject,
                                        body
                                    )
                                    true
                                }

                                Key.DirectionUp -> {

                                    bodyFocusRequester
                                        .requestFocus()
                                    true
                                }

                                Key.Escape -> {

                                    onBack()
                                    true
                                }

                                else -> false
                            }
                        }
                    }
                    .clickable {

                        onSend(
                            toAddress,
                            subject,
                            body
                        )
                    },
            verticalAlignment =
                Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.Center
        ) {

            Text(
                text = "SEND",
                color = AccentOrange,
                fontSize = 9.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(3.dp)
        )

        if (
            statusText.isNotBlank()
        ) {
            Text(
                text = statusText,
                color = AccentOrange,
                fontSize = 6.5.sp,
                fontFamily = InterfaceFont,
                maxLines = 1
            )
        }
    }
}


/*
 * Opens the conversation with this number in the phone's default SMS
 * app (Google Messages, Samsung Messages, ...) unless Prompt Launcher
 * itself is the default. Returns true when it handed off, false when
 * the caller should show Prompt's own thread instead.
 */
internal fun openThreadInMessagingAppIfNotDefault(
    context: Context,
    phoneNumber: String
): Boolean {

    val promptIsDefault =
        runCatching {
            com.forrest.titanlauncher.messages.SmsRoleManager(
                context
            ).isDefaultSmsApp()
        }
            .getOrDefault(
                false
            )

    if (
        promptIsDefault
    ) {
        return false
    }

    return runCatching {
        context.startActivity(
            Intent(
                Intent.ACTION_SENDTO,
                Uri.parse(
                    "smsto:" + phoneNumber
                )
            )
        )
        true
    }
        .getOrDefault(
            false
        )
}
