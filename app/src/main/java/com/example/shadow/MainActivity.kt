package com.example.shadow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.shadow.core.agent.AgentProgress
import com.example.shadow.core.agent.AgentRepository
import com.example.shadow.core.agent.AgentState
import com.example.shadow.core.agent.AgentStateMachine
import com.example.shadow.core.agent.AgentStatus
import com.example.shadow.core.data.DeviceConfigStore
import com.example.shadow.core.logging.LogBuffer
import com.example.shadow.core.telephony.Operator
import com.example.shadow.core.telephony.SimManager
import com.example.shadow.ui.screens.AgentStatusScreen
import com.example.shadow.ui.screens.AuthorizationScreen
import com.example.shadow.ui.screens.PermissionItem
import com.example.shadow.ui.screens.RegistrationScreen
import com.example.shadow.ui.screens.SettingsScreen
import com.example.shadow.ui.theme.ShadowTheme
import java.util.UUID
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShadowTheme {
                AppContent()
            }
        }
    }
}

private enum class AppScreen {
    REGISTRATION,
    AUTHORIZATION,
    SETTINGS,
    STATUS,
}

@Composable
private fun AppContent() {
    val context = LocalContext.current
    val repository = remember { AgentRepository(context) }
    val stateMachine = remember { AgentStateMachine(repository) }
    val deviceConfigStore = remember { DeviceConfigStore(context) }
    val logBuffer = remember { LogBuffer() }
    val simManager = remember { SimManager(context) }
    val scope = rememberCoroutineScope()
    val logEntries by logBuffer.entries.collectAsState()

    val screen = rememberSaveable { mutableStateOf(AppScreen.REGISTRATION) }
    var deviceId by remember { mutableStateOf("loading...") }
    val permissions = remember {
        mutableStateOf(
            listOf(
                PermissionItem("foreground", "Foreground service", false),
                PermissionItem("battery", "Ignore battery optimizations", false),
                PermissionItem("network", "Mobile network access", false),
                PermissionItem("sim", "SIM access", false),
            )
        )
    }
    val simMappings = remember { mutableStateMapOf<Int, Operator?>() }
    val simCards = remember { mutableStateOf(simManager.getAllSimCards()) }

    LaunchedEffect(Unit) {
        deviceId = deviceConfigStore.getOrCreateDeviceId()
        logBuffer.add("App started")
        logBuffer.add("Loaded device_id=$deviceId")
        if (stateMachine.currentState != AgentState.INIT) {
            screen.value = AppScreen.STATUS
        }
        logBuffer.add("State restored: ${stateMachine.currentState}")
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        when (screen.value) {
            AppScreen.REGISTRATION -> RegistrationScreen { login, password ->
                val moved = stateMachine.transition(AgentState.REGISTERED)
                logBuffer.add("Registration submitted for $login (state moved=$moved)")
                screen.value = AppScreen.AUTHORIZATION
            }
            AppScreen.AUTHORIZATION -> AuthorizationScreen { _, _ ->
                scope.launch {
                    deviceConfigStore.setDeviceToken(UUID.randomUUID().toString())
                    logBuffer.add("Device token stored from authorization")
                }
                val moved = stateMachine.transition(AgentState.AUTHORIZED)
                logBuffer.add("Authorization completed (state moved=$moved)")
                screen.value = AppScreen.SETTINGS
            }
            AppScreen.SETTINGS -> SettingsScreen(
                permissions = permissions.value,
                simCards = simCards.value,
                simMappings = simMappings,
                onPermissionToggle = { item ->
                    permissions.value = permissions.value.map { existing ->
                        if (existing.key == item.key) {
                            existing.copy(granted = !existing.granted)
                        } else {
                            existing
                        }
                    }
                    logBuffer.add("Permission ${item.key} toggled to ${!item.granted}")
                },
                onOperatorSelected = { subscriptionId, operator ->
                    simMappings[subscriptionId] = operator
                    logBuffer.add("SIM $subscriptionId mapped to operator ${operator.name}")
                },
                onContinue = {
                    listOf(
                        AgentState.PERMISSIONS_GRANTED,
                        AgentState.SIMS_MAPPED,
                        AgentState.KAFKA_REGISTERED,
                        AgentState.IDLE,
                    ).forEach { nextState ->
                        val moved = stateMachine.transition(nextState)
                        logBuffer.add("State transition to $nextState (moved=$moved)")
                    }
                    screen.value = AppScreen.STATUS
                    scope.launch {
                        simCards.value = simManager.getAllSimCards()
                    }
                },
                canContinue = permissions.value.all { it.granted } &&
                    simCards.value.isNotEmpty() &&
                    simCards.value.all { simMappings[it.subscriptionId] != null },
                )
            AppScreen.STATUS -> {
                val activeSim = simCards.value.firstOrNull()
                val activeOperator = activeSim?.subscriptionId?.let { simMappings[it] }
                val status = AgentStatus(
                    state = stateMachine.currentState,
                    activeOperator = activeOperator,
                    activeSimLabel = activeSim?.displayName,
                    jobId = null,
                    progress = AgentProgress(subnetsTotal = 0, subnetsCompleted = 0, ipsTested = 0),
                    lastErrors = emptyList(),
                )
                AgentStatusScreen(status = status, logEntries = logEntries)
            }
        }

        Text(
            text = "Device ID: $deviceId",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(padding),
        )
    }
}
