package com.luc4n3x.levyra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luc4n3x.levyra.data.parametricProfileFromJsonText
import com.luc4n3x.levyra.data.parametricProfileToJson
import com.luc4n3x.levyra.domain.ParametricBiquad
import com.luc4n3x.levyra.domain.ParametricEqBand
import com.luc4n3x.levyra.domain.ParametricEqProfile
import com.luc4n3x.levyra.domain.ParametricEqualizer
import com.luc4n3x.levyra.domain.ParametricFilterType
import com.luc4n3x.levyra.domain.ParametricProfiles
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.ParametricEqCopy
import com.luc4n3x.levyra.ui.i18n.ParametricProfileCopy
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import java.util.Locale
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

internal enum class EqualizerEditorMode { GRAPHIC, PARAMETRIC }

@Composable
internal fun EqualizerModeSelector(
    selected: EqualizerEditorMode,
    copy: ParametricEqCopy,
    onSelect: (EqualizerEditorMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            EqualizerEditorMode.GRAPHIC to copy.graphicEq,
            EqualizerEditorMode.PARAMETRIC to copy.parametricEq
        ).forEach { (mode, label) ->
            val active = selected == mode
            Surface(
                color = if (active) LevyraCyan.copy(alpha = 0.16f) else LevyraAdaptiveChip,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, if (active) LevyraCyan.copy(alpha = 0.6f) else LevyraAdaptiveHairline),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .selectable(selected = active, role = Role.RadioButton, onClick = { onSelect(mode) })
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
                    Text(
                        text = label,
                        color = if (active) LevyraCyan else LevyraText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

internal data class ParametricProfileActions(
    val onEnabled: (Boolean) -> Unit,
    val onActivateCustom: (String) -> Unit,
    val onActivateFlat: () -> Unit,
    val onSaveDraft: (ParametricEqProfile) -> Boolean,
    val onDuplicate: (ParametricEqProfile, String) -> ParametricEqProfile?,
    val onRename: (String, String) -> Boolean,
    val onDelete: (String) -> Unit,
    val onAudition: (ParametricEqProfile?) -> Unit
)

private sealed interface ParametricProfileDialog {
    data class Rename(val profile: ParametricEqProfile) : ParametricProfileDialog
    data class Delete(val profile: ParametricEqProfile) : ParametricProfileDialog
}

@Composable
internal fun ParametricEqualizerCard(
    enabled: Boolean,
    activeProfile: ParametricEqProfile?,
    customProfiles: List<ParametricEqProfile>,
    copy: ParametricEqCopy,
    profileCopy: ParametricProfileCopy,
    actions: ParametricProfileActions
) {
    var editing by rememberSaveable(stateSaver = EditorSessionSaver) { mutableStateOf<ParametricEditorSession?>(null) }
    var dialog by remember { mutableStateOf<ParametricProfileDialog?>(null) }
    val limitReached = customProfiles.size >= ParametricEqualizer.MAX_CUSTOM_PROFILES
    val externalActive = activeProfile?.takeIf {
        !ParametricProfiles.isCustom(it) && it.id != ParametricEqualizer.defaultProfile.id
    }
    val flatActive = activeProfile == null || activeProfile.id == ParametricEqualizer.defaultProfile.id

    fun duplicateAndEdit(source: ParametricEqProfile) {
        val base = source.name.ifBlank { profileCopy.flat }
        val name = ParametricProfiles.availableName(profileCopy.copyName(base), customProfiles) { number ->
            profileCopy.copyName(base, number)
        }
        actions.onDuplicate(source, name)?.let { created -> editing = ParametricEditorSession.of(created, isNew = false) }
    }

    Surface(
        color = LevyraAdaptiveCard,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(copy.parametricEq, color = LevyraText, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(copy.parametricSubtitle, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = actions.onEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = LevyraBlack,
                        checkedTrackColor = LevyraCyan,
                        uncheckedThumbColor = LevyraMuted,
                        uncheckedTrackColor = LevyraAdaptiveTrack
                    )
                )
            }

            ParametricResponseCurve(
                profile = activeProfile ?: ParametricEqualizer.defaultProfile,
                description = profileCopy.responseCurve,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .alpha(if (enabled) 1f else 0.45f)
            )

            ProfileSectionTitle(profileCopy.yourProfiles)
            if (customProfiles.isEmpty()) {
                Text(profileCopy.emptyCustom, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            customProfiles.forEach { profile ->
                ParametricProfileRow(
                    name = profile.name,
                    detail = "${copy.bands} · ${profile.bands.size}",
                    active = enabled && activeProfile?.id == profile.id,
                    activeLabel = profileCopy.active,
                    optionsLabel = profileCopy.profileOptions,
                    onClick = { actions.onActivateCustom(profile.id) },
                    menu = listOf(
                        ProfileMenuAction(profileCopy.edit, Icons.Rounded.Tune) {
                            editing = ParametricEditorSession.of(profile, isNew = false)
                        },
                        ProfileMenuAction(profileCopy.rename, Icons.Rounded.Edit) {
                            dialog = ParametricProfileDialog.Rename(profile)
                        },
                        ProfileMenuAction(profileCopy.duplicate, Icons.Rounded.ContentCopy, enabled = !limitReached) {
                            duplicateAndEdit(profile)
                        },
                        ProfileMenuAction(profileCopy.delete, Icons.Rounded.DeleteOutline, destructive = true) {
                            dialog = ParametricProfileDialog.Delete(profile)
                        }
                    )
                )
            }
            ParametricAction(
                label = if (limitReached) profileCopy.limitReached else profileCopy.newProfile,
                icon = Icons.Rounded.Add,
                enabled = !limitReached,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val name = ParametricProfiles.availableName(profileCopy.newProfile, customProfiles) { number ->
                        "${profileCopy.newProfile} $number"
                    }
                    editing = ParametricEditorSession.of(ParametricProfiles.create(name), isNew = true)
                }
            )

            if (externalActive != null) {
                ProfileSectionTitle(profileCopy.autoEq)
                ParametricProfileRow(
                    name = externalActive.name,
                    detail = "${copy.bands} · ${externalActive.bands.size}",
                    active = enabled,
                    activeLabel = profileCopy.active,
                    optionsLabel = profileCopy.profileOptions,
                    onClick = { actions.onEnabled(true) },
                    menu = listOf(
                        ProfileMenuAction(profileCopy.duplicate, Icons.Rounded.ContentCopy, enabled = !limitReached) {
                            duplicateAndEdit(externalActive)
                        }
                    )
                )
            }

            ProfileSectionTitle(profileCopy.builtIn)
            ParametricProfileRow(
                name = profileCopy.flat,
                detail = "${copy.bands} · ${ParametricEqualizer.defaultProfile.bands.size}",
                active = enabled && flatActive,
                activeLabel = profileCopy.active,
                optionsLabel = profileCopy.profileOptions,
                onClick = actions.onActivateFlat,
                menu = listOf(
                    ProfileMenuAction(profileCopy.duplicate, Icons.Rounded.ContentCopy, enabled = !limitReached) {
                        duplicateAndEdit(ParametricEqualizer.defaultProfile.copy(name = profileCopy.flat))
                    }
                )
            )
        }
    }

    editing?.let { session ->
        ParametricProfileEditor(
            session = session,
            existingProfiles = customProfiles,
            copy = copy,
            profileCopy = profileCopy,
            onSessionChange = { editing = it },
            onAudition = actions.onAudition,
            onSave = { profile -> if (actions.onSaveDraft(profile)) editing = null },
            onClose = { editing = null }
        )
    }

    when (val current = dialog) {
        is ParametricProfileDialog.Rename -> ParametricNameDialog(
            title = profileCopy.rename,
            initialName = current.profile.name,
            confirmLabel = profileCopy.save,
            profileCopy = profileCopy,
            nameTaken = { ParametricProfiles.nameTaken(it, current.profile.id, customProfiles) },
            onDismiss = { dialog = null },
            onConfirm = { name -> if (actions.onRename(current.profile.id, name)) dialog = null }
        )
        is ParametricProfileDialog.Delete -> ParametricConfirmDialog(
            title = profileCopy.deleteTitle(current.profile.name),
            message = profileCopy.deleteMessage,
            confirmLabel = profileCopy.delete,
            dismissLabel = LocalLevyraStrings.current.cancel,
            accent = LevyraPink,
            onDismiss = { dialog = null },
            onConfirm = {
                actions.onDelete(current.profile.id)
                dialog = null
            }
        )
        null -> Unit
    }
}

private data class ProfileMenuAction(
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val destructive: Boolean = false,
    val onClick: () -> Unit
)

@Composable
private fun ProfileSectionTitle(text: String) {
    Text(
        text = text,
        color = LevyraMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ParametricProfileRow(
    name: String,
    detail: String,
    active: Boolean,
    activeLabel: String,
    optionsLabel: String,
    onClick: () -> Unit,
    menu: List<ProfileMenuAction>
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        color = if (active) LevyraCyan.copy(alpha = 0.12f) else LevyraAdaptiveChip,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (active) LevyraCyan.copy(alpha = 0.5f) else LevyraAdaptiveHairline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = active, role = Role.RadioButton, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = name,
                    color = if (active) LevyraCyan else LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (active) "$activeLabel · $detail" else detail,
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (active) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
            }
            if (menu.isNotEmpty()) {
                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = optionsLabel, tint = LevyraMuted)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        menu.forEach { action ->
                            val tint = if (action.destructive) LevyraPink else LevyraText
                            DropdownMenuItem(
                                text = { Text(action.label, color = tint) },
                                leadingIcon = { Icon(action.icon, contentDescription = null, tint = tint) },
                                enabled = action.enabled,
                                onClick = {
                                    menuOpen = false
                                    action.onClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class EditorBand(val key: Long, val band: ParametricEqBand)

private data class ParametricEditorSession(
    val originalJson: String,
    val isNew: Boolean,
    val id: String,
    val name: String,
    val preampDb: Float,
    val bands: List<EditorBand>,
    val nextKey: Long
) {
    val profile: ParametricEqProfile
        get() = ParametricEqProfile(id = id, name = name, preampDb = preampDb, bands = bands.map { it.band })

    val dirty: Boolean
        get() = isNew || parametricProfileToJson(profile).toString() != originalJson

    fun withBand(key: Long, band: ParametricEqBand): ParametricEditorSession =
        copy(bands = bands.map { if (it.key == key) it.copy(band = band) else it })

    fun withoutBand(key: Long): ParametricEditorSession =
        if (bands.size <= 1) this else copy(bands = bands.filterNot { it.key == key })

    fun withNewBand(): ParametricEditorSession {
        if (bands.size >= ParametricEqualizer.MAX_BANDS) return this
        val frequency = nextBandFrequency(bands.map { it.band.frequencyHz })
        val band = ParametricEqBand(frequencyHz = frequency, gainDb = 0f, q = 1f, filterType = ParametricFilterType.PEAK)
        return copy(bands = bands + EditorBand(nextKey, band), nextKey = nextKey + 1)
    }

    companion object {
        fun of(profile: ParametricEqProfile, isNew: Boolean): ParametricEditorSession = ParametricEditorSession(
            originalJson = parametricProfileToJson(profile).toString(),
            isNew = isNew,
            id = profile.id,
            name = profile.name,
            preampDb = profile.preampDb,
            bands = profile.bands.mapIndexed { index, band -> EditorBand(index.toLong(), band) },
            nextKey = profile.bands.size.toLong()
        )
    }
}

private val EditorSessionSaver = Saver<ParametricEditorSession?, List<Any>>(
    save = { session ->
        if (session == null) {
            emptyList()
        } else {
            listOf(
                session.originalJson,
                session.isNew,
                session.name,
                parametricProfileToJson(session.profile.copy(name = session.name.ifBlank { EMPTY_NAME_PLACEHOLDER })).toString()
            )
        }
    },
    restore = { saved ->
        if (saved.size < 4) {
            null
        } else {
            parametricProfileFromJsonText(saved[3] as String)?.let { profile ->
                ParametricEditorSession.of(profile, isNew = saved[1] as Boolean)
                    .copy(originalJson = saved[0] as String, name = saved[2] as String)
            }
        }
    }
)

private fun nextBandFrequency(existing: List<Float>): Float {
    val anchors = listOf(ParametricProfiles.EDITOR_MIN_FREQUENCY_HZ) + existing.sorted() + ParametricProfiles.EDITOR_MAX_FREQUENCY_HZ
    val widest = anchors.zipWithNext().maxByOrNull { (low, high) -> log10(high) - log10(low) } ?: return 1_000f
    return roundFrequency(10f.pow((log10(widest.first) + log10(widest.second)) / 2f))
}

@Composable
private fun ParametricProfileEditor(
    session: ParametricEditorSession,
    existingProfiles: List<ParametricEqProfile>,
    copy: ParametricEqCopy,
    profileCopy: ParametricProfileCopy,
    onSessionChange: (ParametricEditorSession) -> Unit,
    onAudition: (ParametricEqProfile?) -> Unit,
    onSave: (ParametricEqProfile) -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val focusManager = LocalFocusManager.current
    var confirmDiscard by remember { mutableStateOf(false) }
    var saveRequested by remember { mutableStateOf(false) }
    val latestAudition by rememberUpdatedState(onAudition)
    val latestSession by rememberUpdatedState(session)
    val cleanName = ParametricProfiles.cleanName(session.name)
    val nameError = when {
        cleanName.isEmpty() -> profileCopy.nameRequired
        ParametricProfiles.nameTaken(cleanName, session.id, existingProfiles) -> profileCopy.nameTaken
        else -> null
    }
    val auditionProfile = remember(session.preampDb, session.bands) {
        session.profile.copy(name = session.id)
    }
    LaunchedEffect(auditionProfile) { latestAudition(auditionProfile) }
    val latestExisting by rememberUpdatedState(existingProfiles)
    val latestSave by rememberUpdatedState(onSave)
    LaunchedEffect(saveRequested) {
        if (!saveRequested) return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        saveRequested = false
        val pending = latestSession
        val name = ParametricProfiles.cleanName(pending.name)
        val valid = name.isNotEmpty() && !ParametricProfiles.nameTaken(name, pending.id, latestExisting)
        if (valid && pending.dirty) latestSave(pending.profile.copy(name = name))
    }
    DisposableEffect(Unit) { onDispose { latestAudition(null) } }
    val requestClose: () -> Unit = {
        focusManager.clearFocus()
        if (latestSession.dirty) confirmDiscard = true else onClose()
    }

    Dialog(
        onDismissRequest = requestClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        BackHandler(onBack = requestClose)
        Surface(color = LevyraPanel, modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = requestClose, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = strings.cancel, tint = LevyraText)
                    }
                    Text(
                        text = cleanName.ifEmpty { profileCopy.newProfile },
                        color = LevyraText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                    )
                    ParametricDialogButton(
                        label = profileCopy.save,
                        primary = true,
                        enabled = nameError == null && session.dirty,
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = {
                            focusManager.clearFocus()
                            saveRequested = true
                        }
                    )
                }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "name") {
                        OutlinedTextField(
                            value = session.name,
                            onValueChange = { onSessionChange(latestSession.copy(name = it.take(ParametricEqualizer.MAX_NAME_CHARS))) },
                            singleLine = true,
                            label = { Text(profileCopy.profileName) },
                            isError = nameError != null,
                            supportingText = nameError?.let { error -> { Text(error) } },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            colors = parametricFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item(key = "curve") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ParametricResponseCurve(
                                profile = auditionProfile,
                                description = profileCopy.responseCurve,
                                modifier = Modifier.fillMaxWidth().height(132.dp)
                            )
                            Text(profileCopy.previewing, color = LevyraMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    item(key = "preamp") {
                        ParametricValueEditor(
                            label = strings.preamp,
                            value = session.preampDb,
                            sliderRange = ParametricEqualizer.MIN_PREAMP_DB..ParametricEqualizer.MAX_PREAMP_DB,
                            format = ::decimalText,
                            unit = "dB",
                            rangeText = profileCopy.range(
                                dbLabel(ParametricEqualizer.MIN_PREAMP_DB),
                                dbLabel(ParametricEqualizer.MAX_PREAMP_DB)
                            ),
                            invalidText = profileCopy.invalidNumber,
                            isValid = ParametricProfiles::validPreamp,
                            snap = { (it * 2f).roundToInt() / 2f },
                            onValue = { onSessionChange(latestSession.copy(preampDb = it)) }
                        )
                    }
                    item(key = "bands-header") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${copy.bands} · ${session.bands.size}/${ParametricEqualizer.MAX_BANDS}",
                                color = LevyraText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onSessionChange(latestSession.withNewBand()) },
                                enabled = session.bands.size < ParametricEqualizer.MAX_BANDS,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = copy.addBand, tint = LevyraCyan)
                            }
                        }
                    }
                    items(session.bands, key = { it.key }) { entry ->
                        ParametricBandEditor(
                            number = session.bands.indexOf(entry) + 1,
                            band = entry.band,
                            canRemove = session.bands.size > 1,
                            copy = copy,
                            profileCopy = profileCopy,
                            onBand = { updated -> onSessionChange(latestSession.withBand(entry.key, updated)) },
                            onRemove = { onSessionChange(latestSession.withoutBand(entry.key)) }
                        )
                    }
                }
            }
        }
        if (confirmDiscard) {
            ParametricConfirmDialog(
                title = profileCopy.discardTitle,
                message = profileCopy.discardMessage,
                confirmLabel = profileCopy.discard,
                dismissLabel = profileCopy.keepEditing,
                accent = LevyraPink,
                onDismiss = { confirmDiscard = false },
                onConfirm = {
                    confirmDiscard = false
                    onClose()
                }
            )
        }
    }
}

