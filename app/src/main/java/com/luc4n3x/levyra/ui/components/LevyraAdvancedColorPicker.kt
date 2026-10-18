package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.i18n.ColorPickerCopy
import com.luc4n3x.levyra.ui.i18n.ColorPickerText
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.colorPickerCopy
import com.luc4n3x.levyra.ui.playerAccentsFromSeed
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val HexAllowedChars = "0123456789abcdefABCDEF"
private val PickerDialogShape = RoundedCornerShape(32.dp)
private val PickerStageShape = RoundedCornerShape(24.dp)
private val PaletteFieldShape = RoundedCornerShape(22.dp)
private val HueBarShape = RoundedCornerShape(percent = 50)

private val HueSpectrumColors = listOf(
    Color(0xFFFF0000),
    Color(0xFFFFFF00),
    Color(0xFF00FF00),
    Color(0xFF00FFFF),
    Color(0xFF0000FF),
    Color(0xFFFF00FF),
    Color(0xFFFF0000)
)

@Immutable
internal data class LevyraHsvColor(
    val hue: Float,
    val saturation: Float,
    val value: Float
) {
    fun normalized(): LevyraHsvColor {
        val safeHue = if (hue.isFinite()) {
            val wrapped = hue.mod(360f)
            if (hue >= 360f && wrapped == 0f) 360f else wrapped
        } else {
            0f
        }
        val safeSaturation = if (saturation.isFinite()) saturation.coerceIn(0f, 1f) else 0f
        val safeValue = if (value.isFinite()) value.coerceIn(0f, 1f) else 0f
        return LevyraHsvColor(safeHue, safeSaturation, safeValue)
    }

    fun withPalettePoint(normalizedX: Float, normalizedY: Float): LevyraHsvColor = copy(
        saturation = normalizedX.coerceIn(0f, 1f),
        value = (1f - normalizedY.coerceIn(0f, 1f)).coerceIn(0f, 1f)
    ).normalized()

    fun withHueFraction(normalizedY: Float): LevyraHsvColor = copy(
        hue = (normalizedY.coerceIn(0f, 1f) * 360f).coerceIn(0f, 360f)
    ).normalized()

    fun toArgb(): Int {
        val clean = normalized()
        val h = clean.hue.mod(360f)
        val s = clean.saturation
        val v = clean.value
        val chroma = v * s
        val secondary = chroma * (1f - abs((h / 60f).mod(2f) - 1f))
        val match = v - chroma
        val (rPrime, gPrime, bPrime) = when {
            h < 60f -> Triple(chroma, secondary, 0f)
            h < 120f -> Triple(secondary, chroma, 0f)
            h < 180f -> Triple(0f, chroma, secondary)
            h < 240f -> Triple(0f, secondary, chroma)
            h < 300f -> Triple(secondary, 0f, chroma)
            else -> Triple(chroma, 0f, secondary)
        }
        val r = ((rPrime + match) * 255f).roundToInt().coerceIn(0, 255)
        val g = ((gPrime + match) * 255f).roundToInt().coerceIn(0, 255)
        val b = ((bPrime + match) * 255f).roundToInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun toColor(): Color = Color(toArgb())
}

internal fun argbToHsv(argb: Int, preserveHue: Float = 0f): LevyraHsvColor {
    val r = ((argb ushr 16) and 0xFF) / 255f
    val g = ((argb ushr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val fallbackHue = if (preserveHue.isFinite()) preserveHue.coerceIn(0f, 360f) else 0f
    val hue = when {
        delta <= 1e-5f -> fallbackHue
        max == r -> (60f * ((g - b) / delta)).mod(360f)
        max == g -> (60f * ((b - r) / delta + 2f)).mod(360f)
        else -> (60f * ((r - g) / delta + 4f)).mod(360f)
    }
    val saturation = if (max <= 1e-5f) 0f else (delta / max).coerceIn(0f, 1f)
    return LevyraHsvColor(
        hue = hue,
        saturation = saturation,
        value = max.coerceIn(0f, 1f)
    )
}

internal fun sanitizeHexInput(raw: String): String =
    raw.trim().removePrefix("#").filter { it in HexAllowedChars }.take(6).uppercase(Locale.ROOT)

internal fun parseLevyraHexColor(raw: String): Int? {
    val digits = raw.trim().removePrefix("#")
    if (digits.length != 6 || digits.any { it !in HexAllowedChars }) return null
    return (0xFF000000 or digits.toLong(16)).toInt()
}

internal fun formatLevyraHexColor(color: Int): String =
    String.format(Locale.ROOT, "%06X", color and 0xFFFFFF)

internal fun rgbChannelsOf(argb: Int): Triple<Int, Int, Int> = Triple(
    (argb ushr 16) and 0xFF,
    (argb ushr 8) and 0xFF,
    argb and 0xFF
)

@Composable
internal fun LevyraColorInputAndPickerRow(
    color: Int,
    hexText: String,
    hexLabel: String,
    onHexTextChange: (String) -> Unit,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val pickerCopy = LocalLevyraStrings.current.colorPickerCopy
    val parsed = parseLevyraHexColor(hexText)
    val activeArgb = parsed ?: color
    val displayColor = Color(activeArgb)
    val onDisplayColor = if (displayColor.luminance() > 0.52f) Color.Black else Color.White

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(displayColor)
                    .border(
                        width = 1.5.dp,
                        color = scheme.outlineVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .levyraPressable(
                        onClick = onOpenPicker,
                        onClickLabel = pickerCopy[ColorPickerText.OpenPicker],
                        role = Role.Button,
                        haptic = LevyraHapticAction.Transport
                    )
                    .semantics {
                        contentDescription = "${pickerCopy[ColorPickerText.OpenPicker]} #${formatLevyraHexColor(activeArgb)}"
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(onDisplayColor.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = onDisplayColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            OutlinedTextField(
                value = hexText,
                onValueChange = onHexTextChange,
                singleLine = true,
                isError = parsed == null,
                prefix = { Text("#", fontWeight = FontWeight.Bold) },
                label = { Text(hexLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.weight(1f)
            )

            FilledTonalButton(
                onClick = onOpenPicker,
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                modifier = Modifier.heightIn(min = 52.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = pickerCopy[ColorPickerText.OpenPicker],
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
internal fun LevyraAdvancedColorPickerDialog(
    initialColor: Int,
    presets: List<Int>,
    title: String? = null,
    onPreviewColor: ((Int) -> Unit)? = null,
    onConfirmColor: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val strings = LocalLevyraStrings.current
    val copy = strings.colorPickerCopy
    val haptics = LocalLevyraHaptics.current
    val focusManager = LocalFocusManager.current
    val animated = LocalAnimationsEnabled.current
    val normalizedInitial = initialColor or 0xFF000000.toInt()

    var hsv by remember(normalizedInitial) { mutableStateOf(argbToHsv(normalizedInitial)) }
    var hexInput by remember(normalizedInitial) { mutableStateOf(formatLevyraHexColor(normalizedInitial)) }
    var showSliders by remember { mutableStateOf(false) }
    var lastPreviewedArgb by remember(normalizedInitial) { mutableStateOf(normalizedInitial) }

    val selectedArgb = remember(hsv) { hsv.toArgb() }
    val selectedColor = Color(selectedArgb)
    val hexParsed = parseLevyraHexColor(hexInput)
    val accentPair = remember(selectedArgb) { playerAccentsFromSeed(selectedColor) }
    val animatedPrimary by animateColorAsState(
        targetValue = accentPair.primary,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(180)),
        label = "picker-dialog-primary"
    )

    val updateFromHsv: (LevyraHsvColor) -> Unit = { nextHsv ->
        val clean = nextHsv.normalized()
        hsv = clean
        val argb = clean.toArgb()
        hexInput = formatLevyraHexColor(argb)
        if (argb != lastPreviewedArgb) {
            lastPreviewedArgb = argb
            onPreviewColor?.invoke(argb)
        }
    }

    val selectArgb: (Int) -> Unit = { targetArgb ->
        val opaque = targetArgb or 0xFF000000.toInt()
        val nextHsv = argbToHsv(opaque, preserveHue = hsv.hue)
        hsv = nextHsv
        hexInput = formatLevyraHexColor(opaque)
        if (opaque != lastPreviewedArgb) {
            lastPreviewedArgb = opaque
            onPreviewColor?.invoke(opaque)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = PickerDialogShape,
            color = scheme.surfaceContainer,
            contentColor = scheme.onSurface,
            border = BorderStroke(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        animatedPrimary.copy(alpha = 0.48f),
                        scheme.outlineVariant.copy(alpha = 0.36f)
                    )
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .widthIn(max = 430.dp)
                .imePadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                animatedPrimary.copy(alpha = 0.12f),
                                Color.Transparent,
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ColorPickerHeader(
                        title = title ?: copy[ColorPickerText.Title],
                        subtitle = copy[ColorPickerText.Subtitle],
                        accentColor = animatedPrimary,
                        selectedHex = "#${formatLevyraHexColor(selectedArgb)}",
                        cancelLabel = copy[ColorPickerText.Cancel],
                        onClose = onDismiss
                    )

                    ColorPickerLiveStage(
                        savedArgb = normalizedInitial,
                        selectedArgb = selectedArgb,
                        hsv = hsv,
                        copy = copy,
                        onResetToSaved = {
                            haptics.perform(LevyraHapticAction.Transport)
                            selectArgb(normalizedInitial)
                        }
                    )

                    ColorPickerHsvWorkspace(
                        hsv = hsv,
                        selectedColor = selectedColor,
                        copy = copy,
                        onHsvChange = updateFromHsv
                    )

                    if (presets.isNotEmpty()) {
                        ColorPickerPresetsRow(
                            presets = presets,
                            selectedArgb = selectedArgb,
                            copy = copy,
                            onSelectPreset = { swatch ->
                                haptics.perform(LevyraHapticAction.Confirm)
                                focusManager.clearFocus()
                                selectArgb(swatch)
                            }
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = hexInput,
                            onValueChange = { raw ->
                                val cleaned = sanitizeHexInput(raw)
                                hexInput = cleaned
                                val parsed = parseLevyraHexColor(cleaned)
                                if (parsed != null && parsed != hsv.toArgb()) {
                                    hsv = argbToHsv(parsed, preserveHue = hsv.hue)
                                    if (parsed != lastPreviewedArgb) {
                                        lastPreviewedArgb = parsed
                                        onPreviewColor?.invoke(parsed)
                                    }
                                }
                            },
                            singleLine = true,
                            isError = hexParsed == null,
                            prefix = { Text("#", fontWeight = FontWeight.Bold) },
                            label = { Text(copy[ColorPickerText.HexLabel]) },
                            supportingText = if (hexParsed == null) {
                                { Text(copy[ColorPickerText.HexInvalid], maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            } else {
                                null
                            },
                            shape = RoundedCornerShape(18.dp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = showSliders,
                            onClick = {
                                haptics.perform(LevyraHapticAction.Transport)
                                showSliders = !showSliders
                            },
                            label = {
                                Text(
                                    text = copy[ColorPickerText.FineTune],
                                    maxLines = 1,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.heightIn(min = 52.dp)
                        )
                    }

                    AnimatedVisibility(visible = showSliders) {
                        ColorPickerPrecisionSliders(
                            hsv = hsv,
                            copy = copy,
                            onHsvChange = updateFromHsv
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = scheme.surfaceContainerHighest,
                                contentColor = scheme.onSurface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 50.dp)
                        ) {
                            Text(
                                text = copy[ColorPickerText.Cancel],
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = {
                                val finalArgb = hexParsed ?: selectedArgb
                                haptics.perform(LevyraHapticAction.Confirm)
                                onConfirmColor(finalArgb)
                            },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .weight(1.25f)
                                .heightIn(min = 50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = copy[ColorPickerText.Apply],
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPickerHeader(
    title: String,
    subtitle: String,
    accentColor: Color,
    selectedHex: String,
    cancelLabel: String,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(accentColor.copy(alpha = 0.20f))
                    .border(1.dp, accentColor.copy(alpha = 0.50f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.3).sp
                    ),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = selectedHex,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHighest.copy(alpha = 0.85f))
                    .levyraPressable(
                        onClick = onClose,
                        onClickLabel = cancelLabel,
                        role = Role.Button
                    )
                    .semantics { contentDescription = cancelLabel },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Surface(
            color = scheme.surfaceContainerHigh.copy(alpha = 0.72f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.28f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = scheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ColorPickerLiveStage(
    savedArgb: Int,
    selectedArgb: Int,
    hsv: LevyraHsvColor,
    copy: ColorPickerCopy,
    onResetToSaved: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val animated = LocalAnimationsEnabled.current
    val selectedColor = Color(selectedArgb)
    val savedColor = Color(savedArgb)
    val accentPair = remember(selectedArgb) { playerAccentsFromSeed(selectedColor) }
    val animatedPrimary by animateColorAsState(
        targetValue = accentPair.primary,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(160)),
        label = "picker-stage-primary"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = accentPair.secondary,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.standardTween(160)),
        label = "picker-stage-secondary"
    )
    val onSelected = if (selectedColor.luminance() > 0.52f) Color.Black else Color.White
    val onSaved = if (savedColor.luminance() > 0.52f) Color.Black else Color.White
    val (r, g, b) = rgbChannelsOf(selectedArgb)
    val hDeg = hsv.hue.roundToInt().coerceIn(0, 360)
    val sPct = (hsv.saturation * 100f).roundToInt().coerceIn(0, 100)
    val vPct = (hsv.value * 100f).roundToInt().coerceIn(0, 100)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PickerStageShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.surfaceContainerHigh,
                        scheme.surfaceContainerHighest.copy(alpha = 0.90f)
                    )
                )
            )
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.40f), PickerStageShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surfaceContainerLowest)
                    .border(1.dp, scheme.outlineVariant.copy(alpha = 0.50f), RoundedCornerShape(16.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(54.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 13.dp, bottomStart = 13.dp, topEnd = 6.dp, bottomEnd = 6.dp))
                        .background(savedColor)
                        .levyraPressable(
                            onClick = onResetToSaved,
                            onClickLabel = copy[ColorPickerText.SavedColor],
                            role = Role.Button
                        )
                        .semantics {
                            contentDescription = "${copy[ColorPickerText.SavedColor]} #${formatLevyraHexColor(savedArgb)}"
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = if (savedColor.luminance() > 0.52f) 0.14f else 0.34f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = copy[ColorPickerText.SavedColor],
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSaved,
                            maxLines = 1
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .width(58.dp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 13.dp, bottomEnd = 13.dp))
                        .background(selectedColor)
                        .semantics {
                            contentDescription = "${copy[ColorPickerText.SelectedColor]} #${formatLevyraHexColor(selectedArgb)}"
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = if (selectedColor.luminance() > 0.52f) 0.14f else 0.34f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = copy[ColorPickerText.SelectedColor],
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = onSelected,
                            maxLines = 1
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surfaceContainerLowest)
                    .border(1.dp, animatedPrimary.copy(alpha = 0.42f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Brush.linearGradient(listOf(animatedPrimary, animatedSecondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = onSelected.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "#${formatLevyraHexColor(selectedArgb)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = scheme.onSurface,
                        maxLines = 1
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(scheme.onSurfaceVariant.copy(alpha = 0.20f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.68f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(listOf(animatedPrimary, animatedSecondary))
                                )
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(animatedPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = onSelected,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ColorMetricChip(
                badge = "RGB",
                valueText = "$r, $g, $b",
                accentColor = animatedPrimary,
                modifier = Modifier.weight(0.95f)
            )
            ColorMetricChip(
                badge = "HSV",
                valueText = "${hDeg}°, $sPct%, $vPct%",
                accentColor = animatedPrimary,
                modifier = Modifier.weight(1.05f)
            )
        }
    }
}

@Composable
private fun ColorMetricChip(
    badge: String,
    valueText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(scheme.surfaceContainerLowest.copy(alpha = 0.82f))
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.28f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(accentColor.copy(alpha = 0.16f))
                .padding(horizontal = 5.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badge,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Black,
                color = accentColor,
                maxLines = 1,
                softWrap = false
            )
        }
        Text(
            text = valueText,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ColorPickerHsvWorkspace(
    hsv: LevyraHsvColor,
    selectedColor: Color,
    copy: ColorPickerCopy,
    onHsvChange: (LevyraHsvColor) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        color = scheme.surfaceContainerLow,
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.36f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
                .height(216.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SaturationValuePalette(
                hsv = hsv,
                selectedColor = selectedColor,
                label = copy[ColorPickerText.PaletteLabel],
                saturationLabel = copy[ColorPickerText.SaturationLabel],
                brightnessLabel = copy[ColorPickerText.BrightnessLabel],
                onHsvChange = onHsvChange,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            VerticalHueSpectrumBar(
                hsv = hsv,
                label = copy[ColorPickerText.HueLabel],
                onHsvChange = onHsvChange,
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun SaturationValuePalette(
    hsv: LevyraHsvColor,
    selectedColor: Color,
    label: String,
    saturationLabel: String,
    brightnessLabel: String,
    onHsvChange: (LevyraHsvColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalLevyraHaptics.current
    val animationsEnabled = LocalAnimationsEnabled.current
    val latestHsv by rememberUpdatedState(hsv)
    val latestOnHsvChange by rememberUpdatedState(onHsvChange)
    var dragging by remember { mutableStateOf(false) }
    var atBoundary by remember { mutableStateOf(false) }

    val thumbScale by animateFloatAsState(
        targetValue = if (dragging && animationsEnabled) 1.22f else 1f,
        animationSpec = LevyraPlayerDesign.snappySpring(),
        label = "palette-thumb-scale"
    )
    val pureHueColor = remember(hsv.hue) { Color.hsv(hsv.hue.mod(360f), 1f, 1f) }
    val sPct = (hsv.saturation * 100f).roundToInt()
    val vPct = (hsv.value * 100f).roundToInt()

    val updatePoint: (Offset, IntSize, Boolean) -> Unit = { offset, size, isFirstDown ->
        if (size.width > 0 && size.height > 0) {
            val normX = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
            val normY = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
            val boundaryNow = normX <= 0.005f || normX >= 0.995f || normY <= 0.005f || normY >= 0.995f
            if (isFirstDown || (boundaryNow && !atBoundary)) {
                haptics.perform(LevyraHapticAction.SeekSnap)
            }
            atBoundary = boundaryNow
            latestOnHsvChange(latestHsv.withPalettePoint(normX, normY))
        }
    }

    Box(
        modifier = modifier
            .semantics {
                contentDescription = label
                stateDescription = "$saturationLabel $sPct%, $brightnessLabel $vPct%"
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    dragging = true
                    updatePoint(down.position, size, true)
                    try {
                        var pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                                ?: event.changes.firstOrNull()
                                ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            pointerId = change.id
                            change.consume()
                            updatePoint(change.position, size, false)
                        }
                    } finally {
                        dragging = false
                        atBoundary = false
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(PaletteFieldShape)
                .border(1.2.dp, scheme.outlineVariant.copy(alpha = 0.6f), PaletteFieldShape)
        ) {
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, pureHueColor)
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black)
                )
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = 14.dp.toPx()
            val usableWidth = (size.width - inset * 2f).coerceAtLeast(1f)
            val usableHeight = (size.height - inset * 2f).coerceAtLeast(1f)
            val center = Offset(
                x = inset + hsv.saturation.coerceIn(0f, 1f) * usableWidth,
                y = inset + (1f - hsv.value.coerceIn(0f, 1f)) * usableHeight
            )
            val baseRadius = 10.5.dp.toPx() * thumbScale
            drawCircle(
                color = Color.Black.copy(alpha = 0.42f),
                radius = baseRadius + 3.5.dp.toPx(),
                center = center
            )
            drawCircle(
                color = selectedColor,
                radius = baseRadius,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = baseRadius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.65f),
                radius = (baseRadius - 2.dp.toPx()).coerceAtLeast(2f),
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

@Composable
private fun VerticalHueSpectrumBar(
    hsv: LevyraHsvColor,
    label: String,
    onHsvChange: (LevyraHsvColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalLevyraHaptics.current
    val animationsEnabled = LocalAnimationsEnabled.current
    val latestHsv by rememberUpdatedState(hsv)
    val latestOnHsvChange by rememberUpdatedState(onHsvChange)
    var dragging by remember { mutableStateOf(false) }

    val thumbScale by animateFloatAsState(
        targetValue = if (dragging && animationsEnabled) 1.14f else 1f,
        animationSpec = LevyraPlayerDesign.snappySpring(),
        label = "hue-thumb-scale"
    )
    val pureHueColor = remember(hsv.hue) { Color.hsv(hsv.hue.mod(360f), 1f, 1f) }
    val hueDegrees = hsv.hue.roundToInt().coerceIn(0, 360)

    val updateHue: (Offset, IntSize, Boolean) -> Unit = { offset, size, isFirstDown ->
        if (size.height > 0) {
            val normY = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
            if (isFirstDown) {
                haptics.perform(LevyraHapticAction.SeekSnap)
            }
            latestOnHsvChange(latestHsv.withHueFraction(normY))
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .semantics {
                contentDescription = label
                stateDescription = "${hueDegrees}°"
                setProgress { targetValue ->
                    val clamped = targetValue.coerceIn(0f, 360f)
                    latestOnHsvChange(latestHsv.copy(hue = clamped))
                    true
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    dragging = true
                    updateHue(down.position, size, true)
                    try {
                        var pointerId = down.id
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                                ?: event.changes.firstOrNull()
                                ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            pointerId = change.id
                            change.consume()
                            updateHue(change.position, size, false)
                        }
                    } finally {
                        dragging = false
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .width(34.dp)
                .fillMaxHeight()
                .clip(HueBarShape)
                .border(1.2.dp, scheme.outlineVariant.copy(alpha = 0.6f), HueBarShape)
        ) {
            drawRect(
                brush = Brush.verticalGradient(colors = HueSpectrumColors)
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val insetY = 12.dp.toPx()
            val usableHeight = (size.height - insetY * 2f).coerceAtLeast(1f)
            val fraction = (hsv.hue.coerceIn(0f, 360f) / 360f).coerceIn(0f, 1f)
            val centerY = insetY + fraction * usableHeight
            val thumbWidth = 42.dp.toPx() * thumbScale
            val thumbHeight = 18.dp.toPx() * thumbScale
            val topLeft = Offset(
                x = (size.width - thumbWidth) / 2f,
                y = centerY - thumbHeight / 2f
            )
            val thumbSize = Size(thumbWidth, thumbHeight)
            val corner = CornerRadius(thumbHeight / 2f, thumbHeight / 2f)

            drawRoundRect(
                color = Color.Black.copy(alpha = 0.40f),
                topLeft = topLeft - Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(thumbWidth + 3.dp.toPx(), thumbHeight + 3.dp.toPx()),
                cornerRadius = CornerRadius((thumbHeight + 3.dp.toPx()) / 2f)
            )
            drawRoundRect(
                color = pureHueColor,
                topLeft = topLeft,
                size = thumbSize,
                cornerRadius = corner
            )
            drawRoundRect(
                color = Color.White,
                topLeft = topLeft,
                size = thumbSize,
                cornerRadius = corner,
                style = Stroke(width = 2.8.dp.toPx())
            )
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = topLeft + Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                size = Size(
                    (thumbWidth - 3.dp.toPx()).coerceAtLeast(2f),
                    (thumbHeight - 3.dp.toPx()).coerceAtLeast(2f)
                ),
                cornerRadius = CornerRadius((thumbHeight - 3.dp.toPx()) / 2f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
private fun ColorPickerPresetsRow(
    presets: List<Int>,
    selectedArgb: Int,
    copy: ColorPickerCopy,
    onSelectPreset: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = copy[ColorPickerText.PresetsLabel],
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurfaceVariant
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets, key = { it }) { swatch ->
                val opaqueSwatch = swatch or 0xFF000000.toInt()
                val active = opaqueSwatch == selectedArgb
                val swatchColor = Color(opaqueSwatch)
                val hex = "#" + formatLevyraHexColor(opaqueSwatch)
                val checkTint = if (swatchColor.luminance() > 0.52f) Color.Black else Color.White
                val shape = RoundedCornerShape(if (active) 14.dp else 20.dp)

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .levyraPressable(
                            onClick = { onSelectPreset(opaqueSwatch) },
                            onClickLabel = hex,
                            role = Role.RadioButton
                        )
                        .semantics {
                            selected = active
                            contentDescription = hex
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (active) 40.dp else 34.dp)
                            .clip(shape)
                            .background(swatchColor)
                            .border(
                                width = if (active) 2.5.dp else 1.dp,
                                color = if (active) scheme.onSurface else scheme.outlineVariant.copy(alpha = 0.5f),
                                shape = shape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (active) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = checkTint,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPickerPrecisionSliders(
    hsv: LevyraHsvColor,
    copy: ColorPickerCopy,
    onHsvChange: (LevyraHsvColor) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surfaceContainerHigh)
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ColorChannelSlider(
            label = copy[ColorPickerText.HueLabel],
            valueText = "${hsv.hue.roundToInt().coerceIn(0, 360)}°",
            value = hsv.hue.coerceIn(0f, 360f),
            range = 0f..360f,
            onValueChange = { onHsvChange(hsv.copy(hue = it)) }
        )
        ColorChannelSlider(
            label = copy[ColorPickerText.SaturationLabel],
            valueText = "${(hsv.saturation * 100f).roundToInt().coerceIn(0, 100)}%",
            value = (hsv.saturation * 100f).coerceIn(0f, 100f),
            range = 0f..100f,
            onValueChange = { onHsvChange(hsv.copy(saturation = (it / 100f).coerceIn(0f, 1f))) }
        )
        ColorChannelSlider(
            label = copy[ColorPickerText.BrightnessLabel],
            valueText = "${(hsv.value * 100f).roundToInt().coerceIn(0, 100)}%",
            value = (hsv.value * 100f).coerceIn(0f, 100f),
            range = 0f..100f,
            onValueChange = { onHsvChange(hsv.copy(value = (it / 100f).coerceIn(0f, 1f))) }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColorChannelSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = scheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = scheme.primary
            )
        }
        val sliderState = remember(range) { SliderState(value, 0, range) }
        sliderState.value = value
        Slider(
            state = sliderState,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = label }
        )
    }
}
