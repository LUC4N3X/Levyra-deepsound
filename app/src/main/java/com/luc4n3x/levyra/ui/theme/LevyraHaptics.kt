package com.luc4n3x.levyra.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

enum class LevyraHapticAction {
    Favorite,
    SeekSnap,
    TrackSwipe,
    Reorder,
    Confirm
}

@Immutable
class LevyraHaptics(
    private val feedback: HapticFeedback?,
    private val enabled: Boolean
) {
    fun perform(action: LevyraHapticAction) {
        if (!enabled) return
        val target = feedback ?: return
        target.performHapticFeedback(typeFor(action))
    }

    private fun typeFor(action: LevyraHapticAction): HapticFeedbackType = when (action) {
        LevyraHapticAction.SeekSnap, LevyraHapticAction.Reorder -> HapticFeedbackType.TextHandleMove
        LevyraHapticAction.Favorite, LevyraHapticAction.TrackSwipe, LevyraHapticAction.Confirm ->
            HapticFeedbackType.LongPress
    }

    companion object {
        val Disabled = LevyraHaptics(feedback = null, enabled = false)
    }
}

val LocalLevyraHaptics = staticCompositionLocalOf { LevyraHaptics.Disabled }

@Composable
fun rememberLevyraHaptics(enabled: Boolean): LevyraHaptics {
    val feedback = LocalHapticFeedback.current
    return remember(feedback, enabled) { LevyraHaptics(feedback, enabled) }
}
