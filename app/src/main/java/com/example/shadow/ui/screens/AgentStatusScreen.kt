package com.example.shadow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shadow.core.agent.AgentProgress
import com.example.shadow.core.agent.AgentStatus
import com.example.shadow.core.logging.LogEntry

@Composable
fun AgentStatusScreen(
    status: AgentStatus,
    logEntries: List<LogEntry>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "shadow\npinger")
        Text(text = "Общие сведения")
        InfoRow(label = "HWID", value = status.deviceId)
        InfoRow(label = "Active SIM", value = status.activeSimLabel ?: "—")
        InfoRow(label = "Connected", value = if (status.isConnected) "Да" else "Нет")
        InfoRow(
            label = "Active operator",
            value = status.activeOperator?.name ?: "—",
        )
        InfoRow(label = "Выполнено задач", value = status.tasksCompleted.toString())
        InfoRow(label = "Протестировано подсетей", value = status.subnetsTested.toString())

        HorizontalDivider()

        Text(text = "Текущая задача")
        InfoRow(label = "Current Job ID", value = status.jobId ?: "—")
        ProgressSection(progress = status.progress)

        if (logEntries.isNotEmpty()) {
            HorizontalDivider()
            Text(text = "Логи запуска")
            logEntries.takeLast(3).forEach { entry ->
                Text(text = entry.format())
            }
        }
    }
}

@Composable
private fun ProgressSection(progress: AgentProgress) {
    InfoRow(
        label = "Progress",
        value = "${progress.subnetsCompleted}/${progress.subnetsTotal}",
    )
    InfoRow(label = "Subnets", value = progress.subnetsCompleted.toString())
    InfoRow(label = "IPs tested", value = progress.ipsTested.toString())
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Text(text = "$label: $value")
}