@Composable
private fun ParametricBandEditor(
    number: Int,
    band: ParametricEqBand,
    canRemove: Boolean,
    copy: ParametricEqCopy,
    profileCopy: ParametricProfileCopy,
    onBand: (ParametricEqBand) -> Unit,
    onRemove: () -> Unit
) {
    val latestBand by rememberUpdatedState(band)
    Surface(
        color = LevyraAdaptiveChip,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profileCopy.band(number),
                    color = if (band.enabled) LevyraText else LevyraMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = band.enabled,
                    onCheckedChange = { onBand(latestBand.copy(enabled = it)) },
                    colors = SwitchDefaults.colors(checkedTrackColor = LevyraCyan),
                    modifier = Modifier.semantics { contentDescription = profileCopy.band(number) }
                )
                IconButton(onClick = onRemove, enabled = canRemove, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = copy.removeBand, tint = LevyraMuted)
                }
            }
            Column(
                modifier = Modifier.alpha(if (band.enabled) 1f else 0.5f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ParametricFilterType.entries.forEach { type ->
                        ParametricChip(
                            label = filterLabel(type, copy),
                            selected = band.filterType == type,
                            modifier = Modifier.weight(1f),
                            onClick = { onBand(latestBand.copy(filterType = type)) }
                        )
                    }
                }
                ParametricValueEditor(
                    label = copy.frequency,
                    value = band.frequencyHz,
                    sliderRange = 0f..1f,
                    toSlider = ::frequencyPosition,
                    fromSlider = ::frequencyAt,
                    format = { it.roundToInt().toString() },
                    unit = "Hz",
                    rangeText = profileCopy.range(
                        frequencyLabel(ParametricProfiles.EDITOR_MIN_FREQUENCY_HZ),
                        frequencyLabel(ParametricProfiles.EDITOR_MAX_FREQUENCY_HZ)
                    ),
                    invalidText = profileCopy.invalidNumber,
                    isValid = ParametricProfiles::validFrequency,
                    snap = ::roundFrequency,
                    onValue = { onBand(latestBand.copy(frequencyHz = it)) }
                )
                ParametricValueEditor(
                    label = copy.gain,
                    value = band.gainDb,
                    sliderRange = -ParametricEqualizer.MAX_GAIN_DB..ParametricEqualizer.MAX_GAIN_DB,
                    format = ::decimalText,
                    unit = "dB",
                    rangeText = profileCopy.range(
                        dbLabel(-ParametricEqualizer.MAX_GAIN_DB),
                        dbLabel(ParametricEqualizer.MAX_GAIN_DB)
                    ),
                    invalidText = profileCopy.invalidNumber,
                    isValid = ParametricProfiles::validGain,
                    snap = { (it * 10f).roundToInt() / 10f },
                    onValue = { onBand(latestBand.copy(gainDb = it)) }
                )
                ParametricValueEditor(
                    label = copy.qFactor,
                    value = band.q,
                    sliderRange = 0f..1f,
                    toSlider = ::qPosition,
                    fromSlider = ::qAt,
                    format = ::decimalText,
                    unit = "",
                    rangeText = profileCopy.range(decimalText(ParametricEqualizer.MIN_Q), decimalText(ParametricEqualizer.MAX_Q)),
                    invalidText = profileCopy.invalidNumber,
                    isValid = ParametricProfiles::validQ,
                    snap = { (it * 100f).roundToInt() / 100f },
                    onValue = { onBand(latestBand.copy(q = it)) }
                )
            }
        }
    }
}

