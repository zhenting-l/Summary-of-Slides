package com.lzt.summaryofslides.daily

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dailyDataStore by preferencesDataStore(name = "daily_settings")

data class DailySettings(
    val githubOwner: String,
    val githubRepo: String,
    val githubBranch: String,
    val enableNotifications: Boolean,
    val notifyHour: Int,
    val notifyMinute: Int,
    val lastNotifiedDate: String,
)

class DailySettingsStore(private val context: Context) {
    private object Keys {
        val githubOwner = stringPreferencesKey("github_owner")
        val githubRepo = stringPreferencesKey("github_repo")
        val githubBranch = stringPreferencesKey("github_branch")
        val enableNotifications = booleanPreferencesKey("enable_notifications")
        val notifyHour = intPreferencesKey("notify_hour")
        val notifyMinute = intPreferencesKey("notify_minute")
        val lastNotifiedDate = stringPreferencesKey("last_notified_date")
    }

    val settings: Flow<DailySettings> =
        context.dailyDataStore.data.map { prefs ->
            DailySettings(
                githubOwner = prefs[Keys.githubOwner] ?: "",
                githubRepo = prefs[Keys.githubRepo] ?: "",
                githubBranch = prefs[Keys.githubBranch] ?: "main",
                enableNotifications = prefs[Keys.enableNotifications] ?: false,
                notifyHour = prefs[Keys.notifyHour] ?: 9,
                notifyMinute = prefs[Keys.notifyMinute] ?: 0,
                lastNotifiedDate = prefs[Keys.lastNotifiedDate] ?: "",
            )
        }

    suspend fun update(
        githubOwner: String? = null,
        githubRepo: String? = null,
        githubBranch: String? = null,
        enableNotifications: Boolean? = null,
        notifyHour: Int? = null,
        notifyMinute: Int? = null,
        lastNotifiedDate: String? = null,
    ) {
        context.dailyDataStore.edit { prefs ->
            if (githubOwner != null) prefs[Keys.githubOwner] = githubOwner
            if (githubRepo != null) prefs[Keys.githubRepo] = githubRepo
            if (githubBranch != null) prefs[Keys.githubBranch] = githubBranch
            if (enableNotifications != null) prefs[Keys.enableNotifications] = enableNotifications
            if (notifyHour != null) prefs[Keys.notifyHour] = notifyHour
            if (notifyMinute != null) prefs[Keys.notifyMinute] = notifyMinute
            if (lastNotifiedDate != null) prefs[Keys.lastNotifiedDate] = lastNotifiedDate
        }
    }
}

