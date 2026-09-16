package com.luc4n3x.levyra.ui.player

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.luc4n3x.levyra.ui.PlayerDarkSurface
import com.luc4n3x.levyra.ui.PlayerMinimumContrast
import com.luc4n3x.levyra.ui.PlayerStrongContrast
import com.luc4n3x.levyra.ui.playerAdjustForegroundToward
import com.luc4n3x.levyra.ui.playerCompositeOver
import com.luc4n3x.levyra.ui.playerMix

@Immutable
internal data class PlayerSurfaceTokens(
    val amoled: Boolean,
    val control: Color,
    val controlQuiet: Color,
    val outline: Color,
    val active: Color,
    val activeContent: Color,
    val hero: Color,
    val heroContent: Color,
    val content: Color,
    val contentMuted: Color,
    val contentFaint: Color,
    val glow: Color
)

private const val HeroLift = 0.80f
private const val HeroInkDepth = 0.84f
private const val ActiveLift = 0.42f
private const val ActiveAlpha = 0.32f
private const val ActiveContentLift = 0.86f
private const val AmoledControlDepth = 0.86f
private const val AmoledQuietDepth = 0.92f

internal fun playerSurfaceTokens(primary: Color, amoled: Boolean): PlayerSurfaceTokens {
    val accent = primary.copy(alpha = 1f)
    val hero = accent.playerMix(Color.White, HeroLift)
    val heroContent = accent.playerMix(PlayerDarkSurface, HeroInkDepth)
        .playerAdjustForegroundToward(Color.Black, listOf(hero), PlayerStrongContrast)
        .color
    val active = accent.playerMix(Color.White, ActiveLift).copy(alpha = ActiveAlpha)
    val activeBase = if (amoled) Color.Black else PlayerDarkSurface
    val activeContent = accent.playerMix(Color.White, ActiveContentLift)
        .playerAdjustForegroundToward(
            Color.White,
            listOf(active.playerCompositeOver(activeBase)),
            PlayerMinimumContrast
        )
        .color
    return if (amoled) {
        PlayerSurfaceTokens(
            amoled = true,
            control = accent.playerMix(Color.Black, AmoledControlDepth),
            controlQuiet = accent.playerMix(Color.Black, AmoledQuietDepth),
            outline = accent.copy(alpha = 0.22f),
            active = active,
            activeContent = activeContent,
            hero = hero,
            heroContent = heroContent,
            content = Color.White,
            contentMuted = Color.White.copy(alpha = 0.66f),
            contentFaint = Color.White.copy(alpha = 0.46f),
            glow = accent
        )
    } else {
        PlayerSurfaceTokens(
            amoled = false,
            control = Color.White.copy(alpha = 0.13f),
            controlQuiet = Color.White.copy(alpha = 0.075f),
            outline = Color.White.copy(alpha = 0.07f),
            active = active,
            activeContent = activeContent,
            hero = hero,
            heroContent = heroContent,
            content = Color.White,
            contentMuted = Color.White.copy(alpha = 0.68f),
            contentFaint = Color.White.copy(alpha = 0.48f),
            glow = accent
        )
    }
}

internal val PlayerSurfaceTokens.segmentOutline: Color
    get() = if (amoled) outline else Color.Transparent

internal fun PlayerSurfaceTokens.fillFor(active: Boolean): Color =
    if (active) this.active else controlQuiet

internal fun PlayerSurfaceTokens.tintFor(active: Boolean, idle: Color = contentMuted): Color =
    if (active) activeContent else idle
