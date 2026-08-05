package dev.patrickgold.florisboard.app.settings.ai

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.PreferenceGroup

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun AiHelpScreen() = FlorisScreen {
    title = "OpenRouter Setup Guide"
    previewFieldVisible = false

    content {
        val context = LocalContext.current

        PreferenceGroup(title = "How to Configure AI Features") {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Follow these simple steps to enable AI text actions on your keyboard:",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "1. Get an API Key",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Sign up or log into OpenRouter at openrouter.ai and generate a new API Key.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai/keys"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Get API Key on OpenRouter")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "2. Enter Key in FlorisBoard",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Go back to 'AI Features' settings and paste your key into the 'API Key' field.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "3. Choose AI Model & Level",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Select your preferred model tag (e.g. ~deepseek/deepseek-v4-flash-latest) and adjust the AI Intervention slider from Low to Max.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "4. Use Keyboard Actions",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "On your keyboard toolbar, tap the FixGrammar (spellcheck) or AI Prompt (sparkles) icon to run instant AI text completions!",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
