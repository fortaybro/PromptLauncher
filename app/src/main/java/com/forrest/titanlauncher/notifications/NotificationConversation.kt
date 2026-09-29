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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forrest.titanlauncher.notifications.HubNotification
import kotlinx.coroutines.delay
import com.forrest.titanlauncher.notifications.NotificationCenter
import com.forrest.titanlauncher.notifications.NotificationMessage


/*
 * NOTIFICATION CONVERSATIONS
 *
 * When another app owns SMS, a text only ever reaches Prompt Launcher
 * as a notification. This renders that exchange in the launcher's own
 * conversation styling, so reading a text looks the same whichever
 * app is holding the role.
 *
 * Replies go out through the notification's own reply action, which
 * means the posting app does the sending.
 *
 * What it cannot do is show history: only the messages the
 * notification carried, plus whatever has accumulated while the
 * launcher has been running.
 */


@Composable
internal fun NotificationConversationScreen(
    notification: HubNotification,
    onOpenInApp: () -> Unit,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    var replyText by remember(
        notification.key
    ) {
        mutableStateOf("")
    }

    /*
     * A reply sent from here produces no new notification, so the
     * posting app never tells us about it and the thread would look
     * as though nothing was sent. Echoing it locally keeps the
     * conversation readable.
     *
     * Dropped again if the app later posts the same text back as
     * part of the exchange, so it cannot appear twice.
     */
    var sentLocally by remember(
        notification.key
    ) {
        mutableStateOf<List<NotificationMessage>>(
            emptyList()
        )
    }

    val conversation =
        remember(
            notification.messages,
            sentLocally
        ) {

            val known =
                notification
                    .messages
                    .filter {
                        it.fromUser
                    }
                    .map {
                        it.text.trim()
                    }
                    .toSet()

            (
                    notification.messages +
                            sentLocally.filterNot {
                                it.text.trim() in known
                            }
                    )
                .sortedBy {
                    it.timestamp
                }
        }

    val listState =
        rememberLazyListState()

    val focusRequester =
        remember {
            FocusRequester()
        }

    val appIcon =
        rememberAppIconBitmap(
            notification.packageName
        )

    BackHandler {
        onBack()
    }

    /*
     * Focus lands in the reply field as the thread opens, so typing
     * starts replying immediately. Without this the keyboard went to
     * the command bar underneath and a reply became a prompt.
     *
     * The small delay lets the overlay finish composing; requesting
     * focus in the same frame as the field appears does not take.
     */
    LaunchedEffect(
        notification.key,
        notification.canReply
    ) {

        if (
            notification.canReply
        ) {

            delay(
                60
            )

            runCatching {
                focusRequester
                    .requestFocus()
            }
        }
    }

    /*
     * Opens on the newest message, and follows new ones as they
     * arrive while the thread is on screen.
     */
    LaunchedEffect(
        conversation.size
    ) {

        if (
            conversation.isNotEmpty()
        ) {
            listState.animateScrollToItem(
                conversation.lastIndex
            )
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
                        onBack()
                    }
                },
        contentAlignment =
            Alignment.Center
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    /*
                     * Insets for the camera before the margin is
                     * applied, so the header never sits under the
                     * cutout however the device reports it.
                     */
                    .windowInsetsPadding(
                        WindowInsets.displayCutout
                    )
                    .padding(
                        horizontal = 9.dp,
                        vertical = 12.dp
                    )
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
                    .padding(
                        horizontal = 11.dp,
                        vertical = 10.dp
                    )
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                LauncherBackButton(
                    onBack = onBack
                )

                Spacer(
                    modifier = Modifier.width(7.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            notification.title
                                .ifBlank {
                                    notification.appName
                                }
                                .lowercase(),
                        color = PrimaryText,
                        fontSize = 15.sp,
                        lineHeight = 16.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text =
                            "via " + notification.appName.lowercase(),
                        color = TertiaryText,
                        fontSize = 7.5.sp,
                        lineHeight = 8.sp,
                        fontFamily = InterfaceFont,
                        maxLines = 1
                    )
                }

                if (
                    appIcon != null
                ) {

                    Image(
                        bitmap = appIcon,
                        contentDescription = notification.appName,
                        modifier =
                            Modifier
                                .size(20.dp)
                                .clickable {
                                    onOpenInApp()
                                }
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            LazyColumn(
                state = listState,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        /*
                         * Fills the card, which is what pins the
                         * reply field to the bottom edge rather than
                         * leaving it floating under the messages.
                         */
                        .weight(
                            1f
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(4.dp)
            ) {

                itemsIndexed(
                    items = conversation
                ) { _, message ->

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            if (message.fromUser) {
                                Arrangement.End
                            } else {
                                Arrangement.Start
                            }
                    ) {

                        Column(
                            modifier =
                                Modifier
                                    .widthIn(max = 215.dp)
                                    .background(
                                        if (message.fromUser) {
                                            AccentOrange
                                        } else {
                                            Color(0xFFE3E3E3)
                                        },
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(
                                        horizontal = 9.dp,
                                        vertical = 6.dp
                                    )
                        ) {

                            /*
                             * No sender label: the header already
                             * names the conversation, and in a
                             * one-to-one thread the side of the
                             * screen says the rest.
                             */
                            Text(
                                text = message.text,
                                color = Color.Black,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                fontFamily = InterfaceFont
                            )
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            if (
                notification.canReply
            ) {

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 34.dp)
                            .background(
                                InputSurface,
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                0.75.dp,
                                BorderGray,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = ">",
                        color = AccentOrange,
                        fontSize = 10.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    fun sendReply() {

                        val message =
                            replyText.trim()

                        if (
                            message.isEmpty()
                        ) {
                            return
                        }

                        NotificationCenter
                            .sendReply(
                                context = context,
                                key = notification.key,
                                message = message
                            )

                        sentLocally =
                            sentLocally +
                                    NotificationMessage(
                                        sender = "",
                                        text = message,
                                        timestamp =
                                            System.currentTimeMillis(),
                                        fromUser = true
                                    )

                        replyText = ""
                    }

                    BasicTextField(
                        value = replyText,
                        onValueChange = {
                            replyText =
                                it.replace("\n", " ")
                        },
                        singleLine = true,
                        textStyle =
                            TextStyle(
                                color = PrimaryText,
                                fontSize = 10.sp,
                                lineHeight = 10.sp,
                                fontFamily = InterfaceFont
                            ),
                        cursorBrush =
                            SolidColor(AccentOrange),
                        modifier =
                            Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                /*
                                 * Enter sends, so a reply needs no
                                 * screen contact at all on a device
                                 * with a keyboard.
                                 */
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

                    Text(
                        text = "send",
                        color = AccentOrange,
                        fontSize = 9.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.Medium,
                        modifier =
                            Modifier
                                .clickable {
                                    sendReply()
                                }
                                .padding(
                                    horizontal = 5.dp,
                                    vertical = 3.dp
                                )
                    )
                }

            } else {

                Text(
                    text = "this app does not allow replies here",
                    color = TertiaryText,
                    fontSize = 8.sp,
                    fontFamily = InterfaceFont
                )
            }

        }
    }
}