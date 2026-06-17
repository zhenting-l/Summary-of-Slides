package com.lzt.summaryofslides.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lzt.summaryofslides.data.AppContainer
import com.lzt.summaryofslides.daily.DailyIndex
import com.lzt.summaryofslides.daily.DailyReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DailyHomeViewModel : ViewModel() {
    private val store = AppContainer.dailySettingsStore
    private val repo = AppContainer.dailyRepository

    val settings =
        store.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            com.lzt.summaryofslides.daily.DailySettings("", "", "main", false, 9, 0, ""),
        )

    private val _index = MutableStateFlow<DailyIndex?>(null)
    val index: StateFlow<DailyIndex?> = _index

    private val _latestReport = MutableStateFlow<DailyReport?>(null)
    val latestReport: StateFlow<DailyReport?> = _latestReport

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        viewModelScope.launch {
            _index.value = repo.getCachedIndex()
            val cachedIndex = _index.value
            val latest = cachedIndex?.dates?.sorted()?.lastOrNull()
            if (!latest.isNullOrBlank()) {
                _latestReport.value = repo.getCachedReport(latest)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val s = settings.value
            if (s.githubOwner.isBlank() || s.githubRepo.isBlank()) {
                _message.value = "请先在手机端设置里配置 GitHub 仓库（owner/repo）"
                return@launch
            }
            _loading.value = true
            _message.value = null
            runCatching {
                val idx = repo.refreshIndex(s)
                _index.value = idx
                val latest = idx.dates.sorted().lastOrNull()
                if (!latest.isNullOrBlank()) {
                    _latestReport.value = repo.refreshReport(s, latest)
                } else {
                    _latestReport.value = null
                }
            }.onFailure { e ->
                _message.value = e.message ?: "同步失败"
            }
            _loading.value = false
        }
    }
}

