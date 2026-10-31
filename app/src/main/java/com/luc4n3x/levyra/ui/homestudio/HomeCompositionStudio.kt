package com.luc4n3x.levyra.ui.homestudio

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import com.luc4n3x.levyra.domain.HomeBlock
import com.luc4n3x.levyra.domain.HomeCompositionMode
import com.luc4n3x.levyra.domain.HomeStyle
import com.luc4n3x.levyra.domain.LevyraHomeCatalog
import com.luc4n3x.levyra.domain.LevyraSkin
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.components.LevyraMorphShape
import com.luc4n3x.levyra.ui.i18n.HomeCompositionCopy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.modeBody
import com.luc4n3x.levyra.ui.i18n.modeChanges
import com.luc4n3x.levyra.ui.i18n.modeName
import com.luc4n3x.levyra.ui.theme.AppleMusicDesign
import com.luc4n3x.levyra.ui.theme.LevyraActivePalette
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraIsAppleMusic
import com.luc4n3x.levyra.ui.theme.LevyraIsAtelier
import com.luc4n3x.levyra.ui.theme.LevyraIsIvory
import com.luc4n3x.levyra.ui.theme.LevyraIsNocturne
import com.luc4n3x.levyra.ui.theme.LevyraIsPrism
import com.luc4n3x.levyra.ui.theme.LevyraIsQobuz
import com.luc4n3x.levyra.ui.theme.LevyraIsSpotify
import com.luc4n3x.levyra.ui.theme.LevyraIsTorque
import com.luc4n3x.levyra.ui.theme.LevyraIsYouTubeMusic
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.SkinChrome
import java.util.Locale

internal enum class HomePreviewIdiom { Expressive, Nocturne, Prism, Torque, Ivory, Spotify, AppleMusic, YouTubeMusic, Qobuz, Atelier }

