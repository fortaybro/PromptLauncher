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
import android.provider.Telephony
import android.provider.Settings
import android.view.ViewTreeObserver
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
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.ui.platform.LocalView
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
import com.forrest.titanlauncher.ai.AssistantSharingStore
import com.forrest.titanlauncher.ai.buildAssistantLauncherContext
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TitanHomeScreen(
    contactRepository: ContactRepository,
    smsDatabase: SmsDatabase,
    smsRoleManager: SmsRoleManager,
    todoistTokenStore: TodoistTokenStore,
    todoistRepository: TodoistRepository,
    smsRoleLauncher: (Intent) -> Unit,
    hasContactsPermission: Boolean,
    hasCalendarPermission: Boolean,
    onRequestContactsPermission: () -> Unit,
    onRequestCalendarPermission: () -> Unit,
    onCreateCalendarEvent: (CalendarEventDraft) -> Unit,
    onCalendarSetup: () -> Unit,
    showCalendarSetup: Boolean,
    calendarSetupOptions: List<GoogleCalendarOption>,
    currentCalendarId: String?,
    onSaveCalendarSelection: (GoogleCalendarOption) -> Unit,
    onDismissCalendarSetup: () -> Unit,
    onCalendarStatus: () -> Unit,
    onCreateNote: (String, NoteCategory) -> Unit,
    onOpenNotes: () -> Unit,
    mailMessages: List<MailMessage>,
    onOpenMail: () -> Unit,
    onComposeMail: (EmailContact) -> Unit,
    onOpenMessagesInbox: () -> Unit,
    onOpenConversation: (Contact) -> Unit,
    onOpenGroupThread: (
        List<String>,
        List<String>,
        String
    ) -> Unit,
    onSendMessage: (
        Contact,
        String
    ) -> Unit,
    onOpenHub: () -> Unit,
    upcomingEvent: UpcomingCalendarEvent?,
    calendarAgendaEvents: List<UpcomingCalendarEvent>,
    statusText: String,
    setStatusText: (String) -> Unit,
    commandOverlayOnly: Boolean = false,
    autoFocusCommandInput: Boolean = true,
    showSearchTargetPicker: Boolean = true,
    onCommandOverlayActiveChange: (Boolean) -> Unit = {}
) {

    var commandText by remember {
        mutableStateOf("")
    }

    var showCommandHelp by remember {
        mutableStateOf(false)
    }

    var showAppSearch by remember {
        mutableStateOf(false)
    }

    var showFavoriteApps by remember {
        mutableStateOf(false)
    }

    var showQuickToggles by remember {
        mutableStateOf(false)
    }

    var showQuickReply by remember {
        mutableStateOf(false)
    }

    val soundModeActive =
        rememberSoundModeActive()

    val batterySnapshot =
        rememberBatterySnapshot()

    var showBattery by remember {
        mutableStateOf(false)
    }

    var favoriteAssignSlot by remember {
        mutableStateOf<Int?>(null)
    }

    var assistantQuestion by remember {
        mutableStateOf<String?>(null)
    }

    var assistantAnswer by remember {
        mutableStateOf<String?>(null)
    }

    var assistantTurns by remember {
        mutableStateOf(
            emptyList<AssistantTurn>()
        )
    }

    var assistantThinking by remember {
        mutableStateOf(false)
    }

    var assistantNavigationTarget by remember {
        mutableStateOf<String?>(
            null
        )
    }

    var showGeminiSetup by remember {
        mutableStateOf(false)
    }

    val context =
        LocalContext.current

    val launcherView =
        LocalView.current

    val favoriteAppsStore =
        remember {
            FavoriteAppsStore(
                context
            )
        }

    var favoriteApps by remember {
        mutableStateOf(
            favoriteAppsStore.load()
        )
    }

    val recentSearchPrefs =
        remember {
            context.getSharedPreferences(
                "prompt_launcher_recent_searches",
                Context.MODE_PRIVATE
            )
        }

    fun loadRecentWebSearches(): List<String> {

        return recentSearchPrefs
            .getStringSet(
                "recent_searches",
                emptySet()
            )
            .orEmpty()
            .toList()
            .sortedByDescending {
                recentSearchPrefs.getLong(
                    "recent_search_time_${it.hashCode()}",
                    0L
                )
            }
            .take(
                5
            )
    }

    var recentWebSearches by remember {
        mutableStateOf(
            loadRecentWebSearches()
        )
    }

    fun saveRecentWebSearch(
        query: String
    ) {

        val cleaned =
            query.trim()

        if (
            cleaned.isBlank()
        ) {
            return
        }

        val updated =
            (
                    listOf(
                        cleaned
                    ) +
                            recentWebSearches.filterNot {
                                it.equals(
                                    cleaned,
                                    ignoreCase =
                                        true
                                )
                            }
                    )
                /*
                 * Two is enough to be useful without crowding out the
                 * suggestion list beneath it.
                 */
                .take(
                    2
                )

        val editor =
            recentSearchPrefs
                .edit()
                .putStringSet(
                    "recent_searches",
                    updated.toSet()
                )

        updated.forEachIndexed {
                index,
                item ->

            editor.putLong(
                "recent_search_time_${item.hashCode()}",
                System.currentTimeMillis() -
                        index
            )
        }

        editor.apply()

        recentWebSearches =
            updated
    }

    fun removeRecentWebSearch(
        query: String
    ) {

        val updated =
            recentWebSearches
                .filterNot {
                    it ==
                            query
                }

        recentSearchPrefs
            .edit()
            .putStringSet(
                "recent_searches",
                updated.toSet()
            )
            .remove(
                "recent_search_time_${query.hashCode()}"
            )
            .apply()

        recentWebSearches =
            updated
    }

    fun clearRecentWebSearches() {

        recentSearchPrefs
            .edit()
            .clear()
            .apply()

        recentWebSearches =
            emptyList()
    }

    val usageStatsRepository =
        remember {
            UsageStatsRepository(
                context
            )
        }

    val geminiApiKeyStore =
        remember {
            GeminiApiKeyStore(
                context
            )
        }

    val assistantSharingStore =
        remember {
            AssistantSharingStore(
                context
            )
        }

    var assistantSharing by
    remember {
        mutableStateOf(
            assistantSharingStore.load()
        )
    }

    val geminiRepository =
        remember {
            GeminiRepository()
        }

    val scope =
        rememberCoroutineScope()

    val weatherRepository =
        remember {
            WeatherRepository(
                context
            )
        }

    var weatherSnapshot by remember {
        mutableStateOf<WeatherSnapshot?>(
            null
        )
    }

    fun refreshWeather() {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            weatherSnapshot =
                null
            return
        }

        scope.launch {
            weatherSnapshot =
                weatherRepository
                    .loadCurrentWeather()
        }
    }

    val weatherPermissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (
                granted
            ) {
                setStatusText(
                    "✓ WEATHER READY"
                )
                refreshWeather()
            } else {
                setStatusText(
                    "WEATHER LOCATION NOT ENABLED"
                )
            }
        }

    LaunchedEffect(Unit) {
        while (
            true
        ) {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                weatherSnapshot =
                    weatherRepository
                        .loadCurrentWeather()
            }

            delay(
                15 * 60 * 1000L
            )
        }
    }

    val focusRequester =
        remember {
            FocusRequester()
        }

    val recentMessages by
    smsDatabase
        .smsDao()
        .observeRecentMessages()
        .collectAsStateWithLifecycle(
            initialValue =
                emptyList()
        )

    val quickReplyNotifications by
    NotificationCenter
        .notifications
        .collectAsStateWithLifecycle()

    /*
     * Recomputed only when the underlying lists change. The contact
     * lookups are the same ones the messages inbox performs, and the
     * list is short, so this stays cheap.
     */
    val quickReplyEntries =
        remember(
            recentMessages,
            quickReplyNotifications,
            hasContactsPermission
        ) {

            buildQuickReplyEntries(
                recentMessages =
                    recentMessages,
                notifications =
                    quickReplyNotifications,
                contactNameFor = { number ->

                    if (
                        hasContactsPermission
                    ) {
                        contactRepository
                            .findContactNameByPhoneNumber(
                                number
                            )
                    } else {
                        null
                    }
                },
                contactPhotoFor = { number ->

                    if (
                        hasContactsPermission
                    ) {
                        contactRepository
                            .findContactPhotoUriByPhoneNumber(
                                number
                            )
                    } else {
                        null
                    }
                }
            )
        }

    val storedUnreadCount by
    smsDatabase
        .smsDao()
        .observeUnreadCount()
        .collectAsStateWithLifecycle(
            initialValue =
                0
        )

    val liveNotifications by
    NotificationCenter
        .notifications
        .collectAsStateWithLifecycle()

    /*
     * When another app owns SMS, Prompt Launcher's own database never
     * sees a text, so the card would always read "all caught up"
     * while the hub and the bolt both show waiting messages. In that
     * case the count comes from the messaging app's notifications
     * instead, which is the same source those two already use.
     */
    val unreadCount =
        if (
            storedUnreadCount > 0
        ) {
            storedUnreadCount
        } else {

            liveNotifications.count { notification ->

                notification.messages.isNotEmpty()
            }
        }

    val currentDateTime by
    produceState(
        initialValue =
            Date()
    ) {

        while (
            true
        ) {

            value =
                Date()

            delay(
                30_000
            )
        }
    }

    val productivityHours by
    produceState(
        initialValue =
            List(24) {
                HourProductivityStatus.GRAY
            }
    ) {
        while (
            true
        ) {
            value =
                withContext(
                    Dispatchers.Default
                ) {
                    usageStatsRepository
                        .loadTodayHourlyStatuses()
                }

            delay(
                60_000
            )
        }
    }

    val currentDay =
        SimpleDateFormat(
            "EEEE",
            Locale.getDefault()
        )
            .format(
                currentDateTime
            )
            .lowercase()

    val currentDate =
        SimpleDateFormat(
            "MMM d",
            Locale.getDefault()
        )
            .format(
                currentDateTime
            )
            .lowercase()

    val currentTime =
        SimpleDateFormat(
            "h:mm a",
            Locale.getDefault()
        )
            .format(
                currentDateTime
            )
            .lowercase()

    val latestUnread =
        recentMessages
            .firstOrNull {

                it.incoming &&
                        !it.isRead
            }

    /*
     * Newest message from a notification-backed conversation, used
     * when nothing is stored locally.
     */
    val liveConversationPreview =
        liveNotifications
            .filter {
                it.messages.isNotEmpty()
            }
            .maxByOrNull {
                it.timestamp
            }
            ?.let { notification ->

                val sender =
                    notification.title
                        .ifBlank {
                            notification.appName
                        }

                val body =
                    notification
                        .messages
                        .lastOrNull()
                        ?.text
                        .orEmpty()

                if (
                    body.isBlank()
                ) {
                    null
                } else {
                    sender + ": " + body
                }
            }

    val importantUnreadMail =
        mailMessages
            .filter {

                it.important &&
                        it.unread
            }
            .maxByOrNull {

                it.timestamp
            }

    val importantUnreadMailCount =
        mailMessages.count {

            it.important &&
                    it.unread
        }

    var inlineContactSelectionIndex by remember {
        mutableIntStateOf(
            0
        )
    }

    var emailContactMatches by remember {
        mutableStateOf(
            emptyList<EmailContact>()
        )
    }

    /*
     * When another app owns SMS, Prompt Launcher's own messaging
     * surfaces would be empty — Android delivers texts only to the
     * default app. So the messaging entry points hand off instead of
     * showing a thread that cannot receive anything.
     *
     * Checked at the moment of the tap rather than at composition, so
     * switching the default in settings takes effect immediately.
     *
     * Returns true when the handoff happened.
     */
    fun handOffMessagingIfNotDefault(
        phoneNumber: String? = null
    ): Boolean {

        val isDefault =
            runCatching {
                smsRoleManager.isDefaultSmsApp()
            }
                .getOrDefault(
                    true
                )

        if (
            isDefault
        ) {
            return false
        }

        val intent =
            if (
                phoneNumber.isNullOrBlank()
            ) {

                runCatching {
                    Telephony.Sms
                        .getDefaultSmsPackage(
                            context
                        )
                        ?.let { packageName ->

                            context
                                .packageManager
                                .getLaunchIntentForPackage(
                                    packageName
                                )
                        }
                }
                    .getOrNull()

            } else {

                Intent(
                    Intent.ACTION_SENDTO,
                    Uri.parse(
                        "smsto:" + phoneNumber
                    )
                )
            }

        if (
            intent == null
        ) {

            setStatusText(
                "NO MESSAGING APP FOUND"
            )

            return true
        }

        runCatching {
            context.startActivity(
                intent
            )
        }
            .onFailure {
                setStatusText(
                    "COULD NOT OPEN MESSAGES"
                )
            }

        return true
    }

    fun performResolvedContactCommand(
        command: Command,
        contact: Contact
    ) {

        when (command) {

            is Command.SendMessage -> {

                if (
                    !handOffMessagingIfNotDefault(
                        contact.phoneNumber
                    )
                ) {
                    onSendMessage(
                        contact,
                        command.message
                    )
                }
            }

            is Command.OpenConversation -> {

                if (
                    !handOffMessagingIfNotDefault(
                        contact.phoneNumber
                    )
                ) {
                    onOpenConversation(
                        contact
                    )
                }
            }

            is Command.CallContact ->
                context.startActivity(
                    Intent(
                        Intent.ACTION_DIAL,
                        Uri.parse(
                            "tel:${contact.phoneNumber}"
                        )
                    )
                )

            else -> Unit
        }
    }

    /*
     * Recognises a raw phone number typed after @ or / so it can be
     * dialled or messaged without existing in contacts.
     *
     * Accepts ordinary number punctuation and an optional leading
     * country code. Anything containing a letter is treated as a name
     * and falls through to the usual contact lookup.
     */
    fun directPhoneNumberOrNull(
        value: String
    ): String? {

        val trimmed =
            value.trim()

        if (
            trimmed.isBlank()
        ) {
            return null
        }

        val allowedSeparators =
            setOf(
                ' ',
                '-',
                '(',
                ')',
                '.',
                '+'
            )

        val looksNumeric =
            trimmed.all {
                it.isDigit() ||
                        it in allowedSeparators
            }

        if (
            !looksNumeric
        ) {
            return null
        }

        /*
         * A plus sign only means anything at the front.
         */
        if (
            trimmed.indexOf('+') > 0
        ) {
            return null
        }

        val digits =
            trimmed.filter {
                it.isDigit()
            }

        /*
         * Seven digits is the shortest dialable local number and
         * fifteen is the E.164 ceiling. Outside that range this is
         * more likely a typo than a number.
         */
        if (
            digits.length < 7 ||
            digits.length > 15
        ) {
            return null
        }

        return if (
            trimmed.startsWith("+")
        ) {
            "+" + digits
        } else {
            digits
        }
    }

    fun resolveContactCommand(
        command: Command,
        searchName: String
    ) {

        /*
         * Checked before the contacts permission gate: dialling or
         * texting a number the user typed out in full needs no access
         * to their contacts at all.
         */
        val directNumber =
            directPhoneNumberOrNull(
                searchName
            )

        if (
            directNumber != null
        ) {

            performResolvedContactCommand(
                command,
                Contact(
                    name =
                        directNumber,
                    phoneNumber =
                        directNumber
                )
            )

            return
        }

        if (
            !hasContactsPermission
        ) {

            onRequestContactsPermission()
            return
        }

        val normalizedSearchName =
            searchName
                .trim()

        val matches =
            contactRepository
                .findContacts(
                    normalizedSearchName,
                    16
                )
                .filter { contact ->
                    contact.name
                        .trim()
                        .startsWith(
                            normalizedSearchName,
                            ignoreCase = true
                        )
                }
                .take(
                    8
                )

        val exactMatches =
            matches.filter { contact ->
                contact.name
                    .trim()
                    .equals(
                        normalizedSearchName,
                        ignoreCase = true
                    )
            }

        val chosenContact =
            when {
                exactMatches.size == 1 ->
                    exactMatches.first()

                matches.size == 1 ->
                    matches.first()

                else ->
                    null
            }

        when {

            chosenContact != null ->
                performResolvedContactCommand(
                    command,
                    chosenContact
                )

            matches.isEmpty() ->
                setStatusText(
                    "CONTACT NOT FOUND"
                )

            else ->
                Unit
        }
    }

    fun isEmailAddress(
        value: String
    ): Boolean {

        return Regex(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            RegexOption.IGNORE_CASE
        )
            .matches(
                value.trim()
            )
    }

    fun startEmailCompose(
        target: String
    ) {

        val trimmedTarget =
            target.trim()

        if (
            trimmedTarget.isBlank()
        ) {

            setStatusText(
                "USE: EMAIL NAME"
            )

            return
        }

        if (
            isEmailAddress(
                trimmedTarget
            )
        ) {

            onComposeMail(
                EmailContact(
                    name = trimmedTarget,
                    emailAddress = trimmedTarget
                )
            )

            return
        }

        if (
            !hasContactsPermission
        ) {

            onRequestContactsPermission()
            return
        }

        val matches =
            contactRepository
                .findEmailContacts(
                    searchName = trimmedTarget,
                    limit = 8
                )

        when {

            matches.isEmpty() ->
                setStatusText(
                    "EMAIL CONTACT NOT FOUND"
                )

            matches.size == 1 ->
                onComposeMail(
                    matches.first()
                )

            else ->
                emailContactMatches =
                    matches
        }
    }

    fun openLatestUnreadMessage() {

        val latest =
            latestUnread

        if (
            latest == null
        ) {

            /*
             * Nothing stored means the waiting text arrived as a
             * notification, which belongs to the messaging app — so
             * the card opens that rather than an empty inbox.
             */
            if (
                !handOffMessagingIfNotDefault()
            ) {
                onOpenMessagesInbox()
            }

            return
        }

        if (
            !hasContactsPermission
        ) {

            onRequestContactsPermission()

            return
        }

        val contactName =
            contactRepository
                .findContactNameByPhoneNumber(
                    latest.phoneNumber
                )

        onOpenConversation(
            Contact(
                name =
                    contactName
                        ?: latest.phoneNumber,
                phoneNumber =
                    latest.phoneNumber
            )
        )
    }

    fun openCalendar() {

        try {

            context.startActivity(
                Intent(
                    Intent.ACTION_MAIN
                ).apply {

                    addCategory(
                        Intent.CATEGORY_APP_CALENDAR
                    )
                }
            )

        } catch (
            _: Exception
        ) {

            setStatusText(
                "CALENDAR NOT AVAILABLE"
            )
        }
    }

    fun openMapsSearch(
        query: String
    ) {

        val trimmedQuery =
            query.trim()

        val mapsUri =
            if (
                trimmedQuery.isBlank()
            ) {
                Uri.parse(
                    "geo:0,0?q="
                )
            } else {
                Uri.parse(
                    "geo:0,0?q=${
                        Uri.encode(
                            trimmedQuery
                        )
                    }"
                )
            }

        /*
         * Prefer Google Maps when it is installed so the command
         * feels direct and predictable. If Google Maps is not on
         * the device, fall back to any installed mapping app that
         * understands a standard geo: search URI.
         *
         * Queries such as "coffee near me" are handed directly to
         * Maps, which can use the device's location to show nearby
         * results. Prompt Launcher never needs its own map UI.
         */
        try {

            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    mapsUri
                ).apply {
                    setPackage(
                        "com.google.android.apps.maps"
                    )
                }
            )

        } catch (
            _: Exception
        ) {

            try {

                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        mapsUri
                    )
                )

            } catch (
                _: Exception
            ) {

                setStatusText(
                    "MAPS NOT AVAILABLE"
                )
            }
        }
    }

    val searchPrefs =
        remember {
            context.getSharedPreferences(
                "prompt_launcher_search",
                Context.MODE_PRIVATE
            )
        }

    var searchWithClaude by remember {
        mutableStateOf(
            searchPrefs.getBoolean(
                "use_claude",
                false
            )
        )
    }

    fun setSearchWithClaude(
        useClaude: Boolean
    ) {

        searchWithClaude =
            useClaude

        searchPrefs
            .edit()
            .putBoolean(
                "use_claude",
                useClaude
            )
            .apply()
    }

    /*
     * Claude has no documented query intent, so the text is shared to
     * it the way any app receives text.
     */
    fun openClaudeQuery(
        query: String
    ) {

        val cleaned =
            query.trim()

        if (
            cleaned.isBlank()
        ) {
            return
        }

        saveRecentWebSearch(
            cleaned
        )

        val shared =
            runCatching {

                context.startActivity(
                    Intent(
                        Intent.ACTION_SEND
                    ).apply {

                        type =
                            "text/plain"

                        setPackage(
                            "com.anthropic.claude"
                        )

                        putExtra(
                            Intent.EXTRA_TEXT,
                            cleaned
                        )
                    }
                )

                true
            }
                .getOrDefault(
                    false
                )

        if (
            shared
        ) {
            return
        }

        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://claude.ai/new?q=" +
                                Uri.encode(
                                    cleaned
                                )
                    )
                )
            )
        }
            .onFailure {
                setStatusText(
                    "COULD NOT OPEN CLAUDE"
                )
            }
    }

    fun openChromeWebSearch(
        query: String
    ) {

        val cleaned =
            query.trim()

        if (
            cleaned.isBlank()
        ) {
            return
        }

        saveRecentWebSearch(
            cleaned
        )

        val searchUri =
            Uri.parse(
                "https://www.google.com/search?q=${
                    Uri.encode(
                        cleaned
                    )
                }"
            )

        try {

            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    searchUri
                ).apply {

                    setPackage(
                        "com.android.chrome"
                    )
                }
            )

        } catch (
            _: Exception
        ) {

            try {

                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        searchUri
                    )
                )

            } catch (
                _: Exception
            ) {

                setStatusText(
                    "BROWSER NOT AVAILABLE"
                )
            }
        }
    }


    fun directNavigationTarget(
        rawQuestion: String
    ): String? {

        val trimmed =
            rawQuestion
                .trim()

        val normalized =
            trimmed
                .lowercase()
                .replace(
                    "’",
                    "'"
                )

        val prefixes =
            listOf(
                "navigate me to ",
                "navigate to ",
                "take me to ",
                "directions to ",
                "give me directions to ",
                "drive me to ",
                "route me to ",
                "get me to "
            )

        val prefix =
            prefixes
                .firstOrNull {
                    normalized.startsWith(
                        it
                    )
                }
                ?: return null

        return trimmed
            .substring(
                prefix.length
            )
            .trim()
            .takeIf {
                it.isNotBlank()
            }
    }


    fun isContextualNavigationRequest(
        rawQuestion: String
    ): Boolean {

        val normalized =
            rawQuestion
                .trim()
                .lowercase()
                .replace(
                    "’",
                    "'"
                )
                .removeSuffix(
                    "."
                )
                .removeSuffix(
                    "?"
                )
                .trim()

        return normalized in
                setOf(
                    "take me there",
                    "navigate me there",
                    "navigate there",
                    "go there",
                    "get me there",
                    "drive me there",
                    "directions there",
                    "give me directions there"
                )
    }


    fun isWhereFollowUp(
        rawQuestion: String
    ): Boolean {

        val normalized =
            rawQuestion
                .trim()
                .lowercase()
                .removeSuffix(
                    "?"
                )
                .trim()

        return normalized in
                setOf(
                    "where",
                    "where is it",
                    "where is that",
                    "where is this",
                    "what's the location",
                    "whats the location",
                    "what is the location",
                    "address",
                    "what's the address",
                    "whats the address",
                    "what is the address",
                    "where is the event",
                    "where is that event"
                )
    }


    fun calendarNavigationTarget(): String? {

        val location =
            upcomingEvent
                ?.location
                ?.trim()
                .orEmpty()

        if (
            location.isBlank() ||
            location.startsWith(
                "http",
                ignoreCase =
                    true
            )
        ) {

            return null
        }

        return location
    }


    fun eventAnswer(
        heading: String
    ): String {

        val event =
            upcomingEvent
                ?: return "$heading\n\nnothing upcoming"

        assistantNavigationTarget =
            calendarNavigationTarget()

        return buildString {

            append(
                heading
            )

            append(
                "\n\n"
            )

            append(
                formatCalendarEventTime(
                    event
                )
            )

            append(
                "  "
            )

            append(
                event.title
            )
        }
    }


    fun eventLocalDate(
        event: UpcomingCalendarEvent
    ): LocalDate {

        return Instant
            .ofEpochMilli(
                event.startTime
            )
            .atZone(
                ZoneId.systemDefault()
            )
            .toLocalDate()
    }


    fun eventsForDate(
        date: LocalDate
    ): List<UpcomingCalendarEvent> {

        return calendarAgendaEvents
            .filter {
                eventLocalDate(
                    it
                ) ==
                        date
            }
            .sortedBy {
                it.startTime
            }
    }


    fun eventTimeLabel(
        event: UpcomingCalendarEvent
    ): String {

        if (
            event.allDay
        ) {

            return "all day"
        }

        return SimpleDateFormat(
            "h:mm a",
            Locale.getDefault()
        )
            .format(
                Date(
                    event.startTime
                )
            )
            .lowercase()
    }


    fun agendaAnswer(
        heading: String,
        events: List<UpcomingCalendarEvent>
    ): String {

        if (
            events.isEmpty()
        ) {

            assistantNavigationTarget =
                null

            return "$heading\n\nnothing scheduled"
        }

        assistantNavigationTarget =
            events
                .firstNotNullOfOrNull {
                        event ->

                    event.location
                        .trim()
                        .takeIf {
                                location ->

                            location.isNotBlank() &&
                                    !location.startsWith(
                                        "http",
                                        ignoreCase =
                                            true
                                    )
                        }
                }

        return buildString {

            append(
                heading
            )

            append(
                "\n\n"
            )

            events
                .forEachIndexed {
                        index,
                        event ->

                    append(
                        eventTimeLabel(
                            event
                        )
                    )

                    append(
                        "  "
                    )

                    append(
                        event.title
                    )

                    if (
                        index <
                        events.lastIndex
                    ) {

                        append(
                            "\n\n"
                        )
                    }
                }
        }
    }


    fun calendarAgendaContext(): String {

        if (
            calendarAgendaEvents.isEmpty()
        ) {

            return "No calendar agenda data is currently loaded."
        }

        return calendarAgendaEvents
            .sortedBy {
                it.startTime
            }
            .joinToString(
                separator =
                    "\n"
            ) {
                    event ->

                val date =
                    eventLocalDate(
                        event
                    )

                buildString {

                    append(
                        date.toString()
                    )

                    append(
                        " | "
                    )

                    append(
                        eventTimeLabel(
                            event
                        )
                    )

                    append(
                        " | "
                    )

                    append(
                        event.title
                    )

                    event.location
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                                location ->

                            append(
                                " | location: "
                            )

                            append(
                                location
                            )
                        }
                }
            }
    }


    fun openNaturalNavigation(
        question: String,
        destination: String
    ) {

        val answer =
            "opening maps → $destination"

        assistantQuestion =
            question

        assistantAnswer =
            answer

        assistantTurns =
            listOf(
                AssistantTurn(
                    question =
                        question,
                    answer =
                        answer
                )
            )

        assistantNavigationTarget =
            destination

        assistantThinking =
            false

        setStatusText(
            ""
        )

        openMapsSearch(
            destination
        )
    }


    fun executeCommand(
        command: Command
    ) {

        try {

            when (
                command
            ) {

                Command.OpenKeyProbe ->

                    context.startActivity(
                        Intent(
                            context,
                            KeyProbeActivity::class.java
                        )
                    )

                Command.OpenMessages ->

                    onOpenMessagesInbox()

                Command.OpenPhone ->

                    context.startActivity(
                        Intent(
                            Intent.ACTION_DIAL
                        )
                    )

                Command.OpenMaps ->

                    openMapsSearch(
                        ""
                    )

                Command.OpenChrome ->

                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(
                                "https://www.google.com"
                            )
                        )
                    )

                Command.OpenSettings ->

                    context.startActivity(
                        Intent(
                            Settings.ACTION_SETTINGS
                        )
                    )

                is Command.SetupTodoist -> {

                    todoistTokenStore
                        .saveToken(
                            command.token
                        )

                    setStatusText(
                        "✓ TODOIST CONNECTED"
                    )
                }

                Command.TodoistStatus ->

                    setStatusText(
                        if (
                            todoistTokenStore
                                .hasToken()
                        ) {
                            "✓ TODOIST CONNECTED"
                        } else {
                            "TODOIST NOT CONNECTED"
                        }
                    )

                Command.DisconnectTodoist -> {

                    todoistTokenStore
                        .clearToken()

                    setStatusText(
                        "TODOIST DISCONNECTED"
                    )
                }

                is Command.AddTodoistTask -> {

                    /*
                     * One task command, one selected provider.
                     *
                     * TodoistRepository reads the active provider
                     * from TodoistTokenStore and routes the task to
                     * Google Tasks, Todoist, or returns the setup
                     * message when no provider is selected.
                     */
                    setStatusText(
                        "ADDING"
                    )

                    scope.launch {

                        setStatusText(
                            todoistRepository
                                .createTask(
                                    command.title
                                )
                                .message
                        )
                    }
                }

                Command.RequestSmsRole -> {

                    if (
                        smsRoleManager
                            .isDefaultSmsApp()
                    ) {

                        setStatusText(
                            "✓ SMS READY"
                        )

                    } else if (
                        smsRoleManager
                            .isSmsRoleAvailable()
                    ) {

                        smsRoleLauncher(
                            smsRoleManager
                                .createSmsRoleRequestIntent()
                        )

                    } else {

                        setStatusText(
                            "SMS ROLE NOT AVAILABLE"
                        )
                    }
                }

                is Command.SendMessage -> {

                    resolveContactCommand(
                        command,
                        command.contactName
                    )
                }

                is Command.OpenConversation -> {

                    resolveContactCommand(
                        command,
                        command.contactName
                    )
                }

                is Command.CallContact -> {

                    resolveContactCommand(
                        command,
                        command.contactName
                    )
                }

                is Command.Navigate -> {

                    openMapsSearch(
                        command.destination
                    )
                }

                is Command.Unknown ->

                    setStatusText(
                        "UNKNOWN COMMAND"
                    )

                Command.Empty ->

                    setStatusText(
                        ""
                    )
            }

        } catch (
            _: Exception
        ) {

            setStatusText(
                "COMMAND FAILED"
            )
        }
    }

    /*
     * "@@" starts a group. Recipients accumulate as they are picked,
     * and the thread opens once at least two are chosen — from there
     * it is an ordinary conversation screen with an MMS reply field.
     */
    var groupRecipients by remember {
        mutableStateOf<List<Contact>>(
            emptyList()
        )
    }

    val isGroupCompose =
        commandText
            .trimStart()
            .startsWith("@@")

    val groupComposeQuery =
        if (
            isGroupCompose
        ) {
            commandText
                .trimStart()
                .removePrefix("@@")
                .trim()
        } else {
            ""
        }

    LaunchedEffect(
        isGroupCompose
    ) {

        if (
            !isGroupCompose
        ) {
            groupRecipients =
                emptyList()
        }
    }

    val groupContactMatches =
        remember(
            groupComposeQuery,
            hasContactsPermission,
            groupRecipients
        ) {

            if (
                !hasContactsPermission ||
                groupComposeQuery.isBlank()
            ) {

                emptyList()

            } else {

                runCatching {
                    contactRepository
                        .findContacts(
                            groupComposeQuery
                        )
                }
                    .getOrDefault(
                        emptyList()
                    )
                    .filter { candidate ->

                        groupRecipients.none {
                            normalizePhoneNumber(
                                it.phoneNumber
                            ) ==
                                    normalizePhoneNumber(
                                        candidate.phoneNumber
                                    )
                        }
                    }
                    .take(
                        4
                    )
            }
        }

    var groupSelectionIndex by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(
        groupComposeQuery,
        groupContactMatches.size
    ) {
        groupSelectionIndex =
            0
    }

    fun addGroupRecipient(
        contact: Contact
    ) {

        groupRecipients =
            groupRecipients + contact

        commandText =
            "@@"
    }

    fun openGroupThread(
        draft: String = ""
    ) {

        if (
            groupRecipients.size < 2
        ) {

            setStatusText(
                "PICK AT LEAST TWO PEOPLE"
            )

            return
        }

        val ownNumber =
            OwnNumberStore(
                context
            )
                .get()

        /*
         * The thread key has to include this device, because an
         * incoming reply lists every participant. Leaving it out
         * would file the replies under a different thread.
         */
        val participants =
            (
                    groupRecipients.map {
                        it.phoneNumber
                    } +
                            listOfNotNull(
                                ownNumber
                            )
                    )

        onOpenGroupThread(
            participants,
            groupRecipients.map {
                it.name
            },
            draft
        )

        groupRecipients =
            emptyList()

        commandText =
            ""
    }

    fun submitCommand() {

        val raw =
            commandText.trim()

        /*
         * In group compose, Enter means one of two things. While a
         * typed name still matches someone, the suggestion list
         * handles it. Once nothing matches, the text is the message
         * and the thread opens carrying it.
         */
        if (
            isGroupCompose
        ) {

            openGroupThread(
                groupComposeQuery
            )

            return
        }

        if (
            raw.equals(
                "bug",
                ignoreCase =
                    true
            )
        ) {

            commandText =
                ""

            try {

                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://forms.gle/ArBaaEKFvLtn3bYo8"
                        )
                    )
                )

                setStatusText(
                    ""
                )

            } catch (
                _: Exception
            ) {

                setStatusText(
                    "BUG FORM COULD NOT OPEN"
                )
            }

            return
        }

        if (
            raw.equals(
                "go",
                ignoreCase = true
            )
        ) {

            commandText =
                ""

            openMapsSearch(
                ""
            )

            return
        }

        if (
            raw.startsWith(
                "go ",
                ignoreCase = true
            )
        ) {

            val mapsQuery =
                raw
                    .substring(
                        3
                    )
                    .trim()

            commandText =
                ""

            openMapsSearch(
                mapsQuery
            )

            return
        }

        if (
            raw.startsWith(
                "map ",
                ignoreCase = true
            )
        ) {

            val mapsQuery =
                raw
                    .substring(
                        4
                    )
                    .trim()

            commandText =
                ""

            openMapsSearch(
                mapsQuery
            )

            return
        }

        if (
            raw.startsWith(
                "maps ",
                ignoreCase = true
            )
        ) {

            val mapsQuery =
                raw
                    .substring(
                        5
                    )
                    .trim()

            commandText =
                ""

            openMapsSearch(
                mapsQuery
            )

            return
        }

        if (
            raw == "."
        ) {
            commandText =
                ""

            showCommandHelp =
                false

            showAppSearch =
                true

            return
        }

        if (
            raw == "\""
        ) {
            commandText =
                ""

            onOpenNotes()

            return
        }

        if (
            raw.startsWith("\"")
        ) {
            val noteRaw =
                raw
                    .removePrefix("\"")
                    .trimStart()

            val categoryCode =
                noteRaw
                    .firstOrNull()
                    ?.lowercaseChar()

            /*
             * "p, "w, "i and "j remain explicit category
             * shortcuts when the category letter is followed
             * by whitespace (or is the only character).
             *
             * Any other text after a bare quote defaults to
             * PERSONAL, so:
             *
             * " buy milk
             *
             * saves "buy milk" as a personal note.
             */
            val explicitCategory =
                categoryCode in
                        setOf(
                            'p',
                            'w',
                            'i',
                            'j'
                        ) &&
                        (
                                noteRaw.length ==
                                        1 ||
                                        noteRaw
                                            .getOrNull(
                                                1
                                            )
                                            ?.isWhitespace() ==
                                        true
                                )

            val category =
                if (
                    explicitCategory
                ) {
                    when (
                        categoryCode
                    ) {
                        'w' ->
                            NoteCategory.WORK

                        'i' ->
                            NoteCategory.IDEAS

                        'j' ->
                            NoteCategory.JOURNAL

                        else ->
                            NoteCategory.PERSONAL
                    }
                } else {
                    NoteCategory.PERSONAL
                }

            val body =
                if (
                    explicitCategory
                ) {
                    noteRaw
                        .drop(
                            1
                        )
                        .trim()
                } else {
                    noteRaw.trim()
                }

            if (
                body.isBlank()
            ) {
                setStatusText(
                    "ADD NOTE TEXT"
                )
            } else {
                onCreateNote(
                    body,
                    category
                )
            }

            commandText =
                ""

            return
        }

        if (
            raw.startsWith("!!")
        ) {
            val alarmRaw =
                "alarm " +
                        raw.removePrefix("!!")
                            .trim()

            commandText =
                ""

            val alarmTime =
                parseAlarmTime(
                    alarmRaw
                )

            if (alarmTime == null) {
                setStatusText(
                    "USE: !! 6:30AM"
                )

                return
            }

            try {
                val intent =
                    Intent(
                        AlarmClock.ACTION_SET_ALARM
                    ).apply {
                        putExtra(
                            AlarmClock.EXTRA_HOUR,
                            alarmTime.hour
                        )
                        putExtra(
                            AlarmClock.EXTRA_MINUTES,
                            alarmTime.minute
                        )
                        putExtra(
                            AlarmClock.EXTRA_MESSAGE,
                            "Prompt Launcher"
                        )
                        putExtra(
                            AlarmClock.EXTRA_SKIP_UI,
                            true
                        )
                    }

                context.startActivity(
                    intent
                )

                setStatusText(
                    "✓ ALARM SET · ${alarmTime.display}"
                )
            } catch (
                _: Exception
            ) {
                setStatusText(
                    "ALARM FAILED"
                )
            }

            return
        }

        if (
            raw.startsWith("!")
        ) {
            val timerRaw =
                "timer " +
                        raw.removePrefix("!")
                            .trim()

            commandText =
                ""

            val seconds =
                parseTimerSeconds(
                    timerRaw
                )

            if (seconds == null) {
                setStatusText(
                    "USE: ! 10 MINUTES"
                )

                return
            }

            try {
                val intent =
                    Intent(
                        AlarmClock.ACTION_SET_TIMER
                    ).apply {
                        putExtra(
                            AlarmClock.EXTRA_LENGTH,
                            seconds
                        )
                        putExtra(
                            AlarmClock.EXTRA_MESSAGE,
                            "Prompt Launcher"
                        )
                        putExtra(
                            AlarmClock.EXTRA_SKIP_UI,
                            true
                        )
                    }

                context.startActivity(
                    intent
                )

                setStatusText(
                    "✓ TIMER SET · ${formatTimerDuration(seconds)}"
                )
            } catch (
                _: Exception
            ) {
                setStatusText(
                    "TIMER FAILED"
                )
            }

            return
        }

        if (
            raw.startsWith(":")
        ) {
            val calendarRaw =
                "event " +
                        raw.removePrefix(":")
                            .trim()

            val draft =
                CalendarCommandParser
                    .parse(
                        calendarRaw
                    )

            if (draft != null) {
                onCreateCalendarEvent(
                    draft
                )
            } else {
                setStatusText(
                    "USE: : TITLE TOMORROW AT 2PM"
                )
            }

            commandText =
                ""

            return
        }

        if (
            raw.startsWith("/")
        ) {
            val target =
                raw.removePrefix("/")
                    .trim()

            if (target.isBlank()) {
                setStatusText(
                    "USE: / NAME"
                )
            } else {
                executeCommand(
                    CommandParser.parse(
                        "#$target"
                    )
                )
            }

            commandText =
                ""

            return
        }

        if (
            raw.startsWith("+")
        ) {
            val todoistRaw =
                "-" +
                        raw.removePrefix("+")

            executeCommand(
                CommandParser.parse(
                    todoistRaw
                )
            )

            commandText =
                ""

            return
        }

        if (
            raw.equals(
                "weathersetup",
                ignoreCase =
                    true
            )
        ) {
            commandText =
                ""

            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                setStatusText(
                    "✓ WEATHER READY"
                )
                refreshWeather()
            } else {
                weatherPermissionLauncher.launch(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }

            return
        }

        if (
            raw.equals(
                "weatherstatus",
                ignoreCase =
                    true
            )
        ) {
            commandText =
                ""

            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                setStatusText(
                    "WEATHER LOCATION NOT ENABLED"
                )
            } else {
                refreshWeather()
                setStatusText(
                    weatherSnapshot
                        ?.let { snapshot ->
                            "✓ ${snapshot.condition.uppercase()} · ${snapshot.temperatureF}°"
                        }
                        ?: "✓ WEATHER READY"
                )
            }

            return
        }

        if (
            raw.equals(
                "usagesetup",
                ignoreCase =
                    true
            )
        ) {
            commandText =
                ""

            try {
                context.startActivity(
                    Intent(
                        Settings.ACTION_USAGE_ACCESS_SETTINGS
                    )
                )

                setStatusText(
                    "ENABLE PROMPT LAUNCHER USAGE ACCESS"
                )
            } catch (
                _: Exception
            ) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_SETTINGS
                    )
                )

                setStatusText(
                    "OPEN USAGE ACCESS IN SETTINGS"
                )
            }

            return
        }

        if (
            raw.equals(
                "usagestatus",
                ignoreCase =
                    true
            )
        ) {
            commandText =
                ""

            setStatusText(
                if (
                    usageStatsRepository
                        .hasUsageAccess()
                ) {
                    "✓ USAGE ACCESS READY"
                } else {
                    "USAGE ACCESS NOT ENABLED"
                }
            )

            return
        }

        if (
            raw.equals(
                "usagecheck",
                ignoreCase =
                    true
            )
        ) {
            commandText =
                ""

            if (
                !usageStatsRepository
                    .hasUsageAccess()
            ) {
                setStatusText(
                    "USAGE ACCESS NOT ENABLED"
                )

                return
            }

            setStatusText(
                "CHECKING CURRENT HOUR..."
            )

            scope.launch {
                val diagnostics: CurrentHourUsageDiagnostics? =
                    withContext(
                        Dispatchers.Default
                    ) {
                        usageStatsRepository
                            .loadCurrentHourDiagnostics()
                    }

                if (
                    diagnostics == null
                ) {
                    setStatusText(
                        "USAGE DATA NOT AVAILABLE"
                    )
                } else {
                    val totalMinutes =
                        diagnostics.totalActiveMs /
                                60_000.0

                    val distractingMinutes =
                        diagnostics.distractingMs /
                                60_000.0

                    val result =
                        when (
                            diagnostics.status
                        ) {
                            HourProductivityStatus.GRAY ->
                                "GRAY"

                            HourProductivityStatus.PRODUCTIVE ->
                                "PRODUCTIVE"

                            HourProductivityStatus.UNPRODUCTIVE ->
                                "RED"
                        }

                    setStatusText(
                        String.format(
                            Locale.getDefault(),
                            "NOW %.1fm total · %.1fm distract · %d%% · %s",
                            totalMinutes,
                            distractingMinutes,
                            diagnostics.distractingSharePercent,
                            result
                        )
                    )
                }
            }

            return
        }

        if (
            raw.equals(
                "geminisetup",
                ignoreCase =
                    true
            )
        ) {
            commandText =
                ""
            showGeminiSetup =
                true
            setStatusText(
                ""
            )
            return
        }

        if (
            raw.equals(
                "geministatus",
                ignoreCase =
                    true
            )
        ) {
            commandText =
                ""
            setStatusText(
                if (
                    geminiApiKeyStore.hasKey()
                ) {
                    "✓ GEMINI READY"
                } else {
                    "GEMINI NOT SET UP"
                }
            )
            return
        }

        if (
            raw.equals(
                "geminidisconnect",
                ignoreCase =
                    true
            )
        ) {
            commandText =
                ""
            geminiApiKeyStore.clearKey()
            setStatusText(
                "GEMINI DISCONNECTED"
            )
            return
        }

        if (
            raw.startsWith("?")
        ) {

            val question =
                raw
                    .removePrefix("?")
                    .trim()

            commandText =
                ""

            val normalized =
                question
                    .lowercase()
                    .replace("’", "'")
                    .trim()

            val importantMailCount =
                mailMessages.count {
                    it.important &&
                            it.unread
                }

            val directNavigation =
                directNavigationTarget(
                    question
                )

            if (
                directNavigation != null
            ) {

                openNaturalNavigation(
                    question =
                        question,
                    destination =
                        directNavigation
                )

                return
            }

            if (
                isContextualNavigationRequest(
                    question
                )
            ) {

                val destination =
                    assistantNavigationTarget
                        ?: calendarNavigationTarget()

                if (
                    destination != null
                ) {

                    openNaturalNavigation(
                        question =
                            question,
                        destination =
                            destination
                    )

                    return
                }
            }

            val latestIncoming =
                recentMessages
                    .firstOrNull {
                        it.incoming
                    }

            fun nextEventLine(): String {

                val event =
                    upcomingEvent
                        ?: return "nothing upcoming"

                return buildString {

                    append(
                        "${formatCalendarEventTime(event)}  ${event.title}"
                    )

                    event.location
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
                        ?.let {
                                location ->

                            append(
                                " · $location"
                            )
                        }
                }
            }

            val needsGeminiEmailReasoning =
                (
                        normalized.contains("email") ||
                                normalized.contains("mail")
                        ) &&
                        (
                                normalized.contains("reply") ||
                                        normalized.contains("respond") ||
                                        normalized.contains("most important") ||
                                        normalized.contains("priority") ||
                                        normalized.contains("prioritize") ||
                                        normalized.contains("needs my attention") ||
                                        normalized.contains("need my attention")
                                )

            if (
                needsGeminiEmailReasoning
            ) {
                assistantQuestion =
                    question

                if (
                    !geminiApiKeyStore.hasKey()
                ) {
                    val notSetup =
                        "GEMINI NOT SET UP\n\nrun geminisetup"

                    assistantAnswer =
                        notSetup

                    assistantTurns =
                        listOf(
                            AssistantTurn(
                                question = question,
                                answer = notSetup
                            )
                        )

                    assistantThinking =
                        false

                    setStatusText(
                        ""
                    )
                    return
                }

                if (
                    !assistantSharingStore
                        .load()
                        .mail
                ) {
                    val mailOff =
                        "MAIL SHARING IS OFF\n\nturn on mail in geminisetup to let the assistant read your inbox"

                    assistantAnswer =
                        mailOff

                    assistantTurns =
                        listOf(
                            AssistantTurn(
                                question = question,
                                answer = mailOff
                            )
                        )

                    assistantThinking =
                        false

                    setStatusText(
                        ""
                    )

                    return
                }

                assistantAnswer =
                    "THINKING..."

                assistantTurns =
                    listOf(
                        AssistantTurn(
                            question = question,
                            answer = "THINKING..."
                        )
                    )

                assistantThinking =
                    true

                setStatusText(
                    ""
                )

                scope.launch {
                    val response =
                        geminiRepository.prioritizeEmails(
                            apiKey =
                                geminiApiKeyStore.getKey(),
                            question =
                                question,
                            messages =
                                mailMessages
                        )

                    assistantAnswer =
                        response

                    assistantTurns =
                        listOf(
                            AssistantTurn(
                                question = question,
                                answer = response
                            )
                        )

                    assistantThinking =
                        false
                }

                return
            }

            val answer =
                when {

                    normalized.isBlank() ->
                        "TRY ASKING\n\nwhat do i have today?\nwhat's next?\nwhat did i miss?\nwho texted me last?\nhow many unread messages?\nhow many important emails?"

                    normalized.contains(
                        "what do i have on my calendar"
                    ) ||
                            normalized.contains(
                                "what's on my calendar"
                            ) ||
                            normalized.contains(
                                "whats on my calendar"
                            ) ||
                            normalized.contains(
                                "what is on my calendar"
                            ) ||
                            normalized == "calendar" ->
                        eventAnswer(
                            "NEXT"
                        )

                    normalized == "tomorrow" ||
                            normalized.contains(
                                "what do i have tomorrow"
                            ) ||
                            normalized.contains(
                                "what's on my calendar tomorrow"
                            ) ||
                            normalized.contains(
                                "whats on my calendar tomorrow"
                            ) ||
                            normalized.contains(
                                "what is on my calendar tomorrow"
                            ) ||
                            normalized.contains(
                                "schedule tomorrow"
                            ) ||
                            normalized.contains(
                                "calendar tomorrow"
                            ) -> {

                        val tomorrow =
                            LocalDate
                                .now(
                                    ZoneId.systemDefault()
                                )
                                .plusDays(
                                    1
                                )

                        agendaAnswer(
                            heading =
                                "TOMORROW",
                            events =
                                eventsForDate(
                                    tomorrow
                                )
                        )
                    }

                    normalized.contains("what do i have today") ||
                            normalized == "today" ||
                            normalized.contains("schedule today") -> {

                        val event =
                            upcomingEvent

                        if (
                            event == null
                        ) {
                            "TODAY\n\nnothing upcoming"
                        } else {
                            val eventDay =
                                SimpleDateFormat(
                                    "yyyyMMdd",
                                    Locale.getDefault()
                                ).format(
                                    Date(event.startTime)
                                )

                            val today =
                                SimpleDateFormat(
                                    "yyyyMMdd",
                                    Locale.getDefault()
                                ).format(
                                    Date()
                                )

                            if (
                                eventDay == today
                            ) {

                                assistantNavigationTarget =
                                    calendarNavigationTarget()

                                buildString {

                                    append(
                                        "TODAY\n\n"
                                    )

                                    append(
                                        formatCalendarEventTime(event)
                                            .removePrefix(
                                                "today · "
                                            )
                                    )

                                    append(
                                        "  ${event.title}"
                                    )
                                }

                            } else {

                                assistantNavigationTarget =
                                    calendarNavigationTarget()

                                "TODAY\n\nnothing upcoming today\n\nNEXT\n${nextEventLine()}"
                            }
                        }
                    }

                    normalized.contains("what's next") ||
                            normalized.contains("whats next") ||
                            normalized.contains("next event") ||
                            normalized.contains("next meeting") ->
                        eventAnswer(
                            "NEXT"
                        )

                    isWhereFollowUp(
                        question
                    ) -> {

                        val location =
                            assistantNavigationTarget
                                ?.trim()
                                .orEmpty()
                                .ifBlank {

                                    upcomingEvent
                                        ?.location
                                        ?.trim()
                                        .orEmpty()
                                }

                        if (
                            location.isNotBlank()
                        ) {

                            assistantNavigationTarget =
                                location

                            "LOCATION\n\n$location"

                        } else {

                            "LOCATION\n\nno location is saved for that event"
                        }
                    }

                    normalized.contains("unread message") ||
                            normalized.contains("unread text") ->
                        if (
                            unreadCount == 0
                        ) {
                            "MESSAGES\n\nall caught up"
                        } else {
                            "MESSAGES\n\n$unreadCount unread"
                        }

                    normalized.contains("important email") ||
                            normalized.contains("important mail") ->
                        if (
                            importantMailCount == 0
                        ) {
                            "MAIL\n\nno important unread mail"
                        } else {
                            "MAIL\n\n$importantMailCount important unread"
                        }

                    normalized.contains("who texted me last") ||
                            normalized.contains("last text") ||
                            normalized.contains("last message") -> {

                        if (
                            latestIncoming == null
                        ) {
                            "LAST MESSAGE\n\nno messages yet"
                        } else {
                            val sender =
                                if (
                                    hasContactsPermission
                                ) {
                                    contactRepository
                                        .findContactNameByPhoneNumber(
                                            latestIncoming.phoneNumber
                                        )
                                        ?: latestIncoming.phoneNumber
                                } else {
                                    latestIncoming.phoneNumber
                                }

                            "LAST MESSAGE\n\n${sender.lowercase()}\n${latestIncoming.body.take(120)}"
                        }
                    }

                    normalized.contains("what did i miss") ||
                            normalized.contains("anything important") ||
                            normalized.contains("what needs my attention") -> {

                        val lines =
                            mutableListOf<String>()

                        if (
                            unreadCount > 0
                        ) {
                            lines.add(
                                "• $unreadCount unread ${if (unreadCount == 1) "text" else "texts"}"
                            )
                        }

                        if (
                            importantMailCount > 0
                        ) {
                            lines.add(
                                "• $importantMailCount important ${if (importantMailCount == 1) "email" else "emails"}"
                            )
                        }

                        upcomingEvent
                            ?.let {
                                lines.add(
                                    "• ${formatCalendarEventTime(it)} · ${it.title}"
                                )
                            }

                        if (
                            lines.isEmpty()
                        ) {
                            "ATTENTION\n\nnothing pressing right now"
                        } else {
                            "ATTENTION\n\n${lines.joinToString("\n")}"
                        }
                    }

                    else ->
                        null
                }

            assistantQuestion =
                question

            if (
                answer != null
            ) {
                assistantAnswer =
                    answer

                assistantTurns =
                    listOf(
                        AssistantTurn(
                            question = question,
                            answer = answer
                        )
                    )

                assistantThinking =
                    false

                setStatusText(
                    ""
                )

                return
            }

            if (
                !geminiApiKeyStore.hasKey()
            ) {
                val notSetup =
                    "GEMINI NOT SET UP\n\nrun geminisetup"

                assistantAnswer =
                    notSetup

                assistantTurns =
                    listOf(
                        AssistantTurn(
                            question = question,
                            answer = notSetup
                        )
                    )

                assistantThinking =
                    false

                setStatusText(
                    ""
                )

                return
            }

            val launcherContext =
                buildAssistantLauncherContext(
                    sharing =
                        assistantSharingStore.load(),
                    unreadTextCount =
                        unreadCount,
                    importantUnreadMailCount =
                        importantMailCount,
                    nextEventLine = {
                        upcomingEvent
                            ?.let {
                                "${formatCalendarEventTime(it)}  ${it.title}"
                            }
                            ?: "nothing upcoming"
                    },
                    nextEventLocation = {
                        upcomingEvent?.location
                    },
                    calendarAgenda = {
                        calendarAgendaContext()
                    },
                    latestIncomingText = {
                        latestIncoming
                            ?.let { message ->
                                val sender =
                                    if (
                                        hasContactsPermission
                                    ) {
                                        contactRepository
                                            .findContactNameByPhoneNumber(
                                                message.phoneNumber
                                            )
                                            ?: message.phoneNumber
                                    } else {
                                        message.phoneNumber
                                    }

                                sender to message.body
                            }
                    },
                    mailMessages =
                        mailMessages
                )

            assistantAnswer =
                "THINKING..."

            assistantTurns =
                listOf(
                    AssistantTurn(
                        question = question,
                        answer = "THINKING..."
                    )
                )

            assistantThinking =
                true

            setStatusText(
                ""
            )

            scope.launch {
                val response =
                    geminiRepository.askLauncherQuestion(
                        apiKey =
                            geminiApiKeyStore.getKey(),
                        question =
                            question,
                        launcherContext =
                            launcherContext
                    )

                assistantAnswer =
                    response

                assistantTurns =
                    listOf(
                        AssistantTurn(
                            question = question,
                            answer = response
                        )
                    )

                assistantThinking =
                    false
            }

            return
        }

        if (
            raw.equals(
                "timer",
                ignoreCase =
                    true
            ) ||
            raw.startsWith(
                "timer ",
                ignoreCase =
                    true
            )
        ) {

            commandText =
                ""

            val seconds =
                parseTimerSeconds(
                    raw
                )

            if (
                seconds == null
            ) {

                setStatusText(
                    "USE: TIMER 10 MINUTES"
                )

                return
            }

            try {

                val intent =
                    Intent(
                        AlarmClock.ACTION_SET_TIMER
                    ).apply {

                        putExtra(
                            AlarmClock.EXTRA_LENGTH,
                            seconds
                        )

                        putExtra(
                            AlarmClock.EXTRA_MESSAGE,
                            "Prompt Launcher"
                        )

                        putExtra(
                            AlarmClock.EXTRA_SKIP_UI,
                            true
                        )
                    }

                context.startActivity(
                    intent
                )

                setStatusText(
                    "✓ TIMER SET · ${formatTimerDuration(seconds)}"
                )

            } catch (
                _: Exception
            ) {

                setStatusText(
                    "TIMER FAILED"
                )
            }

            return
        }

        if (
            raw.equals(
                "alarm",
                ignoreCase =
                    true
            ) ||
            raw.startsWith(
                "alarm ",
                ignoreCase =
                    true
            )
        ) {

            commandText =
                ""

            val alarmTime =
                parseAlarmTime(
                    raw
                )

            if (
                alarmTime == null
            ) {

                setStatusText(
                    "USE: ALARM 6:30AM"
                )

                return
            }

            try {

                val intent =
                    Intent(
                        AlarmClock.ACTION_SET_ALARM
                    ).apply {

                        putExtra(
                            AlarmClock.EXTRA_HOUR,
                            alarmTime.hour
                        )

                        putExtra(
                            AlarmClock.EXTRA_MINUTES,
                            alarmTime.minute
                        )

                        putExtra(
                            AlarmClock.EXTRA_MESSAGE,
                            "Prompt Launcher"
                        )

                        putExtra(
                            AlarmClock.EXTRA_SKIP_UI,
                            true
                        )
                    }

                context.startActivity(
                    intent
                )

                setStatusText(
                    "✓ ALARM SET · ${alarmTime.display}"
                )

            } catch (
                _: Exception
            ) {

                setStatusText(
                    "ALARM FAILED"
                )
            }

            return
        }

        if (
            raw.equals(
                "email",
                ignoreCase =
                    true
            )
        ) {

            commandText =
                ""

            setStatusText(
                "USE: EMAIL NAME"
            )

            return
        }

        if (
            raw.startsWith(
                "email ",
                ignoreCase =
                    true
            )
        ) {

            val target =
                raw
                    .substring(
                        6
                    )
                    .trim()

            commandText =
                ""

            startEmailCompose(
                target
            )

            return
        }

        if (
            raw.equals(
                "mail",
                ignoreCase =
                    true
            ) ||
            raw.equals(
                "e",
                ignoreCase =
                    true
            )
        ) {

            commandText =
                ""

            onOpenMail()

            return
        }

        if (
            raw.equals(
                "notes",
                ignoreCase =
                    true
            )
        ) {

            commandText =
                ""

            onOpenNotes()

            return
        }

        if (
            raw.equals(
                "note",
                ignoreCase =
                    true
            )
        ) {

            commandText =
                ""

            setStatusText(
                "USE: NOTE YOUR TEXT"
            )

            return
        }

        if (
            raw.startsWith(
                "note ",
                ignoreCase =
                    true
            )
        ) {

            val body =
                raw
                    .substring(
                        5
                    )
                    .trim()

            commandText =
                ""

            onCreateNote(
                body,
                NoteCategory.PERSONAL
            )

            return
        }

        if (
            raw.equals(
                "calendarsetup",
                ignoreCase =
                    true
            )
        ) {

            commandText =
                ""

            onCalendarSetup()

            return
        }

        if (
            raw.equals(
                "calendarstatus",
                ignoreCase =
                    true
            )
        ) {

            commandText =
                ""

            onCalendarStatus()

            return
        }

        if (
            CalendarCommandParser
                .isCalendarCommand(
                    raw
                )
        ) {

            val draft =
                CalendarCommandParser
                    .parse(
                        raw
                    )

            if (
                draft != null
            ) {

                onCreateCalendarEvent(
                    draft
                )

            } else {

                setStatusText(
                    "USE: EVENT TITLE TOMORROW AT 2PM"
                )
            }

            commandText =
                ""

            return
        }

        val parsedCommand =
            CommandParser.parse(
                raw
            )

        val normalizedRaw =
            raw.lowercase()

        val explicitLauncherWordCommand =
            LauncherLegacyCommands.any {
                    command ->

                normalizedRaw ==
                        command ||
                        normalizedRaw.startsWith(
                            "$command "
                        )
            }

        val explicitSymbolCommand =
            raw.firstOrNull() in
                    LauncherSymbolCommands

        if (
            parsedCommand is
                    Command.Unknown ||
            (
                    !explicitLauncherWordCommand &&
                            !explicitSymbolCommand
                    )
        ) {

            commandText =
                ""

            if (
                showSearchTargetPicker &&
                searchWithClaude
            ) {
                openClaudeQuery(
                    raw
                )
            } else {
                openChromeWebSearch(
                    raw
                )
            }

            return
        }

        executeCommand(
            parsedCommand
        )

        commandText =
            ""
    }

    val inlineContactCommand =
        remember(
            commandText
        ) {
            val trimmed =
                commandText.trim()

            when {
                trimmed.startsWith("@@") ->
                    null

                trimmed.startsWith("@") ->
                    runCatching {
                        CommandParser.parse(
                            trimmed
                        )
                    }
                        .getOrNull()

                trimmed.startsWith("/") ->
                    runCatching {
                        CommandParser.parse(
                            "#${trimmed.removePrefix("/").trim()}"
                        )
                    }
                        .getOrNull()

                else ->
                    null
            }
        }

    val inlineContactQuery =
        when (
            val command = inlineContactCommand
        ) {
            is Command.SendMessage ->
                command.contactName

            is Command.OpenConversation ->
                command.contactName

            is Command.CallContact ->
                command.contactName

            else -> {
                val trimmed =
                    commandText.trim()

                when {
                    trimmed.startsWith("@@") ->
                        ""

                    trimmed.startsWith("@") ->
                        trimmed
                            .removePrefix("@")
                            .trim()

                    trimmed.startsWith("/") ->
                        trimmed
                            .removePrefix("/")
                            .trim()

                    else ->
                        ""
                }
            }
        }
            .trim()

    /*
     * "." alone opens the app list. "." followed by text narrows it
     * inline, the same way "@" narrows contacts, so a known app is
     * two keystrokes and enter rather than a trip through the picker.
     */
    val inlineAppQuery =
        commandText
            .trim()
            .let { trimmed ->

                if (
                    trimmed.startsWith(".")
                ) {
                    trimmed
                        .removePrefix(".")
                        .trim()
                } else {
                    ""
                }
            }

    val inlineAppMatches =
        remember(
            inlineAppQuery
        ) {

            if (
                inlineAppQuery.isBlank()
            ) {

                emptyList()

            } else {

                val query =
                    inlineAppQuery.trim()

                loadLaunchableApps(
                    context
                )
                    .filter { app ->
                        app.label.contains(
                            query,
                            ignoreCase = true
                        )
                    }
                    /*
                     * Prefix matches first: typing "to" should offer
                     * Todoist before Photos.
                     */
                    .sortedWith(
                        compareByDescending<LauncherAppEntry> { app ->
                            app.label.startsWith(
                                query,
                                ignoreCase = true
                            )
                        }
                            .thenBy { app ->
                                app.label.lowercase()
                            }
                    )
                    .take(
                        4
                    )
            }
        }

    var inlineAppSelectionIndex by remember {
        mutableStateOf(0)
    }

    LaunchedEffect(
        inlineAppQuery,
        inlineAppMatches.size
    ) {
        inlineAppSelectionIndex =
            0
    }

    fun chooseInlineApp(
        app: LauncherAppEntry
    ) {

        val launchIntent =
            context
                .packageManager
                .getLaunchIntentForPackage(
                    app.packageName
                )

        if (
            launchIntent == null
        ) {

            setStatusText(
                "APP NOT AVAILABLE"
            )

        } else {

            try {

                context.startActivity(
                    launchIntent
                )

                commandText =
                    ""

            } catch (
                _: Exception
            ) {

                setStatusText(
                    "APP COULD NOT OPEN"
                )
            }
        }
    }

    val inlineContactMatches =
        remember(
            inlineContactQuery,
            hasContactsPermission
        ) {
            if (
                hasContactsPermission &&
                inlineContactQuery.isNotBlank()
            ) {
                contactRepository
                    .findContacts(
                        inlineContactQuery,
                        16
                    )
                    .filter { contact ->
                        contact.name
                            .trim()
                            .startsWith(
                                inlineContactQuery.trim(),
                                ignoreCase = true
                            )
                    }
                    .take(
                        4
                    )
            } else {
                emptyList()
            }
        }

    LaunchedEffect(
        inlineContactQuery,
        inlineContactMatches.size
    ) {
        inlineContactSelectionIndex =
            0
    }

    fun chooseInlineContact(
        contact: Contact
    ) {
        val command =
            inlineContactCommand

        when (command) {
            is Command.SendMessage,
            is Command.OpenConversation,
            is Command.CallContact ->
                performResolvedContactCommand(
                    command,
                    contact
                )

            else -> {
                val trimmed =
                    commandText.trim()

                if (
                    trimmed.startsWith("/")
                ) {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_DIAL,
                            Uri.parse(
                                "tel:${contact.phoneNumber}"
                            )
                        )
                    )
                } else {
                    onOpenConversation(
                        contact
                    )
                }
            }
        }

        commandText =
            ""

        inlineContactSelectionIndex =
            0
    }

    if (
        emailContactMatches.isNotEmpty()
    ) {

        EmailContactChooserScreen(
            contacts =
                emailContactMatches,
            onChoose = {
                    contact ->

                emailContactMatches =
                    emptyList()

                onComposeMail(
                    contact
                )
            },
            onBack = {

                emailContactMatches =
                    emptyList()

                setStatusText(
                    ""
                )
            }
        )

        return
    }

    /*
     * Any overlay that can take keyboard focus has to be listed here.
     * While one is open the command prompt must not grab focus back,
     * and once it closes this flips and the prompt re-requests it.
     * An overlay missing from this list leaves the launcher with no
     * focused field at all once it closes, so key presses go nowhere.
     */
    val launcherCommandCanOwnFocus =
        autoFocusCommandInput &&
                !showAppSearch &&
                !showCommandHelp &&
                !showCalendarSetup &&
                !showGeminiSetup &&
                !showQuickReply &&
                !showBattery &&
                !showQuickToggles &&
                !showFavoriteApps &&
                assistantAnswer == null

    /*
     * The Home prompt must remain keyboard-ready even after Android surfaces
     * temporarily steal window focus during first-run setup (launcher chooser,
     * permission screens, Google authorization, etc.).
     *
     * A one-shot FocusRequester is not enough on a fresh install because it
     * can fire before Prompt Launcher truly owns the window. Re-request after
     * composition and again whenever the Android window regains focus.
     */
    LaunchedEffect(
        launcherCommandCanOwnFocus
    ) {

        if (
            launcherCommandCanOwnFocus
        ) {

            delay(
                60
            )

            focusRequester
                .requestFocus()

            /*
             * A second short retry covers the first frame after returning
             * from onboarding/system UI on slower devices and emulators.
             */
            delay(
                180
            )

            if (
                launcherCommandCanOwnFocus
            ) {

                focusRequester
                    .requestFocus()
            }
        }
    }

    DisposableEffect(
        launcherView,
        launcherCommandCanOwnFocus
    ) {

        val windowFocusListener =
            ViewTreeObserver.OnWindowFocusChangeListener {
                    hasWindowFocus ->

                if (
                    hasWindowFocus &&
                    launcherCommandCanOwnFocus
                ) {

                    scope.launch {

                        delay(
                            60
                        )

                        focusRequester
                            .requestFocus()
                    }
                }
            }

        launcherView
            .viewTreeObserver
            .addOnWindowFocusChangeListener(
                windowFocusListener
            )

        onDispose {

            if (
                launcherView
                    .viewTreeObserver
                    .isAlive
            ) {

                launcherView
                    .viewTreeObserver
                    .removeOnWindowFocusChangeListener(
                        windowFocusListener
                    )
            }
        }
    }

    LaunchedEffect(
        commandText,
        showAppSearch,
        showCommandHelp,
        showCalendarSetup,
        showGeminiSetup,
        showQuickReply,
        showBattery,
        showQuickToggles,
        showFavoriteApps,
        assistantAnswer
    ) {

        if (
            commandOverlayOnly
        ) {

            onCommandOverlayActiveChange(
                commandText.isNotBlank() ||
                        showAppSearch ||
                        showCommandHelp ||
                        showCalendarSetup ||
                        showGeminiSetup ||
                        showQuickReply ||
                        showBattery ||
                        showQuickToggles ||
                        showFavoriteApps ||
                        assistantAnswer != null
            )
        }
    }


    if (
        showGeminiSetup
    ) {
        GeminiSetupScreen(
            hasExistingKey =
                geminiApiKeyStore.hasKey(),
            sharing =
                assistantSharing,
            onSharingChange = {
                    updated ->
                assistantSharingStore.save(
                    updated
                )
                assistantSharing =
                    updated
            },
            onSave = {
                    key ->
                geminiApiKeyStore.saveKey(
                    key
                )
                showGeminiSetup =
                    false
                setStatusText(
                    "✓ GEMINI READY"
                )
            },
            onBack = {
                showGeminiSetup =
                    false
            }
        )

        return
    }

    val commandFocusBlurTarget =
        if (
            commandText.isNotBlank() &&
            !showCommandHelp &&
            !showAppSearch &&
            !showCalendarSetup
        ) {
            3.5.dp
        } else {
            0.dp
        }

    val commandFocusBlur by
    animateDpAsState(
        targetValue =
            commandFocusBlurTarget,
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
            "commandFocusBlur"
    )

    val commandFocusDimTarget =
        if (
            commandText.isNotBlank() &&
            !showCommandHelp &&
            !showAppSearch &&
            !showCalendarSetup
        ) {
            0.72f
        } else {
            0f
        }

    val commandFocusDim by
    animateFloatAsState(
        targetValue =
            commandFocusDimTarget,
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
            "commandFocusDim"
    )

    val overlayBlurTarget =
        if (
            showCommandHelp ||
            showAppSearch ||
            showFavoriteApps ||
            showQuickToggles ||
            showQuickReply ||
            showBattery ||
            showCalendarSetup ||
            assistantAnswer != null
        ) {
            7.dp
        } else {
            0.dp
        }

    val overlayBlur by
    animateDpAsState(
        targetValue =
            overlayBlurTarget,
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
            "overlayBlur"
    )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    if (
                        commandOverlayOnly
                    ) {
                        Color.Transparent
                    } else {
                        BackgroundBlack
                    }
                )
    ) {
        if (
            !commandOverlayOnly
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .blur(
                            overlayBlur
                        )
                        .background(
                            BackgroundBlack
                        )
                        .padding(
                            horizontal =
                                10.dp,
                            vertical =
                                5.dp
                        )
            ) {

                /*
                 * Give the Titan 2 Elite camera cutout a little more
                 * vertical breathing room. The space is reclaimed
                 * below the second info card so the app-button grid
                 * stays in the same vertical position.
                 */
                Spacer(
                    modifier =
                        Modifier.height(
                            12.dp
                        )
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(
                                min = 72.dp
                            )
                            .blur(
                                commandFocusBlur
                            )
                ) {
                    Column(
                        modifier =
                            Modifier
                                .align(
                                    Alignment.CenterStart
                                )
                                .offset(
                                    y = 4.dp
                                )
                    ) {
                        Text(
                            text =
                                "$currentDay, $currentDate",
                            color =
                                SecondaryText,
                            fontSize =
                                11.sp,
                            lineHeight =
                                11.sp,
                            fontFamily =
                                InterfaceFont
                        )

                        if (ShowHomeWeather) {
                            Spacer(
                                modifier =
                                    Modifier.height(
                                        1.dp
                                    )
                            )

                            Row(
                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {
                                WeatherConditionIcon(
                                    condition =
                                        weatherSnapshot
                                            ?.condition
                                )

                                Spacer(
                                    modifier =
                                        Modifier.width(
                                            4.dp
                                        )
                                )

                                Text(
                                    text =
                                        weatherSnapshot
                                            ?.let { snapshot ->
                                                "${snapshot.condition.lowercase()}, ${snapshot.temperatureF}°"
                                            }
                                            ?: "weather",
                                    color =
                                        SecondaryText,
                                    fontSize =
                                        9.sp,
                                    lineHeight =
                                        10.sp,
                                    fontFamily =
                                        InterfaceFont,
                                    maxLines =
                                        1
                                )
                            }
                        }
                    }

                    /*
                     * The camera sits top-left on the Titan, so the
                     * time moved to the right where nothing overlaps
                     * it, and the buttons dropped below it. That also
                     * balances the header: two lines on each side.
                     */
                    Column(
                        modifier =
                            Modifier
                                .align(
                                    Alignment.CenterEnd
                                )
                                .offset(
                                    y = 4.dp
                                ),
                        horizontalAlignment =
                            Alignment.End
                    ) {

                        Text(
                            text =
                                currentTime,
                            color =
                                PrimaryText,
                            fontSize =
                                11.sp,
                            lineHeight =
                                11.sp,
                            fontFamily =
                                InterfaceFont,
                            fontWeight =
                                FontWeight.Medium
                        )

                        Spacer(
                            modifier =
                                Modifier.height(
                                    5.dp
                                )
                        )

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically,
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    8.dp
                                )
                        ) {

                            HeaderBatteryButton(
                                snapshot =
                                    batterySnapshot,
                                onClick = {
                                    showBattery =
                                        true
                                }
                            )

                            HeaderIconButton(
                                icon =
                                    Icons.Outlined.Tune,
                                contentDescription =
                                    "quick toggles",
                                /*
                                 * Accent whenever the phone is not in its
                                 * plain audible state, so a silenced or
                                 * Do Not Disturbed phone is visible from
                                 * the home screen without opening the
                                 * card.
                                 */
                                tint =
                                    if (
                                        soundModeActive
                                    ) {
                                        AccentOrange
                                    } else {
                                        PrimaryText
                                    },
                                onClick = {
                                    showQuickToggles =
                                        true
                                }
                            )

                            HeaderIconButton(
                                icon =
                                    Icons.Outlined.Bolt,
                                contentDescription =
                                    "quick reply",
                                tint =
                                    if (
                                        quickReplyEntries.any {
                                            it.unread
                                        }
                                    ) {
                                        AccentOrange
                                    } else {
                                        PrimaryText
                                    },
                                onClick = {
                                    showQuickReply =
                                        true
                                }
                            )

                            HeaderIconButton(
                                icon =
                                    Icons.Outlined.StarBorder,
                                contentDescription =
                                    "favorites",
                                onClick = {
                                    favoriteAssignSlot =
                                        null

                                    showFavoriteApps =
                                        true
                                }
                            )
                        }
                    }
                }

                if (ShowHomeProductivityDots) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .blur(
                                    commandFocusBlur
                                )
                    ) {
                        /*
                         * Without usage access every hour comes back
                         * pending, which looks like a working row.
                         * Tapping opens the grant screen.
                         */
                        val usageAccessGranted =
                            remember(
                                productivityHours
                            ) {

                                runCatching {
                                    usageStatsRepository
                                        .hasUsageAccess()
                                }
                                    .getOrDefault(
                                        true
                                    )
                            }

                        Box(
                            modifier =
                                Modifier
                                    .then(
                                        if (
                                            usageAccessGranted
                                        ) {
                                            Modifier
                                        } else {
                                            Modifier.clickable {

                                                setStatusText(
                                                    "ALLOW USAGE ACCESS FOR PROMPT LAUNCHER"
                                                )

                                                runCatching {
                                                    context.startActivity(
                                                        Intent(
                                                            Settings.ACTION_USAGE_ACCESS_SETTINGS
                                                        )
                                                    )
                                                }
                                                    .onFailure {
                                                        setStatusText(
                                                            "COULD NOT OPEN SETTINGS"
                                                        )
                                                    }
                                            }
                                        }
                                    )
                        ) {

                            ProductivityDotsRow(
                                statuses =
                                    productivityHours
                            )
                        }
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp
                            )
                    )
                }

                if (ShowHomeAttentionCard) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .blur(
                                    commandFocusBlur
                                )
                    ) {
                        InfoCard(
                            icon =
                                Icons.Outlined.Message,
                            title =
                                when {

                                    unreadCount > 0 &&
                                            importantUnreadMailCount > 0 ->
                                        "$unreadCount texts · $importantUnreadMailCount important mail"

                                    unreadCount == 1 ->
                                        "1 unread message"

                                    unreadCount > 1 ->
                                        "$unreadCount unread messages"

                                    importantUnreadMailCount == 1 ->
                                        "1 important email"

                                    importantUnreadMailCount > 1 ->
                                        "$importantUnreadMailCount important emails"

                                    else ->
                                        "messages"
                                },
                            subtitle =
                                when {

                                    latestUnread != null ->
                                        latestUnread.body.take(
                                            44
                                        )

                                    importantUnreadMail != null ->
                                        "${importantUnreadMail.sender}: ${importantUnreadMail.subject}"
                                            .take(
                                                44
                                            )

                                    /*
                                     * A text that arrived as a
                                     * notification has no stored row
                                     * to preview, so the newest one
                                     * is read from the notification
                                     * itself. Without this the card
                                     * counted the message and then
                                     * claimed everything was clear.
                                     */
                                    liveConversationPreview != null ->
                                        liveConversationPreview.take(
                                            44
                                        )

                                    else ->
                                        "all caught up"
                                },
                            badge =
                                if (
                                    unreadCount +
                                    importantUnreadMailCount > 0
                                ) {
                                    (
                                            unreadCount +
                                                    importantUnreadMailCount
                                            )
                                        .toString()
                                } else {
                                    ""
                                },
                            onClick = {

                                if (
                                    unreadCount > 0
                                ) {

                                    openLatestUnreadMessage()

                                } else if (
                                    importantUnreadMail != null
                                ) {

                                    onOpenMail()

                                } else {

                                    if (
                                        !handOffMessagingIfNotDefault()
                                    ) {
                                        onOpenMessagesInbox()
                                    }
                                }
                            }
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp
                            )
                    )

                }

                if (ShowHomeCalendarCard) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .blur(
                                    commandFocusBlur
                                )
                    ) {
                        InfoCard(
                            icon =
                                Icons.Outlined.Event,
                            title =
                                upcomingEvent
                                    ?.title
                                    ?: "calendar",
                            subtitle =
                                upcomingEvent
                                    ?.let {

                                        formatCalendarEventTime(
                                            it
                                        )
                                    }
                                    ?: "nothing upcoming",
                            badge =
                                "",
                            onClick = {

                                openCalendar()
                            }
                        )
                    }

                }

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .blur(
                                commandFocusBlur
                            )
                ) {
                    AppButtonRow(
                        buttons =
                            listOf(
                                AppButtonData(
                                    Icons.Outlined.Description,
                                    "Note"
                                ) {

                                    onOpenNotes()
                                },
                                AppButtonData(
                                    Icons.Outlined.Event,
                                    "Event"
                                ) {

                                    commandText =
                                        ":"

                                    focusRequester
                                        .requestFocus()
                                },
                                AppButtonData(
                                    Icons.Outlined.AccessTime,
                                    "Clock"
                                ) {

                                    commandText =
                                        "!"

                                    focusRequester
                                        .requestFocus()
                                },
                                AppButtonData(
                                    Icons.Outlined.CheckCircle,
                                    "To Do"
                                ) {

                                    commandText =
                                        "+"

                                    focusRequester
                                        .requestFocus()
                                }
                            )
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(
                            6.dp
                        )
                )

                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .blur(
                                commandFocusBlur
                            )
                ) {
                    AppButtonRow(
                        buttons =
                            listOf(
                                AppButtonData(
                                    Icons.Outlined.Call,
                                    "Call"
                                ) {

                                    commandText =
                                        "/"

                                    focusRequester
                                        .requestFocus()
                                },
                                AppButtonData(
                                    Icons.Outlined.Message,
                                    "Message"
                                ) {

                                    /*
                                     * Straight to the messaging app
                                     * when it owns SMS: that is where
                                     * the conversations actually
                                     * live.
                                     */
                                    if (
                                        !handOffMessagingIfNotDefault()
                                    ) {
                                        onOpenMessagesInbox()
                                    }
                                },
                                AppButtonData(
                                    Icons.Outlined.CameraAlt,
                                    "Camera"
                                ) {

                                    try {

                                        context.startActivity(
                                            Intent(
                                                MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
                                            )
                                        )

                                    } catch (
                                        _: Exception
                                    ) {

                                        setStatusText(
                                            "CAMERA NOT AVAILABLE"
                                        )
                                    }
                                },
                                AppButtonData(
                                    Icons.Outlined.Email,
                                    "Mail"
                                ) {

                                    onOpenMail()
                                }
                            )
                    )
                }

            }
        }

        val commandPromptActive =
            commandText.isNotBlank() &&
                    !showCommandHelp &&
                    !showAppSearch &&
                    !showCalendarSetup

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(
                            alpha =
                                commandFocusDim
                        )
                    )
                    .then(
                        if (
                            commandPromptActive
                        ) {

                            Modifier.pointerInput(
                                commandText
                            ) {

                                detectTapGestures {

                                    commandText =
                                        ""

                                    inlineContactSelectionIndex =
                                        0

                                    scope.launch {

                                        delay(
                                            20
                                        )

                                        focusRequester
                                            .requestFocus()
                                    }
                                }
                            }

                        } else {

                            Modifier
                        }
                    )
        )

        Column(
            modifier =
                Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .fillMaxWidth()
                    .padding(
                        start = 10.dp,
                        end = 10.dp,
                        bottom = 8.dp
                    )
        ) {

            /*
             * Confirmations and errors, as a bubble above the command
             * bar. Deliberately outside the commandText guard below:
             * submitting clears the text, so anything rendered inside
             * that block vanishes at the exact moment the
             * confirmation is set.
             */
            AnimatedVisibility(
                visible =
                    statusText.isNotBlank(),
                enter =
                    fadeIn(),
                exit =
                    fadeOut()
            ) {

                Row(
                    modifier =
                        Modifier
                            .padding(
                                bottom = 5.dp
                            )
                            .background(
                                SurfaceBlack,
                                RoundedCornerShape(
                                    12.dp
                                )
                            )
                            .border(
                                0.75.dp,
                                AccentOrange,
                                RoundedCornerShape(
                                    12.dp
                                )
                            )
                            .padding(
                                horizontal = 10.dp,
                                vertical = 5.dp
                            )
                ) {

                    Text(
                        text =
                            statusText,
                        color =
                            AccentOrange,
                        fontSize =
                            9.5.sp,
                        lineHeight =
                            9.5.sp,
                        fontFamily =
                            InterfaceFont,
                        fontWeight =
                            FontWeight.Medium,
                        maxLines =
                            2,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }
            }

            if (
                commandText.isNotBlank()
            ) {

                /*
                 * Hidden while a contact name is being typed, for the
                 * same reason the command legend is: the suggestions
                 * below are what the user is looking at.
                 */
                if (
                    recentWebSearches.isNotEmpty() &&
                    inlineContactQuery.isBlank() &&
                    groupComposeQuery.isBlank()
                ) {

                    RecentSearchesCard(
                        searches =
                            recentWebSearches,
                        onSearchSelected = {
                                query ->

                            commandText =
                                query

                            inlineContactSelectionIndex =
                                0

                            scope.launch {

                                delay(
                                    20
                                )

                                focusRequester
                                    .requestFocus()
                            }
                        },
                        onRemoveSearch = {
                                query ->

                            removeRecentWebSearch(
                                query
                            )
                        },
                        onClearAll = {

                            clearRecentWebSearches()
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                5.dp
                            )
                    )
                }

                /*
                 * Hidden once a contact name is being typed: the
                 * suggestion list below is what matters then, and the
                 * legend just pushes it off screen.
                 */
                if (
                    inlineContactQuery.isBlank() &&
                    groupComposeQuery.isBlank()
                ) {

                    CommandHintsCard(
                        commandText =
                            commandText,
                        onCommandSelected = {
                                selectedCommand ->

                            commandText =
                                selectedCommand

                            inlineContactSelectionIndex =
                                0

                            scope.launch {

                                delay(
                                    20
                                )

                                focusRequester
                                    .requestFocus()
                            }
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                5.dp
                            )
                    )
                }

                val looksLikeFreeText =
                    showSearchTargetPicker &&
                            commandText.isNotBlank() &&
                            commandText
                                .trim()
                                .firstOrNull() !in
                            LauncherSymbolCommands &&
                            LauncherLegacyCommands.none {
                                    word ->

                                commandText
                                    .trim()
                                    .lowercase()
                                    .let {
                                        it == word ||
                                                it.startsWith(
                                                    "$word "
                                                )
                                    }
                            }

                if (
                    looksLikeFreeText
                ) {

                    Row(
                        modifier =
                            Modifier
                                .padding(
                                    bottom = 5.dp
                                ),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                6.dp
                            )
                    ) {

                        SearchTargetChip(
                            label = "chrome",
                            selected = !searchWithClaude,
                            onClick = {
                                setSearchWithClaude(false)
                            }
                        )

                        SearchTargetChip(
                            label = "claude",
                            selected = searchWithClaude,
                            onClick = {
                                setSearchWithClaude(true)
                            }
                        )
                    }
                }

                CommandContextPanel(
                    commandText =
                        commandText,
                    groupRecipients =
                        groupRecipients.map {
                            it.name
                        },
                    groupMatches =
                        groupContactMatches,
                    selectedGroupIndex =
                        groupSelectionIndex,
                    onChooseGroupContact = {
                            contact ->

                        addGroupRecipient(
                            contact
                        )
                    },
                    contactQuery =
                        inlineContactQuery,
                    contacts =
                        inlineContactMatches,
                    selectedContactIndex =
                        inlineContactSelectionIndex,
                    onChooseContact = {
                            contact ->

                        chooseInlineContact(
                            contact
                        )
                    },
                    apps =
                        inlineAppMatches,
                    selectedAppIndex =
                        inlineAppSelectionIndex,
                    onChooseApp = {
                            app ->

                        chooseInlineApp(
                            app
                        )
                    }
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            5.dp
                        )
                )
            }

            CommandBar(
                commandText =
                    commandText,
                onCommandTextChange = {

                    commandText =
                        it
                },
                focusRequester =
                    focusRequester,
                onSubmit = {

                    submitCommand()
                },
                onPromptClick = {
                    showCommandHelp = true
                },
                hasInlineSuggestions =
                    inlineContactMatches.isNotEmpty() ||
                            inlineAppMatches.isNotEmpty() ||
                            groupContactMatches.isNotEmpty(),
                onNavigateInlineSuggestion = {
                        delta ->

                    if (
                        groupContactMatches.isNotEmpty()
                    ) {
                        groupSelectionIndex =
                            (
                                    groupSelectionIndex +
                                            delta
                                    )
                                .coerceIn(
                                    0,
                                    groupContactMatches.lastIndex
                                )
                    } else if (
                        inlineAppMatches.isNotEmpty()
                    ) {
                        inlineAppSelectionIndex =
                            (
                                    inlineAppSelectionIndex +
                                            delta
                                    )
                                .coerceIn(
                                    0,
                                    inlineAppMatches.lastIndex
                                )
                    } else if (
                        inlineContactMatches.isNotEmpty()
                    ) {
                        inlineContactSelectionIndex =
                            (
                                    inlineContactSelectionIndex +
                                            delta
                                    )
                                .coerceIn(
                                    0,
                                    inlineContactMatches.lastIndex
                                )
                    }
                },
                onChooseInlineSuggestion = {

                    if (
                        groupContactMatches.isNotEmpty()
                    ) {

                        groupContactMatches
                            .getOrNull(
                                groupSelectionIndex
                            )
                            ?.let {
                                addGroupRecipient(
                                    it
                                )
                            }

                    } else if (
                        inlineAppMatches.isNotEmpty()
                    ) {

                        inlineAppMatches
                            .getOrNull(
                                inlineAppSelectionIndex
                            )
                            ?.let {
                                chooseInlineApp(
                                    it
                                )
                            }

                    } else {

                        inlineContactMatches
                            .getOrNull(
                                inlineContactSelectionIndex
                            )
                            ?.let {
                                chooseInlineContact(
                                    it
                                )
                            }
                    }
                },
                visible =
                    commandText.isNotBlank()
            )
        }

        if (
            assistantAnswer != null
        ) {

            val closeAssistant = {

                assistantQuestion =
                    null

                assistantAnswer =
                    null

                assistantTurns =
                    emptyList()

                assistantNavigationTarget =
                    null

                assistantThinking =
                    false
            }

            /*
             * The assistant answer draws over home but registered no
             * back handling, so the press fell through to MainActivity
             * where back on the home screen is a no-op and nothing
             * happened. Catch it here instead.
             */
            BackHandler {
                closeAssistant()
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Color.Black.copy(
                                alpha =
                                    0.26f
                            )
                        )
                        .pointerInput(
                            assistantAnswer
                        ) {

                            detectTapGestures {

                                closeAssistant()
                            }
                        }
            )

            Box(
                modifier =
                    Modifier
                        .align(
                            Alignment.Center
                        )
                        .fillMaxWidth(
                            0.94f
                        )
                        .fillMaxHeight(
                            0.91f
                        )
                        .clip(
                            RoundedCornerShape(
                                16.dp
                            )
                        )
                        .background(
                            BackgroundBlack
                        )
                        .border(
                            0.9.dp,
                            BorderGray,
                            RoundedCornerShape(
                                16.dp
                            )
                        )
                        .pointerInput(Unit) {

                            detectTapGestures {
                                // Consume taps inside the AI Assist window.
                            }
                        }
            ) {

                LocalAssistantScreen(
                    turns =
                        assistantTurns,
                    isThinking =
                        assistantThinking,
                    onSend = {
                            followUp ->

                        if (
                            followUp.isNotBlank() &&
                            !assistantThinking
                        ) {

                            val directNavigation =
                                directNavigationTarget(
                                    followUp
                                )

                            if (
                                directNavigation != null
                            ) {

                                val navigationAnswer =
                                    "opening maps → $directNavigation"

                                assistantTurns =
                                    assistantTurns +
                                            AssistantTurn(
                                                question =
                                                    followUp,
                                                answer =
                                                    navigationAnswer
                                            )

                                assistantAnswer =
                                    navigationAnswer

                                assistantNavigationTarget =
                                    directNavigation

                                openMapsSearch(
                                    directNavigation
                                )

                                return@LocalAssistantScreen
                            }

                            if (
                                isContextualNavigationRequest(
                                    followUp
                                )
                            ) {

                                val destination =
                                    assistantNavigationTarget
                                        ?: calendarNavigationTarget()

                                if (
                                    destination != null
                                ) {

                                    val navigationAnswer =
                                        "opening maps → $destination"

                                    assistantTurns =
                                        assistantTurns +
                                                AssistantTurn(
                                                    question =
                                                        followUp,
                                                    answer =
                                                        navigationAnswer
                                                )

                                    assistantAnswer =
                                        navigationAnswer

                                    openMapsSearch(
                                        destination
                                    )

                                } else {

                                    val noLocation =
                                        "i don't have a location to navigate to yet"

                                    assistantTurns =
                                        assistantTurns +
                                                AssistantTurn(
                                                    question =
                                                        followUp,
                                                    answer =
                                                        noLocation
                                                )

                                    assistantAnswer =
                                        noLocation
                                }

                                return@LocalAssistantScreen
                            }

                            if (
                                isWhereFollowUp(
                                    followUp
                                )
                            ) {

                                val location =
                                    assistantNavigationTarget
                                        ?.trim()
                                        .orEmpty()
                                        .ifBlank {

                                            upcomingEvent
                                                ?.location
                                                ?.trim()
                                                .orEmpty()
                                        }

                                val locationAnswer =
                                    if (
                                        location.isNotBlank()
                                    ) {

                                        assistantNavigationTarget =
                                            location

                                        location

                                    } else {

                                        "no location is saved for that event"
                                    }

                                assistantTurns =
                                    assistantTurns +
                                            AssistantTurn(
                                                question =
                                                    followUp,
                                                answer =
                                                    locationAnswer
                                            )

                                assistantAnswer =
                                    locationAnswer

                                return@LocalAssistantScreen
                            }

                            if (
                                !geminiApiKeyStore.hasKey()
                            ) {

                                assistantTurns =
                                    assistantTurns +
                                            AssistantTurn(
                                                question =
                                                    followUp,
                                                answer =
                                                    "GEMINI NOT SET UP\n\nrun geminisetup"
                                            )

                            } else {

                                val previousTurns =
                                    assistantTurns

                                val history =
                                    previousTurns
                                        .takeLast(
                                            6
                                        )
                                        .joinToString(
                                            separator =
                                                "\n\n"
                                        ) {
                                                turn ->

                                            "USER: ${turn.question}\nASSISTANT: ${turn.answer}"
                                        }

                                val latestIncoming =
                                    recentMessages
                                        .firstOrNull {
                                            it.incoming
                                        }

                                val importantMailCount =
                                    mailMessages.count {
                                        it.important &&
                                                it.unread
                                    }

                                val launcherContext =
                                    buildAssistantLauncherContext(
                                        sharing =
                                            assistantSharingStore.load(),
                                        unreadTextCount =
                                            unreadCount,
                                        importantUnreadMailCount =
                                            importantMailCount,
                                        nextEventLine = {
                                            upcomingEvent
                                                ?.let {
                                                    "${formatCalendarEventTime(it)}  ${it.title}"
                                                }
                                                ?: "nothing upcoming"
                                        },
                                        nextEventLocation = {
                                            upcomingEvent?.location
                                        },
                                        calendarAgenda = {
                                            calendarAgendaContext()
                                        },
                                        latestIncomingText = {
                                            latestIncoming
                                                ?.let { message ->
                                                    val sender =
                                                        if (
                                                            hasContactsPermission
                                                        ) {
                                                            contactRepository
                                                                .findContactNameByPhoneNumber(
                                                                    message.phoneNumber
                                                                )
                                                                ?: message.phoneNumber
                                                        } else {
                                                            message.phoneNumber
                                                        }

                                                    sender to message.body
                                                }
                                        },
                                        mailMessages =
                                            mailMessages
                                    )

                                assistantTurns =
                                    previousTurns +
                                            AssistantTurn(
                                                question =
                                                    followUp,
                                                answer =
                                                    "THINKING..."
                                            )

                                assistantThinking =
                                    true

                                scope.launch {

                                    val response =
                                        geminiRepository
                                            .askConversationFollowUp(
                                                apiKey =
                                                    geminiApiKeyStore.getKey(),
                                                question =
                                                    followUp,
                                                launcherContext =
                                                    launcherContext,
                                                conversationHistory =
                                                    history
                                            )

                                    assistantTurns =
                                        previousTurns +
                                                AssistantTurn(
                                                    question =
                                                        followUp,
                                                    answer =
                                                        response
                                                )

                                    assistantAnswer =
                                        response

                                    assistantThinking =
                                        false
                                }
                            }
                        }
                    },
                    onBack =
                        closeAssistant
                )
            }
        }

        if (showAppSearch) {
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
                                showAppSearch = false
                            }
                        },
                contentAlignment =
                    Alignment.Center
            ) {
                AppSearchOverlayCard(
                    onLaunchApp = {
                            app ->

                        val launchIntent =
                            context
                                .packageManager
                                .getLaunchIntentForPackage(
                                    app.packageName
                                )

                        if (
                            launchIntent == null
                        ) {
                            setStatusText(
                                "APP NOT AVAILABLE"
                            )
                        } else {
                            try {
                                context.startActivity(
                                    launchIntent
                                )

                                showAppSearch =
                                    false
                            } catch (
                                _: Exception
                            ) {
                                setStatusText(
                                    "APP COULD NOT OPEN"
                                )
                            }
                        }
                    },
                    onDismiss = {
                        showAppSearch =
                            false
                    }
                )
            }
        }

        if (showFavoriteApps) {

            FavoritesOverlay(
                favorites =
                    favoriteApps,
                assigningSlot =
                    favoriteAssignSlot,
                onLaunch = { packageName ->

                    val launchIntent =
                        context
                            .packageManager
                            .getLaunchIntentForPackage(
                                packageName
                            )

                    if (
                        launchIntent == null
                    ) {
                        setStatusText(
                            "APP NOT AVAILABLE"
                        )
                    } else {
                        try {
                            context.startActivity(
                                launchIntent
                            )

                            showFavoriteApps =
                                false
                        } catch (
                            _: Exception
                        ) {
                            setStatusText(
                                "APP COULD NOT OPEN"
                            )
                        }
                    }
                },
                onAssignRequest = { slotIndex ->

                    favoriteAssignSlot =
                        slotIndex
                },
                onAssignApp = { slotIndex, app ->

                    favoriteApps =
                        favoriteAppsStore
                            .assign(
                                slotIndex,
                                app.packageName
                            )

                    favoriteAssignSlot =
                        null
                },
                onCancelAssign = {
                    favoriteAssignSlot =
                        null
                },
                onClear = { slotIndex ->

                    favoriteApps =
                        favoriteAppsStore
                            .clear(
                                slotIndex
                            )
                },
                onDismiss = {
                    showFavoriteApps =
                        false

                    favoriteAssignSlot =
                        null
                }
            )
        }

        if (showQuickReply) {

            QuickReplyOverlay(
                entries =
                    quickReplyEntries,
                onSendMessage =
                    onSendMessage,
                onMarkConversationRead = { key ->

                    scope.launch {

                        /*
                         * Quick reply passes a thread key, which for a
                         * one-to-one chat is just the other number.
                         * Marking by thread covers both.
                         */
                        smsDatabase
                            .smsDao()
                            .markThreadRead(
                                key
                            )

                        smsDatabase
                            .smsDao()
                            .markConversationRead(
                                key
                            )
                    }
                },
                onOpenConversation =
                    onOpenConversation,
                onStatus = {
                        message ->

                    setStatusText(
                        message
                    )
                },
                onDismiss = {
                    showQuickReply =
                        false
                }
            )
        }

        if (showBattery) {

            BatteryOverlayCard(
                snapshot =
                    batterySnapshot,
                onDismiss = {
                    showBattery =
                        false
                }
            )
        }

        if (showQuickToggles) {
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
                                showQuickToggles =
                                    false
                            }
                        },
                contentAlignment =
                    Alignment.Center
            ) {

                QuickTogglesOverlayCard(
                    onDismiss = {
                        showQuickToggles =
                            false
                    }
                )
            }
        }

        if (showCalendarSetup) {
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
                                onDismissCalendarSetup()
                            }
                        },
                contentAlignment =
                    Alignment.Center
            ) {
                CalendarSetupOverlayCard(
                    calendars =
                        calendarSetupOptions,
                    currentDefaultId =
                        currentCalendarId,
                    onSave =
                        onSaveCalendarSelection,
                    onDismiss =
                        onDismissCalendarSetup
                )
            }
        }

        if (showCommandHelp) {
            CommandHelpOverlay(
                onDismiss = {
                    showCommandHelp = false
                }
            )
        }
    }
}


