package com.lzt.summaryofslides.ui.daily

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.lzt.summaryofslides.worker.DailySyncWorker
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object DailyWorkScheduler {
    private const val WorkName = "daily-sync"

    fun schedule(context: Context, hour: Int, minute: Int) {
        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        val req =
            PeriodicWorkRequestBuilder<DailySyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(computeInitialDelayMs(hour, minute), TimeUnit.MILLISECONDS)
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WorkName, ExistingPeriodicWorkPolicy.UPDATE, req)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WorkName)
    }

    private fun computeInitialDelayMs(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now()
        var next =
            now.withHour(hour.coerceIn(0, 23))
                .withMinute(minute.coerceIn(0, 59))
                .withSecond(0)
                .withNano(0)
        if (!next.isAfter(now.plusMinutes(1))) {
            next = next.plusDays(1)
        }
        return Duration.between(now, next).toMillis().coerceAtLeast(0)
    }
}