@Composable
private fun ParametricValueEditor(
    label: String,
    value: Float,
    sliderRange: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    unit: String,
    rangeText: String,
    invalidText: String,
    isValid: (Float) -> Boolean,
    snap: (Float) -> Float,
    onValue: (Float) -> Unit,
    toSlider: (Float) -> Float = { it },
    fromSlider: (Float) -> Float = { it }
) {
    val formatted = format(value)
    var text by remember(formatted) { mutableStateOf(formatted) }
    var error by remember(formatted) { mutableStateOf<String?>(null) }
    val commit: () -> Unit = {
        val parsed = ParametricProfiles.parseDecimal(text)
        when {
            parsed == null -> error = invalidText
            !isValid(parsed) -> error = rangeText
            else -> {
                error = null
                if (format(parsed) != formatted) onValue(parsed) else text = formatted
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = LevyraMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    text = input.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }.take(MAX_NUMBER_CHARS)
                    error = null
                },
                singleLine = true,
                isError = error != null,
                suffix = unitSuffix(unit),
                textStyle = TextStyle(color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                colors = parametricFieldColors(),
                modifier = Modifier
                    .width(128.dp)
                    .semantics { contentDescription = label }
                    .onFocusChanged { state -> if (!state.isFocused && text != formatted) commit() }
            )
        }
        error?.let { Text(it, color = LevyraPink, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
        Slider(
            value = toSlider(value).coerceIn(sliderRange.start, sliderRange.endInclusive),
            onValueChange = { position ->
                val next = snap(fromSlider(position))
                if (isValid(next)) onValue(next)
            },
            valueRange = sliderRange,
            colors = SliderDefaults.colors(
                thumbColor = LevyraCyan,
                activeTrackColor = LevyraCyan,
                inactiveTrackColor = LevyraAdaptiveTrack
            )
        )
    }
}

private fun unitSuffix(unit: String): (@Composable () -> Unit)? =
    if (unit.isEmpty()) null else { { Text(unit, fontSize = 12.sp, color = LevyraMuted) } }

@Composable
private fun parametricFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LevyraText,
    unfocusedTextColor = LevyraText,
    focusedBorderColor = LevyraCyan,
    unfocusedBorderColor = LevyraAdaptiveHairline,
    cursorColor = LevyraCyan,
    focusedLabelColor = LevyraCyan,
    unfocusedLabelColor = LevyraMuted
)

