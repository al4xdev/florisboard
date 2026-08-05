package dev.patrickgold.florisboard.ime.smartbar.quickaction

import android.content.Context
import android.util.Log
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private val aiScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
private const val OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions"

@Serializable
@SerialName("fix_grammar")
object FixGrammar : QuickAction() {
    override fun onPointerUp(context: Context) {
        if (AiActionState.isFixGrammarRunning) return
        val ic = FlorisImeService.currentInputConnection() ?: return
        var original = ic.getSelectedText(0)?.toString()
        if (original.isNullOrEmpty()) {
            ic.performContextMenuAction(android.R.id.selectAll)
            original = ic.getSelectedText(0)?.toString()
        }
        if (original.isNullOrBlank()) return

        val prefs by FlorisPreferenceStore
        val apiKey = prefs.ai.apiKey.get()
        if (apiKey.isBlank()) {
            openAiHelpScreen(context)
            return
        }
        val model = prefs.ai.openRouterModel.get().ifBlank {
            "~deepseek/deepseek-v4-flash-latest"
        }
        val systemPrompt = prefs.ai.aiLevel.get().getSystemPrompt()
        val textToProcess = original

        aiScope.launch {
            AiActionState.isFixGrammarRunning = true
            try {
                processAndCommitText(textToProcess, systemPrompt, apiKey, model)
            } catch (e: Exception) {
                Log.w("FixGrammarAction", "request failed", e)
            } finally {
                AiActionState.isFixGrammarRunning = false
            }
        }
    }
}

@Serializable
@SerialName("custom_ai_prompt")
object CustomAiPrompt : QuickAction() {
    override fun onPointerUp(context: Context) {
        if (AiActionState.isCustomPromptRunning) return
        val ic = FlorisImeService.currentInputConnection() ?: return
        var original = ic.getSelectedText(0)?.toString()
        if (original.isNullOrEmpty()) {
            ic.performContextMenuAction(android.R.id.selectAll)
            original = ic.getSelectedText(0)?.toString()
        }
        if (original.isNullOrBlank()) return

        val prefs by FlorisPreferenceStore
        val apiKey = prefs.ai.apiKey.get()
        if (apiKey.isBlank()) {
            openAiHelpScreen(context)
            return
        }
        val model = prefs.ai.openRouterModel.get().ifBlank {
            "~deepseek/deepseek-v4-flash-latest"
        }
        val systemPrompt = prefs.ai.customPrompt.get().ifBlank {
            "You are Chewbacca the Wookiee. Translate all text into Shyriiwook (Wookiee roars like RAWRGWAWWGR, WAAAAHH, ARRRRRRRRRRR, GRRRRR, RWWGG). Keep technical terms intact if any. Reply ONLY with the Wookiee translation."
        }
        val textToProcess = original

        aiScope.launch {
            AiActionState.isCustomPromptRunning = true
            try {
                processAndCommitText(textToProcess, systemPrompt, apiKey, model)
            } catch (e: Exception) {
                Log.w("CustomAiPromptAction", "request failed", e)
            } finally {
                AiActionState.isCustomPromptRunning = false
            }
        }
    }
}

private suspend fun processAndCommitText(textToProcess: String, systemPrompt: String, apiKey: String, model: String) {
    val originalTrimmed = textToProcess.trimEnd()
    val hadPeriodAtEnd = originalTrimmed.endsWith(".")

    val rawResult = executeAiCompletion(textToProcess, systemPrompt, apiKey, model)
    var finalText = rawResult?.trimEnd()

    if (!finalText.isNullOrBlank()) {
        if (hadPeriodAtEnd) {
            if (!finalText.endsWith(".")) {
                finalText = "$finalText."
            }
        } else {
            finalText = finalText.trimEnd('.').trimEnd()
        }

        withContext(Dispatchers.Main) {
            val ic2 = FlorisImeService.currentInputConnection() ?: return@withContext
            ic2.beginBatchEdit()
            ic2.commitText(finalText, 1)
            ic2.endBatchEdit()
        }
    }
}

private fun executeAiCompletion(text: String, systemPrompt: String, apiKey: String, model: String): String? {
    val url = URL(OPENROUTER_URL)
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = "POST"
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("Authorization", "Bearer $apiKey")
    conn.doOutput = true
    conn.connectTimeout = 8000
    conn.readTimeout = 20000

    val body = buildJsonObject {
        put("model", model)
        put("temperature", 0)
        put("max_tokens", 2048)
        put("reasoning", buildJsonObject {
            put("effort", "none")
        })
        put("messages", buildJsonArray {
            add(buildJsonObject {
                put("role", "system")
                put("content", systemPrompt)
            })
            add(buildJsonObject {
                put("role", "user")
                put("content", text)
            })
        })
    }

    try {
        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
        if (conn.responseCode != 200) return null
        val response = conn.inputStream.bufferedReader().readText()
        val parsed = Json.parseToJsonElement(response).jsonObject
        return parsed["choices"]
            ?.jsonArray
            ?.getOrNull(0)
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.content
    } finally {
        conn.disconnect()
    }
}

private fun openAiHelpScreen(context: Context) {
    try {
        val intent = android.content.Intent(context, dev.patrickgold.florisboard.app.FlorisAppActivity::class.java).apply {
            action = android.content.Intent.ACTION_VIEW
            addCategory(android.content.Intent.CATEGORY_BROWSABLE)
            data = android.net.Uri.parse("ui://florisboard/settings/ai/help")
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.w("AiAction", "failed to launch AI help screen", e)
    }
}
