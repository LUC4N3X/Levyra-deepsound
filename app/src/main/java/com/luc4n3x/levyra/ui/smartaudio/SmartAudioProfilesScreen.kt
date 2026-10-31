package com.luc4n3x.levyra.ui.smartaudio

import android.media.AudioDeviceInfo
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.SettingsInputComponent
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.domain.AutoEqCatalogEntry
import com.luc4n3x.levyra.domain.AutoEqImporter
import com.luc4n3x.levyra.domain.LevyraAudioPresets
import com.luc4n3x.levyra.domain.LevyraAudioSettings
import com.luc4n3x.levyra.domain.NightListeningPreset
import com.luc4n3x.levyra.domain.ReplayGainMode
import com.luc4n3x.levyra.domain.SmartAudioDeviceCategory
import com.luc4n3x.levyra.domain.SmartAudioEqMode
import com.luc4n3x.levyra.domain.SmartAudioNightMode
import com.luc4n3x.levyra.domain.SmartAudioProfile
import com.luc4n3x.levyra.feature.audio.LevyraAudioOutputState
import com.luc4n3x.levyra.feature.audio.smartAudioDefaultCategory
import com.luc4n3x.levyra.feature.audio.smartAudioDeviceKey
import com.luc4n3x.levyra.ui.AutoEqCatalogDialog
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.SmartAudioCopy
import com.luc4n3x.levyra.ui.i18n.SmartAudioText
import com.luc4n3x.levyra.ui.i18n.localizedAudioPresetLabel
import com.luc4n3x.levyra.ui.i18n.smartAudioCopy
import com.luc4n3x.levyra.viewmodel.AutoEqCatalogUiState
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class SmartAudioDeviceUi(
    val key: String,
    val name: String,
    val category: SmartAudioDeviceCategory,
    val connected: Boolean,
    val active: Boolean,
    val profile: SmartAudioProfile?
)

internal fun smartAudioDevices(settings: LevyraAudioSettings, output: LevyraAudioOutputState): List<SmartAudioDeviceUi> {
    val activeKey = output.active?.let(::smartAudioDeviceKey)
    val connected = (listOfNotNull(output.active) + output.connected.filter { it.type !in nonMusicOutputTypes })
        .distinctBy(::smartAudioDeviceKey)
        .map { route ->
            val key = smartAudioDeviceKey(route)
            val profile = settings.smartAudioProfiles.firstOrNull { it.deviceKey == key }
            SmartAudioDeviceUi(
                key = key,
                name = route.displayName.ifBlank { profile?.deviceName.orEmpty() },
                category = profile?.category ?: smartAudioDefaultCategory(route),
                connected = true,
                active = key == activeKey,
                profile = profile
            )
        }
    val connectedKeys = connected.map(SmartAudioDeviceUi::key).toSet()
    val remembered = settings.smartAudioProfiles
        .filterNot { it.deviceKey in connectedKeys }
        .map { SmartAudioDeviceUi(it.deviceKey, it.deviceName, it.category, connected = false, active = false, profile = it) }
    return connected.sortedByDescending(SmartAudioDeviceUi::active) + remembered
}

private val nonMusicOutputTypes = setOf(
    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
    AudioDeviceInfo.TYPE_TELEPHONY,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_FM,
    AudioDeviceInfo.TYPE_IP,
    AudioDeviceInfo.TYPE_AUX_LINE,
    24,
    25
)

internal fun smartAudioCategoryIcon(category: SmartAudioDeviceCategory): ImageVector = when (category) {
    SmartAudioDeviceCategory.SPEAKER -> Icons.Rounded.Speaker
    SmartAudioDeviceCategory.WIRED -> Icons.Rounded.Headset
    SmartAudioDeviceCategory.BLUETOOTH_HEADPHONES -> Icons.Rounded.Headphones
    SmartAudioDeviceCategory.BLUETOOTH_SPEAKER -> Icons.Rounded.SpeakerGroup
    SmartAudioDeviceCategory.USB -> Icons.Rounded.Usb
    SmartAudioDeviceCategory.CAR -> Icons.Rounded.DirectionsCar
    SmartAudioDeviceCategory.OTHER -> Icons.Rounded.SettingsInputComponent
}

