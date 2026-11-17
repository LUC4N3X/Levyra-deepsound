@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.luc4n3x.levyra.ui.jam

import androidx.compose.foundation.text.selection.SelectionContainer
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.PI
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import com.luc4n3x.levyra.ui.i18n.jamScanToJoin
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.luc4n3x.levyra.feature.jam.JamInvitationQr
import com.google.zxing.common.BitMatrix
import androidx.compose.runtime.produceState
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.rounded.Headphones
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.ui.theme.LevyraPink
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luc4n3x.levyra.feature.jam.JamSessionCode
import com.luc4n3x.levyra.ui.i18n.jamScanQr
import com.luc4n3x.levyra.ui.i18n.jamScannerUnavailable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.heading
import com.luc4n3x.levyra.ui.AtelierCabinetKnob
import com.luc4n3x.levyra.ui.atelierUppercase
import com.luc4n3x.levyra.ui.theme.AtelierDesign
import com.luc4n3x.levyra.ui.theme.LevyraIsAtelier
import com.luc4n3x.levyra.ui.theme.LevyraIsMono
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.launch

private val JamPermissionOptions = listOf(
    JamGuestPermission.HostOnly,
    JamGuestPermission.AddSongs,
    JamGuestPermission.Collaborative
)

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
    onClose: () -> Unit,
    qrScanner: JamQrScanAction
) {
    CompositionLocalProvider(LocalJamLook provides jamLookForActiveSkin()) {
        JamOverlayContent(
            jam, displayName, onDisplayNameChange, onCreate, onJoin, onLeave, onEnd, onPermissionChange,
            onApprovalRequiredChange, onLockedChange, onApproveParticipant, onRejectParticipant, onRemoveParticipant,
            onClearBans, onShare, onDismissFailure, onClose, qrScanner
        )
    }
}

@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
private fun JamOverlayContent(
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
    onClose: () -> Unit,
    qrScanner: JamQrScanAction
) {
    val look = LocalJamLook.current
    val strings = LocalLevyraStrings.current
    var joinCode by rememberSaveable { mutableStateOf("") }
    var pendingPermissionId by rememberSaveable { mutableStateOf(JamGuestPermission.AddSongs.id) }
    var pendingApproval by rememberSaveable { mutableStateOf(true) }
    val selectedPermission = JamGuestPermission.fromId(pendingPermissionId)
    val session = jam.session
    var sharing by rememberSaveable(jam.code) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val sessionIdentity = "${jam.role?.name.orEmpty()}:${session?.sessionId.orEmpty()}"
    var displayedSessionIdentity by rememberSaveable { mutableStateOf(sessionIdentity) }
    LaunchedEffect(sessionIdentity) {
        if (displayedSessionIdentity != sessionIdentity) {
            listState.scrollToItem(0)
            displayedSessionIdentity = sessionIdentity
        }
    }
    val scope = rememberCoroutineScope()
    val canJoin = !jam.isActive && jam.connection != JamConnectionState.Connecting
    val invitation = remember(jam.deepLink, jam.code) {
        JamSessionCode.parse(jam.deepLink.ifBlank { jam.code })?.deepLink()
    }
    val participants = session?.participants.orEmpty()
    val contributions = remember(session?.queue, participants) {
        jamContributions(session?.queue.orEmpty(), participants)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (look.skinned) {
                    Brush.verticalGradient(listOf(if (LevyraIsAtelier) look.card else look.background, look.background, look.background))
                } else {
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainerLow,
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background
                        )
                    )
                }
            )
    ) {
        if (LevyraIsAtelier) AtelierJamBackdrop() else if (!look.skinned) JamAmbientBackdrop()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
                bottom = 140.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            if (qrScanner.unavailable) {
                item(contentType = "jam-scanner-failure") {
                    JamFailureBanner(strings.jamScannerUnavailable, strings.close, qrScanner.dismissFailure)
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
                            colors = jamFieldColors(),
                            shape = jamFieldShape(),
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
                        JamSecondaryButton(
                            label = strings.jamScanQr,
                            onClick = { scope.launch { listState.scrollToItem(0); qrScanner.scan() } }
                        )
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { joinCode = it.take(JamSessionState.MAX_TEXT_LENGTH) },
                            singleLine = true,
                            label = { Text(strings.jamCodeHint) },
                            colors = jamFieldColors(),
                            shape = jamFieldShape(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        JamPrimaryButton(
                            label = strings.jamJoin,
                            enabled = joinCode.isNotBlank() && canJoin,
                            onClick = { onJoin(joinCode) }
                        )
                        Text(
                            strings.jamLocalNetworkOnly,
                            color = if (look.skinned) look.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                item(contentType = "jam-hero") {
                    JamSessionHero(
                        jam = jam,
                        strings = strings,
                        onShare = { sharing = true }
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
                    Spacer(modifier = Modifier.height(4.dp))
                    JamSecondaryButton(
                        label = if (jam.isHost) strings.jamEnd else strings.jamLeave,
                        onClick = { if (jam.isHost) onEnd() else onLeave() }
                    )
                }
            }
        }
        val scrim = if (look.skinned) look.background else MaterialTheme.colorScheme.background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp)
                .background(Brush.verticalGradient(listOf(scrim, scrim.copy(alpha = 0.85f), Color.Transparent)))
                .clearAndSetSemantics { }
        )
        if (sharing && jam.isHost && invitation != null) {
            Dialog(onDismissRequest = { sharing = false }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
                JamQrShare(
                    invitation = invitation,
                    sessionName = strings.jamTitle,
                    hostName = session?.participants?.firstOrNull { it.isHost }?.name.orEmpty(),
                    onShare = onShare,
                    onClose = { sharing = false }
                )
            }
        }
    }
}

