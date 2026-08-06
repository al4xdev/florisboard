package dev.patrickgold.florisboard.ime.smartbar.quickaction

import android.content.Context
import android.util.Log
import android.widget.Toast
import dev.patrickgold.florisboard.FlorisImeService
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.app.settings.ai.AiBackend
import dev.patrickgold.florisboard.app.settings.ai.AiDefaults
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.smartbar.quickaction.ai.AiConfig
import dev.patrickgold.florisboard.ime.smartbar.quickaction.ai.AiOperation
import dev.patrickgold.florisboard.ime.smartbar.quickaction.ai.AiRequestException
import dev.patrickgold.florisboard.ime.smartbar.quickaction.ai.DeepSeekClient
import dev.patrickgold.florisboard.ime.smartbar.quickaction.ai.OpenRouterClient
import dev.patrickgold.florisboard.ime.smartbar.quickaction.ai.executeAiCompletion
import dev.patrickgold.florisboard.subtypeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.florisboard.lib.android.showLongToast
import java.lang.ref.WeakReference
import java.net.SocketTimeoutException

private val aiScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
private var lastAiToastReference = WeakReference<Toast>(null)

@Serializable
@SerialName("fix_grammar")
object FixGrammar : QuickAction() {
    override fun onPointerUp(context: Context) {
        startAiAction(
            context = context,
            action = RunningAiAction.FIX_GRAMMAR,
            logTag = "FixGrammarAction",
            useStructuredGrammarOutput = true,
        ) {
            val prefs by FlorisPreferenceStore
            val languageTag = try {
                context.subtypeManager().value.activeSubtype.primaryLocale.languageTag()
            } catch (_: Exception) { null }
            prefs.ai.aiLevel.get().getSystemPrompt(languageTag)
        }
    }
}

@Serializable
@SerialName("custom_ai_prompt")
object CustomAiPrompt : QuickAction() {
    override fun onPointerUp(context: Context) {
        startAiAction(
            context = context,
            action = RunningAiAction.CUSTOM_PROMPT,
            logTag = "CustomAiPromptAction",
        ) {
            val prefs by FlorisPreferenceStore
            prefs.ai.customPrompt.get().ifBlank { AiDefaults.CUSTOM_PROMPT }
        }
    }
}

private fun resolveAiConfig(): AiConfig? {
    val prefs by FlorisPreferenceStore
    return when (val backend = prefs.ai.backend.get()) {
        AiBackend.OPEN_ROUTER -> {
            val apiKey = prefs.ai.openRouterApiKey.get()
            if (apiKey.isBlank()) return null
            AiConfig(
                backend = backend,
                client = OpenRouterClient,
                apiKey = apiKey,
                model = prefs.ai.openRouterModel.get().ifBlank { AiDefaults.OPEN_ROUTER_MODEL },
                provider = prefs.ai.openRouterProvider.get().trim(),
            )
        }
        AiBackend.DEEPSEEK -> {
            val apiKey = prefs.ai.deepSeekApiKey.get()
            if (apiKey.isBlank()) return null
            AiConfig(
                backend = backend,
                client = DeepSeekClient,
                apiKey = apiKey,
                model = prefs.ai.deepSeekModel.get().ifBlank { AiDefaults.DEEPSEEK_MODEL },
                provider = "",
            )
        }
    }
}

private fun startAiAction(
    context: Context,
    action: RunningAiAction,
    logTag: String,
    useStructuredGrammarOutput: Boolean = false,
    systemPromptProvider: () -> String,
) {
    if (AiActionState.runningAction != null) return
    val ic = FlorisImeService.currentInputConnection() ?: return
    var original = ic.getSelectedText(0)?.toString()
    if (original.isNullOrEmpty()) {
        ic.performContextMenuAction(android.R.id.selectAll)
        original = ic.getSelectedText(0)?.toString()
    }
    if (original.isNullOrBlank()) return

    val config = resolveAiConfig()
    if (config == null) {
        openAiHelpScreen(context)
        return
    }
    val systemPrompt = systemPromptProvider()
    val textToProcess = original
    val editorInstance by context.editorInstance()
    val operation = AiOperation(ic, editorInstance.activeInfo, textToProcess)

    AiActionState.runningAction = action
    aiScope.launch {
        val monitorJob = monitorAiTarget(context, operation)
        var completed = false
        try {
            completed = processAndCommitText(
                textToProcess,
                systemPrompt,
                config,
                context,
                operation,
                useStructuredGrammarOutput,
            )
        } catch (e: Exception) {
            if (!operation.isCanceled) {
                Log.w(logTag, "request failed", e)
                showAiRequestError(context, config.backend, e)
            }
        } finally {
            monitorJob.cancel()
            withContext(Dispatchers.Main.immediate) {
                if (AiActionState.runningAction == action) {
                    AiActionState.runningAction = null
                    if (completed) AiActionState.complete(action)
                }
            }
        }
    }
}

