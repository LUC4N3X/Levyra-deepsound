package com.luc4n3x.levyra.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LocalLevyraHaptics
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val HexAllowedChars = "0123456789abcdefABCDEF"
private val PickerDialogShape = RoundedCornerShape(28.dp)
private val WorkspaceCardShape = RoundedCornerShape(24.dp)
private val PaletteFieldShape = RoundedCornerShape(24.dp)
private val HueBarShape = CircleShape
private val PaletteThumbInset = 14.dp
private val HueBarThumbInset = 18.dp

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

internal fun paletteNormalizedPoint(
    offset: Offset,
    size: IntSize,
    insetPx: Float
): Pair<Float, Float> {
    val safeInsetX = insetPx.coerceIn(0f, (size.width.toFloat() - 1f).coerceAtLeast(0f) / 2f)
    val safeInsetY = insetPx.coerceIn(0f, (size.height.toFloat() - 1f).coerceAtLeast(0f) / 2f)
    val usableWidth = (size.width.toFloat() - safeInsetX * 2f).coerceAtLeast(1f)
    val usableHeight = (size.height.toFloat() - safeInsetY * 2f).coerceAtLeast(1f)
    val normX = ((offset.x - safeInsetX) / usableWidth).coerceIn(0f, 1f)
    val normY = ((offset.y - safeInsetY) / usableHeight).coerceIn(0f, 1f)
    return normX to normY
}

internal fun paletteIndicatorCenter(
    hsv: LevyraHsvColor,
    canvasSize: Size,
    insetPx: Float
): Offset {
    val safeInsetX = insetPx.coerceIn(0f, (canvasSize.width - 1f).coerceAtLeast(0f) / 2f)
    val safeInsetY = insetPx.coerceIn(0f, (canvasSize.height - 1f).coerceAtLeast(0f) / 2f)
    val usableWidth = (canvasSize.width - safeInsetX * 2f).coerceAtLeast(1f)
    val usableHeight = (canvasSize.height - safeInsetY * 2f).coerceAtLeast(1f)
    return Offset(
        x = safeInsetX + hsv.saturation.coerceIn(0f, 1f) * usableWidth,
        y = safeInsetY + (1f - hsv.value.coerceIn(0f, 1f)) * usableHeight
    )
}

internal fun hueBarNormalizedFraction(
    offset: Float,
    length: Int,
    insetPx: Float
): Float {
    val safeInset = insetPx.coerceIn(0f, (length.toFloat() - 1f).coerceAtLeast(0f) / 2f)
    val usableLength = (length.toFloat() - safeInset * 2f).coerceAtLeast(1f)
    return ((offset - safeInset) / usableLength).coerceIn(0f, 1f)
}