@Composable
private fun JamAmbientBackdrop() {
    val pink = LevyraPink
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clearAndSetSemantics { }
            .drawBehind {
                jamGlow(pink, 0.10f, Offset(size.width * 0.9f, size.height * 0.92f), size.width * 0.8f)
            }
    )
}

@Composable
private fun DefaultJamStage(title: String, subtitle: String, onClose: () -> Unit) {
    val cyan = LevyraCyan
    val violet = LevyraViolet
    val pink = LevyraPink
    val animate = LocalAnimationsEnabled.current
    val ripple = if (animate) {
        rememberInfiniteTransition(label = "jam-stage").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(JamRippleMs, easing = LinearEasing), RepeatMode.Restart),
            label = "jam-stage-ripple"
        )
    } else {
        null
    }
    Box(modifier = Modifier.fillMaxWidth().heightIn(min = JamStageHeight)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clearAndSetSemantics { }
                .drawBehind {
                    val center = Offset(size.width / 2f, JamStageMedallionCenter.toPx())
                    jamGlow(violet, 0.55f, center, size.width * 0.62f)
                    jamGlow(cyan, 0.42f, Offset(size.width * 0.08f, size.height * 0.12f), size.width * 0.55f)
                    jamGlow(pink, 0.40f, Offset(size.width * 0.96f, size.height * 0.46f), size.width * 0.55f)
                    val base = JamMedallionSize.toPx() / 2f
                    val step = JamRingStep.toPx()
                    val phase = ripple?.value ?: 0f
                    for (ring in 0 until JamRingCount) {
                        val position = ring + phase
                        val fade = (1f - position / JamRingCount).coerceIn(0f, 1f)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.22f * fade),
                            radius = base + step * position,
                            center = center,
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                    }
                }
        )
        LevyraBackButton(
            onClick = onClose,
            contentDescription = LocalLevyraStrings.current.back,
            size = 48.dp,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.align(Alignment.TopStart)
        )
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = JamStageMedallionCenter - JamMedallionSize / 2, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(JamMedallionSize)
                    .background(Brush.linearGradient(listOf(cyan, violet, pink)), MaterialShapes.Cookie9Sided.toShape())
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Headphones, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text(
                title,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val JamStageHeight = 360.dp
private val JamStageMedallionCenter = 128.dp
private val JamMedallionSize = 104.dp
private val JamRingStep = 30.dp
private const val JamRingCount = 5
private const val JamRippleMs = 3_200

private fun androidx.compose.ui.graphics.drawscope.DrawScope.jamGlow(color: Color, alpha: Float, center: Offset, radius: Float) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center = center, radius = radius),
        radius = radius,
        center = center
    )
}

