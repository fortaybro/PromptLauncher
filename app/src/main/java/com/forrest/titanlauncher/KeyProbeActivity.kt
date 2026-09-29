package com.forrest.titanlauncher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.forrest.titanlauncher.ui.theme.TitanLauncherTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


/*
 * KEY PROBE
 *
 * A diagnostic screen for mapping the physical keyboard of a device
 * Prompt Launcher has never run on.
 *
 * Every key event is intercepted at dispatchKeyEvent, which is the
 * earliest point the Activity sees input. Nothing is consumed by the
 * view hierarchy first, so modifier keys, the symbol key, and any
 * vendor keys all show up exactly as the OS delivers them.
 *
 * IMPORTANT: this screen swallows every key event, including BACK.
 * Leave it with the on-screen EXIT button. The HOME key is handled by
 * the system and is not dispatched here, so it always works as an
 * escape hatch.
 */


private val ProbeBackground =
    Color(0xFF000000)

private val ProbeBorder =
    Color(0xFF2E2E2E)

private val ProbeSurface =
    Color(0xFF101010)

private val ProbeAccent =
    Color(0xFFFF7A1A)

private val ProbePrimaryText =
    Color(0xFFF2F2F2)

private val ProbeSecondaryText =
    Color(0xFF9A9A9A)

private const val MaxProbeEntries =
    400


internal data class ProbeEntry(
    val index: Int,
    val timestamp: Long,
    val headline: String,
    val detail: String
)


