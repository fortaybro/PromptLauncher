package com.forrest.titanlauncher

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/*
 * CALL ACTIONS
 *
 * Opening a call in the hub asks what to do with it rather than
 * dialling immediately. A missed call is just as often answered with a
 * text as with a callback.
 */

private const val MeetPackageName =
    "com.google.android.apps.tachyon"


@Composable
internal fun CallActionsOverlay(
    displayName: String,
    phoneNumber: String,
    onText: () -> Unit,
    onCall: () -> Unit,
    onDismiss: () -> Unit
) {

    val context =
        LocalContext.current

    var notice by remember {
        mutableStateOf(
            ""
        )
    }

    fun startVideoCall() {

        /*
         * Google Meet exposes no public intent for placing a call to a
         * given number, so this opens the app rather than pretending
         * to dial. Falls back to a notice when Meet is absent.
         */
        val launchIntent =
            context
                .packageManager
                .getLaunchIntentForPackage(
                    MeetPackageName
                )

        if (
            launchIntent != null
        ) {

            runCatching {
                context.startActivity(
                    launchIntent
                )
            }
                .onFailure {
                    notice =
                        "could not open meet"
                }

            return
        }

        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://meet.google.com"
                    )
                )
            )
        }
            .onFailure {
                notice =
                    "meet not available"
            }
    }

    BackHandler {
        onDismiss()
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
            Alignment.Center
    ) {

        Column(
            modifier =
                Modifier
                    .widthIn(
                        max = 215.dp
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
                    .onPreviewKeyEvent { event ->

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
                                    onDismiss()
                                    true
                                }

                                Key.One -> {
                                    onText()
                                    true
                                }

                                Key.Two -> {
                                    onCall()
                                    true
                                }

                                Key.Three -> {
                                    startVideoCall()
                                    true
                                }

                                else ->
                                    false
                            }
                        }
                    }
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    )
        ) {

            Text(
                text =
                    displayName.lowercase(),
                color =
                    PrimaryText,
                fontSize =
                    13.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Medium,
                maxLines =
                    1,
                overflow =
                    TextOverflow.Ellipsis
            )

            if (
                displayName != phoneNumber
            ) {

                Spacer(
                    modifier =
                        Modifier
                            .height(
                                2.dp
                            )
                )

                Text(
                    text =
                        phoneNumber,
                    color =
                        TertiaryText,
                    fontSize =
                        8.sp,
                    fontFamily =
                        InterfaceFont,
                    maxLines =
                        1
                )
            }

            Spacer(
                modifier =
                    Modifier
                        .height(
                            9.dp
                        )
            )

            CallActionTile(
                icon =
                    Icons.Outlined.Message,
                label =
                    "text",
                onClick =
                    onText
            )

            Spacer(
                modifier =
                    Modifier
                        .height(
                            7.dp
                        )
            )

            CallActionTile(
                icon =
                    Icons.Outlined.Call,
                label =
                    "call back",
                onClick =
                    onCall
            )

            Spacer(
                modifier =
                    Modifier
                        .height(
                            7.dp
                        )
            )

            CallActionTile(
                icon =
                    Icons.Outlined.Videocam,
                label =
                    "video call",
                onClick = {
                    startVideoCall()
                }
            )

            if (
                notice.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier
                            .height(
                                8.dp
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
private fun CallActionTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    42.dp
                )
                .background(
                    InputSurface,
                    RoundedCornerShape(
                        10.dp
                    )
                )
                .border(
                    0.75.dp,
                    BorderGray,
                    RoundedCornerShape(
                        10.dp
                    )
                )
                .clickable {
                    onClick()
                }
                .padding(
                    horizontal = 10.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Icon(
            imageVector =
                icon,
            contentDescription =
                label,
            tint =
                PrimaryText,
            modifier =
                Modifier
                    .size(
                        16.dp
                    )
        )

        Spacer(
            modifier =
                Modifier
                    .width(
                        9.dp
                    )
        )

        Text(
            text =
                label,
            color =
                PrimaryText,
            fontSize =
                10.sp,
            fontFamily =
                InterfaceFont
        )
    }
}