@Composable
private fun AtelierJamBackdrop() {
    val accent = AtelierDesign.accent
    Box(
        Modifier
            .fillMaxSize()
            .clearAndSetSemantics { }
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent), center = Offset(size.width * 0.86f, size.height * 0.04f), radius = size.width * 0.9f),
                    radius = size.width * 0.9f,
                    center = Offset(size.width * 0.86f, size.height * 0.04f)
                )
            }
    )
}

@Composable
private fun AtelierJamHeader(title: String, subtitle: String, onClose: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            LevyraBackButton(onClick = onClose, contentDescription = LocalLevyraStrings.current.back, size = 48.dp)
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AtelierDesign.cabinet)
                    .border(1.dp, AtelierDesign.hairlineStrong, CircleShape)
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center
            ) {
                AtelierCabinetKnob(-48f, Modifier.size(40.dp))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = AtelierDesign.text, style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Light), modifier = Modifier.semantics { heading() })
            Text(subtitle, color = AtelierDesign.textSecondary, style = MaterialTheme.typography.bodyLarge)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
    }
}

@Composable
private fun JamHeader(title: String, subtitle: String, onClose: () -> Unit) {
    if (LevyraIsAtelier) {
        AtelierJamHeader(title, subtitle, onClose)
        return
    }
    if (LevyraIsMono) {
        MonoJamHeader(title, subtitle, onClose)
        return
    }
    val look = LocalJamLook.current
    if (look.skinned) {
        Column(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            LevyraBackButton(onClick = onClose, contentDescription = LocalLevyraStrings.current.back, size = 48.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = look.text, style = MaterialTheme.typography.headlineLarge, fontWeight = look.displayWeight, modifier = Modifier.semantics { heading() })
                Text(subtitle, color = look.textSecondary, style = MaterialTheme.typography.bodyLarge)
            }
        }
        return
    }
    DefaultJamStage(title, subtitle, onClose)
}

@Composable
private fun SkinnedJamSessionHero(jam: JamUiState, strings: LevyraStrings, onShare: () -> Unit) {
    val look = LocalJamLook.current
    val connected = jam.connection == JamConnectionState.Connected
    val led by animateColorAsState(if (connected) look.mark else look.textSecondary, label = "jam-led")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(look.cardShape)
            .background(look.card)
            .border(1.dp, look.border, look.cardShape)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(10.dp)
                    .drawBehind {
                        drawCircle(led.copy(alpha = 0.28f), radius = size.minDimension)
                        drawCircle(led)
                    }
                    .clearAndSetSemantics { }
            )
            Column(Modifier.weight(1f)) {
                Text(jamLabel(look, if (jam.isHost) strings.jamRoleHost else strings.jamRoleGuest), color = look.text, style = MaterialTheme.typography.labelMedium)
                Text(jamConnectionText(jam.connection, strings), color = if (connected) look.accent else look.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
            if (jam.isHost && jam.locked) JamBadge(label = strings.jamLocked, icon = Icons.Rounded.Lock)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(look.border))
        if (jam.isHost && jam.code.isNotBlank()) {
            JamInvitationCode(jam.code, Modifier.padding(horizontal = 20.dp, vertical = 16.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(look.border))
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(jamLabel(look, strings.jamParticipants), color = look.textSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text((jam.session?.participants?.size ?: 0).toString(), color = look.text, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = look.displayWeight))
        }
        if (jam.isHost && jam.code.isNotBlank()) {
            Box(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
                JamPrimaryButton(label = strings.jamShareInvite, icon = Icons.Rounded.IosShare, onClick = onShare)
            }
        }
    }
}

