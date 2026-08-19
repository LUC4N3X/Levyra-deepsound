package com.luc4n3x.levyra.ui.update

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.AppUpdateInfo
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.update.formatUpdateDuration
import com.luc4n3x.levyra.update.formatUpdateTransferLine
import com.luc4n3x.levyra.update.updateProgressPercent

sealed interface LevyraUpdatePhase {
    data object Idle : LevyraUpdatePhase

    data class Available(val update: AppUpdateInfo) : LevyraUpdatePhase

    data class Preparing(val versionName: String) : LevyraUpdatePhase

    data class Downloading(
        val versionName: String,
        val downloadedBytes: Long,
        val totalBytes: Long?,
        val bytesPerSecond: Double?,
        val elapsedMs: Long
    ) : LevyraUpdatePhase

    data class Ready(val versionName: String) : LevyraUpdatePhase

    data class Installing(val versionName: String) : LevyraUpdatePhase

    data class PermissionRequired(val versionName: String) : LevyraUpdatePhase

    data class Failed(val versionName: String) : LevyraUpdatePhase
}

@Composable
fun LevyraUpdateBanner(
    phase: LevyraUpdatePhase,
    strings: LevyraStrings,
    onUpdate: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (phase is LevyraUpdatePhase.Idle) return
    var notesExpanded by rememberSaveable(phaseIdentity(phase)) { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(BannerShape)
            .background(LevyraPanel)
            .border(width = 1.dp, color = LevyraGlassBorder, shape = BannerShape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet)))
        )
        Column(modifier = Modifier.padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
            BannerHeader(
                title = phaseTitle(phase, strings),
                version = phaseVersion(phase),
                dismissLabel = strings.close,
                onDismiss = onDismiss.takeIf { isDismissible(phase) }
            )
            BannerBody(phase = phase, strings = strings, notesExpanded = notesExpanded)
            BannerActions(
                phase = phase,
                strings = strings,
                notesExpanded = notesExpanded,
                onToggleNotes = { notesExpanded = !notesExpanded },
                onUpdate = onUpdate,
                onCancel = onCancel,
                onRetry = onRetry
            )
        }
    }
}

@Composable
private fun BannerHeader(
    title: String,
    version: String?,
    dismissLabel: String,
    onDismiss: (() -> Unit)?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = LevyraText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!version.isNullOrBlank()) {
                Text(
                    text = version,
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onDismiss != null) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = dismissLabel,
                    tint = LevyraMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(10.dp))
        }
    }
}

@Composable
private fun BannerBody(
    phase: LevyraUpdatePhase,
    strings: LevyraStrings,
    notesExpanded: Boolean
) {
    when (phase) {
        is LevyraUpdatePhase.Available -> {
            val notes = remember(phase.update.releaseNotes, phase.update.latestVersionName) {
                levyraUpdateNoteLines(phase.update.releaseNotes, phase.update.latestVersionName)
                    .joinToString(separator = "\n")
            }
            if (notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = notes,
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    maxLines = if (notesExpanded) Int.MAX_VALUE else 2,
                    overflow = if (notesExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
                    modifier = if (notesExpanded) {
                        Modifier
                            .heightIn(max = 176.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(end = 10.dp)
                    } else {
                        Modifier.padding(end = 10.dp)
                    }
                )
            }
        }

        is LevyraUpdatePhase.Downloading -> {
            val percent = updateProgressPercent(phase.downloadedBytes, phase.totalBytes)
            Spacer(modifier = Modifier.height(10.dp))
            if (percent != null) DeterminateTrack(fraction = percent / 100f) else IndeterminateTrack()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatUpdateTransferLine(
                        downloadedBytes = phase.downloadedBytes,
                        totalBytes = phase.totalBytes,
                        bytesPerSecond = phase.bytesPerSecond
                    ),
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                val trailing = percent?.let { "$it%" } ?: formatUpdateDuration(phase.elapsedMs)
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = trailing,
                        color = LevyraText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }

        is LevyraUpdatePhase.PermissionRequired -> {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = strings.updateAllowInstalls,
                color = LevyraMuted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(end = 10.dp)
            )
        }

        is LevyraUpdatePhase.Preparing,
        is LevyraUpdatePhase.Installing,
        is LevyraUpdatePhase.Ready -> {
            Spacer(modifier = Modifier.height(10.dp))
            IndeterminateTrack()
        }

        is LevyraUpdatePhase.Failed,
        LevyraUpdatePhase.Idle -> Unit
    }
}

@Composable
private fun BannerActions(
    phase: LevyraUpdatePhase,
    strings: LevyraStrings,
    notesExpanded: Boolean,
    onToggleNotes: () -> Unit,
    onUpdate: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 6.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (phase) {
            is LevyraUpdatePhase.Available -> {
                if (phase.update.releaseNotes.isNotBlank()) {
                    QuietAction(
                        label = if (notesExpanded) strings.close else strings.whatsNew,
                        onClick = onToggleNotes
                    )
                }
                PrimaryAction(label = strings.update, onClick = onUpdate)
            }

            is LevyraUpdatePhase.Preparing,
            is LevyraUpdatePhase.Downloading -> QuietAction(label = strings.cancel, onClick = onCancel)

            is LevyraUpdatePhase.PermissionRequired -> PrimaryAction(label = strings.update, onClick = onRetry)

            is LevyraUpdatePhase.Failed -> PrimaryAction(label = strings.updateRetry, onClick = onRetry)

            is LevyraUpdatePhase.Ready,
            is LevyraUpdatePhase.Installing,
            LevyraUpdatePhase.Idle -> Unit
        }
    }
}

