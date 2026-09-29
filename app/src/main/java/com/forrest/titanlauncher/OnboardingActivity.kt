package com.forrest.titanlauncher

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.forrest.titanlauncher.ai.GeminiApiKeyStore
import com.forrest.titanlauncher.settings.LauncherAccent
import com.forrest.titanlauncher.settings.LauncherInterfaceFont
import com.forrest.titanlauncher.settings.LauncherSettings
import com.forrest.titanlauncher.settings.LauncherSettingsStore
import com.forrest.titanlauncher.settings.LauncherTextColor
import com.forrest.titanlauncher.settings.LauncherThemeMode
import com.forrest.titanlauncher.messages.SmsRoleManager
import com.forrest.titanlauncher.todoist.GoogleTasksAuthManager
import com.forrest.titanlauncher.todoist.TaskProvider
import com.forrest.titanlauncher.usage.UsageStatsRepository
import com.forrest.titanlauncher.todoist.TodoistTokenStore
import com.forrest.titanlauncher.ui.theme.TitanLauncherTheme
import com.forrest.titanlauncher.weather.WeatherRepository
import com.forrest.titanlauncher.weather.WeatherSnapshot

class OnboardingActivity : ComponentActivity() {

    /*
     * Every time we return from an Android system screen
     * (Home chooser, SMS chooser, permissions/settings, etc.)
     * this value changes.
     *
     * Compose observes it and re-checks the real Android state.
     */
    private val resumeRefreshState =
        mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        if (onboardingComplete()) {
            openLauncher()
            return
        }

        hideSystemBars()

        setContent {
            TitanLauncherTheme {
                PromptLauncherOnboarding(
                    resumeRefresh =
                        resumeRefreshState.intValue,
                    initialStep =
                        getSavedOnboardingStep(),
                    onStepChanged = {
                            step ->

                        saveOnboardingStep(
                            step
                        )
                    },
                    onFinish = {
                        markOnboardingComplete()
                        openLauncher()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        resumeRefreshState.intValue +=
            1

        hideSystemBars()
    }

    private fun onboardingComplete(): Boolean {
        return getSharedPreferences(
            ONBOARDING_PREFS,
            Context.MODE_PRIVATE
        ).getBoolean(
            KEY_ONBOARDING_COMPLETE,
            false
        )
    }

    private fun getSavedOnboardingStep(): Int {
        return getSharedPreferences(
            ONBOARDING_PREFS,
            Context.MODE_PRIVATE
        )
            .getInt(
                KEY_ONBOARDING_STEP,
                0
            )
            .coerceIn(
                0,
                6
            )
    }

    private fun saveOnboardingStep(
        step: Int
    ) {
        getSharedPreferences(
            ONBOARDING_PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putInt(
                KEY_ONBOARDING_STEP,
                step.coerceIn(
                    0,
                    6
                )
            )
            .apply()
    }

    private fun markOnboardingComplete() {
        getSharedPreferences(
            ONBOARDING_PREFS,
            Context.MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                KEY_ONBOARDING_COMPLETE,
                true
            )
            .remove(
                KEY_ONBOARDING_STEP
            )
            .apply()
    }

    private fun openLauncher() {
        startActivity(
            Intent(
                this,
                MainActivity::class.java
            ).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
        )

        finish()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

        val controller =
            WindowCompat.getInsetsController(
                window,
                window.decorView
            )

        controller.hide(
            WindowInsetsCompat.Type.systemBars()
        )

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat
                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    companion object {
        private const val ONBOARDING_PREFS =
            "prompt_launcher_onboarding"

        private const val KEY_ONBOARDING_COMPLETE =
            "onboarding_complete"

        private const val KEY_ONBOARDING_STEP =
            "onboarding_step"
    }
}

@Composable
private fun PromptLauncherOnboarding(
    resumeRefresh: Int,
    initialStep: Int,
    onStepChanged: (Int) -> Unit,
    onFinish: () -> Unit
) {
    val context =
        LocalContext.current

    val activity =
        context as Activity

    val displayedVersion =
        remember(context) {
            getPromptLauncherVersionName(
                context
            )
        }

    val taskStore =
        remember {
            TodoistTokenStore(
                context
            )
        }

    val googleTasksAuth =
        remember {
            GoogleTasksAuthManager(
                activity
            )
        }

    val geminiStore =
        remember {
            GeminiApiKeyStore(
                context
            )
        }

    val launcherSettingsStore =
        remember {
            LauncherSettingsStore(
                context
            )
        }

    /*
     * Applied as it is edited, so the choices on the appearance step
     * are what the launcher opens with.
     */
    var launcherSettings by remember {
        mutableStateOf(
            launcherSettingsStore.load()
        )
    }

    val smsRoleManager =
        remember {
            SmsRoleManager(
                context
            )
        }

    val weatherRepository =
        remember {
            WeatherRepository(
                context
            )
        }

    var step by remember(
        initialStep
    ) {
        mutableIntStateOf(
            initialStep.coerceIn(
                0,
                6
            )
        )
    }

    LaunchedEffect(
        step
    ) {
        onStepChanged(
            step
        )
    }

    var permissionRefresh by remember {
        mutableIntStateOf(0)
    }

    var homeRefresh by remember {
        mutableIntStateOf(0)
    }

    var smsRefresh by remember {
        mutableIntStateOf(0)
    }

    var weatherRefresh by remember {
        mutableIntStateOf(0)
    }

    var permissionAttempted by remember {
        mutableStateOf(false)
    }

    var weatherChecking by remember {
        mutableStateOf(false)
    }

    /*
     * Usage Access can't use a system runtime dialog, so onboarding
     * shows its own prompt once, right after the Android prompts.
     */
    var showUsageAccessPrompt by remember {
        mutableStateOf(false)
    }

    var usageAccessPromptShown by remember {
        mutableStateOf(false)
    }

    var weatherCheckAttempted by remember {
        mutableStateOf(false)
    }

    var weatherSnapshot by remember {
        mutableStateOf<WeatherSnapshot?>(
            null
        )
    }

    var todoistToken by remember {
        mutableStateOf("")
    }

    var geminiApiKey by remember {
        mutableStateOf("")
    }

    var taskProvider by remember {
        mutableStateOf(
            taskStore.getTaskProvider()
        )
    }

    var googleTasksConnected by remember {
        mutableStateOf(
            taskStore.isGoogleTasksConnected()
        )
    }

    var taskSetupMessage by remember {
        mutableStateOf<String?>(null)
    }

    var googleTasksAuthorizing by remember {
        mutableStateOf(false)
    }

    var geminiSaved by remember {
        mutableStateOf(
            geminiStore.hasKey()
        )
    }

    /*
     * resumeRefresh is included in these remember keys.
     *
     * That forces a fresh Android permission/role check whenever
     * onboarding returns from a system screen.
     */
    val contactsReady =
        remember(
            permissionRefresh,
            resumeRefresh
        ) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        }

    val calendarReady =
        remember(
            permissionRefresh,
            resumeRefresh
        ) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        }

    val weatherPermissionReady =
        remember(
            permissionRefresh,
            resumeRefresh
        ) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

    /*
     * Usage Access powers the productivity dots. A fresh install
     * resets it, and without it every dot stays gray.
     */
    val usageAccessReady =
        remember(
            permissionRefresh,
            resumeRefresh
        ) {
            runCatching {
                UsageStatsRepository(
                    context
                ).hasUsageAccess()
            }
                .getOrDefault(
                    false
                )
        }

    val essentialPermissionsReady =
        contactsReady &&
                calendarReady

    val shouldRequestPermissions =
        !essentialPermissionsReady ||
                (
                        !weatherPermissionReady &&
                                !permissionAttempted
                        )

    val homeReady =
        remember(
            homeRefresh,
            resumeRefresh
        ) {
            promptLauncherIsHome(
                context
            )
        }

    /*
     * Retained but no longer part of onboarding. Prompt Launcher
     * works alongside whichever app owns SMS now, so asking for the
     * role up front was a step that changed how messaging behaved
     * before the user had seen any of it.
     *
     * The toggles card still offers the switch for anyone who wants
     * it, and these keep that path working.
     */
    val smsRoleAvailable =
        remember(
            smsRefresh,
            resumeRefresh
        ) {
            runCatching {
                smsRoleManager
                    .isSmsRoleAvailable()
            }
                .getOrDefault(
                    false
                )
        }

    val smsReady =
        remember(
            smsRefresh,
            resumeRefresh
        ) {
            runCatching {
                smsRoleManager
                    .isDefaultSmsApp()
            }
                .getOrDefault(
                    false
                )
        }

    /*
     * Weather gets another check whenever we return from
     * Android settings as well.
     */
    LaunchedEffect(
        weatherPermissionReady,
        weatherRefresh,
        resumeRefresh
    ) {
        if (
            weatherPermissionReady
        ) {
            weatherChecking =
                true

            weatherCheckAttempted =
                false

            weatherSnapshot =
                weatherRepository
                    .loadCurrentWeather()

            weatherChecking =
                false

            weatherCheckAttempted =
                true
        } else {
            weatherChecking =
                false

            weatherCheckAttempted =
                false

            weatherSnapshot =
                null
        }
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.RequestMultiplePermissions()
        ) {
            permissionAttempted =
                true

            permissionRefresh +=
                1

            val usageGranted =
                runCatching {
                    UsageStatsRepository(
                        context
                    ).hasUsageAccess()
                }
                    .getOrDefault(
                        false
                    )

            if (
                !usageGranted &&
                !usageAccessPromptShown
            ) {
                usageAccessPromptShown =
                    true

                showUsageAccessPrompt =
                    true
            }
        }

    val appSettingsLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) {
            permissionRefresh +=
                1
        }

    val homeLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) {
            homeRefresh +=
                1
        }

    val smsRoleLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartActivityForResult()
        ) {
            smsRefresh +=
                1
        }

