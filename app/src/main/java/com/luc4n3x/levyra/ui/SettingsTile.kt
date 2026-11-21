package com.luc4n3x.levyra.ui

import androidx.compose.runtime.MutableState
import kotlinx.coroutines.delay
import com.luc4n3x.levyra.ui.theme.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.components.LevyraUtilityStyleActive
import com.luc4n3x.levyra.ui.components.levyraExpressiveCorner
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraText
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsTile(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    enabled: Boolean = true,
    groupIndex: Int? = null,
    groupCount: Int? = null,
    color: Color = settingsTileColor(),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val slot = LocalSettingsGroupSlot.current
    val index = groupIndex ?: slot?.index ?: 0
    val count = groupCount ?: slot?.count ?: 1
    val first = index == 0
    val last = index == count - 1
    val shape = settingsTileShape(interactionSource, first, last)
    val highlight = remember { mutableStateOf(false) }
    val tileModifier = modifier.fillMaxWidth().then(if (SkinChrome.active) Modifier.padding(vertical = 1.dp) else Modifier)
        .then(if (LevyraIsAtelier) Modifier.border(1.dp, AtelierBrass.copy(alpha = 0.28f), shape) else Modifier)
        .then(settingsSearchPulse(highlight, shape))
    val tileContent: @Composable () -> Unit = {
        CompositionLocalProvider(LocalSettingsTileHighlight provides highlight, content = content)
    }
    when {
        checked != null && onCheckedChange != null -> Surface(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            shape = shape,
            color = color,
            interactionSource = interactionSource,
            modifier = tileModifier,
            content = tileContent
        )
        onClick != null -> Surface(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            color = color,
            interactionSource = interactionSource,
            modifier = tileModifier,
            content = tileContent
        )
        else -> Surface(shape = shape, color = color, modifier = tileModifier, content = tileContent)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsTileIcon(icon: ImageVector, accent: Color, shape: Shape = MaterialShapes.Cookie4Sided.toShape()) {
    if (LevyraIsAtelier) return AtelierSettingsKey(icon)
    if (LevyraIsDefaultSkin) return DefaultSettingsAccentIcon(icon, accent)
    val expressive = LevyraUtilityStyleActive && !SkinChrome.active
    Box(
        modifier = Modifier
            .size(if (LevyraIsAppleMusic) 30.dp else 44.dp)
            .clip(if (SkinChrome.active) SkinChrome.iconShape else shape)
            .background(
                when {
                    SkinChrome.active -> SkinChrome.iconBackground
                    expressive -> settingsCategoryTone(accent).container
                    else -> LevyraText.copy(alpha = 0.08f)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            null,
            tint = when {
                SkinChrome.active -> SkinChrome.iconTint
                expressive -> settingsCategoryTone(accent).content
                else -> LevyraText.copy(alpha = 0.88f)
            },
            modifier = Modifier.size(if (LevyraIsAppleMusic) 19.dp else 22.dp)
        )
    }
}

@Composable
internal fun SettingsTileText(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    maxTitleLines: Int = Int.MAX_VALUE,
    maxSubtitleLines: Int = Int.MAX_VALUE
) {
    val searchFocus = LocalSettingsSearchFocus.current?.takeIf { it.title == title }
    val tileHighlight = LocalSettingsTileHighlight.current
    val relocation = remember { BringIntoViewRequester() }
    val measuredHeight = remember { intArrayOf(0) }
    val breathingRoom = with(LocalDensity.current) { SettingsSearchFocusBreathingRoom.toPx() }
    if (searchFocus != null) {
        LaunchedEffect(searchFocus) {
            searchFocus.reached = true
            relocation.bringIntoView(Rect(0f, 0f, 1f, measuredHeight[0] + breathingRoom))
            tileHighlight?.value = true
            searchFocus.onReached()
        }
    }
    Column(
        modifier = modifier.then(
            if (searchFocus != null) {
                Modifier
                    .bringIntoViewRequester(relocation)
                    .onSizeChanged { measuredHeight[0] = it.height }
            } else Modifier
        ),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        SettingsTileTitle(title, maxTitleLines)
        if (subtitle.isNotBlank()) {
            Text(
                subtitle,
                color = LevyraMuted,
                fontFamily = if (LevyraIsAtelier) AtelierSerif else null,
                fontStyle = if (LevyraIsAtelier) FontStyle.Italic else null,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = maxSubtitleLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun settingsTileColor(): Color = when {
    LevyraIsDefaultSkin -> defaultSettingsCardColor()
    LevyraIsSpotify || LevyraIsYouTubeMusic -> Color.Transparent
    LevyraIsAppleMusic -> AppleMusicDesign.groupedBackground
    LevyraIsAtelier -> AtelierSettingsPanelFill
    else -> LevyraAdaptiveCard
}

@Composable
private fun settingsSearchPulse(highlight: MutableState<Boolean>, shape: Shape): Modifier {
    val pulse = remember { Animatable(0f) }
    val animated = LocalAnimationsEnabled.current
    LaunchedEffect(highlight.value) {
        if (!highlight.value) return@LaunchedEffect
        if (animated) {
            repeat(2) {
                pulse.animateTo(1f, tween(durationMillis = 260))
                pulse.animateTo(0f, tween(durationMillis = 420))
            }
        } else {
            pulse.snapTo(1f)
            delay(SETTINGS_SEARCH_STATIC_HIGHLIGHT_MS)
            pulse.snapTo(0f)
        }
        highlight.value = false
    }
    if (!highlight.value) return Modifier
    val pulseColor = LocalSettingsAccent.current ?: MaterialTheme.colorScheme.primary
    return Modifier.border(2.dp, pulseColor.copy(alpha = pulse.value), shape)
}

@Composable
private fun settingsTileShape(interactionSource: MutableInteractionSource, first: Boolean, last: Boolean): Shape {
    val outer = if (SkinChrome.active) SkinChrome.outerCorner else levyraExpressiveCorner(interactionSource, rest = 24.dp, pressed = 14.dp, label = "settingsTileOuter")
    val inner = if (SkinChrome.active) SkinChrome.innerCorner else levyraExpressiveCorner(interactionSource, rest = 6.dp, pressed = 14.dp, label = "settingsTileInner")
    return RoundedCornerShape(
        topStart = if (first) outer else inner,
        topEnd = if (first) outer else inner,
        bottomEnd = if (last) outer else inner,
        bottomStart = if (last) outer else inner
    )
}

private val SettingsSearchFocusBreathingRoom = 160.dp

private const val SETTINGS_SEARCH_STATIC_HIGHLIGHT_MS = 1_400L
