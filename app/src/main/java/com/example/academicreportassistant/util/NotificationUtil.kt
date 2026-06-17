package com.lzt.summaryofslides.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationUtil {
    const val ProgressChannelId = "progress"

    fun ensureChannel(context: Context) {
        ensureProgressChannel(context)
    }

    fun ensureProgressChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(ProgressChannelId) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                ProgressChannelId,
                "处理进度",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    fun buildProgressNotification(
        context: Context,
        title: String,
        text: String,
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, ProgressChannelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }
}