private fun categoryLabelKey(category: SmartAudioDeviceCategory): SmartAudioText = when (category) {
    SmartAudioDeviceCategory.SPEAKER -> SmartAudioText.CategorySpeaker
    SmartAudioDeviceCategory.WIRED -> SmartAudioText.CategoryWired
    SmartAudioDeviceCategory.BLUETOOTH_HEADPHONES -> SmartAudioText.CategoryBluetoothHeadphones
    SmartAudioDeviceCategory.BLUETOOTH_SPEAKER -> SmartAudioText.CategoryBluetoothSpeaker
    SmartAudioDeviceCategory.USB -> SmartAudioText.CategoryUsb
    SmartAudioDeviceCategory.CAR -> SmartAudioText.CategoryCar
    SmartAudioDeviceCategory.OTHER -> SmartAudioText.CategoryOther
}

@Composable
internal fun SmartAudioEntryCard(
    settings: LevyraAudioSettings,
    output: LevyraAudioOutputState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = smartAudioCopy(LocalLevyraStrings.current.code)
    val scheme = MaterialTheme.colorScheme
    val active = output.active
    val activeProfile = active?.let { route ->
        settings.smartAudioProfiles.firstOrNull { it.deviceKey == smartAudioDeviceKey(route) }
    }
    val category = activeProfile?.category ?: active?.let(::smartAudioDefaultCategory) ?: SmartAudioDeviceCategory.OTHER
    Surface(
        color = scheme.secondaryContainer,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 30.dp),
        modifier = modifier.fillMaxWidth().clickable(onClick = onOpen)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(scheme.onSecondaryContainer.copy(alpha = 0.12f))
            ) {
                Icon(smartAudioCategoryIcon(category), null, tint = scheme.onSecondaryContainer)
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(copy[SmartAudioText.SmartTitle], style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = scheme.onSecondaryContainer)
                val detail = when {
                    !settings.smartAudioProfilesEnabled -> copy[SmartAudioText.SmartOff]
                    activeProfile != null -> copy[SmartAudioText.SmartProfileActive].replace("{name}", activeProfile.deviceName)
                    else -> copy[SmartAudioText.SmartCount].replace("{n}", settings.smartAudioProfiles.size.toString())
                }
                Text(detail, style = MaterialTheme.typography.bodySmall, color = scheme.onSecondaryContainer.copy(alpha = 0.78f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SmartAudioProfilesScreen(
    settings: LevyraAudioSettings,
    output: LevyraAudioOutputState,
    animated: Boolean,
    catalog: AutoEqCatalogUiState,
    onEnabled: (Boolean) -> Unit,
    onSave: (SmartAudioProfile) -> Unit,
    onRemove: (String) -> Unit,
    onOpenCatalog: () -> Unit,
    onCatalogQuery: (String) -> Unit,
    onCatalogSelect: (AutoEqCatalogEntry) -> Unit,
    onCatalogSelectionConsumed: () -> Unit,
    onCatalogClose: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    val copy = smartAudioCopy(LocalLevyraStrings.current.code)
    val scheme = MaterialTheme.colorScheme
    val strings = LocalLevyraStrings.current
    val devices = remember(settings.smartAudioProfiles, output.active, output.connected) { smartAudioDevices(settings, output) }
    var editing by remember { mutableStateOf<SmartAudioDeviceUi?>(null) }
    Surface(color = scheme.surface, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, strings.close, tint = scheme.onSurface)
                    }
                    Text(copy[SmartAudioText.SmartTitle], style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = scheme.onSurface, modifier = Modifier.padding(start = 4.dp))
                }
                Text(copy[SmartAudioText.SmartSubtitle], style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp, top = 2.dp))
            }
            item(key = "automation") {
                Surface(color = scheme.surfaceContainer, shape = RoundedCornerShape(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(copy[SmartAudioText.SmartAutomatic], style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                            Text(copy[SmartAudioText.SmartAutomaticHint], style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        Switch(checked = settings.smartAudioProfilesEnabled, onCheckedChange = onEnabled)
                    }
                }
            }
            items(devices, key = SmartAudioDeviceUi::key) { device ->
                SmartAudioDeviceCard(
                    device = device,
                    copy = copy,
                    automation = settings.smartAudioProfilesEnabled,
                    animated = animated,
                    onClick = { editing = device },
                    modifier = if (animated) Modifier.animateItem() else Modifier
                )
            }
        }
    }
    editing?.let { device ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { editing = null }, sheetState = sheetState, containerColor = scheme.surfaceContainerLow) {
            SmartAudioProfileEditor(
                device = device,
                settings = settings,
                catalog = catalog,
                onOpenCatalog = onOpenCatalog,
                onCatalogSelectionConsumed = onCatalogSelectionConsumed,
                onSave = {
                    onSave(it)
                    editing = null
                },
                onReset = {
                    onRemove(device.key)
                    editing = null
                }
            )
        }
    }
    if (catalog.visible && catalog.selection == null) {
        AutoEqCatalogDialog(
            state = catalog,
            onQuery = onCatalogQuery,
            onSelect = onCatalogSelect,
            onRetry = onOpenCatalog,
            onDismiss = onCatalogClose
        )
    }
}

