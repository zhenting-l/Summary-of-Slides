package com.lzt.summaryofslides.ui.daily

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
fun DailyHistoryScreen(
    onBack: () -> Unit,
    onOpenReport: (String) -> Unit,
) {
    val vm: DailyHistoryViewModel = viewModel()
    val index by vm.index.collectAsState()
    val loading by vm.loading.collectAsState()
    val message by vm.message.collectAsState()

    val dates = index?.dates?.sortedDescending().orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日报历史") },
                navigationIcon = { Button(onClick = onBack) { Text("返回") } },
                actions = {
                    Button(onClick = { vm.refresh() }, enabled = !loading) { Text(if (loading) "同步中…" else "同步") }
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
            if (!message.isNullOrBlank()) {
                Text(message.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(dates, key = { it }) { d ->
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenReport(d) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Text(
                            text = d,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

