package com.luc4n3x.levyra.ui

import androidx.compose.runtime.getValue
import com.luc4n3x.levyra.ui.theme.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.LevyraLyricsStudioSettings
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricVocalRole
import com.luc4n3x.levyra.domain.LyricsActiveColorMode
import com.luc4n3x.levyra.domain.LyricsAlignment
import com.luc4n3x.levyra.domain.LyricsFontWeightPreset
import com.luc4n3x.levyra.domain.LyricsGlowEffect
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.lyrics.LYRICS_INSTRUMENTAL_DOT_COUNT
import com.luc4n3x.levyra.ui.lyrics.LyricsInstrumentalGap
import com.luc4n3x.levyra.ui.lyrics.adaptiveLyricFontSizeSp
import com.luc4n3x.levyra.ui.lyrics.lyricLineScaleSafeWidthPx
import com.luc4n3x.levyra.ui.lyrics.lyricsInstrumentalDotIntensity
import com.luc4n3x.levyra.ui.lyrics.lyricsInstrumentalProgress
import com.luc4n3x.levyra.ui.theme.LevyraMotion
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.levyraFontFamilyFor
import java.util.Locale

@Composable
internal fun KaraokeLyricLine(
    line: LyricLine,
    positionProvider: () -> Long,
    instrumentalGap: LyricsInstrumentalGap?,
    isActive: Boolean,
    isPrimaryActive: Boolean,
    synced: Boolean,
    viewMode: LyricsViewMode,
    distanceFromActive: Int,
    focusMode: Boolean,
    layoutWidthDp: Int?,
    blurEnabled: Boolean,
    animationsEnabled: Boolean,
    sectionLabel: String?,
    showRomanization: Boolean,
    showTranslation: Boolean,
    accentEnd: Color,
    selectionMode: Boolean,
    selected: Boolean,
    selectable: Boolean,
    studioSettings: LevyraLyricsStudioSettings = LevyraLyricsStudioSettings(),
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val cinema = viewMode == LyricsViewMode.CINEMA
    val compact = viewMode == LyricsViewMode.COMPACT
    val strings = LocalLevyraStrings.current
    val currentLongClick by rememberUpdatedState(onLongClick)
    val selectionIndicator = rememberVectorPainter(Icons.Rounded.CheckCircle)
    val lineAccessibilityActions = remember(strings.shareLyrics, selectable) {
        if (selectable) {
            listOf(
                CustomAccessibilityAction(strings.shareLyrics) {
                    currentLongClick()
                    true
                }
            )
        } else {
            emptyList()
        }
    }
    val sectionLocale = remember(strings.code) { Locale.forLanguageTag(strings.code.replace('_', '-')) }
    val lyricLayoutDirection = LocalLayoutDirection.current
    val alignment = lyricLineAlignment(line.role, studioSettings.alignment)
    val textAlign = lyricLineTextAlign(line.role, studioSettings.alignment)
    val roleScale = lyricRoleScale(line.role, compact)
    val maxActiveScale = if (compact) LYRICS_COMPACT_ACTIVE_LINE_SCALE else LYRICS_ACTIVE_LINE_SCALE
    val activeScale by animateFloatAsState(
        targetValue = when {
            isPrimaryActive && compact -> LYRICS_COMPACT_ACTIVE_LINE_SCALE
            isPrimaryActive -> LYRICS_ACTIVE_LINE_SCALE
            else -> studioSettings.inactiveScale
        },
        animationSpec = LevyraMotion.physics(animationsEnabled, LevyraMotion.spatial),
        label = "lyrics-line-scale"
    )
    val targetAlpha = lyricsFocusAlpha(
        distance = distanceFromActive,
        focusMode = focusMode,
        compact = compact,
        synced = synced
    )
    val lineAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = LevyraMotion.spec(animationsEnabled, tween(durationMillis = LevyraMotion.Durations.Short, easing = LevyraMotion.Easings.Decelerate)),
        label = "lyrics-line-alpha"
    )
    val lineBlur by animateDpAsState(
        targetValue = lyricsFocusBlurDp(distanceFromActive, focusMode, synced, blurEnabled).dp,
        animationSpec = LevyraMotion.spec(animationsEnabled, tween(durationMillis = LevyraMotion.Durations.Short + 20, easing = LevyraMotion.Easings.Standard)),
        label = "lyrics-line-blur"
    )
    val baseFontSizeSp = lyricBaseFontSizeSp(compact, cinema, isPrimaryActive) * roleScale * studioSettings.textSize.scale
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    val lineWidthDp = layoutWidthDp ?: configuration.screenWidthDp
    val resolvedFontSizeSp = remember(
        baseFontSizeSp,
        line.text,
        lineWidthDp,
        configuration.screenHeightDp,
        fontScale
    ) {
        adaptiveLyricFontSizeSp(
            baseSizeSp = baseFontSizeSp,
            characterCount = line.text.length,
            availableWidthDp = (lineWidthDp - LYRICS_HORIZONTAL_GUTTER_DP).toFloat(),
            availableHeightDp = configuration.screenHeightDp.toFloat(),
            fontScale = fontScale
        )
    }
    val resolvedFontSize = resolvedFontSizeSp.sp
    val lineHeight = LevyraTypeRhythm.lineHeight(resolvedFontSizeSp)
    val spotifyLyrics = studioSettings.spotifyLyricsApplies()
    val inactiveColor = lyricInactiveColor(spotifyLyrics, isActive, synced, compact, studioSettings.inactiveOpacity)
    val activeTextColor = lyricActiveTextColor(spotifyLyrics, studioSettings, accentEnd)
    val animatedMainColor by animateColorAsState(
        targetValue = if (isActive || isPrimaryActive) activeTextColor else inactiveColor,
        animationSpec = LevyraMotion.spec(animationsEnabled, tween(durationMillis = LevyraMotion.Durations.Short, easing = LevyraMotion.Easings.Decelerate)),
        label = "lyrics-main-text-color"
    )
    val animatedInactiveColor by animateColorAsState(
        targetValue = inactiveColor,
        animationSpec = LevyraMotion.spec(animationsEnabled, tween(durationMillis = LevyraMotion.Durations.Short, easing = LevyraMotion.Easings.Decelerate)),
        label = "lyrics-inactive-color"
    )
    val animatedSectionColor by animateColorAsState(
        targetValue = (if (spotifyLyrics) Color.White else accentEnd).copy(alpha = if (isPrimaryActive) 0.92f else 0.56f),
        animationSpec = LevyraMotion.spec(animationsEnabled, tween(durationMillis = LevyraMotion.Durations.Short, easing = LevyraMotion.Easings.Decelerate)),
        label = "lyrics-section-color"
    )
    val animatedRomanizationColor by animateColorAsState(
        targetValue = if (isActive) Color.White.copy(alpha = 0.76f) else Color.White.copy(alpha = 0.40f),
        animationSpec = LevyraMotion.spec(animationsEnabled, tween(durationMillis = LevyraMotion.Durations.Short, easing = LevyraMotion.Easings.Decelerate)),
        label = "lyrics-romanization-color"
    )
    val animatedTranslationColor by animateColorAsState(
        targetValue = if (isActive) accentEnd.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.44f),
        animationSpec = LevyraMotion.spec(animationsEnabled, tween(durationMillis = LevyraMotion.Durations.Short, easing = LevyraMotion.Easings.Decelerate)),
        label = "lyrics-translation-color"
    )
    val horizontalPadding = lyricRolePadding(line.role, compact)
    val resolvedRomanization = line.romanized.ifBlank {
        line.words.joinToString("") { word -> word.romanized }.trim()
    }
    val studioFontFamily = remember(studioSettings.fontPreset, spotifyLyrics) {
        if (spotifyLyrics) SpotifyFont else levyraFontFamilyFor(studioSettings.fontPreset)
    }
    val resolvedFontWeight = lyricFontWeight(spotifyLyrics, isPrimaryActive, studioSettings.fontWeight)

    val glowShadow = lyricGlowShadow(studioSettings.glowEffect, activeTextColor)
    val activeWordColor = activeTextColor
    val completedWordColor = activeTextColor
    val pendingWordColor = if (spotifyLyrics) SpotifyLyricsPending else Color.White.copy(alpha = if (compact) 0.50f else 0.58f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (selected || selectionMode) {
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (selected) accentEnd.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.035f))
                } else {
                    Modifier
                }
            )
            .padding(horizontal = horizontalPadding, vertical = if (compact) 2.dp else 0.dp)
            .padding(horizontal = if (selectionMode) 10.dp else 0.dp, vertical = if (selectionMode) 7.dp else 0.dp)
            .then(if (lineBlur > 0.dp) Modifier.blur(lineBlur, BlurredEdgeTreatment.Unbounded) else Modifier)
            .layout { measurable, constraints ->
                if (!constraints.hasBoundedWidth) {
                    val placeable = measurable.measure(constraints)
                    return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                }
                val contentWidth = lyricLineScaleSafeWidthPx(constraints.maxWidth, maxActiveScale)
                val placeable = measurable.measure(
                    constraints.copy(
                        minWidth = minOf(constraints.minWidth, contentWidth),
                        maxWidth = contentWidth
                    )
                )
                val width = maxOf(constraints.minWidth, placeable.width)
                layout(width, placeable.height) {
                    placeable.placeRelative(lyricLineOffsetX(line.role, studioSettings.alignment, width - placeable.width), 0)
                }
            }
            .graphicsLayer {
                scaleX = activeScale
                scaleY = activeScale
                alpha = lineAlpha
                transformOrigin = lyricLineTransformOrigin(line.role, studioSettings.alignment, lyricLayoutDirection == LayoutDirection.Rtl)
            }
            .drawWithContent {
                drawContent()
                if (selected) {
                    val indicator = 20.dp.toPx()
                    val inset = 8.dp.toPx()
                    val left = if (layoutDirection == LayoutDirection.Rtl) inset else size.width - indicator - inset
                    translate(left = left, top = inset) {
                        with(selectionIndicator) {
                            draw(Size(indicator, indicator), colorFilter = ColorFilter.tint(Color.White))
                        }
                    }
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (selectable) onLongClick else null,
                onLongClickLabel = if (selectable) strings.shareLyrics else null
            )
            .semantics {
                if (selectionMode && selectable) {
                    this.selected = selected
                    stateDescription = if (selected) strings.shareLyricsLineSelected else strings.shareLyricsLineNotSelected
                }
                customActions = lineAccessibilityActions
            },
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 4.dp)
    ) {
        if (!sectionLabel.isNullOrBlank()) {
            Text(
                text = sectionLabel.uppercase(sectionLocale),
                color = animatedSectionColor,
                fontSize = if (compact) 9.sp else 10.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(if (compact) 9.sp else 10.sp),
                fontWeight = FontWeight.Black,
                letterSpacing = 0.9.sp,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (instrumentalGap != null) {
            LyricsInstrumentalPulse(
                gap = instrumentalGap,
                positionProvider = positionProvider,
                accent = accentEnd,
                compact = compact,
                alignment = alignment
            )
        }
        if (line.words.isNotEmpty() && synced && studioSettings.progressiveKaraokeEnabled) {
            KaraokeWordTimedText(
                lineText = line.text,
                words = line.words,
                positionProvider = positionProvider,
                isActive = isActive,
                fontSize = resolvedFontSize,
                lineHeight = lineHeight,
                textAlign = textAlign,
                inactiveColor = animatedInactiveColor,
                completedColor = completedWordColor,
                activeColor = activeWordColor,
                pendingColor = pendingWordColor,
                fontWeight = resolvedFontWeight,
                fontFamily = studioFontFamily,
                shadow = if (isPrimaryActive) glowShadow else null
            )
        } else {
            Text(
                text = if (line.isInstrumental) "♪" else line.text,
                color = animatedMainColor,
                fontFamily = studioFontFamily,
                fontSize = resolvedFontSize,
                lineHeight = lineHeight,
                fontWeight = resolvedFontWeight,
                textAlign = textAlign,
                style = TextStyle(shadow = if (isPrimaryActive) glowShadow else null),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (showRomanization && resolvedRomanization.isNotBlank()) {
            Text(
                text = resolvedRomanization,
                color = animatedRomanizationColor,
                fontSize = lyricSecondarySize(compact, cinema && isPrimaryActive, 11.sp, 15.sp, 13.sp),
                lineHeight = if (compact) 15.sp else 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (showTranslation && line.translated.isNotBlank()) {
            Text(
                text = line.translated,
                color = animatedTranslationColor,
                fontSize = lyricSecondarySize(compact, cinema && isPrimaryActive, 12.sp, 16.sp, 14.sp),
                lineHeight = if (compact) 16.sp else 20.sp,
                fontWeight = FontWeight.Medium,
                textAlign = textAlign,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun lyricLineAlignment(role: LyricVocalRole, alignment: LyricsAlignment): Alignment.Horizontal = when (role) {
    LyricVocalRole.DUET_RIGHT -> Alignment.End
    LyricVocalRole.BACKGROUND -> Alignment.CenterHorizontally
    else -> when (alignment) {
        LyricsAlignment.START -> Alignment.Start
        LyricsAlignment.CENTER -> Alignment.CenterHorizontally
        LyricsAlignment.END -> Alignment.End
    }
}

private fun lyricLineTextAlign(role: LyricVocalRole, alignment: LyricsAlignment): TextAlign = when (role) {
    LyricVocalRole.DUET_RIGHT -> TextAlign.End
    LyricVocalRole.BACKGROUND -> TextAlign.Center
    else -> alignment.textAlign
}

private fun lyricRoleScale(role: LyricVocalRole, compact: Boolean): Float = when (role) {
    LyricVocalRole.BACKGROUND -> if (compact) 0.86f else 0.80f
    LyricVocalRole.DUET_LEFT, LyricVocalRole.DUET_RIGHT -> 0.92f
    LyricVocalRole.MAIN -> 1f
}

private fun lyricBaseFontSizeSp(compact: Boolean, cinema: Boolean, primary: Boolean): Float = when {
    compact && primary -> 22f
    compact -> 17f
    cinema && primary -> 29f
    cinema -> 24f
    primary -> 26f
    else -> 21f
}

private fun lyricInactiveColor(spotify: Boolean, active: Boolean, synced: Boolean, compact: Boolean, inactiveOpacity: Float): Color = when {
    spotify && !active && synced -> SpotifyLyricsPending
    active -> Color.White.copy(alpha = 0.76f)
    synced -> Color.White.copy(alpha = if (compact) (inactiveOpacity + 0.10f).coerceAtMost(0.90f) else inactiveOpacity)
    else -> Color.White.copy(alpha = 0.88f)
}

private fun lyricActiveTextColor(spotify: Boolean, settings: LevyraLyricsStudioSettings, accentEnd: Color): Color =
    if (spotify) Color.White else when (settings.activeColorMode) {
        LyricsActiveColorMode.WHITE -> Color.White
        LyricsActiveColorMode.AUTOMATIC -> accentEnd
        LyricsActiveColorMode.CUSTOM -> Color(settings.customAccentColor)
    }

private fun lyricRolePadding(role: LyricVocalRole, compact: Boolean): Dp = when (role) {
    LyricVocalRole.BACKGROUND -> if (compact) 16.dp else 24.dp
    LyricVocalRole.DUET_LEFT, LyricVocalRole.DUET_RIGHT -> 10.dp
    LyricVocalRole.MAIN -> 0.dp
}

private fun lyricFontWeight(spotify: Boolean, primary: Boolean, preset: LyricsFontWeightPreset): FontWeight = when {
    spotify -> FontWeight.Bold
    primary -> preset.weight
    preset == LyricsFontWeightPreset.BLACK || preset == LyricsFontWeightPreset.EXTRA_BOLD -> FontWeight.Bold
    preset == LyricsFontWeightPreset.BOLD -> FontWeight.SemiBold
    else -> FontWeight.Medium
}

private fun lyricGlowShadow(effect: LyricsGlowEffect, color: Color): Shadow? = when (effect) {
    LyricsGlowEffect.OFF -> null
    LyricsGlowEffect.SUBTLE -> Shadow(color = color.copy(alpha = 0.55f), blurRadius = 14f, offset = Offset.Zero)
    LyricsGlowEffect.VIBRANT -> Shadow(color = color.copy(alpha = 0.85f), blurRadius = 26f, offset = Offset.Zero)
}

private fun lyricLineOffsetX(role: LyricVocalRole, alignment: LyricsAlignment, slack: Int): Int = when (role) {
    LyricVocalRole.DUET_RIGHT -> slack
    LyricVocalRole.BACKGROUND -> slack / 2
    else -> when (alignment) {
        LyricsAlignment.START -> 0
        LyricsAlignment.CENTER -> slack / 2
        LyricsAlignment.END -> slack
    }
}

private fun lyricLineTransformOrigin(role: LyricVocalRole, alignment: LyricsAlignment, rtl: Boolean): TransformOrigin {
    val end = TransformOrigin(if (rtl) 0f else 1f, 0.5f)
    return when (role) {
        LyricVocalRole.DUET_RIGHT -> end
        LyricVocalRole.BACKGROUND -> TransformOrigin.Center
        else -> when (alignment) {
            LyricsAlignment.START -> TransformOrigin(if (rtl) 1f else 0f, 0.5f)
            LyricsAlignment.CENTER -> TransformOrigin.Center
            LyricsAlignment.END -> end
        }
    }
}

private fun lyricSecondarySize(compact: Boolean, cinemaPrimary: Boolean, compactSize: TextUnit, cinemaSize: TextUnit, regular: TextUnit): TextUnit = when {
    compact -> compactSize
    cinemaPrimary -> cinemaSize
    else -> regular
}

@Composable
private fun LyricsInstrumentalPulse(
    gap: LyricsInstrumentalGap,
    positionProvider: () -> Long,
    accent: Color,
    compact: Boolean,
    alignment: Alignment.Horizontal
) {
    val dotSize = if (compact) 7.dp else 9.dp
    val spacing = if (compact) 5.dp else 7.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 4.dp else 8.dp)
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(spacing, alignment),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(LYRICS_INSTRUMENTAL_DOT_COUNT) { index ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .drawBehind {
                        val progress = lyricsInstrumentalProgress(positionProvider(), gap)
                        val intensity = lyricsInstrumentalDotIntensity(progress, index)
                        drawCircle(
                            color = accent,
                            radius = size.minDimension / 2f * (0.55f + 0.45f * intensity),
                            alpha = 0.30f + 0.60f * intensity
                        )
                    }
            )
        }
    }
}
