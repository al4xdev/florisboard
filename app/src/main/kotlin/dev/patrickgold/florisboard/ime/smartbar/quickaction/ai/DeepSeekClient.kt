package dev.patrickgold.florisboard.ime.smartbar.quickaction.ai

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The native DeepSeek API silently ignores `provider` and `reasoning`. Ignoring `reasoning` means
 * thinking stays enabled, so this backend disables it via the `thinking` field instead and never
 * sends a provider block.
 */
internal object DeepSeekClient : AiClient {
    override val endpointUrl = "https://api.deepseek.com/chat/completions"

    override fun buildRequestBody(
        text: String,
        systemPrompt: String,
        model: String,
        provider: String,
        useStructuredGrammarOutput: Boolean,
    ): JsonObject = buildJsonObject {
        put("model", model)
        put("temperature", 0)
        put("max_tokens", 2048)
        put("thinking", buildJsonObject {
            put("type", "disabled")
        })
        if (useStructuredGrammarOutput) {
            putGrammarToolContract()
        }
        putChatMessages(systemPrompt, text)
    }
}