@Composable
private fun ParametricResponseCurve(
    profile: ParametricEqProfile,
    description: String,
    modifier: Modifier = Modifier
) {
    val response = remember(profile.preampDb, profile.bands) {
        FloatArray(ResponseFrequencies.size).also { out ->
            ParametricBiquad.responseDb(profile, ResponseFrequencies, RESPONSE_SAMPLE_RATE, out)
        }
    }
    val markers = remember(profile.bands) { profile.bands.filter { it.enabled } }
    val fillBrush = remember {
        Brush.verticalGradient(
            listOf(LevyraCyan.copy(alpha = 0.22f), LevyraCyan.copy(alpha = 0.02f), LevyraCyan.copy(alpha = 0.22f))
        )
    }
    val gridColor = LevyraAdaptiveHairline
    Canvas(
        modifier = modifier
            .background(LevyraAdaptiveChip, RoundedCornerShape(14.dp))
            .semantics { contentDescription = description }
    ) {
        val limit = (response.maxOfOrNull { abs(it) } ?: 0f).coerceIn(RESPONSE_MIN_SPAN_DB, ParametricEqualizer.MAX_GAIN_DB)
        val inset = 10.dp.toPx()
        val middle = size.height / 2f
        fun yFor(db: Float): Float = middle - (db / limit).coerceIn(-1f, 1f) * (middle - inset)
        fun xFor(frequency: Float): Float = frequencyPosition(frequency) * size.width
        ResponseGridFrequencies.forEach { frequency ->
            val x = xFor(frequency)
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
        }
        drawLine(LevyraMuted.copy(alpha = 0.4f), Offset(0f, middle), Offset(size.width, middle), strokeWidth = 1.dp.toPx())
        val line = Path()
        val fill = Path()
        response.forEachIndexed { index, db ->
            val x = xFor(ResponseFrequencies[index])
            val y = yFor(db)
            if (index == 0) {
                line.moveTo(x, y)
                fill.moveTo(x, middle)
                fill.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(size.width, middle)
        fill.close()
        drawPath(fill, fillBrush)
        drawPath(line, LevyraCyan, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        markers.forEach { band ->
            val center = Offset(xFor(band.frequencyHz), yFor(band.gainDb + profile.preampDb))
            drawCircle(LevyraBlack, radius = 4.5.dp.toPx(), center = center)
            drawCircle(LevyraCyan, radius = 3.dp.toPx(), center = center)
        }
    }
}

@Composable
private fun ParametricChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) LevyraCyan.copy(alpha = 0.16f) else LevyraAdaptiveChip,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (selected) LevyraCyan.copy(alpha = 0.55f) else LevyraAdaptiveHairline),
        modifier = modifier
            .heightIn(min = 44.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(
                label,
                color = if (selected) LevyraCyan else LevyraText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ParametricAction(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = LevyraAdaptiveChip,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LevyraAdaptiveHairline),
        modifier = modifier
            .heightIn(min = 48.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
            Text(label, color = LevyraText, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 7.dp))
        }
    }
}

