package com.luc4n3x.levyra.ui.jam

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.feature.jam.JamConnectionState
import com.luc4n3x.levyra.feature.jam.JamFailure
import com.luc4n3x.levyra.feature.jam.JamGuestPermission
import com.luc4n3x.levyra.feature.jam.JamParticipant
import com.luc4n3x.levyra.feature.jam.JamPendingParticipant
import com.luc4n3x.levyra.feature.jam.JamSessionState
import com.luc4n3x.levyra.feature.jam.JamTrack
import com.luc4n3x.levyra.feature.jam.JamUiState
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraPanelSoft
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm

private val JamPermissionOptions = listOf(
    JamGuestPermission.HostOnly,
    JamGuestPermission.AddSongs,
    JamGuestPermission.Collaborative
)

private val JamHairline: Color get() = LevyraMuted.copy(alpha = 0.16f)

@Composable
internal fun LevyraJamOverlay(
    jam: JamUiState,
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    onCreate: (JamGuestPermission, Boolean) -> Unit,
    onJoin: (String) -> Unit,
    onLeave: () -> Unit,
    onEnd: () -> Unit,
    onPermissionChange: (JamGuestPermission) -> Unit,
    onApprovalRequiredChange: (Boolean) -> Unit,
    onLockedChange: (Boolean) -> Unit,
    onApproveParticipant: (String) -> Unit,
    onRejectParticipant: (String) -> Unit,
    onRemoveParticipant: (String, Boolean) -> Unit,
    onClearBans: () -> Unit,
    onShare: (String) -> Unit,
    onDismissFailure: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    var joinCode by rememberSaveable { mutableStateOf("") }
    var pendingPermissionId by rememberSaveable { mutableStateOf(JamGuestPermission.AddSongs.id) }
    var pendingApproval by rememberSaveable { mutableStateOf(true) }
    val selectedPermission = JamGuestPermission.fromId(pendingPermissionId)
    val session = jam.session
    val participants = session?.participants.orEmpty()
    val contributions = remember(session?.queue, participants) {
        jamContributions(session?.queue.orEmpty(), participants)
    }

    Box(modifier = Modifier.fillMaxSize().background(LevyraInk)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(
                start = LevyraPlayerDesign.GutterCompact,
                end = LevyraPlayerDesign.GutterCompact,
                top = LevyraPlayerDesign.SpaceMd,
                bottom = 140.dp
            ),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
        ) {
            item(contentType = "jam-header") {
                JamHeader(title = strings.jamTitle, subtitle = strings.jamSubtitle, onClose = onClose)
            }

            jam.failure?.let { failure ->
                item(contentType = "jam-failure") {
                    JamFailureBanner(
                        message = jamFailureText(failure, strings),
                        dismissLabel = strings.close,
                        onDismiss = onDismissFailure
                    )
                }
            }

            if (!jam.isActive) {
                item(contentType = "jam-identity") {
                    JamCard {
                        JamCardTitle(strings.jamDisplayName)
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = onDisplayNameChange,
                            singleLine = true,
                            label = { Text(strings.jamDisplayName) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item(contentType = "jam-create") {
                    JamCard {
                        JamCardTitle(strings.jamCreate)
                        JamPermissionPicker(
                            selected = selectedPermission,
                            strings = strings,
                            onSelect = { pendingPermissionId = it.id }
                        )
                        JamSwitchRow(
                            title = strings.jamApprovalRequired,
                            subtitle = strings.jamApprovalRequiredSubtitle,
                            checked = pendingApproval,
                            onCheckedChange = { pendingApproval = it }
                        )
                        JamPrimaryButton(
                            label = strings.jamCreate,
                            onClick = { onCreate(selectedPermission, pendingApproval) }
                        )
                    }
                }
                item(contentType = "jam-join") {
                    JamCard {
                        JamCardTitle(strings.jamJoin)
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { joinCode = it.take(JamSessionState.MAX_TEXT_LENGTH) },
                            singleLine = true,
                            label = { Text(strings.jamCodeHint) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        JamPrimaryButton(
                            label = strings.jamJoin,
                            enabled = joinCode.isNotBlank(),
                            onClick = { onJoin(joinCode) }
                        )
                        Text(
                            strings.jamLocalNetworkOnly,
                            color = LevyraMuted,
                            fontSize = 12.sp,
                            lineHeight = LevyraTypeRhythm.lineHeight(12f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                item(contentType = "jam-hero") {
                    JamSessionHero(
                        jam = jam,
                        strings = strings,
                        onShare = { onShare(jam.deepLink.ifBlank { jam.code }) }
                    )
                }

                if (jam.isHost) {
                    if (jam.pending.isNotEmpty()) {
                        item(contentType = "jam-pending-header") {
                            JamSectionLabel("${strings.jamPendingRequests} · ${jam.pending.size}")
                        }
                        items(
                            items = jam.pending,
                            key = { "pending-${it.participantId}" },
                            contentType = { "jam-pending" }
                        ) { request ->
                            JamPendingRow(
                                request = request,
                                strings = strings,
                                onApprove = { onApproveParticipant(request.participantId) },
                                onReject = { onRejectParticipant(request.participantId) }
                            )
                        }
                    }

                    item(contentType = "jam-moderation") {
                        JamCard {
                            JamCardTitle(strings.jamHostControls)
                            JamSwitchRow(
                                title = strings.jamApprovalRequired,
                                subtitle = strings.jamApprovalRequiredSubtitle,
                                checked = jam.requireApproval,
                                onCheckedChange = onApprovalRequiredChange
                            )
                            JamSwitchRow(
                                title = strings.jamLockSession,
                                subtitle = strings.jamLockSessionSubtitle,
                                checked = jam.locked,
                                onCheckedChange = onLockedChange
                            )
                            if (jam.bannedCount > 0) {
                                JamInlineAction(
                                    icon = Icons.Rounded.Block,
                                    label = "${strings.jamBannedGuests} · ${jam.bannedCount}",
                                    actionLabel = strings.jamClearBans,
                                    onClick = onClearBans
                                )
                            }
                        }
                    }

                    item(contentType = "jam-permissions") {
                        JamCard {
                            JamCardTitle(strings.jamPermissions)
                            JamPermissionPicker(
                                selected = jam.permission,
                                strings = strings,
                                onSelect = onPermissionChange
                            )
                        }
                    }
                }

                item(contentType = "jam-participants-header") {
                    JamSectionLabel("${strings.jamParticipants} · ${participants.size}")
                }

                if (participants.isEmpty()) {
                    item(contentType = "jam-participants-empty") {
                        JamEmptyRow(strings.jamNoParticipants)
                    }
                } else {
                    items(
                        items = participants,
                        key = { it.id },
                        contentType = { "jam-participant" }
                    ) { participant ->
                        JamParticipantRow(
                            participant = participant,
                            strings = strings,
                            isSelf = participant.id == jam.selfParticipantId,
                            contributions = contributions[participant.id] ?: 0,
                            canModerate = jam.isHost && !participant.isHost,
                            onKick = { onRemoveParticipant(participant.id, false) },
                            onBan = { onRemoveParticipant(participant.id, true) }
                        )
                    }
                }

                item(contentType = "jam-exit") {
                    Spacer(modifier = Modifier.height(LevyraPlayerDesign.SpaceXs))
                    JamSecondaryButton(
                        label = if (jam.isHost) strings.jamEnd else strings.jamLeave,
                        onClick = { if (jam.isHost) onEnd() else onLeave() }
                    )
                }
            }
        }
    }
}

@Composable
private fun JamHeader(title: String, subtitle: String, onClose: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget)
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = LocalLevyraStrings.current.back,
                tint = LevyraText
            )
        }
        Column(modifier = Modifier.padding(start = LevyraPlayerDesign.SpaceXs)) {
            Text(
                title,
                color = LevyraText,
                fontSize = 26.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(26f),
                fontWeight = FontWeight.Black
            )
            Text(
                subtitle,
                color = LevyraMuted,
                fontSize = 13.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(13f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun JamSessionHero(jam: JamUiState, strings: LevyraStrings, onShare: () -> Unit) {
    val connected = jam.connection == JamConnectionState.Connected
    val statusColor by animateColorAsState(
        targetValue = if (connected) LevyraCyan else LevyraMuted,
        animationSpec = LevyraPlayerDesign.standardTween(),
        label = "jam-status"
    )
    Surface(
        color = LevyraPanel,
        border = BorderStroke(LevyraPlayerDesign.Hairline, JamHairline),
        shape = LevyraPlayerDesign.ShapeMd,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(LevyraPlayerDesign.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
            ) {
                Box(
                    modifier = Modifier
                        .size(LevyraPlayerDesign.SpaceSm)
                        .background(statusColor, CircleShape)
                        .clearAndSetSemantics { }
                )
                Text(
                    if (jam.isHost) strings.jamRoleHost else strings.jamRoleGuest,
                    color = LevyraText,
                    fontSize = 20.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(20f),
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.weight(1f)
                )
                if (jam.isHost && jam.locked) {
                    JamBadge(label = strings.jamLocked, icon = Icons.Rounded.Lock)
                }
            }
            Text(
                jamConnectionText(jam.connection, strings),
                color = statusColor,
                fontSize = 13.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(13f),
                fontWeight = FontWeight.SemiBold
            )

            if (jam.isHost && jam.code.isNotBlank()) {
                Spacer(modifier = Modifier.height(LevyraPlayerDesign.SpaceXxs))
                Text(
                    strings.jamSessionCode,
                    color = LevyraMuted,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    jam.code,
                    color = LevyraText,
                    fontSize = 24.sp,
                    lineHeight = LevyraTypeRhythm.lineHeight(24f),
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(LevyraPlayerDesign.SpaceXxs))
                JamPrimaryButton(
                    label = strings.jamShareInvite,
                    icon = Icons.Rounded.IosShare,
                    onClick = onShare
                )
            }
        }
    }
}

@Composable
private fun JamPendingRow(
    request: JamPendingParticipant,
    strings: LevyraStrings,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        color = LevyraPanelSoft,
        border = BorderStroke(LevyraPlayerDesign.Hairline, LevyraCyan.copy(alpha = 0.32f)),
        shape = LevyraPlayerDesign.ShapeSm,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = LevyraPlayerDesign.SpaceMd,
                vertical = LevyraPlayerDesign.SpaceSm
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
        ) {
            JamAvatar(name = request.name.ifBlank { strings.jamRoleGuest }, accent = LevyraCyan)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    request.name.ifBlank { strings.jamRoleGuest },
                    color = LevyraText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    strings.jamPendingRequests,
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onReject,
                modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = strings.jamReject, tint = LevyraMuted)
            }
            IconButton(
                onClick = onApprove,
                modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = strings.jamApprove, tint = LevyraCyan)
            }
        }
    }
}

@Composable
private fun JamParticipantRow(
    participant: JamParticipant,
    strings: LevyraStrings,
    isSelf: Boolean,
    contributions: Int,
    canModerate: Boolean,
    onKick: () -> Unit,
    onBan: () -> Unit
) {
    val role = when {
        participant.isHost -> strings.jamRoleHost
        else -> strings.jamRoleGuest
    }
    val detail = if (contributions > 0) {
        "$role · ${strings.formatTrackCount(contributions)}"
    } else {
        role
    }
    Surface(
        color = LevyraPanel,
        border = BorderStroke(LevyraPlayerDesign.Hairline, JamHairline),
        shape = LevyraPlayerDesign.ShapeSm,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = LevyraPlayerDesign.SpaceMd,
                vertical = LevyraPlayerDesign.SpaceSm
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
        ) {
            JamAvatar(
                name = participant.name.ifBlank { role },
                accent = if (participant.isHost) LevyraCyan else LevyraMuted
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs)
                ) {
                    Text(
                        participant.name.ifBlank { role },
                        color = LevyraText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isSelf) JamBadge(label = strings.jamYou)
                }
                Text(
                    detail,
                    color = LevyraMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (canModerate) {
                IconButton(
                    onClick = onKick,
                    modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = strings.jamKick, tint = LevyraMuted)
                }
                IconButton(
                    onClick = onBan,
                    modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget)
                ) {
                    Icon(Icons.Rounded.Block, contentDescription = strings.jamBan, tint = LevyraMuted)
                }
            }
        }
    }
}

