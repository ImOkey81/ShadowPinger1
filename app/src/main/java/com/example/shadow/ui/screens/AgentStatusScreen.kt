package com.example.shadow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shadow.core.agent.AgentProgress
import com.example.shadow.core.agent.AgentStatus

@Composable
fun AgentStatusScreen(
    status: AgentStatus,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Agent status")
        Button(onClick = onOpenSettings) {
            Text(text = "Настройки")
        }
        Text(text = "State: ${status.state}")
        Text(text = "Active SIM: ${status.activeSimLabel ?: "—"}")
        Text(text = "Active operator: ${status.activeOperator ?: "—"}")
        Text(text = "Job ID: ${status.jobId ?: "—"}")
        Text(text = "IP tested: ${status.currentIp ?: "—"}")
        ProgressSection(progress = status.progress)
        if (status.lastErrors.isNotEmpty()) {
            Text(text = "Last errors:")
            status.lastErrors.forEach { error ->
                Text(text = "• $error")
            }
        }
    }
}

@Composable
private fun ProgressSection(progress: AgentProgress) {
    Text(
        text = "Progress: ${progress.subnetsCompleted}/${progress.subnetsTotal} subnets, " +
            "${progress.ipsTested} IPs tested",
    )
}
