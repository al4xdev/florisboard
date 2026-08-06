package dev.patrickgold.florisboard.app.settings.ai.deepseek

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
import dev.patrickgold.florisboard.app.settings.ai.AiDefaults

@Composable
fun DeepSeekHelpContent() {
    val context = LocalContext.current

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
            text = "Sign up or log into the DeepSeek open platform at platform.deepseek.com and create a new API Key.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.deepseek.com/api_keys"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text("Get API Key on DeepSeek")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "2. Enter Key in FlorisBoard",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Go back to 'AI Features' settings, make sure the backend is set to DeepSeek API, and paste your key into the 'API Key' field. OpenRouter and DeepSeek keys are stored separately and are not interchangeable.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "3. Choose AI Model & Level",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Pick ${AiDefaults.DEEPSEEK_MODEL} or ${AiDefaults.DEEPSEEK_MODEL_PRO} and adjust the AI Intervention slider from Low to Max. There is no provider setting on this backend.",
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
