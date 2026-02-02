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

@Composable
fun AgentStatusScreen(
    status: AgentStatus,
    hwid: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Общие сведения")
        InfoRow(label = "HWID", value = hwid)
        InfoRow(label = "Active SIM", value = status.activeSimLabel ?: "—")
        InfoRow(label = "Connected", value = status.state.name)
        InfoRow(label = "Active operator", value = status.activeOperator?.name ?: "—")
        InfoRow(label = "Выполнено задач", value = status.progress.subnetsCompleted.toString())
        InfoRow(label = "Протестировано подсетей", value = status.progress.subnetsTotal.toString())

        HorizontalDivider()

        Text(text = "Текущая задача")
        InfoRow(label = "Current Job ID", value = status.jobId ?: "—")
        ProgressSection(progress = status.progress)
    }
}

@Composable
private fun ProgressSection(progress: AgentProgress) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        InfoRow(
            label = "Progress",
            value = "${progress.subnetsCompleted}/${progress.subnetsTotal}",
        )
        InfoRow(label = "Subnets", value = progress.subnetsTotal.toString())
        InfoRow(label = "IPs tested", value = progress.ipsTested.toString())
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
) {
    Text(text = "$label • $value")
}
