package com.forrest.titanlauncher.messages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.forrest.titanlauncher.MessageNotifications
import com.forrest.titanlauncher.contacts.ContactRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val SEVEN_DAYS_MS =
    7L * 24L * 60L * 60L * 1000L

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {


        val action =
            intent.action

        if (
            action != Telephony.Sms.Intents.SMS_DELIVER_ACTION &&
            action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        ) {


            return
        }

        val messages =
            Telephony.Sms.Intents
                .getMessagesFromIntent(intent)


        if (messages.isEmpty()) {


            return
        }

        val pendingResult =
            goAsync()

        val database =
            SmsDatabase.getInstance(
                context
            )

        val dao =
            database.smsDao()

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            try {

                /*
                 * A long text arrives as several PDU parts in a
                 * single broadcast. Inserting each part produced the
                 * message chopped into chunks in the thread, so the
                 * parts are joined back into the one message the
                 * sender actually wrote.
                 *
                 * Ordering is the order the carrier delivered them,
                 * which is the concatenation order.
                 */
                val sender =
                    messages
                        .firstNotNullOfOrNull {
                            it.displayOriginatingAddress
                        }
                        ?: "Unknown"

                val body =
                    messages
                        .joinToString(
                            separator = ""
                        ) {
                            it.messageBody
                                ?: ""
                        }

                /*
                 * timestampMillis is the service centre's clock, not
                 * the phone's, and carries its own timezone offset.
                 * Trust it only when it is in the past and recent;
                 * otherwise use the moment of delivery.
                 */
                val receivedAt =
                    System.currentTimeMillis()

                val reportedAt =
                    messages
                        .first()
                        .timestampMillis

                val timestamp =
                    if (
                        reportedAt in
                        (receivedAt - SEVEN_DAYS_MS)..receivedAt
                    ) {
                        reportedAt
                    } else {
                        receivedAt
                    }


                val rowId =
                    dao.insert(
                        SmsMessage(
                            phoneNumber =
                                sender,

                            body =
                                body,

                            timestamp =
                                timestamp,

                            incoming =
                                true,

                            isRead =
                                false,

                            /*
                             * One-to-one threads are keyed on the
                             * other person's number, so SMS and MMS
                             * conversations share one namespace.
                             */
                            threadKey =
                                threadKeyFor(
                                    listOf(
                                        sender
                                    )
                                ),

                            senderNumber =
                                sender
                        )
                    )


                /*
                 * As the default SMS app nothing else will post for
                 * this, so the shade entry and its sound are ours to
                 * raise. Suppressed when that conversation is already
                 * on screen; the in-app sound covers that case.
                 */
                val displayName =
                    runCatching {
                        ContactRepository(
                            context
                        )
                            .findContactNameByPhoneNumber(
                                sender
                            )
                    }
                        .getOrNull()
                        ?: sender

                MessageNotifications.notifyIncoming(
                    context = context,
                    phoneNumber = sender,
                    displayName = displayName,
                    body = body
                )


            } catch (e: Exception) {


            } finally {

                pendingResult.finish()
            }
        }
    }
}