package com.forrest.titanlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat


/*
 * BATTERY
 *
 * The header pill and the card behind it.
 *
 * Battery saver cannot be switched on by an ordinary app: setting it
 * needs WRITE_SECURE_SETTINGS, which is granted to system apps and
 * adb only. Reading it is allowed, so the card reports the real state
 * and hands off to the system screen to change it rather than
 * pretending to own a switch it does not.
 */


internal data class BatterySnapshot(
    val level: Int,
    val charging: Boolean,
    val powerSave: Boolean
)


@Composable
internal fun rememberBatterySnapshot(): BatterySnapshot {

    val context =
        LocalContext.current

    val powerManager =
        remember {
            context.getSystemService(
                Context.POWER_SERVICE
            ) as? PowerManager
        }

    var snapshot by remember {
        mutableStateOf(
            BatterySnapshot(
                level = 0,
                charging = false,
                powerSave = false
            )
        )
    }

    DisposableEffect(Unit) {

        fun readFrom(
            intent: Intent?
        ): BatterySnapshot {

            val level =
                intent
                    ?.getIntExtra(
                        BatteryManager.EXTRA_LEVEL,
                        -1
                    )
                    ?: -1

            val scale =
                intent
                    ?.getIntExtra(
                        BatteryManager.EXTRA_SCALE,
                        -1
                    )
                    ?: -1

            val percent =
                if (
                    level >= 0 &&
                    scale > 0
                ) {
                    (level * 100) / scale
                } else {
                    0
                }

            val status =
                intent
                    ?.getIntExtra(
                        BatteryManager.EXTRA_STATUS,
                        -1
                    )
                    ?: -1

            val charging =
                status ==
                        BatteryManager.BATTERY_STATUS_CHARGING ||
                        status ==
                        BatteryManager.BATTERY_STATUS_FULL

            return BatterySnapshot(
                level =
                    percent,
                charging =
                    charging,
                powerSave =
                    powerManager
                        ?.isPowerSaveMode
                        ?: false
            )
        }

        /*
         * ACTION_BATTERY_CHANGED is sticky, so registering returns
         * the current state immediately rather than waiting for the
         * next change.
         */
        val batteryReceiver =
            object :
                BroadcastReceiver() {

                override fun onReceive(
                    received: Context?,
                    intent: Intent?
                ) {
                    snapshot =
                        readFrom(
                            intent
                        )
                }
            }

        val sticky =
            runCatching {
                ContextCompat.registerReceiver(
                    context,
                    batteryReceiver,
                    IntentFilter(
                        Intent.ACTION_BATTERY_CHANGED
                    ),
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            }
                .getOrNull()

        snapshot =
            readFrom(
                sticky
            )

        /*
         * Battery saver changes do not come through the battery
         * broadcast, so it needs its own.
         */
        val powerSaveReceiver =
            object :
                BroadcastReceiver() {

                override fun onReceive(
                    received: Context?,
                    intent: Intent?
                ) {
                    snapshot =
                        snapshot.copy(
                            powerSave =
                                powerManager
                                    ?.isPowerSaveMode
                                    ?: false
                        )
                }
            }

        runCatching {
            ContextCompat.registerReceiver(
                context,
                powerSaveReceiver,
                IntentFilter(
                    PowerManager.ACTION_POWER_SAVE_MODE_CHANGED
                ),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        onDispose {

            runCatching {
                context.unregisterReceiver(
                    batteryReceiver
                )
            }

            runCatching {
                context.unregisterReceiver(
                    powerSaveReceiver
                )
            }
        }
    }

    return snapshot
}


/*
 * The header button. Same 30.dp circle as HeaderIconButton so the
 * four sit as one row of equal buttons; the level goes inside it
 * without a percent sign, which would not fit legibly.
 *
 * Accent means "not on plain battery": charging, or battery saver
 * engaged.
 */
@Composable
internal fun HeaderBatteryButton(
    snapshot: BatterySnapshot,
    onClick: () -> Unit
) {

    val highlighted =
        snapshot.charging ||
                snapshot.powerSave

    Box(
        modifier =
            Modifier
                .size(
                    30.dp
                )
                .background(
                    SurfaceBlack,
                    CircleShape
                )
                .border(
                    0.8.dp,
                    if (
                        highlighted
                    ) {
                        AccentOrange
                    } else {
                        BorderGray
                    },
                    CircleShape
                )
                .clickable {
                    onClick()
                },
        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                snapshot.level
                    .toString(),
            color =
                if (
                    highlighted
                ) {
                    AccentOrange
                } else {
                    PrimaryText
                },
            fontSize =
                9.sp,
            lineHeight =
                9.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.Medium,
            maxLines =
                1
        )
    }
}


@Composable
internal fun BatteryOverlayCard(
    snapshot: BatterySnapshot,
    onDismiss: () -> Unit
) {

    val context =
        LocalContext.current

    var notice by remember {
        mutableStateOf(
            ""
        )
    }

    fun openBatterySaverSettings() {

        val opened =
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_BATTERY_SAVER_SETTINGS
                    )
                )
                true
            }
                .getOrDefault(
                    false
                )

        if (
            !opened
        ) {

            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_SETTINGS
                    )
                )
            }
                .onFailure {
                    notice =
                        "could not open settings"
                }
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
                            event.type ==
                            KeyEventType.KeyDown &&
                            event.key == Key.Escape
                        ) {
                            onDismiss()
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

            Text(
                text =
                    "battery",
                color =
                    PrimaryText,
                fontSize =
                    14.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Medium
            )

            Spacer(
                modifier =
                    Modifier
                        .height(
                            8.dp
                        )
            )

            Row(
                verticalAlignment =
                    Alignment.Bottom
            ) {

                Text(
                    text =
                        snapshot.level
                            .toString(),
                    color =
                        if (
                            snapshot.charging
                        ) {
                            AccentOrange
                        } else {
                            PrimaryText
                        },
                    fontSize =
                        30.sp,
                    lineHeight =
                        30.sp,
                    fontFamily =
                        InterfaceFont,
                    fontWeight =
                        FontWeight.Medium
                )

                Text(
                    text =
                        "%",
                    color =
                        SecondaryText,
                    fontSize =
                        13.sp,
                    fontFamily =
                        InterfaceFont,
                    modifier =
                        Modifier
                            .padding(
                                bottom = 2.dp,
                                start = 2.dp
                            )
                )
            }

            Spacer(
                modifier =
                    Modifier
                        .height(
                            3.dp
                        )
            )

            Text(
                text =
                    if (
                        snapshot.charging
                    ) {
                        "charging"
                    } else {
                        "on battery"
                    },
                color =
                    SecondaryText,
                fontSize =
                    9.5.sp,
                fontFamily =
                    InterfaceFont
            )

            Spacer(
                modifier =
                    Modifier
                        .height(
                            9.dp
                        )
            )

            /*
             * Reports the real state, then hands off. An app cannot
             * set battery saver itself.
             */
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
                            if (
                                snapshot.powerSave
                            ) {
                                AccentOrange
                            } else {
                                BorderGray
                            },
                            RoundedCornerShape(
                                10.dp
                            )
                        )
                        .clickable {
                            openBatterySaverSettings()
                        }
                        .padding(
                            horizontal = 10.dp
                        ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(
                    imageVector =
                        Icons.Outlined.BatterySaver,
                    contentDescription =
                        "low power mode",
                    tint =
                        if (
                            snapshot.powerSave
                        ) {
                            AccentOrange
                        } else {
                            SecondaryText
                        },
                    modifier =
                        Modifier
                            .size(
                                17.dp
                            )
                )

                Spacer(
                    modifier =
                        Modifier
                            .width(
                                9.dp
                            )
                )

                /*
                 * The label itself carries the state: accent when
                 * battery saver is engaged, normal when it is not.
                 */
                Text(
                    text =
                        "low power mode",
                    color =
                        if (
                            snapshot.powerSave
                        ) {
                            AccentOrange
                        } else {
                            PrimaryText
                        },
                    fontSize =
                        10.sp,
                    fontFamily =
                        InterfaceFont
                )
            }

            Spacer(
                modifier =
                    Modifier
                        .height(
                            5.dp
                        )
            )

            Text(
                text =
                    "opens android settings",
                color =
                    TertiaryText,
                fontSize =
                    8.sp,
                fontFamily =
                    InterfaceFont
            )

            if (
                notice.isNotBlank()
            ) {

                Spacer(
                    modifier =
                        Modifier
                            .height(
                                6.dp
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