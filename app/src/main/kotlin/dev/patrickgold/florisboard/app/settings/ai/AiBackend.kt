package dev.patrickgold.florisboard.app.settings.ai

enum class AiBackend(
    val label: String,
    val description: String,
    val helpTitle: String,
) {
    OPEN_ROUTER(
        label = "OpenRouter",
        description = "Routes to many models and providers through openrouter.ai",
        helpTitle = "OpenRouter Setup Guide",
    ),
    DEEPSEEK(
        label = "DeepSeek API",
        description = "Talks directly to api.deepseek.com, no provider routing",
        helpTitle = "DeepSeek Setup Guide",
    ),
}