    val googleTasksConsentLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            googleTasksAuthorizing =
                false

            val token =
                googleTasksAuth
                    .getAccessTokenFromResult(
                        result.data
                    )

            if (
                !token.isNullOrBlank()
            ) {
                taskStore
                    .selectGoogleTasks()

                taskProvider =
                    TaskProvider.GOOGLE_TASKS

                googleTasksConnected =
                    true

                taskSetupMessage =
                    "CONNECTED"
            } else {
                taskSetupMessage =
                    "GOOGLE TASKS CONNECTION CANCELLED"
            }
        }

    fun requestSetupPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    fun openApplicationSettings() {
        try {
            appSettingsLauncher.launch(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse(
                        "package:${context.packageName}"
                    )
                )
            )
        } catch (
            _: Exception
        ) {
            context.startActivity(
                Intent(
                    Settings.ACTION_SETTINGS
                )
            )
        }
    }

    /*
     * Usage Access is a special app-op, not a runtime permission, so
     * it can only be granted from its own system screen. Falls back to
     * this app's details page if the Usage Access screen is missing.
     */
    fun openUsageAccessSettings() {
        try {
            appSettingsLauncher.launch(
                Intent(
                    Settings.ACTION_USAGE_ACCESS_SETTINGS
                )
            )
        } catch (
            _: Exception
        ) {
            openApplicationSettings()
        }
    }

    fun requestHomeRole() {
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {
            val roleManager =
                context.getSystemService(
                    RoleManager::class.java
                )

            if (
                roleManager != null &&
                roleManager.isRoleAvailable(
                    RoleManager.ROLE_HOME
                )
            ) {
                if (
                    roleManager.isRoleHeld(
                        RoleManager.ROLE_HOME
                    )
                ) {
                    homeRefresh +=
                        1
                } else {
                    try {
                        homeLauncher.launch(
                            roleManager
                                .createRequestRoleIntent(
                                    RoleManager.ROLE_HOME
                                )
                        )
                    } catch (
                        _: Exception
                    ) {
                        openHomeSettings(
                            context =
                                context,
                            launcher = { intent ->
                                homeLauncher.launch(
                                    intent
                                )
                            }
                        )
                    }
                }

                return
            }
        }

        openHomeSettings(
            context =
                context,
            launcher = { intent ->
                homeLauncher.launch(
                    intent
                )
            }
        )
    }

    fun requestSmsRole() {
        if (
            smsRoleManager
                .isDefaultSmsApp()
        ) {
            smsRefresh +=
                1

            return
        }

        if (
            !smsRoleManager
                .isSmsRoleAvailable()
        ) {
            smsRefresh +=
                1

            return
        }

        try {
            smsRoleLauncher.launch(
                smsRoleManager
                    .createSmsRoleRequestIntent()
            )
        } catch (
            _: Exception
        ) {
            smsRefresh +=
                1
        }
    }

    fun connectGoogleTasks() {
        googleTasksAuthorizing =
            true

        taskSetupMessage =
            "CONNECTING..."

        googleTasksAuth.authorize(
            onAuthorized = {
                taskStore
                    .selectGoogleTasks()

                taskProvider =
                    TaskProvider.GOOGLE_TASKS

                googleTasksConnected =
                    true

                googleTasksAuthorizing =
                    false

                taskSetupMessage =
                    "CONNECTED"
            },
            onNeedsUserConsent = {
                    pendingIntent ->

                try {
                    val request =
                        IntentSenderRequest
                            .Builder(
                                pendingIntent.intentSender
                            )
                            .build()

                    googleTasksConsentLauncher
                        .launch(
                            request
                        )
                } catch (
                    _: Exception
                ) {
                    googleTasksAuthorizing =
                        false

                    taskSetupMessage =
                        "COULDN'T OPEN GOOGLE SIGN-IN"
                }
            },
            onError = {
                    message ->

                googleTasksAuthorizing =
                    false

                taskSetupMessage =
                    message
            }
        )
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(
                    Color.Black
                )
                /*
                 * The Titan's camera sits in the top-left of the
                 * panel and was clipping the header, which a fixed
                 * top padding could never account for. Insetting by
                 * the reported cutout lets the OS say how much room
                 * the camera needs, on any device.
                 */
                .windowInsetsPadding(
                    WindowInsets.displayCutout
                        .union(
                            WindowInsets.statusBars
                        )
                )
                .padding(
                    start = 28.dp,
                    end = 24.dp,
                    top = 26.dp,
                    bottom = 16.dp
                )
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize()
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
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
                        text =
                            "PROMPT LAUNCHER",
                        color =
                            Color(0xFFF1F1F1),
                        fontSize =
                            22.sp,
                        fontFamily =
                            PoppinsFamily,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(
                        text =
                            "BETA $displayedVersion",
                        color =
                            Color(0xFF707070),
                        fontSize =
                            12.5.sp,
                        fontFamily =
                            PoppinsFamily
                    )
                }

                Text(
                    text =
                        "${step + 1} / 7",
                    color =
                        Color(0xFF707070),
                    fontSize =
                        14.sp,
                    fontFamily =
                        PoppinsFamily
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        18.dp
                    )
            )

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(
                            1f
                        )
                        /*
                         * Larger type plus the cutout inset leaves
                         * less room than before, so step content
                         * scrolls rather than clipping on the
                         * tallest pages.
                         */
                        .verticalScroll(
                            rememberScrollState()
                        )
            ) {
                when (
                    step
                ) {
                    0 -> {
                        WelcomeStep()
                    }

                    1 -> {
                        PermissionsStep(
                            contactsReady =
                                contactsReady,
                            calendarReady =
                                calendarReady,
                            weatherPermissionReady =
                                weatherPermissionReady,
                            weatherChecking =
                                weatherChecking,
                            weatherCheckAttempted =
                                weatherCheckAttempted,
                            weatherSnapshot =
                                weatherSnapshot,
                            permissionAttempted =
                                permissionAttempted,
                            usageAccessReady =
                                usageAccessReady,
                            onOpenSettings = {
                                openApplicationSettings()
                            },
                            onOpenUsageAccess = {
                                openUsageAccessSettings()
                            },
                            onRetryWeather = {
                                weatherRefresh +=
                                    1
                            }
                        )
                    }

                    2 -> {
                        HomeStep(
                            homeReady =
                                homeReady
                        )
                    }

                    3 -> {
                        TasksSetupStep(
                            provider =
                                taskProvider,
                            googleTasksConnected =
                                googleTasksConnected,
                            googleTasksAuthorizing =
                                googleTasksAuthorizing,
                            taskSetupMessage =
                                taskSetupMessage,
                            todoistToken =
                                todoistToken,
                            onTodoistTokenChange = {
                                todoistToken =
                                    it
                            },
                            onConnectGoogleTasks = {
                                connectGoogleTasks()
                            }
                        )
                    }

                    4 -> {
                        GeminiSetupStep(
                            apiKey =
                                geminiApiKey,
                            onApiKeyChange = {
                                geminiApiKey =
                                    it
                            },
                            saved =
                                geminiSaved
                        )
                    }

                    5 -> {
                        AppearanceSetupStep(
                            settings =
                                launcherSettings,
                            onSettingsChange = {
                                    updated ->

                                launcherSettings =
                                    updated

                                launcherSettingsStore.save(
                                    updated
                                )
                            }
                        )
                    }

                    else -> {
                        ReadyStep(
                            geminiReady =
                                geminiSaved,
                            weatherPermissionReady =
                                weatherPermissionReady,
                            weatherSnapshot =
                                weatherSnapshot
                        )
                    }
                }
            }

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            if (
                showUsageAccessPrompt
            ) {
                UsageAccessPromptDialog(
                    onAllow = {
                        showUsageAccessPrompt =
                            false

                        openUsageAccessSettings()
                    },
                    onDismiss = {
                        showUsageAccessPrompt =
                            false
                    }
                )
            }

            when (
                step
            ) {
                0 -> {
                    SetupPrimaryButton(
                        text =
                            "GET STARTED",
                        onClick = {
                            step =
                                1
                        }
                    )
                }

                1 -> {
                    SetupPrimaryButton(
                        text =
                            if (
                                shouldRequestPermissions
                            ) {
                                "ALLOW ACCESS"
                            } else {
                                "CONTINUE"
                            },
                        onClick = {
                            if (
                                shouldRequestPermissions
                            ) {
                                requestSetupPermissions()
                            } else if (
                                !usageAccessReady &&
                                !usageAccessPromptShown
                            ) {
                                /*
                                 * Runtime permissions were already
                                 * granted, so ask about usage here.
                                 */
                                usageAccessPromptShown =
                                    true

                                showUsageAccessPrompt =
                                    true
                            } else {
                                step =
                                    2
                            }
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp
                            )
                    )

                    SetupSecondaryButton(
                        text =
                            "BACK",
                        onClick = {
                            step =
                                0
                        }
                    )
                }

                2 -> {
                    SetupPrimaryButton(
                        text =
                            if (
                                homeReady
                            ) {
                                "CONTINUE"
                            } else {
                                "CHOOSE PROMPT LAUNCHER"
                            },
                        onClick = {
                            if (
                                homeReady
                            ) {
                                step =
                                    3
                            } else {
                                requestHomeRole()
                            }
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp
                            )
                    )

                    SetupSecondaryButton(
                        text =
                            "BACK",
                        onClick = {
                            step =
                                1
                        }
                    )
                }

                3 -> {
                    val taskButtonText =
                        when {
                            todoistToken.isNotBlank() ->
                                "USE TODOIST & CONTINUE"

                            taskProvider ==
                                    TaskProvider.GOOGLE_TASKS ->
                                "CONTINUE"

                            taskProvider ==
                                    TaskProvider.TODOIST ->
                                "CONTINUE"

                            else ->
                                "SKIP FOR NOW"
                        }

                    SetupPrimaryButton(
                        text =
                            taskButtonText,
                        onClick = {

                            if (
                                todoistToken.isNotBlank()
                            ) {
                                taskStore
                                    .saveToken(
                                        todoistToken.trim()
                                    )

                                taskProvider =
                                    TaskProvider.TODOIST

                                todoistToken =
                                    ""
                            }

                            step =
                                4
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp
                            )
                    )

                    SetupSecondaryButton(
                        text =
                            "BACK",
                        onClick = {
                            step =
                                2
                        }
                    )
                }

                5 -> {
                    SetupPrimaryButton(
                        text =
                            "CONTINUE",
                        onClick = {
                            step =
                                6
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp
                            )
                    )

                    SetupSecondaryButton(
                        text =
                            "BACK",
                        onClick = {
                            step =
                                4
                        }
                    )
                }

                4 -> {
                    SetupPrimaryButton(
                        text =
                            when {
                                geminiApiKey.isNotBlank() ->
                                    "SAVE & CONTINUE"

                                geminiSaved ->
                                    "CONTINUE"

                                else ->
                                    "SKIP FOR NOW"
                            },
                        onClick = {

                            if (
                                geminiApiKey.isNotBlank()
                            ) {
                                geminiStore
                                    .saveKey(
                                        geminiApiKey.trim()
                                    )

                                geminiSaved =
                                    true

                                geminiApiKey =
                                    ""
                            }

                            step =
                                5
                        }
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                6.dp
                            )
                    )

                    SetupSecondaryButton(
                        text =
                            "BACK",
                        onClick = {
                            step =
                                4
                        }
                    )
                }

                else -> {
                    SetupPrimaryButton(
                        text =
                            "START PROMPTING",
                        onClick =
                            onFinish
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column {
        Text(
            text =
                "YOUR PHONE,\nBY PROMPT.",
            color =
                Color(0xFFF1F1F1),
            fontSize =
                32.sp,
            lineHeight =
                38.sp,
            fontFamily =
                PoppinsFamily,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(
                    14.dp
                )
        )

        Text(
            text =
                "Prompt Launcher keeps the things you need close and everything else out of the way.",
            color =
                Color(0xFF999999),
            fontSize =
                17.sp,
            lineHeight =
                23.sp,
            fontFamily =
                PoppinsFamily
        )

        Spacer(
            modifier =
                Modifier.height(
                    18.dp
                )
        )

        Text(
            text =
                "We’ll set up the essentials, then connect any optional services you want to use.",
            color =
                Color(0xFF666666),
            fontSize =
                14.sp,
            lineHeight =
                20.sp,
            fontFamily =
                PoppinsFamily
        )
    }
}

@Composable
private fun PermissionsStep(
    contactsReady: Boolean,
    calendarReady: Boolean,
    weatherPermissionReady: Boolean,
    weatherChecking: Boolean,
    weatherCheckAttempted: Boolean,
    weatherSnapshot: WeatherSnapshot?,
    permissionAttempted: Boolean,
    usageAccessReady: Boolean,
    onOpenSettings: () -> Unit,
    onOpenUsageAccess: () -> Unit,
    onRetryWeather: () -> Unit
) {
    Column {
        Text(
            text =
                "DEVICE ACCESS",
            color =
                Color(0xFFF1F1F1),
            fontSize =
                26.sp,
            fontFamily =
                PoppinsFamily,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )

        Text(
            text =
                "Prompt uses a few Android permissions to keep your launcher useful.",
            color =
                Color(0xFF999999),
            fontSize =
                14.sp,
            lineHeight =
                20.sp,
            fontFamily =
                PoppinsFamily
        )

        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )

        CompactPermissionStatusRow(
            label =
                "CONTACTS",
            status =
                if (
                    contactsReady
                ) {
                    "READY"
                } else {
                    "NEEDED"
                },
            ready =
                contactsReady
        )

        Spacer(
            modifier =
                Modifier.height(
                    7.dp
                )
        )

        CompactPermissionStatusRow(
            label =
                "CALENDAR",
            status =
                if (
                    calendarReady
                ) {
                    "READY"
                } else {
                    "NEEDED"
                },
            ready =
                calendarReady
        )

        Spacer(
            modifier =
                Modifier.height(
                    7.dp
                )
        )

        val weatherStatus =
            when {
                !weatherPermissionReady ->
                    "OPTIONAL"

                weatherChecking ->
                    "CHECKING"

                weatherSnapshot != null ->
                    "READY"

                weatherCheckAttempted ->
                    "WAITING"

                else ->
                    "ENABLED"
            }

        CompactPermissionStatusRow(
            label =
                "WEATHER",
            status =
                weatherStatus,
            ready =
                weatherSnapshot != null
        )

        Spacer(
            modifier =
                Modifier.height(
                    7.dp
                )
        )

        /*
         * Tappable until granted: opens the Usage Access screen.
         * Status refreshes on return through resumeRefresh.
         */
        Box(
            modifier =
                if (
                    usageAccessReady
                ) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpenUsageAccess()
                        }
                }
        ) {
            CompactPermissionStatusRow(
                label =
                    "PRODUCTIVITY",
                status =
                    if (
                        usageAccessReady
                    ) {
                        "READY"
                    } else {
                        "ALLOW →"
                    },
                ready =
                    usageAccessReady
            )
        }

        if (
            !usageAccessReady
        ) {
            Text(
                text =
                    "Turn on Usage Access for Prompt Launcher so the hourly dots can fill in. If the switch is grayed out, open App info → ⋮ → Allow restricted settings first.",
                color =
                    Color(0xFF5F5F5F),
                fontSize =
                    11.5.sp,
                lineHeight =
                    15.5.sp,
                fontFamily =
                    PoppinsFamily
            )
        }

        if (
            permissionAttempted &&
            (
                    !contactsReady ||
                            !calendarReady
                    )
        ) {
            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
                    )
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        "Access still disabled.",
                    color =
                        Color(0xFF777777),
                    fontSize =
                        12.sp,
                    fontFamily =
                        PoppinsFamily
                )

                Text(
                    text =
                        "APP SETTINGS →",
                    color =
                        Color(0xFFFF6438),
                    fontSize =
                        12.sp,
                    fontFamily =
                        PoppinsFamily,
                    fontWeight =
                        FontWeight.Bold,
                    modifier =
                        Modifier.clickable {
                            onOpenSettings()
                        }
                )
            }
        } else if (
            weatherPermissionReady &&
            weatherCheckAttempted &&
            weatherSnapshot == null
        ) {
            Spacer(
                modifier =
                    Modifier.height(
                        9.dp
                    )
            )

            Text(
                text =
                    "Location access is on, but weather isn't available yet.",
                color =
                    Color(0xFF5F5F5F),
                fontSize =
                    11.5.sp,
                lineHeight =
                    15.5.sp,
                fontFamily =
                    PoppinsFamily
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )

            Text(
                text =
                    "RETRY WEATHER →",
                color =
                    Color(0xFFFF6438),
                fontSize =
                    11.5.sp,
                fontFamily =
                    PoppinsFamily,
                fontWeight =
                    FontWeight.Bold,
                modifier =
                    Modifier.clickable {
                        onRetryWeather()
                    }
            )
        } else if (
            permissionAttempted &&
            contactsReady &&
            calendarReady &&
            !weatherPermissionReady
        ) {
            Spacer(
                modifier =
                    Modifier.height(
                        9.dp
                    )
            )

            Text(
                text =
                    "Weather is optional. Prompt still works without it.",
                color =
                    Color(0xFF5F5F5F),
                fontSize =
                    11.5.sp,
                lineHeight =
                    15.5.sp,
                fontFamily =
                    PoppinsFamily
            )
        }
    }
}

