package com.luc4n3x.levyra.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.feature.audio.LevyraAudioOutputRoute
import com.luc4n3x.levyra.feature.audio.LevyraAudioRouteSelectionState
import com.luc4n3x.levyra.feature.audio.openLevyraSystemOutputSwitcher
import com.luc4n3x.levyra.feature.audio.rememberLevyraAudioOutputState
import com.luc4n3x.levyra.player.PlaybackService
import com.luc4n3x.levyra.ui.components.LevyraGlassIntensity
import com.luc4n3x.levyra.ui.components.LevyraGlassSurface
import com.luc4n3x.levyra.ui.i18n.LevyraSystemPlayerCopy
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.systemPlayerCopy
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign

private val AudioRouteCardShape = RoundedCornerShape(20.dp)

@Composable
internal fun AudioRouteCenterSheet(
    surfaces: PlayerSurfaceTokens,
    animated: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val copy = LocalLevyraStrings.current.systemPlayerCopy()
    val output = rememberLevyraAudioOutputState()
    var expanded by rememberSaveable { mutableStateOf(false) }

    PlayerSheetFrame(
        surfaces = surfaces,
        animated = animated,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 28.dp)
        ) {
            Text(
                text = copy.outputTitle,
                color = surfaces.content,
                fontSize = 23.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = copy.outputSubtitle,
                color = surfaces.contentMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            output.active?.let { active ->
                AudioRouteSectionLabel(copy.currentOutput, surfaces)
                AudioRouteCard(
                    route = active,
                    selected = true,
                    enabled = true,
                    pending = false,
                    copy = copy,
                    surfaces = surfaces,
                    expanded = expanded,
                    animated = animated,
                    onClick = { expanded = !expanded }
                )
            }

            AnimatedVisibility(
                visible = expanded || output.active == null,
                enter = if (animated) expandVertically(LevyraPlayerDesign.smoothSpring()) else EnterTransition.None,
                exit = if (animated) shrinkVertically(LevyraPlayerDesign.smoothSpring()) else ExitTransition.None
            ) {
                Column(modifier = Modifier.padding(top = 18.dp).selectableGroup()) {
                    AudioRouteSectionLabel(copy.availableOutputs, surfaces)
                    val alternatives = output.connected.filterNot { it.routeKey == output.active?.routeKey }
                    if (alternatives.isEmpty()) {
                        Text(
                            text = copy.noOutputs,
                            color = surfaces.contentMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 10.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            alternatives.forEach { route ->
                                val pending = route.routeKey == output.requestedRouteKey &&
                                    output.selectionState == LevyraAudioRouteSelectionState.Applying
                                AudioRouteCard(
                                    route = route,
                                    selected = false,
                                    enabled = output.directSelectionAvailable &&
                                        output.selectionState != LevyraAudioRouteSelectionState.Applying,
                                    pending = pending,
                                    copy = copy,
                                    surfaces = surfaces,
                                    onClick = { PlaybackService.requestAudioOutput(route.routeKey) }
                                )
                            }
                        }
                    }
                }
            }

            PlayerOutputVolume(
                contentColor = surfaces.content,
                mutedColor = surfaces.contentMuted,
                activeColor = surfaces.activeContent,
                trackColor = surfaces.control,
                modifier = Modifier.padding(top = 18.dp)
            )

            when (output.selectionState) {
                LevyraAudioRouteSelectionState.Applying -> AudioRouteStatus(
                    copy.switchingOutput,
                    surfaces
                )
                LevyraAudioRouteSelectionState.Failed -> AudioRouteStatus(
                    copy.outputChangeFailed,
                    surfaces
                )
                LevyraAudioRouteSelectionState.Idle -> Unit
            }

            LevyraGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .defaultMinSize(minHeight = LevyraPlayerDesign.MinimumTouchTarget)
                    .clickable(role = Role.Button) { openLevyraSystemOutputSwitcher(context) },
                shape = AudioRouteCardShape,
                baseTint = surfaces.control,
                fallbackColor = surfaces.control,
                fallbackBorderColor = surfaces.outline,
                intensity = LevyraGlassIntensity.Subtle,
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = surfaces.activeContent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = copy.openSystemOutput,
                        color = surfaces.content,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = copy.systemManaged,
                color = surfaces.contentFaint,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 2.dp, top = 8.dp)
            )
        }
    }
}

@Composable
private fun AudioRouteSectionLabel(label: String, surfaces: PlayerSurfaceTokens) {
    Text(
        text = label,
        color = surfaces.activeContent,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.2.sp,
        modifier = Modifier
            .padding(start = 2.dp, bottom = 8.dp)
            .semantics { heading() }
    )
}

