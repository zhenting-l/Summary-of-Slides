package com.lzt.summaryofslides.ui.daily

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyHomeScreen(
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReport: (String) -> Unit,
) {
    val vm: DailyHomeViewModel = viewModel()
    val settings by vm.settings.collectAsState()
    val report by vm.latestReport.collectAsState()
    val loading by vm.loading.collectAsState()
    val message by vm.message.collectAsState()

    val papers =
        report?.papers
            ?.sortedByDescending { it.score }
            ?.take(10)
            .orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("优化论文日报") },
                navigationIcon = { Button(onClick = onBack) { Text("返回") } },
                actions = {
                    Button(onClick = onOpenHistory) { Text("历史") }
                    Button(onClick = onOpenSettings) { Text("设置") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (settings.githubOwner.isBlank() || settings.githubRepo.isBlank()) {
                Text("未配置同步源：请先在设置里填写 GitHub owner/repo")
                Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth()) { Text("去设置") }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { vm.refresh() },
                        enabled = !loading,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (loading) "同步中…" else "同步")
                    }
                    val latest = report?.reportDate.orEmpty()
                    Button(
                        onClick = { if (latest.isNotBlank()) onOpenReport(latest) },
                        enabled = latest.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("查看最新")
                    }
                }
            }

            val latestDate = report?.reportDate
            if (!latestDate.isNullOrBlank()) {
                Text("最新日期：$latestDate", style = MaterialTheme.typography.titleMedium)
            }
            if (!message.isNullOrBlank()) {
                Text(message.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(papers, key = { it.arxivId.ifBlank { it.id } }) { p ->
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { if (report?.reportDate?.isNotBlank() == true) onOpenReport(report!!.reportDate) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(p.title.ifBlank { p.arxivId }, style = MaterialTheme.typography.titleMedium, maxLines = 3)
                            val sub = buildString {
                                if (p.subfield.isNotBlank()) append(p.subfield)
                                if (p.score > 0) {
                                    if (isNotEmpty()) append(" · ")
                                    append("score ")
                                    append(String.format("%.1f", p.score))
                                }
                            }
                            if (sub.isNotBlank()) {
                                Text(sub, style = MaterialTheme.typography.bodySmall)
                            }
                            if (p.authors.isNotEmpty()) {
                                Text(p.authors.take(6).joinToString(", "), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}