@Immutable
internal class HomeStudioSkinTokens(
    val idiom: HomePreviewIdiom,
    val expressive: Boolean,
    val cardShape: Shape,
    val thumbShape: Shape,
    val controlShape: Shape,
    val chipShape: Shape,
    val previewRadius: Dp,
    val display: TextStyle,
    val accent: Color,
    val onAccent: Color,
    val card: Color,
    val cardLifted: Color,
    val line: Color,
    private val titlesUppercase: Boolean,
    private val phoneRestCorner: Dp,
    private val phoneSelectedCorner: Dp,
    private val phoneChamfered: Boolean
) {
    fun phoneShape(progress: Float): Shape {
        val corner = lerp(phoneRestCorner, phoneSelectedCorner, progress)
        return if (phoneChamfered) CutCornerShape(topEnd = corner, bottomStart = corner) else RoundedCornerShape(corner)
    }

    fun title(text: String, locale: Locale): String = if (titlesUppercase) text.uppercase(locale) else text

    companion object {
        @Composable
        fun current(): HomeStudioSkinTokens {
            val scheme = MaterialTheme.colorScheme
            val idiom = currentIdiom()
            val expressive = idiom == HomePreviewIdiom.Expressive
            val outer = if (expressive) 28.dp else SkinChrome.outerCorner
            val accent = when (idiom) {
                HomePreviewIdiom.Expressive -> scheme.primary
                HomePreviewIdiom.Torque, HomePreviewIdiom.Ivory -> LevyraActivePalette.blue
                else -> LevyraCyan
            }
            return HomeStudioSkinTokens(
                idiom = idiom,
                expressive = expressive,
                cardShape = RoundedCornerShape(outer),
                thumbShape = RoundedCornerShape(
                    when {
                        expressive -> 16.dp
                        LevyraIsAppleMusic -> AppleMusicDesign.cornerM
                        else -> SkinChrome.innerCorner
                    }
                ),
                controlShape = if (expressive) CircleShape else SkinChrome.fieldShape,
                chipShape = if (expressive) CircleShape else SkinChrome.chipShape,
                previewRadius = previewRadiusFor(idiom),
                display = displayStyleFor(idiom),
                accent = accent,
                onAccent = if (accent.luminance() > 0.5f) Color.Black else Color.White,
                card = if (expressive) scheme.surfaceContainerHigh else LevyraPanel,
                cardLifted = if (expressive) scheme.secondaryContainer else LevyraPanelSoft,
                line = if (expressive) scheme.outlineVariant else LevyraActivePalette.outline,
                titlesUppercase = idiom == HomePreviewIdiom.Torque,
                phoneRestCorner = if (expressive) 16.dp else if (LevyraIsTorque) 0.dp else (outer * 0.7f).coerceAtMost(16.dp),
                phoneSelectedCorner = if (expressive) 30.dp else if (LevyraIsTorque) 14.dp else (outer + 4.dp).coerceAtMost(28.dp),
                phoneChamfered = LevyraIsTorque
            )
        }

        private fun currentIdiom(): HomePreviewIdiom = when {
            LevyraIsNocturne -> HomePreviewIdiom.Nocturne
            LevyraIsTorque -> HomePreviewIdiom.Torque
            LevyraIsIvory -> HomePreviewIdiom.Ivory
            LevyraIsSpotify -> HomePreviewIdiom.Spotify
            LevyraIsAppleMusic -> HomePreviewIdiom.AppleMusic
            LevyraIsYouTubeMusic -> HomePreviewIdiom.YouTubeMusic
            LevyraIsQobuz -> HomePreviewIdiom.Qobuz
            LevyraIsAtelier -> HomePreviewIdiom.Atelier
            LevyraIsPrism -> HomePreviewIdiom.Prism
            else -> HomePreviewIdiom.Expressive
        }

        private fun previewRadiusFor(idiom: HomePreviewIdiom): Dp = when (idiom) {
            HomePreviewIdiom.Expressive -> 8.dp
            HomePreviewIdiom.Ivory -> 9.dp
            HomePreviewIdiom.Prism -> 5.dp
            HomePreviewIdiom.AppleMusic -> 4.dp
            HomePreviewIdiom.Nocturne, HomePreviewIdiom.Spotify, HomePreviewIdiom.Qobuz -> 2.dp
            HomePreviewIdiom.YouTubeMusic, HomePreviewIdiom.Atelier -> 1.dp
            HomePreviewIdiom.Torque -> 0.dp
        }

        private fun displayStyleFor(idiom: HomePreviewIdiom): TextStyle = when (idiom) {
            HomePreviewIdiom.Nocturne -> TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium)
            HomePreviewIdiom.Torque -> TextStyle(fontWeight = FontWeight.Black, fontStyle = FontStyle.Italic, letterSpacing = 0.4.sp)
            HomePreviewIdiom.Expressive, HomePreviewIdiom.Prism -> TextStyle(fontWeight = FontWeight.Black)
            HomePreviewIdiom.Ivory -> TextStyle(fontWeight = FontWeight.ExtraBold)
            HomePreviewIdiom.Spotify, HomePreviewIdiom.AppleMusic, HomePreviewIdiom.YouTubeMusic -> TextStyle(fontWeight = FontWeight.Bold)
            HomePreviewIdiom.Qobuz, HomePreviewIdiom.Atelier -> TextStyle(fontWeight = FontWeight.SemiBold)
        }
    }
}

internal enum class HomeRestoreAction { BackToOriginal, ResetOrder }

@Composable
internal fun rememberHomeStudioLocale(): Locale {
    val code = LocalLevyraStrings.current.code
    return remember(code) { Locale.forLanguageTag(code) }
}