@Composable
internal fun CommandHelpOverlay(
    onDismiss: () -> Unit
) {

    var pageProgress by remember {
        mutableFloatStateOf(
            0f
        )
    }

    val scope =
        rememberCoroutineScope()

    fun animateToPage(
        page: Int
    ) {

        val target =
            page
                .coerceIn(
                    0,
                    1
                )
                .toFloat()

        scope.launch {
            animate(
                initialValue =
                    pageProgress,
                targetValue =
                    target,
                animationSpec =
                    tween(
                        durationMillis =
                            180
                    )
            ) {
                    value,
                    _ ->

                pageProgress =
                    value
            }
        }
    }

    BoxWithConstraints(
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
                }
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

                pageProgress =
                    (
                            pageProgress -
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
                        when {
                            velocity <
                                    -900f ->
                                1f

                            velocity >
                                    900f ->
                                0f

                            pageProgress >=
                                    0.5f ->
                                1f

                            else ->
                                0f
                        }

                    animate(
                        initialValue =
                            pageProgress,
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

                        pageProgress =
                            value
                    }
                }
            )

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        swipeModifier
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
                                            -pageProgress *
                                                    widthPx
                                            )
                                        .roundToInt(),
                                y =
                                    0
                            )
                        },
                contentAlignment =
                    Alignment.Center
            ) {
                SymbolCommandHelpCard()
            }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .offset {

                            IntOffset(
                                x =
                                    (
                                            (
                                                    1f -
                                                            pageProgress
                                                    ) *
                                                    widthPx
                                            )
                                        .roundToInt(),
                                y =
                                    0
                            )
                        },
                contentAlignment =
                    Alignment.Center
            ) {
                WordCommandHelpCard()
            }

            if (
                pageProgress <
                0.5f
            ) {
                CommandHelpPageButton(
                    symbol =
                        ">",
                    modifier =
                        Modifier
                            .align(
                                Alignment.CenterEnd
                            )
                            .padding(
                                end =
                                    7.dp
                            ),
                    onClick = {
                        animateToPage(
                            1
                        )
                    }
                )
            } else {
                CommandHelpPageButton(
                    symbol =
                        "<",
                    modifier =
                        Modifier
                            .align(
                                Alignment.CenterStart
                            )
                            .padding(
                                start =
                                    7.dp
                            ),
                    onClick = {
                        animateToPage(
                            0
                        )
                    }
                )
            }
        }
    }
}


