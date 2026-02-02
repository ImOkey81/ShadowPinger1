package com.example.shadow

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.shadow.core.agent.AgentProgress
import com.example.shadow.core.agent.AgentRepository
import com.example.shadow.core.agent.AgentState
import com.example.shadow.core.agent.AgentStateMachine
import com.example.shadow.core.agent.AgentStatus
import com.example.shadow.core.data.DeviceConfigStore
import com.example.shadow.core.logging.LogBuffer
import com.example.shadow.core.service.AgentForegroundService
import com.example.shadow.core.telephony.Operator
import com.example.shadow.core.telephony.SimManager
import com.example.shadow.ui.screens.AgentStatusScreen
import com.example.shadow.ui.screens.SettingsScreen
import com.example.shadow.ui.theme.ShadowTheme

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
    val logEntries by logBuffer.entries.collectAsState()

    val screen = rememberSaveable { mutableStateOf(AppScreen.STATUS) }
    var deviceId by remember { mutableStateOf("loading...") }
    val permissions = remember { mutableStateOf(mapOf<String, Boolean>()) }
    val simMappings = remember { mutableStateMapOf<Int, Operator?>() }
    val simCards = remember { mutableStateOf(simManager.getAllSimCards()) }
    var isForegroundRunning by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        deviceId = deviceConfigStore.getOrCreateDeviceId()
        logBuffer.add("App started")
        logBuffer.add("Loaded device_id=$deviceId")
        if (stateMachine.currentState != AgentState.INIT) {
            screen.value = AppScreen.STATUS
        }
        logBuffer.add("State restored: ${stateMachine.currentState}")
        permissions.value = mapOf(
            "foreground" to isForegroundRunning,
            "battery" to false,
            "network" to false,
            "sim" to false,
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = screen.value == AppScreen.STATUS,
                    onClick = { screen.value = AppScreen.STATUS },
                    label = { Text("Общие сведения") },
                    icon = {},
                )
                NavigationBarItem(
                    selected = screen.value == AppScreen.SETTINGS,
                    onClick = { screen.value = AppScreen.SETTINGS },
                    label = { Text("Настройки") },
                    icon = {},
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Top,
        ) {
            when (screen.value) {
                AppScreen.SETTINGS -> SettingsScreen(
                    permissions = permissions.value,
                    simCards = simCards.value,
                    simMappings = simMappings,
                    onPermissionToggle = { key ->
                        val updated = !(permissions.value[key] ?: false)
                        permissions.value = permissions.value + (key to updated)
                        logBuffer.add("Permission $key toggled to $updated")
                    },
                    onOperatorSelected = { subscriptionId, operator ->
                        simMappings[subscriptionId] = operator
                        logBuffer.add("SIM $subscriptionId mapped to operator ${operator.name}")
                    },
                    onStartForeground = {
                        val intent = Intent(context, AgentForegroundService::class.java)
                        ContextCompat.startForegroundService(context, intent)
                        isForegroundRunning = true
                        permissions.value = permissions.value + ("foreground" to true)
                        logBuffer.add("Foreground service started")
                    },
                    onStopForeground = {
                        val intent = Intent(context, AgentForegroundService::class.java)
                        context.stopService(intent)
                        isForegroundRunning = false
                        permissions.value = permissions.value + ("foreground" to false)
                        logBuffer.add("Foreground service stopped")
                    },
                )
                AppScreen.STATUS -> {
                    val activeSim = simCards.value.firstOrNull()
                    val activeOperator = activeSim?.subscriptionId?.let { simMappings[it] }
                    val status = AgentStatus(
                        deviceId = deviceId,
                        state = stateMachine.currentState,
                        activeOperator = activeOperator,
                        activeSimLabel = activeSim?.displayName,
                        isConnected = isForegroundRunning,
                        tasksCompleted = 0,
                        subnetsTested = 0,
                        jobId = null,
                        progress = AgentProgress(
                            subnetsTotal = 0,
                            subnetsCompleted = 0,
                            ipsTested = 0,
                        ),
                        lastErrors = emptyList(),
                    )
                    AgentStatusScreen(status = status, logEntries = logEntries)
                }
            }
        }
    }
}
