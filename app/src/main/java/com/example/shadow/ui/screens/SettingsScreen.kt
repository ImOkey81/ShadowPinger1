package com.example.shadow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shadow.core.telephony.Operator
import com.example.shadow.core.telephony.SimInfo

@Composable
fun SettingsScreen(
    permissions: Map<String, Boolean>,
    simCards: List<SimInfo>,
    simMappings: Map<Int, Operator?>,
    onPermissionToggle: (String) -> Unit,
    onOperatorSelected: (subscriptionId: Int, operator: Operator) -> Unit,
    onStartForeground: () -> Unit,
    onStopForeground: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Настройки")
        Text(text = "Разрешения:")
        PermissionRow(
            title = "Foreground service",
            isGranted = permissions["foreground"] == true,
            onClick = onStartForeground,
        )
        PermissionRow(
            title = "Оптимизация заряда батареи",
            isGranted = permissions["battery"] == true,
            onClick = { onPermissionToggle("battery") },
        )
        PermissionRow(
            title = "Доступ к мобильной сети",
            isGranted = permissions["network"] == true,
            onClick = { onPermissionToggle("network") },
        )
        PermissionRow(
            title = "Доступ к SIM-карте",
            isGranted = permissions["sim"] == true,
            onClick = { onPermissionToggle("sim") },
        )

        if (permissions["foreground"] == true) {
            Button(onClick = onStopForeground) {
                Text(text = "Остановить foreground сервис")
            }
        }

        HorizontalDivider()

        Text(text = "SIM-карты")
        simCards.forEach { sim ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Локальное имя: ${sim.displayName}")
                Text(text = "UID SIM: ${sim.subscriptionId}")
                OperatorSelector(
                    selected = simMappings[sim.subscriptionId],
                    onSelected = { operator -> onOperatorSelected(sim.subscriptionId, operator) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { /* TODO: add SIM flow */ }, enabled = simCards.isNotEmpty()) {
            Text(text = "Добавить SIM")
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    isGranted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title)
        Button(onClick = onClick) {
            Text(text = if (isGranted) "Разрешено" else "Разрешить")
        }
    }
}

@Composable
private fun OperatorSelector(
    selected: Operator?,
    onSelected: (Operator) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Оператор")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Operator.values().forEach { operator ->
                FilterChip(
                    selected = selected == operator,
                    onClick = { onSelected(operator) },
                    label = { Text(operator.name) },
                )
            }
        }
    }
}
