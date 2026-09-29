package com.forrest.titanlauncher.messages

import android.app.Service
import android.content.Intent
import android.os.IBinder

class RespondViaMessageService : Service() {

    override fun onBind(
        intent: Intent?
    ): IBinder? {

        return null
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        // Android can invoke this service for
        // "respond with text" actions from
        // an incoming phone call.

        stopSelf(startId)

        return START_NOT_STICKY
    }
}