/*
 * Styled like Android's permission prompts. Allow hands off to the
 * Usage Access screen, since the toggle itself only lives there.
 */
@Composable
private fun UsageAccessPromptDialog(
    onAllow: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest =
            onDismiss
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF141414),
                        RoundedCornerShape(
                            18.dp
                        )
                    )
                    .border(
                        width =
                            1.dp,
                        color =
                            Color(0xFF242424),
                        shape =
                            RoundedCornerShape(
                                18.dp
                            )
                    )
                    .padding(
                        horizontal =
                            18.dp,
                        vertical =
                            18.dp
                    ),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {
            Text(
                text =
                    "Allow Prompt Launcher to access app usage?",
                color =
                    Color(0xFFF1F1F1),
                fontSize =
                    16.sp,
                lineHeight =
                    21.sp,
                fontFamily =
                    PoppinsFamily,
                fontWeight =
                    FontWeight.Bold,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(
                        8.dp
                    )
            )

            Text(
                text =
                    "This powers your hourly productivity dots. On the next screen, tap Prompt Launcher and turn on usage access.",
                color =
                    Color(0xFF999999),
                fontSize =
                    12.5.sp,
                lineHeight =
                    17.sp,
                fontFamily =
                    PoppinsFamily,
                textAlign =
                    TextAlign.Center
            )

            Spacer(
                modifier =
                    Modifier.height(
                        16.dp
                    )
            )

            SetupPrimaryButton(
                text =
                    "ALLOW",
                onClick =
                    onAllow
            )

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            SetupSecondaryButton(
                text =
                    "NOT NOW",
                onClick =
                    onDismiss
            )
        }
    }
}

