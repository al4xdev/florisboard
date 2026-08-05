package dev.patrickgold.florisboard.ime.smartbar.quickaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AiActionState {
    var isFixGrammarRunning by mutableStateOf(false)
    var isCustomPromptRunning by mutableStateOf(false)
}