class KeyProbeActivity :
    ComponentActivity() {

    private val entries =
        mutableStateListOf<ProbeEntry>()

    private var sequence =
        0

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

        hideSystemBars()

        setContent {

            TitanLauncherTheme {

                TitanUiScale {

                    KeyProbeScreen(
                        entries =
                            entries,
                        deviceReport =
                            buildDeviceReport(),
                        onClear = {
                            entries.clear()
                            sequence = 0
                        },
                        onCopy = {
                            copyReportToClipboard()
                        },
                        onShare = {
                            shareReport()
                        },
                        onExit = {
                            finish()
                        }
                    )
                }
            }
        }
    }


    /*
     * Every key event is recorded and consumed.
     */
    override fun dispatchKeyEvent(
        event: KeyEvent
    ): Boolean {

        recordKeyEvent(
            event
        )

        return true
    }


    /*
     * The Titan keyboard surface is touch sensitive. If its scroll and
     * cursor gestures arrive as motion events rather than key events,
     * they land here.
     */
    override fun dispatchGenericMotionEvent(
        event: MotionEvent
    ): Boolean {

        recordMotionEvent(
            event
        )

        return true
    }


    private fun recordKeyEvent(
        event: KeyEvent
    ) {

        val action =
            when (
                event.action
            ) {
                KeyEvent.ACTION_DOWN ->
                    "DOWN"

                KeyEvent.ACTION_UP ->
                    "UP"

                KeyEvent.ACTION_MULTIPLE ->
                    "MULTIPLE"

                else ->
                    "ACTION_" + event.action
            }

        val keyName =
            KeyEvent.keyCodeToString(
                event.keyCode
            )

        val headline =
            action +
                    "  " +
                    keyName +
                    "  (" +
                    event.keyCode +
                    ")"

        val unicodeValue =
            event.getUnicodeChar(
                event.metaState
            )

        val printable =
            if (
                unicodeValue > 0
            ) {
                "'" +
                        unicodeValue
                            .toChar() +
                        "'"
            } else {
                "none"
            }

        val detail =
            buildString {

                append("scan=")
                append(event.scanCode)

                append("  repeat=")
                append(event.repeatCount)

                append("  char=")
                append(printable)

                append("\nmeta=")
                append(
                    describeModifiers(
                        event
                    )
                )

                append("  metaState=0x")
                append(
                    Integer.toHexString(
                        event.metaState
                    )
                )

                append("\nsource=")
                append(
                    describeSources(
                        event.source
                    )
                )

                append("\ndevice=")
                append(
                    event.device?.name
                        ?: "unknown"
                )

                append("  id=")
                append(event.deviceId)
            }

        appendEntry(
            headline,
            detail
        )
    }


    private fun recordMotionEvent(
        event: MotionEvent
    ) {

        val axisLabels =
            listOf(
                MotionEvent.AXIS_X to "X",
                MotionEvent.AXIS_Y to "Y",
                MotionEvent.AXIS_HSCROLL to "HSCROLL",
                MotionEvent.AXIS_VSCROLL to "VSCROLL",
                MotionEvent.AXIS_RELATIVE_X to "RELATIVE_X",
                MotionEvent.AXIS_RELATIVE_Y to "RELATIVE_Y"
            )

        val activeAxes =
            axisLabels
                .mapNotNull {
                        (axis, label) ->

                    val value =
                        event.getAxisValue(
                            axis
                        )

                    if (
                        value == 0f
                    ) {
                        null
                    } else {
                        label +
                                "=" +
                                value
                    }
                }
                .joinToString("  ")
                .ifEmpty {
                    "no non-zero axes"
                }

        val headline =
            "MOTION  " +
                    MotionEvent.actionToString(
                        event.action
                    )

        val detail =
            buildString {

                append(activeAxes)

                append("\nsource=")
                append(
                    describeSources(
                        event.source
                    )
                )

                append("\ndevice=")
                append(
                    event.device?.name
                        ?: "unknown"
                )

                append("  id=")
                append(event.deviceId)
            }

        appendEntry(
            headline,
            detail
        )
    }


    private fun appendEntry(
        headline: String,
        detail: String
    ) {

        sequence += 1

        entries.add(
            0,
            ProbeEntry(
                index =
                    sequence,
                timestamp =
                    System.currentTimeMillis(),
                headline =
                    headline,
                detail =
                    detail
            )
        )

        while (
            entries.size > MaxProbeEntries
        ) {
            entries.removeAt(
                entries.size - 1
            )
        }
    }


    private fun describeModifiers(
        event: KeyEvent
    ): String {

        val active =
            mutableListOf<String>()

        if (event.isShiftPressed) active.add("SHIFT")
        if (event.isAltPressed) active.add("ALT")
        if (event.isCtrlPressed) active.add("CTRL")
        if (event.isMetaPressed) active.add("META")
        if (event.isFunctionPressed) active.add("FN")
        if (event.isSymPressed) active.add("SYM")
        if (event.isCapsLockOn) active.add("CAPS_LOCK")
        if (event.isNumLockOn) active.add("NUM_LOCK")

        return if (
            active.isEmpty()
        ) {
            "none"
        } else {
            active.joinToString("+")
        }
    }


    private fun describeSources(
        source: Int
    ): String {

        val active =
            mutableListOf<String>()

        val candidates =
            listOf(
                InputDevice.SOURCE_KEYBOARD to "KEYBOARD",
                InputDevice.SOURCE_DPAD to "DPAD",
                InputDevice.SOURCE_GAMEPAD to "GAMEPAD",
                InputDevice.SOURCE_TOUCHSCREEN to "TOUCHSCREEN",
                InputDevice.SOURCE_MOUSE to "MOUSE",
                InputDevice.SOURCE_STYLUS to "STYLUS",
                InputDevice.SOURCE_TRACKBALL to "TRACKBALL",
                InputDevice.SOURCE_TOUCHPAD to "TOUCHPAD",
                InputDevice.SOURCE_JOYSTICK to "JOYSTICK",
                InputDevice.SOURCE_ROTARY_ENCODER to "ROTARY_ENCODER",
                InputDevice.SOURCE_TOUCH_NAVIGATION to "TOUCH_NAVIGATION"
            )

        candidates.forEach {
                (flag, label) ->

            if (
                source and flag == flag
            ) {
                active.add(
                    label
                )
            }
        }

        if (
            active.isEmpty()
        ) {
            active.add(
                "0x" +
                        Integer.toHexString(
                            source
                        )
            )
        }

        return active.joinToString("|")
    }


    private fun buildDeviceReport(): String {

        return buildString {

            append("INPUT DEVICES\n")

            val deviceIds =
                InputDevice.getDeviceIds()

            if (
                deviceIds.isEmpty()
            ) {
                append("\nnone reported")
                return@buildString
            }

            deviceIds.forEach {
                    id ->

                val device =
                    InputDevice.getDevice(
                        id
                    )

                if (
                    device != null
                ) {

                    append("\n")
                    append("id ")
                    append(id)
                    append("  ")
                    append(device.name)

                    append("\n  sources = ")
                    append(
                        describeSources(
                            device.sources
                        )
                    )

                    append("\n  keyboardType = ")
                    append(
                        when (
                            device.keyboardType
                        ) {
                            InputDevice.KEYBOARD_TYPE_ALPHABETIC ->
                                "ALPHABETIC"

                            InputDevice.KEYBOARD_TYPE_NON_ALPHABETIC ->
                                "NON_ALPHABETIC"

                            else ->
                                "NONE"
                        }
                    )

                    append("\n  virtual = ")
                    append(device.isVirtual)

                    append("\n")
                }
            }
        }
    }


    private fun buildFullReport(): String {

        val formatter =
            SimpleDateFormat(
                "HH:mm:ss.SSS",
                Locale.US
            )

        return buildString {

            append("PROMPT LAUNCHER KEY PROBE\n")

            append("build ")
            append(
                android.os.Build.MODEL
            )
            append(" / ")
            append(
                android.os.Build.MANUFACTURER
            )
            append(" / Android ")
            append(
                android.os.Build.VERSION.RELEASE
            )
            append(" / API ")
            append(
                android.os.Build.VERSION.SDK_INT
            )
            append("\n\n")

            append(
                buildDeviceReport()
            )

            append("\n\nEVENTS (newest first, ")
            append(entries.size)
            append(" captured)\n")

            entries.forEach {
                    entry ->

                append("\n#")
                append(entry.index)
                append("  ")
                append(
                    formatter.format(
                        Date(
                            entry.timestamp
                        )
                    )
                )
                append("  ")
                append(entry.headline)
                append("\n")
                append(
                    entry.detail
                        .prependIndent("    ")
                )
                append("\n")
            }
        }
    }


    private fun copyReportToClipboard() {

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as? ClipboardManager
                ?: return

        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                "Key probe",
                buildFullReport()
            )
        )
    }


    private fun shareReport() {

        val intent =
            Intent(
                Intent.ACTION_SEND
            ).apply {

                type =
                    "text/plain"

                putExtra(
                    Intent.EXTRA_SUBJECT,
                    "Prompt Launcher key probe"
                )

                putExtra(
                    Intent.EXTRA_TEXT,
                    buildFullReport()
                )
            }

        startActivity(
            Intent.createChooser(
                intent,
                "Send key probe"
            )
        )
    }


    private fun hideSystemBars() {

        val controller =
            WindowCompat.getInsetsController(
                window,
                window.decorView
            )

        controller.hide(
            WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.navigationBars()
        )
    }
}


