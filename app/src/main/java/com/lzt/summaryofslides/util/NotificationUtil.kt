package com.lzt.summaryofslides.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationUtil {
    const val DailyChannelId = "daily_report"

    fun ensureDailyChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(DailyChannelId) != null) return
        nm.createNotificationChannel(
            NotificationChannel(
                DailyChannelId,
                "优化论文日报",
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }
}