@Composable
private fun SmartAudioDeviceCard(
    device: SmartAudioDeviceUi,
    copy: SmartAudioCopy,
    automation: Boolean,
    animated: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val cornerSpec = if (animated) spring<androidx.compose.ui.unit.Dp>(Spring.DampingRatioLowBouncy, Spring.StiffnessLow) else snap()
    val corner by animateDpAsState(if (device.active) 38.dp else 22.dp, cornerSpec, label = "smart-device-corner")
    val container by animateColorAsState(
        targetValue = if (device.active) scheme.primaryContainer else scheme.surfaceContainerHigh,
        label = "smart-device-container"
    )
    val content = if (device.active) scheme.onPrimaryContainer else scheme.onSurface
    val muted = if (device.active) scheme.onPrimaryContainer.copy(alpha = 0.76f) else scheme.onSurfaceVariant
    Surface(color = container, shape = RoundedCornerShape(corner), modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(if (device.active) 20.dp else 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmartDeviceAvatar(device, content, muted, animated)
                SmartDeviceTitle(device, copy, content, muted, Modifier.weight(1f).padding(start = 12.dp))
            }
            Spacer(Modifier.height(10.dp))
            SmartDeviceProfile(device, copy, automation, content, animated)
        }
    }
}

@Composable
private fun SmartDeviceAvatar(device: SmartAudioDeviceUi, content: Color, muted: Color, animated: Boolean) {
    val pulse = remember(device.key) { Animatable(0f) }
    LaunchedEffect(device.active, animated) {
        if (device.active && animated) {
            pulse.snapTo(0f)
            pulse.animateTo(1f, tween(900))
        }
    }
    val outerSize = if (device.active) 58.dp else 46.dp
    val innerSize = if (device.active) 46.dp else 40.dp
    val innerAlpha = if (device.active) 0.14f else 0.08f
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(outerSize)) {
        if (device.active) {
            Canvas(Modifier.fillMaxSize()) {
                val progress = pulse.value
                if (progress in 0.001f..0.999f) {
                    drawCircle(content.copy(alpha = (1f - progress) * 0.5f), size.minDimension / 2f * (0.72f + progress * 0.28f), style = Stroke(2.dp.toPx()))
                }
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(innerSize).clip(CircleShape).background(content.copy(alpha = innerAlpha))
        ) {
            Icon(smartAudioCategoryIcon(device.category), null, tint = if (device.connected) content else muted)
        }
    }
}

