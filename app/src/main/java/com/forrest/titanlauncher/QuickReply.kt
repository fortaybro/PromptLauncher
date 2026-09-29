package com.forrest.titanlauncher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forrest.titanlauncher.contacts.Contact
import com.forrest.titanlauncher.messages.MmsSender
import com.forrest.titanlauncher.messages.SmsMessage
import com.forrest.titanlauncher.notifications.HubNotification
import com.forrest.titanlauncher.notifications.NotificationMessage
import com.forrest.titanlauncher.notifications.NotificationCenter


/*
 * QUICK REPLY
 *
 * The bolt button in the home header. Shows recent incoming texts and
 * any notification the posting app marked as replyable, and lets the
 * user answer without leaving home.
 *
 * Texts go out through the launcher's own send path. Notifications
 * fire their own direct-reply action, the same one the system shade
 * uses, so replying to a chat app works without that app ever being
 * opened.
 */


/*
 * Four rows at their 44.dp minimum plus the 5.dp gaps between them.
 * The list caps here and scrolls, so the card is the same height
 * whether four alerts are waiting or twelve.
 */
private val QuickReplyListMaxHeight =
    175.dp

internal data class QuickReplyEntry(
    val id: String,
    val title: String,
    val preview: String,
    val timestamp: Long,
    val isMessage: Boolean,
    val phoneNumber: String? = null,
    val photoUri: String? = null,
    val notificationKey: String? = null,
    val canReply: Boolean = true,
    val unread: Boolean = false,

    /*
     * Present on group threads. Replying needs every participant, not
     * just whoever sent the message being answered.
     */
    val threadKey: String = "",
    val participants: List<String> = emptyList(),
    val isGroup: Boolean = false,

    /*
     * The app that posted a notification, so its icon can stand in
     * for the generic glyph.
     */
    val sourcePackage: String? = null,

    /*
     * Recent messages from a MessagingStyle notification, newest
     * last. Empty for texts and for apps that post a plain
     * notification.
     */
    val messages: List<NotificationMessage> = emptyList()
)


/*
 * Builds the combined list: incoming texts newest first, then any
 * replyable notifications, capped so the card stays a sensible height.
 */
internal fun buildQuickReplyEntries(
    recentMessages: List<SmsMessage>,
    notifications: List<HubNotification>,
    contactNameFor: (String) -> String?,
    contactPhotoFor: (String) -> String?,
    limit: Int = 12
): List<QuickReplyEntry> {

    val messageEntries =
        recentMessages
            .filter {
                it.incoming
            }
            .groupBy {

                /*
                 * By thread, so a group is one entry rather than one
                 * per member who has written.
                 */
                it.threadKey
                    .ifBlank {
                        it.phoneNumber
                    }
            }
            .values
            .mapNotNull { thread ->

                /*
                 * Only threads still waiting on the user appear here.
                 * Replying marks the thread read, which is what makes
                 * the entry drop out of the list and the bolt go dim.
                 */
                if (
                    thread.none {
                        it.incoming &&
                                !it.isRead
                    }
                ) {
                    return@mapNotNull null
                }

                val latest =
                    thread
                        .maxByOrNull {
                            it.timestamp
                        }
                        ?: return@mapNotNull null

                val isGroup =
                    thread.any {
                        it.isGroup
                    }

                val threadKey =
                    latest.threadKey
                        .ifBlank {
                            latest.phoneNumber
                        }

                val participants =
                    threadKey
                        .split(",")
                        .filter {
                            it.isNotBlank()
                        }

                QuickReplyEntry(
                    unread =
                        thread.any {
                            it.incoming &&
                                    !it.isRead
                        },
                    id =
                        "sms:" + threadKey,
                    threadKey =
                        threadKey,
                    participants =
                        participants,
                    isGroup =
                        isGroup,
                    title =
                        if (
                            isGroup
                        ) {

                            participants
                                .map { number ->

                                    (
                                            contactNameFor(
                                                number
                                            )
                                                ?: number
                                            )
                                        .substringBefore(" ")
                                }
                                .joinToString(
                                    separator = ", "
                                )

                        } else {

                            contactNameFor(
                                latest.phoneNumber
                            )
                                ?: latest.phoneNumber
                        },
                    preview =
                        latest.body,
                    timestamp =
                        latest.timestamp,
                    isMessage =
                        true,
                    phoneNumber =
                        latest.phoneNumber,
                    photoUri =
                        if (
                            isGroup
                        ) {
                            null
                        } else {
                            contactPhotoFor(
                                latest.phoneNumber
                            )
                        }
                )
            }

    val notificationEntries =
        notifications
            .filter {
                it.canReply
            }
            .map { notification ->

                QuickReplyEntry(
                    unread =
                        true,
                    id =
                        "noti:" + notification.key,
                    title =
                        notification.title
                            .ifBlank {
                                notification.appName
                            },
                    preview =
                        notification.text,
                    timestamp =
                        notification.timestamp,
                    isMessage =
                        false,
                    notificationKey =
                        notification.key,
                    messages =
                        notification.messages,
                    sourcePackage =
                        notification.packageName
                )
            }

    return (messageEntries + notificationEntries)
        .sortedByDescending {
            it.timestamp
        }
        .take(
            limit
        )
}