@Composable
private fun CompactPermissionStatusRow(
    label: String,
    status: String,
    ready: Boolean
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(
                    min = 24.dp
                ),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text =
                label,
            color =
                Color(0xFF999999),
            fontSize =
                14.sp,
            fontFamily =
                PoppinsFamily
        )

        Text(
            text =
                status,
            color =
                if (
                    ready
                ) {
                    Color(0xFFF1F1F1)
                } else {
                    Color(0xFFFF6438)
                },
            fontSize =
                12.5.sp,
            fontFamily =
                PoppinsFamily,
            fontWeight =
                FontWeight.Bold
        )
    }
}

@Composable
private fun HomeStep(
    homeReady: Boolean
) {
    Column {
        StepTitle(
            title =
                "MAKE IT HOME",
            description =
                "Choose Prompt Launcher as your default Home app so the Home button always returns here."
        )

        Spacer(
            modifier =
                Modifier.height(
                    20.dp
                )
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Text(
                text =
                    "DEFAULT HOME",
                color =
                    Color(0xFF999999),
                fontSize =
                    15.sp,
                fontFamily =
                    PoppinsFamily
            )

            Text(
                text =
                    if (
                        homeReady
                    ) {
                        "READY"
                    } else {
                        "NOT SET"
                    },
                color =
                    if (
                        homeReady
                    ) {
                        Color(0xFFF1F1F1)
                    } else {
                        Color(0xFFFF6438)
                    },
                fontSize =
                    14.sp,
                fontFamily =
                    PoppinsFamily,
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )

        Text(
            text =
                if (
                    homeReady
                ) {
                    "Prompt Launcher is now your Home app."
                } else {
                    "Android will ask which launcher you want to use."
                },
            color =
                Color(0xFF666666),
            fontSize =
                12.5.sp,
            lineHeight =
                15.5.sp,
            fontFamily =
                PoppinsFamily,
            modifier =
                Modifier.fillMaxWidth(),
            textAlign =
                TextAlign.Start
        )
    }
}

@Composable
private fun MessagesSetupStep(
    smsReady: Boolean,
    smsRoleAvailable: Boolean,
    onOpenAppSettings: () -> Unit
) {

    Column(
        modifier =
            Modifier.fillMaxSize()
    ) {

        StepTitle(
            title =
                "MESSAGES",
            description =
                "Choose Prompt Launcher as your default SMS app."
        )

        Spacer(
            modifier =
                Modifier.height(
                    8.dp
                )
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text =
                    "DEFAULT SMS APP",
                color =
                    Color(0xFF999999),
                fontSize =
                    14.sp,
                fontFamily =
                    PoppinsFamily
            )

            Text(
                text =
                    when {

                        smsReady ->
                            "READY"

                        !smsRoleAvailable ->
                            "UNAVAILABLE"

                        else ->
                            "NOT SET"
                    },
                color =
                    if (
                        smsReady
                    ) {
                        Color(0xFFF1F1F1)
                    } else {
                        Color(0xFFFF6438)
                    },
                fontSize =
                    12.5.sp,
                fontFamily =
                    PoppinsFamily,
                fontWeight =
                    FontWeight.Bold
            )
        }

        Spacer(
            modifier =
                Modifier.height(
                    7.dp
                )
        )

        Text(
            text =
                when {

                    smsReady ->
                        "Messages are ready inside Prompt Launcher."

                    !smsRoleAvailable ->
                        "Default SMS is unavailable on this device. You can continue setup."

                    else ->
                        "Android will ask which app should handle SMS."
                },
            color =
                Color(0xFF666666),
            fontSize =
                12.sp,
            lineHeight =
                15.5.sp,
            fontFamily =
                PoppinsFamily
        )

        if (
            !smsReady &&
            smsRoleAvailable
        ) {

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
                        .border(
                            width =
                                1.dp,
                            color =
                                Color(0xFF2D2D2D),
                            shape =
                                RoundedCornerShape(
                                    8.dp
                                )
                        )
                        .padding(
                            horizontal =
                                8.dp,
                            vertical =
                                6.dp
                        )
            ) {

                Column {

                    Text(
                        text =
                            "SIDELOADED BETA",
                        color =
                            Color(0xFFFF6438),
                        fontSize =
                            11.sp,
                        fontFamily =
                            PoppinsFamily,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                3.dp
                            )
                    )

                    Text(
                        text =
                            "If SMS access is denied: open app settings, tap ⋮ > Allow restricted settings, then return here and choose Prompt Launcher again.",
                        color =
                            Color(0xFF777777),
                        fontSize =
                            10.5.sp,
                        lineHeight =
                            13.sp,
                        fontFamily =
                            PoppinsFamily
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                5.dp
                            )
                    )

                    Text(
                        text =
                            "OPEN APP SETTINGS →",
                        color =
                            Color(0xFFFF6438),
                        fontSize =
                            11.5.sp,
                        lineHeight =
                            13.sp,
                        fontFamily =
                            PoppinsFamily,
                        fontWeight =
                            FontWeight.Bold,
                        maxLines =
                            1,
                        modifier =
                            Modifier
                                .clickable {
                                    onOpenAppSettings()
                                }
                                .padding(
                                    vertical =
                                        1.dp
                                )
                    )
                }
            }
        }
    }
}