private const val PhoneAspect = 9f / 17f
private const val PhoneRestScale = 0.93f
private val PhoneLift = 6.dp
private val PhoneShadow = 14.dp
private val RowPhoneMinimum = 96.dp
private val RowPhoneMaximum = 220.dp
private val PagerPhoneWidth = 132.dp
private const val RowFontScaleLimit = 1.3f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val StageHaloMorph: Morph by lazy {
    Morph(MaterialShapes.Circle.normalized(), MaterialShapes.Cookie12Sided.normalized())
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val StageBadgeMorph: Morph by lazy {
    Morph(MaterialShapes.Circle.normalized(), MaterialShapes.Cookie9Sided.normalized())
}

@Composable
internal fun HomeCompositionStudio(
    skin: LevyraSkin,
    mode: HomeCompositionMode,
    style: HomeStyle,
    customBlocks: List<HomeBlock>,
    highlighted: HomeBlock?,
    tokens: HomeStudioSkinTokens,
    tones: HomePreviewTones,
    copy: HomeCompositionCopy,
    onSelect: (HomeCompositionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val locale = rememberHomeStudioLocale()
    val levyraBlocks = remember(skin) { LevyraHomeCatalog.levyraBlocks(skin) }
    val customPreview = remember(customBlocks, levyraBlocks) {
        if (customBlocks.size >= CustomPreviewMinimum) customBlocks else (customBlocks + levyraBlocks).distinct()
    }
    val phone: @Composable (HomeCompositionMode, Modifier) -> Unit = { option, phoneModifier ->
        StagePhone(
            name = tokens.title(copy.modeName(option), locale),
            description = copy.modeBody(option),
            inUseLabel = tokens.title(copy.inUse, locale),
            selected = option == mode,
            tokens = tokens,
            onClick = { onSelect(option) },
            modifier = phoneModifier
        ) { insertProgress ->
            when (option) {
                HomeCompositionMode.Original -> HomeOriginalPhone(tokens.idiom, style, tones, Modifier.fillMaxSize())
                HomeCompositionMode.Custom -> HomeCustomPhone(customPreview, highlighted, tokens, tones, insertProgress, Modifier.fillMaxSize())
                HomeCompositionMode.Levyra -> HomeLevyraPhone(levyraBlocks, tokens, tones, Modifier.fillMaxSize())
            }
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = tokens.title(copy.stageHeading, locale),
            style = MaterialTheme.typography.titleLarge.merge(tokens.display),
            color = LevyraText,
            modifier = Modifier.semantics { heading() }
        )
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .clip(tokens.cardShape)
                .background(tokens.card)
        ) {
            val gap = 8.dp
            val inset = 6.dp
            val fitted = (maxWidth - inset * 2 - gap * 2) / 3
            val fontScale = LocalDensity.current.fontScale
            if (fitted >= RowPhoneMinimum && fontScale <= RowFontScaleLimit) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = inset, vertical = 18.dp)
                        .selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(gap, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.Top
                ) {
                    HomeCompositionMode.entries.forEach { option ->
                        key(option) {
                            phone(option, Modifier.width(min(fitted, RowPhoneMaximum)))
                        }
                    }
                }
            } else {
                val pager = rememberLazyListState(HomeCompositionMode.entries.indexOf(mode))
                val animated = LocalAnimationsEnabled.current
                LaunchedEffect(mode) {
                    val index = HomeCompositionMode.entries.indexOf(mode)
                    if (animated) pager.animateScrollToItem(index) else pager.scrollToItem(index)
                }
                LazyRow(
                    state = pager,
                    flingBehavior = rememberSnapFlingBehavior(pager),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                ) {
                    items(HomeCompositionMode.entries, key = { it.storageId }, contentType = { "home-stage-phone" }) { option ->
                        phone(option, Modifier.width(PagerPhoneWidth))
                    }
                }
            }
        }
        HomeStageSummary(mode, tokens, copy, locale)
    }
}

private const val CustomPreviewMinimum = 3