@Composable
internal fun QuickReplyOverlay(
    entries: List<QuickReplyEntry>,
    onSendMessage: (Contact, String) -> Unit,
    onMarkConversationRead: (String) -> Unit,
    onOpenConversation: (Contact) -> Unit,
    onStatus: (String) -> Unit = {},
    onDismiss: () -> Unit
) {

    val context =
        LocalContext.current

    val messageSounds =
        rememberMessageSounds()

    var selectedEntry by remember {
        mutableStateOf<QuickReplyEntry?>(
            null
        )
    }

    var replyText by remember {
        mutableStateOf(
            ""
        )
    }

    var notice by remember {
        mutableStateOf(
            ""
        )
    }

    val replyFocus =
        remember {
            FocusRequester()
        }

    /*
     * Clearing without answering: a text is marked read, a
     * notification is dismissed outright. Either way the entry stops
     * being outstanding and drops out of the list.
     */
    fun dismissEntry(
        entry: QuickReplyEntry
    ) {

        if (
            entry.isMessage
        ) {

            entry.phoneNumber
                ?.let(
                    onMarkConversationRead
                )

        } else {

            entry.notificationKey
                ?.let {
                    NotificationCenter
                        .dismissNotification(
                            it
                        )
                }
        }
    }

    fun closeReply() {

        selectedEntry =
            null

        replyText =
            ""
    }

    fun sendReply() {

        val entry =
            selectedEntry
                ?: return

        val message =
            replyText.trim()

        if (
            message.isBlank()
        ) {
            return
        }

        messageSounds
            .playQuickReplySend()

        /*
         * Same confirmation the command bar gives for a note or a
         * task, so sending from here feels like the rest of the
         * launcher rather than a silent action.
         */
        onStatus(
            "✓ MESSAGE SENT"
        )

        if (
            entry.isMessage &&
            entry.isGroup
        ) {

            /*
             * A group reply is an MMS to every participant, the same
             * path the conversation screen uses.
             */
            MmsSender.send(
                context =
                    context,
                recipients =
                    entry.participants,
                body =
                    message,
                threadKey =
                    entry.threadKey
            )

            onMarkConversationRead(
                entry.threadKey
            )

            closeReply()
            return
        }

        if (
            entry.isMessage
        ) {

            val number =
                entry.phoneNumber
                    ?: return

            onSendMessage(
                Contact(
                    name =
                        entry.title,
                    phoneNumber =
                        number
                ),
                message
            )

            /*
             * Answering clears the alert, so the bolt stops glowing
             * for something already dealt with.
             */
            onMarkConversationRead(
                number
            )

            /*
             * Back to the list rather than out of the overlay. The
             * entry disappears on its own once the thread reads as
             * handled, so what is left is what still needs answering.
             */
            closeReply()
            return
        }

        val key =
            entry.notificationKey
                ?: return

        val sent =
            NotificationCenter.sendReply(
                context =
                    context,
                key =
                    key,
                message =
                    message
            )

        if (
            sent
        ) {
            NotificationCenter
                .dismissNotification(
                    key
                )

            closeReply()
        } else {
            notice =
                "could not send reply"
        }
    }

    BackHandler {

        if (
            selectedEntry != null
        ) {
            closeReply()
        } else {
            onDismiss()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = 0.18f
                    )
                )
                .pointerInput(Unit) {
                    detectTapGestures {
                        onDismiss()
                    }
                },
        contentAlignment =
            Alignment.TopCenter
    ) {

        Column(
            modifier =
                Modifier
                    /*
                     * Fills the upper third and spans the width, with
                     * margins so the blurred home screen still frames
                     * it on every side.
                     */
                    .padding(
                        horizontal = 10.dp,
                        vertical = 12.dp
                    )
                    .fillMaxWidth()
                    /*
                     * Both views size to their content. The list is
                     * bounded by QuickReplyListMaxHeight rather than a
                     * screen fraction, which keeps the card honest at
                     * any UI scale.
                     */
                    .background(
                        SurfaceBlack,
                        RoundedCornerShape(
                            14.dp
                        )
                    )
                    .border(
                        0.8.dp,
                        BorderGray,
                        RoundedCornerShape(
                            14.dp
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures {
                            // Consume taps inside the card.
                        }
                    }
                    .onPreviewKeyEvent { event ->

                        if (
                            event.type !=
                            KeyEventType.KeyDown
                        ) {
                            false
                        } else if (
                            event.key == Key.Escape
                        ) {

                            if (
                                selectedEntry != null
                            ) {
                                closeReply()
                            } else {
                                onDismiss()
                            }

                            true
                        } else {
                            false
                        }
                    }
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    )
        ) {

            /*
             * Indented so the Titan's top-left camera does not clip
             * it. The card itself stays full width.
             */
            Text(
                text =
                    "quick reply",
                color =
                    PrimaryText,
                fontSize =
                    14.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Medium,
                modifier =
                    Modifier
                        .padding(
                            start = 26.dp
                        )
            )

            Spacer(
                modifier =
                    Modifier
                        .height(
                            8.dp
                        )
            )

            val activeEntry =
                selectedEntry

            if (
                activeEntry == null
            ) {

                if (
                    entries.isEmpty()
                ) {

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    64.dp
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        Text(
                            text =
                                "go enjoy life",
                            color =
                                TertiaryText,
                            fontSize =
                                12.sp,
                            fontFamily =
                                InterfaceFont
                        )
                    }
                } else {

                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    max =
                                        QuickReplyListMaxHeight
                                ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                5.dp
                            )
                    ) {

                        items(
                            items =
                                entries,
                            key = {
                                it.id
                            }
                        ) { entry ->

                            QuickReplyRow(
                                entry =
                                    entry,
                                onDismissEntry = {
                                    dismissEntry(
                                        entry
                                    )
                                },
                                onClick = {

                                    notice =
                                        ""

                                    replyText =
                                        ""

                                    selectedEntry =
                                        entry
                                }
                            )
                        }
                    }
                }
            } else {

                LaunchedEffect(
                    activeEntry.id
                ) {
                    replyFocus
                        .requestFocus()
                }

                val activePhoto =
                    rememberContactPhoto(
                        activeEntry.photoUri
                    )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    /*
                     * Width only, not a fixed square: at 12.sp the
                     * glyph's line box is taller than 24.dp, so
                     * constraining the height clipped it. The row
                     * centres it against the name.
                     */
                    Box(
                        modifier =
                            Modifier
                                .width(
                                    24.dp
                                ),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        if (
                            activePhoto != null
                        ) {

                            Image(
                                bitmap =
                                    activePhoto,
                                contentDescription =
                                    activeEntry.title,
                                contentScale =
                                    ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .size(
                                            24.dp
                                        )
                                        .clip(
                                            CircleShape
                                        )
                            )
                        } else {

                            Text(
                                text =
                                    "@",
                                color =
                                    AccentOrange,
                                fontSize =
                                    12.sp,
                                lineHeight =
                                    12.sp,
                                fontFamily =
                                    InterfaceFont,
                                textAlign =
                                    TextAlign.Center,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier
                                .width(
                                    8.dp
                                )
                    )

                    Text(
                        text =
                            activeEntry.title,
                        color =
                            PrimaryText,
                        fontSize =
                            11.sp,
                        fontFamily =
                            InterfaceFont,
                        fontWeight =
                            FontWeight.Medium,
                        maxLines =
                            1,
                        overflow =
                            TextOverflow.Ellipsis,
                        modifier =
                            Modifier
                                .weight(1f)
                    )

                    /*
                     * Only texts have a conversation to open. Taking
                     * it opens the thread exactly as the messages app
                     * would.
                     */
                    /*
                     * A text opens its conversation here; a
                     * notification fires its own content intent,
                     * which is the deep link the posting app supplied
                     * — so WhatsApp lands on that chat, Asana on that
                     * task, and so on.
                     */
                    val canOpenFull =
                        (
                                activeEntry.isMessage &&
                                        activeEntry.phoneNumber != null
                                ) ||
                                activeEntry.notificationKey != null

                    if (
                        canOpenFull
                    ) {

                        Text(
                            text =
                                "go to full message",
                            color =
                                AccentOrange,
                            fontSize =
                                8.5.sp,
                            fontFamily =
                                InterfaceFont,
                            maxLines =
                                1,
                            modifier =
                                Modifier
                                    .clickable {

                                        val number =
                                            activeEntry.phoneNumber

                                        val key =
                                            activeEntry.notificationKey

                                        /*
                                         * Fired before the overlay
                                         * closes. Dismissing first
                                         * can clear the notification
                                         * this depends on, leaving
                                         * nothing to open.
                                         */
                                        if (
                                            activeEntry.isMessage &&
                                            number != null
                                        ) {

                                            closeReply()
                                            onDismiss()

                                            onOpenConversation(
                                                Contact(
                                                    name =
                                                        activeEntry.title,
                                                    phoneNumber =
                                                        number
                                                )
                                            )

                                        } else if (
                                            key != null
                                        ) {

                                            NotificationCenter
                                                .openNotification(
                                                    key =
                                                        key,
                                                    context =
                                                        context
                                                )

                                            closeReply()
                                            onDismiss()
                                        }
                                    }
                                    .padding(
                                        horizontal = 4.dp,
                                        vertical = 2.dp
                                    )
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier
                            .height(
                                5.dp
                            )
                )

                /*
                 * Messaging apps post the recent exchange as
                 * structured messages, so it can be shown as a
                 * conversation rather than one flattened line.
                 *
                 * This is only what the notification carried — the
                 * last few messages, not a history.
                 */
                if (
                    activeEntry.messages.isNotEmpty()
                ) {

                    /*
                     * Opens at the newest message rather than the
                     * oldest: the reason to look is what just
                     * arrived, and scrolling down every time is a
                     * chore.
                     */
                    val bubbleScroll =
                        rememberScrollState()

                    LaunchedEffect(
                        activeEntry.id,
                        activeEntry.messages.size
                    ) {
                        bubbleScroll.scrollTo(
                            bubbleScroll.maxValue
                        )
                    }

                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                /*
                                 * Sized for four bubbles. Anything
                                 * older scrolls rather than growing
                                 * the card into the reply field.
                                 */
                                .heightIn(
                                    max = 142.dp
                                )
                                .verticalScroll(
                                    bubbleScroll
                                ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                4.dp
                            )
                    ) {

                        activeEntry.messages
                            .takeLast(
                                12
                            )
                            .forEach { message ->

                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth(),
                                    horizontalArrangement =
                                        if (
                                            message.fromUser
                                        ) {
                                            Arrangement.End
                                        } else {
                                            Arrangement.Start
                                        }
                                ) {

                                    Column(
                                        modifier =
                                            Modifier
                                                .widthIn(
                                                    max = 168.dp
                                                )
                                                .background(
                                                    if (
                                                        message.fromUser
                                                    ) {
                                                        AccentOrange
                                                    } else {
                                                        Color(0xFFE3E3E3)
                                                    },
                                                    RoundedCornerShape(
                                                        10.dp
                                                    )
                                                )
                                                .padding(
                                                    horizontal = 8.dp,
                                                    vertical = 5.dp
                                                )
                                    ) {

                                        if (
                                            !message.fromUser &&
                                            message.sender.isNotBlank()
                                        ) {

                                            Text(
                                                text =
                                                    message.sender,
                                                color =
                                                    Color.Black.copy(
                                                        alpha = 0.55f
                                                    ),
                                                fontSize =
                                                    6.5.sp,
                                                lineHeight =
                                                    6.5.sp,
                                                fontFamily =
                                                    InterfaceFont,
                                                fontWeight =
                                                    FontWeight.Medium
                                            )

                                            Spacer(
                                                modifier =
                                                    Modifier.height(
                                                        2.dp
                                                    )
                                            )
                                        }

                                        Text(
                                            text =
                                                message.text,
                                            color =
                                                Color.Black,
                                            fontSize =
                                                8.5.sp,
                                            lineHeight =
                                                11.sp,
                                            fontFamily =
                                                InterfaceFont
                                        )
                                    }
                                }
                            }
                    }

                } else {

                    Text(
                        text =
                            activeEntry.preview,
                        color =
                            SecondaryText,
                        fontSize =
                            9.5.sp,
                        lineHeight =
                            13.sp,
                        fontFamily =
                            InterfaceFont,
                        maxLines =
                            4,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }

                Spacer(
                    modifier =
                        Modifier
                            .height(
                                8.dp
                            )
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            /*
                             * A fixed height rather than a minimum, so
                             * the prompt, the placeholder and the
                             * caret all centre against the same box
                             * instead of each against its own font
                             * metrics.
                             */
                            .height(
                                38.dp
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
                                horizontal = 8.dp,
                                vertical = 6.dp
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
                            11.sp,
                        fontFamily =
                            InterfaceFont
                    )

                    Spacer(
                        modifier =
                            Modifier
                                .width(
                                    6.dp
                                )
                    )

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        contentAlignment =
                            Alignment.CenterStart
                    ) {

                        if (
                            replyText.isEmpty()
                        ) {

                            Text(
                                text =
                                    "reply",
                                color =
                                    TertiaryText,
                                fontSize =
                                    10.sp,
                                fontFamily =
                                    InterfaceFont
                            )
                        }

                        BasicTextField(
                            value =
                                replyText,
                            onValueChange = {
                                replyText = it
                            },
                            singleLine =
                                true,
                            textStyle =
                                TextStyle(
                                    color =
                                        PrimaryText,
                                    fontSize =
                                        10.sp,
                                    lineHeight =
                                        10.sp,
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
                                        replyFocus
                                    )
                                    .onPreviewKeyEvent { event ->

                                        if (
                                            event.type ==
                                            KeyEventType.KeyDown &&
                                            event.key == Key.Enter
                                        ) {
                                            sendReply()
                                            true
                                        } else {
                                            false
                                        }
                                    }
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier
                            .height(
                                7.dp
                            )
                )

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            6.dp
                        )
                ) {

                    QuickReplyButton(
                        label =
                            "back",
                        modifier =
                            Modifier
                                .weight(1f),
                        onClick = {
                            notice = ""
                            closeReply()
                        }
                    )

                    QuickReplyButton(
                        label =
                            "send",
                        modifier =
                            Modifier
                                .weight(1f),
                        accent =
                            true,
                        onClick = {
                            sendReply()
                        }
                    )
                }
            }

            if (
                notice.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier
                            .height(
                                7.dp
                            )
                )

                Text(
                    text =
                        notice,
                    color =
                        AccentOrange,
                    fontSize =
                        9.sp,
                    fontFamily =
                        InterfaceFont
                )
            }
        }
    }
}


