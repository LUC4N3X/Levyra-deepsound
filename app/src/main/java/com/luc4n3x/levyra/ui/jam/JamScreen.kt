package com.luc4n3x.levyra.ui.jam

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.feature.jam.JamConnectionState
import com.luc4n3x.levyra.feature.jam.JamFailure
import com.luc4n3x.levyra.feature.jam.JamGuestPermission
import com.luc4n3x.levyra.feature.jam.JamParticipant
import com.luc4n3x.levyra.feature.jam.JamSessionState
import com.luc4n3x.levyra.feature.jam.JamUiState
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText

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
    onCreate: (JamGuestPermission) -> Unit,
    onJoin: (String) -> Unit,
    onLeave: () -> Unit,
    onEnd: () -> Unit,
    onPermissionChange: (JamGuestPermission) -> Unit,
    onRemoveParticipant: (String) -> Unit,
    onShare: (String) -> Unit,
    onDismissFailure: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    var joinCode by rememberSaveable { mutableStateOf("") }
    var pendingPermissionId by rememberSaveable { mutableStateOf(JamGuestPermission.AddSongs.id) }
    val selectedPermission = JamGuestPermission.fromId(pendingPermissionId)
    val session = jam.session

    Box(modifier = Modifier.fillMaxSize().background(LevyraInk)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 140.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(contentType = "jam-header") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.back,
                            tint = LevyraText
                        )
                    }
                    Column(modifier = Modifier.padding(start = 6.dp)) {
                        Text(strings.jamTitle, color = LevyraText, fontSize = 26.sp, fontWeight = FontWeight.Black)
                        Text(
                            strings.jamSubtitle,
                            color = LevyraMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            jam.failure?.let { failure ->
                item(contentType = "jam-failure") {
                    JamCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                jamFailureText(failure, strings),
                                color = LevyraText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onDismissFailure) {
                                Icon(Icons.Rounded.Close, contentDescription = strings.close, tint = LevyraMuted)
                            }
                        }
                    }
                }
            }

            if (!jam.isActive) {
                item(contentType = "jam-name") {
                    JamCard {
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
                        Text(
                            strings.jamPermissions,
                            color = LevyraText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        JamPermissionOptions.forEach { permission ->
                            JamOptionRow(
                                label = jamPermissionLabel(permission, strings),
                                selected = permission == selectedPermission,
                                onClick = { pendingPermissionId = permission.id }
                            )
                        }
                        Button(
                            onClick = { onCreate(selectedPermission) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LevyraCyan,
                                contentColor = LevyraOnAccent
                            )
                        ) {
                            Text(strings.jamCreate, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item(contentType = "jam-join") {
                    JamCard {
                        OutlinedTextField(
                            value = joinCode,
                            onValueChange = { joinCode = it.take(JamSessionState.MAX_TEXT_LENGTH) },
                            singleLine = true,
                            label = { Text(strings.jamCodeHint) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { onJoin(joinCode) },
                            enabled = joinCode.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LevyraCyan,
                                contentColor = LevyraOnAccent
                            )
                        ) {
                            Text(strings.jamJoin, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            strings.jamLocalNetworkOnly,
                            color = LevyraMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                item(contentType = "jam-status") {
                    JamCard {
                        Text(
                            if (jam.isHost) strings.jamRoleHost else strings.jamRoleGuest,
                            color = LevyraText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            jamConnectionText(jam.connection, strings),
                            color = if (jam.connection == JamConnectionState.Connected) LevyraCyan else LevyraMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (jam.isHost && jam.code.isNotBlank()) {
                            Text(
                                strings.jamSessionCode,
                                color = LevyraMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(jam.code, color = LevyraText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onShare(jam.deepLink.ifBlank { jam.code }) }) {
                                    Icon(Icons.Rounded.Share, contentDescription = strings.share, tint = LevyraText)
                                }
                                TextButton(onClick = { onShare(jam.deepLink.ifBlank { jam.code }) }) {
                                    Text(strings.share, color = LevyraText, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (jam.isHost) {
                    item(contentType = "jam-permissions") {
                        JamCard {
                            Text(
                                strings.jamPermissions,
                                color = LevyraText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            JamPermissionOptions.forEach { permission ->
                                JamOptionRow(
                                    label = jamPermissionLabel(permission, strings),
                                    selected = permission == jam.permission,
                                    onClick = { onPermissionChange(permission) }
                                )
                            }
                        }
                    }
                }

                item(contentType = "jam-participants-header") {
                    Text(
                        strings.jamParticipants,
                        color = LevyraText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(
                    items = session?.participants.orEmpty(),
                    key = { it.id },
                    contentType = { "jam-participant" }
                ) { participant ->
                    JamParticipantRow(
                        participant = participant,
                        strings = strings,
                        canRemove = jam.isHost && !participant.isHost,
                        onRemove = { onRemoveParticipant(participant.id) }
                    )
                }

                item(contentType = "jam-exit") {
                    Button(
                        onClick = { if (jam.isHost) onEnd() else onLeave() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LevyraPanel,
                            contentColor = LevyraText
                        )
                    ) {
                        Text(if (jam.isHost) strings.jamEnd else strings.jamLeave, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun JamCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(LevyraPanel)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        content()
    }
}

@Composable
private fun JamOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
    ) {
        Text(
            label,
            color = if (selected) LevyraCyan else LevyraMuted,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun JamParticipantRow(
    participant: JamParticipant,
    strings: LevyraStrings,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LevyraPanel)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                participant.name.ifBlank { strings.jamRoleGuest },
                color = LevyraText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (participant.isHost) strings.jamRoleHost else strings.jamRoleGuest,
                color = LevyraMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = strings.remove, tint = LevyraMuted)
            }
        }
    }
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
    JamConnectionState.Connected -> strings.connected
}

private fun jamFailureText(failure: JamFailure, strings: LevyraStrings): String = when (failure) {
    JamFailure.InvalidCode -> strings.jamInvalidCode
    JamFailure.NotAuthorized -> strings.jamNotAuthorized
    JamFailure.HostEnded -> strings.jamHostEnded
    JamFailure.ConnectionFailed,
    JamFailure.ProtocolError -> strings.jamConnectionFailed
}
