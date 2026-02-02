package com.example.shadow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
        Text(text = "Agent status")
        Text(text = "State: ${status.state}")
        Text(text = "Active SIM: ${status.activeSimLabel ?: "—"}")
        Text(text = "Active operator: ${status.activeOperator ?: "—"}")
        Text(text = "Job ID: ${status.jobId ?: "—"}")
        ProgressSection(progress = status.progress)
        if (status.lastErrors.isNotEmpty()) {
            Text(text = "Last errors:")
            status.lastErrors.forEach { error ->
                Text(text = "• $error")
            }
        }

        DebugLogsSection(logEntries = logEntries)
    }
}

@Composable
private fun ProgressSection(progress: AgentProgress) {
    Text(
        text = "Progress: ${progress.subnetsCompleted}/${progress.subnetsTotal} subnets, " +
            "${progress.ipsTested} IPs tested",
    )
}

@Composable
private fun DebugLogsSection(logEntries: List<LogEntry>) {
    val clipboardManager = LocalClipboardManager.current
    val logText = logEntries.joinToString("\n") { it.format() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Debug logs (${logEntries.size}/500)")
        Button(
            onClick = { clipboardManager.setText(AnnotatedString(logText)) },
            enabled = logEntries.isNotEmpty(),
        ) {
            Text(text = "Copy logs")
        }
        if (logEntries.isEmpty()) {
            Text(text = "No logs yet.")
        } else {
            LazyColumn {
                items(logEntries) { entry ->
                    Text(text = entry.format())
                }
            }
        }
    }
}
