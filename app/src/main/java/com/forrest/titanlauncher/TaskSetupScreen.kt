package com.forrest.titanlauncher

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forrest.titanlauncher.todoist.GoogleTasksAuthManager
import com.forrest.titanlauncher.todoist.TaskProvider
import com.forrest.titanlauncher.todoist.TodoistTokenStore

/*
 * TASKS SETUP
 *
 * The same choice onboarding offers, reachable any time with the
 * "taskssetup" command: connect Google Tasks, paste a Todoist token,
 * or turn tasks off. Whatever is picked here is what "+" adds to.
 */

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@Composable
internal fun TaskSetupScreen(
    onStatus: (String) -> Unit,
    onBack: () -> Unit
) {

    val context =
        LocalContext.current

    val activity =
        remember(
            context
        ) {
            context.findActivity()
        }

    val taskStore =
        remember {
            TodoistTokenStore(
                context
            )
        }

    val googleTasksAuth =
        remember(
            activity
        ) {
            activity?.let {
                GoogleTasksAuthManager(
                    it
                )
            }
        }

    var provider by remember {
        mutableStateOf(
            taskStore.getTaskProvider()
        )
    }

    var notice by remember {
        mutableStateOf(
            ""
        )
    }

    var todoistToken by remember {
        mutableStateOf(
            ""
        )
    }

    var connecting by remember {
        mutableStateOf(
            false
        )
    }

    fun googleTasksConnected() {
        taskStore.selectGoogleTasks()

        provider =
            TaskProvider.GOOGLE_TASKS

        connecting =
            false

        notice =
            "google tasks connected"

        onStatus(
            "✓ GOOGLE TASKS READY"
        )
    }

    val consentLauncher =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->

            val token =
                googleTasksAuth
                    ?.getAccessTokenFromResult(
                        result.data
                    )

            if (
                !token.isNullOrBlank()
            ) {
                googleTasksConnected()
            } else {
                connecting =
                    false

                notice =
                    "google tasks connection cancelled"
            }
        }

    fun connectGoogleTasks() {

        val auth =
            googleTasksAuth

        if (
            auth == null
        ) {
            notice =
                "could not start google sign-in"
            return
        }

        connecting =
            true

        notice =
            "connecting..."

        auth.authorize(
            onAuthorized = {
                googleTasksConnected()
            },
            onNeedsUserConsent = { pendingIntent ->

                runCatching {
                    consentLauncher.launch(
                        IntentSenderRequest
                            .Builder(
                                pendingIntent.intentSender
                            )
                            .build()
                    )
                }
                    .onFailure {
                        connecting =
                            false

                        notice =
                            "could not open google sign-in"
                    }
            },
            onError = { message ->
                connecting =
                    false

                notice =
                    message.lowercase()
            }
        )
    }

    fun saveTodoistToken() {

        val cleaned =
            todoistToken.trim()

        /*
         * No new token typed but one is already saved: just switch
         * back to Todoist.
         */
        if (
            cleaned.isBlank() &&
            taskStore.hasTodoistToken()
        ) {
            taskStore.selectTodoist()

            provider =
                TaskProvider.TODOIST

            notice =
                "switched to todoist"

            onStatus(
                "✓ TODOIST READY"
            )

            return
        }

        if (
            cleaned.isBlank()
        ) {
            notice =
                "paste your todoist token first"
            return
        }

        taskStore.saveToken(
            cleaned
        )

        todoistToken =
            ""

        provider =
            TaskProvider.TODOIST

        notice =
            "todoist connected"

        onStatus(
            "✓ TODOIST READY"
        )
    }

    fun turnOff() {
        taskStore.turnOffTasks()

        provider =
            TaskProvider.NONE

        notice =
            "tasks turned off"
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
                .onPreviewKeyEvent { event ->
                    if (
                        event.type ==
                        KeyEventType.KeyDown &&
                        event.key ==
                        Key.Escape
                    ) {
                        onBack()
                        true
                    } else {
                        false
                    }
                }
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = CameraSafeStartPadding
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {
            LauncherBackButton(
                onBack = onBack
            )

            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )

            Text(
                text =
                    "TASKS SETUP",
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
                "\"+\" adds to: " +
                        when (provider) {
                            TaskProvider.GOOGLE_TASKS -> "google tasks"
                            TaskProvider.TODOIST -> "todoist"
                            TaskProvider.NONE -> "nothing yet"
                        },
            color =
                PrimaryText,
            fontSize =
                10.sp,
            fontFamily =
                InterfaceFont
        )

        Spacer(
            modifier =
                Modifier.height(
                    12.dp
                )
        )

        TaskSetupSectionLabel(
            "GOOGLE TASKS"
        )

        TaskSetupButton(
            text =
                when {
                    connecting ->
                        "connecting..."

                    provider == TaskProvider.GOOGLE_TASKS ->
                        "✓ connected · reconnect"

                    taskStore.isGoogleTasksConnected() ->
                        "switch to google tasks"

                    else ->
                        "connect google tasks"
                },
            highlighted =
                provider == TaskProvider.GOOGLE_TASKS,
            onClick = {
                when {
                    connecting -> Unit

                    /*
                     * Already connected before: switch straight back
                     * without signing in again.
                     */
                    provider != TaskProvider.GOOGLE_TASKS &&
                            taskStore.isGoogleTasksConnected() ->
                        googleTasksConnected()

                    else ->
                        connectGoogleTasks()
                }
            }
        )

        Spacer(
            modifier =
                Modifier.height(
                    14.dp
                )
        )

        TaskSetupSectionLabel(
            "OR TODOIST"
        )

        Text(
            text =
                if (
                    taskStore.hasTodoistToken()
                ) {
                    "your token is saved. paste a new one only to replace it"
                } else {
                    "paste your api token (todoist → settings → integrations → developer), then press enter"
                },
            color =
                SecondaryText,
            fontSize =
                8.sp,
            lineHeight =
                10.sp,
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
                todoistToken,
            onValueChange = {
                todoistToken =
                    it.replace(
                        "\n",
                        ""
                    )
            },
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
                ),
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
                    .onPreviewKeyEvent { event ->
                        if (
                            event.type ==
                            KeyEventType.KeyDown &&
                            event.key ==
                            Key.Enter
                        ) {
                            saveTodoistToken()
                            true
                        } else {
                            false
                        }
                    }
        )

        Spacer(
            modifier =
                Modifier.height(
                    6.dp
                )
        )

        TaskSetupButton(
            text =
                when {
                    provider == TaskProvider.TODOIST ->
                        "✓ todoist connected · save new token"

                    taskStore.hasTodoistToken() ->
                        "switch to todoist (token saved)"

                    else ->
                        "use todoist"
                },
            highlighted =
                provider == TaskProvider.TODOIST,
            onClick = {
                saveTodoistToken()
            }
        )

        if (
            provider != TaskProvider.NONE
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        14.dp
                    )
            )

            TaskSetupButton(
                text =
                    "turn tasks off",
                highlighted =
                    false,
                onClick = {
                    turnOff()
                }
            )
        }

        if (
            notice.isNotBlank()
        ) {

            Spacer(
                modifier =
                    Modifier.height(
                        10.dp
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
private fun TaskSetupSectionLabel(
    text: String
) {
    Text(
        text =
            text,
        color =
            SecondaryText,
        fontSize =
            8.sp,
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
private fun TaskSetupButton(
    text: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(
                    0.75.dp,
                    if (
                        highlighted
                    ) {
                        AccentOrange
                    } else {
                        BorderGray
                    },
                    RoundedCornerShape(
                        8.dp
                    )
                )
                .clickable {
                    onClick()
                }
                .padding(
                    horizontal =
                        10.dp,
                    vertical =
                        9.dp
                ),
        horizontalArrangement =
            Arrangement.Start,
        verticalAlignment =
            Alignment.CenterVertically
    ) {
        Text(
            text =
                text,
            color =
                if (
                    highlighted
                ) {
                    AccentOrange
                } else {
                    PrimaryText
                },
            fontSize =
                10.sp,
            fontFamily =
                InterfaceFont,
            fontWeight =
                FontWeight.Medium
        )
    }
}
