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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Lightbulb
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
fun NotesScreen(
    noteStore: NoteStore,
    onOpenNote: (NoteItem) -> Unit,
    onCreateNote: (NoteCategory) -> Unit,
    onBack: () -> Unit
) {
    /*
     * Observed rather than snapshotted, so a note created from the
     * command bar while this screen is open appears immediately.
     */
    val notes by
    noteStore
        .notes
        .collectAsStateWithLifecycle()
    var selectedTab by remember {
        mutableIntStateOf(0)
    }
    var selectedIndex by remember {
        mutableIntStateOf(0)
    }
    val categories =
        listOf<NoteCategory?>(
            null,
            NoteCategory.PERSONAL,
            NoteCategory.WORK,
            NoteCategory.IDEAS,
            NoteCategory.JOURNAL
        )
    val tabLabels =
        listOf(
            "all",
            "personal",
            "work",
            "ideas",
            "journal"
        )
    val activeCategory =
        categories[selectedTab]
    val filteredNotes =
        if (activeCategory == null) {
            notes
        } else {
            notes.filter {
                it.category == activeCategory
            }
        }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    fun refreshNotes() {
        /*
         * The store publishes on every write, so nothing to pull here.
         * Kept so the delete and editor paths read the same as before.
         */
    }

    var pendingDeleteNote by remember {
        mutableStateOf<NoteItem?>(null)
    }

    fun deleteNote(note: NoteItem) {
        noteStore.deleteNote(note.id)
        refreshNotes()
        selectedIndex =
            if (filteredNotes.isEmpty()) 0
            else selectedIndex.coerceAtMost((filteredNotes.size - 1).coerceAtLeast(0))
    }

    LaunchedEffect(Unit) {
        refreshNotes()
        focusRequester.requestFocus()
    }

    LaunchedEffect(selectedTab) {
        selectedIndex = 0
    }

    LaunchedEffect(selectedIndex, filteredNotes.size) {
        if (filteredNotes.isNotEmpty()) {
            listState.animateScrollToItem(
                selectedIndex.coerceAtMost(filteredNotes.lastIndex)
            )
        }
    }

    /*
     * Matches the other overlays: the list behind the confirmation
     * blurs rather than just dimming, animated unless the user has
     * asked for reduced motion.
     */
    val notesBlur by
    animateDpAsState(
        targetValue =
            if (
                pendingDeleteNote != null
            ) {
                7.dp
            } else {
                0.dp
            },
        animationSpec =
            tween(
                durationMillis =
                    if (
                        ReduceLauncherMotion
                    ) {
                        0
                    } else {
                        180
                    }
            ),
        label =
            "notesBlur"
    )

    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .blur(notesBlur)
                    .background(BackgroundBlack)
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) {
                            false
                        } else {
                            when (event.key) {
                                Key.DirectionDown -> {
                                    if (filteredNotes.isNotEmpty()) {
                                        selectedIndex =
                                            (selectedIndex + 1)
                                                .coerceAtMost(filteredNotes.lastIndex)
                                    }
                                    true
                                }

                                Key.DirectionUp -> {
                                    selectedIndex =
                                        (selectedIndex - 1)
                                            .coerceAtLeast(0)
                                    true
                                }

                                Key.DirectionLeft -> {
                                    selectedTab =
                                        (selectedTab - 1)
                                            .coerceAtLeast(0)
                                    true
                                }

                                Key.DirectionRight -> {
                                    selectedTab =
                                        (selectedTab + 1)
                                            .coerceAtMost(tabLabels.lastIndex)
                                    true
                                }

                                Key.Enter -> {
                                    filteredNotes
                                        .getOrNull(selectedIndex)
                                        ?.let(onOpenNote)
                                    true
                                }

                                Key.N -> {
                                    onCreateNote(
                                        activeCategory ?: NoteCategory.PERSONAL
                                    )
                                    true
                                }

                                Key.Escape -> {
                                    onBack()
                                    true
                                }

                                else -> false
                            }
                        }
                    }
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = CameraSafeStartPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LauncherBackButton(onBack = onBack)

                Spacer(Modifier.width(8.dp))

                Text(
                    text = "notes",
                    color = PrimaryText,
                    fontSize = 29.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.width(5.dp))

                Text(
                    text = "${notes.size} notes",
                    color = TertiaryText,
                    fontSize = 6.5.sp,
                    fontFamily = InterfaceFont
                )

                Spacer(Modifier.weight(1f))

                Box(
                    modifier =
                        Modifier
                            .size(26.dp)
                            .background(
                                AccentOrange,
                                CircleShape
                            )
                            .clickable {
                                onCreateNote(
                                    activeCategory ?: NoteCategory.PERSONAL
                                )
                            },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = Color.White,
                        fontSize = 18.sp,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(7.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                tabLabels.forEachIndexed { index, label ->
                    val selected = index == selectedTab
                    Column(
                        modifier =
                            Modifier
                                .clickable {
                                    selectedTab = index
                                }
                                .padding(horizontal = 2.dp, vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            color =
                                if (selected) AccentOrange
                                else SecondaryText,
                            fontSize = 9.5.sp,
                            lineHeight = 9.8.sp,
                            fontFamily = InterfaceFont,
                            fontWeight =
                                if (selected) FontWeight.Bold
                                else FontWeight.Normal
                        )
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier =
                                Modifier
                                    .height(1.dp)
                                    .width(22.dp)
                                    .background(
                                        if (selected) AccentOrange
                                        else Color.Transparent
                                    )
                        )
                    }
                }
            }

            MinimalDivider()
            Spacer(Modifier.height(4.dp))

            if (filteredNotes.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text =
                            if (activeCategory == null) "no notes"
                            else "no ${activeCategory.name.lowercase()} notes",
                        color = TertiaryText,
                        fontSize = 9.sp,
                        fontFamily = InterfaceFont
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    itemsIndexed(
                        items = filteredNotes,
                        key = { _, note -> note.id }
                    ) { index, note ->
                        NoteInboxRow(
                            note = note,
                            selected = index == selectedIndex,
                            onClick = {
                                selectedIndex = index
                                onOpenNote(note)
                            },
                            onDelete = {

                                /*
                                 * A swipe is easy to trigger by accident
                                 * and deleting a note cannot be undone,
                                 * so it asks first.
                                 */
                                pendingDeleteNote =
                                    note
                            }
                        )
                    }
                }
            }

        }

        val noteAwaitingDelete =
            pendingDeleteNote

        if (
            noteAwaitingDelete != null
        ) {

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
                                pendingDeleteNote =
                                    null
                            }
                        },
                contentAlignment =
                    Alignment.Center
            ) {

                Column(
                    modifier =
                        Modifier
                            .widthIn(
                                max = 190.dp
                            )
                            .background(
                                SurfaceBlack,
                                RoundedCornerShape(
                                    12.dp
                                )
                            )
                            .border(
                                0.8.dp,
                                BorderGray,
                                RoundedCornerShape(
                                    12.dp
                                )
                            )
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    // Consume taps inside the card.
                                }
                            }
                            .padding(
                                horizontal = 14.dp,
                                vertical = 12.dp
                            ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "delete?",
                        color = PrimaryText,
                        fontSize = 13.sp,
                        lineHeight = 13.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            noteAwaitingDelete.title
                                .ifBlank { "untitled note" },
                        color = SecondaryText,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        fontFamily = InterfaceFont,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(
                        modifier = Modifier.height(11.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(7.dp)
                    ) {

                        NoteConfirmButton(
                            label = "no",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                pendingDeleteNote = null
                            }
                        )

                        NoteConfirmButton(
                            label = "yes",
                            modifier = Modifier.weight(1f),
                            accent = true,
                            onClick = {

                                deleteNote(
                                    noteAwaitingDelete
                                )

                                pendingDeleteNote = null
                            }
                        )
                    }
                }
            }
        }

    }
}