@Composable
internal fun SymbolCommandHelpCard() {

    Column(
        modifier =
            Modifier
                .widthIn(
                    max =
                        238.dp
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
                        // Keep taps inside the floating card from dismissing it.
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
                "commands",
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
                    6.dp
                )
        )

        CommandHelpRow(
            command =
                ".",
            label =
                "apps"
        )
        CommandHelpRow(
            command =
                "@",
            label =
                "message"
        )
        CommandHelpRow(
            command =
                "/",
            label =
                "call"
        )
        CommandHelpRow(
            command =
                ":",
            label =
                "event"
        )
        CommandHelpRow(
            command =
                "+",
            label =
                "to do"
        )
        CommandHelpRow(
            command =
                "?",
            label =
                "assistant"
        )
        CommandHelpRow(
            command =
                "\"",
            label =
                "note"
        )
        CommandHelpRow(
            command =
                "!",
            label =
                "timer"
        )
        CommandHelpRow(
            command =
                "!!",
            label =
                "alarm"
        )
    }
}


@Composable
internal fun WordCommandHelpCard() {

    Column(
        modifier =
            Modifier
                .widthIn(
                    max =
                        258.dp
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
                        // Keep taps inside the floating card from dismissing it.
                    }
                }
                .padding(
                    horizontal =
                        11.dp,
                    vertical =
                        8.dp
                )
    ) {
        Text(
            text =
                "word commands",
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
                    5.dp
                )
        )

        WordCommandHelpRow(
            command =
                "go <place>",
            label =
                "maps search"
        )
        WordCommandHelpRow(
            command =
                "calendarsetup",
            label =
                "choose calendar"
        )
        WordCommandHelpRow(
            command =
                "weathersetup",
            label =
                "weather access"
        )
        WordCommandHelpRow(
            command =
                "usagesetup",
            label =
                "usage access"
        )
        WordCommandHelpRow(
            command =
                "geminisetup",
            label =
                "assistant setup"
        )
        WordCommandHelpRow(
            command =
                "email <name>",
            label =
                "compose mail"
        )
        WordCommandHelpRow(
            command =
                "settings",
            label =
                "device settings"
        )
        WordCommandHelpRow(
            command =
                "todoistsetup",
            label =
                "connect todoist"
        )
        WordCommandHelpRow(
            command =
                "bug",
            label =
                "report beta issue"
        )
    }
}


