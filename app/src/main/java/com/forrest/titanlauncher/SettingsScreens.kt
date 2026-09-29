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
import com.forrest.titanlauncher.calendar.CalendarAssistCalendar
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
import com.forrest.titanlauncher.settings.LauncherSecondaryTextColor
import com.forrest.titanlauncher.settings.LauncherTextColor
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
internal fun SwipeRightToHomeContainer(
    edgeOnly: Boolean,
    edgeTopPaddingDp: Int = 0,
    onReturnHome: () -> Unit,
    homeContent: @Composable () -> Unit,
    screenContent: @Composable () -> Unit
) {

    var dragProgress by remember {
        mutableFloatStateOf(
            0f
        )
    }

    BoxWithConstraints(
        modifier =
            Modifier.fillMaxSize()
    ) {

        val widthPx =
            constraints
                .maxWidth
                .toFloat()
                .coerceAtLeast(
                    1f
                )

        val dragState =
            rememberDraggableState {
                    delta ->

                dragProgress =
                    (
                            dragProgress +
                                    delta /
                                    widthPx
                            )
                        .coerceIn(
                            0f,
                            1f
                        )
            }

        val swipeModifier =
            Modifier.draggable(
                state =
                    dragState,
                orientation =
                    Orientation.Horizontal,
                onDragStopped = {
                        velocity ->

                    val target =
                        if (
                            velocity >
                            900f ||
                            dragProgress >=
                            0.38f
                        ) {
                            1f
                        } else {
                            0f
                        }

                    animate(
                        initialValue =
                            dragProgress,
                        targetValue =
                            target,
                        animationSpec =
                            tween(
                                durationMillis =
                                    170
                            )
                    ) {
                            value,
                            _ ->

                        dragProgress =
                            value
                    }

                    if (
                        target ==
                        1f
                    ) {

                        onReturnHome()
                    }
                }
            )

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .offset {

                            IntOffset(
                                x =
                                    (
                                            -widthPx +
                                                    dragProgress *
                                                    widthPx
                                            )
                                        .roundToInt(),
                                y =
                                    0
                            )
                        }
            ) {

                homeContent()
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .offset {

                            IntOffset(
                                x =
                                    (
                                            dragProgress *
                                                    widthPx
                                            )
                                        .roundToInt(),
                                y =
                                    0
                            )
                        }
                        .then(
                            if (
                                edgeOnly
                            ) {
                                Modifier
                            } else {
                                swipeModifier
                            }
                        )
            ) {

                screenContent()
            }

            if (
                edgeOnly
            ) {

                Box(
                    modifier =
                        Modifier
                            .align(
                                Alignment.CenterStart
                            )
                            .fillMaxHeight()
                            .width(
                                30.dp
                            )
                            .padding(
                                top =
                                    edgeTopPaddingDp.dp
                            )
                            .then(
                                swipeModifier
                            )
                )
            }
        }
    }
}

@Composable
internal fun HomeHubSettingsSwipeContainer(
    showHub: Boolean,
    showSettings: Boolean,
    settingsDetailOpen: Boolean,
    onShowHub: () -> Unit,
    onShowHome: () -> Unit,
    onShowSettings: () -> Unit,
    homeContent: @Composable () -> Unit,
    hubContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit
) {
    val selectedPage =
        when {
            showHub -> 1f
            showSettings -> -1f
            else -> 0f
        }

    var dragProgress by remember {
        mutableFloatStateOf(
            selectedPage
        )
    }

    var isDragging by remember {
        mutableStateOf(
            false
        )
    }

    BoxWithConstraints(
        modifier =
            Modifier.fillMaxSize()
    ) {
        val widthPx =
            constraints
                .maxWidth
                .toFloat()
                .coerceAtLeast(
                    1f
                )

        val dragState =
            rememberDraggableState {
                    delta ->

                dragProgress =
                    (
                            dragProgress +
                                    delta /
                                    widthPx
                            )
                        .coerceIn(
                            -1f,
                            1f
                        )
            }

        LaunchedEffect(
            showHub,
            showSettings,
            widthPx
        ) {
            if (
                !isDragging
            ) {
                val target =
                    when {
                        showHub -> 1f
                        showSettings -> -1f
                        else -> 0f
                    }

                if (
                    abs(
                        dragProgress -
                                target
                    ) >
                    0.001f
                ) {
                    animate(
                        initialValue =
                            dragProgress,
                        targetValue =
                            target,
                        animationSpec =
                            tween(
                                durationMillis =
                                    190
                            )
                    ) {
                            value,
                            _ ->

                        dragProgress =
                            value
                    }
                }
            }
        }

        val gestureModifier =
            if (
                settingsDetailOpen
            ) {
                Modifier
            } else {
                Modifier.draggable(
                    state =
                        dragState,
                    orientation =
                        Orientation.Horizontal,
                    onDragStarted = {
                        isDragging =
                            true
                    },
                    onDragStopped = {
                            velocity ->

                        val flingThreshold =
                            900f

                        val target =
                            when {
                                velocity > flingThreshold ->
                                    (dragProgress + 1f)
                                        .coerceAtMost(1f)
                                        .let {
                                            when {
                                                it > 0.5f -> 1f
                                                it < -0.5f -> -1f
                                                else -> 0f
                                            }
                                        }

                                velocity < -flingThreshold ->
                                    (dragProgress - 1f)
                                        .coerceAtLeast(-1f)
                                        .let {
                                            when {
                                                it > 0.5f -> 1f
                                                it < -0.5f -> -1f
                                                else -> 0f
                                            }
                                        }

                                dragProgress >= 0.5f -> 1f
                                dragProgress <= -0.5f -> -1f
                                else -> 0f
                            }

                        when (target) {
                            1f -> onShowHub()
                            -1f -> onShowSettings()
                            else -> onShowHome()
                        }

                        animate(
                            initialValue =
                                dragProgress,
                            targetValue =
                                target,
                            animationSpec =
                                tween(
                                    durationMillis =
                                        170
                                )
                        ) {
                                value,
                                _ ->

                            dragProgress =
                                value
                        }

                        isDragging =
                            false
                    }
                )
            }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        gestureModifier
                    )
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x =
                                    (
                                            -widthPx +
                                                    dragProgress *
                                                    widthPx
                                            )
                                        .roundToInt(),
                                y = 0
                            )
                        }
            ) {
                hubContent()
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x =
                                    (
                                            dragProgress *
                                                    widthPx
                                            )
                                        .roundToInt(),
                                y = 0
                            )
                        }
            ) {
                homeContent()
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .offset {
                            IntOffset(
                                x =
                                    (
                                            widthPx +
                                                    dragProgress *
                                                    widthPx
                                            )
                                        .roundToInt(),
                                y = 0
                            )
                        }
            ) {
                settingsContent()
            }
        }
    }
}

