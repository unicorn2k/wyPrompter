package com.wyprompter.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {

    const val CHANNEL_ID = "prompter_overlay"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "提词器悬浮窗",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "悬浮提词运行中"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }
}