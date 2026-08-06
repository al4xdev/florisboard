package dev.patrickgold.florisboard.app.settings.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.settings.ai.deepseek.DeepSeekHelpContent
import dev.patrickgold.florisboard.app.settings.ai.openrouter.OpenRouterHelpContent
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun AiHelpScreen() = FlorisScreen {
    val store by FlorisPreferenceStore
    val backend by store.ai.backend.collectAsState()

    title = backend.helpTitle
    previewFieldVisible = false

    content {
        PreferenceGroup(title = "How to Configure AI Features") {
            when (backend) {
                AiBackend.OPEN_ROUTER -> OpenRouterHelpContent()
                AiBackend.DEEPSEEK -> DeepSeekHelpContent()
            }
        }
    }
}