@Composable
private fun ParametricNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    profileCopy: ParametricProfileCopy,
    nameTaken: (String) -> Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by rememberSaveable(initialName) { mutableStateOf(initialName.take(ParametricEqualizer.MAX_NAME_CHARS)) }
    val strings = LocalLevyraStrings.current
    val clean = ParametricProfiles.cleanName(name)
    val error = when {
        clean.isEmpty() -> profileCopy.nameRequired
        nameTaken(clean) -> profileCopy.nameTaken
        else -> null
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = LevyraPanel,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, LevyraAdaptiveHairline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(title, color = LevyraText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(ParametricEqualizer.MAX_NAME_CHARS) },
                    singleLine = true,
                    label = { Text(profileCopy.profileName) },
                    isError = error != null,
                    supportingText = error?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { if (error == null) onConfirm(clean) }),
                    colors = parametricFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ParametricDialogButton(strings.cancel, primary = false, enabled = true, modifier = Modifier.weight(1f), onClick = onDismiss)
                    ParametricDialogButton(
                        label = confirmLabel,
                        primary = true,
                        enabled = error == null,
                        modifier = Modifier.weight(1f),
                        onClick = { onConfirm(clean) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ParametricConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    accent: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = LevyraPanel,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, LevyraAdaptiveHairline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, color = LevyraText, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(message, color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ParametricDialogButton(dismissLabel, primary = false, enabled = true, modifier = Modifier.weight(1f), onClick = onDismiss)
                    ParametricDialogButton(
                        label = confirmLabel,
                        primary = true,
                        enabled = true,
                        modifier = Modifier.weight(1f),
                        accent = accent,
                        onClick = onConfirm
                    )
                }
            }
        }
    }
}