@Composable
private fun TasksSetupStep(
    provider: TaskProvider,
    googleTasksConnected: Boolean,
    googleTasksAuthorizing: Boolean,
    taskSetupMessage: String?,
    todoistToken: String,
    onTodoistTokenChange: (String) -> Unit,
    onConnectGoogleTasks: () -> Unit
) {
    Column {
        Text(
            text =
                "TASKS",
            color =
                Color(0xFFF1F1F1),
            fontSize =
                26.sp,
            fontFamily =
                PoppinsFamily,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(
                    4.dp
                )
        )

        Text(
            text =
                "Choose where Prompt should save your to-dos.",
            color =
                Color(0xFF999999),
            fontSize =
                13.5.sp,
            lineHeight =
                18.sp,
            fontFamily =
                PoppinsFamily
        )

        Spacer(
            modifier =
                Modifier.height(
                    7.dp
                )
        )

        GoogleTasksCompactCard(
            provider =
                provider,
            connected =
                googleTasksConnected,
            authorizing =
                googleTasksAuthorizing,
            onConnect =
                onConnectGoogleTasks
        )

        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )

        TodoistExpandedCard(
            provider =
                provider,
            token =
                todoistToken,
            onTokenChange =
                onTodoistTokenChange
        )

        if (
            !taskSetupMessage.isNullOrBlank()
        ) {
            Spacer(
                modifier =
                    Modifier.height(
                        4.dp
                    )
            )

            Text(
                text =
                    taskSetupMessage,
                color =
                    if (
                        taskSetupMessage.equals(
                            "CONNECTED",
                            ignoreCase = true
                        )
                    ) {
                        Color(0xFFF1F1F1)
                    } else {
                        Color(0xFFFF6438)
                    },
                fontSize =
                    10.5.sp,
                fontFamily =
                    PoppinsFamily,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GoogleTasksCompactCard(
    provider: TaskProvider,
    connected: Boolean,
    authorizing: Boolean,
    onConnect: () -> Unit
) {
    /*
     * Sized by its content. The old fixed 60.dp was measured against
     * smaller type and clipped the "RECOMMENDED" line underneath the
     * title once the onboarding text scaled up.
     */
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(
                    min = 60.dp
                )
                .border(
                    width =
                        1.dp,
                    color =
                        if (
                            provider ==
                            TaskProvider.GOOGLE_TASKS
                        ) {
                            Color(0xFF666666)
                        } else {
                            Color(0xFF252525)
                        },
                    shape =
                        RoundedCornerShape(
                            9.dp
                        )
                )
                .clickable(
                    enabled =
                        !authorizing
                ) {
                    onConnect()
                }
                .padding(
                    horizontal =
                        12.dp,
                    vertical =
                        6.dp
                )
    ) {
        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text =
                        "GOOGLE TASKS",
                    color =
                        Color(0xFFF1F1F1),
                    fontSize =
                        14.sp,
                    fontFamily =
                        PoppinsFamily,
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    modifier =
                        Modifier.height(
                            2.dp
                        )
                )

                Text(
                    text =
                        if (
                            provider ==
                            TaskProvider.GOOGLE_TASKS &&
                            connected
                        ) {
                            "CONNECTED"
                        } else {
                            "RECOMMENDED"
                        },
                    color =
                        if (
                            provider ==
                            TaskProvider.GOOGLE_TASKS &&
                            connected
                        ) {
                            Color(0xFFF1F1F1)
                        } else {
                            Color(0xFFFF6438)
                        },
                    fontSize =
                        10.5.sp,
                    fontFamily =
                        PoppinsFamily,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Text(
                text =
                    when {
                        authorizing ->
                            "CONNECTING"

                        provider ==
                                TaskProvider.GOOGLE_TASKS &&
                                connected ->
                            "CONNECTED"

                        else ->
                            "CONNECT"
                    },
                color =
                    if (
                        provider ==
                        TaskProvider.GOOGLE_TASKS &&
                        connected
                    ) {
                        Color(0xFFF1F1F1)
                    } else {
                        Color(0xFFFF6438)
                    },
                fontSize =
                    11.5.sp,
                fontFamily =
                    PoppinsFamily,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TodoistExpandedCard(
    provider: TaskProvider,
    token: String,
    onTokenChange: (String) -> Unit
) {
    /*
     * Sized by its content rather than a fixed height. The old 92.dp
     * was measured against a monospace title; Poppins sits in a taller
     * line box, which pushed the token field past the bottom border.
     */
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width =
                        1.dp,
                    color =
                        if (
                            provider ==
                            TaskProvider.TODOIST
                        ) {
                            Color(0xFF666666)
                        } else {
                            Color(0xFF252525)
                        },
                    shape =
                        RoundedCornerShape(
                            9.dp
                        )
                )
                .padding(
                    horizontal =
                        10.dp,
                    vertical =
                        8.dp
                )
    ) {
        Column(
            modifier =
                Modifier.fillMaxWidth()
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text =
                        "TODOIST",
                    color =
                        Color(0xFFF1F1F1),
                    fontSize =
                        14.sp,
                    fontFamily =
                        PoppinsFamily,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    text =
                        if (
                            provider ==
                            TaskProvider.TODOIST
                        ) {
                            "CONNECTED"
                        } else {
                            "OPTIONAL"
                        },
                    color =
                        if (
                            provider ==
                            TaskProvider.TODOIST
                        ) {
                            Color(0xFFF1F1F1)
                        } else {
                            Color(0xFF707070)
                        },
                    fontSize =
                        11.sp,
                    fontFamily =
                        PoppinsFamily,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                modifier =
                    Modifier.height(
                        6.dp
                    )
            )

            CompactSecretField(
                value =
                    token,
                onValueChange =
                    onTokenChange,
                placeholder =
                    "PASTE TODOIST API TOKEN"
            )
        }
    }
}

@Composable
private fun GeminiSetupStep(
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    saved: Boolean
) {
    Column {
        Text(
            text =
                "AI ASSISTANT",
            color =
                Color(0xFFF1F1F1),
            fontSize =
                26.sp,
            fontFamily =
                PoppinsFamily,
            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )

        Text(
            text =
                "Optional. Add a Gemini API key to power Prompt Launcher with tasteful AI integration.",
            color =
                Color(0xFF999999),
            fontSize =
                14.sp,
            lineHeight =
                20.sp,
            fontFamily =
                PoppinsFamily
        )

        Spacer(
            modifier =
                Modifier.height(
                    10.dp
                )
        )

        ConnectionStatusRow(
            label =
                "GEMINI",
            ready =
                saved,
            readyText =
                "KEY SAVED"
        )

        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )

        SetupSecretField(
            value =
                apiKey,
            onValueChange =
                onApiKeyChange,
            placeholder =
                "PASTE GEMINI API KEY"
        )

        Spacer(
            modifier =
                Modifier.height(
                    9.dp
                )
        )

        Text(
            text =
                "Create your key in Google AI Studio.\n\n" +
                        "Your question goes straight from this phone to Google Gemini with your key. " +
                        "Prompt Launcher has no server. Calendar, texts and mail are NOT sent unless you " +
                        "turn each one on later with the geminisetup command, which lists exactly what each sends.",
            color =
                Color(0xFF5F5F5F),
            fontSize =
                12.sp,
            lineHeight =
                17.sp,
            fontFamily =
                PoppinsFamily
        )
    }
}

@Composable
private fun ReadyStep(
    geminiReady: Boolean,
    weatherPermissionReady: Boolean,
    weatherSnapshot: WeatherSnapshot?
) {
    Column {
        StepTitle(
            title =
                "YOU’RE READY",
            description =
                "The prompt is the fastest way around your phone."
        )

        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )

        CommandPreviewRow(
            command =
                ".",
            action =
                "apps"
        )

        CommandPreviewRow(
            command =
                "@",
            action =
                "message"
        )

        CommandPreviewRow(
            command =
                "/",
            action =
                "call"
        )

        CommandPreviewRow(
            command =
                ":",
            action =
                "event"
        )

        CommandPreviewRow(
            command =
                "+",
            action =
                "to do"
        )

        CommandPreviewRow(
            command =
                "?",
            action =
                "assistant"
        )

        Spacer(
            modifier =
                Modifier.height(
                    9.dp
                )
        )

        if (
            weatherSnapshot != null
        ) {
            Text(
                text =
                    "Weather ready.",
                color =
                    Color(0xFF666666),
                fontSize =
                    12.sp,
                fontFamily =
                    PoppinsFamily
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )
        } else if (
            weatherPermissionReady
        ) {
            Text(
                text =
                    "Weather location enabled. Prompt will keep trying.",
                color =
                    Color(0xFF666666),
                fontSize =
                    12.sp,
                fontFamily =
                    PoppinsFamily
            )

            Spacer(
                modifier =
                    Modifier.height(
                        5.dp
                    )
            )
        }

        Text(
            text =
                "More commands are available from the help card inside Prompt.",
            color =
                Color(0xFF555555),
            fontSize =
                12.sp,
            lineHeight =
                17.sp,
            fontFamily =
                PoppinsFamily
        )
    }
}

