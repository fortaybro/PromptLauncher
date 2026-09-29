package com.forrest.titanlauncher.messages

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsSender(
    private val context: Context
) {

    fun sendSms(
        phoneNumber: String,
        message: String,
        onResult: (
            success: Boolean,
            resultMessage: String
        ) -> Unit
    ) {

        val action =
            "${context.packageName}.SMS_SENT_${System.currentTimeMillis()}"

        val sentIntent =
            Intent(
                action
            ).apply {

                setPackage(
                    context.packageName
                )
            }

        val requestCode =
            (
                    System.currentTimeMillis() %
                            Int.MAX_VALUE
                    )
                .toInt()

        val sentPendingIntent =
            PendingIntent.getBroadcast(
                context,
                requestCode,
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        lateinit var sentReceiver:
                BroadcastReceiver

        var outgoingMessageSaved =
            false

        sentReceiver =
            object :
                BroadcastReceiver() {

                override fun onReceive(
                    receiverContext: Context?,
                    intent: Intent?
                ) {

                    val success =
                        resultCode ==
                                Activity.RESULT_OK

                    val resultText =
                        when (
                            resultCode
                        ) {

                            Activity.RESULT_OK ->
                                "SENT"

                            SmsManager.RESULT_ERROR_GENERIC_FAILURE ->
                                "GENERIC FAILURE"

                            SmsManager.RESULT_ERROR_NO_SERVICE ->
                                "NO CELL SERVICE"

                            SmsManager.RESULT_ERROR_NULL_PDU ->
                                "SMS DATA ERROR"

                            SmsManager.RESULT_ERROR_RADIO_OFF ->
                                "CELL RADIO OFF"

                            SmsManager.RESULT_ERROR_LIMIT_EXCEEDED ->
                                "SMS LIMIT EXCEEDED"

                            else ->
                                "ERROR CODE $resultCode"
                        }

                    if (
                        success &&
                        !outgoingMessageSaved
                    ) {

                        outgoingMessageSaved =
                            true

                        saveOutgoingMessage(
                            phoneNumber =
                                phoneNumber,

                            message =
                                message
                        )
                    }

                    try {

                        context.unregisterReceiver(
                            sentReceiver
                        )

                    } catch (
                        _: Exception
                    ) {
                    }

                    onResult(
                        success,
                        resultText
                    )
                }
            }

        ContextCompat.registerReceiver(
            context,
            sentReceiver,
            IntentFilter(
                action
            ),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        try {

            val smsManager =
                context.getSystemService(
                    SmsManager::class.java
                )

            if (
                message.length <= 160
            ) {

                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    sentPendingIntent,
                    null
                )

            } else {

                val parts =
                    smsManager.divideMessage(
                        message
                    )

                val sentIntents =
                    ArrayList<PendingIntent>()

                repeat(
                    parts.size
                ) {

                    sentIntents.add(
                        sentPendingIntent
                    )
                }

                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    sentIntents,
                    null
                )
            }

        } catch (
            e: Exception
        ) {

            try {

                context.unregisterReceiver(
                    sentReceiver
                )

            } catch (
                _: Exception
            ) {
            }

            onResult(
                false,
                e.message
                    ?: "UNKNOWN SMS ERROR"
            )
        }
    }

    private fun saveOutgoingMessage(
        phoneNumber: String,
        message: String
    ) {

        val database =
            SmsDatabase.getInstance(
                context
            )

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            try {

                database
                    .smsDao()
                    .insert(
                        SmsMessage(
                            phoneNumber =
                                phoneNumber,

                            body =
                                message,

                            timestamp =
                                System.currentTimeMillis(),

                            incoming =
                                false,

                            isRead =
                                true,

                            threadKey =
                                threadKeyFor(
                                    listOf(
                                        phoneNumber
                                    )
                                )
                        )
                    )

            } catch (
                _: Exception
            ) {
            }
        }
    }
}