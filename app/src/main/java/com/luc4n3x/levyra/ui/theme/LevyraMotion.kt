package com.luc4n3x.levyra.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.IntOffset
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled

@Immutable
data class LevyraSpring(
    val dampingRatio: Float,
    val stiffness: Float
) {
    fun <T> spec(): SpringSpec<T> = spring(dampingRatio = dampingRatio, stiffness = stiffness)

    fun <T> spec(visibilityThreshold: T): SpringSpec<T> =
        spring(dampingRatio = dampingRatio, stiffness = stiffness, visibilityThreshold = visibilityThreshold)
}

private const val PanelRestScale = 0.98f
private const val PageEnterScale = 0.965f
private const val PageExitScale = 0.985f

object LevyraMotion {

    object Durations {
        const val Instant: Int = 90
        const val Quick: Int = 120
        const val Short: Int = 180
        const val Medium: Int = 260
        const val Long: Int = 420
        const val Palette: Int = 650
    }

    object Easings {
        val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val Standard: Easing = CubicBezierEasing(0.3f, 0f, 0.1f, 1f)
        val Decelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
        val Accelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    }

    object Scale {
        const val Row: Float = 0.985f
        const val Tile: Float = 0.975f
        const val Surface: Float = 0.98f
        const val Control: Float = 0.96f
        const val Lifted: Float = 1.012f
    }

    object Springs {
        const val PressDamping: Float = 0.72f
        const val PressStiffness: Float = 900f
        const val ReleaseDamping: Float = 0.58f
        const val ReleaseStiffness: Float = 760f
        const val ExpandDamping: Float = 0.82f
        const val ExpandStiffness: Float = 360f
        const val CollapseDamping: Float = 0.86f
        const val CollapseStiffness: Float = 430f
        const val SettleDamping: Float = 0.90f
        const val SettleStiffness: Float = 540f
        const val SnappyDamping: Float = 0.82f
        const val SnappyStiffness: Float = 1_100f
        const val ExpressiveDamping: Float = 0.64f
        const val ExpressiveStiffness: Float = 360f
        const val ReorderDamping: Float = 0.86f
        const val ReorderStiffness: Float = 700f
        const val SpatialDamping: Float = 0.88f
        const val SpatialStiffness: Float = 420f
        const val GestureDamping: Float = 0.92f
        const val GestureStiffness: Float = 620f
        const val FeedbackDamping: Float = 0.62f
        const val FeedbackStiffness: Float = 1_300f
    }

    val press = LevyraSpring(Springs.PressDamping, Springs.PressStiffness)
    val release = LevyraSpring(Springs.ReleaseDamping, Springs.ReleaseStiffness)
    val expand = LevyraSpring(Springs.ExpandDamping, Springs.ExpandStiffness)
    val collapse = LevyraSpring(Springs.CollapseDamping, Springs.CollapseStiffness)
    val settle = LevyraSpring(Springs.SettleDamping, Springs.SettleStiffness)
    val snappy = LevyraSpring(Springs.SnappyDamping, Springs.SnappyStiffness)
    val expressive = LevyraSpring(Springs.ExpressiveDamping, Springs.ExpressiveStiffness)
    val reorder = LevyraSpring(Springs.ReorderDamping, Springs.ReorderStiffness)
    val spatial = LevyraSpring(Springs.SpatialDamping, Springs.SpatialStiffness)
    val gesture = LevyraSpring(Springs.GestureDamping, Springs.GestureStiffness)
    val feedback = LevyraSpring(Springs.FeedbackDamping, Springs.FeedbackStiffness)

    val enabled: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalAnimationsEnabled.current

    fun <T> spec(enabled: Boolean, spec: AnimationSpec<T>): AnimationSpec<T> =
        if (enabled) spec else snap()

    fun <T> physics(enabled: Boolean, spring: LevyraSpring): AnimationSpec<T> =
        if (enabled) spring.spec() else snap()

    fun <T> fade(durationMillis: Int = Durations.Short): TweenSpec<T> =
        tween(durationMillis = durationMillis, easing = Easings.Standard)

    fun <T> crossfade(): TweenSpec<T> =
        tween(durationMillis = Durations.Medium, easing = Easings.Standard)

    fun <T> artwork(): TweenSpec<T> =
        tween(durationMillis = Durations.Long, easing = Easings.Decelerate)

    fun <T> palette(): TweenSpec<T> =
        tween(durationMillis = Durations.Palette, easing = Easings.Decelerate)

    fun <T> emphasized(durationMillis: Int = Durations.Medium + 60): TweenSpec<T> =
        tween(durationMillis = durationMillis, easing = Easings.Emphasized)

    fun <T> standard(durationMillis: Int = Durations.Medium - 40): TweenSpec<T> =
        tween(durationMillis = durationMillis, easing = Easings.Standard)

