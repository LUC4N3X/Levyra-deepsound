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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.painter.Painter
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
        lyricShareActions(strings.shareLyrics, selectable) { currentLongClick() }
    }
    val sectionLocale = remember(strings.code) { Locale.forLanguageTag(strings.code.replace('_', '-')) }
    val lyricLayoutDirection = LocalLayoutDirection.current
    val alignment = lyricLineAlignment(line.role, studioSettings.alignment)
    val textAlign = lyricLineTextAlign(line.role, studioSettings.alignment)
    val roleScale = lyricRoleScale(line.role, compact)
    val maxActiveScale = lyricMaxActiveScale(compact)
    val activeScale by animateFloatAsState(
        targetValue = lyricActiveScale(isPrimaryActive, compact, studioSettings.inactiveScale),
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
    val spotifyLyrics = studioSettings.spotifyLyricsApplies()
    val inactiveColor = lyricInactiveColor(spotifyLyrics, isActive, synced, compact, studioSettings.inactiveOpacity)
    val activeTextColor = lyricActiveTextColor(spotifyLyrics, studioSettings, accentEnd)
    val colorSpec = LevyraMotion.spec(animationsEnabled, tween<Color>(durationMillis = LevyraMotion.Durations.Short, easing = LevyraMotion.Easings.Decelerate))
    val animatedMainColor by animateColorAsState(
        targetValue = lyricMainColor(isActive, isPrimaryActive, activeTextColor, inactiveColor),
        animationSpec = colorSpec,
        label = "lyrics-main-text-color"
    )
    val animatedInactiveColor by animateColorAsState(
        targetValue = inactiveColor,
        animationSpec = colorSpec,
        label = "lyrics-inactive-color"
    )
    val animatedSectionColor by animateColorAsState(
        targetValue = lyricSectionColor(spotifyLyrics, accentEnd, isPrimaryActive),
        animationSpec = colorSpec,
        label = "lyrics-section-color"
    )
    val animatedRomanizationColor by animateColorAsState(
        targetValue = lyricRomanizationColor(isActive),
        animationSpec = colorSpec,
        label = "lyrics-romanization-color"
    )
    val animatedTranslationColor by animateColorAsState(
        targetValue = lyricTranslationColor(isActive, accentEnd),
        animationSpec = colorSpec,
        label = "lyrics-translation-color"
    )
    val resolvedRomanization = line.romanized.ifBlank {
        line.words.joinToString("") { word -> word.romanized }.trim()
    }
    val studioFontFamily = remember(studioSettings.fontPreset, spotifyLyrics) { lyricFontFamily(spotifyLyrics, studioSettings) }
    val typography = LyricLineTypography(
        fontSize = resolvedFontSizeSp.sp,
        lineHeight = LevyraTypeRhythm.lineHeight(resolvedFontSizeSp),
        fontWeight = lyricFontWeight(spotifyLyrics, isPrimaryActive, studioSettings.fontWeight),
        fontFamily = studioFontFamily,
        textAlign = textAlign,
        shadow = lyricGlowShadow(studioSettings.glowEffect, activeTextColor).takeIf { isPrimaryActive }
    )
    val cinemaPrimary = cinema && isPrimaryActive

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lyricSelectionSurface(selected, selectionMode, accentEnd)
            .lyricLinePadding(lyricRolePadding(line.role, compact), compact, selectionMode)
            .lyricLineBlur(lineBlur)
            .lyricScaleSafeLayout(line.role, studioSettings.alignment, maxActiveScale)
            .graphicsLayer {
                scaleX = activeScale
                scaleY = activeScale
                alpha = lineAlpha
                transformOrigin = lyricLineTransformOrigin(line.role, studioSettings.alignment, lyricLayoutDirection == LayoutDirection.Rtl)
            }
            .lyricSelectionIndicator(selected, selectionIndicator)
            .lyricLineInteraction(
                LyricLineSelection(selectionMode, selectable, selected),
                LyricLineLabels(strings.shareLyrics, strings.shareLyricsLineSelected, strings.shareLyricsLineNotSelected),
                lineAccessibilityActions,
                onClick,
                onLongClick
            ),
        horizontalAlignment = alignment,
        verticalArrangement = Arrangement.spacedBy(lyricLineGap(compact))
    ) {
        LyricLineSectionLabel(sectionLabel, sectionLocale, animatedSectionColor, compact, textAlign)
        if (instrumentalGap != null) {
            LyricsInstrumentalPulse(
                gap = instrumentalGap,
                positionProvider = positionProvider,
                accent = accentEnd,
                compact = compact,
                alignment = alignment
            )
        }
        LyricLineMainText(
            line = line,
            wordTimed = lyricUsesWordTiming(line, synced, studioSettings.progressiveKaraokeEnabled),
            positionProvider = positionProvider,
            isActive = isActive,
            typography = typography,
            colors = LyricLineColors(
                main = animatedMainColor,
                inactive = animatedInactiveColor,
                active = activeTextColor,
                pending = lyricPendingWordColor(spotifyLyrics, compact)
            )
        )
        LyricLineSecondaryText(
            text = resolvedRomanization.takeIf { showRomanization }.orEmpty(),
            color = animatedRomanizationColor,
            fontSize = lyricSecondarySize(compact, cinemaPrimary, 11.sp, 15.sp, 13.sp),
            lineHeight = lyricSecondarySize(compact, false, 15.sp, 18.sp, 18.sp),
            textAlign = textAlign
        )
        LyricLineSecondaryText(
            text = line.translated.takeIf { showTranslation }.orEmpty(),
            color = animatedTranslationColor,
            fontSize = lyricSecondarySize(compact, cinemaPrimary, 12.sp, 16.sp, 14.sp),
            lineHeight = lyricSecondarySize(compact, false, 16.sp, 20.sp, 20.sp),
            textAlign = textAlign
        )
    }
}

