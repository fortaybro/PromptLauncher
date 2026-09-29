package com.forrest.titanlauncher.todoist

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.suspendCancellableCoroutine
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resume

enum class TaskProvider {
    NONE,
    GOOGLE_TASKS,
    TODOIST
}

class TodoistTokenStore(
    context: Context
) {

    internal val appContext =
        context.applicationContext

    private val taskPreferences =
        appContext.getSharedPreferences(
            TASK_PREFS,
            Context.MODE_PRIVATE
        )

    private val legacyTodoistPreferences =
        appContext.getSharedPreferences(
            "todoist_prefs",
            Context.MODE_PRIVATE
        )

    private val legacyTodoistPreferencesTwo =
        appContext.getSharedPreferences(
            "todoist",
            Context.MODE_PRIVATE
        )

    private val authorizationClient =
        Identity.getAuthorizationClient(
            appContext
        )

    init {
        migrateLegacyTodoistTokenIfNeeded()
    }

    // ---------------------------------------------------------
    // TODOIST
    // ---------------------------------------------------------

    fun saveToken(
        token: String
    ) {
        val cleaned =
            token.trim()

        if (
            cleaned.isBlank()
        ) {
            clearToken()
            return
        }

        saveEncryptedTodoistToken(
            token = cleaned,
            selectTodoistProvider = true
        )

        clearLegacyPlaintextTodoistTokens()
    }

    fun getToken(): String {

        val encryptedValue =
            taskPreferences.getString(
                KEY_TODOIST_TOKEN_ENCRYPTED,
                null
            )
                ?: return ""

        val ivValue =
            taskPreferences.getString(
                KEY_TODOIST_TOKEN_IV,
                null
            )
                ?: return ""

        return try {

            val encryptedBytes =
                Base64.decode(
                    encryptedValue,
                    Base64.NO_WRAP
                )

            val ivBytes =
                Base64.decode(
                    ivValue,
                    Base64.NO_WRAP
                )

            val cipher =
                Cipher.getInstance(
                    TRANSFORMATION
                )

            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateTodoistSecretKey(),
                GCMParameterSpec(
                    GCM_TAG_LENGTH_BITS,
                    ivBytes
                )
            )

            cipher.doFinal(
                encryptedBytes
            )
                .toString(
                    Charsets.UTF_8
                )

        } catch (
            _: Exception
        ) {

            clearEncryptedTodoistToken()

            ""
        }
    }

    fun token(): String {
        return getToken()
    }

    fun getApiToken(): String {
        return getToken()
    }

    fun clearToken() {

        val wasTodoistProvider =
            getTaskProvider() ==
                    TaskProvider.TODOIST

        clearEncryptedTodoistToken()
        clearLegacyPlaintextTodoistTokens()

        if (
            wasTodoistProvider
        ) {
            setTaskProvider(
                TaskProvider.NONE
            )
        }
    }

    // ---------------------------------------------------------
    // TASK PROVIDER
    // ---------------------------------------------------------

    fun getTaskProvider(): TaskProvider {

        val stored =
            taskPreferences.getString(
                KEY_PROVIDER,
                null
            )

        if (
            !stored.isNullOrBlank()
        ) {
            return runCatching {
                TaskProvider.valueOf(
                    stored
                )
            }.getOrDefault(
                TaskProvider.NONE
            )
        }

        return if (
            getToken().isNotBlank()
        ) {
            TaskProvider.TODOIST
        } else {
            TaskProvider.NONE
        }
    }

    private fun setTaskProvider(
        provider: TaskProvider
    ) {
        taskPreferences
            .edit()
            .putString(
                KEY_PROVIDER,
                provider.name
            )
            .apply()
    }

    fun selectTodoist() {
        setTaskProvider(
            TaskProvider.TODOIST
        )
    }

    fun selectGoogleTasks() {

        taskPreferences
            .edit()
            .putString(
                KEY_PROVIDER,
                TaskProvider.GOOGLE_TASKS.name
            )
            .putBoolean(
                KEY_GOOGLE_TASKS_CONNECTED,
                true
            )
            .apply()
    }

    fun clearTaskProvider() {

        taskPreferences
            .edit()
            .putString(
                KEY_PROVIDER,
                TaskProvider.NONE.name
            )
            .putBoolean(
                KEY_GOOGLE_TASKS_CONNECTED,
                false
            )
            .apply()
    }

    fun isGoogleTasksConnected(): Boolean {

        return taskPreferences.getBoolean(
            KEY_GOOGLE_TASKS_CONNECTED,
            false
        )
    }

    fun hasToken(): Boolean {

        return when (
            getTaskProvider()
        ) {

            TaskProvider.GOOGLE_TASKS ->
                isGoogleTasksConnected()

            TaskProvider.TODOIST ->
                getToken().isNotBlank()

            TaskProvider.NONE ->
                false
        }
    }

    // ---------------------------------------------------------
    // GOOGLE TASKS
    // ---------------------------------------------------------

    suspend fun getGoogleTasksAccessToken(): String? {

        val request =
            AuthorizationRequest
                .builder()
                .setRequestedScopes(
                    listOf(
                        Scope(
                            GOOGLE_TASKS_SCOPE
                        )
                    )
                )
                .build()

        return suspendCancellableCoroutine {
                continuation ->

            authorizationClient
                .authorize(
                    request
                )
                .addOnSuccessListener {
                        result ->

                    if (
                        !continuation.isActive
                    ) {
                        return@addOnSuccessListener
                    }

                    if (
                        result.hasResolution()
                    ) {
                        continuation.resume(
                            null
                        )

                        return@addOnSuccessListener
                    }

                    val accessToken =
                        result.accessToken
                            ?.trim()
                            ?.takeIf {
                                it.isNotBlank()
                            }

                    if (
                        accessToken != null
                    ) {

                        taskPreferences
                            .edit()
                            .putBoolean(
                                KEY_GOOGLE_TASKS_CONNECTED,
                                true
                            )
                            .apply()
                    }

                    continuation.resume(
                        accessToken
                    )
                }
                .addOnFailureListener {

                    if (
                        continuation.isActive
                    ) {
                        continuation.resume(
                            null
                        )
                    }
                }
        }
    }

    // ---------------------------------------------------------
    // ENCRYPTION
    // ---------------------------------------------------------

    private fun saveEncryptedTodoistToken(
        token: String,
        selectTodoistProvider: Boolean
    ) {

        val cipher =
            Cipher.getInstance(
                TRANSFORMATION
            )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateTodoistSecretKey()
        )

        val encryptedBytes =
            cipher.doFinal(
                token.toByteArray(
                    Charsets.UTF_8
                )
            )

        val encryptedValue =
            Base64.encodeToString(
                encryptedBytes,
                Base64.NO_WRAP
            )

        val ivValue =
            Base64.encodeToString(
                cipher.iv,
                Base64.NO_WRAP
            )

        val editor =
            taskPreferences
                .edit()
                .putString(
                    KEY_TODOIST_TOKEN_ENCRYPTED,
                    encryptedValue
                )
                .putString(
                    KEY_TODOIST_TOKEN_IV,
                    ivValue
                )
                .remove(
                    LEGACY_CURRENT_TODOIST_TOKEN
                )

        if (
            selectTodoistProvider
        ) {
            editor.putString(
                KEY_PROVIDER,
                TaskProvider.TODOIST.name
            )
        }

        editor.apply()
    }

    private fun getOrCreateTodoistSecretKey(): SecretKey {

        val keyStore =
            KeyStore.getInstance(
                ANDROID_KEYSTORE
            ).apply {
                load(
                    null
                )
            }

        val existingKey =
            keyStore.getKey(
                TODOIST_KEY_ALIAS,
                null
            ) as? SecretKey

        if (
            existingKey != null
        ) {
            return existingKey
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

        val keySpec =
            KeyGenParameterSpec.Builder(
                TODOIST_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                        KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setUserAuthenticationRequired(
                    false
                )
                .build()

        keyGenerator.init(
            keySpec
        )

        return keyGenerator.generateKey()
    }

    private fun migrateLegacyTodoistTokenIfNeeded() {

        val existingEncrypted =
            taskPreferences.getString(
                KEY_TODOIST_TOKEN_ENCRYPTED,
                null
            )

        if (
            !existingEncrypted.isNullOrBlank()
        ) {
            clearLegacyPlaintextTodoistTokens()
            return
        }

        val legacyToken =
            listOf(
                taskPreferences.getString(
                    LEGACY_CURRENT_TODOIST_TOKEN,
                    null
                ),
                legacyTodoistPreferences.getString(
                    "todoist_token",
                    null
                ),
                legacyTodoistPreferences.getString(
                    "token",
                    null
                ),
                legacyTodoistPreferencesTwo.getString(
                    "todoist_token",
                    null
                ),
                legacyTodoistPreferencesTwo.getString(
                    "token",
                    null
                )
            )
                .firstOrNull {
                    !it.isNullOrBlank()
                }
                ?.trim()

        if (
            legacyToken.isNullOrBlank()
        ) {
            clearLegacyPlaintextTodoistTokens()
            return
        }

        try {

            saveEncryptedTodoistToken(
                token = legacyToken,
                selectTodoistProvider = false
            )

            clearLegacyPlaintextTodoistTokens()

        } catch (
            _: Exception
        ) {

            /*
             * Security takes priority over retaining a legacy
             * plaintext credential. The user can reconnect
             * Todoist if Android Keystore creation fails.
             */
            clearLegacyPlaintextTodoistTokens()
        }
    }

    private fun clearEncryptedTodoistToken() {

        taskPreferences
            .edit()
            .remove(
                KEY_TODOIST_TOKEN_ENCRYPTED
            )
            .remove(
                KEY_TODOIST_TOKEN_IV
            )
            .apply()
    }

    private fun clearLegacyPlaintextTodoistTokens() {

        taskPreferences
            .edit()
            .remove(
                LEGACY_CURRENT_TODOIST_TOKEN
            )
            .apply()

        legacyTodoistPreferences
            .edit()
            .remove(
                "todoist_token"
            )
            .remove(
                "token"
            )
            .apply()

        legacyTodoistPreferencesTwo
            .edit()
            .remove(
                "todoist_token"
            )
            .remove(
                "token"
            )
            .apply()
    }

    companion object {

        const val GOOGLE_TASKS_SCOPE =
            "https://www.googleapis.com/auth/tasks"

        private const val TASK_PREFS =
            "prompt_task_integration"

        private const val KEY_PROVIDER =
            "task_provider"

        private const val KEY_GOOGLE_TASKS_CONNECTED =
            "google_tasks_connected"

        private const val LEGACY_CURRENT_TODOIST_TOKEN =
            "todoist_token"

        private const val KEY_TODOIST_TOKEN_ENCRYPTED =
            "encrypted_todoist_token"

        private const val KEY_TODOIST_TOKEN_IV =
            "encrypted_todoist_token_iv"

        private const val TODOIST_KEY_ALIAS =
            "prompt_launcher_todoist_key"

        private const val ANDROID_KEYSTORE =
            "AndroidKeyStore"

        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        private const val GCM_TAG_LENGTH_BITS =
            128
    }
}