package com.forrest.titanlauncher

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.LauncherApps
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
import android.provider.ContactsContract
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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.ContentScale
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
import com.forrest.titanlauncher.messages.MmsSender
import com.forrest.titanlauncher.contacts.EmailContact
import com.forrest.titanlauncher.messages.SmsDatabase
import com.forrest.titanlauncher.messages.SmsMessage
import com.forrest.titanlauncher.messages.SmsRoleManager
import com.forrest.titanlauncher.messages.SmsSender
import com.forrest.titanlauncher.mail.GmailRepository
import com.forrest.titanlauncher.mail.GoogleMailAuthManager
import com.forrest.titanlauncher.mail.MailMessage
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
fun MessagesInboxScreen(
    smsDatabase: SmsDatabase,
    contactRepository: ContactRepository,
    hasContactsPermission: Boolean,
    onOpenConversation: (Contact) -> Unit,
    onOpenThread: (ConversationThread, Contact) -> Unit,
    onBack: () -> Unit
) {

    val inboxContext =
        LocalContext.current

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

    val allMessages by
    smsDatabase
        .smsDao()
        .observeAllMessages()
        .collectAsStateWithLifecycle(
            initialValue =
                emptyList()
        )

    val conversations by
    produceState(
        initialValue =
            emptyList<ConversationSummary>(),
        key1 =
            allMessages,
        key2 =
            hasContactsPermission
    ) {

        value =
            withContext(
                Dispatchers.IO
            ) {

                val ownNumber =
                    OwnNumberStore(
                        inboxContext
                    )
                        .resolve(
                            allMessages
                        )

                allMessages
                    .groupBy {

                        /*
                         * Grouped by thread rather than by number, so
                         * a group conversation is one row no matter
                         * which member sent the latest message. Rows
                         * written before the migration fall back to
                         * their number.
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

                        val participants =
                            threadKey
                                .split(",")
                                .filter {
                                    it.isNotBlank()
                                }

                        /*
                         * A group is named by its members. Falls back
                         * to the raw numbers where a participant is
                         * not in contacts.
                         */
                        val displayName =
                            if (
                                isGroup
                            ) {

                                groupTitleFor(
                                    participants =
                                        participants,
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

                        ConversationSummary(
                            photoUri =
                                if (
                                    isGroup
                                ) {
                                    null
                                } else {
                                    contactPhotoUri
                                },
                            contact =
                                Contact(
                                    name =
                                        displayName,
                                    phoneNumber =
                                        latest.phoneNumber
                                ),
                            latestMessage =
                                latest,
                            unreadCount =
                                thread.count {
                                        message ->

                                    message.incoming &&
                                            !message.isRead
                                },
                            threadKey =
                                threadKey,
                            participants =
                                participants,
                            isGroup =
                                isGroup
                        )
                    }
                    .sortedByDescending {

                        it.latestMessage.id
                    }
            }
    }

    /*
     * Groups route through onOpenThread so the conversation screen
     * gets the whole participant list, not just whoever sent last.
     */
    fun openSummary(
        summary: ConversationSummary
    ) {

        if (
            summary.isGroup
        ) {

            onOpenThread(
                ConversationThread(
                    threadKey =
                        summary.threadKey,
                    title =
                        summary.contact.name,
                    participants =
                        summary.participants,
                    isGroup =
                        true
                ),
                summary.contact
            )

        } else {

            onOpenConversation(
                summary.contact
            )
        }
    }

    LaunchedEffect(Unit) {

        focusRequester
            .requestFocus()
    }

    LaunchedEffect(
        selectedIndex
    ) {

        if (
            conversations.isNotEmpty()
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
                                    conversations.isNotEmpty()
                                ) {

                                    selectedIndex =
                                        (
                                                selectedIndex + 1
                                                )
                                            .coerceAtMost(
                                                conversations.lastIndex
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

                                conversations
                                    .getOrNull(
                                        selectedIndex
                                    )
                                    ?.let {

                                        openSummary(
                                            it
                                        )
                                    }

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

            LauncherBackButton(onBack = onBack)

            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )

            Text(
                text =
                    "messages",
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
                    4.dp
                )
        )

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
                    3.dp
                )
        ) {

            itemsIndexed(
                items =
                    conversations,
                key = {
                        _,
                        summary ->

                    summary.threadKey
                        .ifBlank {
                            normalizePhoneNumber(
                                summary.contact.phoneNumber
                            )
                        }
                }
            ) {
                    index,
                    summary ->

                ConversationInboxRow(
                    summary =
                        summary,
                    selected =
                        index ==
                                selectedIndex,
                    onClick = {

                        selectedIndex =
                            index

                        openSummary(
                            summary
                        )
                    }
                )
            }
        }

    }
}

