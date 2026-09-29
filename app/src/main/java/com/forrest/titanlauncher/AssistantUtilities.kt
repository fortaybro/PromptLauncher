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
import androidx.compose.foundation.layout.PaddingValues
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
import com.forrest.titanlauncher.ai.AssistantSharing
import com.forrest.titanlauncher.ai.AssistantSharingDisclosure
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
fun GeminiSetupScreen(
    hasExistingKey: Boolean,
    sharing: AssistantSharing,
    onSharingChange: (AssistantSharing) -> Unit,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    var apiKey by remember {
        mutableStateOf(
            ""
        )
    }

    val focusRequester =
        remember {
            FocusRequester()
        }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
                .focusable()
                .verticalScroll(
                    rememberScrollState()
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
                            Key.Enter -> {
                                if (
                                    apiKey.isNotBlank()
                                ) {
                                    onSave(
                                        apiKey
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
                    "GEMINI SETUP",
                color =
                    PrimaryText,
                fontSize =
                    14.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Medium
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
                    10.dp
                )
        )

        Text(
            text =
                if (
                    hasExistingKey
                ) {
                    "replace your Gemini API key"
                } else {
                    "enter your Gemini API key"
                },
            color =
                SecondaryText,
            fontSize =
                8.sp,
            fontFamily =
                InterfaceFont
        )

        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )

        BasicTextField(
            value =
                apiKey,
            onValueChange = {
                apiKey =
                    it
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        34.dp
                    )
                    .background(
                        InputSurface,
                        RoundedCornerShape(
                            7.dp
                        )
                    )
                    .border(
                        0.75.dp,
                        BorderGray,
                        RoundedCornerShape(
                            7.dp
                        )
                    )
                    .padding(
                        horizontal =
                            8.dp,
                        vertical =
                            7.dp
                    )
                    .focusRequester(
                        focusRequester
                    ),
            singleLine =
                true,
            visualTransformation =
                PasswordVisualTransformation(),
            textStyle =
                TextStyle(
                    color =
                        PrimaryText,
                    fontSize =
                        9.sp,
                    fontFamily =
                        InterfaceFont
                ),
            cursorBrush =
                SolidColor(
                    PrimaryText
                )
        )

        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )

        /*
         * Disclosure before anything is enabled: what leaves the
         * phone, and a switch per source. All sources start off.
         */
        Text(
            text =
                "WHAT GETS SENT TO GOOGLE GEMINI",
            color =
                PrimaryText,
            fontSize =
                9.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.Medium
        )

        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )

        GeminiDisclosureText(
            AssistantSharingDisclosure.ALWAYS
        )

        GeminiDisclosureText(
            AssistantSharingDisclosure.DESTINATION
        )

        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )

        Text(
            text =
                "ALSO SEND (OFF UNTIL YOU TURN IT ON)",
            color =
                SecondaryText,
            fontSize =
                8.sp,
            fontFamily =
                InterfaceFont
        )

        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )

        SettingsToggleRow(
            label = "calendar",
            enabled = sharing.calendar,
            onToggle = {
                onSharingChange(
                    sharing.copy(
                        calendar = it
                    )
                )
            }
        )

        GeminiDisclosureText(
            AssistantSharingDisclosure.CALENDAR
        )

        SettingsToggleRow(
            label = "texts",
            enabled = sharing.texts,
            onToggle = {
                onSharingChange(
                    sharing.copy(
                        texts = it
                    )
                )
            }
        )

        GeminiDisclosureText(
            AssistantSharingDisclosure.TEXTS
        )

        SettingsToggleRow(
            label = "mail",
            enabled = sharing.mail,
            onToggle = {
                onSharingChange(
                    sharing.copy(
                        mail = it
                    )
                )
            }
        )

        GeminiDisclosureText(
            AssistantSharingDisclosure.MAIL
        )

        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )

    }
}

@Composable
private fun GeminiDisclosureText(
    text: String
) {
    Text(
        text =
            text,
        color =
            SecondaryText,
        fontSize =
            7.5.sp,
        lineHeight =
            10.sp,
        fontFamily =
            InterfaceFont,
        modifier =
            Modifier.padding(
                bottom =
                    5.dp
            )
    )
}

