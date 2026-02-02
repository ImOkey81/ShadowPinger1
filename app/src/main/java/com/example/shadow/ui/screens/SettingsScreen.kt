package com.example.shadow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shadow.core.telephony.Operator
import com.example.shadow.core.telephony.SimInfo

@Composable
fun SettingsScreen(
    permissions: List<PermissionItem>,
    simCards: List<SimInfo>,
    simMappings: Map<Int, Operator?>,
    onPermissionToggle: (PermissionItem) -> Unit,
    onOperatorSelected: (subscriptionId: Int, operator: Operator) -> Unit,
    onContinue: () -> Unit,
    canContinue: Boolean,
) {
    var showSimDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Настройки")
        Text(text = "Разрешения:")
        permissions.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = item.label)
                Button(onClick = { onPermissionToggle(item) }) {
                    Text(text = if (item.granted) "Разрешено" else "Разрешить")
                }
            }
        }

        HorizontalDivider()

        Text(text = "SIM-карты")
        if (simMappings.isEmpty()) {
            Text(text = "Пока нет добавленных SIM.")
        } else {
            simMappings.forEach { (subscriptionId, operator) ->
                val sim = simCards.firstOrNull { it.subscriptionId == subscriptionId }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Локальное имя: ${sim?.displayName ?: "—"}")
                    Text(text = "Оператор: ${operator?.name ?: "—"}")
                }
            }
        }

        Button(onClick = { showSimDialog = true }, enabled = simCards.isNotEmpty()) {
            Text(text = "Добавить SIM")
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (!canContinue) {
            Text(text = "Назначьте оператора для SIM и выдайте разрешения.")
        }
        Button(onClick = onContinue, enabled = canContinue) {
            Text(text = "Продолжить")
        }
    }

    if (showSimDialog) {
        AddSimDialog(
            simCards = simCards,
            simMappings = simMappings,
            onDismiss = { showSimDialog = false },
            onConfirm = { subscriptionId, operator ->
                onOperatorSelected(subscriptionId, operator)
                showSimDialog = false
            },
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AddSimDialog(
    simCards: List<SimInfo>,
    simMappings: Map<Int, Operator?>,
    onDismiss: () -> Unit,
    onConfirm: (Int, Operator) -> Unit,
) {
    val availableSims = simCards.filter { sim -> simMappings[sim.subscriptionId] == null }
    var selectedSimId by remember { mutableStateOf(availableSims.firstOrNull()?.subscriptionId) }
    var selectedOperator by remember { mutableStateOf(Operator.values().firstOrNull()) }
    var simExpanded by remember { mutableStateOf(false) }
    var operatorExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(availableSims, simMappings) {
        if (selectedSimId !in availableSims.map { it.subscriptionId }) {
            selectedSimId = availableSims.firstOrNull()?.subscriptionId
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Добавление SIM") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = "Обнаруженные SIM:")
                ExposedDropdownMenuBox(
                    expanded = simExpanded,
                    onExpandedChange = { simExpanded = !simExpanded },
                ) {
                    val selectedSim = availableSims.firstOrNull { it.subscriptionId == selectedSimId }
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        value = selectedSim?.displayName ?: "Нет доступных SIM",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = simExpanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = simExpanded,
                        onDismissRequest = { simExpanded = false },
                    ) {
                        availableSims.forEach { sim ->
                            DropdownMenuItem(
                                text = { Text(text = sim.displayName) },
                                onClick = {
                                    selectedSimId = sim.subscriptionId
                                    simExpanded = false
                                },
                            )
                        }
                    }
                }

                Text(text = "Оператор")
                ExposedDropdownMenuBox(
                    expanded = operatorExpanded,
                    onExpandedChange = { operatorExpanded = !operatorExpanded },
                ) {
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        value = selectedOperator?.name ?: "—",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = operatorExpanded) },
                    )
                    ExposedDropdownMenu(
                        expanded = operatorExpanded,
                        onDismissRequest = { operatorExpanded = false },
                    ) {
                        Operator.values().forEach { operator ->
                            DropdownMenuItem(
                                text = { Text(text = operator.name) },
                                onClick = {
                                    selectedOperator = operator
                                    operatorExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val simId = selectedSimId
                    val operator = selectedOperator
                    if (simId != null && operator != null) {
                        onConfirm(simId, operator)
                    }
                },
                enabled = selectedSimId != null && selectedOperator != null,
            ) {
                Text(text = "Добавить SIM")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Отмена")
            }
        },
    )
}

data class PermissionItem(
    val key: String,
    val label: String,
    val granted: Boolean,
)