@Composable
private fun QuietAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text = label, color = LevyraMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(text = label, color = LevyraCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DeterminateTrack(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 10.dp)
            .height(4.dp)
            .clip(TrackShape)
            .background(LevyraGlassBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(TrackShape)
                .background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet)))
        )
    }
}

@Composable
private fun IndeterminateTrack() {
    val transition = rememberInfiniteTransition(label = "levyra-update-track")
    val offset by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "levyra-update-sweep"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 10.dp)
            .height(4.dp)
            .clip(TrackShape)
            .background(LevyraGlassBorder)
    ) {
        Layout(
            content = {
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .clip(TrackShape)
                        .background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet)))
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { measurables, constraints ->
            val trackWidth = constraints.maxWidth
            val segmentWidth = (trackWidth * 0.35f).toInt().coerceAtLeast(1)
            val placeable = measurables.first().measure(
                constraints.copy(minWidth = segmentWidth, maxWidth = segmentWidth)
            )
            layout(trackWidth, placeable.height) {
                placeable.placeRelative(x = (trackWidth * offset).toInt(), y = 0)
            }
        }
    }
}

internal fun levyraUpdateNoteLines(notes: String, version: String): List<String> {
    val versionKey = version.trim().lowercase()
    return notes
        .lineSequence()
        .map { it.trim() }
        .map { raw ->
            raw
                .replace(Regex("^#{1,6}\\s*"), "")
                .replace(Regex("^[-*•]+\\s*"), "")
                .replace(Regex("\\*\\*([^*]+)\\*\\*")) { match -> match.groupValues[1] }
                .replace(Regex("`([^`]+)`")) { match -> match.groupValues[1] }
                .replace(Regex("\\[([^\\]]+)]\\(([^)]+)\\)")) { match -> match.groupValues[1] }
                .replace("__", "")
                .replace("**", "")
                .trim()
        }
        .filter { line -> line.isNotBlank() && line != "---" && line.any { it.isLetterOrDigit() } }
        .filterNot { line ->
            val lower = line.lowercase()
            lower == "novità" ||
                lower == "changelog" ||
                lower.startsWith("levyra v$versionKey") ||
                lower.startsWith("levyra $versionKey") ||
                lower.startsWith("versione $versionKey")
        }
        .distinct()
        .take(12)
        .toList()
}

private val BannerShape = RoundedCornerShape(20.dp)
private val TrackShape = RoundedCornerShape(2.dp)

private fun phaseIdentity(phase: LevyraUpdatePhase): String = when (phase) {
    LevyraUpdatePhase.Idle -> "idle"
    is LevyraUpdatePhase.Available -> "available:${phase.update.latestVersionName}"
    is LevyraUpdatePhase.Preparing -> "preparing:${phase.versionName}"
    is LevyraUpdatePhase.Downloading -> "downloading:${phase.versionName}"
    is LevyraUpdatePhase.Ready -> "ready:${phase.versionName}"
    is LevyraUpdatePhase.Installing -> "installing:${phase.versionName}"
    is LevyraUpdatePhase.PermissionRequired -> "permission:${phase.versionName}"
    is LevyraUpdatePhase.Failed -> "failed:${phase.versionName}"
}

private fun phaseTitle(phase: LevyraUpdatePhase, strings: LevyraStrings): String = when (phase) {
    LevyraUpdatePhase.Idle -> ""
    is LevyraUpdatePhase.Available -> strings.newUpdate
    is LevyraUpdatePhase.Preparing -> strings.updatePreparing
    is LevyraUpdatePhase.Downloading -> strings.updateDownloading
    is LevyraUpdatePhase.Ready -> strings.updateReadyToInstall
    is LevyraUpdatePhase.Installing -> strings.updateInstalling
    is LevyraUpdatePhase.PermissionRequired -> strings.updateReadyToInstall
    is LevyraUpdatePhase.Failed -> strings.updateFailed
}

private fun phaseVersion(phase: LevyraUpdatePhase): String? = when (phase) {
    LevyraUpdatePhase.Idle -> null
    is LevyraUpdatePhase.Available -> phase.update.latestVersionName
    is LevyraUpdatePhase.Preparing -> phase.versionName
    is LevyraUpdatePhase.Downloading -> phase.versionName
    is LevyraUpdatePhase.Ready -> phase.versionName
    is LevyraUpdatePhase.Installing -> phase.versionName
    is LevyraUpdatePhase.PermissionRequired -> phase.versionName
    is LevyraUpdatePhase.Failed -> phase.versionName
}?.takeIf { it.isNotBlank() }

private fun isDismissible(phase: LevyraUpdatePhase): Boolean = when (phase) {
    is LevyraUpdatePhase.Available,
    is LevyraUpdatePhase.Failed,
    is LevyraUpdatePhase.PermissionRequired -> true
    else -> false
}
