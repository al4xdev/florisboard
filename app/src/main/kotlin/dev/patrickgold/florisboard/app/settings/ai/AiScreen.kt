package dev.patrickgold.florisboard.app.settings.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.R
import dev.patrickgold.florisboard.app.LocalNavController
import dev.patrickgold.florisboard.app.Routes
import dev.patrickgold.florisboard.app.settings.ai.deepseek.DeepSeekPreferenceGroup
import dev.patrickgold.florisboard.app.settings.ai.openrouter.OpenRouterPreferenceGroup
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.ListPreference
import dev.patrickgold.jetpref.datastore.ui.Preference
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries
import dev.patrickgold.jetpref.material.ui.JetPrefAlertDialog
import dev.patrickgold.jetpref.material.ui.JetPrefTextField
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun AiScreen() = FlorisScreen {
    title = "AI Features"
    previewFieldVisible = true

    content {
        val navController = LocalNavController.current
        val scope = rememberCoroutineScope()

        val backend by prefs.ai.backend.collectAsState()
        val customPrompt by prefs.ai.customPrompt.collectAsState()
        val aiLevel by prefs.ai.aiLevel.collectAsState()

        var showPromptDialog by remember { mutableStateOf(false) }

        PreferenceGroup(title = "AI Backend") {
            ListPreference(
                listPref = prefs.ai.backend,
                icon = Icons.Default.Api,
                title = "Backend",
                entries = listPrefEntries {
                    entry(
                        key = AiBackend.OPEN_ROUTER,
                        label = AiBackend.OPEN_ROUTER.label,
                        description = AiBackend.OPEN_ROUTER.description,
                    )
                    entry(
                        key = AiBackend.DEEPSEEK,
                        label = AiBackend.DEEPSEEK.label,
                        description = AiBackend.DEEPSEEK.description,
                    )
                },
            )
        }

        when (backend) {
            AiBackend.OPEN_ROUTER -> OpenRouterPreferenceGroup()
            AiBackend.DEEPSEEK -> DeepSeekPreferenceGroup()
        }

        PreferenceGroup(title = "Editing") {
            AiLevelSliderPreference(
                selectedLevel = aiLevel,
                onLevelSelected = { newLevel ->
                    scope.launch {
                        prefs.ai.aiLevel.set(newLevel)
                    }
                },
            )

            Preference(
                icon = ImageVector.vectorResource(id = R.drawable.ic_auto_awesome),
                title = "Custom AI Prompt",
                summary = customPrompt.ifBlank { "Default prompt" },
                onClick = { showPromptDialog = true },
            )

            Preference(
                icon = Icons.Default.HelpOutline,
                title = "Setup Guide & Help",
                summary = "Learn how to get a ${backend.label} key and configure AI actions",
                onClick = { navController.navigate(Routes.Settings.AiHelp) },
            )
        }

        if (showPromptDialog) {
            var tempPrompt by remember { mutableStateOf(customPrompt) }
            JetPrefAlertDialog(
                title = "Custom AI Prompt",
                confirmLabel = "Save",
                dismissLabel = "Cancel",
                neutralLabel = "Restore Default",
                onNeutral = {
                    scope.launch {
                        prefs.ai.customPrompt.set(AiDefaults.CUSTOM_PROMPT)
                    }
                    showPromptDialog = false
                },
                onConfirm = {
                    scope.launch {
                        prefs.ai.customPrompt.set(tempPrompt.trim())
                    }
                    showPromptDialog = false
                },
                onDismiss = { showPromptDialog = false },
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("System prompt executed by the 'AI Prompt' action:")
                    Spacer(modifier = Modifier.height(8.dp))
                    JetPrefTextField(
                        value = tempPrompt,
                        onValueChange = { tempPrompt = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
fun AiLevelSliderPreference(
    selectedLevel: AiLevel,
    onLevelSelected: (AiLevel) -> Unit,
) {
    val levels = remember { AiLevel.entries.toTypedArray() }
    var sliderValue by remember(selectedLevel) { mutableFloatStateOf(selectedLevel.ordinal.toFloat()) }

    val levelColor = when (selectedLevel) {
        AiLevel.LOW -> Color(0xFF4CAF50)
        AiLevel.MED -> Color(0xFF2196F3)
        AiLevel.HIGH -> Color(0xFFFF9800)
        AiLevel.XHIGH -> Color(0xFFFF5722)
        AiLevel.MAX -> Color(0xFFE91E63)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "AI Intervention Level",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${selectedLevel.label} · ${selectedLevel.shortTitle}",
                style = MaterialTheme.typography.labelLarge,
                color = levelColor,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                sliderValue = newValue
                val newIndex = newValue.roundToInt().coerceIn(0, levels.size - 1)
                val newLevel = levels[newIndex]
                if (newLevel != selectedLevel) {
                    onLevelSelected(newLevel)
                }
            },
            valueRange = 0f..(levels.size - 1).toFloat(),
            steps = levels.size - 2,
            colors = SliderDefaults.colors(
                thumbColor = levelColor,
                activeTrackColor = levelColor,
                activeTickColor = levelColor.copy(alpha = 0.7f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = AiLevel.entries.first().shortTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = AiLevel.entries.last().shortTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = selectedLevel.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
