package com.luc4n3x.levyra.ui.album

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import com.luc4n3x.levyra.ui.PlayerMinimumContrast
import com.luc4n3x.levyra.ui.harmonizePlayerAccents
import com.luc4n3x.levyra.ui.playerAdjustForegroundToward
import com.luc4n3x.levyra.ui.playerAmbienceOf
import com.luc4n3x.levyra.ui.playerContrastGradient
import com.luc4n3x.levyra.ui.playerMix

@Immutable
internal data class AlbumStageColors(
    val fieldTop: Color,
    val fieldMid: Color,
    val base: Color,
    val content: Color,
    val contentMuted: Color,
    val accent: Color,
    val actionStart: Color,
    val actionEnd: Color,
    val actionContent: Color,
    val secondaryFill: Color,
    val hairline: Color
)

internal val AlbumStageLightInk = Color(0xFF111218)
internal val AlbumNeutralPaletteStart = Color(0xFF8E95A1)
internal val AlbumNeutralPaletteEnd = Color(0xFF585E69)
internal const val AlbumHeroDissolve = 0.40f
internal const val AlbumSplitHeroFraction = 0.46f
internal const val AlbumSplitListStartFraction = 0.42f
internal val AlbumHeaderOverlap: Dp = 28.dp
internal val AlbumFieldTail: Dp = 360.dp
private val AlbumContentMaxWidth: Dp = 680.dp
private val AlbumMinimumGutter: Dp = 20.dp

internal fun albumStageColors(primary: Color, secondary: Color, lightTheme: Boolean): AlbumStageColors {
    val accents = harmonizePlayerAccents(primary, secondary)
    val action = playerContrastGradient(accents.primary, accents.secondary, PlayerMinimumContrast)
    val fieldTop: Color
    val fieldMid: Color
    val base: Color
    val content: Color
    val mutedAlpha: Float
    if (lightTheme) {
        fieldTop = accents.primary.playerMix(Color.White, 0.80f).copy(alpha = 1f)
        fieldMid = accents.primary.playerMix(Color.White, 0.91f).copy(alpha = 1f)
        base = accents.primary.playerMix(Color.White, 0.96f).copy(alpha = 1f)
        content = AlbumStageLightInk
        mutedAlpha = 0.68f
    } else {
        val ambience = playerAmbienceOf(accents.primary, accents.secondary)
        fieldTop = ambience.tint.playerMix(ambience.control, 0.30f).copy(alpha = 1f)
        fieldMid = ambience.elevated.playerMix(ambience.tint, 0.18f).copy(alpha = 1f)
        base = ambience.base
        content = Color.White
        mutedAlpha = 0.70f
    }
    val fields = listOf(fieldTop, fieldMid, base)
    val muted = content.copy(alpha = mutedAlpha)
        .playerAdjustForegroundToward(content, fields, PlayerMinimumContrast)
        .color
    val accent = accents.primary
        .playerAdjustForegroundToward(content, fields, PlayerMinimumContrast)
        .color
    return AlbumStageColors(
        fieldTop = fieldTop,
        fieldMid = fieldMid,
        base = base,
        content = content,
        contentMuted = muted,
        accent = accent,
        actionStart = action.start,
        actionEnd = action.end,
        actionContent = action.content,
        secondaryFill = content.copy(alpha = 0.10f),
        hairline = content.copy(alpha = 0.09f)
    )
}

internal fun albumStackedHeroHeight(width: Dp, height: Dp): Dp =
    max(min(width * 1.06f, height * 0.60f), min(width, 280.dp))

internal fun albumContentGutter(width: Dp): Dp =
    max(AlbumMinimumGutter, (width - AlbumContentMaxWidth) / 2)
