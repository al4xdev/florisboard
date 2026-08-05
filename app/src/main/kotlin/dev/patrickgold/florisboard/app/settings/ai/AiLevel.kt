package dev.patrickgold.florisboard.app.settings.ai

enum class AiLevel(val label: String, val shortTitle: String, val summary: String) {
    LOW("Low", "Minimal", "Fix only severe typos and spelling errors"),
    MED("Med", "Standard", "Fix grammar and spelling while preserving structure"),
    HIGH("High", "Vibing", "Improve sentence flow and natural fluency"),
    XHIGH("XHigh", "Expressive", "Rephrase for expressive, fluent technical style"),
    MAX("Max", "Full Polish", "Full professional polish and sharp clarity");

    fun getSystemPrompt(): String {
        val baseRules = """Rules:
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

            HIGH -> """Fix grammar and spelling while improving sentence flow and natural fluency ("vibing").
Keep the original meaning intact.
$baseRules"""

            XHIGH -> """Fix grammar, spelling, and rephrase sentences to sound highly natural, clear, and expressive in a technical context.
$baseRules"""

            MAX -> """Transform the input text into maximum polish, sharp clarity, and flawless professional technical style.
Retain the exact original core message.
$baseRules"""
        }
    }
}
