package com.lzt.summaryofslides.worker

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lzt.summaryofslides.MainActivity
import com.lzt.summaryofslides.data.AppContainer
import com.lzt.summaryofslides.util.NotificationUtil
import kotlinx.coroutines.flow.first

class DailySyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val store = AppContainer.dailySettingsStore
        val repo = AppContainer.dailyRepository
        val s = store.settings.first()
        if (!s.enableNotifications) return Result.success()
        if (s.githubOwner.isBlank() || s.githubRepo.isBlank()) return Result.success()

        return runCatching {
            val idx = repo.refreshIndex(s)
            val latest = idx.dates.sorted().lastOrNull().orEmpty()
            if (latest.isBlank() || latest == s.lastNotifiedDate) return Result.success()

            val report = repo.refreshReport(s, latest)
            postNotification(latest, report.papers.sortedByDescending { it.score }.take(3).map { it.title }.filter { it.isNotBlank() })
            store.update(lastNotifiedDate = latest)
            Result.success()
        }.getOrElse { e ->
            val msg = e.message.orEmpty()
            if (msg.contains("HTTP 404") || msg.contains("HTTP 401") || msg.contains("HTTP 403")) {
                Result.success()
            } else {
                Result.retry()
            }
        }
    }

    private fun postNotification(reportDate: String, topTitles: List<String>) {
        NotificationUtil.ensureDailyChannel(applicationContext)
        val title = "优化论文日报：$reportDate"
        val text =
            when {
                topTitles.isEmpty() -> "有新的日报"
                topTitles.size == 1 -> topTitles.first()
                else -> topTitles.joinToString("；").take(120)
            }
        val intent =
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        val pi =
            PendingIntent.getActivity(
                applicationContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notification =
            NotificationCompat.Builder(applicationContext, NotificationUtil.DailyChannelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build()
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { nm.notify(reportDate.hashCode(), notification) }
    }
}

