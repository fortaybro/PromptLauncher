package com.forrest.titanlauncher

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


/*
 * QUICK TOGGLES
 *
 * Three device states that are otherwise several taps deep in system
 * settings. Reached from the tune button in the home header.
 *
 * Do Not Disturb and silent ringer both require notification policy
 * access. Prompt Launcher already asks for notification listener
 * access during onboarding, which on most builds carries policy
 * access with it. When it does not, tapping the toggle sends the user
 * to the system grant screen rather than failing silently.
 */


private const val QuickTogglePrefsName =
    "prompt_launcher_quick_toggles"

private const val KeyRingerBeforeDnd =
    "ringer_before_dnd"

/*
 * Android couples silent and zen: setting RINGER_MODE_SILENT moves the
 * interruption filter off ALL on its own. Reading the filter alone
 * therefore cannot tell "the user asked for Do Not Disturb" apart from
 * "the ringer is muted". This flag records the former so the card can
 * show silent without claiming Do Not Disturb is on.
 */
private const val KeyDndFromCard =
    "dnd_from_card"


private fun findTorchCameraId(
    cameraManager: CameraManager
): String? {

    return runCatching {

        val ids =
            cameraManager.cameraIdList

        ids.firstOrNull { id ->

            val characteristics =
                cameraManager
                    .getCameraCharacteristics(
                        id
                    )

            characteristics.get(
                CameraCharacteristics.FLASH_INFO_AVAILABLE
            ) == true &&
                    characteristics.get(
                        CameraCharacteristics.LENS_FACING
                    ) == CameraCharacteristics.LENS_FACING_BACK
        }
            ?: ids.firstOrNull { id ->

                cameraManager
                    .getCameraCharacteristics(
                        id
                    )
                    .get(
                        CameraCharacteristics.FLASH_INFO_AVAILABLE
                    ) == true
            }
    }
        .getOrNull()
}


/*
 * True when the phone is not in its plain, audible state: Do Not
 * Disturb is on, or the ringer is anything other than normal.
 *
 * Reads the same signals as the toggles card, including the
 * dnd_from_card flag, so the header button and the card can never
 * disagree about whether Do Not Disturb is on.
 */
