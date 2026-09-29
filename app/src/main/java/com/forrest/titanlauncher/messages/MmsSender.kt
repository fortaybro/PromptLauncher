package com.forrest.titanlauncher.messages

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.forrest.titanlauncher.normalizePhoneNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream


/*
 * MMS SEND
 *
 * The mirror of MmsPdu.kt: builds an M-Send.req rather than reading
 * one. Encoding is less forgiving than decoding — a decoder can skip
 * a field it does not understand, but every byte an encoder writes
 * has to be right or the carrier rejects the message with no useful
 * error.
 *
 * Text only. Attachments would mean more part types and a SMIL
 * layout part; a group text needs neither.
 */


internal const val MmsSentAction =
    "com.forrest.titanlauncher.MMS_SENT"

internal const val MmsExtraThreadKey =
    "mms_thread_key"

internal const val MmsExtraBody =
    "mms_body"

/* PDU type */
private const val TypeSendReq = 0x80

/* Header field codes, with the well-known bit set */
private const val HeaderMessageType = 0x8C
private const val HeaderTransactionId = 0x98
private const val HeaderVersion = 0x8D
private const val HeaderFrom = 0x89
private const val HeaderTo = 0x97
private const val HeaderContentType = 0x84

/* From field: let the carrier insert the sender's own number */
private const val InsertAddressToken = 0x81

/* MMS 1.1 as a short integer */
private const val MmsVersion11 = 0x91

/* Well-known content types, with the high bit set */
private const val TypeMultipartMixed = 0xB3
private const val TypeTextPlain = 0x83

/* charset parameter, and UTF-8 as its value */
private const val ParamCharset = 0x81
private const val CharsetUtf8 = 0xEA


private class PduWriter {

    private val out =
        ByteArrayOutputStream()

    fun byte(
        value: Int
    ) {
        out.write(
            value and 0xFF
        )
    }

    fun bytes(
        value: ByteArray
    ) {
        out.write(
            value,
            0,
            value.size
        )
    }

    /*
     * NUL-terminated ASCII.
     */
    fun textString(
        value: String
    ) {

        bytes(
            value.toByteArray(
                Charsets.UTF_8
            )
        )

        byte(
            0x00
        )
    }

    /*
     * Seven bits per byte, high bit set on all but the last.
     */
    fun uintvar(
        value: Int
    ) {

        if (
            value == 0
        ) {
            byte(
                0
            )
            return
        }

        val parts =
            mutableListOf<Int>()

        var remaining =
            value

        while (
            remaining > 0
        ) {
            parts.add(
                remaining and 0x7F
            )

            remaining =
                remaining shr 7
        }

        parts
            .reversed()
            .forEachIndexed { index, part ->

                byte(
                    if (
                        index ==
                        parts.lastIndex
                    ) {
                        part
                    } else {
                        part or 0x80
                    }
                )
            }
    }

    fun toByteArray(): ByteArray =
        out.toByteArray()

    val size: Int
        get() = out.size()
}


internal object MmsPduBuilder {

    /*
     * Addresses go out as "+15551234567/TYPE=PLMN", which is the form
     * every carrier expects even though the suffix looks redundant.
     */
    private fun addressFor(
        number: String
    ): String {

        val digits =
            number.filter {
                it.isDigit()
            }

        val e164 =
            when {

                number.startsWith("+") ->
                    number.substringBefore("/")

                digits.length == 10 ->
                    "+1" + digits

                digits.length == 11 &&
                        digits.startsWith("1") ->
                    "+" + digits

                else ->
                    "+" + digits
            }

        return e164 + "/TYPE=PLMN"
    }