@Composable
private fun JamSessionHero(jam: JamUiState, strings: LevyraStrings, onShare: () -> Unit) {
    if (LevyraIsMono) {
        MonoJamSessionHero(
            jam.session?.participants.orEmpty(), jam.isHost, jam.connection == JamConnectionState.Connected,
            jamConnectionText(jam.connection, strings), jam.isHost && jam.locked, jam.code, onShare
        )
        return
    }
    if (LocalJamLook.current.skinned) {
        SkinnedJamSessionHero(jam, strings, onShare)
        return
    }
    val connected = jam.connection == JamConnectionState.Connected
    val scheme = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraLarge
    val invitation = remember(jam.deepLink, jam.code) {
        JamSessionCode.parse(jam.deepLink.ifBlank { jam.code })?.deepLink()
    }
    val hostName = jam.session?.participants?.firstOrNull { it.isHost }?.name.orEmpty()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(scheme.surfaceContainerLow)
            .border(1.dp, scheme.outlineVariant.copy(alpha = 0.6f), shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            JamLiveDot(connected)
            Text(
                jamConnectionText(jam.connection, strings),
                color = scheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (jam.isHost && jam.locked) {
                Icon(Icons.Rounded.Lock, contentDescription = strings.jamLocked, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            Text(
                if (jam.isHost) strings.jamRoleHost else strings.jamRoleGuest,
                color = scheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
        if (jam.isHost && invitation != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                JamInlineQr(invitation, strings.jamScanToJoin)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        strings.jamScanToJoin,
                        color = scheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        strings.jamLocalNetworkOnly,
                        color = scheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (jam.code.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                jam.code,
                                color = scheme.onSurface,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                letterSpacing = 0.4.sp,
                                modifier = Modifier.semantics { contentDescription = "${strings.jamSessionCode}: ${jam.code}" }
                            )
                        }
                    }
                }
            }
            Button(
                onClick = onShare,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(containerColor = LevyraCyan, contentColor = LevyraOnAccent),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            ) {
                Icon(Icons.Rounded.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.jamShareInvite, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
        } else if (!jam.isHost && hostName.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                JamAvatar(name = hostName, accent = LevyraCyan)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(hostName, color = scheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(strings.jamRoleHost, color = scheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun jamSignatureBrush(): Brush {
    val cyan = LevyraCyan
    val violet = LevyraViolet
    val pink = LevyraPink
    return remember(cyan, violet, pink) { Brush.linearGradient(listOf(cyan, violet, pink)) }
}

@Composable
private fun JamLiveDot(connected: Boolean) {
    val dot = if (connected) LevyraCyan else MaterialTheme.colorScheme.onSurfaceVariant
    val animate = connected && LocalAnimationsEnabled.current
    val pulse = if (animate) {
        rememberInfiniteTransition(label = "jam-live").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(JamLivePulseMs), RepeatMode.Restart),
            label = "jam-live-pulse"
        )
    } else {
        null
    }
    Box(
        Modifier
            .size(10.dp)
            .clearAndSetSemantics { }
            .drawBehind {
                val progress = pulse?.value ?: 0f
                if (pulse != null) drawCircle(dot.copy(alpha = 0.4f * (1f - progress)), radius = size.minDimension / 2f * (1f + progress * 1.4f))
                drawCircle(dot)
            }
    )
}

@Composable
private fun JamInlineQr(invitation: String, description: String) {
    val qr by produceState<Result<BitMatrix>?>(null, invitation) {
        value = withContext(Dispatchers.Default) { runCatching { JamInvitationQr.encode(invitation) } }
    }
    Box(
        modifier = Modifier
            .size(JamInlineQrSize)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        val matrix = qr?.getOrNull()
        when {
            matrix != null -> JamQrImage(matrix, description)
            qr == null -> CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            else -> Icon(Icons.Rounded.QrCode2, contentDescription = null, tint = Color.Black.copy(alpha = 0.4f))
        }
    }
}

private val JamInlineQrSize = 128.dp

private const val JamLivePulseMs = 1_600

@Composable
private fun JamPendingRow(
    request: JamPendingParticipant,
    strings: LevyraStrings,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val look = LocalJamLook.current
    Surface(
        color = if (look.skinned) look.card else MaterialTheme.colorScheme.secondaryContainer,
        shape = if (look.skinned) look.cardShape else MaterialTheme.shapes.large,
        border = if (look.skinned) BorderStroke(1.dp, look.accent.copy(alpha = 0.45f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            JamAvatar(name = request.name.ifBlank { strings.jamRoleGuest }, accent = if (look.skinned) look.accent else MaterialTheme.colorScheme.secondary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    request.name.ifBlank { strings.jamRoleGuest },
                    color = if (look.skinned) look.text else MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    strings.jamPendingRequests,
                    color = if (look.skinned) look.textSecondary else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onReject,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = strings.jamReject)
            }
            IconButton(
                onClick = onApprove,
                colors = if (look.skinned) {
                    IconButtonDefaults.filledIconButtonColors(containerColor = look.accent, contentColor = look.onAccent)
                } else {
                    IconButtonDefaults.filledTonalIconButtonColors()
                },
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = strings.jamApprove)
            }
        }
    }
}

@Suppress("ComplexMethod", "CognitiveComplexMethod")
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
    val look = LocalJamLook.current
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
        color = if (look.skinned) look.card else MaterialTheme.colorScheme.surfaceContainer,
        shape = if (look.skinned) look.cardShape else MaterialTheme.shapes.large,
        border = if (look.skinned) BorderStroke(1.dp, look.border) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            JamAvatar(
                name = participant.name.ifBlank { role },
                accent = when {
                    look.skinned && participant.isHost -> look.mark
                    look.skinned -> look.textSecondary
                    participant.isHost -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.secondary
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        participant.name.ifBlank { role },
                        color = if (look.skinned) look.text else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isSelf) JamBadge(label = strings.jamYou)
                }
                Text(
                    detail,
                    color = if (look.skinned) look.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (canModerate) {
                IconButton(
                    onClick = onKick,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = strings.jamKick)
                }
                IconButton(
                    onClick = onBan,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.Block, contentDescription = strings.jamBan)
                }
            }
        }
    }
}