@Composable
internal fun rememberSoundModeActive(): Boolean {

    val context =
        LocalContext.current

    val audioManager =
        remember {
            context.getSystemService(
                Context.AUDIO_SERVICE
            ) as? AudioManager
        }

    val notificationManager =
        remember {
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as? NotificationManager
        }

    val togglePreferences =
        remember {
            context.getSharedPreferences(
                QuickTogglePrefsName,
                Context.MODE_PRIVATE
            )
        }

    var active by remember {
        mutableStateOf(
            false
        )
    }

    fun readState(): Boolean {

        val ringer =
            audioManager
                ?.ringerMode
                ?: AudioManager.RINGER_MODE_NORMAL

        val filter =
            notificationManager
                ?.currentInterruptionFilter
                ?: NotificationManager.INTERRUPTION_FILTER_ALL

        val requestedFromCard =
            togglePreferences
                .getBoolean(
                    KeyDndFromCard,
                    false
                )

        val systemZenActive =
            filter !=
                    NotificationManager.INTERRUPTION_FILTER_ALL &&
                    filter !=
                    NotificationManager.INTERRUPTION_FILTER_UNKNOWN

        val zenIsJustSilence =
            ringer ==
                    AudioManager.RINGER_MODE_SILENT &&
                    !requestedFromCard

        val dndOn =
            systemZenActive &&
                    !zenIsJustSilence

        return dndOn ||
                ringer !=
                AudioManager.RINGER_MODE_NORMAL
    }

    /*
     * The system broadcasts both changes, so the button stays correct
     * even when the change came from the volume keys or the shade
     * rather than from this launcher.
     */
    DisposableEffect(Unit) {

        active =
            readState()

        val receiver =
            object :
                BroadcastReceiver() {

                override fun onReceive(
                    received: Context?,
                    intent: Intent?
                ) {
                    active =
                        readState()
                }
            }

        val intentFilter =
            IntentFilter()
                .apply {

                    addAction(
                        AudioManager.RINGER_MODE_CHANGED_ACTION
                    )

                    addAction(
                        NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED
                    )
                }

        runCatching {
            ContextCompat.registerReceiver(
                context,
                receiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }

        onDispose {
            runCatching {
                context.unregisterReceiver(
                    receiver
                )
            }
        }
    }

    return active
}


@Composable
internal fun QuickTogglesOverlayCard(
    onDismiss: () -> Unit
) {

    val context =
        LocalContext.current

    val notificationManager =
        remember {
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as? NotificationManager
        }

    val audioManager =
        remember {
            context.getSystemService(
                Context.AUDIO_SERVICE
            ) as? AudioManager
        }

    val cameraManager =
        remember {
            context.getSystemService(
                Context.CAMERA_SERVICE
            ) as? CameraManager
        }

    val togglePreferences =
        remember {
            context.getSharedPreferences(
                QuickTogglePrefsName,
                Context.MODE_PRIVATE
            )
        }

    val torchCameraId =
        remember(
            cameraManager
        ) {
            cameraManager
                ?.let(
                    ::findTorchCameraId
                )
        }

    var dndOn by remember {
        mutableStateOf(
            false
        )
    }

    var ringerMode by remember {
        mutableStateOf(
            AudioManager.RINGER_MODE_NORMAL
        )
    }

    var torchOn by remember {
        mutableStateOf(
            false
        )
    }

    var notice by remember {
        mutableStateOf(
            ""
        )
    }

    fun refreshSystemState() {

        val filter =
            notificationManager
                ?.currentInterruptionFilter
                ?: NotificationManager.INTERRUPTION_FILTER_ALL

        val currentRinger =
            audioManager
                ?.ringerMode
                ?: AudioManager.RINGER_MODE_NORMAL

        val requestedFromCard =
            togglePreferences
                .getBoolean(
                    KeyDndFromCard,
                    false
                )

        ringerMode =
            currentRinger

        val systemZenActive =
            filter !=
                    NotificationManager.INTERRUPTION_FILTER_ALL &&
                    filter !=
                    NotificationManager.INTERRUPTION_FILTER_UNKNOWN

        /*
         * Zen that exists only because the ringer is muted is not Do
         * Not Disturb as far as this card is concerned.
         */
        val zenIsJustSilence =
            currentRinger ==
                    AudioManager.RINGER_MODE_SILENT &&
                    !requestedFromCard

        dndOn =
            systemZenActive &&
                    !zenIsJustSilence
    }

    LaunchedEffect(Unit) {
        refreshSystemState()
    }

    /*
     * The torch callback reports the real hardware state, so the tile
     * stays correct even if something else turns the light on or off.
     */
    DisposableEffect(
        cameraManager,
        torchCameraId
    ) {

        val callback =
            object :
                CameraManager.TorchCallback() {

                override fun onTorchModeChanged(
                    cameraId: String,
                    enabled: Boolean
                ) {
                    if (
                        cameraId == torchCameraId
                    ) {
                        torchOn = enabled
                    }
                }

                override fun onTorchModeUnavailable(
                    cameraId: String
                ) {
                    if (
                        cameraId == torchCameraId
                    ) {
                        torchOn = false
                    }
                }
            }

        runCatching {
            cameraManager
                ?.registerTorchCallback(
                    callback,
                    null
                )
        }

        onDispose {
            runCatching {
                cameraManager
                    ?.unregisterTorchCallback(
                        callback
                    )
            }
        }
    }

    fun openPolicyAccessSettings() {

        notice =
            "grant access, then try again"

        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                )
            )
        }
    }

    fun toggleDoNotDisturb() {

        val manager =
            notificationManager
                ?: return

        if (
            !manager.isNotificationPolicyAccessGranted
        ) {
            openPolicyAccessSettings()
            return
        }

        val turningOn =
            !dndOn

        runCatching {

            if (
                turningOn
            ) {

                /*
                 * Remember the ringer we are leaving so switching Do
                 * Not Disturb back off can put it back.
                 */
                togglePreferences
                    .edit()
                    .putInt(
                        KeyRingerBeforeDnd,
                        audioManager
                            ?.ringerMode
                            ?: AudioManager.RINGER_MODE_NORMAL
                    )
                    .putBoolean(
                        KeyDndFromCard,
                        true
                    )
                    .apply()

                manager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY
                )

                audioManager?.ringerMode =
                    AudioManager.RINGER_MODE_SILENT

            } else {

                togglePreferences
                    .edit()
                    .putBoolean(
                        KeyDndFromCard,
                        false
                    )
                    .apply()

                manager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_ALL
                )

                audioManager?.ringerMode =
                    togglePreferences
                        .getInt(
                            KeyRingerBeforeDnd,
                            AudioManager.RINGER_MODE_NORMAL
                        )
            }

            notice = ""
        }
            .onFailure {
                notice =
                    "could not change dnd"
            }

        refreshSystemState()
    }

    fun cycleRinger() {

        val manager =
            audioManager
                ?: return

        val nextMode =
            when (
                ringerMode
            ) {
                AudioManager.RINGER_MODE_NORMAL ->
                    AudioManager.RINGER_MODE_VIBRATE

                AudioManager.RINGER_MODE_VIBRATE ->
                    AudioManager.RINGER_MODE_SILENT

                else ->
                    AudioManager.RINGER_MODE_NORMAL
            }

        val goingSilent =
            nextMode ==
                    AudioManager.RINGER_MODE_SILENT

        /*
         * Silent is its own state and does not switch Do Not Disturb
         * on, but Android still requires policy access to set it.
         */
        if (
            goingSilent &&
            notificationManager
                ?.isNotificationPolicyAccessGranted != true
        ) {
            openPolicyAccessSettings()
            return
        }

        runCatching {

            /*
             * Moving off silent clears any zen that was in force,
             * whether this card asked for it or the mute implied it.
             */
            if (
                !goingSilent
            ) {

                togglePreferences
                    .edit()
                    .putBoolean(
                        KeyDndFromCard,
                        false
                    )
                    .apply()

                notificationManager
                    ?.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_ALL
                    )
            }

            manager.ringerMode =
                nextMode

            notice = ""
        }
            .onFailure {
                notice =
                    "could not change ringer"
            }

        refreshSystemState()
    }

    fun toggleTorch() {

        val manager =
            cameraManager

        val cameraId =
            torchCameraId

        if (
            manager == null ||
            cameraId == null
        ) {
            notice =
                "no flash on this device"
            return
        }

        runCatching {
            manager.setTorchMode(
                cameraId,
                !torchOn
            )

            notice = ""
        }
            .onFailure {
                notice =
                    "flashlight unavailable"
            }
    }

    BackHandler {
        onDismiss()
    }

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
                                toggleDoNotDisturb()
                                true
                            }

                            Key.Two -> {
                                toggleTorch()
                                true
                            }

                            Key.Three -> {
                                cycleRinger()
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
                "toggles",
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

        QuickToggleTile(
            icon =
                Icons.Outlined.DoNotDisturbOn,
            label =
                "do not disturb",
            state =
                if (
                    dndOn
                ) {
                    "on"
                } else {
                    "off"
                },
            active =
                dndOn,
            onClick = {
                toggleDoNotDisturb()
            }
        )

        Spacer(
            modifier =
                Modifier
                    .height(
                        7.dp
                    )
        )

        QuickToggleTile(
            icon =
                if (
                    torchOn
                ) {
                    Icons.Outlined.FlashOn
                } else {
                    Icons.Outlined.FlashOff
                },
            label =
                "flashlight",
            state =
                if (
                    torchOn
                ) {
                    "on"
                } else {
                    "off"
                },
            active =
                torchOn,
            onClick = {
                toggleTorch()
            }
        )

        Spacer(
            modifier =
                Modifier
                    .height(
                        7.dp
                    )
        )

        QuickToggleTile(
            icon =
                when (
                    ringerMode
                ) {
                    AudioManager.RINGER_MODE_SILENT ->
                        Icons.Outlined.VolumeOff

                    AudioManager.RINGER_MODE_VIBRATE ->
                        Icons.Outlined.Vibration

                    else ->
                        Icons.Outlined.VolumeUp
                },
            label =
                "ringer",
            state =
                when (
                    ringerMode
                ) {
                    AudioManager.RINGER_MODE_SILENT ->
                        "silent"

                    AudioManager.RINGER_MODE_VIBRATE ->
                        "vibrate"

                    else ->
                        "normal"
                },
            active =
                ringerMode !=
                        AudioManager.RINGER_MODE_NORMAL,
            onClick = {
                cycleRinger()
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


@Composable
private fun QuickToggleTile(
    icon: ImageVector,
    label: String,
    state: String?,
    active: Boolean,
    onClick: () -> Unit,
    labelColor: Color? = null
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    44.dp
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
                        active
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
                if (
                    active
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

        Text(
            text =
                label,
            color =
                labelColor
                    ?: PrimaryText,
            fontSize =
                10.sp,
            fontFamily =
                InterfaceFont
        )

        Spacer(
            modifier =
                Modifier
                    .weight(1f)
        )

        /*
         * Tiles whose label already carries the state pass null here
         * rather than repeating it on the right.
         */
        if (
            state != null
        ) {

            Text(
                text =
                    state,
                color =
                    if (
                        active
                    ) {
                        AccentOrange
                    } else {
                        TertiaryText
                    },
                fontSize =
                    9.sp,
                fontFamily =
                    InterfaceFont
            )
        }
    }
}