@Composable
private fun QuickReplyRow(
    entry: QuickReplyEntry,
    onDismissEntry: () -> Unit,
    onClick: () -> Unit
) {

    val photo =
        rememberContactPhoto(
            entry.photoUri
        )

    /*
     * A notification's own app icon says where it came from far more
     * directly than the app's name written out beside it.
     */
    val sourceIcon =
        rememberAppIconBitmap(
            entry.sourcePackage
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(
                    min = 40.dp
                )
                .background(
                    InputSurface,
                    RoundedCornerShape(
                        9.dp
                    )
                )
                .border(
                    0.75.dp,
                    BorderGray,
                    RoundedCornerShape(
                        9.dp
                    )
                )
                .clickable {
                    onClick()
                }
                .padding(
                    horizontal = 8.dp,
                    vertical = 6.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        20.dp
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            if (
                photo != null
            ) {

                Image(
                    bitmap =
                        photo,
                    contentDescription =
                        entry.title,
                    contentScale =
                        ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(
                                20.dp
                            )
                            .clip(
                                CircleShape
                            )
                )

            } else if (
                sourceIcon != null
            ) {

                Image(
                    bitmap =
                        sourceIcon,
                    contentDescription =
                        entry.title,
                    modifier =
                        Modifier
                            .size(
                                18.dp
                            )
                )

            } else {

                Text(
                    text =
                        "@",
                    color =
                        AccentOrange,
                    fontSize =
                        11.sp,
                    fontFamily =
                        InterfaceFont,
                    fontWeight =
                        FontWeight.Bold
                )
            }
        }

        Spacer(
            modifier =
                Modifier
                    .width(
                        8.dp
                    )
        )

        Column(
            modifier =
                Modifier
                    .weight(1f)
        ) {

            Text(
                text =
                    entry.title,
                color =
                    PrimaryText,
                fontSize =
                    9.5.sp,
                lineHeight =
                    10.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Medium,
                /*
                 * Two lines, now that the app name no longer takes
                 * the right-hand side: a group thread name rarely
                 * fits on one.
                 */
                maxLines =
                    2,
                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                text =
                    entry.preview,
                color =
                    SecondaryText,
                fontSize =
                    8.sp,
                fontFamily =
                    InterfaceFont,
                maxLines =
                    1,
                overflow =
                    TextOverflow.Ellipsis
            )
        }

        Spacer(
            modifier =
                Modifier
                    .width(
                        4.dp
                    )
        )

        /*
         * Clears the alert without replying. Its own clickable
         * consumes the tap, so the row underneath does not open.
         */
        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.Top
                    )
                    .size(
                        18.dp
                    )
                    .clickable {
                        onDismissEntry()
                    },
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector =
                    Icons.Outlined.Close,
                contentDescription =
                    "clear alert",
                tint =
                    TertiaryText,
                modifier =
                    Modifier
                        .size(
                            10.dp
                        )
            )
        }
    }
}


@Composable
private fun QuickReplyButton(
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: () -> Unit
) {

    Box(
        modifier =
            modifier
                .height(
                    32.dp
                )
                .background(
                    if (
                        accent
                    ) {
                        AccentOrange
                    } else {
                        InputSurface
                    },
                    RoundedCornerShape(
                        8.dp
                    )
                )
                .border(
                    0.75.dp,
                    if (
                        accent
                    ) {
                        AccentOrange
                    } else {
                        BorderGray
                    },
                    RoundedCornerShape(
                        8.dp
                    )
                )
                .clickable {
                    onClick()
                },
        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                label,
            color =
                if (
                    accent
                ) {
                    BackgroundBlack
                } else {
                    PrimaryText
                },
            fontSize =
                10.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.Medium
        )
    }
}