    fun buildSendRequest(
        recipients: List<String>,
        body: String,
        transactionId: String
    ): ByteArray {

        val writer =
            PduWriter()

        writer.byte(
            HeaderMessageType
        )
        writer.byte(
            TypeSendReq
        )

        writer.byte(
            HeaderTransactionId
        )
        writer.textString(
            transactionId
        )

        writer.byte(
            HeaderVersion
        )
        writer.byte(
            MmsVersion11
        )

        /*
         * From: one byte of value, the insert-address token. The
         * carrier fills in the real number, which is what lets this
         * work without knowing our own MSISDN.
         */
        writer.byte(
            HeaderFrom
        )
        writer.byte(
            0x01
        )
        writer.byte(
            InsertAddressToken
        )

        recipients.forEach { number ->

            writer.byte(
                HeaderTo
            )

            writer.textString(
                addressFor(
                    number
                )
            )
        }

        writer.byte(
            HeaderContentType
        )
        writer.byte(
            TypeMultipartMixed
        )

        /*
         * One part: the text.
         *
         * Its content type is length-prefixed so the charset can ride
         * along, which is what stops non-ASCII characters arriving as
         * rubbish.
         */
        val text =
            body.toByteArray(
                Charsets.UTF_8
            )

        val contentType =
            PduWriter()
                .apply {

                    byte(
                        0x03
                    )

                    byte(
                        TypeTextPlain
                    )

                    byte(
                        ParamCharset
                    )

                    byte(
                        CharsetUtf8
                    )
                }
                .toByteArray()

        writer.uintvar(
            1
        )

        writer.uintvar(
            contentType.size
        )

        writer.uintvar(
            text.size
        )

        writer.bytes(
            contentType
        )

        writer.bytes(
            text
        )

        return writer.toByteArray()
    }
}


internal object MmsSender {

    fun send(
        context: Context,
        recipients: List<String>,
        body: String,
        threadKey: String
    ): Boolean {

        if (
            recipients.isEmpty() ||
            body.isBlank()
        ) {
            return false
        }

        val transactionId =
            "T" + System.currentTimeMillis()

        val pdu =
            runCatching {
                MmsPduBuilder.buildSendRequest(
                    recipients =
                        recipients,
                    body =
                        body,
                    transactionId =
                        transactionId
                )
            }
                .getOrNull()
                ?: return false

        val fileName =
            "send_" +
                    transactionId +
                    ".pdu"

        val file =
            MmsDownloader.fileFor(
                context,
                fileName
            )

        val written =
            runCatching {
                file.writeBytes(
                    pdu
                )
                true
            }
                .getOrDefault(
                    false
                )

        if (
            !written
        ) {
            return false
        }

        val contentUri =
            runCatching {
                MmsDownloader.contentUriFor(
                    context,
                    file
                )
            }
                .getOrNull()
                ?: return false

        val sentIntent =
            Intent(
                MmsSentAction
            )
                .apply {

                    setPackage(
                        context.packageName
                    )

                    putExtra(
                        MmsExtraFileName,
                        fileName
                    )

                    putExtra(
                        MmsExtraThreadKey,
                        threadKey
                    )

                    putExtra(
                        MmsExtraBody,
                        body
                    )
                }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                transactionId.hashCode(),
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_MUTABLE
            )


        return runCatching {

            SmsManager
                .getDefault()
                .sendMultimediaMessage(
                    context,
                    contentUri,
                    null,
                    null,
                    pendingIntent
                )

            true
        }
            .onFailure { error ->

            }
            .getOrDefault(
                false
            )
    }
}


/*
 * The carrier accepted or rejected it. Either way the message is
 * stored, so a failure is visible in the thread rather than silently
 * vanishing.
 */
class MmsSentReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val threadKey =
            intent.getStringExtra(
                MmsExtraThreadKey
            )
                ?: return

        val body =
            intent.getStringExtra(
                MmsExtraBody
            )
                ?: return

        val ok =
            resultCode ==
                    android.app.Activity.RESULT_OK


        intent
            .getStringExtra(
                MmsExtraFileName
            )
            ?.let { name ->

                runCatching {
                    MmsDownloader
                        .fileFor(
                            context,
                            name
                        )
                        .delete()
                }
            }

        val pendingResult =
            goAsync()

        CoroutineScope(
            Dispatchers.IO
        )
            .launch {

                runCatching {

                    SmsDatabase
                        .getInstance(
                            context
                        )
                        .smsDao()
                        .insert(
                            SmsMessage(
                                phoneNumber =
                                    threadKey,
                                body =
                                    if (
                                        ok
                                    ) {
                                        body
                                    } else {
                                        body + "\n[not delivered]"
                                    },
                                timestamp =
                                    System.currentTimeMillis(),
                                incoming =
                                    false,
                                isRead =
                                    true,
                                threadKey =
                                    threadKey,
                                isGroup =
                                    normalizePhoneNumber(
                                        threadKey
                                    ) != threadKey
                            )
                        )
                }

                pendingResult.finish()
            }
    }
}