@Composable
private fun AudioRouteCard(
    route: LevyraAudioOutputRoute,
    selected: Boolean,
    enabled: Boolean,
    pending: Boolean,
    copy: LevyraSystemPlayerCopy,
    surfaces: PlayerSurfaceTokens,
    modifier: Modifier = Modifier,
    expanded: Boolean? = null,
    animated: Boolean = false,
    onClick: () -> Unit = {}
) {
    val category = routeCategory(route, copy)
    val cardTint = if (selected) surfaces.active else surfaces.controlQuiet
    LevyraGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 68.dp)
            .then(audioRouteSelectionModifier(selected, enabled, expanded, copy.chooseOutput, onClick)),
        shape = if (selected) LevyraPlayerDesign.ShapeLg else AudioRouteCardShape,
        baseTint = cardTint,
        fallbackColor = cardTint,
        fallbackBorderColor = if (selected) surfaces.activeContent.copy(alpha = 0.35f) else surfaces.outline,
        intensity = if (selected) LevyraGlassIntensity.Standard else LevyraGlassIntensity.Subtle,
        contentAlignment = Alignment.CenterStart
    ) {
        AudioRouteCardContent(
            route = route,
            category = category,
            details = routeDetails(route, category, copy),
            selected = selected,
            pending = pending,
            expanded = expanded,
            animated = animated,
            surfaces = surfaces
        )
    }
}

private fun audioRouteSelectionModifier(
    selected: Boolean,
    enabled: Boolean,
    expanded: Boolean?,
    onClickLabel: String,
    onClick: () -> Unit
): Modifier = if (expanded == null) {
    Modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
} else {
    Modifier.clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick)
        .semantics {
            if (expanded) collapse { onClick(); true } else expand { onClick(); true }
        }
}

@Composable
private fun AudioRouteCardContent(
    route: LevyraAudioOutputRoute,
    category: String,
    details: String,
    selected: Boolean,
    pending: Boolean,
    expanded: Boolean?,
    animated: Boolean,
    surfaces: PlayerSurfaceTokens
) {
    Row(
        modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = audioRouteIcon(route),
            contentDescription = null,
            tint = if (selected) surfaces.activeContent else surfaces.contentMuted,
            modifier = Modifier.size(22.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = route.displayName.ifBlank { category },
                color = surfaces.content,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = details,
                color = surfaces.contentMuted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        AudioRouteCardIndicator(pending, selected, surfaces)
        if (expanded != null) {
            AudioRouteExpansionIndicator(expanded, animated, surfaces)
        }
    }
}

private fun audioRouteIcon(route: LevyraAudioOutputRoute) = when {
    route.bluetooth || route.wired -> Icons.Rounded.Headphones
    route.external -> Icons.Rounded.Usb
    route.speaker -> Icons.Rounded.PhoneAndroid
    else -> Icons.AutoMirrored.Rounded.VolumeUp
}

@Composable
private fun AudioRouteCardIndicator(
    pending: Boolean,
    selected: Boolean,
    surfaces: PlayerSurfaceTokens
) {
    when {
        pending -> CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = surfaces.activeContent,
            strokeWidth = 2.dp
        )
        selected -> Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = surfaces.activeContent,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
private fun AudioRouteExpansionIndicator(
    expanded: Boolean,
    animated: Boolean,
    surfaces: PlayerSurfaceTokens
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = LevyraPlayerDesign.motion(animated, LevyraPlayerDesign.snappySpring()),
        label = "audioRouteExpansion"
    )
    Icon(
        imageVector = Icons.Rounded.ExpandMore,
        contentDescription = null,
        tint = surfaces.activeContent,
        modifier = Modifier.padding(start = 8.dp).size(24.dp)
            .graphicsLayer { rotationZ = chevronRotation }
    )
}

@Composable
private fun AudioRouteStatus(label: String, surfaces: PlayerSurfaceTokens) {
    Text(
        text = label,
        color = surfaces.contentMuted,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 2.dp, top = 12.dp)
    )
}

private fun routeDetails(
    route: LevyraAudioOutputRoute,
    category: String,
    copy: LevyraSystemPlayerCopy
): String = buildList {
    add(category)
    route.sampleRates.takeIf { it.isNotEmpty() }?.let { rates ->
        add("${copy.sampleRates}: ${rates.joinToString(" / ") { formatTechnicalSampleRate(it) }}")
    }
    route.channelCounts.maxOrNull()?.let { count ->
        add("${copy.channels}: ${formatTechnicalChannels(count)}")
    }
}.joinToString(" · ")

private fun routeCategory(route: LevyraAudioOutputRoute, copy: LevyraSystemPlayerCopy): String = when {
    route.bluetooth -> copy.bluetooth
    route.wired -> copy.wired
    route.external -> copy.external
    route.speaker -> copy.speaker
    else -> copy.systemManaged
}
