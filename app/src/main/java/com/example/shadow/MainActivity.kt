package com.example.shadow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.shadow.core.agent.AgentProgress
import com.example.shadow.core.agent.AgentRepository
import com.example.shadow.core.agent.AgentState
import com.example.shadow.core.agent.AgentStateMachine
import com.example.shadow.core.agent.AgentStatus
import com.example.shadow.core.device.HwidProvider
import com.example.shadow.core.telephony.Operator
import com.example.shadow.core.telephony.SimManager
import com.example.shadow.ui.screens.AgentStatusScreen
import com.example.shadow.ui.screens.AuthorizationScreen
import com.example.shadow.ui.screens.PermissionItem
import com.example.shadow.ui.screens.RegistrationScreen
import com.example.shadow.ui.screens.SettingsScreen
import com.example.shadow.ui.theme.ShadowTheme
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
    val hwid = remember { HwidProvider(context).getOrCreate() }
    val simManager = remember { SimManager(context) }
    val scope = rememberCoroutineScope()

    val screen = rememberSaveable { mutableStateOf(AppScreen.SETTINGS) }
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
        if (stateMachine.currentState != AgentState.INIT) {
            screen.value = AppScreen.STATUS
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = { screen.value = AppScreen.SETTINGS }) {
                    Text(text = "Настройки")
                }
                TextButton(onClick = { screen.value = AppScreen.STATUS }) {
                    Text(text = "Статус")
                }
            }
        },
    ) { _ ->
        when (screen.value) {
            AppScreen.REGISTRATION -> RegistrationScreen { _, _ ->
                stateMachine.transition(AgentState.REGISTERED)
                screen.value = AppScreen.AUTHORIZATION
            }
            AppScreen.AUTHORIZATION -> AuthorizationScreen { _, _ ->
                stateMachine.transition(AgentState.AUTHORIZED)
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
                },
                onOperatorSelected = { subscriptionId, operator ->
                    simMappings[subscriptionId] = operator
                },
                onContinue = {
                    stateMachine.transition(AgentState.PERMISSIONS_GRANTED)
                    stateMachine.transition(AgentState.SIMS_MAPPED)
                    stateMachine.transition(AgentState.KAFKA_REGISTERED)
                    stateMachine.transition(AgentState.IDLE)
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
                AgentStatusScreen(
                    status = status,
                    hwid = hwid,
                )
            }
        }
    }
}