@Composable
private fun JamAvatar(name: String, accent: Color) {
    val look = LocalJamLook.current
    val initial = remember(name) {
        name.trim().firstOrNull()?.uppercase() ?: "?"
    }
    val shape = if (look.skinned) CircleShape else MaterialShapes.Cookie9Sided.toShape()
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(accent.copy(alpha = if (look.skinned) 0.1f else 0.16f), shape)
            .then(if (look.skinned) Modifier.border(1.dp, accent.copy(alpha = 0.5f), shape) else Modifier)
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        Text(initial, color = accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun JamBadge(label: String, icon: ImageVector? = null) {
    val look = LocalJamLook.current
    Surface(
        color = if (look.skinned) Color.Transparent else MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = if (look.skinned) look.mark else MaterialTheme.colorScheme.onTertiaryContainer,
        shape = if (look.skinned) look.buttonShape else MaterialTheme.shapes.extraLarge,
        border = if (look.skinned) BorderStroke(1.dp, look.border) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            }
            Text(if (look.skinned) jamLabel(look, label) else label, style = if (look.skinned) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun JamCard(content: @Composable () -> Unit) {
    val look = LocalJamLook.current
    if (look.skinned) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(look.cardShape)
                .background(look.card)
                .border(1.dp, look.border, look.cardShape)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) { content() }
        return
    }
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun JamCardTitle(text: String) {
    val look = LocalJamLook.current
    if (look.skinned) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.semantics(mergeDescendants = true) { heading() }) {
            if (LevyraIsAtelier) AtelierCabinetKnob(-40f, Modifier.size(22.dp).clearAndSetSemantics { })
            Text(jamLabel(look, text), color = look.mark, style = MaterialTheme.typography.labelMedium)
        }
        return
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(
                    jamSignatureBrush(),
                    MaterialShapes.Cookie6Sided.toShape()
                )
        )
        Text(
            text,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun JamSectionLabel(text: String) {
    val look = LocalJamLook.current
    Text(
        if (look.skinned) jamLabel(look, text) else text,
        color = if (look.skinned) look.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
        style = if (look.skinned) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
    )
}

@Composable
private fun JamEmptyRow(text: String) {
    val look = LocalJamLook.current
    Surface(
        color = if (look.skinned) look.card else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = if (look.skinned) look.cardShape else MaterialTheme.shapes.large,
        border = if (look.skinned) BorderStroke(1.dp, look.border) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text,
            color = if (look.skinned) look.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Suppress("ComplexMethod", "CognitiveComplexMethod")
@Composable
private fun JamPermissionPicker(
    selected: JamGuestPermission,
    strings: LevyraStrings,
    onSelect: (JamGuestPermission) -> Unit
) {
    if (LevyraIsAtelier) {
        AtelierJamPermissionPicker(selected, strings, onSelect)
        return
    }
    if (LevyraIsMono) {
        MonoJamPermissionPicker(JamPermissionOptions, selected, { jamPermissionLabel(it, strings) }, onSelect)
        return
    }
    val look = LocalJamLook.current
    if (look.skinned) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
            JamPermissionOptions.forEach { permission ->
                val active = permission == selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .clip(look.rowShape)
                        .background(if (active) look.selectedRow else look.background.copy(alpha = 0.4f))
                        .then(if (active) Modifier.border(1.dp, look.mark.copy(alpha = 0.7f), look.rowShape) else Modifier)
                        .selectable(selected = active, role = Role.RadioButton) { onSelect(permission) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        jamPermissionLabel(permission, strings),
                        color = if (active) look.text else look.textSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (active) look.titleWeight else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (active) Icon(Icons.Rounded.Check, contentDescription = null, tint = look.mark, modifier = Modifier.size(18.dp))
                }
            }
        }
        return
    }
    val scheme = MaterialTheme.colorScheme
    val motion = LocalAnimationsEnabled.current
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.selectableGroup()
    ) {
        JamPermissionOptions.forEach { permission ->
            val active = permission == selected
            val amplitude by animateDpAsState(
                targetValue = if (active) JamWaveAmplitude else 0.dp,
                animationSpec = if (motion) spring(dampingRatio = 0.55f, stiffness = 380f) else snap(),
                label = "jam-option-wave"
            )
            val shape = if (active) remember(amplitude) { JamWavyPillShape(amplitude, JamWaveLength) } else JamOptionRestShape
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clip(shape)
                    .background(if (active) scheme.primaryContainer else scheme.surfaceContainerHigh)
                    .selectable(selected = active, role = Role.RadioButton) { onSelect(permission) }
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    jamPermissionLabel(permission, strings),
                    color = if (active) scheme.onPrimaryContainer else scheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (active) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = scheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private class JamWavyPillShape(private val amplitude: Dp, private val wavelength: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val wave = with(density) { amplitude.toPx() }.coerceIn(0f, size.height / 6f)
        val cap = (size.height - wave * 2f) / 2f
        val start = cap
        val end = size.width - cap
        val run = (end - start).coerceAtLeast(0f)
        val waves = max(1, (run / with(density) { wavelength.toPx() }).roundToInt())
        val samples = waves * JamWaveSamples
        val path = Path()
        path.moveTo(start, wave)
        for (index in 1..samples) {
            val fraction = index.toFloat() / samples
            path.lineTo(start + run * fraction, wave - wave * sin(fraction * waves * 2f * PI.toFloat()))
        }
        path.arcTo(Rect(end - cap, wave, end + cap, size.height - wave), -90f, 180f, false)
        for (index in 1..samples) {
            val fraction = index.toFloat() / samples
            path.lineTo(end - run * fraction, size.height - wave + wave * sin(fraction * waves * 2f * PI.toFloat()))
        }
        path.arcTo(Rect(start - cap, wave, start + cap, size.height - wave), 90f, 180f, false)
        path.close()
        return Outline.Generic(path)
    }
}

private val JamOptionRestShape = RoundedCornerShape(20.dp)
private val JamWaveAmplitude = 3.dp
private val JamWaveLength = 34.dp
private const val JamWaveSamples = 12

@Composable
private fun AtelierJamPermissionPicker(
    selected: JamGuestPermission,
    strings: LevyraStrings,
    onSelect: (JamGuestPermission) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(AtelierDesign.panelShape)
            .border(1.dp, AtelierDesign.hairlineStrong, AtelierDesign.panelShape)
            .selectableGroup()
    ) {
        JamPermissionOptions.forEachIndexed { index, permission ->
            val active = permission == selected
            val knob by animateFloatAsState(if (active) -40f else -140f, label = "atelier-jam-knob")
            if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(AtelierDesign.hairline))
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .background(if (active) AtelierDesign.panelRaised else Color.Transparent)
                    .selectable(selected = active, role = Role.RadioButton) { onSelect(permission) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AtelierCabinetKnob(knob, Modifier.size(26.dp))
                Text(
                    jamPermissionLabel(permission, strings),
                    color = if (active) AtelierDesign.text else AtelierDesign.textSecondary,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Box(Modifier.size(8.dp).background(if (active) AtelierDesign.accentBright else AtelierDesign.hairlineStrong, CircleShape))
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
    val look = LocalJamLook.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                color = if (look.skinned) look.text else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (look.skinned) FontWeight.Medium else FontWeight.Bold
            )
            Text(
                subtitle,
                color = if (look.skinned) look.textSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = if (look.skinned) {
                SwitchDefaults.colors(
                    checkedThumbColor = look.onAccent,
                    checkedTrackColor = look.accent,
                    checkedBorderColor = look.accent,
                    uncheckedThumbColor = look.textSecondary,
                    uncheckedTrackColor = look.background,
                    uncheckedBorderColor = look.border
                )
            } else {
                SwitchDefaults.colors()
            }
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
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClick, shapes = ButtonDefaults.shapes()) { Text(actionLabel) }
    }
}

@Composable
private fun JamPrimaryButton(
    label: String,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    val look = LocalJamLook.current
    Button(
        enabled = enabled,
        onClick = onClick,
        shapes = if (look.skinned) ButtonDefaults.shapes(shape = look.buttonShape, pressedShape = look.cardShape) else ButtonDefaults.shapes(),
        colors = if (look.skinned) {
            ButtonDefaults.buttonColors(
                containerColor = look.accent,
                contentColor = look.onAccent,
                disabledContainerColor = look.selectedRow,
                disabledContentColor = look.textSecondary
            )
        } else {
            ButtonDefaults.buttonColors()
        },
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun JamSecondaryButton(label: String, onClick: () -> Unit) {
    val look = LocalJamLook.current
    OutlinedButton(
        onClick = onClick,
        shapes = if (look.skinned) ButtonDefaults.shapes(shape = look.buttonShape, pressedShape = look.cardShape) else ButtonDefaults.shapes(),
        colors = if (look.skinned) ButtonDefaults.outlinedButtonColors(contentColor = look.text) else ButtonDefaults.outlinedButtonColors(),
        border = if (look.skinned) BorderStroke(1.dp, look.border) else ButtonDefaults.outlinedButtonBorder(true),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun JamFailureBanner(message: String, dismissLabel: String, onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(
                start = 20.dp,
                end = 8.dp,
                top = 12.dp,
                bottom = 12.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                shapes = IconButtonDefaults.shapes(),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = dismissLabel)
            }
        }
    }
}

@Composable
private fun jamFieldColors(): TextFieldColors =
    LocalJamLook.current.let { look ->
    if (look.skinned) {
        OutlinedTextFieldDefaults.colors(
            focusedTextColor = look.text,
            unfocusedTextColor = look.text,
            focusedBorderColor = look.accent,
            unfocusedBorderColor = look.border,
            focusedLabelColor = look.mark,
            unfocusedLabelColor = look.textSecondary,
            cursorColor = look.mark,
            focusedContainerColor = look.background,
            unfocusedContainerColor = look.background
        )
    } else {
        OutlinedTextFieldDefaults.colors()
    }
    }

@Composable
private fun jamFieldShape(): Shape {
    val look = LocalJamLook.current
    return if (look.skinned) look.rowShape else OutlinedTextFieldDefaults.shape
}

@Composable
private fun jamLabel(look: JamLook, text: String): String = if (look.uppercaseLabels) atelierUppercase(text) else text

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