@Composable
private fun StepTitle(
    title: String,
    description: String
) {
    Text(
        text =
            title,
        color =
            Color(0xFFF1F1F1),
        fontSize =
            27.sp,
        fontFamily =
            PoppinsFamily,
        fontWeight =
            FontWeight.Bold
    )

    Spacer(
        modifier =
            Modifier.height(
                10.dp
            )
    )

    Text(
        text =
            description,
        color =
            Color(0xFF999999),
        fontSize =
            15.sp,
        lineHeight =
            22.sp,
        fontFamily =
            PoppinsFamily
    )
}

@Composable
private fun ConnectionStatusRow(
    label: String,
    ready: Boolean,
    readyText: String
) {
    Row(
        modifier =
            Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.SpaceBetween,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text =
                label,
            color =
                Color(0xFF999999),
            fontSize =
                14.sp,
            fontFamily =
                PoppinsFamily
        )

        Text(
            text =
                if (
                    ready
                ) {
                    readyText
                } else {
                    "OPTIONAL"
                },
            color =
                if (
                    ready
                ) {
                    Color(0xFFF1F1F1)
                } else {
                    Color(0xFF707070)
                },
            fontSize =
                12.sp,
            fontFamily =
                PoppinsFamily,
            fontWeight =
                FontWeight.Bold
        )
    }
}