@Composable
fun ConversationInboxRow(
    summary: ConversationSummary,
    selected: Boolean,
    onClick: () -> Unit
) {

    val message =
        summary.latestMessage

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 49.dp)
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
                .clickable {

                    onClick()
                }
                .padding(
                    horizontal =
                        8.dp,
                    vertical =
                        5.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        val contactPhoto =
            rememberContactPhoto(
                summary.photoUri
            )

        /*
         * The avatar slot is a fixed width whether or not a photo
         * exists, so every thread's name starts at the same x and the
         * list does not look ragged when only some contacts have
         * pictures.
         */
        Box(
            modifier =
                Modifier.size(
                    26.dp
                ),
            contentAlignment =
                Alignment.Center
        ) {

            if (
                contactPhoto != null
            ) {

                Image(
                    bitmap =
                        contactPhoto,
                    contentDescription =
                        summary.contact.name,
                    contentScale =
                        ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(
                                26.dp
                            )
                            .clip(
                                CircleShape
                            )
                )
            }
        }

        Spacer(
            modifier =
                Modifier.width(
                    8.dp
                )
        )

        /*
         * Unread state is carried by weight and colour rather than a
         * separate marker: bold and full strength when waiting, lighter
         * and regular once read.
         */
        val unread =
            summary.unreadCount > 0

        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Row {

                Text(
                    text =
                        summary
                            .contact
                            .name
                            .lowercase(),
                    color =
                        if (
                            unread
                        ) {
                            PrimaryText
                        } else {
                            SecondaryText
                        },
                    fontSize =
                        10.sp,
                    fontFamily =
                        InterfaceFont,
                    fontWeight =
                        if (
                            unread
                        ) {
                            FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                    modifier =
                        Modifier.weight(
                            1f
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
                        InterfaceFont
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        2.dp
                    )
            )

            Text(
                text =
                    if (
                        message.incoming
                    ) {
                        message.body
                    } else {
                        "you: ${message.body}"
                    },
                color =
                    if (
                        unread
                    ) {
                        SecondaryText
                    } else {
                        TertiaryText
                    },
                fontSize =
                    8.sp,
                lineHeight =
                    10.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    if (
                        unread
                    ) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    },
                maxLines =
                    1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }

    }
}



