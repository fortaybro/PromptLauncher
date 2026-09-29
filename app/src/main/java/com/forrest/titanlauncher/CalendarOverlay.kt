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
internal fun CalendarSetupOverlayCard(
    calendars: List<GoogleCalendarOption>,
    currentDefaultId: String?,
    onSave: (GoogleCalendarOption) -> Unit,
    onDismiss: () -> Unit
) {

    val focusRequester =
        remember {
            FocusRequester()
        }

    val listState =
        rememberLazyListState()

    var highlightedIndex by remember {
        mutableIntStateOf(
            0
        )
    }

    var selectedCalendar by remember(
        calendars,
        currentDefaultId
    ) {
        mutableStateOf<GoogleCalendarOption?>(
            calendars.firstOrNull {
                it.id ==
                        currentDefaultId
            }
                ?: calendars.firstOrNull {
                    it.primary
                }
                ?: calendars.firstOrNull()
        )
    }

    LaunchedEffect(
        calendars,
        currentDefaultId
    ) {

        val startingIndex =
            calendars
                .indexOfFirst {
                    it.id ==
                            selectedCalendar
                                ?.id
                }

        highlightedIndex =
            if (
                startingIndex >=
                0
            ) {
                startingIndex
            } else {
                0
            }

        focusRequester
            .requestFocus()
    }

    LaunchedEffect(
        highlightedIndex,
        calendars.size
    ) {

        if (
            calendars.isNotEmpty()
        ) {

            highlightedIndex =
                highlightedIndex
                    .coerceIn(
                        0,
                        calendars.lastIndex
                    )

            listState
                .animateScrollToItem(
                    highlightedIndex
                )
        } else {

            highlightedIndex =
                0
        }
    }

    BackHandler {
        onDismiss()
    }

    Column(
        modifier =
            Modifier
                .widthIn(
                    max =
                        270.dp
                )
                .heightIn(
                    min =
                        220.dp,
                    max =
                        330.dp
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
                        /*
                         * Consume taps inside the card so only
                         * taps outside the card dismiss it.
                         */
                    }
                }
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
                                    calendars.isNotEmpty()
                                ) {

                                    highlightedIndex =
                                        (
                                                highlightedIndex + 1
                                                )
                                            .coerceAtMost(
                                                calendars.lastIndex
                                            )
                                }

                                true
                            }

                            Key.DirectionUp -> {

                                highlightedIndex =
                                    (
                                            highlightedIndex - 1
                                            )
                                        .coerceAtLeast(
                                            0
                                        )

                                true
                            }

                            Key.Enter -> {

                                calendars
                                    .getOrNull(
                                        highlightedIndex
                                    )
                                    ?.let {
                                        selectedCalendar =
                                            it
                                    }

                                true
                            }

                            Key.Escape -> {

                                onDismiss()
                                true
                            }

                            else ->
                                false
                        }
                    }
                }
                .padding(
                    horizontal =
                        12.dp,
                    vertical =
                        10.dp
                )
    ) {

        Text(
            text =
                "calendar setup",
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
                Modifier.height(
                    8.dp
                )
        )

        if (
            calendars.isEmpty()
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
                        "no writable calendars found",
                    color =
                        TertiaryText,
                    fontSize =
                        8.sp,
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
                        4.dp
                    )
            ) {

                itemsIndexed(
                    items =
                        calendars,
                    key = {
                            _,
                            calendar ->

                        calendar.id
                    }
                ) {
                        index,
                        calendar ->

                    CalendarSetupOverlayRow(
                        calendar =
                            calendar,
                        selected =
                            selectedCalendar
                                ?.id ==
                                    calendar.id,
                        highlighted =
                            index ==
                                    highlightedIndex,
                        onClick = {

                            highlightedIndex =
                                index

                            selectedCalendar =
                                calendar
                        }
                    )
                }
            }
        }

        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        32.dp
                    )
                    .background(
                        color =
                            if (
                                selectedCalendar !=
                                null
                            ) {
                                AccentOrange
                            } else {
                                InputSurface
                            },
                        shape =
                            RoundedCornerShape(
                                8.dp
                            )
                    )
                    .border(
                        width =
                            0.75.dp,
                        color =
                            if (
                                selectedCalendar !=
                                null
                            ) {
                                AccentOrange
                            } else {
                                BorderGray
                            },
                        shape =
                            RoundedCornerShape(
                                8.dp
                            )
                    )
                    .clickable(
                        enabled =
                            selectedCalendar !=
                                    null
                    ) {

                        selectedCalendar
                            ?.let(
                                onSave
                            )
                    },
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text =
                    "DONE",
                color =
                    if (
                        selectedCalendar !=
                        null
                    ) {
                        if (
                            settingsChoiceTextNeedsDarkAccent()
                        ) {
                            BackgroundBlack
                        } else {
                            Color.White
                        }
                    } else {
                        TertiaryText
                    },
                fontSize =
                    9.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
internal fun CalendarSetupOverlayRow(
    calendar: GoogleCalendarOption,
    selected: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    42.dp
                )
                .background(
                    color =
                        if (
                            selected
                        ) {
                            AccentOrange.copy(
                                alpha =
                                    0.12f
                            )
                        } else if (
                            highlighted
                        ) {
                            InputSurface
                        } else {
                            BackgroundBlack
                        },
                    shape =
                        RoundedCornerShape(
                            8.dp
                        )
                )
                .border(
                    width =
                        if (
                            selected
                        ) {
                            1.dp
                        } else {
                            0.75.dp
                        },
                    color =
                        if (
                            selected
                        ) {
                            AccentOrange
                        } else {
                            BorderGray
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
                        10.dp
                ),
        contentAlignment =
            Alignment.CenterStart
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    if (
                        selected
                    ) {
                        ">"
                    } else {
                        " "
                    },
                color =
                    AccentOrange,
                fontSize =
                    10.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.width(
                        7.dp
                    )
            )

            Text(
                text =
                    calendar.summary
                        .lowercase(),
                color =
                    if (
                        selected
                    ) {
                        AccentOrange
                    } else {
                        PrimaryText
                    },
                fontSize =
                    9.5.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    if (
                        selected
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