@Composable
private fun JamAvatar(name: String, accent: Color) {
    val initial = remember(name) {
        name.trim().firstOrNull()?.uppercase() ?: "?"
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(accent.copy(alpha = 0.18f), CircleShape)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Text(initial, color = accent, fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun JamBadge(label: String, icon: ImageVector? = null) {
    Surface(
        color = LevyraCyan.copy(alpha = 0.16f),
        shape = LevyraPlayerDesign.ShapePill
    ) {
        Row(
            modifier = Modifier.padding(horizontal = LevyraPlayerDesign.SpaceSm, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(12.dp))
            }
            Text(label, color = LevyraCyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun JamCard(content: @Composable () -> Unit) {
    Surface(
        color = LevyraPanel,
        border = BorderStroke(LevyraPlayerDesign.Hairline, JamHairline),
        shape = LevyraPlayerDesign.ShapeMd,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(LevyraPlayerDesign.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
        ) {
            content()
        }
    }
}

@Composable
private fun JamCardTitle(text: String) {
    Text(
        text,
        color = LevyraText,
        fontSize = 15.sp,
        lineHeight = LevyraTypeRhythm.lineHeight(15f),
        fontWeight = FontWeight.Black
    )
}

@Composable
private fun JamSectionLabel(text: String) {
    Text(
        text,
        color = LevyraMuted,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(top = LevyraPlayerDesign.SpaceSm, start = LevyraPlayerDesign.SpaceXs)
    )
}

@Composable
private fun JamEmptyRow(text: String) {
    Surface(
        color = LevyraPanel,
        border = BorderStroke(LevyraPlayerDesign.Hairline, JamHairline),
        shape = LevyraPlayerDesign.ShapeSm,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text,
            color = LevyraMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(LevyraPlayerDesign.SpaceLg)
        )
    }
}

@Composable
private fun JamPermissionPicker(
    selected: JamGuestPermission,
    strings: LevyraStrings,
    onSelect: (JamGuestPermission) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceXs),
        modifier = Modifier.selectableGroup()
    ) {
        JamPermissionOptions.forEach { permission ->
            val active = permission == selected
            Surface(
                color = if (active) LevyraCyan.copy(alpha = 0.16f) else Color.Transparent,
                border = BorderStroke(
                    LevyraPlayerDesign.Hairline,
                    if (active) LevyraCyan.copy(alpha = 0.42f) else JamHairline
                ),
                shape = LevyraPlayerDesign.ShapeSm,
                onClick = { onSelect(permission) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = LevyraPlayerDesign.MinimumTouchTarget)
                    .semantics {
                        this.role = Role.RadioButton
                        this.selected = active
                    }
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = LevyraPlayerDesign.SpaceMd,
                        vertical = LevyraPlayerDesign.SpaceMd
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
                ) {
                    Text(
                        jamPermissionLabel(permission, strings),
                        color = if (active) LevyraCyan else LevyraText,
                        fontSize = 14.sp,
                        lineHeight = LevyraTypeRhythm.lineHeight(14f),
                        fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (active) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = LevyraCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JamSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LevyraPlayerDesign.MinimumTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceMd)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = LevyraText,
                fontSize = 14.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(14f),
                fontWeight = FontWeight.Bold
            )
            Text(
                subtitle,
                color = LevyraMuted,
                fontSize = 12.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(12f),
                fontWeight = FontWeight.Medium
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = LevyraBlack,
                checkedTrackColor = LevyraCyan,
                uncheckedThumbColor = LevyraMuted,
                uncheckedTrackColor = LevyraPanelSoft
            )
        )
    }
}

@Composable
private fun JamInlineAction(
    icon: ImageVector,
    label: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LevyraPlayerDesign.MinimumTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm)
    ) {
        Icon(icon, contentDescription = null, tint = LevyraMuted, modifier = Modifier.size(18.dp))
        Text(
            label,
            color = LevyraText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Surface(
            color = Color.Transparent,
            border = BorderStroke(LevyraPlayerDesign.Hairline, JamHairline),
            shape = LevyraPlayerDesign.ShapePill,
            onClick = onClick,
            modifier = Modifier.sizeIn(minHeight = LevyraPlayerDesign.MinimumTouchTarget)
        ) {
            Text(
                actionLabel,
                color = LevyraText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(
                    horizontal = LevyraPlayerDesign.SpaceMd,
                    vertical = LevyraPlayerDesign.SpaceSm
                )
            )
        }
    }
}

@Composable
private fun JamPrimaryButton(
    label: String,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        color = if (enabled) LevyraCyan else LevyraPanelSoft,
        shape = LevyraPlayerDesign.ShapePill,
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LevyraPlayerDesign.MinimumTouchTarget)
    ) {
        Row(
            modifier = Modifier.padding(vertical = LevyraPlayerDesign.SpaceMd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LevyraPlayerDesign.SpaceSm, Alignment.CenterHorizontally)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (enabled) LevyraOnAccent else LevyraMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                label,
                color = if (enabled) LevyraOnAccent else LevyraMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun JamSecondaryButton(label: String, onClick: () -> Unit) {
    Surface(
        color = Color.Transparent,
        border = BorderStroke(LevyraPlayerDesign.Hairline, JamHairline),
        shape = LevyraPlayerDesign.ShapePill,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LevyraPlayerDesign.MinimumTouchTarget)
    ) {
        Box(
            modifier = Modifier.padding(vertical = LevyraPlayerDesign.SpaceMd),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = LevyraText, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun JamFailureBanner(message: String, dismissLabel: String, onDismiss: () -> Unit) {
    Surface(
        color = LevyraPanelSoft,
        border = BorderStroke(LevyraPlayerDesign.Hairline, LevyraCyan.copy(alpha = 0.28f)),
        shape = LevyraPlayerDesign.ShapeSm,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(
                start = LevyraPlayerDesign.SpaceLg,
                end = LevyraPlayerDesign.SpaceSm,
                top = LevyraPlayerDesign.SpaceSm,
                bottom = LevyraPlayerDesign.SpaceSm
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                message,
                color = LevyraText,
                fontSize = 13.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(13f),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(LevyraPlayerDesign.MinimumTouchTarget)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = dismissLabel, tint = LevyraMuted)
            }
        }
    }
}

internal fun jamContributions(
    queue: List<JamTrack>,
    participants: List<JamParticipant>
): Map<String, Int> {
    if (queue.isEmpty() || participants.isEmpty()) return emptyMap()
    val known = participants.mapTo(hashSetOf()) { it.id }
    val counts = mutableMapOf<String, Int>()
    queue.forEach { track ->
        val owner = track.addedBy
        if (owner.isNotBlank() && owner in known) {
            counts[owner] = (counts[owner] ?: 0) + 1
        }
    }
    return counts
}

private fun jamPermissionLabel(permission: JamGuestPermission, strings: LevyraStrings): String = when (permission) {
    JamGuestPermission.HostOnly -> strings.jamPermissionHostOnly
    JamGuestPermission.AddSongs -> strings.jamPermissionAddSongs
    JamGuestPermission.Collaborative -> strings.jamPermissionCollaborative
}

private fun jamConnectionText(state: JamConnectionState, strings: LevyraStrings): String = when (state) {
    JamConnectionState.Idle,
    JamConnectionState.Disconnected -> strings.jamDisconnected
    JamConnectionState.Connecting -> strings.jamConnecting
    JamConnectionState.AwaitingApproval -> strings.jamAwaitingApproval
    JamConnectionState.Connected -> strings.connected
}

private fun jamFailureText(failure: JamFailure, strings: LevyraStrings): String = when (failure) {
    JamFailure.InvalidCode -> strings.jamInvalidCode
    JamFailure.NotAuthorized -> strings.jamNotAuthorized
    JamFailure.HostEnded -> strings.jamHostEnded
    JamFailure.Rejected -> strings.jamRejected
    JamFailure.Banned -> strings.jamBannedMessage
    JamFailure.SessionLocked -> strings.jamSessionLockedMessage
    JamFailure.SessionFull -> strings.jamSessionFull
    JamFailure.Removed -> strings.jamRemovedMessage
    JamFailure.ConnectionFailed,
    JamFailure.ProtocolError -> strings.jamConnectionFailed
}
