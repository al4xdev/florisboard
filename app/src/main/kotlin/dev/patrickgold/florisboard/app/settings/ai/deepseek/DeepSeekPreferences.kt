package dev.patrickgold.florisboard.app.settings.ai.deepseek

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.SmartToy
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
import dev.patrickgold.florisboard.app.settings.ai.AiDefaults
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
fun PreferenceUiScope<FlorisPreferenceModel>.DeepSeekPreferenceGroup() {
    val scope = rememberCoroutineScope()

    val apiKey by prefs.ai.deepSeekApiKey.collectAsState()
    val model by prefs.ai.deepSeekModel.collectAsState()

    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }

    PreferenceGroup(title = "DeepSeek Integration") {
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
            title = "DeepSeek Model",
            summary = model.ifBlank { "No model configured" },
            onClick = { showModelDialog = true },
        )
    }

    if (showApiKeyDialog) {
        var tempApiKey by remember { mutableStateOf(apiKey) }
        JetPrefAlertDialog(
            title = "DeepSeek API Key",
            confirmLabel = "Save",
            dismissLabel = "Cancel",
            onConfirm = {
                scope.launch {
                    prefs.ai.deepSeekApiKey.set(tempApiKey.trim())
                }
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Enter your DeepSeek API Key from platform.deepseek.com:")
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
            title = "DeepSeek Model",
            confirmLabel = "Save",
            dismissLabel = "Cancel",
            neutralLabel = "Restore Default",
            onNeutral = {
                scope.launch {
                    prefs.ai.deepSeekModel.set(AiDefaults.DEEPSEEK_MODEL)
                }
                showModelDialog = false
            },
            onConfirm = {
                scope.launch {
                    prefs.ai.deepSeekModel.set(tempModel.trim())
                }
                showModelDialog = false
            },
            onDismiss = { showModelDialog = false },
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "The native DeepSeek API accepts exactly two model slugs: " +
                        "${AiDefaults.DEEPSEEK_MODEL} (fast, the default) and ${AiDefaults.DEEPSEEK_MODEL_PRO}."
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("There is no provider setting: requests go straight to api.deepseek.com with thinking disabled for the lowest latency.")
                Spacer(modifier = Modifier.height(8.dp))
                JetPrefTextField(
                    value = tempModel,
                    onValueChange = { tempModel = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
