package com.forrest.titanlauncher.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class GeminiApiKeyStore(
    context: Context
) {

    private val prefs =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    init {
        migrateLegacyPlaintextKeyIfNeeded()
    }

    fun saveKey(
        key: String
    ) {
        val trimmedKey =
            key.trim()

        if (
            trimmedKey.isBlank()
        ) {
            clearKey()
            return
        }

        val cipher =
            Cipher.getInstance(
                TRANSFORMATION
            )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateSecretKey()
        )

        val encryptedBytes =
            cipher.doFinal(
                trimmedKey.toByteArray(
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

        prefs.edit()
            .putString(
                KEY_ENCRYPTED_VALUE,
                encryptedValue
            )
            .putString(
                KEY_IV,
                ivValue
            )
            .remove(
                LEGACY_PLAINTEXT_KEY
            )
            .apply()
    }

    fun getKey(): String {

        val encryptedValue =
            prefs.getString(
                KEY_ENCRYPTED_VALUE,
                null
            )
                ?: return ""

        val ivValue =
            prefs.getString(
                KEY_IV,
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
                getOrCreateSecretKey(),
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

            clearEncryptedValues()

            ""
        }
    }

    fun hasKey(): Boolean {

        return getKey()
            .isNotBlank()
    }

    fun clearKey() {

        prefs.edit()
            .remove(
                KEY_ENCRYPTED_VALUE
            )
            .remove(
                KEY_IV
            )
            .remove(
                LEGACY_PLAINTEXT_KEY
            )
            .apply()
    }

    private fun migrateLegacyPlaintextKeyIfNeeded() {

        val encryptedValue =
            prefs.getString(
                KEY_ENCRYPTED_VALUE,
                null
            )

        if (
            !encryptedValue.isNullOrBlank()
        ) {

            prefs.edit()
                .remove(
                    LEGACY_PLAINTEXT_KEY
                )
                .apply()

            return
        }

        val legacyKey =
            prefs.getString(
                LEGACY_PLAINTEXT_KEY,
                null
            )
                ?.trim()
                ?: return

        if (
            legacyKey.isBlank()
        ) {

            prefs.edit()
                .remove(
                    LEGACY_PLAINTEXT_KEY
                )
                .apply()

            return
        }

        try {

            saveKey(
                legacyKey
            )

        } catch (
            _: Exception
        ) {

            prefs.edit()
                .remove(
                    LEGACY_PLAINTEXT_KEY
                )
                .apply()
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {

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
                KEY_ALIAS,
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
                KEY_ALIAS,
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

    private fun clearEncryptedValues() {

        prefs.edit()
            .remove(
                KEY_ENCRYPTED_VALUE
            )
            .remove(
                KEY_IV
            )
            .apply()
    }

    companion object {

        private const val PREFS_NAME =
            "titan_gemini"

        private const val LEGACY_PLAINTEXT_KEY =
            "api_key"

        private const val KEY_ENCRYPTED_VALUE =
            "encrypted_api_key"

        private const val KEY_IV =
            "encrypted_api_key_iv"

        private const val KEY_ALIAS =
            "prompt_launcher_gemini_key"

        private const val ANDROID_KEYSTORE =
            "AndroidKeyStore"

        private const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        private const val GCM_TAG_LENGTH_BITS =
            128
    }
}