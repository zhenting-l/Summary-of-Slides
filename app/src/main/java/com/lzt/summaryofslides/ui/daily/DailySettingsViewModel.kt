package com.lzt.summaryofslides.ui.daily

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lzt.summaryofslides.data.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DailySettingsViewModel : ViewModel() {
    private val store = AppContainer.dailySettingsStore
    private val repo = AppContainer.dailyRepository

    val settings =
        store.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            com.lzt.summaryofslides.daily.DailySettings("", "", "main", false, 9, 0, ""),
        )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun save(
        githubOwner: String,
        githubRepo: String,
        githubBranch: String,
        enableNotifications: Boolean,
        notifyHour: Int,
        notifyMinute: Int,
        context: Context,
    ) {
        viewModelScope.launch {
            _loading.value = true
            _message.value = null
            runCatching {
                store.update(
                    githubOwner = githubOwner.trim(),
                    githubRepo = githubRepo.trim(),
                    githubBranch = githubBranch.trim().ifBlank { "main" },
                    enableNotifications = enableNotifications,
                    notifyHour = notifyHour.coerceIn(0, 23),
                    notifyMinute = notifyMinute.coerceIn(0, 59),
                )
                if (enableNotifications) {
                    DailyWorkScheduler.schedule(context, notifyHour, notifyMinute)
                } else {
                    DailyWorkScheduler.cancel(context)
                }
            }.onFailure { e ->
                _message.value = e.message ?: "保存失败"
            }.onSuccess {
                _message.value = "已保存"
            }
            _loading.value = false
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            val s = settings.value
            if (s.githubOwner.isBlank() || s.githubRepo.isBlank()) {
                _message.value = "请先填写 owner/repo"
                return@launch
            }
            _loading.value = true
            _message.value = null
            runCatching {
                repo.refreshIndex(s)
            }.onSuccess {
                _message.value = "连接成功"
            }.onFailure { e ->
                _message.value = e.message ?: "连接失败"
            }
            _loading.value = false
        }
    }
}