private suspend fun processAndCommitText(
    textToProcess: String,
    systemPrompt: String,
    config: AiConfig,
    context: Context,
    operation: AiOperation,
    useStructuredGrammarOutput: Boolean = false,
): Boolean {
    val originalTrimmed = textToProcess.trimEnd()
    val hadPeriodAtEnd = originalTrimmed.endsWith(".")

    val rawResult = executeAiCompletion(
        config,
        textToProcess,
        systemPrompt,
        operation,
        useStructuredGrammarOutput,
    )
    var finalText = rawResult.trimEnd()

    if (finalText.isBlank()) throw AiRequestException()
    if (hadPeriodAtEnd) {
        if (!finalText.endsWith(".")) {
            finalText = "$finalText."
        }
    } else {
        finalText = finalText.trimEnd('.').trimEnd()
    }

    val committed = withContext(Dispatchers.Main.immediate) {
        if (!isAiTargetValidOnMain(context, operation)) return@withContext false
        operation.finish()
        operation.inputConnection.beginBatchEdit()
        operation.inputConnection.commitText(finalText, 1)
        operation.inputConnection.endBatchEdit()
        true
    }
    if (!committed) cancelAiOperation(context, operation)
    return committed
}

private fun monitorAiTarget(context: Context, operation: AiOperation) = aiScope.launch {
    val editorInstance by context.editorInstance()
    combine(editorInstance.activeInfoFlow, editorInstance.activeContentFlow) { _, _ -> Unit }
        .first { operation.isFinished || !isAiTargetValid(context, operation) }
    if (!operation.isFinished) cancelAiOperation(context, operation)
}

private suspend fun isAiTargetValid(context: Context, operation: AiOperation): Boolean {
    return withContext(Dispatchers.Main.immediate) {
        isAiTargetValidOnMain(context, operation)
    }
}

private fun isAiTargetValidOnMain(context: Context, operation: AiOperation): Boolean {
    val editorInstance by context.editorInstance()
    val currentConnection = FlorisImeService.currentInputConnection()
    return !operation.isCanceled &&
        editorInstance.activeInfo == operation.editorInfo &&
        currentConnection === operation.inputConnection &&
        currentConnection.getSelectedText(0)?.toString() == operation.originalText
}

private suspend fun cancelAiOperation(context: Context, operation: AiOperation) {
    if (!operation.cancel()) return
    showAiToast(context, "AI request canceled because the text field or selection changed.")
}

private suspend fun showAiRequestError(context: Context, backend: AiBackend, error: Exception) {
    val message = when {
        error is SocketTimeoutException -> "AI request timed out. Try again."
        error is AiRequestException && error.statusCode == 401 -> "AI request failed (HTTP 401). Check your API key."
        error is AiRequestException && error.statusCode == 402 -> when (backend) {
            AiBackend.OPEN_ROUTER -> "AI request failed (HTTP 402). Check your OpenRouter balance."
            AiBackend.DEEPSEEK -> "AI request failed (HTTP 402). Check your DeepSeek balance."
        }
        error is AiRequestException && error.statusCode == 404 -> when (backend) {
            AiBackend.OPEN_ROUTER -> "AI request failed (HTTP 404). Check the model and provider."
            AiBackend.DEEPSEEK -> "AI request failed (HTTP 404). Check the model slug."
        }
        error is AiRequestException && error.statusCode == 429 -> "AI request failed (HTTP 429). Try again shortly."
        error is AiRequestException && error.statusCode != null -> "AI request failed (HTTP ${error.statusCode}). Try again."
        else -> when (backend) {
            AiBackend.OPEN_ROUTER -> "AI request failed. Check the model and provider settings."
            AiBackend.DEEPSEEK -> "AI request failed. Check the model setting."
        }
    }
    showAiToast(context, message)
}

private suspend fun showAiToast(context: Context, message: String) {
    withContext(Dispatchers.Main.immediate) {
        lastAiToastReference.get()?.cancel()
        lastAiToastReference = WeakReference(context.showLongToast(message))
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