@Composable
fun ConversationScreen(
    contact: Contact,
    thread: ConversationThread? = null,
    smsDatabase: SmsDatabase,
    statusText: String,
    onSend: (String) -> Unit,
    onBack: () -> Unit,
    floatingMode: Boolean = false
) {

    var replyText by remember(
        thread?.threadKey
    ) {
        mutableStateOf(
            thread?.draft
                ?: ""
        )
    }

    val conversationContext =
        LocalContext.current

    /*
     * Looked up once per contact. Null for unsaved numbers and for
     * saved contacts with no picture, in which case the header simply
     * shows no avatar.
     */
    val contactPhotoUri =
        remember(
            contact.phoneNumber
        ) {

            runCatching {
                ContactRepository(
                    conversationContext
                )
                    .findContactPhotoUriByPhoneNumber(
                        contact.phoneNumber
                    )
            }
                .getOrNull()
        }

    val contactPhoto =
        rememberContactPhoto(
            contactPhotoUri
        )

    val focusRequester =
        remember {
            FocusRequester()
        }

    val listState =
        rememberLazyListState()

    /*
     * A group has no single "other number" to match on, so its
     * messages are fetched by thread key. One-to-one threads keep the
     * number-matching query they have always used.
     */
    val messages by
    (
            if (
                thread != null
            ) {
                smsDatabase
                    .smsDao()
                    .observeThread(
                        thread.threadKey
                    )
            } else {
                smsDatabase
                    .smsDao()
                    .observeConversation(
                        contact.phoneNumber
                    )
            }
            )
        .collectAsStateWithLifecycle(
            initialValue =
                emptyList()
        )

    /*
     * Participant numbers to first names, resolved once per thread so
     * every bubble is not doing its own contact lookup.
     */
    val senderNames =
        remember(
            thread?.threadKey
        ) {

            val repository =
                ContactRepository(
                    conversationContext
                )

            (thread?.participants ?: emptyList())
                .associate { number ->

                    val resolved =
                        runCatching {
                            repository
                                .findContactNameByPhoneNumber(
                                    number
                                )
                        }
                            .getOrNull()
                            ?: number

                    normalizePhoneNumber(
                        number
                    ) to
                            resolved.substringBefore(" ")
                }
        }

    val messageSounds =
        rememberMessageSounds()

    /*
     * Tells the SMS receiver this thread is on screen, so an arriving
     * message plays the in-app sound instead of raising a shade
     * notification. Also clears any notification already showing for
     * this person, since opening the thread means it has been seen.
     */
    DisposableEffect(
        contact.phoneNumber
    ) {

        MessageNotifications.activeConversationNumber =
            contact.phoneNumber

        MessageNotifications.clearThread(
            conversationContext,
            contact.phoneNumber
        )

        onDispose {
            MessageNotifications.activeConversationNumber =
                null
        }
    }

    /*
     * The receive sound fires only for messages that actually arrive
     * while this thread is open.
     *
     * Comparing against the moment the screen opened, rather than
     * seeding from the first emission: the conversation flow emits an
     * empty list before the real one, so seeding from that first
     * emission made the real load look like a brand new message and
     * played a sound every time the thread was reopened.
     */
    val openedAt =
        remember(
            contact.phoneNumber
        ) {
            System.currentTimeMillis()
        }

    var lastPlayedIncomingId by remember(
        contact.phoneNumber
    ) {
        mutableStateOf<Long?>(null)
    }

    LaunchedEffect(
        messages
    ) {

        val newestIncoming =
            messages
                .filter {
                    it.incoming
                }
                .maxByOrNull {
                    it.timestamp
                }
                ?: return@LaunchedEffect

        if (
            newestIncoming.timestamp > openedAt &&
            newestIncoming.id != lastPlayedIncomingId
        ) {

            lastPlayedIncomingId =
                newestIncoming.id

            messageSounds.playReceive()
        }
    }

    LaunchedEffect(Unit) {

        focusRequester
            .requestFocus()
    }

    LaunchedEffect(
        messages.size
    ) {

        if (
            messages.isNotEmpty()
        ) {

            delay(
                50
            )

            listState
                .scrollToItem(
                    messages.lastIndex
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

            LauncherBackButton(onBack = onBack)

            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        (-8).dp
                    )
            ) {

                Text(
                    text =
                        if (
                            thread != null
                        ) {
                            thread.title
                                .lowercase()
                        } else {
                            contact
                                .name
                                .trim()
                                .substringBefore(" ")
                                .lowercase()
                        },
                    color =
                        PrimaryText,
                    fontSize =
                        if (
                            floatingMode
                        ) {
                            14.5.sp
                        } else {
                            13.sp
                        },
                    fontFamily =
                        InterfaceFont
                )

                Text(
                    text =
                        if (
                            thread != null
                        ) {
                            thread.participants.size
                                .toString() + " people"
                        } else {
                            contact.phoneNumber
                        },
                    color =
                        TertiaryText,
                    fontSize =
                        if (
                            floatingMode
                        ) {
                            8.sp
                        } else {
                            7.5.sp
                        },
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
             * Saved contacts with a picture get an avatar on the right
             * of the header. Unsaved numbers leave the space empty
             * rather than showing a placeholder.
             */
            if (
                contactPhoto != null
            ) {

                Image(
                    bitmap =
                        contactPhoto,
                    contentDescription =
                        contact.name,
                    contentScale =
                        ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(
                                if (
                                    floatingMode
                                ) {
                                    30.dp
                                } else {
                                    26.dp
                                }
                            )
                            .clip(
                                CircleShape
                            )
                            /*
                             * Opens this person's contact card in the
                             * system Contacts app. Falls back to a
                             * new-contact form if the number is not
                             * saved.
                             */
                            .clickable {

                                val lookupUri =
                                    runCatching {
                                        ContactRepository(
                                            conversationContext
                                        )
                                            .findContactLookupUriByPhoneNumber(
                                                contact.phoneNumber
                                            )
                                    }
                                        .getOrNull()

                                val intent =
                                    if (
                                        lookupUri != null
                                    ) {

                                        Intent(
                                            Intent.ACTION_VIEW,
                                            lookupUri
                                        )

                                    } else {

                                        Intent(
                                            Intent.ACTION_INSERT_OR_EDIT
                                        )
                                            .apply {

                                                type =
                                                    ContactsContract.Contacts
                                                        .CONTENT_ITEM_TYPE

                                                putExtra(
                                                    ContactsContract.Intents.Insert.PHONE,
                                                    contact.phoneNumber
                                                )
                                            }
                                    }

                                runCatching {
                                    conversationContext
                                        .startActivity(
                                            intent
                                        )
                                }
                            }
                )
            }
        }

        /*
         * The dotted divider and its padding are gone so the thread
         * starts directly beneath the contact header, which buys back
         * roughly 20dp of scrollable height on a short screen.
         */
        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )

        LazyColumn(
            state =
                listState,
            modifier =
                Modifier
                    .weight(
                        1f
                    )
                    .fillMaxWidth(),
            verticalArrangement =
                Arrangement.spacedBy(
                    (-5).dp
                )
        ) {

            items(
                items =
                    messages,
                key = {

                    it.id
                }
            ) {
                    message ->

                ConversationMessage(
                    message =
                        message,
                    contactName =
                        contact.name,
                    floatingMode =
                        floatingMode,
                    senderLabel =
                        if (
                            thread != null &&
                            message.incoming
                        ) {
                            senderNames[
                                normalizePhoneNumber(
                                    message.senderNumber
                                        ?: message.phoneNumber
                                )
                            ]
                        } else {
                            null
                        }
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        14.dp
                    ),
            contentAlignment =
                Alignment.CenterStart
        ) {

            if (
                statusText.isNotBlank()
            ) {

                Text(
                    text =
                        statusText,
                    color =
                        TertiaryText,
                    fontSize =
                        7.5.sp,
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
                            38.dp
                        } else {
                            34.dp
                        }
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

            Text(
                text =
                    ">",
                color =
                    AccentOrange,
                fontSize =
                    if (
                        floatingMode
                    ) {
                        15.5.sp
                    } else {
                        14.sp
                    }
            )

            Spacer(
                modifier =
                    Modifier.width(
                        5.dp
                    )
            )

            BasicTextField(
                value =
                    replyText,
                onValueChange = {

                    replyText =
                        it.replace(
                            "\n",
                            ""
                        )
                },
                singleLine =
                    true,
                textStyle =
                    TextStyle(
                        color =
                            PrimaryText,
                        fontSize =
                            if (
                                floatingMode
                            ) {
                                12.8.sp
                            } else {
                                12.sp
                            },
                        fontFamily =
                            InterfaceFont
                    ),
                cursorBrush =
                    SolidColor(
                        PrimaryText
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .focusRequester(
                            focusRequester
                        )
                        .onPreviewKeyEvent {
                                event ->

                            when {

                                event.key ==
                                        Key.Enter &&
                                        event.type ==
                                        KeyEventType.KeyDown -> {

                                    val message =
                                        replyText.trim()

                                    if (
                                        message.isNotEmpty()
                                    ) {

                                        messageSounds
                                            .playSend()

                                        /*
                                         * A group reply goes out as
                                         * MMS to every participant;
                                         * a one-to-one thread keeps
                                         * the plain SMS path.
                                         */
                                        if (
                                            thread != null
                                        ) {

                                            MmsSender.send(
                                                context =
                                                    conversationContext,
                                                recipients =
                                                    thread.participants,
                                                body =
                                                    message,
                                                threadKey =
                                                    thread.threadKey
                                            )

                                        } else {

                                            onSend(
                                                message
                                            )
                                        }

                                        replyText =
                                            ""
                                    }

                                    true
                                }

                                event.key ==
                                        Key.Escape &&
                                        event.type ==
                                        KeyEventType.KeyDown -> {

                                    onBack()

                                    true
                                }

                                else ->
                                    false
                            }
                        }
            )
        }
    }
}

@Composable
fun ConversationMessage(
    message: SmsMessage,
    contactName: String,
    floatingMode: Boolean = false,
    senderLabel: String? = null
) {

    val incoming =
        message.incoming

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            if (
                incoming
            ) {
                Arrangement.Start
            } else {
                Arrangement.End
            }
    ) {

        Column(
            modifier =
                Modifier.widthIn(
                    max =
                        if (
                            floatingMode
                        ) {
                            286.dp
                        } else {
                            270.dp
                        }
                ),
            horizontalAlignment =
                if (
                    incoming
                ) {
                    Alignment.Start
                } else {
                    Alignment.End
                }
        ) {

            /*
             * Only groups need this: in a one-to-one thread the side
             * of the screen already says who is talking.
             */
            if (
                senderLabel != null
            ) {

                Text(
                    text =
                        senderLabel,
                    color =
                        SecondaryText,
                    fontSize =
                        8.sp,
                    lineHeight =
                        8.sp,
                    fontFamily =
                        InterfaceFont,
                    modifier =
                        Modifier
                            .padding(
                                start = 4.dp,
                                bottom = 2.dp
                            )
                )
            }

            Text(
                text =
                    message.body,
                color =
                    Color.Black,
                fontSize =
                    if (
                        floatingMode
                    ) {
                        10.5.sp
                    } else {
                        10.sp
                    },
                lineHeight =
                    if (
                        floatingMode
                    ) {
                        13.5.sp
                    } else {
                        13.sp
                    },
                fontFamily =
                    InterfaceFont,
                textAlign =
                    if (
                        incoming
                    ) {
                        TextAlign.Start
                    } else {
                        TextAlign.End
                    },
                modifier =
                    Modifier
                        .background(
                            color =
                                if (
                                    incoming
                                ) {
                                    Color(
                                        0xFFE3E3E3
                                    )
                                } else {
                                    AccentOrange
                                },
                            shape =
                                RoundedCornerShape(
                                    10.dp
                                )
                        )
                        .padding(
                            horizontal =
                                9.dp,
                            vertical =
                                6.dp
                        )
            )

            Text(
                text =
                    formatMessageTime(
                        message.timestamp
                    ),
                color =
                    TertiaryText,
                fontSize =
                    if (
                        floatingMode
                    ) {
                        7.sp
                    } else {
                        6.5.sp
                    },
                fontFamily =
                    InterfaceFont
            )
        }
    }
}

fun normalizePhoneNumber(
    phoneNumber: String
): String {

    val digits =
        phoneNumber.filter {

            it.isDigit()
        }

    return if (
        digits.length > 10
    ) {
        digits.takeLast(
            10
        )
    } else {
        digits
    }
}

fun formatMessageTime(
    timestamp: Long
): String {

    return SimpleDateFormat(
        "h:mm a",
        Locale.getDefault()
    )
        .format(
            Date(
                timestamp
            )
        )
        .lowercase()
}

fun formatInboxTime(
    timestamp: Long
): String {

    val date =
        Date(
            timestamp
        )

    val todayFormat =
        SimpleDateFormat(
            "yyyyMMdd",
            Locale.getDefault()
        )

    return if (
        todayFormat.format(
            Date()
        ) ==
        todayFormat.format(
            date
        )
    ) {

        formatMessageTime(
            timestamp
        )

    } else {

        SimpleDateFormat(
            "MMM d",
            Locale.getDefault()
        )
            .format(
                date
            )
            .lowercase()
    }
}