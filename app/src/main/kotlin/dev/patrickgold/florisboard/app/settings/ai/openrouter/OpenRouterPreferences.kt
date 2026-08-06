package dev.patrickgold.florisboard.app.settings.ai.openrouter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceModel
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.PreferenceUiScope
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefTextField
import kotlinx.coroutines.launch

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun PreferenceUiScope<FlorisPreferenceModel>.OpenRouterPreferenceGroup() {
    val scope = rememberCoroutineScope()

    val apiKey by prefs.ai.openRouterApiKey.collectAsState()
    val model by prefs.ai.openRouterModel.collectAsState()
    val provider by prefs.ai.openRouterProvider.collectAsState()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var showProviderDialog by remember { mutableStateOf(false) }

    PreferenceGroup(title = "OpenRouter Integration") {
        Preference(
            icon = Icons.Default.Key,
            title = "API Key",
            summary = if (apiKey.isNotBlank()) {
                "••••••••" + apiKey.takeLast(4)
            } else {
                "No key configured"
            },
            onClick = { showApiKeyDialog = true },
        )

        Preference(
            icon = Icons.Default.SmartToy,
            title = "OpenRouter Model",
            summary = model.ifBlank { "No model configured" },
            onClick = { showModelDialog = true },
        )

        Preference(
            icon = Icons.Default.Storage,
            title = "OpenRouter Provider",
            summary = provider.ifBlank { "Automatic routing" },
            onClick = { showProviderDialog = true },
        )
    }

    if (showApiKeyDialog) {
        var tempApiKey by remember { mutableStateOf(apiKey) }
        JetPrefAlertDialog(
            title = "OpenRouter API Key",
            confirmLabel = "Save",
            dismissLabel = "Cancel",
            onConfirm = {
                scope.launch {
                    prefs.ai.openRouterApiKey.set(tempApiKey.trim())
                }
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Enter your OpenRouter API Key:")
                Spacer(modifier = Modifier.height(8.dp))
                JetPrefTextField(
                    value = tempApiKey,
                    onValueChange = { tempApiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showModelDialog) {
        var tempModel by remember { mutableStateOf(model) }
        JetPrefAlertDialog(
            title = "OpenRouter Model",
            confirmLabel = "Save",
            dismissLabel = "Cancel",
            onConfirm = {
                scope.launch {
                    prefs.ai.openRouterModel.set(tempModel.trim())
                }
                showModelDialog = false
            },
            onDismiss = { showModelDialog = false },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("OpenRouter model tag (e.g. deepseek/deepseek-v4-flash-0731, anthropic/claude-3.5-sonnet):")
                Spacer(modifier = Modifier.height(8.dp))
                JetPrefTextField(
                    value = tempModel,
                    onValueChange = { tempModel = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (showProviderDialog) {
        var tempProvider by remember { mutableStateOf(provider) }
        JetPrefAlertDialog(
            title = "OpenRouter Provider",
            confirmLabel = "Save",
            dismissLabel = "Cancel",
            neutralLabel = "Automatic",
            onNeutral = {
                scope.launch {
                    prefs.ai.openRouterProvider.set("")
                }
                showProviderDialog = false
            },
            onConfirm = {
                scope.launch {
                    prefs.ai.openRouterProvider.set(tempProvider.trim())
                }
                showProviderDialog = false
            },
            onDismiss = { showProviderDialog = false },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Provider slug (e.g. deepinfra/fp4, baseten/fp8, deepseek). The request will use only this provider, with no fallback.")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Grammar Fix uses forced tool calling. The selected model and provider must support tools and tool_choice. DeepInfra FP4 is the default.")
                Spacer(modifier = Modifier.height(8.dp))
                JetPrefTextField(
                    value = tempProvider,
                    onValueChange = { tempProvider = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