@Composable
internal fun CommandHelpPageButton(
    symbol: String,
    modifier: Modifier =
        Modifier,
    onClick: () -> Unit
) {

    Box(
        modifier =
            modifier
                .size(
                    width =
                        34.dp,
                    height =
                        56.dp
                )
                .background(
                    SurfaceBlack.copy(
                        alpha =
                            0.94f
                    ),
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
                .clickable(
                    onClick =
                        onClick
                ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text =
                symbol,
            color =
                PrimaryText,
            fontSize =
                22.sp,
            lineHeight =
                22.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.Medium
        )
    }
}


@Composable
internal fun WordCommandHelpRow(
    command: String,
    label: String
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        1.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text =
                command,
            color =
                LauncherCommandRed,
            fontSize =
                8.2.sp,
            lineHeight =
                9.5.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.Bold,
            modifier =
                Modifier.width(
                    112.dp
                ),
            maxLines =
                1
        )

        Text(
            text =
                label,
            color =
                PrimaryText,
            fontSize =
                8.sp,
            lineHeight =
                9.5.sp,
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
internal fun AppSearchOverlayCard(
    title: String = "apps",
    onLaunchApp: (LauncherAppEntry) -> Unit,
    onDismiss: () -> Unit
) {
    val context =
        LocalContext.current

    val focusRequester =
        remember {
            FocusRequester()
        }

    val listState =
        rememberLazyListState()

    val allApps =
        remember(
            context
        ) {
            loadLaunchableApps(
                context
            )
        }

    var query by remember {
        mutableStateOf("")
    }

    var selectedIndex by remember {
        mutableIntStateOf(
            0
        )
    }

    val filteredApps =
        remember(
            allApps,
            query
        ) {
            val normalized =
                query
                    .trim()
                    .lowercase()

            if (
                normalized.isBlank()
            ) {

                allApps

            } else {

                allApps
                    .filter { app ->

                        app.label
                            .trim()
                            .lowercase()
                            .startsWith(
                                normalized
                            )
                    }
                    .sortedBy {
                        it.label
                            .lowercase()
                    }
            }
        }

    LaunchedEffect(Unit) {
        focusRequester
            .requestFocus()
    }

    LaunchedEffect(
        query
    ) {
        selectedIndex =
            0
    }

    LaunchedEffect(
        selectedIndex,
        filteredApps.size
    ) {
        if (
            filteredApps.isNotEmpty()
        ) {
            selectedIndex =
                selectedIndex
                    .coerceIn(
                        0,
                        filteredApps.lastIndex
                    )

            listState
                .animateScrollToItem(
                    selectedIndex
                )
        } else {
            selectedIndex =
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
                    max = 205.dp
                )
                .heightIn(
                    min = 230.dp,
                    max = 340.dp
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
                        // Consume taps inside the card so only outside taps dismiss it.
                    }
                }
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
                                    filteredApps.isNotEmpty()
                                ) {
                                    selectedIndex =
                                        (selectedIndex + 1)
                                            .coerceAtMost(
                                                filteredApps.lastIndex
                                            )
                                }

                                true
                            }

                            Key.DirectionUp -> {
                                selectedIndex =
                                    (selectedIndex - 1)
                                        .coerceAtLeast(
                                            0
                                        )

                                true
                            }

                            Key.Enter -> {
                                filteredApps
                                    .getOrNull(
                                        selectedIndex
                                    )
                                    ?.let(
                                        onLaunchApp
                                    )

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
                    horizontal = 12.dp,
                    vertical = 10.dp
                )
    ) {
        Text(
            text = title,
            color = PrimaryText,
            fontSize = 14.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.Medium
        )

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
                        32.dp
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
                        horizontal =
                            8.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text = ">",
                color = PrimaryText,
                fontSize = 12.sp,
                fontFamily = InterfaceFont,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.width(
                        6.dp
                    )
            )

            BasicTextField(
                value =
                    query,
                onValueChange = {
                    query =
                        it.replace(
                            "\n",
                            ""
                        )
                },
                modifier =
                    Modifier
                        .weight(
                            1f
                        )
                        .fillMaxHeight()
                        .focusRequester(
                            focusRequester
                        ),
                singleLine =
                    true,
                textStyle =
                    TextStyle(
                        color =
                            PrimaryText,
                        fontSize =
                            10.sp,
                        fontFamily =
                            InterfaceFont
                    ),
                cursorBrush =
                    SolidColor(
                        PrimaryText
                    ),
                decorationBox = {
                        innerTextField ->

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.CenterStart
                    ) {
                        if (
                            query.isBlank()
                        ) {
                            Text(
                                text =
                                    "search apps",
                                color =
                                    TertiaryText,
                                fontSize =
                                    9.sp,
                                fontFamily =
                                    InterfaceFont
                            )
                        }

                        innerTextField()
                    }
                }
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    6.dp
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
                    2.dp
                )
        ) {
            if (
                filteredApps.isEmpty()
            ) {
                item {
                    Text(
                        text =
                            "no apps found",
                        color =
                            SecondaryText,
                        fontSize =
                            8.sp,
                        fontFamily =
                            InterfaceFont,
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    8.dp,
                                vertical =
                                    10.dp
                            )
                    )
                }
            } else {
                itemsIndexed(
                    items =
                        filteredApps,
                    key = {
                            _,
                            app ->

                        app.packageName
                    }
                ) {
                        _,
                        app ->

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(
                                    34.dp
                                )
                                .background(
                                    Color.Transparent,
                                    RoundedCornerShape(
                                        7.dp
                                    )
                                )
                                .clickable {
                                    onLaunchApp(
                                        app
                                    )
                                }
                                .padding(
                                    horizontal =
                                        8.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Text(
                            text =
                                app.label,
                            color =
                                PrimaryText,
                            fontSize =
                                12.5.sp,
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

                        val appIcon =
                            rememberAppIconBitmap(
                                app.packageName
                            )

                        if (
                            appIcon != null
                        ) {

                            Spacer(
                                modifier =
                                    Modifier.width(
                                        8.dp
                                    )
                            )

                            Image(
                                bitmap =
                                    appIcon,
                                contentDescription =
                                    app.label,
                                modifier =
                                    Modifier.size(
                                        20.dp
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun EmailContactChooserScreen(
    contacts: List<EmailContact>,
    onChoose: (EmailContact) -> Unit,
    onBack: () -> Unit
) {

    val focusRequester =
        remember {
            FocusRequester()
        }

    val listState =
        rememberLazyListState()

    var selectedIndex by remember {
        mutableIntStateOf(
            0
        )
    }

    LaunchedEffect(Unit) {

        focusRequester
            .requestFocus()
    }

    LaunchedEffect(
        selectedIndex
    ) {

        if (
            contacts.isNotEmpty()
        ) {

            listState
                .animateScrollToItem(
                    selectedIndex
                )
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
                    horizontal = 10.dp,
                    vertical = 7.dp
                )
                .focusRequester(
                    focusRequester
                )
                .focusable()
                .onPreviewKeyEvent {
                        event ->

                    if (
                        event.type != KeyEventType.KeyDown
                    ) {

                        false

                    } else {

                        when (
                            event.key
                        ) {

                            Key.DirectionDown -> {

                                selectedIndex =
                                    (selectedIndex + 1)
                                        .coerceAtMost(
                                            contacts.lastIndex
                                        )
                                true
                            }

                            Key.DirectionUp -> {

                                selectedIndex =
                                    (selectedIndex - 1)
                                        .coerceAtLeast(
                                            0
                                        )
                                true
                            }

                            Key.Enter -> {

                                contacts
                                    .getOrNull(
                                        selectedIndex
                                    )
                                    ?.let(
                                        onChoose
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

        Text(
            text = "CHOOSE EMAIL",
            color = PrimaryText,
            fontSize = 14.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.Medium,
            modifier =
                Modifier.padding(
                    start = CameraSafeStartPadding
                )
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        MinimalDivider()

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {

            itemsIndexed(
                items = contacts,
                key = {
                        _,
                        contact ->

                    "${contact.name}|${contact.emailAddress}"
                }
            ) {
                    index,
                    contact ->

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(
                                if (
                                    index == selectedIndex
                                ) {
                                    SurfaceBlack
                                } else {
                                    BackgroundBlack
                                },
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                width =
                                    if (
                                        index == selectedIndex
                                    ) {
                                        0.75.dp
                                    } else {
                                        0.dp
                                    },
                                color =
                                    if (
                                        index == selectedIndex
                                    ) {
                                        BorderGray
                                    } else {
                                        Color.Transparent
                                    },
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {

                                selectedIndex = index
                                onChoose(contact)
                            }
                            .padding(
                                horizontal = 8.dp
                            ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            if (
                                index == selectedIndex
                            ) {
                                ">"
                            } else {
                                " "
                            },
                        color = AccentOrange,
                        fontSize = 11.sp,
                        fontFamily = InterfaceFont
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = contact.name.lowercase(),
                            color = PrimaryText,
                            fontSize = 10.sp,
                            fontFamily = InterfaceFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = contact.emailAddress,
                            color = TertiaryText,
                            fontSize = 7.sp,
                            fontFamily = InterfaceFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

    }
}

data class AppButtonData(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

@Composable
fun AppButtonRow(
    buttons: List<AppButtonData>
) {

    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        buttons.forEach {
                button ->

            MinimalAppButton(
                icon =
                    button.icon,
                title =
                    button.label,
                onClick =
                    button.onClick
            )
        }
    }
}

@Composable
fun MinimalAppButton(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {

    val displayIconSizeDp =
        HomeButtonIconSizeDp * 1.22f

    Box(
        modifier =
            Modifier
                .size(
                    80.dp
                )
                .background(
                    HomeButtonSurface,
                    RoundedCornerShape(
                        HomeButtonCornerRadiusDp.dp
                    )
                )
                .border(
                    width =
                        0.75.dp,
                    color =
                        BorderGray,
                    shape =
                        RoundedCornerShape(
                            HomeButtonCornerRadiusDp.dp
                        )
                )
                .clickable {

                    onClick()
                }
    ) {

        Icon(
            imageVector =
                icon,
            contentDescription =
                title,
            tint =
                PrimaryText,
            modifier =
                Modifier
                    .align(
                        Alignment.Center
                    )
                    .offset(
                        y = (-5).dp
                    )
                    .size(
                        displayIconSizeDp.dp
                    )
        )

        Text(
            text =
                title,
            color =
                SecondaryText,
            fontSize =
                HomeButtonLabelSizeSp.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.Normal,
            textAlign =
                TextAlign.Center,
            maxLines =
                1,
            modifier =
                Modifier
                    .align(
                        Alignment.BottomCenter
                    )
                    .padding(
                        bottom = 5.dp
                    )
        )
    }
}

@Composable
fun InfoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    onClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    52.dp
                )
                .background(
                    HomeInfoCardSurface,
                    RoundedCornerShape(
                        9.dp
                    )
                )
                .border(
                    width =
                        0.75.dp,
                    color =
                        BorderGray,
                    shape =
                        RoundedCornerShape(
                            9.dp
                        )
                )
                .clickable {

                    onClick()
                }
                .padding(
                    horizontal =
                        8.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(
                        25.dp
                    )
                    .background(
                        AccentOrange.copy(
                            alpha =
                                0.12f
                        ),
                        RoundedCornerShape(
                            6.dp
                        )
                    ),
            contentAlignment =
                Alignment.Center
        ) {

            Icon(
                imageVector =
                    icon,
                contentDescription =
                    null,
                tint =
                    AccentOrange,
                modifier =
                    Modifier.size(
                        15.dp
                    )
            )
        }

        Spacer(
            modifier =
                Modifier.width(
                    8.dp
                )
        )

        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(
                text =
                    title,
                color =
                    PrimaryText,
                fontSize =
                    10.sp,
                lineHeight =
                    12.sp,
                fontFamily =
                    InterfaceFont,
                maxLines =
                    1
            )

            Spacer(
                modifier =
                    Modifier.height(
                        2.dp
                    )
            )

            Text(
                text =
                    subtitle,
                color =
                    TertiaryText,
                fontSize =
                    8.sp,
                lineHeight =
                    10.sp,
                fontFamily =
                    InterfaceFont,
                maxLines =
                    1
            )
        }

        if (
            badge.isNotBlank()
        ) {

            Text(
                text =
                    badge,
                color =
                    AccentOrange,
                fontSize =
                    8.sp,
                fontFamily =
                    InterfaceFont
            )
        }
    }
}

internal val LauncherSymbolCommands =
    setOf('@', '/', ':', '+', '?', '"', '!', '.')

internal val LauncherLegacyCommands =
    listOf(
        "weathersetup",
        "weatherstatus",
        "usagesetup",
        "usagestatus",
        "usagecheck",
        "geminisetup",
        "geministatus",
        "geminidisconnect",
        "email",
        "mail",
        "e",
        "calendarsetup",
        "calendarstatus",
        "todoistsetup",
        "todoiststatus",
        "todoistdisconnect",
        "messages",
        "phone",
        "go",
        "maps",
        "chrome",
        "settings",
        "bug",

        /*
         * The keyboard diagnostic. Without this the word falls
         * through to a web search, because only commands listed here
         * reach CommandParser.
         */
        "keys",
        "keyprobe",
        "keytest"
    )

internal fun sanitizeLauncherCommandInput(
    input: String
): String? {

    return input.replace(
        "\n",
        ""
    )
}


internal fun launcherRecognizedCommandPrefix(
    text: String
): Int {

    if (
        text.isBlank()
    ) {
        return 0
    }

    if (
        text.startsWith(
            "!!"
        )
    ) {
        return 2
    }

    /*
     * "@@" is one command, not an "@" followed by a stray character,
     * so both symbols are highlighted.
     */
    if (
        text.startsWith(
            "@@"
        )
    ) {
        return 2
    }

    if (
        text.length >=
        2 &&
        text.first() ==
        '"' &&
        text[1].lowercaseChar() in
        setOf(
            'p',
            'w',
            'i',
            'j'
        )
    ) {
        return 2
    }

    if (
        text.first() in
        LauncherSymbolCommands
    ) {
        return 1
    }

    val trimmed =
        text.trimStart()

    val firstToken =
        trimmed.substringBefore(
            " "
        )
            .lowercase()

    val hasTrailingText =
        trimmed.contains(
            " "
        )

    /*
     * These commands intentionally accept free-form text after the command
     * name and should remain command-colored while the user keeps typing.
     */
    val commandsWithArguments =
        setOf(
            "go",
            "email",
            "note",
            "timer",
            "alarm",
            "event"
        )

    if (
        hasTrailingText &&
        firstToken !in
        commandsWithArguments
    ) {
        return 0
    }

    val exactWordCommand =
        LauncherLegacyCommands
            .firstOrNull {
                it.equals(
                    firstToken,
                    ignoreCase =
                        true
                )
            }

    if (
        exactWordCommand !=
        null
    ) {
        return firstToken.length
    }

    /*
     * Only show partial-command coloring before the user has committed to
     * a full free-form sentence. This means:
     *
     * cal -> red (could become calendarsetup)
     * calendar -> red (still a true prefix)
     * calendars -> white (no longer a prefix)
     * calendars for sale -> white and will search Chrome
     */
    if (
        !hasTrailingText
    ) {

        val partialMatches =
            LauncherLegacyCommands
                .filter {
                    it.startsWith(
                        firstToken,
                        ignoreCase =
                            true
                    )
                }

        if (
            partialMatches.isNotEmpty()
        ) {
            return firstToken.length
        }
    }

    return 0
}


internal val LauncherCommandVisualTransformation =
    VisualTransformation { source ->

        val text =
            source.text

        val builder =
            AnnotatedString.Builder(
                text
            )

        val commandEnd =
            launcherRecognizedCommandPrefix(
                text
            )

        if (
            commandEnd >
            0
        ) {

            builder.addStyle(
                style =
                    SpanStyle(
                        color =
                            LauncherCommandRed,
                        fontSize =
                            16.sp,
                        fontWeight =
                            FontWeight.Bold
                    ),
                start =
                    0,
                end =
                    commandEnd
            )
        }

        TransformedText(
            builder.toAnnotatedString(),
            OffsetMapping.Identity
        )
    }


internal data class LauncherCommandContextText(
    val action: String,
    val detail: String? = null
)

internal fun extractTemporalHint(
    value: String
): String? {
    val normalized =
        value
            .trim()
            .lowercase()

    if (
        normalized.isBlank()
    ) {
        return null
    }

    val time =
        "\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?"

    val weekday =
        "(?:mon(?:day)?|tue(?:sday)?|wed(?:nesday)?|thu(?:rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)"

    val month =
        "(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)"

    val patterns =
        listOf(
            Regex(
                "\\b(?:today|tomorrow|tonight)(?:\\s+(?:at\\s+)?$time)?\\b"
            ),
            Regex(
                "\\b$weekday(?:\\s*,?\\s*(?:at\\s+)?$time)?\\b"
            ),
            Regex(
                "\\b$month\\s+\\d{1,2}(?:\\s*,?\\s*(?:at\\s+)?$time)?\\b"
            ),
            Regex(
                "\\b\\d{1,2}/\\d{1,2}(?:/\\d{2,4})?(?:\\s+(?:at\\s+)?$time)?\\b"
            ),
            Regex(
                "\\bat\\s+$time\\b"
            )
        )

    val matched =
        patterns
            .firstNotNullOfOrNull { pattern ->
                pattern
                    .find(
                        normalized
                    )
                    ?.value
                    ?.trim()
            }

    if (
        matched == null
    ) {
        return null
    }

    val matchedAlreadyHasTime =
        Regex(
            "\\b\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)\\b"
        ).containsMatchIn(
            matched
        ) ||
                Regex(
                    "\\b\\d{1,2}:\\d{2}\\b"
                ).containsMatchIn(
                    matched
                )

    if (
        matchedAlreadyHasTime
    ) {
        return matched
    }

    val remainder =
        normalized
            .substringAfter(
                matched,
                missingDelimiterValue = ""
            )
            .trimStart()

    val trailingTime =
        Regex(
            "^(?:at\\s+)?\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)\\b"
        )
            .find(
                remainder
            )
            ?.value
            ?.trim()

    return if (
        trailingTime != null
    ) {
        "$matched $trailingTime"
    } else {
        matched
    }
}

internal fun launcherCommandContext(
    commandText: String,
    contactQuery: String
): LauncherCommandContextText {
    val raw =
        commandText.trim()

    if (
        raw.isBlank()
    ) {
        return LauncherCommandContextText(
            action = "command"
        )
    }

    return when {
        raw.startsWith("@") ->
            LauncherCommandContextText(
                action = "message",
                detail =
                    contactQuery
                        .takeIf {
                            it.isNotBlank()
                        }
            )

        raw.startsWith("/") ->
            LauncherCommandContextText(
                action = "call",
                detail =
                    contactQuery
                        .takeIf {
                            it.isNotBlank()
                        }
            )

        raw.startsWith("+") -> {
            val body =
                raw
                    .removePrefix("+")
                    .trim()

            LauncherCommandContextText(
                action = "add task",
                detail =
                    extractTemporalHint(
                        body
                    )
            )
        }

        raw.startsWith(":") -> {
            val body =
                raw
                    .removePrefix(":")
                    .trim()

            LauncherCommandContextText(
                action = "add event",
                detail =
                    extractTemporalHint(
                        body
                    )
            )
        }

        raw.startsWith("\"") -> {
            val noteRaw =
                raw
                    .removePrefix("\"")
                    .trimStart()

            val categoryCode =
                noteRaw
                    .firstOrNull()
                    ?.lowercaseChar()

            val explicitCategory =
                categoryCode in
                        setOf(
                            'p',
                            'w',
                            'i',
                            'j'
                        ) &&
                        (
                                noteRaw.length ==
                                        1 ||
                                        noteRaw
                                            .getOrNull(
                                                1
                                            )
                                            ?.isWhitespace() ==
                                        true
                                )

            val category =
                if (
                    explicitCategory
                ) {
                    when (
                        categoryCode
                    ) {
                        'w' ->
                            "work"

                        'i' ->
                            "ideas"

                        'j' ->
                            "journal"

                        else ->
                            "personal"
                    }
                } else {
                    "personal"
                }

            LauncherCommandContextText(
                action = "add note",
                detail = category
            )
        }

        raw.startsWith("!!") ->
            LauncherCommandContextText(
                action = "set alarm",
                detail =
                    raw
                        .removePrefix("!!")
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
            )

        raw.startsWith("!") ->
            LauncherCommandContextText(
                action = "set timer",
                detail =
                    raw
                        .removePrefix("!")
                        .trim()
                        .takeIf {
                            it.isNotBlank()
                        }
            )

        raw.startsWith("?") ->
            LauncherCommandContextText(
                action = "ask assistant"
            )

        raw.startsWith(".") ->
            LauncherCommandContextText(
                action = "press enter or type the app name"
            )

        raw.equals(
            "g",
            ignoreCase = true
        ) ->
            LauncherCommandContextText(
                action = "maps"
            )

        raw.equals(
            "go",
            ignoreCase = true
        ) ->
            LauncherCommandContextText(
                action = "maps"
            )

        raw.startsWith(
            "go ",
            ignoreCase = true
        ) ->
            LauncherCommandContextText(
                action = "maps",
                detail =
                    raw
                        .substring(
                            3
                        )
                        .trim()
                        .lowercase()
                        .takeIf {
                            it.isNotBlank()
                        }
            )

        raw.startsWith(
            "map ",
            ignoreCase = true
        ) ->
            LauncherCommandContextText(
                action = "maps",
                detail =
                    raw
                        .substring(
                            4
                        )
                        .trim()
                        .lowercase()
                        .takeIf {
                            it.isNotBlank()
                        }
            )

        raw.startsWith(
            "maps ",
            ignoreCase = true
        ) ->
            LauncherCommandContextText(
                action = "maps",
                detail =
                    raw
                        .substring(
                            5
                        )
                        .trim()
                        .lowercase()
                        .takeIf {
                            it.isNotBlank()
                        }
            )

        raw.startsWith(
            "email",
            ignoreCase = true
        ) ->
            LauncherCommandContextText(
                action = "compose mail"
            )

        raw.equals(
            "mail",
            ignoreCase = true
        ) ||
                raw.equals(
                    "e",
                    ignoreCase = true
                ) ->
            LauncherCommandContextText(
                action = "open mail"
            )

        raw.startsWith(
            "calendarsetup",
            ignoreCase = true
        ) ->
            LauncherCommandContextText(
                action = "calendar setup"
            )

        raw.startsWith(
            "weather",
            ignoreCase = true
        ) ->
            LauncherCommandContextText(
                action = "weather"
            )

        raw.startsWith(
            "usage",
            ignoreCase = true
        ) ->
            LauncherCommandContextText(
                action = "usage"
            )

        raw.startsWith(
            "gemini",
            ignoreCase = true
        ) ->
            LauncherCommandContextText(
                action = "assistant setup"
            )

        raw.equals(
            "messages",
            ignoreCase = true
        ) ->
            LauncherCommandContextText(
                action = "open messages"
            )

        raw.equals(
            "phone",
            ignoreCase = true
        ) ->
            LauncherCommandContextText(
                action = "open phone"
            )

        raw.equals(
            "maps",
            ignoreCase = true
        ) ->
            LauncherCommandContextText(
                action = "open maps"
            )

        raw.equals(
            "chrome",
            ignoreCase = true
        ) ->
            LauncherCommandContextText(
                action = "open browser"
            )

        raw.equals(
            "settings",
            ignoreCase = true
        ) ->
            LauncherCommandContextText(
                action = "open settings"
            )

        else ->
            LauncherCommandContextText(
                action = "command"
            )
    }
}

@Composable
internal fun RecentSearchesCard(
    searches: List<String>,
    onSearchSelected: (String) -> Unit,
    onRemoveSearch: (String) -> Unit,
    onClearAll: () -> Unit
) {

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    SurfaceBlack.copy(
                        alpha =
                            0.96f
                    ),
                    RoundedCornerShape(
                        12.dp
                    )
                )
                .border(
                    width =
                        0.8.dp,
                    color =
                        BorderGray,
                    shape =
                        RoundedCornerShape(
                            12.dp
                        )
                )
                .padding(
                    horizontal =
                        8.dp,
                    vertical =
                        7.dp
                )
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    "recent searches",
                color =
                    TertiaryText,
                fontSize =
                    7.5.sp,
                lineHeight =
                    8.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Medium,
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            Text(
                text =
                    "clear all",
                color =
                    AccentOrange,
                fontSize =
                    7.5.sp,
                lineHeight =
                    8.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Medium,
                modifier =
                    Modifier.clickable {
                        onClearAll()
                    }
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    5.dp
                )
        )

        searches
            .take(
                3
            )
            .forEachIndexed {
                    index,
                    query ->

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(
                                28.dp
                            )
                            .background(
                                InputSurface.copy(
                                    alpha =
                                        0.38f
                                ),
                                RoundedCornerShape(
                                    7.dp
                                )
                            )
                            .clickable {

                                onSearchSelected(
                                    query
                                )
                            }
                            .padding(
                                start =
                                    7.dp,
                                end =
                                    4.dp
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            "↻",
                        color =
                            TertiaryText,
                        fontSize =
                            9.sp,
                        fontFamily =
                            InterfaceFont
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                6.dp
                            )
                    )

                    Text(
                        text =
                            query,
                        color =
                            PrimaryText,
                        fontSize =
                            8.3.sp,
                        lineHeight =
                            9.sp,
                        fontFamily =
                            InterfaceFont,
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
                            "×",
                        color =
                            SecondaryText,
                        fontSize =
                            13.sp,
                        lineHeight =
                            13.sp,
                        fontFamily =
                            InterfaceFont,
                        fontWeight =
                            FontWeight.Medium,
                        textAlign =
                            TextAlign.Center,
                        modifier =
                            Modifier
                                .size(
                                    24.dp
                                )
                                .clickable {

                                    onRemoveSearch(
                                        query
                                    )
                                }
                                .padding(
                                    top =
                                        2.dp
                                )
                    )
                }

                if (
                    index <
                    searches.take(
                        3
                    ).lastIndex
                ) {

                    Spacer(
                        modifier =
                            Modifier.height(
                                4.dp
                            )
                    )
                }
            }
    }
}


internal data class CommandHintItem(
    val command: String,
    val label: String
)


internal data class WordCommandHintItem(
    val display: String,
    val value: String,
    val label: String
)


@Composable
internal fun CommandHintsCard(
    commandText: String,
    onCommandSelected: (String) -> Unit
) {

    var page by remember {
        mutableIntStateOf(
            0
        )
    }

    var dragOffsetPx by remember {
        mutableFloatStateOf(
            0f
        )
    }

    val scope =
        rememberCoroutineScope()

    val trimmed =
        commandText.trimStart()

    val activeCommand =
        when {
            trimmed.startsWith("!!") ->
                "!!"

            trimmed.startsWith("@") ->
                "@"

            trimmed.startsWith("/") ->
                "/"

            trimmed.startsWith(":") ->
                ":"

            trimmed.startsWith("+") ->
                "+"

            trimmed.startsWith("?") ->
                "?"

            trimmed.startsWith("\"") ->
                "\""

            trimmed.startsWith("!") ->
                "!"

            trimmed.startsWith(".") ->
                "."

            else ->
                null
        }

    val activeWordCommand =
        when {

            trimmed.startsWith(
                "calendarsetup",
                ignoreCase =
                    true
            ) ->
                "calendarsetup"

            trimmed.startsWith(
                "weathersetup",
                ignoreCase =
                    true
            ) ->
                "weathersetup"

            trimmed.startsWith(
                "usagesetup",
                ignoreCase =
                    true
            ) ->
                "usagesetup"

            trimmed.startsWith(
                "geminisetup",
                ignoreCase =
                    true
            ) ->
                "geminisetup"

            trimmed.startsWith(
                "todoistsetup",
                ignoreCase =
                    true
            ) ->
                "todoistsetup"

            trimmed.startsWith(
                "settings",
                ignoreCase =
                    true
            ) ->
                "settings"

            trimmed.startsWith(
                "bug",
                ignoreCase =
                    true
            ) ->
                "bug"

            trimmed.startsWith(
                "email",
                ignoreCase =
                    true
            ) ->
                "email "

            trimmed.startsWith(
                "go",
                ignoreCase =
                    true
            ) ->
                "go "

            else ->
                null
        }

    val symbolRows =
        listOf(
            listOf(
                CommandHintItem(
                    command = "@",
                    label = "message"
                ),
                CommandHintItem(
                    command = "/",
                    label = "call"
                ),
                CommandHintItem(
                    command = "+",
                    label = "to do"
                )
            ),
            listOf(
                CommandHintItem(
                    command = "\"",
                    label = "note"
                ),
                CommandHintItem(
                    command = ":",
                    label = "event"
                ),
                CommandHintItem(
                    command = "?",
                    label = "ai assist"
                )
            ),
            listOf(
                CommandHintItem(
                    command = ".",
                    label = "apps"
                ),
                CommandHintItem(
                    command = "!",
                    label = "timer"
                ),
                CommandHintItem(
                    command = "!!",
                    label = "alarm"
                )
            )
        )

    val wordRows =
        listOf(
            listOf(
                WordCommandHintItem(
                    display = "go",
                    value = "go ",
                    label = "maps"
                ),
                WordCommandHintItem(
                    display = "email",
                    value = "email ",
                    label = "compose"
                ),
                WordCommandHintItem(
                    display = "settings",
                    value = "settings",
                    label = "device"
                )
            ),
            listOf(
                WordCommandHintItem(
                    display = "calendarsetup",
                    value = "calendarsetup",
                    label = "calendar"
                ),
                WordCommandHintItem(
                    display = "weathersetup",
                    value = "weathersetup",
                    label = "weather"
                ),
                WordCommandHintItem(
                    display = "usagesetup",
                    value = "usagesetup",
                    label = "usage"
                )
            ),
            listOf(
                WordCommandHintItem(
                    display = "geminisetup",
                    value = "geminisetup",
                    label = "assistant"
                ),
                WordCommandHintItem(
                    display = "todoistsetup",
                    value = "todoistsetup",
                    label = "todoist"
                ),
                WordCommandHintItem(
                    display = "bug",
                    value = "bug",
                    label = "report"
                )
            )
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    SurfaceBlack.copy(
                        alpha =
                            0.96f
                    ),
                    RoundedCornerShape(
                        12.dp
                    )
                )
                .border(
                    width =
                        0.8.dp,
                    color =
                        BorderGray,
                    shape =
                        RoundedCornerShape(
                            12.dp
                        )
                )
                .padding(
                    horizontal =
                        8.dp,
                    vertical =
                        7.dp
                )
    ) {

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    if (
                        page ==
                        0
                    ) {
                        "commands"
                    } else {
                        "word commands"
                    },
                color =
                    TertiaryText,
                fontSize =
                    7.5.sp,
                lineHeight =
                    8.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Medium,
                modifier =
                    Modifier.weight(
                        1f
                    )
            )

            Text(
                text =
                    if (
                        page ==
                        0
                    ) {
                        ">"
                    } else {
                        "<"
                    },
                color =
                    SecondaryText,
                fontSize =
                    11.sp,
                lineHeight =
                    11.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Bold,
                modifier =
                    Modifier
                        .width(
                            22.dp
                        )
                        .clickable {

                            page =
                                if (
                                    page ==
                                    0
                                ) {
                                    1
                                } else {
                                    0
                                }

                            dragOffsetPx =
                                0f
                        }
                        .padding(
                            vertical =
                                1.dp
                        ),
                textAlign =
                    TextAlign.Center
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    5.dp
                )
        )

        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(
                        92.dp
                    )
        ) {

            val pageWidthPx =
                constraints
                    .maxWidth
                    .toFloat()
                    .coerceAtLeast(
                        1f
                    )

            val dragState =
                rememberDraggableState {
                        delta ->

                    val proposed =
                        dragOffsetPx +
                                delta

                    dragOffsetPx =
                        when (
                            page
                        ) {

                            0 ->
                                proposed.coerceIn(
                                    -pageWidthPx,
                                    0f
                                )

                            else ->
                                proposed.coerceIn(
                                    0f,
                                    pageWidthPx
                                )
                        }
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        /*
                         * Both pages are always composed and simply
                         * translated, so the one off screen has to be
                         * clipped or it spills past the card edge.
                         */
                        .clipToBounds()
                        .draggable(
                            state =
                                dragState,
                            orientation =
                                Orientation.Horizontal,
                            onDragStarted = {

                                dragOffsetPx =
                                    0f
                            },
                            onDragStopped = {
                                    velocity ->

                                val threshold =
                                    pageWidthPx *
                                            0.22f

                                val moveToNext =
                                    page ==
                                            0 &&
                                            (
                                                    dragOffsetPx <=
                                                            -threshold ||
                                                            velocity <
                                                            -900f
                                                    )

                                val moveToPrevious =
                                    page ==
                                            1 &&
                                            (
                                                    dragOffsetPx >=
                                                            threshold ||
                                                            velocity >
                                                            900f
                                                    )

                                val targetOffset =
                                    when {

                                        moveToNext ->
                                            -pageWidthPx

                                        moveToPrevious ->
                                            pageWidthPx

                                        else ->
                                            0f
                                    }

                                scope.launch {

                                    animate(
                                        initialValue =
                                            dragOffsetPx,
                                        targetValue =
                                            targetOffset,
                                        animationSpec =
                                            tween(
                                                durationMillis =
                                                    if (
                                                        ReduceLauncherMotion
                                                    ) {
                                                        0
                                                    } else {
                                                        170
                                                    }
                                            )
                                    ) {
                                            value,
                                            _ ->

                                        dragOffsetPx =
                                            value
                                    }

                                    when {

                                        moveToNext ->
                                            page =
                                                1

                                        moveToPrevious ->
                                            page =
                                                0
                                    }

                                    dragOffsetPx =
                                        0f
                                }
                            }
                        )
            ) {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {

                                translationX =
                                    (
                                            -page *
                                                    pageWidthPx
                                            ) +
                                            dragOffsetPx
                            }
                ) {

                    SymbolCommandHintsPage(
                        rows =
                            symbolRows,
                        activeCommand =
                            activeCommand,
                        onCommandSelected =
                            onCommandSelected
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {

                                translationX =
                                    (
                                            (
                                                    1 -
                                                            page
                                                    ) *
                                                    pageWidthPx
                                            ) +
                                            dragOffsetPx
                            }
                ) {

                    WordCommandHintsPage(
                        rows =
                            wordRows,
                        activeWordCommand =
                            activeWordCommand,
                        onCommandSelected =
                            onCommandSelected
                    )
                }
            }
        }
    }
}


@Composable
private fun SymbolCommandHintsPage(
    rows: List<List<CommandHintItem>>,
    activeCommand: String?,
    onCommandSelected: (String) -> Unit
) {

    Column {

        rows.forEachIndexed {
                rowIndex,
                row ->

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        5.dp
                    )
            ) {

                row.forEach {
                        item ->

                    val selected =
                        activeCommand ==
                                item.command

                    Row(
                        modifier =
                            Modifier
                                .weight(
                                    1f
                                )
                                .height(
                                    24.dp
                                )
                                .background(
                                    if (
                                        selected
                                    ) {
                                        HomeCommandSurface.copy(
                                            alpha =
                                                0.96f
                                        )
                                    } else {
                                        InputSurface.copy(
                                            alpha =
                                                0.42f
                                        )
                                    },
                                    RoundedCornerShape(
                                        7.dp
                                    )
                                )
                                .border(
                                    width =
                                        if (
                                            selected
                                        ) {
                                            0.8.dp
                                        } else {
                                            0.5.dp
                                        },
                                    color =
                                        if (
                                            selected
                                        ) {
                                            AccentOrange.copy(
                                                alpha =
                                                    0.70f
                                            )
                                        } else {
                                            BorderGray.copy(
                                                alpha =
                                                    0.70f
                                            )
                                        },
                                    shape =
                                        RoundedCornerShape(
                                            7.dp
                                        )
                                )
                                .clickable {

                                    onCommandSelected(
                                        item.command
                                    )
                                }
                                .padding(
                                    horizontal =
                                        6.dp
                                ),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text =
                                item.command,
                            color =
                                if (
                                    selected
                                ) {
                                    AccentOrange
                                } else {
                                    TertiaryText
                                },
                            fontSize =
                                9.5.sp,
                            lineHeight =
                                10.sp,
                            fontFamily =
                                InterfaceFont,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.width(
                                    5.dp
                                )
                        )

                        Text(
                            text =
                                item.label,
                            color =
                                if (
                                    selected
                                ) {
                                    PrimaryText
                                } else {
                                    SecondaryText
                                },
                            fontSize =
                                7.8.sp,
                            lineHeight =
                                8.5.sp,
                            fontFamily =
                                InterfaceFont,
                            fontWeight =
                                if (
                                    selected
                                ) {
                                    FontWeight.Medium
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

            if (
                rowIndex <
                rows.lastIndex
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )
            }
        }
    }
}


@Composable
private fun WordCommandHintsPage(
    rows: List<List<WordCommandHintItem>>,
    activeWordCommand: String?,
    onCommandSelected: (String) -> Unit
) {

    Column {

        rows.forEachIndexed {
                rowIndex,
                row ->

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        5.dp
                    )
            ) {

                row.forEach {
                        item ->

                    val selected =
                        activeWordCommand ==
                                item.value

                    Column(
                        modifier =
                            Modifier
                                .weight(
                                    1f
                                )
                                .height(
                                    28.dp
                                )
                                .background(
                                    if (
                                        selected
                                    ) {
                                        HomeCommandSurface.copy(
                                            alpha =
                                                0.96f
                                        )
                                    } else {
                                        InputSurface.copy(
                                            alpha =
                                                0.42f
                                        )
                                    },
                                    RoundedCornerShape(
                                        7.dp
                                    )
                                )
                                .border(
                                    width =
                                        if (
                                            selected
                                        ) {
                                            0.8.dp
                                        } else {
                                            0.5.dp
                                        },
                                    color =
                                        if (
                                            selected
                                        ) {
                                            AccentOrange.copy(
                                                alpha =
                                                    0.70f
                                            )
                                        } else {
                                            BorderGray.copy(
                                                alpha =
                                                    0.70f
                                            )
                                        },
                                    shape =
                                        RoundedCornerShape(
                                            7.dp
                                        )
                                )
                                .clickable {

                                    onCommandSelected(
                                        item.value
                                    )
                                }
                                .padding(
                                    horizontal =
                                        5.dp,
                                    vertical =
                                        3.dp
                                ),
                        verticalArrangement =
                            Arrangement.Center
                    ) {

                        /*
                         * One line only, styled to match the symbol
                         * page's labels. The descriptions underneath
                         * never fitted the button height and told the
                         * reader little the command name did not.
                         */
                        Text(
                            text =
                                item.display,
                            color =
                                if (
                                    selected
                                ) {
                                    PrimaryText
                                } else {
                                    SecondaryText
                                },
                            fontSize =
                                7.8.sp,
                            lineHeight =
                                8.5.sp,
                            fontFamily =
                                InterfaceFont,
                            fontWeight =
                                if (
                                    selected
                                ) {
                                    FontWeight.Medium
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

            if (
                rowIndex <
                rows.lastIndex
            ) {

                Spacer(
                    modifier =
                        Modifier.height(
                            4.dp
                        )
                )
            }
        }
    }
}


@Composable
internal fun CommandContextPanel(
    commandText: String,
    groupRecipients: List<String> = emptyList(),
    groupMatches: List<Contact> = emptyList(),
    selectedGroupIndex: Int = 0,
    onChooseGroupContact: (Contact) -> Unit = {},
    contactQuery: String,
    contacts: List<Contact>,
    selectedContactIndex: Int,
    onChooseContact: (Contact) -> Unit,
    apps: List<LauncherAppEntry> = emptyList(),
    selectedAppIndex: Int = 0,
    onChooseApp: (LauncherAppEntry) -> Unit = {}
) {
    val context =
        launcherCommandContext(
            commandText = commandText,
            contactQuery = contactQuery
        )

    val lightModeContext =
        BackgroundBlack ==
                Color(
                    0xFFFAF9F6
                )

    val contextPrimaryColor =
        if (
            lightModeContext
        ) {
            Color(
                0xFFD4D4D4
            )
        } else {
            PrimaryText
        }

    val contextSecondaryColor =
        if (
            lightModeContext
        ) {
            Color(
                0xFFBBBBBB
            )
        } else {
            SecondaryText
        }

    val contextTertiaryColor =
        if (
            lightModeContext
        ) {
            Color(
                0xFFA6A6A6
            )
        } else {
            TertiaryText
        }

    Column(
        modifier =
            Modifier.fillMaxWidth()
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 5.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text =
                    context.action,
                color =
                    contextPrimaryColor,
                fontSize =
                    9.5.sp,
                lineHeight =
                    11.sp,
                fontFamily =
                    InterfaceFont,
                fontWeight =
                    FontWeight.Medium,
                maxLines =
                    1
            )

            context.detail
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                        detail ->

                    Spacer(
                        modifier =
                            Modifier.width(
                                6.dp
                            )
                    )

                    Text(
                        text =
                            "→",
                        color =
                            contextTertiaryColor,
                        fontSize =
                            9.sp,
                        fontFamily =
                            InterfaceFont
                    )

                    Spacer(
                        modifier =
                            Modifier.width(
                                6.dp
                            )
                    )

                    Text(
                        text =
                            detail.lowercase(),
                        color =
                            contextSecondaryColor,
                        fontSize =
                            9.5.sp,
                        lineHeight =
                            11.sp,
                        fontFamily =
                            InterfaceFont,
                        maxLines =
                            1,
                        overflow =
                            TextOverflow.Ellipsis
                    )
                }
        }

        if (
            contacts.isNotEmpty()
        ) {
            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )

            contacts.forEachIndexed {
                    index,
                    contact ->

                val selected =
                    index ==
                            selectedContactIndex

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            /*
                             * A minimum rather than a fixed height:
                             * the name and number together exceed
                             * 27.dp in Poppins, which clipped the
                             * number off the bottom.
                             */
                            .heightIn(
                                min = 27.dp
                            )
                            .background(
                                if (
                                    selected
                                ) {
                                    HomeCommandSurface.copy(
                                        alpha = 0.78f
                                    )
                                } else {
                                    Color.Transparent
                                },
                                RoundedCornerShape(
                                    7.dp
                                )
                            )
                            .clickable {
                                onChooseContact(
                                    contact
                                )
                            }
                            .padding(
                                horizontal = 6.dp,
                                vertical = 3.dp
                            ),
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
                            9.sp,
                        fontFamily =
                            InterfaceFont,
                        fontWeight =
                            FontWeight.Bold,
                        modifier =
                            Modifier.width(
                                14.dp
                            )
                    )

                    Column(
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    ) {
                        Text(
                            text =
                                contact.name.lowercase(),
                            color =
                                PrimaryText,
                            fontSize =
                                9.5.sp,
                            lineHeight =
                                10.sp,
                            fontFamily =
                                InterfaceFont,
                            maxLines =
                                1,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        Text(
                            text =
                                contact.phoneNumber,
                            color =
                                TertiaryText,
                            fontSize =
                                6.8.sp,
                            lineHeight =
                                7.5.sp,
                            fontFamily =
                                InterfaceFont,
                            maxLines =
                                1
                        )
                    }
                }
            }
        }

        /*
         * Group compose: who is already on the list, then who else
         * can be added.
         */
        if (
            groupRecipients.isNotEmpty()
        ) {

            Text(
                text =
                    "group · " +
                            groupRecipients.joinToString(
                                separator = ", "
                            ) {
                                it.substringBefore(" ")
                            },
                color =
                    AccentOrange,
                fontSize =
                    9.sp,
                fontFamily =
                    InterfaceFont,
                maxLines =
                    2,
                overflow =
                    TextOverflow.Ellipsis,
                modifier =
                    Modifier
                        .padding(
                            horizontal = 6.dp,
                            vertical = 3.dp
                        )
            )

            if (
                groupRecipients.size >= 2
            ) {

                Text(
                    text =
                        "enter to open the thread",
                    color =
                        TertiaryText,
                    fontSize =
                        8.sp,
                    fontFamily =
                        InterfaceFont,
                    modifier =
                        Modifier
                            .padding(
                                horizontal = 6.dp
                            )
                )
            }
        }

        if (
            groupMatches.isNotEmpty()
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )

            groupMatches.forEachIndexed {
                    index,
                    contact ->

                InlineContactRow(
                    contact =
                        contact,
                    selected =
                        index ==
                                selectedGroupIndex,
                    onClick = {
                        onChooseGroupContact(
                            contact
                        )
                    }
                )
            }
        }

        /*
         * App matches for a "." query. Same row treatment as the
         * contact suggestions so both feel like one mechanism.
         */
        if (
            apps.isNotEmpty()
        ) {
            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )

            apps.forEachIndexed {
                    index,
                    app ->

                val selected =
                    index ==
                            selectedAppIndex

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            /*
                             * A minimum rather than a fixed height:
                             * the name and number together exceed
                             * 27.dp in Poppins, which clipped the
                             * number off the bottom.
                             */
                            .heightIn(
                                min = 27.dp
                            )
                            .background(
                                if (
                                    selected
                                ) {
                                    HomeCommandSurface.copy(
                                        alpha = 0.78f
                                    )
                                } else {
                                    Color.Transparent
                                },
                                RoundedCornerShape(
                                    7.dp
                                )
                            )
                            .clickable {
                                onChooseApp(
                                    app
                                )
                            }
                            .padding(
                                horizontal = 6.dp,
                                vertical = 3.dp
                            ),
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
                            9.sp,
                        fontFamily =
                            InterfaceFont,
                        fontWeight =
                            FontWeight.Bold,
                        modifier =
                            Modifier.width(
                                14.dp
                            )
                    )

                    Text(
                        text =
                            app.label.lowercase(),
                        color =
                            PrimaryText,
                        fontSize =
                            9.5.sp,
                        lineHeight =
                            10.sp,
                        fontFamily =
                            InterfaceFont,
                        maxLines =
                            1,
                        overflow =
                            TextOverflow.Ellipsis,
                        modifier =
                            Modifier.weight(
                                1f
                            )
                    )

                    val appIcon =
                        rememberAppIconBitmap(
                            app.packageName
                        )

                    if (
                        appIcon != null
                    ) {

                        Spacer(
                            modifier =
                                Modifier.width(
                                    6.dp
                                )
                        )

                        Image(
                            bitmap =
                                appIcon,
                            contentDescription =
                                app.label,
                            modifier =
                                Modifier.size(
                                    16.dp
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommandBar(
    commandText: String,
    onCommandTextChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onSubmit: () -> Unit,
    onPromptClick: () -> Unit,
    hasInlineSuggestions: Boolean,
    onNavigateInlineSuggestion: (Int) -> Unit,
    onChooseInlineSuggestion: () -> Unit,
    visible: Boolean
) {

    var commandFieldValue by
    remember {
        mutableStateOf(
            TextFieldValue(
                text =
                    commandText,
                selection =
                    TextRange(
                        commandText.length
                    )
            )
        )
    }

    LaunchedEffect(
        commandText
    ) {

        if (
            commandFieldValue.text !=
            commandText
        ) {

            commandFieldValue =
                TextFieldValue(
                    text =
                        commandText,
                    selection =
                        TextRange(
                            commandText.length
                        )
                )
        }
    }

    val promptPulseTransition =
        rememberInfiniteTransition(
            label = "commandPromptPulse"
        )

    val promptScale by
    promptPulseTransition.animateFloat(
        initialValue =
            if (ReduceLauncherMotion) 1f else 0.96f,
        targetValue =
            if (ReduceLauncherMotion) 1f else 1.12f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(
                    durationMillis = 850
                ),
                repeatMode = RepeatMode.Reverse
            ),
        label = "commandPromptScale"
    )

    Box(
        modifier =
            if (
                visible
            ) {
                Modifier
                    .fillMaxWidth()
                    .height(
                        52.dp
                    )
            } else {
                Modifier.size(
                    1.dp
                )
            }
    ) {

        if (
            visible
        ) {

            /*
             * Soft accent-colored halo behind the prompt bar so the
             * command field feels like it is lifting off the screen.
             * This uses the current launcher accent color, so it works
             * in both light and dark mode and stays in sync with the
             * user's Appearance selection.
             */
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .padding(
                            horizontal = 2.dp,
                            vertical = 3.dp
                        )
                        .blur(
                            10.dp
                        )
                        .background(
                            AccentOrange.copy(
                                alpha = 0.20f
                            ),
                            RoundedCornerShape(
                                13.dp
                            )
                        )
            )

            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .padding(
                            horizontal = 1.dp,
                            vertical = 2.dp
                        )
                        .background(
                            AccentOrange.copy(
                                alpha = 0.08f
                            ),
                            RoundedCornerShape(
                                12.dp
                            )
                        )
                        .border(
                            1.1.dp,
                            AccentOrange.copy(
                                alpha = 0.34f
                            ),
                            RoundedCornerShape(
                                12.dp
                            )
                        )
            )
        }

        Row(
            modifier =
                if (
                    visible
                ) {
                    Modifier
                        .fillMaxWidth()
                        .height(
                            44.dp
                        )
                        .align(
                            Alignment.Center
                        )
                        .background(
                            HomeCommandSurface,
                            RoundedCornerShape(
                                10.dp
                            )
                        )
                        .border(
                            1.25.dp,
                            AccentOrange.copy(
                                alpha = 0.88f
                            ),
                            RoundedCornerShape(
                                10.dp
                            )
                        )
                        .padding(
                            horizontal =
                                9.dp
                        )
                } else {
                    Modifier.size(
                        1.dp
                    )
                },
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            if (
                visible
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .width(
                                24.dp
                            ),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Text(
                        text =
                            ">",
                        color =
                            PrimaryText,
                        fontSize =
                            22.sp,
                        fontFamily =
                            InterfaceFont,
                        fontWeight =
                            FontWeight.Bold,
                        modifier =
                            Modifier.graphicsLayer {
                                scaleX = promptScale
                                scaleY = promptScale
                            }
                    )
                }

                Spacer(
                    modifier =
                        Modifier.width(
                            3.dp
                        )
                )
            }

            BasicTextField(
                value =
                    commandFieldValue,
                onValueChange = { incoming ->

                    sanitizeLauncherCommandInput(
                        incoming.text
                    )
                        ?.let { accepted ->

                            commandFieldValue =
                                if (
                                    accepted ==
                                    incoming.text
                                ) {

                                    incoming

                                } else {

                                    TextFieldValue(
                                        text =
                                            accepted,
                                        selection =
                                            TextRange(
                                                accepted.length
                                            )
                                    )
                                }

                            onCommandTextChange(
                                accepted
                            )
                        }
                },
                singleLine =
                    true,
                visualTransformation =
                    LauncherCommandVisualTransformation,
                textStyle =
                    TextStyle(
                        color =
                            PrimaryText,
                        fontSize =
                            14.sp,
                        fontFamily =
                            InterfaceFont,
                        fontWeight =
                            FontWeight.Medium
                    ),
                cursorBrush =
                    SolidColor(
                        if (
                            visible
                        ) {
                            AccentOrange
                        } else {
                            Color.Transparent
                        }
                    ),
                modifier =
                    if (
                        visible
                    ) {
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
                                        Key.DirectionDown -> {
                                            if (
                                                hasInlineSuggestions
                                            ) {
                                                onNavigateInlineSuggestion(
                                                    1
                                                )
                                                true
                                            } else {
                                                false
                                            }
                                        }

                                        Key.DirectionUp -> {
                                            if (
                                                hasInlineSuggestions
                                            ) {
                                                onNavigateInlineSuggestion(
                                                    -1
                                                )
                                                true
                                            } else {
                                                false
                                            }
                                        }

                                        Key.Enter -> {
                                            if (
                                                hasInlineSuggestions
                                            ) {
                                                onChooseInlineSuggestion()
                                            } else {
                                                onSubmit()
                                            }

                                            true
                                        }

                                        else ->
                                            false
                                    }
                                }
                            }
                    } else {
                        Modifier
                            .size(
                                1.dp
                            )
                            .focusRequester(
                                focusRequester
                            )
                            .onPreviewKeyEvent {
                                    event ->

                                if (
                                    event.key ==
                                    Key.Enter &&
                                    event.type ==
                                    KeyEventType.KeyDown
                                ) {

                                    onSubmit()

                                    true

                                } else {

                                    false
                                }
                            }
                    }
            )
        }
    }
}

@Composable
internal fun CommandHelpRow(
    command: String,
    label: String
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 2.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text = command,
            color = LauncherCommandRed,
            fontSize = 11.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(
                34.dp
            )
        )

        Text(
            text = label,
            color = PrimaryText,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontFamily = InterfaceFont,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InlineContactRow(
    contact: Contact,
    selected: Boolean,
    onClick: () -> Unit
) {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(
                    min = 27.dp
                )
                .background(
                    if (
                        selected
                    ) {
                        HomeCommandSurface.copy(
                            alpha = 0.78f
                        )
                    } else {
                        Color.Transparent
                    },
                    RoundedCornerShape(
                        7.dp
                    )
                )
                .clickable {
                    onClick()
                }
                .padding(
                    horizontal = 6.dp,
                    vertical = 3.dp
                ),
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
                9.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.Bold,
            modifier =
                Modifier.width(
                    14.dp
                )
        )

        Column(
            modifier =
                Modifier.weight(
                    1f
                )
        ) {

            Text(
                text =
                    contact.name.lowercase(),
                color =
                    PrimaryText,
                fontSize =
                    9.5.sp,
                lineHeight =
                    10.sp,
                fontFamily =
                    InterfaceFont,
                maxLines =
                    1,
                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                text =
                    contact.phoneNumber,
                color =
                    TertiaryText,
                fontSize =
                    6.8.sp,
                lineHeight =
                    7.5.sp,
                fontFamily =
                    InterfaceFont,
                maxLines =
                    1
            )
        }
    }
}


@Composable
private fun SearchTargetChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Box(
        modifier =
            Modifier
                .height(24.dp)
                .background(
                    if (selected) AccentOrange else Color.Transparent,
                    RoundedCornerShape(8.dp)
                )
                .border(
                    0.75.dp,
                    if (selected) AccentOrange else BorderGray,
                    RoundedCornerShape(8.dp)
                )
                .clickable { onClick() }
                .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = label,
            color = if (selected) BackgroundBlack else SecondaryText,
            fontSize = 8.5.sp,
            lineHeight = 8.5.sp,
            fontFamily = InterfaceFont,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}