@Composable
private fun CompactSecretField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    /*
     * A minimum rather than a fixed height, with padding inside the
     * box. Poppins sits in a taller line box than the monospace this
     * was measured against, which clipped the token text top and
     * bottom.
     */
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(
                    min = 38.dp
                )
                .background(
                    color =
                        Color(0xFF1A1A1A),
                    shape =
                        RoundedCornerShape(
                            6.dp
                        )
                )
                .border(
                    width =
                        1.dp,
                    color =
                        Color(0xFF383838),
                    shape =
                        RoundedCornerShape(
                            6.dp
                        )
                )
                .padding(
                    vertical =
                        6.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {
        BasicTextField(
            value =
                value,
            onValueChange = {
                val cleaned =
                    it.replace(
                        "\n",
                        ""
                    )

                if (
                    cleaned.length <=
                    512
                ) {
                    onValueChange(
                        cleaned
                    )
                }
            },
            singleLine =
                true,
            visualTransformation =
                PasswordVisualTransformation(),
            textStyle =
                TextStyle(
                    color =
                        Color(0xFFF1F1F1),
                    fontSize =
                        12.5.sp,
                    lineHeight =
                        17.sp,
                    fontFamily =
                        PoppinsFamily
                ),
            cursorBrush =
                SolidColor(
                    Color(0xFFFF6438)
                ),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal =
                            9.dp
                    ),
            decorationBox = {
                    innerTextField ->

                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    if (
                        value.isBlank()
                    ) {
                        Text(
                            text =
                                placeholder,
                            color =
                                Color(0xFF666666),
                            fontSize =
                                11.5.sp,
                            lineHeight =
                                15.5.sp,
                            fontFamily =
                                PoppinsFamily,
                            fontWeight =
                                FontWeight.Bold,
                            modifier =
                                Modifier.align(
                                    Alignment.Center
                                )
                        )
                    }

                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal =
                                        1.dp
                                ),
                        contentAlignment =
                            Alignment.CenterStart
                    ) {
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
private fun SetupSecretField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
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
                        Color(0xFF1A1A1A),
                    shape =
                        RoundedCornerShape(
                            8.dp
                        )
                )
                .border(
                    width =
                        1.dp,
                    color =
                        Color(0xFF383838),
                    shape =
                        RoundedCornerShape(
                            8.dp
                        )
                ),
        contentAlignment =
            Alignment.Center
    ) {
        BasicTextField(
            value =
                value,
            onValueChange = {
                val cleaned =
                    it.replace(
                        "\n",
                        ""
                    )

                if (
                    cleaned.length <=
                    512
                ) {
                    onValueChange(
                        cleaned
                    )
                }
            },
            singleLine =
                true,
            visualTransformation =
                PasswordVisualTransformation(),
            textStyle =
                TextStyle(
                    color =
                        Color(0xFFF1F1F1),
                    fontSize =
                        14.sp,
                    fontFamily =
                        PoppinsFamily
                ),
            cursorBrush =
                SolidColor(
                    Color(0xFFFF6438)
                ),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal =
                            12.dp
                    ),
            decorationBox = {
                    innerTextField ->

                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {
                    if (
                        value.isBlank()
                    ) {
                        Text(
                            text =
                                placeholder,
                            color =
                                Color(0xFF666666),
                            fontSize =
                                12.sp,
                            fontFamily =
                                PoppinsFamily,
                            fontWeight =
                                FontWeight.Bold,
                            modifier =
                                Modifier.align(
                                    Alignment.Center
                                )
                        )
                    }

                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.CenterStart
                    ) {
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
private fun CommandPreviewRow(
    command: String,
    action: String
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical =
                        3.dp
                ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text =
                command,
            color =
                Color(0xFFFF6438),
            fontSize =
                17.sp,
            fontFamily =
                PoppinsFamily,
            fontWeight =
                FontWeight.Bold,
            modifier =
                Modifier.fillMaxWidth(
                    0.22f
                )
        )

        Text(
            text =
                action,
            color =
                Color(0xFF999999),
            fontSize =
                13.5.sp,
            fontFamily =
                PoppinsFamily
        )
    }
}

@Composable
private fun SetupPrimaryButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color =
                        Color(0xFFF1F1F1),
                    shape =
                        RoundedCornerShape(
                            10.dp
                        )
                )
                .clickable {
                    onClick()
                }
                .padding(
                    vertical =
                        11.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text =
                text,
            color =
                Color.Black,
            fontSize =
                14.sp,
            fontFamily =
                PoppinsFamily,
            fontWeight =
                FontWeight.Bold
        )
    }
}

@Composable
private fun SetupSecondaryButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    width =
                        1.dp,
                    color =
                        Color(0xFF242424),
                    shape =
                        RoundedCornerShape(
                            10.dp
                        )
                )
                .clickable {
                    onClick()
                }
                .padding(
                    vertical =
                        9.dp
                ),
        contentAlignment =
            Alignment.Center
    ) {
        Text(
            text =
                text,
            color =
                Color(0xFF777777),
            fontSize =
                12.5.sp,
            fontFamily =
                PoppinsFamily,
            fontWeight =
                FontWeight.Bold
        )
    }
}

