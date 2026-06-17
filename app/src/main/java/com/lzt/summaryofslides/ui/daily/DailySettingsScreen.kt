package com.lzt.summaryofslides.ui.daily

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySettingsScreen(
    onBack: () -> Unit,
) {
    val vm: DailySettingsViewModel = viewModel()
    val settings by vm.settings.collectAsState()
    val message by vm.message.collectAsState()
    val loading by vm.loading.collectAsState()
    val context = LocalContext.current

    var owner by remember { mutableStateOf("") }
    var repo by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var enableNotifications by remember { mutableStateOf(false) }
    var notifyHour by remember { mutableStateOf("9") }
    var notifyMinute by remember { mutableStateOf("0") }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val hour = (notifyHour.toIntOrNull() ?: 9).coerceIn(0, 23)
            val minute = (notifyMinute.toIntOrNull() ?: 0).coerceIn(0, 59)
            vm.save(
                githubOwner = owner,
                githubRepo = repo,
                githubBranch = branch,
                enableNotifications = granted,
                notifyHour = hour,
                notifyMinute = minute,
                context = context,
            )
        }

    LaunchedEffect(settings) {
        owner = settings.githubOwner
        repo = settings.githubRepo
        branch = settings.githubBranch
        enableNotifications = settings.enableNotifications
        notifyHour = settings.notifyHour.toString()
        notifyMinute = settings.notifyMinute.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日报设置") },
                navigationIcon = { Button(onClick = onBack) { Text("返回") } },
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("同步源（GitHub）", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = owner,
                        onValueChange = { owner = it },
                        label = { Text("Owner") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = repo,
                        onValueChange = { repo = it },
                        label = { Text("Repo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = branch,
                        onValueChange = { branch = it },
                        label = { Text("Branch（默认 main）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("每日通知（本机）")
                        Switch(
                            checked = enableNotifications,
                            onCheckedChange = { enableNotifications = it },
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedTextField(
                            value = notifyHour,
                            onValueChange = { notifyHour = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("小时") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        OutlinedTextField(
                            value = notifyMinute,
                            onValueChange = { notifyMinute = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("分钟") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = { vm.testConnection() },
                            enabled = !loading,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (loading) "检查中…" else "测试连接")
                        }
                        Button(
                            onClick = {
                                val hour = (notifyHour.toIntOrNull() ?: 9).coerceIn(0, 23)
                                val minute = (notifyMinute.toIntOrNull() ?: 0).coerceIn(0, 59)
                                val needPermission =
                                    enableNotifications &&
                                        Build.VERSION.SDK_INT >= 33 &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                                        android.content.pm.PackageManager.PERMISSION_GRANTED
                                if (needPermission) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    return@Button
                                }
                                vm.save(
                                    githubOwner = owner,
                                    githubRepo = repo,
                                    githubBranch = branch,
                                    enableNotifications = enableNotifications,
                                    notifyHour = hour,
                                    notifyMinute = minute,
                                    context = context,
                                )
                            },
                            enabled = !loading,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("保存")
                        }
                    }
                }
            }
        }
    }
}
