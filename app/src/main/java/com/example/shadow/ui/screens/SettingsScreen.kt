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
import com.example.shadow.core.permissions.PermissionType
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(text = "Permissions")
        permissions.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = item.label)
                Button(onClick = { onPermissionToggle(item) }) {
                    Text(text = if (item.granted) "Granted" else "Grant")
                }
            }
        }

        HorizontalDivider()

        Text(text = "SIM cards")
        simCards.forEach { sim ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${sim.displayName} • ${sim.carrierName} • slot ${sim.slotIndex}",
                )
                Text(text = "UID: ${sim.simUid}")
                OperatorSelector(
                    selected = simMappings[sim.subscriptionId],
                    onSelected = { operator -> onOperatorSelected(sim.subscriptionId, operator) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (!canContinue) {
            Text(text = "Map all SIMs and grant required permissions to activate the agent.")
        }
        Button(onClick = onContinue, enabled = canContinue) {
            Text(text = "Continue")
        }
    }
}

@Composable
private fun OperatorSelector(
    selected: Operator?,
    onSelected: (Operator) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = "Operator")
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

data class PermissionItem(
    val type: PermissionType,
    val label: String,
    val granted: Boolean,
)
