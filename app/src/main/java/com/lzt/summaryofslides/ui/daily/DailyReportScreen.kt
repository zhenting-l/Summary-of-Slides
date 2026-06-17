package com.lzt.summaryofslides.ui.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lzt.summaryofslides.daily.DailyPaper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DailyReportScreen(
    reportDate: String,
    onBack: () -> Unit,
) {
    val vm: DailyReportViewModel = viewModel()
    val report by vm.report.collectAsState()
    val loading by vm.loading.collectAsState()
    val message by vm.message.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(reportDate) {
        vm.load(reportDate)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(reportDate) },
                navigationIcon = { Button(onClick = onBack) { Text("返回") } },
                actions = {
                    Button(onClick = { vm.load(reportDate, forceRemote = true) }, enabled = !loading) {
                        Text(if (loading) "刷新中…" else "刷新")
                    }
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
            val papers = report?.papers.orEmpty().sortedByDescending { it.score }
            val grouped = papers.groupBy { it.subfield.ifBlank { "未分类" } }
            val collapsedByGroup = remember(reportDate) { mutableStateMapOf<String, Boolean>() }

            LaunchedEffect(reportDate, grouped.size) {
                grouped.keys.forEach { key ->
                    if (!collapsedByGroup.containsKey(key)) {
                        collapsedByGroup[key] = true
                    }
                }
            }

            val allCollapsed = grouped.isNotEmpty() && grouped.keys.all { collapsedByGroup[it] == true }
            val anyCollapsed = grouped.isNotEmpty() && grouped.keys.any { collapsedByGroup[it] == true }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = {
                        grouped.keys.forEach { key ->
                            collapsedByGroup[key] = true
                        }
                    },
                    enabled = grouped.isNotEmpty() && !allCollapsed,
                ) {
                    Text("全部收起")
                }
                TextButton(
                    onClick = {
                        grouped.keys.forEach { key ->
                            collapsedByGroup[key] = false
                        }
                    },
                    enabled = grouped.isNotEmpty() && anyCollapsed,
                ) {
                    Text("全部展开")
                }
            }

            val keywordHotspots = computeKeywordHotspots(papers)
            val activeAuthors = computeActiveAuthors(papers)

            if (keywordHotspots.isNotEmpty() || activeAuthors.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("快速概览", style = MaterialTheme.typography.titleMedium)

                        if (keywordHotspots.isNotEmpty()) {
                            Text("关键词热点", style = MaterialTheme.typography.bodySmall)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                keywordHotspots.forEach { (keyword, count) ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("$keyword $count") },
                                        colors = AssistChipDefaults.assistChipColors(),
                                    )
                                }
                            }
                        }

                        if (activeAuthors.isNotEmpty()) {
                            Text("活跃作者", style = MaterialTheme.typography.bodySmall)
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                activeAuthors.forEach { (name, count) ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text("$name $count") },
                                        colors = AssistChipDefaults.assistChipColors(),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!message.isNullOrBlank()) {
                Text(message.orEmpty(), color = MaterialTheme.colorScheme.error)
            }

            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                grouped.forEach { (subfield, groupPapers) ->
                    item(key = "group-$subfield") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val current = collapsedByGroup[subfield] == true
                                collapsedByGroup[subfield] = !current
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(subfield, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${groupPapers.size} 篇 ${if (collapsedByGroup[subfield] == true) "（展开）" else "（收起）"}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }

                    if (collapsedByGroup[subfield] != true) {
                        items(groupPapers, key = { it.arxivId.ifBlank { it.id } }) { p ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Column(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        p.title.ifBlank { p.arxivId },
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 4,
                                    )

                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        if (p.subfield.isNotBlank()) {
                                            AssistChip(
                                                onClick = {},
                                                label = { Text(p.subfield) },
                                                colors = AssistChipDefaults.assistChipColors(),
                                            )
                                        }
                                        if (p.score > 0) {
                                            AssistChip(
                                                onClick = {},
                                                label = { Text("score ${String.format("%.1f", p.score)}") },
                                                colors = AssistChipDefaults.assistChipColors(),
                                            )
                                        }
                                    }

                                    if (p.authors.isNotEmpty()) {
                                        Text(
                                            p.authors.joinToString(", "),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 4,
                                        )
                                    }
                                    if (p.recommendationReason.isNotBlank()) {
                                        Text(p.recommendationReason, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Button(
                                            onClick = { if (p.arxivUrl.isNotBlank()) uriHandler.openUri(p.arxivUrl) },
                                            enabled = p.arxivUrl.isNotBlank(),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("arXiv")
                                        }
                                        Button(
                                            onClick = { if (p.pdfUrl.isNotBlank()) uriHandler.openUri(p.pdfUrl) },
                                            enabled = p.pdfUrl.isNotBlank(),
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text("PDF")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun computeActiveAuthors(papers: List<DailyPaper>): List<Pair<String, Int>> {
    if (papers.isEmpty()) return emptyList()
    val counts = mutableMapOf<String, Int>()
    papers.forEach { paper ->
        paper.authors.forEach { author ->
            val key = author.trim()
            if (key.isNotBlank()) {
                counts[key] = (counts[key] ?: 0) + 1
            }
        }
    }
    return counts.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).take(12)
        .map { it.key to it.value }
}

private fun computeKeywordHotspots(papers: List<DailyPaper>): List<Pair<String, Int>> {
    if (papers.isEmpty()) return emptyList()
    val stopwords =
        setOf(
            "a",
            "an",
            "and",
            "are",
            "as",
            "at",
            "based",
            "be",
            "by",
            "for",
            "from",
            "in",
            "into",
            "is",
            "of",
            "on",
            "or",
            "that",
            "the",
            "this",
            "to",
            "via",
            "with",
        )
    val regex = Regex("[A-Za-z]{4,}")
    val counts = mutableMapOf<String, Int>()
    papers.forEach { paper ->
        val text = paper.title
        regex.findAll(text).forEach { match ->
            val token = match.value.lowercase()
            if (token !in stopwords) {
                counts[token] = (counts[token] ?: 0) + 1
            }
        }
    }
    return counts.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).take(12)
        .map { it.key to it.value }
}
