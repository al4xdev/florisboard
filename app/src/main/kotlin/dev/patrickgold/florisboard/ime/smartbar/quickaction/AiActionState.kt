package dev.patrickgold.florisboard.ime.smartbar.quickaction

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class RunningAiAction {
    FIX_GRAMMAR,
    CUSTOM_PROMPT,
}

data class AiActionCompletion(
    val action: RunningAiAction,
    val id: Long,
)

object AiActionState {
    var runningAction by mutableStateOf<RunningAiAction?>(null)
    var completion by mutableStateOf<AiActionCompletion?>(null)
        private set

    private var nextCompletionId = 0L

    fun complete(action: RunningAiAction) {
        completion = AiActionCompletion(action, ++nextCompletionId)
    }

    fun consumeCompletion(id: Long) {
        if (completion?.id == id) completion = null
    }
}
