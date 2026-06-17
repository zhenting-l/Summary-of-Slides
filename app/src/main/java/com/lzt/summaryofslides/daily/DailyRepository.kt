package com.lzt.summaryofslides.daily

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class DailyRepository(
    private val appContext: Context,
) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private fun dailyDir(): File = File(appContext.filesDir, "daily").apply { mkdirs() }

    private fun cacheFile(path: String): File = File(dailyDir(), path).apply { parentFile?.mkdirs() }

    suspend fun getCachedIndex(): DailyIndex? =
        withContext(Dispatchers.IO) {
            val f = cacheFile("index.json")
            if (!f.exists()) return@withContext null
            runCatching { json.decodeFromString(DailyIndex.serializer(), f.readText()) }.getOrNull()
        }

    suspend fun getCachedReport(reportDate: String): DailyReport? =
        withContext(Dispatchers.IO) {
            val f = cacheFile("reports/$reportDate.json")
            if (!f.exists()) return@withContext null
            runCatching { json.decodeFromString(DailyReport.serializer(), f.readText()) }.getOrNull()
        }

    suspend fun refreshIndex(settings: DailySettings): DailyIndex {
        val url = rawUrl(settings, "index.json")
        val text = httpGet(url)
        withContext(Dispatchers.IO) { cacheFile("index.json").writeText(text) }
        return json.decodeFromString(DailyIndex.serializer(), text)
    }

    suspend fun refreshReport(settings: DailySettings, reportDate: String): DailyReport {
        val url = rawUrl(settings, "reports/$reportDate.json")
        val text = httpGet(url)
        withContext(Dispatchers.IO) { cacheFile("reports/$reportDate.json").writeText(text) }
        return json.decodeFromString(DailyReport.serializer(), text)
    }

    private fun rawUrl(settings: DailySettings, path: String): String {
        val owner = settings.githubOwner.trim()
        val repo = settings.githubRepo.trim()
        val branch = settings.githubBranch.trim().ifBlank { "main" }
        return "https://raw.githubusercontent.com/$owner/$repo/$branch/$path"
    }

    private suspend fun httpGet(url: String): String =
        withContext(Dispatchers.IO) {
            val req =
                Request.Builder()
                    .url(url)
                    .get()
                    .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val body = resp.body?.string().orEmpty()
                    throw IllegalStateException("HTTP ${resp.code}: $body")
                }
                resp.body?.string() ?: ""
            }
        }
}