@Composable
internal fun SettingsMainScreen(
    onAppearance: () -> Unit,
    onHomeScreen: () -> Unit,
    onCalendars: () -> Unit,
    onProductivityBar: () -> Unit,
    onNotifications: () -> Unit,
    onBackupRestore: () -> Unit,
    onReadability: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .verticalScroll(
                    rememberScrollState()
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
            Text(
                text =
                    "settings",
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
        }

        Spacer(
            modifier = Modifier.height(9.dp)
        )

        SettingsSectionLabel(
            text = "DEVICE"
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        SettingsNavigationRow(
            symbol = "◉",
            title = "appearance",
            subtitle = "theme, font & accent",
            onClick = onAppearance
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        SettingsNavigationRow(
            symbol = "⌂",
            title = "home screen",
            subtitle = "buttons, cards & layout",
            onClick = onHomeScreen
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        SettingsNavigationRow(
            symbol = "•",
            title = "calendars",
            subtitle = "home & ai calendar visibility",
            onClick = onCalendars
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        SettingsNavigationRow(
            symbol = "•••",
            title = "productivity bar",
            subtitle = "dots & distracting apps",
            onClick = onProductivityBar
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        SettingsNavigationRow(
            symbol = "•",
            title = "noti",
            subtitle = "hub notification apps",
            onClick = onNotifications
        )

        Spacer(
            modifier = Modifier.height(11.dp)
        )

        SettingsSectionLabel(
            text = "ACCESSIBILITY"
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        SettingsNavigationRow(
            symbol = "Aa",
            title = "readability",
            subtitle = "type size & contrast",
            onClick = onReadability
        )

        Spacer(
            modifier = Modifier.height(11.dp)
        )

        SettingsSectionLabel(
            text = "DATA"
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        SettingsNavigationRow(
            symbol = "•",
            title = "backup & restore",
            subtitle = "save or restore launcher settings",
            onClick = onBackupRestore
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

    }
}

@Composable
internal fun BackupRestoreSettingsScreen(
    statusText: String,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit
) {
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
        SettingsHeader(
            title = "backup & restore",
            onBack = onBack
        )

        Spacer(
            modifier = Modifier.height(9.dp)
        )

        SettingsSectionLabel(
            text = "SETTINGS"
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        SettingsNavigationRow(
            symbol = "•",
            title = "back up settings",
            subtitle = "save a portable settings file",
            onClick = onBackup
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        SettingsNavigationRow(
            symbol = "•",
            title = "restore settings",
            subtitle = "load settings from a backup file",
            onClick = onRestore
        )

        if (
            statusText.isNotBlank()
        ) {
            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = statusText,
                color =
                    if (
                        statusText.contains(
                            "failed"
                        )
                    ) {
                        TertiaryText
                    } else {
                        AccentOrange
                    },
                fontSize = 8.sp,
                fontFamily = InterfaceFont
            )
        }

        Spacer(
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun AppearanceSettingsScreen(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    BackgroundBlack
                )
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 7.dp
                )
    ) {
        SettingsHeader(
            title = "appearance",
            onBack = onBack
        )

        Spacer(
            modifier = Modifier.height(9.dp)
        )

        SettingsSectionLabel(
            text = "STYLE"
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        SettingsControlCard {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "accent",
                    color = PrimaryText,
                    fontSize = 9.5.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = FontWeight.Medium
                )

                Spacer(
                    modifier = Modifier.height(7.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LauncherAccent.entries.forEach { accent ->
                        val selected =
                            settings.accent == accent

                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        if (selected) 18.dp else 15.dp
                                    )
                                    .background(
                                        accentColorFor(accent),
                                        CircleShape
                                    )
                                    .border(
                                        width = if (selected) 1.5.dp else 0.dp,
                                        color = if (selected) PrimaryText else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        onSettingsChange(
                                            settings.copy(
                                                accent = accent
                                            )
                                        )
                                    }
                        )
                    }
                }
            }
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        SettingsControlCard {
            SettingsChoiceWrapRow(
                label = "theme",
                choices =
                    listOf(
                        "dark",
                        "light",
                        "navy",
                        "slate",
                        "tan",
                        "forest"
                    ),
                selectedIndex =
                    when (settings.themeMode) {
                        LauncherThemeMode.DARK -> 0
                        LauncherThemeMode.LIGHT -> 1
                        LauncherThemeMode.NAVY -> 2
                        LauncherThemeMode.SLATE -> 3
                        LauncherThemeMode.TAN -> 4
                        LauncherThemeMode.FOREST -> 5
                    },
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            themeMode =
                                when (index) {
                                    1 -> LauncherThemeMode.LIGHT
                                    2 -> LauncherThemeMode.NAVY
                                    3 -> LauncherThemeMode.SLATE
                                    4 -> LauncherThemeMode.TAN
                                    5 -> LauncherThemeMode.FOREST
                                    else -> LauncherThemeMode.DARK
                                }
                        )
                    )
                }
            )
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        SettingsControlCard {
            /*
             * "default" leaves each theme to supply its own text
             * colours. Anything else overrides all three tiers.
             */
            SettingsChoiceWrapRow(
                label = "text color",
                choices =
                    listOf(
                        "default",
                        "white",
                        "black",
                        "warm",
                        "cool",
                        "accent"
                    ),
                selectedIndex =
                    when (settings.textColor) {
                        LauncherTextColor.DEFAULT -> 0
                        LauncherTextColor.WHITE -> 1
                        LauncherTextColor.BLACK -> 2
                        LauncherTextColor.WARM -> 3
                        LauncherTextColor.COOL -> 4
                        LauncherTextColor.ACCENT -> 5
                    },
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            textColor =
                                when (index) {
                                    1 -> LauncherTextColor.WHITE
                                    2 -> LauncherTextColor.BLACK
                                    3 -> LauncherTextColor.WARM
                                    4 -> LauncherTextColor.COOL
                                    5 -> LauncherTextColor.ACCENT
                                    else -> LauncherTextColor.DEFAULT
                                }
                        )
                    )
                }
            )
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        SettingsControlCard {
            /*
             * Controls the muted text: labels, previews, timestamps
             * and hints. "default" leaves it derived from the theme
             * and the primary text colour.
             */
            SettingsChoiceWrapRow(
                label = "secondary text",
                choices =
                    listOf(
                        "default",
                        "light",
                        "medium",
                        "dark",
                        "warm",
                        "cool",
                        "accent"
                    ),
                selectedIndex =
                    when (settings.secondaryTextColor) {
                        LauncherSecondaryTextColor.DEFAULT -> 0
                        LauncherSecondaryTextColor.LIGHT -> 1
                        LauncherSecondaryTextColor.MEDIUM -> 2
                        LauncherSecondaryTextColor.DARK -> 3
                        LauncherSecondaryTextColor.WARM -> 4
                        LauncherSecondaryTextColor.COOL -> 5
                        LauncherSecondaryTextColor.ACCENT -> 6
                    },
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            secondaryTextColor =
                                when (index) {
                                    1 -> LauncherSecondaryTextColor.LIGHT
                                    2 -> LauncherSecondaryTextColor.MEDIUM
                                    3 -> LauncherSecondaryTextColor.DARK
                                    4 -> LauncherSecondaryTextColor.WARM
                                    5 -> LauncherSecondaryTextColor.COOL
                                    6 -> LauncherSecondaryTextColor.ACCENT
                                    else -> LauncherSecondaryTextColor.DEFAULT
                                }
                        )
                    )
                }
            )
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        SettingsControlCard {
            /*
             * Four choices will not fit beside the label on a Titan
             * sized screen, so this one wraps onto its own lines.
             */
            SettingsChoiceWrapRow(
                label = "interface font",
                choices =
                    listOf(
                        "mono",
                        "sans",
                        "serif",
                        "poppins"
                    ),
                selectedIndex =
                    when (settings.interfaceFont) {
                        LauncherInterfaceFont.MONO -> 0
                        LauncherInterfaceFont.SANS -> 1
                        LauncherInterfaceFont.SERIF -> 2
                        LauncherInterfaceFont.POPPINS -> 3
                    },
                onSelected = { index ->
                    onSettingsChange(
                        settings.copy(
                            interfaceFont =
                                when (index) {
                                    1 -> LauncherInterfaceFont.SANS
                                    2 -> LauncherInterfaceFont.SERIF
                                    3 -> LauncherInterfaceFont.POPPINS
                                    else -> LauncherInterfaceFont.MONO
                                }
                        )
                    )
                }
            )
        }

        /*
         * A weighted spacer would demand infinite height inside a
         * scrolling column. A fixed tail keeps the last card clear of
         * the bottom edge instead.
         */
        Spacer(
            modifier = Modifier.height(14.dp)
        )

    }
}

internal data class LauncherAppEntry(
    val label: String,
    val packageName: String
)

internal fun loadLaunchableApps(
    context: Context
): List<LauncherAppEntry> {
    val packageManager =
        context.packageManager

    @Suppress("DEPRECATION")
    val installedApplications =
        packageManager
            .getInstalledApplications(
                0
            )

    return installedApplications
        .mapNotNull { applicationInfo ->
            val packageName =
                applicationInfo.packageName

            val launchIntent =
                packageManager
                    .getLaunchIntentForPackage(
                        packageName
                    )

            if (
                packageName.isBlank() ||
                packageName ==
                context.packageName ||
                launchIntent == null ||
                !applicationInfo.enabled
            ) {
                null
            } else {
                LauncherAppEntry(
                    label =
                        packageManager
                            .getApplicationLabel(
                                applicationInfo
                            )
                            .toString()
                            .ifBlank {
                                packageName
                            },
                    packageName =
                        packageName
                )
            }
        }
        .distinctBy {
            it.packageName
        }
        .sortedBy {
            it.label.lowercase()
        }
}

internal data class InstalledProductivityApp(
    val label: String,
    val packageName: String
)

internal fun loadInstalledProductivityApps(
    context: Context
): List<InstalledProductivityApp> {
    val packageManager =
        context.packageManager

    @Suppress("DEPRECATION")
    val installedApplications =
        packageManager
            .getInstalledApplications(
                0
            )

    val excludedPackages =
        setOf(
            context.packageName,
            "com.android.settings",
            "com.android.stk"
        )

    return installedApplications
        .mapNotNull { applicationInfo ->
            val packageName =
                applicationInfo.packageName

            val launchIntent =
                packageManager
                    .getLaunchIntentForPackage(
                        packageName
                    )

            if (
                packageName.isBlank() ||
                packageName in excludedPackages ||
                launchIntent == null ||
                !applicationInfo.enabled
            ) {
                null
            } else {
                InstalledProductivityApp(
                    label =
                        packageManager
                            .getApplicationLabel(
                                applicationInfo
                            )
                            .toString()
                            .ifBlank {
                                packageName
                            },
                    packageName =
                        packageName
                )
            }
        }
        .distinctBy {
            it.packageName
        }
        .sortedBy {
            it.label.lowercase()
        }
}

@Composable
internal fun NotificationSettingsScreen(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    /*
     * Mail and the dialers are hidden because Prompt Launcher always
     * shows those itself, so a toggle would do nothing. The messaging
     * apps are only hidden while Prompt owns SMS: when another app
     * holds the role its notifications do appear, so they need a
     * toggle like anything else.
     */
    val apps = remember(context) {

        val promptOwnsSms =
            runCatching {
                android.provider.Telephony.Sms
                    .getDefaultSmsPackage(
                        context
                    ) ==
                        context.packageName
            }
                .getOrDefault(
                    true
                )

        val hidden =
            buildSet {

                add("com.google.android.gm")
                add("com.google.android.dialer")
                add("com.android.dialer")

                if (
                    promptOwnsSms
                ) {
                    add("com.google.android.apps.messaging")
                    add("com.android.messaging")
                }
            }

        loadLaunchableApps(context)
            .filterNot { app ->
                app.packageName in hidden
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundBlack)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        SettingsHeader(
            title = "noti",
            onBack = onBack
        )

        Spacer(Modifier.height(7.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            item {
                SettingsSectionLabel(text = "HUB APPS")
            }

            items(
                items = apps,
                key = { it.packageName }
            ) { app ->
                val enabled =
                    app.packageName !in settings.mutedNotificationApps

                SettingsControlCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val updatedMuted =
                                    settings.mutedNotificationApps.toMutableSet()

                                if (enabled) {
                                    updatedMuted.add(app.packageName)
                                } else {
                                    updatedMuted.remove(app.packageName)
                                }

                                onSettingsChange(
                                    settings.copy(
                                        mutedNotificationApps = updatedMuted
                                    )
                                )
                            }
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = app.label,
                            color = PrimaryText,
                            fontSize = 11.sp,
                            lineHeight = 12.sp,
                            fontFamily = InterfaceFont,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = if (enabled) "on" else "off",
                            color = if (enabled) AccentOrange else TertiaryText,
                            fontSize = 8.sp,
                            fontFamily = InterfaceFont,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}


@Composable
internal fun CalendarVisibilitySettingsScreen(
    calendars: List<CalendarAssistCalendar>,
    hiddenCalendarIds: Set<String>,
    currentEventCalendarId: String?,
    loading: Boolean,
    onToggleCalendar: (
        calendarId: String,
        visible: Boolean
    ) -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {

    val currentEventCalendarName =
        calendars
            .firstOrNull {
                it.id ==
                        currentEventCalendarId
            }
            ?.summary
            ?: if (
                currentEventCalendarId.isNullOrBlank()
            ) {
                "not set"
            } else {
                "selected calendar"
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
    ) {

        SettingsHeader(
            title =
                "calendars",
            onBack =
                onBack
        )

        Spacer(
            modifier =
                Modifier.height(
                    7.dp
                )
        )

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(
                        1f
                    ),
            verticalArrangement =
                Arrangement.spacedBy(
                    5.dp
                )
        ) {

            item {

                SettingsSectionLabel(
                    text =
                        "EVENT CREATION"
                )
            }

            item {

                SettingsControlCard {

                    Column(
                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            text =
                                "event calendar",
                            color =
                                PrimaryText,
                            fontSize =
                                9.5.sp,
                            fontFamily =
                                InterfaceFont,
                            fontWeight =
                                FontWeight.Medium
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    3.dp
                                )
                        )

                        Text(
                            text =
                                currentEventCalendarName
                                    .lowercase(),
                            color =
                                AccentOrange,
                            fontSize =
                                8.sp,
                            fontFamily =
                                InterfaceFont,
                            fontWeight =
                                FontWeight.Medium,
                            maxLines =
                                1,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    2.dp
                                )
                        )

                        Text(
                            text =
                                "calendarsetup controls where \":\" adds events",
                            color =
                                TertiaryText,
                            fontSize =
                                6.5.sp,
                            lineHeight =
                                7.sp,
                            fontFamily =
                                InterfaceFont
                        )
                    }
                }
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            3.dp
                        )
                )

                SettingsSectionLabel(
                    text =
                        "VISIBLE CALENDARS"
                )
            }

            item {

                Text(
                    text =
                        "controls the home calendar card + ai assist",
                    color =
                        TertiaryText,
                    fontSize =
                        6.5.sp,
                    lineHeight =
                        7.sp,
                    fontFamily =
                        InterfaceFont,
                    modifier =
                        Modifier.padding(
                            horizontal =
                                2.dp,
                            vertical =
                                1.dp
                        )
                )
            }

            if (
                loading &&
                calendars.isEmpty()
            ) {

                item {

                    SettingsControlCard {

                        Text(
                            text =
                                "loading calendars...",
                            color =
                                TertiaryText,
                            fontSize =
                                8.sp,
                            fontFamily =
                                InterfaceFont
                        )
                    }
                }

            } else if (
                calendars.isEmpty()
            ) {

                item {

                    SettingsControlCard {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onRefresh()
                                    },
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text =
                                    "no calendars loaded",
                                color =
                                    TertiaryText,
                                fontSize =
                                    8.sp,
                                fontFamily =
                                    InterfaceFont,
                                modifier =
                                    Modifier.weight(
                                        1f
                                    )
                            )

                            Text(
                                text =
                                    "refresh",
                                color =
                                    AccentOrange,
                                fontSize =
                                    7.5.sp,
                                fontFamily =
                                    InterfaceFont,
                                fontWeight =
                                    FontWeight.Medium
                            )
                        }
                    }
                }

            } else {

                items(
                    items =
                        calendars,
                    key = {
                        it.id
                    }
                ) {
                        calendar ->

                    val visible =
                        calendar.id !in
                                hiddenCalendarIds

                    SettingsControlCard {

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {

                                        onToggleCalendar(
                                            calendar.id,
                                            !visible
                                        )
                                    }
                                    .padding(
                                        vertical =
                                            1.dp
                                    ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text =
                                    calendar.summary
                                        .lowercase(),
                                color =
                                    PrimaryText,
                                fontSize =
                                    10.5.sp,
                                lineHeight =
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
                                    Modifier.weight(
                                        1f
                                    )
                            )

                            Text(
                                text =
                                    if (
                                        visible
                                    ) {
                                        "on"
                                    } else {
                                        "off"
                                    },
                                color =
                                    if (
                                        visible
                                    ) {
                                        AccentOrange
                                    } else {
                                        TertiaryText
                                    },
                                fontSize =
                                    8.sp,
                                fontFamily =
                                    InterfaceFont,
                                fontWeight =
                                    FontWeight.Medium
                            )
                        }
                    }
                }

                item {

                    Text(
                        text =
                            if (
                                loading
                            ) {
                                "refreshing..."
                            } else {
                                "tap a calendar to show or hide it"
                            },
                        color =
                            TertiaryText,
                        fontSize =
                            6.5.sp,
                        fontFamily =
                            InterfaceFont,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onRefresh()
                                }
                                .padding(
                                    vertical =
                                        3.dp
                                )
                    )
                }
            }
        }
    }
}


