package com.forrest.titanlauncher

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext


/*
 * MESSAGE SOUNDS
 *
 * SoundPool rather than MediaPlayer: the clips are decoded once and
 * held in memory, so a send plays the instant the key is pressed
 * instead of after a decode. MediaPlayer's setup cost is small but
 * audible on something this short.
 *
 * Clips live in res/raw as sms_send.wav, sms_receive.wav and
 * quick_reply_send.wav.
 */


internal class MessageSounds(
    context: Context
) {

    private val pool =
        SoundPool.Builder()
            .setMaxStreams(
                2
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    /*
                     * USAGE_NOTIFICATION rather than USAGE_MEDIA, so
                     * these follow the notification volume and stay
                     * quiet when the phone is silenced.
                     */
                    .setUsage(
                        AudioAttributes.USAGE_NOTIFICATION
                    )
                    .setContentType(
                        AudioAttributes.CONTENT_TYPE_SONIFICATION
                    )
                    .build()
            )
            .build()

    private val sendId =
        runCatching {
            pool.load(
                context,
                R.raw.sms_send,
                1
            )
        }
            .getOrDefault(
                0
            )

    private val receiveId =
        runCatching {
            pool.load(
                context,
                R.raw.sms_receive,
                1
            )
        }
            .getOrDefault(
                0
            )

    private val quickReplyId =
        runCatching {
            pool.load(
                context,
                R.raw.quick_reply_send,
                1
            )
        }
            .getOrDefault(
                0
            )

    private fun play(
        soundId: Int
    ) {

        if (
            soundId == 0
        ) {
            return
        }

        runCatching {
            pool.play(
                soundId,
                1f,
                1f,
                1,
                0,
                1f
            )
        }
    }

    fun playSend() {

        play(
            sendId
        )
    }

    fun playReceive() {

        play(
            receiveId
        )
    }

    /*
     * Answering from the bolt overlay rather than inside a thread, so
     * it gets its own clip.
     */
    fun playQuickReplySend() {

        play(
            quickReplyId
        )
    }

    fun release() {

        runCatching {
            pool.release()
        }
    }
}


@Composable
internal fun rememberMessageSounds(): MessageSounds {

    val context =
        LocalContext.current

    val sounds =
        remember {
            MessageSounds(
                context.applicationContext
            )
        }

    DisposableEffect(sounds) {

        onDispose {
            sounds.release()
        }
    }

    return sounds
}