private class LyricLineTypography(
    val fontSize: TextUnit,
    val lineHeight: TextUnit,
    val fontWeight: FontWeight,
    val fontFamily: FontFamily,
    val textAlign: TextAlign,
    val shadow: Shadow?
)

private class LyricLineColors(val main: Color, val inactive: Color, val active: Color, val pending: Color)

private class LyricLineSelection(val selectionMode: Boolean, val selectable: Boolean, val selected: Boolean)

private class LyricLineLabels(val share: String, val selected: String, val notSelected: String)

@Composable
private fun LyricLineSectionLabel(label: String?, locale: Locale, color: Color, compact: Boolean, textAlign: TextAlign) {
    if (label.isNullOrBlank()) return
    val size = if (compact) 9.sp else 10.sp
    Text(
        text = label.uppercase(locale),
        color = color,
        fontSize = size,
        lineHeight = LevyraTypeRhythm.lineHeight(size),
        fontWeight = FontWeight.Black,
        letterSpacing = 0.9.sp,
        textAlign = textAlign,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LyricLineMainText(
    line: LyricLine,
    wordTimed: Boolean,
    positionProvider: () -> Long,
    isActive: Boolean,
    typography: LyricLineTypography,
    colors: LyricLineColors
) {
    if (wordTimed) {
        KaraokeWordTimedText(
            lineText = line.text,
            words = line.words,
            positionProvider = positionProvider,
            isActive = isActive,
            fontSize = typography.fontSize,
            lineHeight = typography.lineHeight,
            textAlign = typography.textAlign,
            inactiveColor = colors.inactive,
            completedColor = colors.active,
            activeColor = colors.active,
            pendingColor = colors.pending,
            fontWeight = typography.fontWeight,
            fontFamily = typography.fontFamily,
            shadow = typography.shadow
        )
    } else {
        Text(
            text = if (line.isInstrumental) "♪" else line.text,
            color = colors.main,
            fontFamily = typography.fontFamily,
            fontSize = typography.fontSize,
            lineHeight = typography.lineHeight,
            fontWeight = typography.fontWeight,
            textAlign = typography.textAlign,
            style = TextStyle(shadow = typography.shadow),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LyricLineSecondaryText(text: String, color: Color, fontSize: TextUnit, lineHeight: TextUnit, textAlign: TextAlign) {
    if (text.isBlank()) return
    Text(
        text = text,
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = FontWeight.Medium,
        textAlign = textAlign,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun Modifier.lyricSelectionSurface(selected: Boolean, selectionMode: Boolean, accent: Color): Modifier =
    if (selected || selectionMode) {
        clip(RoundedCornerShape(16.dp))
            .background(if (selected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.035f))
    } else {
        this
    }

private fun Modifier.lyricLinePadding(horizontal: Dp, compact: Boolean, selectionMode: Boolean): Modifier =
    padding(horizontal = horizontal, vertical = if (compact) 2.dp else 0.dp)
        .padding(horizontal = if (selectionMode) 10.dp else 0.dp, vertical = if (selectionMode) 7.dp else 0.dp)

private fun Modifier.lyricLineBlur(radius: Dp): Modifier =
    if (radius > 0.dp) blur(radius, BlurredEdgeTreatment.Unbounded) else this

private fun Modifier.lyricScaleSafeLayout(role: LyricVocalRole, alignment: LyricsAlignment, maxScale: Float): Modifier =
    layout { measurable, constraints ->
        if (!constraints.hasBoundedWidth) {
            val placeable = measurable.measure(constraints)
            return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
        }
        val contentWidth = lyricLineScaleSafeWidthPx(constraints.maxWidth, maxScale)
        val placeable = measurable.measure(
            constraints.copy(
                minWidth = minOf(constraints.minWidth, contentWidth),
                maxWidth = contentWidth
            )
        )
        val width = maxOf(constraints.minWidth, placeable.width)
        layout(width, placeable.height) {
            placeable.placeRelative(lyricLineOffsetX(role, alignment, width - placeable.width), 0)
        }
    }

private fun Modifier.lyricSelectionIndicator(selected: Boolean, indicator: Painter): Modifier =
    drawWithContent {
        drawContent()
        if (selected) {
            val indicatorSize = 20.dp.toPx()
            val inset = 8.dp.toPx()
            val left = if (layoutDirection == LayoutDirection.Rtl) inset else size.width - indicatorSize - inset
            translate(left = left, top = inset) {
                with(indicator) {
                    draw(Size(indicatorSize, indicatorSize), colorFilter = ColorFilter.tint(Color.White))
                }
            }
        }
    }

private fun Modifier.lyricLineInteraction(
    selection: LyricLineSelection,
    labels: LyricLineLabels,
    actions: List<CustomAccessibilityAction>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
): Modifier = combinedClickable(
    onClick = onClick,
    onLongClick = onLongClick.takeIf { selection.selectable },
    onLongClickLabel = labels.share.takeIf { selection.selectable }
).semantics {
    if (selection.selectionMode && selection.selectable) {
        this.selected = selection.selected
        stateDescription = if (selection.selected) labels.selected else labels.notSelected
    }
    customActions = actions
}

private fun lyricShareActions(label: String, selectable: Boolean, onShare: () -> Unit): List<CustomAccessibilityAction> =
    if (selectable) {
        listOf(
            CustomAccessibilityAction(label) {
                onShare()
                true
            }
        )
    } else {
        emptyList()
    }

private fun lyricMaxActiveScale(compact: Boolean): Float =
    if (compact) LYRICS_COMPACT_ACTIVE_LINE_SCALE else LYRICS_ACTIVE_LINE_SCALE

private fun lyricActiveScale(primary: Boolean, compact: Boolean, inactiveScale: Float): Float =
    if (primary) lyricMaxActiveScale(compact) else inactiveScale

private fun lyricMainColor(active: Boolean, primary: Boolean, activeColor: Color, inactiveColor: Color): Color =
    if (active || primary) activeColor else inactiveColor

private fun lyricSectionColor(spotify: Boolean, accent: Color, primary: Boolean): Color =
    (if (spotify) Color.White else accent).copy(alpha = if (primary) 0.92f else 0.56f)

private fun lyricRomanizationColor(active: Boolean): Color =
    Color.White.copy(alpha = if (active) 0.76f else 0.40f)

private fun lyricTranslationColor(active: Boolean, accent: Color): Color =
    if (active) accent.copy(alpha = 0.88f) else Color.White.copy(alpha = 0.44f)

private fun lyricFontFamily(spotify: Boolean, settings: LevyraLyricsStudioSettings): FontFamily =
    if (spotify) SpotifyFont else levyraFontFamilyFor(settings.fontPreset)

private fun lyricPendingWordColor(spotify: Boolean, compact: Boolean): Color =
    if (spotify) SpotifyLyricsPending else Color.White.copy(alpha = if (compact) 0.50f else 0.58f)

private fun lyricLineGap(compact: Boolean): Dp = if (compact) 2.dp else 4.dp

private fun lyricUsesWordTiming(line: LyricLine, synced: Boolean, progressive: Boolean): Boolean =
    line.words.isNotEmpty() && synced && progressive

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
