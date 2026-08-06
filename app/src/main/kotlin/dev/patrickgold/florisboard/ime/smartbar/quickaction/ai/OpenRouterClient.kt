package dev.patrickgold.florisboard.ime.smartbar.quickaction.ai

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal object OpenRouterClient : AiClient {
    override val endpointUrl = "https://openrouter.ai/api/v1/chat/completions"

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
        put("reasoning", buildJsonObject {
            put("effort", "none")
        })
        if (provider.isNotBlank() || useStructuredGrammarOutput) {
            put("provider", buildJsonObject {
                if (provider.isNotBlank()) {
                    put("only", buildJsonArray {
                        add(provider)
                    })
                    put("allow_fallbacks", false)
                }
                if (useStructuredGrammarOutput) {
                    put("require_parameters", true)
                }
            })
        }
        if (useStructuredGrammarOutput) {
            putGrammarToolContract()
        }
        putChatMessages(systemPrompt, text)
    }
}
