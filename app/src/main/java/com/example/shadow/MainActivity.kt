package com.example.shadow

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.shadow.core.agent.AgentProgress
import com.example.shadow.core.agent.AgentRepository
import com.example.shadow.core.agent.AgentState
import com.example.shadow.core.agent.AgentStateMachine
import com.example.shadow.core.agent.AgentStatus
import com.example.shadow.core.device.HwidProvider
import com.example.shadow.core.network.BackendResult
import com.example.shadow.core.network.FakeBackendClient
import com.example.shadow.core.permissions.PermissionManager
import com.example.shadow.core.permissions.PermissionType
import com.example.shadow.core.storage.AuthTokenStore
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
    val authTokenStore = remember { AuthTokenStore(context) }
    val backendClient = remember { FakeBackendClient() }
    val permissionManager = remember { PermissionManager(context) }
    val scope = rememberCoroutineScope()

    val screen = rememberSaveable { mutableStateOf(AppScreen.REGISTRATION) }
    val registrationError = remember { mutableStateOf<String?>(null) }
    val authorizationError = remember { mutableStateOf<String?>(null) }

    val permissions = remember {
        mutableStateOf(buildPermissionItems(permissionManager))
    }
    val simMappings = remember { mutableStateMapOf<Int, Operator?>() }
    val simCards = remember { mutableStateOf(simManager.getAllSimCards()) }

    val simPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        permissions.value = buildPermissionItems(permissionManager)
    }

    LaunchedEffect(Unit) {
        if (stateMachine.currentState != AgentState.INIT) {
            screen.value = AppScreen.STATUS
        }
    }

    LaunchedEffect(permissions.value) {
        if (permissionManager.isGranted(PermissionType.SIM_ACCESS)) {
            simCards.value = simManager.getAllSimCards()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        when (screen.value) {
            AppScreen.REGISTRATION -> RegistrationScreen(
                errorMessage = registrationError.value,
            ) { login, password ->
                scope.launch {
                    when (val result = backendClient.register(login, password)) {
                        is BackendResult.Success -> {
                            registrationError.value = null
                            stateMachine.transition(AgentState.REGISTERED)
                            screen.value = AppScreen.AUTHORIZATION
                        }
                        is BackendResult.Failure -> {
                            registrationError.value = result.message
                        }
                    }
                }
            }
            AppScreen.AUTHORIZATION -> AuthorizationScreen(
                errorMessage = authorizationError.value,
            ) { login, password ->
                scope.launch {
                    when (val result = backendClient.authorize(login, password)) {
                        is BackendResult.Success -> {
                            authorizationError.value = null
                            authTokenStore.saveToken(result.data)
                            stateMachine.transition(AgentState.AUTHORIZED)
                            screen.value = AppScreen.SETTINGS
                        }
                        is BackendResult.Failure -> {
                            authorizationError.value = result.message
                        }
                    }
                }
            }
            AppScreen.SETTINGS -> SettingsScreen(
                permissions = permissions.value,
                simCards = simCards.value,
                simMappings = simMappings,
                onPermissionToggle = { item ->
                    when (item.type) {
                        PermissionType.SIM_ACCESS -> simPermissionLauncher.launch(
                            Manifest.permission.READ_PHONE_STATE,
                        )
                        PermissionType.BATTERY_OPTIMIZATION ->
                            context.startActivity(permissionManager.batteryOptimizationIntent())
                        else -> Unit
                    }
                    permissions.value = buildPermissionItems(permissionManager)
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
                    currentIp = null,
                    progress = AgentProgress(subnetsTotal = 0, subnetsCompleted = 0, ipsTested = 0),
                    lastErrors = emptyList(),
                )
                AgentStatusScreen(status = status)
            }
        }

        Text(
            text = "HWID: $hwid",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(padding),
        )
    }
}

private fun buildPermissionItems(permissionManager: PermissionManager): List<PermissionItem> {
    return listOf(
        PermissionItem(
            type = PermissionType.FOREGROUND_SERVICE,
            label = "Foreground service",
            granted = permissionManager.isGranted(PermissionType.FOREGROUND_SERVICE),
        ),
        PermissionItem(
            type = PermissionType.BATTERY_OPTIMIZATION,
            label = "Ignore battery optimizations",
            granted = permissionManager.isGranted(PermissionType.BATTERY_OPTIMIZATION),
        ),
        PermissionItem(
            type = PermissionType.MOBILE_NETWORK,
            label = "Mobile network access",
            granted = permissionManager.isGranted(PermissionType.MOBILE_NETWORK),
        ),
        PermissionItem(
            type = PermissionType.SIM_ACCESS,
            label = "SIM access",
            granted = permissionManager.isGranted(PermissionType.SIM_ACCESS),
        ),
    )
}
