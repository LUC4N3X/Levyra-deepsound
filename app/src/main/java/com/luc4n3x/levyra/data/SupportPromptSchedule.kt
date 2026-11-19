package com.luc4n3x.levyra.data

data class SupportPromptState(
    val versionCode: Int,
    val opens: Int,
    val lastOpenAtMs: Long,
    val finished: Boolean
)

internal object SupportPromptSchedule {
    const val OPENS_PER_PROMPT = 10
    const val LAST_PROMPT_OPEN = 51
    const val SESSION_GAP_MS = 30L * 60L * 1_000L

    fun recordOpen(state: SupportPromptState, versionCode: Int, nowMs: Long): SupportPromptState? {
        val sameVersion = state.versionCode == versionCode
        val elapsed = nowMs - state.lastOpenAtMs
        if (sameVersion && state.lastOpenAtMs > 0L && elapsed in 0L until SESSION_GAP_MS) return null
        val previousOpens = if (sameVersion) state.opens else 0
        return state.copy(
            versionCode = versionCode,
            opens = (previousOpens + 1).coerceAtMost(LAST_PROMPT_OPEN + 1),
            lastOpenAtMs = nowMs
        )
    }

    fun isDue(state: SupportPromptState): Boolean =
        !state.finished &&
            state.opens in 1..LAST_PROMPT_OPEN &&
            state.opens % OPENS_PER_PROMPT == 1
}