@Composable
private fun SmartDeviceTitle(
    device: SmartAudioDeviceUi,
    copy: SmartAudioCopy,
    content: Color,
    muted: Color,
    modifier: Modifier
) {
    val status = when {
        device.active -> copy[SmartAudioText.SmartPlayingHere]
        device.connected -> copy[SmartAudioText.SmartConnected]
        else -> copy[SmartAudioText.SmartRemembered]
    }
    Column(modifier) {
        Text(
            device.name.ifBlank { copy[categoryLabelKey(device.category)] },
            style = if (device.active) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(status, style = MaterialTheme.typography.bodySmall, color = muted)
    }
}

@Composable
private fun SmartDeviceProfile(
    device: SmartAudioDeviceUi,
    copy: SmartAudioCopy,
    automation: Boolean,
    content: Color,
    animated: Boolean
) {
    val strings = LocalLevyraStrings.current
    val profile = device.profile
    AnimatedContent(
        targetState = profile?.let { profileSummary(it, copy, strings) } ?: copy[SmartAudioText.SmartNoProfile],
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
        label = "smart-profile-summary"
    ) { summary ->
        Text(
            if (automation) summary else "$summary · ${copy[SmartAudioText.SmartOff]}",
            style = MaterialTheme.typography.labelLarge,
            color = content
        )
    }
    if (profile != null && profile.eqMode != SmartAudioEqMode.KEEP_GLOBAL && profile.eqMode != SmartAudioEqMode.OFF) {
        Spacer(Modifier.height(10.dp))
        EqPreviewCurve(profileCurve(profile), content, animated, Modifier.fillMaxWidth().height(if (device.active) 54.dp else 34.dp))
    }
}

private fun profileSummary(
    profile: SmartAudioProfile,
    copy: SmartAudioCopy,
    strings: com.luc4n3x.levyra.ui.i18n.LevyraStrings
): String = when (profile.eqMode) {
    SmartAudioEqMode.KEEP_GLOBAL -> copy[SmartAudioText.EqKeepGlobal]
    SmartAudioEqMode.OFF -> copy[SmartAudioText.EqOff]
    SmartAudioEqMode.PRESET -> profile.presetLabel.ifBlank {
        strings.localizedAudioPresetLabel(profile.presetId, LevyraAudioPresets.preset(profile.presetId).fallbackLabel)
    }
    SmartAudioEqMode.PARAMETRIC -> profile.parametricProfile?.name ?: copy[SmartAudioText.EqAutoEq]
}

private fun profileCurve(profile: SmartAudioProfile): List<Float> = when (profile.eqMode) {
    SmartAudioEqMode.PARAMETRIC -> {
        val bands = profile.parametricProfile?.bands.orEmpty().sortedBy { it.frequencyHz }
        if (bands.isEmpty()) listOf(0f, 0f) else bands.map { it.gainDb / 12f }
    }
    else -> profile.bandLevels.map { it / 100f }
}

@Composable
private fun EqPreviewCurve(levels: List<Float>, color: Color, animated: Boolean, modifier: Modifier = Modifier) {
    val reveal = remember(levels) { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(levels, animated) {
        if (animated) reveal.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)) else reveal.snapTo(1f)
    }
    Canvas(modifier) {
        val middle = size.height / 2f
        drawLine(color.copy(alpha = 0.22f), Offset(0f, middle), Offset(size.width, middle), 1.dp.toPx())
        if (levels.size < 2) return@Canvas
        val step = size.width / (levels.size - 1)
        val path = Path()
        fun pointY(index: Int) = middle - levels[index].coerceIn(-1f, 1f) * middle * 0.9f * reveal.value
        path.moveTo(0f, pointY(0))
        for (index in 1 until levels.size) {
            val previousX = step * (index - 1)
            val x = step * index
            path.cubicTo(previousX + step / 2f, pointY(index - 1), x - step / 2f, pointY(index), x, pointY(index))
        }
        drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SmartAudioProfileEditor(
    device: SmartAudioDeviceUi,
    settings: LevyraAudioSettings,
    catalog: AutoEqCatalogUiState,
    onOpenCatalog: () -> Unit,
    onCatalogSelectionConsumed: () -> Unit,
    onSave: (SmartAudioProfile) -> Unit,
    onReset: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = smartAudioCopy(strings.code)
    val scheme = MaterialTheme.colorScheme
    var draft by remember(device.key) {
        mutableStateOf(device.profile ?: SmartAudioProfile(device.key, device.name, device.category))
    }
    val selection = catalog.selection
    LaunchedEffect(selection) {
        val picked = selection ?: return@LaunchedEffect
        draft = withContext(Dispatchers.Default) { applyCatalogProfile(draft, picked.name, picked.profileText) }
        onCatalogSelectionConsumed()
    }
    val presets = remember(settings.customPresets) { LevyraAudioPresets.presets + settings.customPresets }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(smartAudioCategoryIcon(draft.category), null, tint = scheme.primary, modifier = Modifier.size(30.dp))
            Text(
                device.name.ifBlank { copy[categoryLabelKey(draft.category)] },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        EditorSection(copy[SmartAudioText.SmartDeviceType]) {
            SmartAudioDeviceCategory.entries.forEach { category ->
                ExpressiveChoice(copy[categoryLabelKey(category)], draft.category == category, smartAudioCategoryIcon(category)) {
                    draft = draft.copy(category = category)
                }
            }
        }
        EditorSection(copy[SmartAudioText.SmartEqualizer]) {
            ExpressiveChoice(copy[SmartAudioText.EqKeepGlobal], draft.eqMode == SmartAudioEqMode.KEEP_GLOBAL) {
                draft = draft.copy(eqMode = SmartAudioEqMode.KEEP_GLOBAL)
            }
            ExpressiveChoice(copy[SmartAudioText.EqOff], draft.eqMode == SmartAudioEqMode.OFF) {
                draft = draft.copy(eqMode = SmartAudioEqMode.OFF)
            }
            ExpressiveChoice(copy[SmartAudioText.EqPreset], draft.eqMode == SmartAudioEqMode.PRESET, Icons.Rounded.GraphicEq) {
                draft = draft.copy(eqMode = SmartAudioEqMode.PRESET)
            }
            ExpressiveChoice(copy[SmartAudioText.EqAutoEq], draft.eqMode == SmartAudioEqMode.PARAMETRIC && draft.parametricProfile != null, Icons.Rounded.Headphones) {
                onOpenCatalog()
            }
        }
        if (draft.eqMode == SmartAudioEqMode.PRESET) {
            EditorSection(copy[SmartAudioText.EqPreset]) {
                presets.forEach { preset ->
                    ExpressiveChoice(
                        strings.localizedAudioPresetLabel(preset.id, preset.fallbackLabel),
                        draft.presetId == preset.id && draft.presetLabel.isBlank()
                    ) {
                        draft = draft.copy(
                            presetId = preset.id,
                            presetLabel = "",
                            bandLevels = preset.levels,
                            bassBoost = preset.bassBoost,
                            preampDb = preset.preampDb
                        )
                    }
                }
            }
            Column {
                Row {
                    Text(copy[SmartAudioText.SmartPreamp], style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(String.format(Locale.ROOT, "%+.1f dB", draft.preampDb), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = scheme.onSurface)
                }
                Slider(
                    value = draft.preampDb,
                    onValueChange = { draft = draft.copy(preampDb = (it * 2f).roundToInt() / 2f) },
                    valueRange = SmartAudioProfile.MIN_PREAMP_DB..SmartAudioProfile.MAX_PREAMP_DB
                )
                EqPreviewCurve(profileCurve(draft), scheme.primary, animated = false, modifier = Modifier.fillMaxWidth().height(56.dp))
            }
        }
        if (draft.eqMode == SmartAudioEqMode.PARAMETRIC) {
            draft.parametricProfile?.let { parametric ->
                Text(parametric.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                EqPreviewCurve(profileCurve(draft), scheme.primary, animated = false, modifier = Modifier.fillMaxWidth().height(56.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.heightIn(min = 48.dp)) {
            Text(copy[SmartAudioText.SmartReplayGainOverride], style = MaterialTheme.typography.titleSmall, color = scheme.onSurface, modifier = Modifier.weight(1f).padding(end = 12.dp))
            Switch(checked = draft.overrideReplayGain, onCheckedChange = { draft = draft.copy(overrideReplayGain = it) })
        }
        if (draft.overrideReplayGain) {
            EditorSection(null) {
                listOf(
                    ReplayGainMode.OFF to SmartAudioText.EqOff,
                    ReplayGainMode.TRACK to SmartAudioText.ReplayGainTrack,
                    ReplayGainMode.ALBUM to SmartAudioText.ReplayGainAlbum,
                    ReplayGainMode.SMART to SmartAudioText.ReplayGainSmart
                ).forEach { (mode, label) ->
                    ExpressiveChoice(copy[label], draft.replayGainMode == mode) { draft = draft.copy(replayGainMode = mode) }
                }
            }
        }
        EditorSection(copy[SmartAudioText.NightTitle]) {
            listOf(
                SmartAudioNightMode.KEEP_GLOBAL to SmartAudioText.EqKeepGlobal,
                SmartAudioNightMode.OFF to SmartAudioText.EqOff,
                SmartAudioNightMode.ON to SmartAudioText.SmartOn
            ).forEach { (mode, label) ->
                ExpressiveChoice(copy[label], draft.nightMode == mode) { draft = draft.copy(nightMode = mode) }
            }
        }
        if (draft.nightMode == SmartAudioNightMode.ON) {
            EditorSection(null) {
                NightListeningPreset.entries.forEach { preset ->
                    ExpressiveChoice(copy[nightPresetLabelKey(preset)], draft.nightPreset == preset) { draft = draft.copy(nightPreset = preset) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            if (device.profile != null) {
                TextButton(onClick = onReset, modifier = Modifier.heightIn(min = 48.dp)) { Text(copy[SmartAudioText.SmartReset]) }
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { onSave(draft) }, modifier = Modifier.heightIn(min = 48.dp)) { Text(copy[SmartAudioText.SmartSave]) }
        }
    }
}

internal fun applyCatalogProfile(draft: SmartAudioProfile, name: String, text: String): SmartAudioProfile {
    val parametric = AutoEqImporter.parseParametric(text, name)
    if (parametric is AutoEqImporter.ParametricParseResult.Success) {
        return draft.copy(eqMode = SmartAudioEqMode.PARAMETRIC, parametricProfile = parametric.profile.copy(name = name))
    }
    val graphic = AutoEqImporter.parse(text)
    if (graphic is AutoEqImporter.ParseResult.Success) {
        return draft.copy(
            eqMode = SmartAudioEqMode.PRESET,
            presetId = LevyraAudioPresets.FLAT,
            presetLabel = name,
            bandLevels = graphic.profile.bandLevels,
            bassBoost = 0,
            preampDb = graphic.profile.preampDb
        )
    }
    return draft
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorSection(title: String?, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun ExpressiveChoice(label: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val corner by animateDpAsState(if (selected) 12.dp else 24.dp, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), label = "smart-choice-corner")
    val container by animateColorAsState(if (selected) scheme.primary else scheme.surfaceContainerHighest, label = "smart-choice-container")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(corner))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val tint = if (selected) scheme.onPrimary else scheme.onSurface
        if (icon != null) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp).padding(end = 0.dp))
            Spacer(Modifier.size(8.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = tint)
    }
}