@Composable
fun LocalAssistantScreen(
    turns: List<AssistantTurn>,
    isThinking: Boolean,
    onSend: (String) -> Unit,
    onBack: () -> Unit
) {

    val focusRequester =
        remember {
            FocusRequester()
        }

    val listState =
        rememberLazyListState()

    var inputText by
    remember {
        mutableStateOf(
            ""
        )
    }

    LaunchedEffect(Unit) {

        focusRequester
            .requestFocus()
    }

    LaunchedEffect(
        turns.size,
        turns.lastOrNull()?.answer
    ) {

        if (
            turns.isNotEmpty()
        ) {

            listState
                .animateScrollToItem(
                    turns.lastIndex
                )
        }
    }

    /*
     * Keep AI Assist keyboard-first after every Gemini response.
     * When isThinking changes back to false, the physical keyboard
     * focus is explicitly returned to the reply field.
     */
    LaunchedEffect(
        isThinking
    ) {

        if (
            !isThinking
        ) {

            delay(
                30
            )

            focusRequester
                .requestFocus()
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
                        12.dp,
                    vertical =
                        10.dp
                )
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
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

            Text(
                text =
                    "ai assist",
                color =
                    PrimaryText,
                fontSize =
                    14.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Medium
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    7.dp
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
                    10.dp
                ),
            contentPadding =
                PaddingValues(
                    vertical =
                        4.dp
                )
        ) {

            itemsIndexed(
                items =
                    turns
            ) {
                    index,
                    turn ->

                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.End
                    ) {

                        /*
                         * Same bubble treatment as a sent SMS: accent
                         * fill, dark text, right aligned.
                         */
                        Text(
                            text =
                                turn.question,
                            color =
                                Color.Black,
                            fontSize =
                                11.8.sp,
                            lineHeight =
                                15.5.sp,
                            fontFamily =
                                InterfaceFont,
                            textAlign =
                                TextAlign.Start,
                            modifier =
                                Modifier
                                    .widthIn(
                                        max =
                                            255.dp
                                    )
                                    .background(
                                        color =
                                            AccentOrange,
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
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                5.dp
                            )
                    )

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.Start
                    ) {

                        val thinkingThisTurn =
                            isThinking &&
                                    index ==
                                    turns.lastIndex &&
                                    turn.answer ==
                                    "THINKING..."

                        /*
                         * Same bubble treatment as a received SMS.
                         */
                        Text(
                            text =
                                if (
                                    thinkingThisTurn
                                ) {
                                    "thinking..."
                                } else {
                                    turn.answer
                                },
                            color =
                                if (
                                    thinkingThisTurn
                                ) {
                                    Color(0xFF6B6B6B)
                                } else {
                                    Color.Black
                                },
                            fontSize =
                                11.8.sp,
                            lineHeight =
                                15.5.sp,
                            fontFamily =
                                InterfaceFont,
                            textAlign =
                                TextAlign.Start,
                            modifier =
                                Modifier
                                    .widthIn(
                                        max =
                                            255.dp
                                    )
                                    .background(
                                        color =
                                            Color(
                                                0xFFE3E3E3
                                            ),
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
                    }
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(
                    7.dp
                )
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        38.dp
                    )
                    .background(
                        InputSurface,
                        RoundedCornerShape(
                            9.dp
                        )
                    )
                    .border(
                        width =
                            0.9.dp,
                        color =
                            AccentOrange.copy(
                                alpha =
                                    0.72f
                            ),
                        shape =
                            RoundedCornerShape(
                                9.dp
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
                    15.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.width(
                        6.dp
                    )
            )

            BasicTextField(
                value =
                    inputText,
                onValueChange = {

                    inputText =
                        it
                },
                modifier =
                    Modifier
                        .weight(
                            1f
                        )
                        .focusRequester(
                            focusRequester
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

                                    Key.Enter -> {

                                        val text =
                                            inputText
                                                .trim()

                                        if (
                                            text.isNotBlank() &&
                                            !isThinking
                                        ) {

                                            inputText =
                                                ""

                                            onSend(
                                                text
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
                        },
                singleLine =
                    true,
                textStyle =
                    TextStyle(
                        color =
                            PrimaryText,
                        fontSize =
                            11.5.sp,
                        fontFamily =
                            InterfaceFont,
                        fontWeight =
                            FontWeight.Medium
                    ),
                cursorBrush =
                    SolidColor(
                        AccentOrange
                    )
            )
        }

    }
}


fun formatCalendarEventTime(
    event: UpcomingCalendarEvent
): String {

    val eventDate =
        Date(
            event.startTime
        )

    val dateFormat =
        SimpleDateFormat(
            "yyyyMMdd",
            Locale.getDefault()
        )

    val now =
        Date()

    val today =
        dateFormat.format(
            now
        )

    val tomorrowCalendar =
        java.util.Calendar
            .getInstance()
            .apply {

                time =
                    now

                add(
                    java.util.Calendar.DAY_OF_YEAR,
                    1
                )
            }

    val eventDay =
        dateFormat.format(
            eventDate
        )

    val dayLabel =
        when (
            eventDay
        ) {

            today ->
                "today"

            dateFormat.format(
                tomorrowCalendar.time
            ) ->
                "tomorrow"

            else ->
                SimpleDateFormat(
                    "EEE MMM d",
                    Locale.getDefault()
                )
                    .format(
                        eventDate
                    )
                    .lowercase()
        }

    if (
        event.allDay
    ) {

        return "$dayLabel · all day"
    }

    return "$dayLabel · ${
        SimpleDateFormat(
            "h:mm a",
            Locale.getDefault()
        )
            .format(
                eventDate
            )
            .lowercase()
    }"
}

data class ParsedAlarmTime(
    val hour: Int,
    val minute: Int,
    val display: String
)

/*
 * Accepts the way people actually type a timer, not just a bare
 * "10 minutes": "set 5 minute timer", "timer for 90 seconds",
 * "1 hour 30 min" all work.
 *
 * Every number-and-unit pair in the string is summed, so compound
 * durations come out right. A bare number with no unit means minutes,
 * which is what people expect from "! 10".
 */
fun parseTimerSeconds(
    command: String
): Int? {

    val input =
        command
            .trim()
            .lowercase()
            .removePrefix(
                "timer"
            )
            .trim()

    if (
        input.isBlank()
    ) {
        return null
    }

    val pattern =
        Regex(
            """(\d+)\s*(seconds?|secs?|s|minutes?|mins?|m|hours?|hrs?|h)?"""
        )

    var total = 0L
    var sawNumber = false

    pattern
        .findAll(
            input
        )
        .forEach { match ->

            val amount =
                match
                    .groupValues[1]
                    .toLongOrNull()
                    ?: return@forEach

            sawNumber = true

            val multiplier =
                when (
                    match.groupValues[2]
                ) {

                    "second",
                    "seconds",
                    "sec",
                    "secs",
                    "s" ->
                        1L

                    "hour",
                    "hours",
                    "hr",
                    "hrs",
                    "h" ->
                        3600L

                    else ->
                        60L
                }

            total += amount * multiplier
        }

    if (
        !sawNumber
    ) {
        return null
    }

    return total
        .takeIf {
            it in 1..Int.MAX_VALUE.toLong()
        }
        ?.toInt()
}

fun formatTimerDuration(
    seconds: Int
): String {

    return when {

        seconds % 3600 == 0 ->
            "${seconds / 3600} HR"

        seconds % 60 == 0 ->
            "${seconds / 60} MIN"

        else ->
            "$seconds SEC"
    }
}

fun parseAlarmTime(
    command: String
): ParsedAlarmTime? {

    /*
     * Finds the time anywhere in the phrase rather than demanding the
     * whole string be one, so "set alarm for 7:30am" and "wake me at
     * 6" both work.
     */
    val input =
        command
            .trim()
            .lowercase()
            .replace(
                " ",
                ""
            )

    val match =
        Regex(
            """(\d{1,2})(?::(\d{2}))?(am|pm)?"""
        )
            .find(
                input
            )
            ?: return null

    var hour =
        match
            .groupValues[1]
            .toIntOrNull()
            ?: return null

    val minute =
        match
            .groupValues[2]
            .ifBlank {
                "0"
            }
            .toIntOrNull()
            ?: return null

    if (
        minute !in 0..59
    ) {

        return null
    }

    val suffix =
        match
            .groupValues[3]

    if (
        suffix.isNotBlank()
    ) {

        if (
            hour !in 1..12
        ) {

            return null
        }

        hour =
            when (
                suffix
            ) {

                "am" ->
                    if (
                        hour == 12
                    ) {
                        0
                    } else {
                        hour
                    }

                "pm" ->
                    if (
                        hour == 12
                    ) {
                        12
                    } else {
                        hour + 12
                    }

                else ->
                    return null
            }

    } else if (
        hour !in 0..23
    ) {

        return null
    }

    val calendar =
        java.util.Calendar
            .getInstance()
            .apply {

                set(
                    java.util.Calendar.HOUR_OF_DAY,
                    hour
                )

                set(
                    java.util.Calendar.MINUTE,
                    minute
                )
            }

    val display =
        SimpleDateFormat(
            "h:mm a",
            Locale.getDefault()
        )
            .format(
                calendar.time
            )
            .lowercase()

    return ParsedAlarmTime(
        hour =
            hour,
        minute =
            minute,
        display =
            display
    )
}

@Composable
fun WeatherConditionIcon(
    condition: String?
) {

    val normalized =
        condition
            ?.trim()
            ?.lowercase()
            ?: ""

    val icon =
        when {

            normalized.contains(
                "rain"
            ) ||
                    normalized.contains(
                        "drizzle"
                    ) ||
                    normalized.contains(
                        "shower"
                    ) ||
                    normalized.contains(
                        "storm"
                    ) -> {
                Icons.Outlined.WaterDrop
            }

            normalized.contains(
                "cloud"
            ) ||
                    normalized.contains(
                        "overcast"
                    ) ||
                    normalized.contains(
                        "fog"
                    ) ||
                    normalized.contains(
                        "mist"
                    ) -> {
                Icons.Outlined.Cloud
            }

            else -> {
                Icons.Outlined.WbSunny
            }
        }

    /*
     * Coloured for the condition rather than the accent: a sun reads
     * as a sun, rain as rain. The accent is used everywhere else in
     * the launcher, so tinting weather with it flattened the one
     * place colour carries meaning.
     */
    val tint =
        when (
            icon
        ) {

            Icons.Outlined.WaterDrop ->
                Color(0xFF5B9BD5)

            Icons.Outlined.Cloud ->
                Color(0xFF9AA0A6)

            else ->
                Color(0xFFF5A623)
        }

    Icon(
        imageVector =
            icon,
        contentDescription =
            null,
        tint =
            tint,
        modifier =
            Modifier.size(
                12.dp
            )
    )
}

@Composable
fun ProductivityDotsRow(
    statuses: List<HourProductivityStatus>
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 4.dp
                )
                .height(
                    9.dp
                ),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        repeat(24) {
                hour ->

            val status =
                statuses
                    .getOrNull(
                        hour
                    )
                    ?: HourProductivityStatus.GRAY

            val dotColor =
                when (
                    status
                ) {
                    HourProductivityStatus.GRAY ->
                        ProductivityGray

                    HourProductivityStatus.PRODUCTIVE ->
                        PrimaryText

                    HourProductivityStatus.UNPRODUCTIVE ->
                        ProductivityRed
                }

            Box(
                modifier =
                    Modifier
                        .size(
                            4.8.dp
                        )
                        .background(
                            dotColor,
                            CircleShape
                        )
            )
        }
    }
}

@Composable
fun MinimalDivider() {

    Text(
        text =
            "· · · · · · · · · · · · · · · · · · · ·",
        color =
            BorderGray,
        fontSize =
            7.sp,
        fontFamily =
            InterfaceFont,
        maxLines =
            1
    )
}