@Composable
internal fun HomeScreenSettingsScreen(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit,
    onBack: () -> Unit
) {
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
        SettingsHeader(
            title = "home screen",
            onBack = onBack
        )

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            item {
                SettingsSectionLabel(
                    text = "SURFACES"
                )
            }

            item {
                SettingsControlCard {
                    SettingsChoiceRow(
                        label = "app buttons",
                        choices = listOf("black", "charcoal", "gray"),
                        selectedIndex = settings.appButtonSurface.ordinal,
                        onSelected = { index ->
                            onSettingsChange(
                                settings.copy(
                                    appButtonSurface = HomeSurfaceTone.entries[index]
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsControlCard {
                    SettingsChoiceRow(
                        label = "info cards",
                        choices = listOf("black", "charcoal", "gray"),
                        selectedIndex = settings.infoCardSurface.ordinal,
                        onSelected = { index ->
                            onSettingsChange(
                                settings.copy(
                                    infoCardSurface = HomeSurfaceTone.entries[index]
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsControlCard {
                    SettingsChoiceRow(
                        label = "command box",
                        choices = listOf("dark", "medium", "light"),
                        selectedIndex = settings.commandSurface.ordinal,
                        onSelected = { index ->
                            onSettingsChange(
                                settings.copy(
                                    commandSurface = CommandSurfaceTone.entries[index]
                                )
                            )
                        }
                    )
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(3.dp)
                )
                SettingsSectionLabel(
                    text = "BUTTONS"
                )
            }

            item {
                SettingsControlCard {
                    SettingsChoiceRow(
                        label = "icon size",
                        choices = listOf("small", "normal", "large"),
                        selectedIndex = settings.buttonIconSize.ordinal,
                        onSelected = { index ->
                            onSettingsChange(
                                settings.copy(
                                    buttonIconSize = HomeElementSize.entries[index]
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsControlCard {
                    SettingsChoiceRow(
                        label = "label size",
                        choices = listOf("small", "normal", "large"),
                        selectedIndex = settings.buttonLabelSize.ordinal,
                        onSelected = { index ->
                            onSettingsChange(
                                settings.copy(
                                    buttonLabelSize = HomeElementSize.entries[index]
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsControlCard {
                    SettingsChoiceRow(
                        label = "corners",
                        choices = listOf("square", "soft", "round"),
                        selectedIndex = settings.buttonCorners.ordinal,
                        onSelected = { index ->
                            onSettingsChange(
                                settings.copy(
                                    buttonCorners = HomeCornerStyle.entries[index]
                                )
                            )
                        }
                    )
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(3.dp)
                )
                SettingsSectionLabel(
                    text = "HOME ELEMENTS"
                )
            }

            item {
                SettingsControlCard {
                    SettingsToggleRow(
                        label = "search picker",
                        enabled = settings.showSearchTargetPicker,
                        onToggle = { enabled ->
                            onSettingsChange(
                                settings.copy(
                                    showSearchTargetPicker = enabled
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsControlCard {
                    SettingsToggleRow(
                        label = "weather",
                        enabled = settings.showWeather,
                        onToggle = { enabled ->
                            onSettingsChange(
                                settings.copy(
                                    showWeather = enabled
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsControlCard {
                    SettingsToggleRow(
                        label = "calendar card",
                        enabled = settings.showCalendarCard,
                        onToggle = { enabled ->
                            onSettingsChange(
                                settings.copy(
                                    showCalendarCard = enabled
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsControlCard {
                    SettingsToggleRow(
                        label = "attention card",
                        enabled = settings.showAttentionCard,
                        onToggle = { enabled ->
                            onSettingsChange(
                                settings.copy(
                                    showAttentionCard = enabled
                                )
                            )
                        }
                    )
                }
            }

        }
    }
}

@Composable
internal fun ProductivityBarSettingsScreen(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit,
    onBack: () -> Unit
) {
    val context =
        LocalContext.current

    val installedApps =
        remember(
            context
        ) {
            loadInstalledProductivityApps(
                context
            )
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
        SettingsHeader(
            title = "productivity bar",
            onBack = onBack
        )

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            item {
                SettingsSectionLabel(
                    text = "BAR"
                )
            }

            item {
                SettingsControlCard {
                    SettingsToggleRow(
                        label = "productivity dots",
                        enabled = settings.showProductivityDots,
                        onToggle = { enabled ->
                            onSettingsChange(
                                settings.copy(
                                    showProductivityDots =
                                        enabled
                                )
                            )
                        }
                    )
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(3.dp)
                )

                SettingsSectionLabel(
                    text = "DISTRACTING APPS"
                )
            }

            if (
                installedApps.isEmpty()
            ) {
                item {
                    SettingsControlCard {
                        Text(
                            text =
                                "no installed apps found",
                            color =
                                SecondaryText,
                            fontSize =
                                8.sp,
                            fontFamily =
                                InterfaceFont
                        )
                    }
                }
            } else {
                items(
                    installedApps,
                    key = {
                        it.packageName
                    }
                ) { app ->
                    SettingsControlCard {
                        SettingsAppToggleRow(
                            appName =
                                app.label,
                            packageName =
                                app.packageName,
                            enabled =
                                app.packageName in
                                        settings.distractingApps,
                            onToggle = { enabled ->
                                val updatedPackages =
                                    settings
                                        .distractingApps
                                        .toMutableSet()
                                        .apply {
                                            if (
                                                enabled
                                            ) {
                                                add(
                                                    app.packageName
                                                )
                                            } else {
                                                remove(
                                                    app.packageName
                                                )
                                            }
                                        }
                                        .toSet()

                                onSettingsChange(
                                    settings.copy(
                                        distractingApps =
                                            updatedPackages
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReadabilitySettingsScreen(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit,
    onBack: () -> Unit
) {
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
        SettingsHeader(
            title = "readability",
            onBack = onBack
        )

        Spacer(
            modifier = Modifier.height(7.dp)
        )

        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {
            item {
                SettingsSectionLabel(
                    text = "TEXT"
                )
            }

            item {
                SettingsControlCard {
                    SettingsChoiceRow(
                        label = "text size",
                        choices = listOf(
                            "small",
                            "normal",
                            "large",
                            "x-large"
                        ),
                        selectedIndex =
                            settings.readabilityTextSize.ordinal,
                        onSelected = { index ->
                            onSettingsChange(
                                settings.copy(
                                    readabilityTextSize =
                                        ReadabilityTextSize.entries[index]
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsControlCard {
                    SettingsChoiceRow(
                        label = "text weight",
                        choices = listOf(
                            "normal",
                            "medium",
                            "bold"
                        ),
                        selectedIndex =
                            settings.readabilityTextWeight.ordinal,
                        onSelected = { index ->
                            onSettingsChange(
                                settings.copy(
                                    readabilityTextWeight =
                                        ReadabilityTextWeight.entries[index]
                                )
                            )
                        }
                    )
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(3.dp)
                )
                SettingsSectionLabel(
                    text = "VISIBILITY"
                )
            }

            item {
                SettingsControlCard {
                    SettingsChoiceRow(
                        label = "contrast",
                        choices = listOf(
                            "standard",
                            "high",
                            "maximum"
                        ),
                        selectedIndex =
                            settings.readabilityContrast.ordinal,
                        onSelected = { index ->
                            onSettingsChange(
                                settings.copy(
                                    readabilityContrast =
                                        ReadabilityContrast.entries[index]
                                )
                            )
                        }
                    )
                }
            }

            item {
                SettingsControlCard {
                    SettingsToggleRow(
                        label = "dim secondary text",
                        enabled = settings.dimSecondaryText,
                        onToggle = { enabled ->
                            onSettingsChange(
                                settings.copy(
                                    dimSecondaryText = enabled
                                )
                            )
                        }
                    )
                }
            }

            item {
                Spacer(
                    modifier = Modifier.height(3.dp)
                )
                SettingsSectionLabel(
                    text = "MOTION"
                )
            }

            item {
                SettingsControlCard {
                    SettingsChoiceRow(
                        label = "motion",
                        choices = listOf(
                            "normal",
                            "reduced"
                        ),
                        selectedIndex =
                            settings.readabilityMotion.ordinal,
                        onSelected = { index ->
                            onSettingsChange(
                                settings.copy(
                                    readabilityMotion =
                                        ReadabilityMotion.entries[index]
                                )
                            )
                        }
                    )
                }
            }

            item {
            }
        }
    }
}

@Composable
internal fun LauncherBackButton(
    onBack: () -> Unit
) {
    Text(
        text = "<",
        color = AccentOrange,
        fontSize = 24.sp,
        lineHeight = 24.sp,
        fontFamily = InterfaceFont,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clickable {
            onBack()
        }
    )
}

@Composable
internal fun SettingsHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = CameraSafeStartPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LauncherBackButton(onBack = onBack)

        Spacer(
            modifier = Modifier.width(6.dp)
        )

        Text(
            text = title,
            color = PrimaryText,
            fontSize = 14.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
internal fun SettingsSectionLabel(
    text: String
) {
    Text(
        text = text,
        color = TertiaryText,
        fontSize = 6.5.sp,
        fontFamily = InterfaceFont,
        fontWeight = FontWeight.Bold
    )
}

@Composable
internal fun SettingsNavigationRow(
    symbol: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    SurfaceBlack,
                    RoundedCornerShape(9.dp)
                )
                .border(
                    0.75.dp,
                    BorderGray,
                    RoundedCornerShape(9.dp)
                )
                .clickable {
                    onClick()
                }
                .padding(
                    horizontal = 9.dp
                ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "•",
            color = PrimaryText,
            fontSize = 12.sp,
            fontFamily = InterfaceFont,
            modifier = Modifier.width(14.dp),
            textAlign = TextAlign.Center
        )

        Spacer(
            modifier = Modifier.width(5.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                color = PrimaryText,
                fontSize = 9.5.sp,
                lineHeight = 9.8.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = subtitle,
                color = TertiaryText,
                fontSize = 6.5.sp,
                lineHeight = 6.8.sp,
                fontFamily = InterfaceFont
            )
        }

        Text(
            text = ">",
            color = SecondaryText,
            fontSize = 11.sp,
            fontFamily = InterfaceFont
        )
    }
}

@Composable
internal fun SettingsControlCard(
    content: @Composable () -> Unit
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    SurfaceBlack,
                    RoundedCornerShape(9.dp)
                )
                .border(
                    0.75.dp,
                    BorderGray,
                    RoundedCornerShape(9.dp)
                )
                .padding(
                    horizontal = 10.dp,
                    vertical = 9.dp
                )
    ) {
        content()
    }
}

@Composable
internal fun SettingsChoiceRow(
    label: String,
    choices: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = PrimaryText,
            fontSize = 9.5.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        choices.forEachIndexed { index, choice ->
            val selected =
                index == selectedIndex

            Text(
                text = choice,
                color =
                    if (selected) {
                        if (settingsChoiceTextNeedsDarkAccent()) BackgroundBlack else PrimaryText
                    } else {
                        SecondaryText
                    },
                fontSize = 7.5.sp,
                lineHeight = 7.5.sp,
                fontFamily = InterfaceFont,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .background(
                            if (selected) AccentOrange else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            0.75.dp,
                            if (selected) AccentOrange else BorderGray,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            onSelected(index)
                        }
                        .padding(
                            horizontal = 7.dp,
                            vertical = 4.dp
                        )
            )

            if (index != choices.lastIndex) {
                Spacer(
                    modifier = Modifier.width(4.dp)
                )
            }
        }
    }
}

@Composable
internal fun SettingsChoiceWrapRow(
    label: String,
    choices: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    perRow: Int = 3
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = PrimaryText,
            fontSize = 9.5.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        choices
            .withIndex()
            .chunked(perRow)
            .forEachIndexed { rowIndex, rowChoices ->

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    /*
                     * Every chip takes an equal share of the row and a
                     * fixed height, so rows line up with each other no
                     * matter how long the individual labels are.
                     */
                    rowChoices.forEach { indexedChoice ->

                        val index =
                            indexedChoice.index

                        val selected =
                            index == selectedIndex

                        Box(
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .height(26.dp)
                                    .background(
                                        if (selected) AccentOrange else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        0.75.dp,
                                        if (selected) AccentOrange else BorderGray,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        onSelected(index)
                                    },
                            contentAlignment = Alignment.Center
                        ) {
                            /*
                             * Explicit lineHeight and centred
                             * textAlign: without them the label is
                             * centred by its line box, and Poppins
                             * reports more descent than ascent, so it
                             * sat low in the chip.
                             */
                            Text(
                                text = indexedChoice.value,
                                color =
                                    if (selected) {
                                        if (settingsChoiceTextNeedsDarkAccent()) BackgroundBlack else PrimaryText
                                    } else {
                                        SecondaryText
                                    },
                                fontSize = 7.5.sp,
                                lineHeight = 7.5.sp,
                                fontFamily = InterfaceFont,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }

                        Spacer(
                            modifier = Modifier.width(4.dp)
                        )
                    }

                    /*
                     * Pads a short final row so its chips keep the same
                     * width as the rows above.
                     */
                    repeat(
                        perRow - rowChoices.size
                    ) {
                        Spacer(
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(
                            modifier = Modifier.width(4.dp)
                        )
                    }
                }

                if (rowIndex != (choices.size - 1) / perRow) {
                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )
                }
            }
    }
}

@Composable
internal fun SettingsToggleRow(
    label: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = PrimaryText,
            fontSize = 9.5.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = if (enabled) "on" else "off",
            color =
                if (enabled && settingsChoiceTextNeedsDarkAccent()) {
                    BackgroundBlack
                } else if (enabled) {
                    PrimaryText
                } else {
                    SecondaryText
                },
            fontSize = 7.5.sp,
            fontFamily = InterfaceFont,
            modifier =
                Modifier
                    .background(
                        if (enabled) AccentOrange else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        0.75.dp,
                        if (enabled) AccentOrange else BorderGray,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        onToggle(!enabled)
                    }
                    .padding(
                        horizontal = 10.dp,
                        vertical = 4.dp
                    )
        )
    }
}

@Composable
internal fun SettingsAppToggleRow(
    appName: String,
    packageName: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onToggle(
                        !enabled
                    )
                },
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {
            Text(
                text = appName,
                color = PrimaryText,
                fontSize = 9.5.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )

            Text(
                text = packageName,
                color = TertiaryText,
                fontSize = 6.sp,
                fontFamily = InterfaceFont,
                maxLines = 1
            )
        }

        Spacer(
            modifier = Modifier.width(
                8.dp
            )
        )

        Text(
            text =
                if (enabled) {
                    "on"
                } else {
                    "off"
                },
            color =
                if (
                    enabled &&
                    settingsChoiceTextNeedsDarkAccent()
                ) {
                    BackgroundBlack
                } else if (
                    enabled
                ) {
                    PrimaryText
                } else {
                    SecondaryText
                },
            fontSize = 7.5.sp,
            fontFamily = InterfaceFont,
            modifier =
                Modifier
                    .background(
                        if (enabled) {
                            AccentOrange
                        } else {
                            Color.Transparent
                        },
                        RoundedCornerShape(
                            8.dp
                        )
                    )
                    .border(
                        0.75.dp,
                        if (enabled) {
                            AccentOrange
                        } else {
                            BorderGray
                        },
                        RoundedCornerShape(
                            8.dp
                        )
                    )
                    .padding(
                        horizontal = 10.dp,
                        vertical = 4.dp
                    )
        )
    }
}

internal fun settingsChoiceTextNeedsDarkAccent(): Boolean {
    return AccentOrange == Color(0xFFFFC83D)
}

internal fun hasNotificationListenerAccess(
    context: Context
): Boolean {
    val enabledListeners =
        Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
            ?: return false

    return enabledListeners
        .split(":")
        .mapNotNull {
            ComponentName.unflattenFromString(
                it
            )
        }
        .any {
            it.packageName ==
                    context.packageName
        }
}