internal fun hueBarIndicatorCenter(
    hsv: LevyraHsvColor,
    canvasLength: Float,
    insetPx: Float
): Float {
    val safeInset = insetPx.coerceIn(0f, (canvasLength - 1f).coerceAtLeast(0f) / 2f)
    val usableLength = (canvasLength - safeInset * 2f).coerceAtLeast(1f)
    val fraction = (hsv.hue.coerceIn(0f, 360f) / 360f).coerceIn(0f, 1f)
    return safeInset + fraction * usableLength
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LevyraColorInputAndPickerRow(
    color: Int,
    hexText: String,
    hexLabel: String,
    onHexTextChange: (String) -> Unit,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pickerCopy = LocalLevyraStrings.current.colorPickerCopy
    val parsed = parseLevyraHexColor(hexText)
    val activeArgb = parsed ?: color
    val displayColor = Color(activeArgb)
    val onDisplayColor = if (displayColor.luminance() > 0.52f) Color.Black else Color.White
    val swatchShape = MaterialShapes.Cookie9Sided.toShape()

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
                    .clip(swatchShape)
                    .background(displayColor)
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
                        .size(28.dp)
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
                shape = RoundedCornerShape(18.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.weight(1f)
            )

            FilledTonalButton(
                onClick = onOpenPicker,
                shapes = ButtonDefaults.shapes(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                modifier = Modifier.heightIn(min = 52.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
                Text(
                    text = pickerCopy[ColorPickerText.OpenPicker],
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun LevyraAdvancedColorPickerDialog(
    initialColor: Int,
    presets: List<Int>,
    title: String? = null,
    onPreviewColor: ((Int) -> Unit)? = null,
    onConfirmColor: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val copy = LocalLevyraStrings.current.colorPickerCopy
    val haptics = LocalLevyraHaptics.current
    val focusManager = LocalFocusManager.current
    val normalizedInitial = remember { initialColor or 0xFF000000.toInt() }
    val latestOnPreviewColor by rememberUpdatedState(onPreviewColor)
    val latestOnConfirmColor by rememberUpdatedState(onConfirmColor)
    val latestOnDismiss by rememberUpdatedState(onDismiss)

    var hsv by remember { mutableStateOf(argbToHsv(normalizedInitial)) }
    var hexInput by remember { mutableStateOf(formatLevyraHexColor(normalizedInitial)) }
    var showSliders by remember { mutableStateOf(false) }
    var lastPreviewedArgb by remember { mutableStateOf(normalizedInitial) }

    val selectedArgb = remember(hsv) { hsv.toArgb() }
    val selectedColor = Color(selectedArgb)
    val hexParsed = parseLevyraHexColor(hexInput)

    val dispatchPreview: (Int) -> Unit = { argb ->
        if (argb != lastPreviewedArgb) {
            lastPreviewedArgb = argb
            latestOnPreviewColor?.invoke(argb)
        }
    }
    val updateFromHsv: (LevyraHsvColor) -> Unit = { nextHsv ->
        val clean = nextHsv.normalized()
        hsv = clean
        val argb = clean.toArgb()
        hexInput = formatLevyraHexColor(argb)
        dispatchPreview(argb)
    }
    val selectArgb: (Int) -> Unit = { targetArgb ->
        val opaque = targetArgb or 0xFF000000.toInt()
        hsv = argbToHsv(opaque, preserveHue = hsv.hue)
        hexInput = formatLevyraHexColor(opaque)
        dispatchPreview(opaque)
    }

    Dialog(
        onDismissRequest = { latestOnDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        ColorPickerDialogContainer(
            header = {
                ColorPickerHeader(
                    title = title ?: copy[ColorPickerText.Title],
                    subtitle = copy[ColorPickerText.Subtitle]
                )
            },
            footer = {
                ColorPickerActionFooter(
                    cancelLabel = copy[ColorPickerText.Cancel],
                    applyLabel = copy[ColorPickerText.Apply],
                    applyColor = Color(hexParsed ?: selectedArgb),
                    onCancel = { latestOnDismiss() },
                    onApply = {
                        val finalArgb = hexParsed ?: selectedArgb
                        haptics.perform(LevyraHapticAction.Confirm)
                        latestOnConfirmColor(finalArgb)
                    }
                )
            }
        ) {
            ColorPickerComparisonHero(
                savedArgb = normalizedInitial,
                selectedArgb = selectedArgb,
                copy = copy,
                onResetToSaved = {
                    haptics.perform(LevyraHapticAction.Transport)
                    selectArgb(normalizedInitial)
                }
            )
            ColorPickerMetricsRow(
                hsv = hsv,
                selectedArgb = selectedArgb
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
            ColorPickerHexAndTuneRow(
                hexInput = hexInput,
                isHexError = hexParsed == null,
                showSliders = showSliders,
                copy = copy,
                onHexChange = { raw ->
                    val cleaned = sanitizeHexInput(raw)
                    hexInput = cleaned
                    val parsed = parseLevyraHexColor(cleaned)
                    if (parsed != null && parsed != hsv.toArgb()) {
                        hsv = argbToHsv(parsed, preserveHue = hsv.hue)
                        dispatchPreview(parsed)
                    }
                },
                onToggleSliders = {
                    haptics.perform(LevyraHapticAction.Transport)
                    showSliders = !showSliders
                },
                onDoneIme = { focusManager.clearFocus() }
            )
            AnimatedVisibility(
                visible = showSliders,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ColorPickerPrecisionSliders(
                    hsv = hsv,
                    copy = copy,
                    onHsvChange = updateFromHsv
                )
            }
        }
    }
}

@Composable
private fun ColorPickerDialogContainer(
    header: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = PickerDialogShape,
        color = scheme.surfaceContainerHigh,
        contentColor = scheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp)
            .widthIn(max = 420.dp)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                header()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                content()
            }
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                footer()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColorPickerHexAndTuneRow(
    hexInput: String,
    isHexError: Boolean,
    showSliders: Boolean,
    copy: ColorPickerCopy,
    onHexChange: (String) -> Unit,
    onToggleSliders: () -> Unit,
    onDoneIme: () -> Unit
) {
    val supportingContent: (@Composable () -> Unit)? = if (isHexError) {
        { Text(copy[ColorPickerText.HexInvalid], maxLines = 1, overflow = TextOverflow.Ellipsis) }
    } else {
        null
    }
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = hexInput,
            onValueChange = onHexChange,
            singleLine = true,
            isError = isHexError,
            prefix = { Text("#", fontWeight = FontWeight.Bold) },
            label = { Text(copy[ColorPickerText.HexLabel], maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingText = supportingContent,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDoneIme() }),
            modifier = Modifier.weight(1f)
        )
        FilledTonalIconToggleButton(
            checked = showSliders,
            onCheckedChange = { onToggleSliders() },
            shapes = IconButtonDefaults.toggleableShapes(),
            modifier = Modifier
                .padding(top = 8.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = copy[ColorPickerText.FineTune]
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColorPickerActionFooter(
    cancelLabel: String,
    applyLabel: String,
    applyColor: Color,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    val buttonHeight = ButtonDefaults.MediumContainerHeight
    val applyContentColor = if (applyColor.luminance() > 0.45f) Color.Black else Color.White
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onCancel,
            shapes = ButtonDefaults.shapes(),
            contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = buttonHeight)
        ) {
            Text(
                text = cancelLabel,
                style = ButtonDefaults.textStyleFor(buttonHeight),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Button(
            onClick = onApply,
            shapes = ButtonDefaults.shapes(),
            colors = ButtonDefaults.buttonColors(
                containerColor = applyColor,
                contentColor = applyContentColor
            ),
            contentPadding = ButtonDefaults.contentPaddingFor(buttonHeight),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = buttonHeight)
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.iconSizeFor(buttonHeight))
            )
            Spacer(modifier = Modifier.size(ButtonDefaults.iconSpacingFor(buttonHeight)))
            Text(
                text = applyLabel,
                style = ButtonDefaults.textStyleFor(buttonHeight),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ColorPickerHeader(
    title: String,
    subtitle: String
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ColorPickerComparisonHero(
    savedArgb: Int,
    selectedArgb: Int,
    copy: ColorPickerCopy,
    onResetToSaved: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ColorPickerSwatchPane(
            argb = savedArgb,
            label = copy[ColorPickerText.SavedColor],
            emphasized = false,
            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp),
            onClick = if (savedArgb != selectedArgb) onResetToSaved else null,
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
        )
        ColorPickerSwatchPane(
            argb = selectedArgb,
            label = copy[ColorPickerText.SelectedColor],
            emphasized = true,
            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 24.dp, bottomEnd = 24.dp),
            onClick = null,
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
        )
    }
}

@Composable
private fun ColorPickerSwatchPane(
    argb: Int,
    label: String,
    emphasized: Boolean,
    shape: RoundedCornerShape,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val color = Color(argb)
    val onColor = if (color.luminance() > 0.45f) Color.Black else Color.White
    val hex = "#${formatLevyraHexColor(argb)}"
    Column(
        modifier = modifier
            .clip(shape)
            .background(color)
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.35f), shape)
            .then(
                if (onClick != null) {
                    Modifier.levyraPressable(
                        onClick = onClick,
                        onClickLabel = label,
                        role = Role.Button
                    )
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = onColor.copy(alpha = 0.78f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = hex,
            fontFamily = FontFamily.Monospace,
            style = if (emphasized) {
                MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
            } else {
                MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            },
            color = onColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ColorPickerMetricsRow(
    hsv: LevyraHsvColor,
    selectedArgb: Int
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val (r, g, b) = rgbChannelsOf(selectedArgb)
    val hDeg = hsv.hue.roundToInt().coerceIn(0, 360)
    val sPct = (hsv.saturation * 100f).roundToInt().coerceIn(0, 100)
    val vPct = (hsv.value * 100f).roundToInt().coerceIn(0, 100)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColorMetricText(badge = "HSV", valueText = "${hDeg}° $sPct% $vPct%", accentColor = accentColor)
        ColorMetricText(badge = "RGB", valueText = "$r $g $b", accentColor = accentColor)
    }
}

@Composable
private fun ColorMetricText(
    badge: String,
    valueText: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = badge,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = accentColor,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = valueText,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SaturationValuePalette(
            hsv = hsv,
            selectedColor = selectedColor,
            label = copy[ColorPickerText.PaletteLabel],
            saturationLabel = copy[ColorPickerText.SaturationLabel],
            brightnessLabel = copy[ColorPickerText.BrightnessLabel],
            onHsvChange = onHsvChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
        )
        HueSpectrumBar(
            hsv = hsv,
            label = copy[ColorPickerText.HueLabel],
            onHsvChange = onHsvChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
        )
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
        targetValue = if (dragging && animationsEnabled) 1.24f else 1f,
        animationSpec = LevyraPlayerDesign.snappySpring(),
        label = "palette-thumb-scale"
    )
    val pureHueColor = remember(hsv.hue) { Color.hsv(hsv.hue.mod(360f), 1f, 1f) }
    val sPct = (hsv.saturation * 100f).roundToInt()
    val vPct = (hsv.value * 100f).roundToInt()
    val insetPx = with(LocalDensity.current) { PaletteThumbInset.toPx() }

    val updatePoint: (Offset, IntSize, Boolean) -> Unit = { offset, size, isFirstDown ->
        if (size.width > 0 && size.height > 0) {
            val (normX, normY) = paletteNormalizedPoint(offset, size, insetPx)
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
            .pointerInput(insetPx) {
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
                .border(1.dp, scheme.outlineVariant.copy(alpha = 0.50f), PaletteFieldShape)
        ) {
            val safeInsetX = insetPx.coerceIn(0f, (size.width - 1f).coerceAtLeast(0f) / 2f)
            val safeInsetY = insetPx.coerceIn(0f, (size.height - 1f).coerceAtLeast(0f) / 2f)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.White, pureHueColor),
                    startX = safeInsetX,
                    endX = (size.width - safeInsetX).coerceAtLeast(safeInsetX + 1f)
                )
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = safeInsetY,
                    endY = (size.height - safeInsetY).coerceAtLeast(safeInsetY + 1f)
                )
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = paletteIndicatorCenter(hsv, size, insetPx)
            val baseRadius = 11.dp.toPx() * thumbScale
            drawCircle(
                color = Color.Black.copy(alpha = 0.36f),
                radius = baseRadius + 4.dp.toPx(),
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
                color = Color.Black.copy(alpha = 0.60f),
                radius = (baseRadius - 2.dp.toPx()).coerceAtLeast(2f),
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }
    }
}

@Composable
private fun HueSpectrumBar(
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
        targetValue = if (dragging && animationsEnabled) 1.15f else 1f,
        animationSpec = LevyraPlayerDesign.snappySpring(),
        label = "hue-thumb-scale"
    )
    val pureHueColor = remember(hsv.hue) { Color.hsv(hsv.hue.mod(360f), 1f, 1f) }
    val hueDegrees = hsv.hue.roundToInt().coerceIn(0, 360)
    val insetPx = with(LocalDensity.current) { HueBarThumbInset.toPx() }

    val updateHue: (Offset, IntSize, Boolean) -> Unit = { offset, size, isFirstDown ->
        if (size.width > 0) {
            val fraction = hueBarNormalizedFraction(offset.x, size.width, insetPx)
            if (isFirstDown) {
                haptics.perform(LevyraHapticAction.SeekSnap)
            }
            latestOnHsvChange(latestHsv.withHueFraction(fraction))
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
            .pointerInput(insetPx) {
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
                .fillMaxSize()
                .clip(HueBarShape)
                .border(1.dp, scheme.outlineVariant.copy(alpha = 0.50f), HueBarShape)
        ) {
            val safeInset = insetPx.coerceIn(0f, (size.width - 1f).coerceAtLeast(0f) / 2f)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = HueSpectrumColors,
                    startX = safeInset,
                    endX = (size.width - safeInset).coerceAtLeast(safeInset + 1f)
                )
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(
                x = hueBarIndicatorCenter(hsv, size.width, insetPx),
                y = size.height / 2f
            )
            val radius = (size.height / 2f - 3.dp.toPx()).coerceAtLeast(6.dp.toPx()) * thumbScale
            drawCircle(
                color = Color.Black.copy(alpha = 0.36f),
                radius = radius + 2.dp.toPx(),
                center = center
            )
            drawCircle(
                color = pureHueColor,
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ColorPickerPresetsRow(
    presets: List<Int>,
    selectedArgb: Int,
    copy: ColorPickerCopy,
    onSelectPreset: (Int) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val animated = LocalAnimationsEnabled.current
    val activeCookieShape = MaterialShapes.Cookie9Sided.toShape()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = copy[ColorPickerText.PresetsLabel],
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets, key = { it }) { swatch ->
                val opaqueSwatch = swatch or 0xFF000000.toInt()
                val active = opaqueSwatch == selectedArgb
                val swatchColor = Color(opaqueSwatch)
                val hex = "#" + formatLevyraHexColor(opaqueSwatch)
                val checkTint = if (swatchColor.luminance() > 0.52f) Color.Black else Color.White
                val swatchSize by animateDpAsState(
                    targetValue = if (active) 42.dp else 34.dp,
                    animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.snappySpring()),
                    label = "preset-swatch-size"
                )
                val shape = if (active) activeCookieShape else CircleShape

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
                            .size(swatchSize)
                            .clip(shape)
                            .background(swatchColor)
                            .then(
                                if (!active) {
                                    Modifier.border(
                                        width = 1.dp,
                                        color = scheme.outlineVariant.copy(alpha = 0.45f),
                                        shape = shape
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (active) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = checkTint,
                                modifier = Modifier.size(18.dp)
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
    Surface(
        color = scheme.surfaceContainerHigh,
        shape = WorkspaceCardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = scheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = valueText,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
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