@Composable
private fun KeyProbeScreen(
    entries: SnapshotStateList<ProbeEntry>,
    deviceReport: String,
    onClear: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onExit: () -> Unit
) {

    var showDevices by remember {
        mutableStateOf(
            false
        )
    }

    var actionNotice by remember {
        mutableStateOf(
            ""
        )
    }

    val timeFormatter =
        remember {
            SimpleDateFormat(
                "HH:mm:ss.SSS",
                Locale.US
            )
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    ProbeBackground
                )
                .padding(
                    10.dp
                )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    "KEY PROBE",
                color =
                    ProbeAccent,
                fontSize =
                    13.sp,
                fontWeight =
                    FontWeight.Bold,
                fontFamily =
                    FontFamily.Monospace
            )

            Spacer(
                modifier =
                    Modifier
                        .weight(1f)
            )

            Text(
                text =
                    entries.size
                        .toString() +
                            " events",
                color =
                    ProbeSecondaryText,
                fontSize =
                    11.sp,
                fontFamily =
                    FontFamily.Monospace
            )
        }

        Spacer(
            modifier =
                Modifier
                    .height(6.dp)
        )

        Text(
            text =
                if (
                    showDevices
                ) {
                    "Input devices reported by the OS."
                } else {
                    "Press every key, then COPY."
                },
            color =
                ProbeSecondaryText,
            fontSize =
                10.sp,
            fontFamily =
                FontFamily.Monospace
        )

        Spacer(
            modifier =
                Modifier
                    .height(8.dp)
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    5.dp
                )
        ) {

            ProbeButton(
                label =
                    if (
                        showDevices
                    ) {
                        "EVENTS"
                    } else {
                        "DEVICES"
                    },
                modifier =
                    Modifier
                        .weight(1f)
            ) {
                showDevices =
                    !showDevices

                actionNotice =
                    ""
            }

            ProbeButton(
                label =
                    "COPY",
                modifier =
                    Modifier
                        .weight(1f)
            ) {
                onCopy()

                actionNotice =
                    "Copied to clipboard"
            }

            ProbeButton(
                label =
                    "SHARE",
                modifier =
                    Modifier
                        .weight(1f)
            ) {
                onShare()
            }
        }

        Spacer(
            modifier =
                Modifier
                    .height(5.dp)
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(
                    5.dp
                )
        ) {

            ProbeButton(
                label =
                    "CLEAR",
                modifier =
                    Modifier
                        .weight(1f)
            ) {
                onClear()

                actionNotice =
                    ""
            }

            ProbeButton(
                label =
                    "EXIT",
                modifier =
                    Modifier
                        .weight(1f)
            ) {
                onExit()
            }
        }

        if (
            actionNotice.isNotBlank()
        ) {

            Spacer(
                modifier =
                    Modifier
                        .height(6.dp)
            )

            Text(
                text =
                    actionNotice,
                color =
                    ProbeAccent,
                fontSize =
                    10.sp,
                fontFamily =
                    FontFamily.Monospace
            )
        }

        Spacer(
            modifier =
                Modifier
                    .height(8.dp)
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(
                        1.dp,
                        ProbeBorder,
                        RoundedCornerShape(
                            8.dp
                        )
                    )
                    .padding(
                        8.dp
                    )
        ) {

            if (
                showDevices
            ) {

                Text(
                    text =
                        deviceReport,
                    color =
                        ProbePrimaryText,
                    fontSize =
                        10.sp,
                    fontFamily =
                        FontFamily.Monospace,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(
                                rememberScrollState()
                            )
                )
            } else if (
                entries.isEmpty()
            ) {

                Text(
                    text =
                        "Waiting for input.\n\nPress any key on the\nphysical keyboard.",
                    color =
                        ProbeSecondaryText,
                    fontSize =
                        11.sp,
                    fontFamily =
                        FontFamily.Monospace
                )
            } else {

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize(),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            6.dp
                        )
                ) {

                    items(
                        items =
                            entries,
                        key = {
                                entry ->

                            entry.index
                        }
                    ) { entry ->

                        ProbeEntryRow(
                            entry =
                                entry,
                            time =
                                timeFormatter.format(
                                    Date(
                                        entry.timestamp
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun ProbeEntryRow(
    entry: ProbeEntry,
    time: String
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    ProbeSurface,
                    RoundedCornerShape(
                        6.dp
                    )
                )
                .padding(
                    7.dp
                )
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
        ) {

            Text(
                text =
                    "#" +
                            entry.index,
                color =
                    ProbeSecondaryText,
                fontSize =
                    9.sp,
                fontFamily =
                    FontFamily.Monospace
            )

            Spacer(
                modifier =
                    Modifier
                        .weight(1f)
            )

            Text(
                text =
                    time,
                color =
                    ProbeSecondaryText,
                fontSize =
                    9.sp,
                fontFamily =
                    FontFamily.Monospace
            )
        }

        Spacer(
            modifier =
                Modifier
                    .height(3.dp)
        )

        Text(
            text =
                entry.headline,
            color =
                ProbeAccent,
            fontSize =
                11.sp,
            fontWeight =
                FontWeight.Bold,
            fontFamily =
                FontFamily.Monospace
        )

        Spacer(
            modifier =
                Modifier
                    .height(3.dp)
        )

        Text(
            text =
                entry.detail,
            color =
                ProbePrimaryText,
            fontSize =
                10.sp,
            fontFamily =
                FontFamily.Monospace
        )
    }
}


@Composable
private fun ProbeButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    Box(
        modifier =
            modifier
                .height(34.dp)
                .background(
                    ProbeSurface,
                    RoundedCornerShape(
                        6.dp
                    )
                )
                .border(
                    1.dp,
                    ProbeBorder,
                    RoundedCornerShape(
                        6.dp
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
                ProbePrimaryText,
            fontSize =
                10.sp,
            fontWeight =
                FontWeight.Bold,
            fontFamily =
                FontFamily.Monospace
        )
    }
}