@Composable
private fun StagePhone(
    name: String,
    description: String,
    inUseLabel: String,
    selected: Boolean,
    tokens: HomeStudioSkinTokens,
    onClick: () -> Unit,
    modifier: Modifier,
    preview: @Composable (insertProgress: () -> Float) -> Unit
) {
    val animated = LocalAnimationsEnabled.current
    val lift by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (animated) {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
        } else {
            snap()
        },
        label = "home-stage-lift"
    )
    val settled = lift.coerceIn(0f, 1f)
    val phoneShape = tokens.phoneShape(settled)
    Column(
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(PhoneAspect)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center
        ) {
            if (tokens.expressive) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .graphicsLayer {
                            val halo = 0.8f + 0.34f * settled
                            scaleX = halo
                            scaleY = halo
                            alpha = settled
                        }
                        .background(tokens.accent.copy(alpha = 0.24f), LevyraMorphShape(StageHaloMorph, settled))
                )
            }
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val scale = PhoneRestScale + (1f - PhoneRestScale) * lift
                        scaleX = scale
                        scaleY = scale
                        translationY = -PhoneLift.toPx() * lift
                        shadowElevation = PhoneShadow.toPx() * settled
                        shape = phoneShape
                        clip = true
                    }
                    .border(if (selected) 2.dp else 1.dp, if (selected) tokens.accent else tokens.line, phoneShape)
            ) {
                preview { settled }
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(28.dp)
                    .graphicsLayer {
                        scaleX = lift
                        scaleY = lift
                        alpha = settled
                    }
                    .background(
                        tokens.accent,
                        if (tokens.expressive) LevyraMorphShape(StageBadgeMorph, settled) else tokens.chipShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = tokens.onAccent, modifier = Modifier.size(16.dp))
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium.merge(tokens.display).copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                hyphens = Hyphens.Auto
            ),
            color = if (selected) LevyraText else LevyraMuted,
            textAlign = TextAlign.Center
        )
        Text(
            text = inUseLabel,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.onAccent,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .graphicsLayer { alpha = if (selected) 1f else 0f }
                .then(if (selected) Modifier else Modifier.clearAndSetSemantics { })
                .background(tokens.accent, tokens.chipShape)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun HomeStageSummary(
    mode: HomeCompositionMode,
    tokens: HomeStudioSkinTokens,
    copy: HomeCompositionCopy,
    locale: Locale
) {
    val animated = LocalAnimationsEnabled.current
    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            if (animated) {
                (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 8 }) togetherWith fadeOut(tween(110))
            } else {
                EnterTransition.None togetherWith ExitTransition.None
            }
        },
        label = "home-stage-summary",
        modifier = Modifier.fillMaxWidth()
    ) { shown ->
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = tokens.title(copy.modeName(shown), locale),
                style = MaterialTheme.typography.titleMedium.merge(tokens.display),
                color = LevyraText
            )
            Text(text = copy.modeBody(shown), style = MaterialTheme.typography.bodyMedium, color = LevyraMuted)
            HomeStageFact(copy.changesLabel, copy.modeChanges(shown))
            HomeStageFact(copy.staysLabel, copy.staysText)
        }
    }
}

@Composable
private fun HomeStageFact(label: String, value: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = LevyraText, fontWeight = FontWeight.SemiBold)) {
                append(label)
                append(": ")
            }
            append(value)
        },
        style = MaterialTheme.typography.bodySmall,
        color = LevyraMuted
    )
}

@Composable
internal fun HomeBackToOriginalAction(
    visible: Boolean,
    tokens: HomeStudioSkinTokens,
    copy: HomeCompositionCopy,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animated = LocalAnimationsEnabled.current
    val locale = rememberHomeStudioLocale()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (animated) fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 2 } else EnterTransition.None,
        exit = if (animated) fadeOut(tween(120)) + slideOutVertically(tween(160)) { it / 2 } else ExitTransition.None,
        label = "home-back-to-original"
    ) {
        Button(
            onClick = onClick,
            shape = tokens.controlShape,
            colors = ButtonDefaults.buttonColors(containerColor = tokens.accent, contentColor = tokens.onAccent),
            modifier = Modifier.heightIn(min = 48.dp)
        ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(tokens.title(copy.backToOriginal, locale))
        }
    }
}

@Composable
internal fun HomeRestoreDialog(
    action: HomeRestoreAction,
    skinName: String,
    tokens: HomeStudioSkinTokens,
    copy: HomeCompositionCopy,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (action) {
                    HomeRestoreAction.BackToOriginal -> copy.backToOriginal
                    HomeRestoreAction.ResetOrder -> copy.resetOrder
                }
            )
        },
        text = { Text(copy.restoreBody.replace("{skin}", skinName)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(copy.restoreConfirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel) } },
        shape = tokens.cardShape,
        containerColor = tokens.card,
        titleContentColor = LevyraText,
        textContentColor = LevyraMuted
    )
}