private fun promptLauncherIsHome(
    context: Context
): Boolean {
    if (
        Build.VERSION.SDK_INT >=
        Build.VERSION_CODES.Q
    ) {
        val roleManager =
            context.getSystemService(
                RoleManager::class.java
            )

        if (
            roleManager != null &&
            roleManager.isRoleAvailable(
                RoleManager.ROLE_HOME
            )
        ) {
            return roleManager.isRoleHeld(
                RoleManager.ROLE_HOME
            )
        }
    }

    val homeIntent =
        Intent(
            Intent.ACTION_MAIN
        ).apply {
            addCategory(
                Intent.CATEGORY_HOME
            )
        }

    val resolved =
        context.packageManager
            .resolveActivity(
                homeIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )

    return resolved
        ?.activityInfo
        ?.packageName ==
            context.packageName
}

private fun openHomeSettings(
    context: Context,
    launcher: (Intent) -> Unit
) {
    try {
        launcher(
            Intent(
                Settings.ACTION_HOME_SETTINGS
            )
        )
    } catch (
        _: Exception
    ) {
        context.startActivity(
            Intent(
                Settings.ACTION_SETTINGS
            )
        )
    }
}

private fun getPromptLauncherVersionName(
    context: Context
): String {
    return try {
        val packageInfo =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {
                context.packageManager
                    .getPackageInfo(
                        context.packageName,
                        PackageManager.PackageInfoFlags.of(
                            0
                        )
                    )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager
                    .getPackageInfo(
                        context.packageName,
                        0
                    )
            }

        packageInfo
            .versionName
            ?.takeIf {
                it.isNotBlank()
            }
            ?: "1.0"

    } catch (
        _: Exception
    ) {
        "1.0"
    }
}

/*
 * APPEARANCE
 *
 * The last thing before the launcher opens, so the first screen the
 * user sees is already theirs. Writes straight through to the
 * settings store — the same values the settings screens edit later.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSetupStep(
    settings: LauncherSettings,
    onSettingsChange: (LauncherSettings) -> Unit
) {

    /*
     * Derived through the same mappers the launcher uses, so what is
     * shown here is literally what the home screen will render.
     */
    val previewAccent =
        accentColorFor(
            settings.accent
        )

    val previewPalette =
        paletteFor(
            settings.themeMode
        )

    val previewFont =
        when (
            settings.interfaceFont
        ) {
            LauncherInterfaceFont.MONO -> FontFamily.Monospace
            LauncherInterfaceFont.SANS -> FontFamily.SansSerif
            LauncherInterfaceFont.SERIF -> FontFamily.Serif
            LauncherInterfaceFont.POPPINS -> PoppinsFamily
        }

    val previewTextOverride =
        textColorOverrideFor(
            settings.textColor,
            previewAccent
        )

    val previewPrimary =
        previewTextOverride
            ?: previewPalette.primaryText

    val previewSecondary =
        previewTextOverride
            ?.copy(
                alpha = 0.62f
            )
            ?: previewPalette.secondaryText

    Column {

        StepTitle(
            title =
                "MAKE IT YOURS",
            description =
                "Pick a look. You can change any of this later in settings."
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        /*
         * A miniature of the home screen header, so the choices are
         * judged against something recognisable rather than in the
         * abstract.
         */
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        previewPalette.background,
                        RoundedCornerShape(11.dp)
                    )
                    .border(
                        0.8.dp,
                        previewPalette.border,
                        RoundedCornerShape(11.dp)
                    )
                    .padding(
                        horizontal = 12.dp,
                        vertical = 10.dp
                    )
        ) {

            Text(
                text = "monday, sep 28",
                color = previewPrimary,
                fontSize = 13.sp,
                lineHeight = 14.sp,
                fontFamily = previewFont,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "cloudy, 84°",
                color = previewSecondary,
                fontSize = 11.sp,
                lineHeight = 12.sp,
                fontFamily = previewFont
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = "> message shan",
                color = previewAccent,
                fontSize = 12.sp,
                lineHeight = 13.sp,
                fontFamily = previewFont,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        OnboardingChoiceRow(
            accent = previewAccent,
            font = previewFont,
            label = "accent",
            options =
                LauncherAccent.entries.map {
                    it.name.lowercase()
                },
            selectedIndex =
                LauncherAccent.entries.indexOf(
                    settings.accent
                ),
            onSelected = { index ->
                onSettingsChange(
                    settings.copy(
                        accent =
                            LauncherAccent.entries[index]
                    )
                )
            }
        )

        Spacer(
            modifier = Modifier.height(11.dp)
        )

        OnboardingChoiceRow(
            accent = previewAccent,
            font = previewFont,
            label = "theme",
            options =
                LauncherThemeMode.entries.map {
                    it.name.lowercase()
                },
            selectedIndex =
                LauncherThemeMode.entries.indexOf(
                    settings.themeMode
                ),
            onSelected = { index ->
                onSettingsChange(
                    settings.copy(
                        themeMode =
                            LauncherThemeMode.entries[index]
                    )
                )
            }
        )

        Spacer(
            modifier = Modifier.height(11.dp)
        )

        OnboardingChoiceRow(
            accent = previewAccent,
            font = previewFont,
            label = "font",
            options =
                LauncherInterfaceFont.entries.map {
                    it.name.lowercase()
                },
            selectedIndex =
                LauncherInterfaceFont.entries.indexOf(
                    settings.interfaceFont
                ),
            onSelected = { index ->
                onSettingsChange(
                    settings.copy(
                        interfaceFont =
                            LauncherInterfaceFont.entries[index]
                    )
                )
            }
        )

        Spacer(
            modifier = Modifier.height(11.dp)
        )

        OnboardingChoiceRow(
            accent = previewAccent,
            font = previewFont,
            label = "text",
            options =
                LauncherTextColor.entries.map {
                    it.name.lowercase()
                },
            selectedIndex =
                LauncherTextColor.entries.indexOf(
                    settings.textColor
                ),
            onSelected = { index ->
                onSettingsChange(
                    settings.copy(
                        textColor =
                            LauncherTextColor.entries[index]
                    )
                )
            }
        )
    }
}


/*
 * Wrapping chips, because seven accents will not fit on one line at
 * onboarding's larger type.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingChoiceRow(
    accent: Color,
    font: FontFamily,
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {

    Column {

        Text(
            text = label,
            color = Color(0xFF8A8A8A),
            fontSize = 11.sp,
            lineHeight = 11.sp,
            fontFamily = font
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        FlowRow(
            horizontalArrangement =
                Arrangement.spacedBy(6.dp),
            verticalArrangement =
                Arrangement.spacedBy(6.dp)
        ) {

            options.forEachIndexed { index, option ->

                val selected =
                    index == selectedIndex

                Box(
                    modifier =
                        Modifier
                            .heightIn(min = 30.dp)
                            .background(
                                if (selected) {
                                    accent
                                } else {
                                    Color(0xFF1C1C1C)
                                },
                                RoundedCornerShape(9.dp)
                            )
                            .border(
                                0.8.dp,
                                if (selected) {
                                    accent
                                } else {
                                    Color(0xFF3A3A3A)
                                },
                                RoundedCornerShape(9.dp)
                            )
                            .clickable {
                                onSelected(index)
                            }
                            .padding(
                                horizontal = 11.dp,
                                vertical = 6.dp
                            ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = option,
                        color =
                            if (selected) {
                                Color(0xFF101010)
                            } else {
                                Color(0xFFBDBDBD)
                            },
                        fontSize = 11.sp,
                        lineHeight = 11.sp,
                        fontFamily = font,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}