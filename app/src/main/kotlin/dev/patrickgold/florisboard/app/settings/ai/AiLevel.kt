package dev.patrickgold.florisboard.app.settings.ai

enum class AiLevel(val label: String, val shortTitle: String, val summary: String) {
    LOW("Low", "Minimal", "Fix only severe typos and spelling errors"),
    MED("Med", "Standard", "Fix grammar and spelling while preserving structure"),
    HIGH("High", "Smooth", "Subtle flow improvements, keeps your words and slang"),
    XHIGH("XHigh", "Expressive", "Gentle rephrasing for natural flow, preserves tone"),
    MAX("Max", "Full Polish", "Full professional polish and sharp clarity");

    fun getSystemPrompt(languageTag: String? = null): String {
        val langHint = if (!languageTag.isNullOrBlank()) {
            "\nThe user's keyboard language is $languageTag."
        } else ""

        val baseRules = """$langHint
Rules:
- Reply in the exact same language as the input text.
- Do NOT translate technical terms (code, commands, git terms like commit/branch/merge/rebase, framework names, APIs, jargon, file paths, URLs, identifiers, quoted strings).
- Do not change code, commands, file paths, URLs, identifiers, or quoted strings.
- Reply with ONLY the result text. No explanations, no quotes, no preamble."""

        return when (this) {
            LOW -> """Fix ONLY obvious spelling and typo errors in the input text.
Do NOT change sentence structure, style, or word choice.
$baseRules"""

            MED -> """Fix ONLY grammar, spelling, punctuation, typos, and capitalization errors.
Keep the original sentence structure and tone intact.
$baseRules"""

            HIGH -> """Fix grammar and spelling. You may make SMALL, SUBTLE improvements to punctuation and flow, but keep the original words, slang, abbreviations, and sentence structure as close to the original as possible.
Do NOT rewrite or heavily rephrase.
$baseRules"""

            XHIGH -> """Fix grammar and spelling, and gently rephrase for natural flow while keeping the original tone and vocabulary.
You may adjust word order or add minor connecting words, but do NOT heavily rewrite.
$baseRules"""

            MAX -> """Full professional polish: fix all errors and transform into clear, fluent, professional style while retaining the core message.
$baseRules"""
        }
    }
}