@Composable
private fun NoteConfirmButton(
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: () -> Unit
) {

    Box(
        modifier =
            modifier
                .height(30.dp)
                .background(
                    if (accent) AccentOrange else InputSurface,
                    RoundedCornerShape(8.dp)
                )
                .border(
                    0.75.dp,
                    if (accent) AccentOrange else BorderGray,
                    RoundedCornerShape(8.dp)
                )
                .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = label,
            color = if (accent) BackgroundBlack else PrimaryText,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            fontFamily = InterfaceFont,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun NoteInboxRow(
    note: NoteItem,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var horizontalOffset by remember(note.id) {
        mutableStateOf(0f)
    }

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .background(
                    SurfaceBlack,
                    RoundedCornerShape(8.dp)
                )
    ) {
        Text(
            text = "DELETE",
            color = AccentOrange,
            fontSize = 8.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
        )

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .offset {
                        IntOffset(
                            horizontalOffset.roundToInt(),
                            0
                        )
                    }
                    .background(
                        if (selected) SurfaceBlack
                        else BackgroundBlack,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = if (selected) 0.75.dp else 0.dp,
                        color = if (selected) BorderGray else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .pointerInput(note.id) {
                        detectHorizontalDragGestures(
                            onHorizontalDrag = { _, dragAmount ->
                                horizontalOffset =
                                    (horizontalOffset + dragAmount)
                                        .coerceIn(-220f, 0f)
                            },
                            onDragEnd = {
                                if (horizontalOffset <= -110f) {
                                    onDelete()
                                }
                                horizontalOffset = 0f
                            },
                            onDragCancel = {
                                horizontalOffset = 0f
                            }
                        )
                    }
                    .clickable { onClick() }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            /*
             * The category's own icon stands in for the old marker, so
             * the list says what kind of note each row is at a glance.
             * Fixed width keeps every title starting at the same x.
             */
            Box(
                modifier = Modifier.width(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector =
                        when (note.category) {
                            NoteCategory.PERSONAL -> Icons.Outlined.Description
                            NoteCategory.WORK -> Icons.Outlined.Handyman
                            NoteCategory.IDEAS -> Icons.Outlined.Lightbulb
                            NoteCategory.JOURNAL -> Icons.Outlined.Edit
                        },
                    contentDescription =
                        note.category.name.lowercase(),
                    tint = AccentOrange,
                    modifier = Modifier.size(12.dp)
                )
            }

            Spacer(Modifier.width(6.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.title.ifBlank { "New note" },
                        color = PrimaryText,
                        fontSize = 9.5.sp,
                        fontFamily = InterfaceFont,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text =
                            "${note.category.name.lowercase()}  ${formatNoteDate(note.updatedAt)}",
                        color = TertiaryText,
                        fontSize = 5.8.sp,
                        fontFamily = InterfaceFont,
                        maxLines = 1
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text =
                        note.body
                            .replace("\n", " ")
                            .ifBlank { "empty note" },
                    color = SecondaryText,
                    fontSize = 7.5.sp,
                    lineHeight = 9.sp,
                    fontFamily = InterfaceFont,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

internal fun buildStyledNoteText(
    text: String,
    ranges: List<NoteStyleRange>
): AnnotatedString {
    val builder = AnnotatedString.Builder(text)

    ranges.forEach { range ->
        val start = range.start.coerceIn(0, text.length)
        val end = range.end.coerceIn(start, text.length)

        if (end > start) {
            val style =
                when (range.style) {
                    NoteTextStyle.BOLD ->
                        SpanStyle(fontWeight = FontWeight.Bold)

                    NoteTextStyle.ITALIC ->
                        SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)

                    NoteTextStyle.UNDERLINE ->
                        SpanStyle(textDecoration = TextDecoration.Underline)
                }

            builder.addStyle(
                style = style,
                start = start,
                end = end
            )
        }
    }

    var lineStart = 0
    while (lineStart <= text.length) {
        val lineEnd =
            text.indexOf('\n', lineStart)
                .let { if (it < 0) text.length else it }

        if (
            lineEnd - lineStart >= 2 &&
            text.startsWith("☑ ", lineStart)
        ) {
            val contentStart = (lineStart + 2).coerceAtMost(lineEnd)
            if (contentStart < lineEnd) {
                builder.addStyle(
                    style =
                        SpanStyle(
                            textDecoration = TextDecoration.LineThrough,
                            color = SecondaryText
                        ),
                    start = contentStart,
                    end = lineEnd
                )
            }
        }

        if (lineEnd >= text.length) break
        lineStart = lineEnd + 1
    }

    return builder.toAnnotatedString()
}

internal fun adjustNoteRangesForEdit(
    oldText: String,
    newText: String,
    ranges: List<NoteStyleRange>
): List<NoteStyleRange> {
    if (oldText == newText) return ranges

    var prefix = 0
    val minLength = minOf(oldText.length, newText.length)
    while (
        prefix < minLength &&
        oldText[prefix] == newText[prefix]
    ) {
        prefix++
    }

    var suffix = 0
    while (
        suffix < oldText.length - prefix &&
        suffix < newText.length - prefix &&
        oldText[oldText.length - 1 - suffix] ==
        newText[newText.length - 1 - suffix]
    ) {
        suffix++
    }

    val oldChangeEnd = oldText.length - suffix
    val newChangeEnd = newText.length - suffix
    val delta = newChangeEnd - oldChangeEnd

    return ranges
        .mapNotNull { range ->
            val adjusted =
                when {
                    range.end <= prefix -> range

                    range.start >= oldChangeEnd ->
                        range.copy(
                            start = range.start + delta,
                            end = range.end + delta
                        )

                    else ->
                        range.copy(
                            start = minOf(range.start, prefix),
                            end = (range.end + delta).coerceAtLeast(prefix)
                        )
                }

            val start = adjusted.start.coerceIn(0, newText.length)
            val end = adjusted.end.coerceIn(start, newText.length)

            if (end > start) {
                adjusted.copy(start = start, end = end)
            } else {
                null
            }
        }
}

internal fun insertedNoteTextRange(
    oldText: String,
    newText: String
): TextRange? {
    if (newText.length <= oldText.length) return null

    var prefix = 0
    val minLength = minOf(oldText.length, newText.length)
    while (
        prefix < minLength &&
        oldText[prefix] == newText[prefix]
    ) {
        prefix++
    }

    var suffix = 0
    while (
        suffix < oldText.length - prefix &&
        suffix < newText.length - prefix &&
        oldText[oldText.length - 1 - suffix] ==
        newText[newText.length - 1 - suffix]
    ) {
        suffix++
    }

    val newChangeEnd = newText.length - suffix
    return if (newChangeEnd > prefix) {
        TextRange(prefix, newChangeEnd)
    } else {
        null
    }
}

internal fun mergeNoteStyleRanges(
    ranges: List<NoteStyleRange>
): List<NoteStyleRange> {
    return ranges
        .groupBy { it.style }
        .flatMap { (style, styleRanges) ->
            val sorted = styleRanges.sortedBy { it.start }
            val merged = mutableListOf<NoteStyleRange>()

            sorted.forEach { range ->
                val last = merged.lastOrNull()
                if (
                    last != null &&
                    range.start <= last.end
                ) {
                    merged[merged.lastIndex] =
                        last.copy(
                            end = maxOf(last.end, range.end)
                        )
                } else {
                    merged.add(
                        range.copy(style = style)
                    )
                }
            }

            merged
        }
        .sortedWith(
            compareBy<NoteStyleRange> { it.start }
                .thenBy { it.end }
                .thenBy { it.style.name }
        )
}

internal data class ChecklistEditResult(
    val text: String,
    val selection: TextRange,
    val handled: Boolean
)

internal fun shouldResetNoteTypingStylesOnEnter(
    oldValue: TextFieldValue,
    incoming: TextFieldValue
): Boolean {
    if (!oldValue.selection.collapsed) return false

    val oldText = oldValue.text
    val cursor = oldValue.selection.start.coerceIn(0, oldText.length)
    val expected =
        oldText.substring(0, cursor) +
                "\n" +
                oldText.substring(cursor)

    if (incoming.text != expected) return false

    val lineStart =
        oldText.lastIndexOf(
            '\n',
            (cursor - 1).coerceAtLeast(0)
        )
            .let { if (it < 0) 0 else it + 1 }

    val lineEnd =
        oldText.indexOf('\n', cursor)
            .let { if (it < 0) oldText.length else it }

    if (cursor != lineEnd) return false

    val line = oldText.substring(lineStart, lineEnd)

    return line.isBlank() ||
            line == "☐ " ||
            line == "☑ " ||
            line == "• "
}

internal fun applyChecklistEnterBehavior(
    oldValue: TextFieldValue,
    incoming: TextFieldValue
): ChecklistEditResult {
    val oldText = oldValue.text
    val oldSelection = oldValue.selection

    if (!oldSelection.collapsed) {
        return ChecklistEditResult(
            text = incoming.text,
            selection = incoming.selection,
            handled = false
        )
    }

    val cursor = oldSelection.start.coerceIn(0, oldText.length)
    val expected =
        oldText.substring(0, cursor) +
                "\n" +
                oldText.substring(cursor)

    if (incoming.text != expected) {
        return ChecklistEditResult(
            text = incoming.text,
            selection = incoming.selection,
            handled = false
        )
    }

    val lineStart =
        oldText.lastIndexOf(
            '\n',
            (cursor - 1).coerceAtLeast(0)
        )
            .let { if (it < 0) 0 else it + 1 }

    val lineEnd =
        oldText.indexOf('\n', cursor)
            .let { if (it < 0) oldText.length else it }

    if (cursor != lineEnd) {
        return ChecklistEditResult(
            text = incoming.text,
            selection = incoming.selection,
            handled = false
        )
    }

    val line = oldText.substring(lineStart, lineEnd)
    val prefix =
        when {
            line.startsWith("☐ ") -> "☐ "
            line.startsWith("☑ ") -> "☑ "
            line.startsWith("• ") -> "• "
            else -> null
        }
            ?: return ChecklistEditResult(
                text = incoming.text,
                selection = incoming.selection,
                handled = false
            )

    val content = line.removePrefix(prefix)

    return if (content.isBlank()) {
        val newText =
            oldText.removeRange(
                lineStart,
                (lineStart + prefix.length)
                    .coerceAtMost(oldText.length)
            )

        ChecklistEditResult(
            text = newText,
            selection = TextRange(lineStart.coerceIn(0, newText.length)),
            handled = true
        )
    } else {
        val nextPrefix =
            if (prefix == "• ") {
                "• "
            } else {
                "☐ "
            }

        val insertion = "\n$nextPrefix"
        val newText =
            oldText.substring(0, cursor) +
                    insertion +
                    oldText.substring(cursor)

        ChecklistEditResult(
            text = newText,
            selection = TextRange(cursor + insertion.length),
            handled = true
        )
    }
}

@Composable
fun NoteDetailScreen(
    note: NoteItem,
    noteStore: NoteStore,
    onNoteUpdated: (NoteItem) -> Unit,
    onBack: () -> Unit
) {
    var title by remember(note.id) {
        mutableStateOf(note.title)
    }
    var category by remember(note.id) {
        mutableStateOf(note.category)
    }
    var styleRanges by remember(note.id) {
        mutableStateOf(note.styleRanges)
    }
    var bodyValue by remember(note.id) {
        mutableStateOf(
            TextFieldValue(
                annotatedString =
                    buildStyledNoteText(
                        note.body,
                        note.styleRanges
                    ),
                selection = TextRange(note.body.length)
            )
        )
    }
    var activeBold by remember(note.id) {
        mutableStateOf(false)
    }
    var activeItalic by remember(note.id) {
        mutableStateOf(false)
    }
    var activeUnderline by remember(note.id) {
        mutableStateOf(false)
    }
    var bodyLayoutResult by remember(note.id) {
        mutableStateOf<TextLayoutResult?>(null)
    }
    val bodyFocusRequester = remember(note.id) {
        FocusRequester()
    }

    fun persist() {
        val updated =
            noteStore.updateNote(
                note.copy(
                    title = title,
                    body = bodyValue.text,
                    category = category,
                    styleRanges = styleRanges
                )
            )
        onNoteUpdated(updated)
    }

    fun updateBody(
        text: String,
        selection: TextRange,
        newRanges: List<NoteStyleRange> = styleRanges
    ) {
        styleRanges = mergeNoteStyleRanges(newRanges)
        bodyValue =
            TextFieldValue(
                annotatedString =
                    buildStyledNoteText(
                        text,
                        styleRanges
                    ),
                selection =
                    TextRange(
                        selection.start.coerceIn(0, text.length),
                        selection.end.coerceIn(0, text.length)
                    )
            )
        persist()
    }

    fun isStyleActive(style: NoteTextStyle): Boolean {
        return when (style) {
            NoteTextStyle.BOLD -> activeBold
            NoteTextStyle.ITALIC -> activeItalic
            NoteTextStyle.UNDERLINE -> activeUnderline
        }
    }

    fun setStyleActive(
        style: NoteTextStyle,
        active: Boolean
    ) {
        when (style) {
            NoteTextStyle.BOLD -> activeBold = active
            NoteTextStyle.ITALIC -> activeItalic = active
            NoteTextStyle.UNDERLINE -> activeUnderline = active
        }
    }

    fun toggleStyle(style: NoteTextStyle) {
        val selection = bodyValue.selection

        if (selection.collapsed) {
            setStyleActive(
                style,
                !isStyleActive(style)
            )
            bodyFocusRequester.requestFocus()
            return
        }

        val target = selection
        val overlapping =
            styleRanges.any { range ->
                range.style == style &&
                        range.start < target.end &&
                        range.end > target.start
            }

        val updatedRanges =
            if (overlapping) {
                styleRanges.filterNot { range ->
                    range.style == style &&
                            range.start < target.end &&
                            range.end > target.start
                }
            } else {
                styleRanges +
                        NoteStyleRange(
                            start = target.start,
                            end = target.end,
                            style = style
                        )
            }

        updateBody(
            text = bodyValue.text,
            selection = target,
            newRanges = updatedRanges
        )
        bodyFocusRequester.requestFocus()
    }

    fun toggleLinePrefix(
        firstPrefix: String,
        secondPrefix: String? = null
    ) {
        val text = bodyValue.text
        val cursor = bodyValue.selection.start.coerceIn(0, text.length)
        val lineStart =
            text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
                .let { if (it < 0) 0 else it + 1 }
        val before = text.substring(0, lineStart)
        val after = text.substring(lineStart)

        val (replacementAfter, delta) =
            when {
                secondPrefix != null && after.startsWith(secondPrefix) ->
                    firstPrefix + after.removePrefix(secondPrefix) to
                            (firstPrefix.length - secondPrefix.length)

                after.startsWith(firstPrefix) && secondPrefix != null ->
                    secondPrefix + after.removePrefix(firstPrefix) to
                            (secondPrefix.length - firstPrefix.length)

                after.startsWith(firstPrefix) ->
                    after.removePrefix(firstPrefix) to -firstPrefix.length

                else ->
                    firstPrefix + after to firstPrefix.length
            }

        val newText = before + replacementAfter
        val newRanges =
            adjustNoteRangesForEdit(
                oldText = text,
                newText = newText,
                ranges = styleRanges
            )
        val newCursor =
            (cursor + delta)
                .coerceIn(0, newText.length)

        updateBody(
            text = newText,
            selection = TextRange(newCursor),
            newRanges = newRanges
        )
    }

    fun toggleChecklistPrefix() {
        val text = bodyValue.text
        val cursor = bodyValue.selection.start.coerceIn(0, text.length)
        val lineStart =
            text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
                .let { if (it < 0) 0 else it + 1 }
        val before = text.substring(0, lineStart)
        val after = text.substring(lineStart)

        val (replacementAfter, delta) =
            when {
                after.startsWith("☐ ") ->
                    after.removePrefix("☐ ") to -2

                after.startsWith("☑ ") ->
                    after.removePrefix("☑ ") to -2

                else ->
                    "☐ " + after to 2
            }

        val newText = before + replacementAfter
        val newRanges =
            adjustNoteRangesForEdit(
                oldText = text,
                newText = newText,
                ranges = styleRanges
            )
        val newCursor =
            (cursor + delta).coerceIn(0, newText.length)

        updateBody(
            text = newText,
            selection = TextRange(newCursor),
            newRanges = newRanges
        )
        bodyFocusRequester.requestFocus()
    }

    fun toggleCheckboxAtOffset(offset: Int) {
        val text = bodyValue.text
        if (text.isEmpty()) return

        val safeOffset = offset.coerceIn(0, text.length)
        val lineStart =
            text.lastIndexOf(
                '\n',
                (safeOffset - 1).coerceAtLeast(0)
            )
                .let { if (it < 0) 0 else it + 1 }

        val lineEnd =
            text.indexOf('\n', safeOffset)
                .let { if (it < 0) text.length else it }

        if (lineStart >= lineEnd) return

        val line = text.substring(lineStart, lineEnd)
        val replacementPrefix =
            when {
                line.startsWith("☐ ") -> "☑ "
                line.startsWith("☑ ") -> "☐ "
                else -> return
            }

        if (safeOffset > lineStart + 2) return

        val newText =
            text.replaceRange(
                lineStart,
                (lineStart + 2).coerceAtMost(text.length),
                replacementPrefix
            )

        updateBody(
            text = newText,
            selection = bodyValue.selection,
            newRanges = styleRanges
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BackgroundBlack)
                .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = CameraSafeStartPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LauncherBackButton(onBack = onBack)

            Spacer(Modifier.width(8.dp))

            Text(
                text = "note",
                color = PrimaryText,
                fontSize = 12.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = "saved",
                color = TertiaryText,
                fontSize = 6.sp,
                fontFamily = InterfaceFont
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            NoteCategory.entries.forEach { option ->
                val selected = category == option
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .height(24.dp)
                            .background(
                                if (selected) AccentOrange
                                else SurfaceBlack,
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                0.75.dp,
                                if (selected) AccentOrange else BorderGray,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                category = option
                                persist()
                            },
                    contentAlignment = Alignment.Center
                ) {
                    /*
                     * Explicit lineHeight and centred textAlign:
                     * without them the label is centred by its line
                     * box, and Poppins reports more descent than
                     * ascent, so it sits high in the button.
                     */
                    Text(
                        text = option.name.lowercase(),
                        color =
                            if (selected) Color.White
                            else SecondaryText,
                        fontSize = 6.5.sp,
                        lineHeight = 6.5.sp,
                        fontFamily = InterfaceFont,
                        textAlign = TextAlign.Center,
                        fontWeight =
                            if (selected) FontWeight.Bold
                            else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        BasicTextField(
            value = title,
            onValueChange = { newTitle ->
                title = newTitle.take(80)
                persist()
            },
            textStyle =
                TextStyle(
                    color = PrimaryText,
                    fontSize = 16.sp,
                    lineHeight = 18.sp,
                    fontFamily = InterfaceFont,
                    fontWeight = FontWeight.Bold
                ),
            cursorBrush = SolidColor(PrimaryText),
            singleLine = true,
            modifier =
                Modifier
                    .fillMaxWidth()
                    /*
                     * A minimum rather than a fixed height: 16.sp
                     * bold Poppins needs more room than 38.dp minus
                     * its padding leaves, so descenders were being
                     * clipped.
                     */
                    .heightIn(min = 38.dp)
                    .background(
                        InputSurface,
                        RoundedCornerShape(7.dp)
                    )
                    .border(
                        0.75.dp,
                        BorderGray,
                        RoundedCornerShape(7.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 9.dp)
        )

        Spacer(Modifier.height(5.dp))

        BasicTextField(
            value = bodyValue,
            onValueChange = { incoming ->
                val resetTypingStyles =
                    shouldResetNoteTypingStylesOnEnter(
                        oldValue = bodyValue,
                        incoming = incoming
                    )

                if (resetTypingStyles) {
                    activeBold = false
                    activeItalic = false
                    activeUnderline = false
                }

                val checklistResult =
                    applyChecklistEnterBehavior(
                        oldValue = bodyValue,
                        incoming = incoming
                    )

                val nextText = checklistResult.text
                var newRanges =
                    adjustNoteRangesForEdit(
                        oldText = bodyValue.text,
                        newText = nextText,
                        ranges = styleRanges
                    )

                if (!checklistResult.handled && !resetTypingStyles) {
                    val insertedRange =
                        insertedNoteTextRange(
                            oldText = bodyValue.text,
                            newText = nextText
                        )

                    if (
                        insertedRange != null &&
                        !insertedRange.collapsed
                    ) {
                        if (activeBold) {
                            newRanges =
                                newRanges +
                                        NoteStyleRange(
                                            start = insertedRange.start,
                                            end = insertedRange.end,
                                            style = NoteTextStyle.BOLD
                                        )
                        }
                        if (activeItalic) {
                            newRanges =
                                newRanges +
                                        NoteStyleRange(
                                            start = insertedRange.start,
                                            end = insertedRange.end,
                                            style = NoteTextStyle.ITALIC
                                        )
                        }
                        if (activeUnderline) {
                            newRanges =
                                newRanges +
                                        NoteStyleRange(
                                            start = insertedRange.start,
                                            end = insertedRange.end,
                                            style = NoteTextStyle.UNDERLINE
                                        )
                        }
                    }
                }

                styleRanges = mergeNoteStyleRanges(newRanges)
                bodyValue =
                    TextFieldValue(
                        annotatedString =
                            buildStyledNoteText(
                                nextText,
                                styleRanges
                            ),
                        selection = checklistResult.selection,
                        composition =
                            if (checklistResult.handled) null
                            else incoming.composition
                    )
                persist()
            },
            textStyle =
                TextStyle(
                    color = PrimaryText,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontFamily = InterfaceFont
                ),
            cursorBrush = SolidColor(PrimaryText),
            onTextLayout = { result ->
                bodyLayoutResult = result
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .focusRequester(bodyFocusRequester)
                    .background(
                        InputSurface,
                        RoundedCornerShape(7.dp)
                    )
                    .border(
                        0.75.dp,
                        BorderGray,
                        RoundedCornerShape(7.dp)
                    )
                    .padding(8.dp)
                    .pointerInput(bodyValue.text, bodyLayoutResult) {
                        awaitPointerEventScope {
                            var checkboxPressLineStart: Int? = null

                            while (true) {
                                val event =
                                    awaitPointerEvent(
                                        androidx.compose.ui.input.pointer.PointerEventPass.Initial
                                    )

                                val change =
                                    event.changes.firstOrNull()
                                        ?: continue

                                val layout =
                                    bodyLayoutResult

                                if (change.pressed && !change.previousPressed) {
                                    checkboxPressLineStart = null

                                    if (layout != null) {
                                        val position = change.position

                                        if (
                                            position.x <= 72.dp.toPx() &&
                                            position.y >= 0f &&
                                            position.y <= layout.size.height.toFloat()
                                        ) {
                                            val safeY =
                                                position.y.coerceIn(
                                                    0f,
                                                    (layout.size.height - 1)
                                                        .coerceAtLeast(0)
                                                        .toFloat()
                                                )
                                            val lineIndex =
                                                layout.getLineForVerticalPosition(safeY)
                                            val lineStart =
                                                layout.getLineStart(lineIndex)
                                                    .coerceIn(
                                                        0,
                                                        bodyValue.text.length
                                                    )
                                            val lineEnd =
                                                layout.getLineEnd(
                                                    lineIndex,
                                                    visibleEnd = true
                                                )
                                                    .coerceIn(
                                                        lineStart,
                                                        bodyValue.text.length
                                                    )
                                            val lineText =
                                                bodyValue.text.substring(
                                                    lineStart,
                                                    lineEnd
                                                )

                                            if (
                                                lineText.startsWith("☐ ") ||
                                                lineText.startsWith("☑ ")
                                            ) {
                                                checkboxPressLineStart = lineStart
                                                change.consume()
                                            }
                                        }
                                    }
                                } else if (!change.pressed && change.previousPressed) {
                                    val lineStart = checkboxPressLineStart

                                    if (lineStart != null) {
                                        change.consume()
                                        toggleCheckboxAtOffset(lineStart)
                                        checkboxPressLineStart = null
                                    }
                                } else if (checkboxPressLineStart != null) {
                                    change.consume()
                                }
                            }
                        }
                    },
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (bodyValue.text.isBlank()) {
                        Text(
                            text = "write your note...",
                            color = TertiaryText,
                            fontSize = 10.sp,
                            fontFamily = InterfaceFont
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(Modifier.height(5.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoteToolbarButton(
                label = "B",
                bold = true,
                selected = activeBold,
                onClick = {
                    toggleStyle(NoteTextStyle.BOLD)
                }
            )
            NoteToolbarButton(
                label = "I",
                italic = true,
                selected = activeItalic,
                onClick = {
                    toggleStyle(NoteTextStyle.ITALIC)
                }
            )
            NoteToolbarButton(
                label = "U",
                underline = true,
                selected = activeUnderline,
                onClick = {
                    toggleStyle(NoteTextStyle.UNDERLINE)
                }
            )
            NoteToolbarButton(
                label = "☐",
                onClick = {
                    toggleChecklistPrefix()
                }
            )
            NoteToolbarButton(
                label = "•",
                onClick = {
                    toggleLinePrefix(
                        firstPrefix = "• "
                    )
                    bodyFocusRequester.requestFocus()
                }
            )
        }

        Spacer(Modifier.height(3.dp))

    }
}

@Composable
internal fun NoteToolbarButton(
    label: String,
    bold: Boolean = false,
    italic: Boolean = false,
    underline: Boolean = false,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .width(46.dp)
                .height(28.dp)
                .background(
                    if (selected) AccentOrange else SurfaceBlack,
                    RoundedCornerShape(6.dp)
                )
                .border(
                    0.75.dp,
                    if (selected) AccentOrange else BorderGray,
                    RoundedCornerShape(6.dp)
                )
                .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else PrimaryText,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            textAlign = TextAlign.Center,
            fontFamily = InterfaceFont,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            fontStyle =
                if (italic) androidx.compose.ui.text.font.FontStyle.Italic
                else androidx.compose.ui.text.font.FontStyle.Normal,
            textDecoration =
                if (underline) TextDecoration.Underline
                else TextDecoration.None
        )
    }
}

fun formatNoteDate(
    timestamp: Long
): String {

    return SimpleDateFormat(
        "MMM d · h:mm a",
        Locale.getDefault()
    )
        .format(
            Date(
                timestamp
            )
        )
        .lowercase()
}