package com.forrest.titanlauncher

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.TelephonyManager
import com.forrest.titanlauncher.messages.SmsMessage


/*
 * OWN NUMBER
 *
 * Group titles should name the other people on the thread, not the
 * person reading it, so the launcher has to know which participant is
 * you. That is surprisingly awkward on Android.
 *
 * Three sources, in order of reliability:
 *
 *   1. A number resolved earlier and remembered.
 *   2. TelephonyManager, which needs READ_SMS and still returns null
 *      on plenty of carriers and every eSIM-only setup.
 *   3. Inference: across every group thread, your number appears in
 *      the participant list but never as the sender of an incoming
 *      message. Once exactly one candidate remains it is yours, and
 *      it gets remembered so the guesswork happens once.
 */

private const val OwnNumberPrefsName =
    "prompt_launcher_identity"

private const val OwnNumberKey =
    "own_number"


class OwnNumberStore(
    private val context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            OwnNumberPrefsName,
            Context.MODE_PRIVATE
        )

    fun get(): String? {

        val stored =
            preferences
                .getString(
                    OwnNumberKey,
                    null
                )
                ?.takeIf {
                    it.isNotBlank()
                }

        if (
            stored != null
        ) {
            return stored
        }

        val detected =
            detectFromTelephony()

        if (
            detected != null
        ) {
            set(
                detected
            )
        }

        return detected
    }

    fun set(
        number: String
    ) {

        val normalized =
            normalizePhoneNumber(
                number
            )

        if (
            normalized.isBlank()
        ) {
            return
        }

        preferences
            .edit()
            .putString(
                OwnNumberKey,
                normalized
            )
            .apply()
    }

    @SuppressLint("MissingPermission")
    private fun detectFromTelephony(): String? {

        return runCatching {

            val manager =
                context.getSystemService(
                    Context.TELEPHONY_SERVICE
                ) as? TelephonyManager

            manager
                ?.line1Number
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.let {
                    normalizePhoneNumber(
                        it
                    )
                }
                ?.takeIf {
                    it.isNotBlank()
                }
        }
            .getOrNull()
    }

    /*
     * Called wherever the full message list is already in hand. Cheap
     * once the number is known, since get() short-circuits.
     */
    fun resolve(
        allMessages: List<SmsMessage>
    ): String? {

        get()
            ?.let {
                return it
            }

        val inferred =
            inferOwnNumber(
                allMessages
            )
                ?: return null

        set(
            inferred
        )

        return inferred
    }
}


/*
 * Your number is in every group's participant list and is never the
 * sender of an incoming message. Returns null while more than one
 * candidate still fits, which resolves itself as more people write.
 */
internal fun inferOwnNumber(
    allMessages: List<SmsMessage>
): String? {

    val groupMessages =
        allMessages.filter {
            it.isGroup &&
                    it.threadKey.isNotBlank()
        }

    if (
        groupMessages.isEmpty()
    ) {
        return null
    }

    val incomingSenders =
        allMessages
            .filter {
                it.incoming
            }
            .mapNotNull {
                it.senderNumber
                    ?: it.phoneNumber
            }
            .map {
                normalizePhoneNumber(
                    it
                )
            }
            .filter {
                it.isNotBlank()
            }
            .toSet()

    /*
     * Intersect the participants of every group thread: only you are
     * guaranteed to be on all of them.
     */
    val threads =
        groupMessages
            .map { message ->

                message.threadKey
                    .split(",")
                    .map {
                        it.trim()
                    }
                    .filter {
                        it.isNotBlank()
                    }
                    .toSet()
            }
            .distinct()

    val shared =
        threads
            .reduce { accumulated, participants ->

                accumulated.intersect(
                    participants
                )
            }

    val candidates =
        shared - incomingSenders

    return candidates
        .singleOrNull()
}


/*
 * Names a group by the people on it, leaving the reader out.
 */
internal fun groupTitleFor(
    participants: List<String>,
    ownNumber: String?,
    nameFor: (String) -> String?
): String {

    val others =
        participants
            .filter { number ->

                ownNumber == null ||
                        normalizePhoneNumber(
                            number
                        ) != ownNumber
            }
            .ifEmpty {
                participants
            }

    return others
        .map { number ->

            (
                    nameFor(
                        number
                    )
                        ?: number
                    )
                .substringBefore(" ")
        }
        .joinToString(
            separator = ", "
        )
}