    fun overlayEnter(enabled: Boolean): EnterTransition =
        if (enabled) fadeIn(tween(Durations.Short, easing = Easings.Decelerate)) else EnterTransition.None

    fun overlayExit(enabled: Boolean): ExitTransition =
        if (enabled) fadeOut(tween(Durations.Quick + 20, easing = Easings.Standard)) else ExitTransition.None

    fun sheetEnter(enabled: Boolean): EnterTransition =
        if (enabled) {
            slideInVertically(expand.spec(visibilityThreshold = IntOffset.VisibilityThreshold)) { it / 3 } +
                fadeIn(tween(Durations.Short, easing = Easings.Decelerate))
        } else {
            EnterTransition.None
        }

    fun sheetExit(enabled: Boolean): ExitTransition =
        if (enabled) {
            slideOutVertically(tween(Durations.Short, easing = Easings.Accelerate)) { it / 4 } +
                fadeOut(tween(Durations.Quick, easing = Easings.Standard))
        } else {
            ExitTransition.None
        }

    fun panelEnter(enabled: Boolean): EnterTransition =
        if (enabled) {
            slideInVertically(spatial.spec(visibilityThreshold = IntOffset.VisibilityThreshold)) { it / 12 } +
                scaleIn(spatial.spec(), initialScale = PanelRestScale) +
                fadeIn(tween(Durations.Short, easing = Easings.Decelerate))
        } else {
            EnterTransition.None
        }

    fun panelExit(enabled: Boolean): ExitTransition =
        if (enabled) {
            slideOutVertically(tween(Durations.Short, easing = Easings.Accelerate)) { it / 16 } +
                scaleOut(tween(Durations.Short, easing = Easings.Accelerate), targetScale = PanelRestScale) +
                fadeOut(tween(Durations.Quick + 20, easing = Easings.Standard))
        } else {
            ExitTransition.None
        }

    fun pageEnter(enabled: Boolean): EnterTransition =
        if (enabled) {
            scaleIn(tween(Durations.Medium + 60, easing = Easings.Emphasized), initialScale = PageEnterScale) +
                fadeIn(tween(Durations.Short + 20, easing = Easings.Decelerate))
        } else {
            EnterTransition.None
        }

    fun pageExit(enabled: Boolean): ExitTransition =
        if (enabled) {
            scaleOut(tween(Durations.Short, easing = Easings.Accelerate), targetScale = PageExitScale) +
                fadeOut(tween(Durations.Short - 20, easing = Easings.Standard))
        } else {
            ExitTransition.None
        }

    fun tabSwap(enabled: Boolean, direction: Int): ContentTransform =
        if (enabled) {
            val enter = slideInHorizontally(tween(Durations.Medium + 60, easing = Easings.Emphasized)) { it * direction } +
                fadeIn(tween(Durations.Short + 40, easing = Easings.Decelerate))
            val exit = slideOutHorizontally(tween(Durations.Medium - 20, easing = Easings.Emphasized)) { -it * direction / 3 } +
                fadeOut(tween(Durations.Short - 20, easing = Easings.Standard))
            enter togetherWith exit
        } else {
            EnterTransition.None togetherWith ExitTransition.None
        }

    /**
     * Metadata and artwork swap for a track change. [direction] is +1 when the queue moved forward,
     * -1 when it moved back and 0 when the change did not come from a queue step; it is already
     * expressed in screen space, so callers mirror it for RTL.
     */
    fun trackChange(enabled: Boolean, direction: Int): ContentTransform {
        if (!enabled) return EnterTransition.None togetherWith ExitTransition.None
        val enter = if (direction == 0) {
            slideInVertically(settle.spec(visibilityThreshold = IntOffset.VisibilityThreshold)) { it / 4 }
        } else {
            slideInHorizontally(spatial.spec(visibilityThreshold = IntOffset.VisibilityThreshold)) { it * direction / 5 }
        }
        val exit = if (direction == 0) {
            slideOutVertically(tween(Durations.Short - 40, easing = Easings.Accelerate)) { -it / 4 }
        } else {
            slideOutHorizontally(tween(Durations.Short - 20, easing = Easings.Accelerate)) { -it * direction / 6 }
        }
        return ContentTransform(
            targetContentEnter = enter + fadeIn(tween(Durations.Short + 40, delayMillis = 40, easing = Easings.Decelerate)),
            initialContentExit = exit + fadeOut(tween(Durations.Instant + 10, easing = Easings.Standard)),
            sizeTransform = SizeTransform(clip = false)
        )
    }

    fun contentSwap(enabled: Boolean): ContentTransform =
        if (enabled) {
            fadeIn(tween(Durations.Medium - 40, delayMillis = Durations.Instant - 30, easing = Easings.Decelerate)) togetherWith
                fadeOut(tween(Durations.Instant, easing = Easings.Standard))
        } else {
            EnterTransition.None togetherWith ExitTransition.None
        }
}
