package com.forrest.titanlauncher.messages

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.content.FileProvider
import com.forrest.titanlauncher.MessageNotifications
import com.forrest.titanlauncher.contacts.ContactRepository
import com.forrest.titanlauncher.normalizePhoneNumber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


/*
 * MMS RECEIVE
 *
 * Two steps, because that is how MMS works.
 *
 *   1. A WAP push arrives carrying an M-Notification.ind: who it is
 *      from, and a URL at the carrier's MMSC where the real message
 *      is waiting. It does not contain the message.
 *
 *   2. downloadMultimediaMessage fetches that URL into a file we
 *      provide, and broadcasts when it is done. The file holds an
 *      M-Retrieve.conf, which is the message.
 *
 * Read-only: nothing here sends. Group threads become visible; to
 * reply you still need another app or the send path built later.
 */


internal const val MmsDownloadedAction =
    "com.forrest.titanlauncher.MMS_DOWNLOADED"

internal const val MmsExtraFileName =
    "mms_file_name"

internal const val MmsExtraLocation =
    "mms_location"


/*
 * The platform's MMS service writes the downloaded PDU, so it needs a
 * content URI it can write to rather than a bare file path.
 */
class MmsFileProvider :
    FileProvider()


internal object MmsDownloader {

    fun fileFor(
        context: Context,
        name: String
    ): File {

        val directory =
            File(
                context.cacheDir,
                "mms"
            )
                .apply {
                    mkdirs()
                }

        return File(
            directory,
            name
        )
    }

    fun contentUriFor(
        context: Context,
        file: File
    ): Uri {

        return FileProvider.getUriForFile(
            context,
            context.packageName + ".mmsfileprovider",
            file
        )
    }
}


/*
 * Step one: the WAP push.
 */
class MmsReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val data =
            intent.getByteArrayExtra(
                "data"
            )
                ?: run {


                    return
                }

        val pdu =
            MmsPduParser.parse(
                data
            )

        if (
            pdu == null
        ) {


            return
        }


        val location =
            pdu.contentLocation

        if (
            location.isNullOrBlank()
        ) {


            return
        }

        val fileName =
            "mms_" +
                    System.currentTimeMillis() +
                    ".pdu"

        val file =
            MmsDownloader.fileFor(
                context,
                fileName
            )

        val contentUri =
            runCatching {
                MmsDownloader.contentUriFor(
                    context,
                    file
                )
            }
                .getOrNull()
                ?: run {


                    return
                }

        val downloadedIntent =
            Intent(
                MmsDownloadedAction
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
                        MmsExtraLocation,
                        location
                    )
                }

        val pendingIntent =
            android.app.PendingIntent.getBroadcast(
                context,
                fileName.hashCode(),
                downloadedIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                        android.app.PendingIntent.FLAG_MUTABLE
            )

        runCatching {

            SmsManager
                .getDefault()
                .downloadMultimediaMessage(
                    context,
                    location,
                    contentUri,
                    null,
                    pendingIntent
                )
        }
            .onFailure { error ->

            }
    }
}


/*
 * Step two: the download finished, so read and store it.
 */
class MmsDownloadedReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val fileName =
            intent.getStringExtra(
                MmsExtraFileName
            )
                ?: return

        val file =
            MmsDownloader.fileFor(
                context,
                fileName
            )

        if (
            !file.exists() ||
            file.length() == 0L
        ) {


            return
        }

        val bytes =
            runCatching {
                file.readBytes()
            }
                .getOrNull()
                ?: return

        val pdu =
            MmsPduParser.parse(
                bytes
            )

        if (
            pdu == null
        ) {


            return
        }

        val participants =
            pdu.participants()

        val sender =
            pdu.from
                ?: participants.firstOrNull()
                ?: return

        val threadKey =
            threadKeyFor(
                participants
            )

        val isGroup =
            participants.size > 2

        val body =
            pdu.bodyText()

        val timestamp =
            if (
                pdu.dateSeconds > 0
            ) {
                pdu.dateSeconds * 1000L
            } else {
                System.currentTimeMillis()
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
                                    sender,
                                body =
                                    body,
                                timestamp =
                                    timestamp,
                                incoming =
                                    true,
                                isRead =
                                    false,
                                threadKey =
                                    threadKey,
                                senderNumber =
                                    sender,
                                isGroup =
                                    isGroup
                            )
                        )

                    val displayName =
                        if (
                            isGroup
                        ) {
                            groupDisplayName(
                                context,
                                participants
                            )
                        } else {
                            ContactRepository(
                                context
                            )
                                .findContactNameByPhoneNumber(
                                    sender
                                )
                                ?: sender
                        }

                    MessageNotifications.notifyIncoming(
                        context = context,
                        phoneNumber = threadKey,
                        displayName = displayName,
                        body = body
                    )
                }
                    .onFailure { error ->

                    }

                runCatching {
                    file.delete()
                }

                pendingResult.finish()
            }
    }
}


/*
 * Everyone on the thread, normalised and sorted, so the same people
 * always resolve to the same conversation no matter who sent what.
 */
internal fun threadKeyFor(
    participants: List<String>
): String {

    return participants
        .map {
            normalizePhoneNumber(
                it
            )
        }
        .filter {
            it.isNotBlank()
        }
        .distinct()
        .sorted()
        .joinToString(
            separator = ","
        )
}


private fun groupDisplayName(
    context: Context,
    participants: List<String>
): String {

    val repository =
        ContactRepository(
            context
        )

    val names =
        participants
            .mapNotNull { number ->

                runCatching {
                    repository
                        .findContactNameByPhoneNumber(
                            number
                        )
                }
                    .getOrNull()
                    ?: number
            }
            .map {
                it.substringBefore(" ")
            }

    return when {

        names.isEmpty() ->
            "group message"

        names.size <= 3 ->
            names.joinToString(
                separator = ", "
            )

        else ->
            names
                .take(
                    2
                )
                .joinToString(
                    separator = ", "
                ) +
                    " +" +
                    (names.size - 2)
    }
}