@Composable
private fun ParametricDialogButton(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    accent: Color = LevyraCyan,
    onClick: () -> Unit
) {
    Surface(
        color = if (primary) accent else LevyraAdaptiveChip,
        shape = RoundedCornerShape(14.dp),
        border = if (primary) null else BorderStroke(1.dp, LevyraAdaptiveHairline),
        modifier = modifier
            .heightIn(min = 44.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Text(label, color = if (primary) LevyraBlack else LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

private fun filterLabel(type: ParametricFilterType, copy: ParametricEqCopy): String = when (type) {
    ParametricFilterType.PEAK -> copy.peak
    ParametricFilterType.LOW_SHELF -> copy.lowShelf
    ParametricFilterType.HIGH_SHELF -> copy.highShelf
}

private fun frequencyPosition(frequencyHz: Float): Float = logPosition(
    frequencyHz,
    ParametricProfiles.EDITOR_MIN_FREQUENCY_HZ,
    ParametricProfiles.EDITOR_MAX_FREQUENCY_HZ
)

private fun frequencyAt(position: Float): Float =
    logValue(position, ParametricProfiles.EDITOR_MIN_FREQUENCY_HZ, ParametricProfiles.EDITOR_MAX_FREQUENCY_HZ)

private fun qPosition(q: Float): Float = logPosition(q, ParametricEqualizer.MIN_Q, ParametricEqualizer.MAX_Q)

private fun qAt(position: Float): Float = logValue(position, ParametricEqualizer.MIN_Q, ParametricEqualizer.MAX_Q)

private fun logPosition(value: Float, min: Float, max: Float): Float {
    val bounded = value.coerceIn(min, max)
    return ((log10(bounded) - log10(min)) / (log10(max) - log10(min))).coerceIn(0f, 1f)
}

private fun logValue(position: Float, min: Float, max: Float): Float {
    val lower = log10(min)
    val upper = log10(max)
    return 10f.pow(lower + (upper - lower) * position.coerceIn(0f, 1f)).coerceIn(min, max)
}

private fun roundFrequency(raw: Float): Float {
    val rounded = when {
        raw < 100f -> raw.roundToInt().toFloat()
        raw < 1_000f -> (raw / 5f).roundToInt() * 5f
        else -> (raw / 10f).roundToInt() * 10f
    }
    return rounded.coerceIn(ParametricProfiles.EDITOR_MIN_FREQUENCY_HZ, ParametricProfiles.EDITOR_MAX_FREQUENCY_HZ)
}

private fun frequencyLabel(value: Float): String = if (value >= 1_000f) {
    val kilohertz = value / 1_000f
    val pattern = if (kilohertz >= 10f || kilohertz % 1f == 0f) "%.0f kHz" else "%.1f kHz"
    String.format(Locale.US, pattern, kilohertz)
} else {
    "${value.roundToInt()} Hz"
}

private fun decimalText(value: Float): String {
    val rounded = (value * 100f).roundToInt() / 100f
    if (rounded % 1f == 0f) return rounded.roundToInt().toString()
    return String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
}

private fun dbLabel(value: Float): String = String.format(Locale.US, "%+.0f dB", value)

private const val RESPONSE_SAMPLE_RATE = 48_000
private const val RESPONSE_POINTS = 96
private const val RESPONSE_MIN_SPAN_DB = 6f
private const val MAX_NUMBER_CHARS = 8
private const val EMPTY_NAME_PLACEHOLDER = "-"

private val ResponseFrequencies: FloatArray = FloatArray(RESPONSE_POINTS) { index ->
    frequencyAt(index / (RESPONSE_POINTS - 1f))
}

private val ResponseGridFrequencies = floatArrayOf(100f, 1_000f, 10_000f)
