package com.luc4n3x.levyra.ui.youtubemusic

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.luc4n3x.levyra.ui.i18n.YoutubeMusicLinkCopy
import com.luc4n3x.levyra.ui.i18n.YoutubeMusicLinkText
import com.luc4n3x.levyra.viewmodel.youtubemusic.ComputerLinkFailure
import com.luc4n3x.levyra.viewmodel.youtubemusic.ComputerLinkState
import kotlinx.coroutines.delay

@Composable
internal fun rememberComputerLinkScanner(prompt: String, onCode: (String) -> Unit, onUnavailable: () -> Unit): () -> Unit {
    val currentCode by rememberUpdatedState(onCode)
    val currentUnavailable by rememberUpdatedState(onUnavailable)
    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        when {
            result.originalIntent?.getBooleanExtra(Intents.Scan.MISSING_CAMERA_PERMISSION, false) == true -> currentUnavailable()
            result.contents != null -> currentCode(result.contents.take(MAX_SCANNED_LENGTH))
        }
    }
    return {
        try {
            launcher.launch(
                ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt(prompt).setBeepEnabled(false)
                    .setBarcodeImageEnabled(false).setOrientationLocked(false)
                    .addExtra(Intents.Scan.SHOW_MISSING_CAMERA_PERMISSION_DIALOG, false)
            )
        } catch (_: ActivityNotFoundException) {
            currentUnavailable()
        } catch (_: SecurityException) {
            currentUnavailable()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComputerLinkSheet(
    state: ComputerLinkState,
    copy: YoutubeMusicLinkCopy,
    onConfirm: () -> Unit,
    onScanAgain: () -> Unit,
    onDismiss: () -> Unit
) {
    if (state == ComputerLinkState.Idle) return
    LaunchedEffect(state) {
        if (state is ComputerLinkState.Done) {
            delay(DONE_HOLD_MS)
            onDismiss()
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            LinkIllustration(state)
            AnimatedContent(
                targetState = state,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                contentKey = { it::class }
            ) { current ->
                Column(
                    Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (current) {
                        ComputerLinkState.Connecting -> {
                            SheetTitle(copy[YoutubeMusicLinkText.Connecting])
                            LinearProgressIndicator(Modifier.fillMaxWidth(0.6f))
                        }
                        is ComputerLinkState.Confirm -> {
                            SheetTitle(copy.format(YoutubeMusicLinkText.ConfirmTitle, name = current.device.name))
                            if (current.device.os.isNotBlank()) DeviceChip(current.device.os)
                            SheetBody(copy[YoutubeMusicLinkText.ConfirmBody])
                        }
                        is ComputerLinkState.Sending -> {
                            SheetTitle(copy[YoutubeMusicLinkText.SigningIn])
                            LinearProgressIndicator(Modifier.fillMaxWidth(0.6f))
                        }
                        is ComputerLinkState.Done -> SheetTitle(copy.format(YoutubeMusicLinkText.SentTo, name = current.device.name))
                        is ComputerLinkState.Failed -> {
                            SheetTitle(copy[YoutubeMusicLinkText.SignInComputer])
                            SheetBody(copy[current.reason.text()], color = MaterialTheme.colorScheme.error)
                        }
                        ComputerLinkState.Idle -> Unit
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
                when (state) {
                    is ComputerLinkState.Confirm -> {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(copy[YoutubeMusicLinkText.Cancel]) }
                        Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text(copy[YoutubeMusicLinkText.SignIn]) }
                    }
                    is ComputerLinkState.Failed -> {
                        OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(copy[YoutubeMusicLinkText.Close]) }
                        if (state.reason != ComputerLinkFailure.NotSignedIn && state.reason != ComputerLinkFailure.CameraUnavailable) {
                            Button(onClick = onScanAgain, modifier = Modifier.weight(1f)) { Text(copy[YoutubeMusicLinkText.Retry]) }
                        }
                    }
                    ComputerLinkState.Connecting, is ComputerLinkState.Sending ->
                        OutlinedButton(onClick = onDismiss) { Text(copy[YoutubeMusicLinkText.Cancel]) }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun LinkIllustration(state: ComputerLinkState) {
    val scheme = MaterialTheme.colorScheme
    val (tint, centerIcon) = when (state) {
        is ComputerLinkState.Done -> scheme.tertiary to Icons.Rounded.CheckCircle
        is ComputerLinkState.Failed -> scheme.error to Icons.Rounded.ErrorOutline
        else -> scheme.primary to null
    }
    val flow = rememberInfiniteTransition()
    val phase by flow.animateFloat(0f, 1f, infiniteRepeatable(tween(1_200), RepeatMode.Restart))
    val moving = state == ComputerLinkState.Connecting || state is ComputerLinkState.Sending
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        DeviceBubble(Icons.Rounded.PhoneAndroid, scheme.secondaryContainer, scheme.onSecondaryContainer)
        Row(Modifier.width(76.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            if (centerIcon != null) {
                Icon(centerIcon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            } else {
                repeat(DOTS) { index ->
                    val lit = moving && ((phase * DOTS).toInt() == index)
                    Box(
                        Modifier.size(8.dp).scale(if (lit) 1.35f else 1f).alpha(if (lit || !moving) 1f else 0.35f)
                            .clip(CircleShape).background(tint)
                    )
                }
            }
        }
        DeviceBubble(Icons.Rounded.Computer, scheme.primaryContainer, scheme.onPrimaryContainer)
    }
}

@Composable
private fun DeviceBubble(icon: ImageVector, container: Color, content: Color) {
    Box(Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(container), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(34.dp))
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        textAlign = TextAlign.Center,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun SheetBody(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = color, textAlign = TextAlign.Center)
}

@Composable
private fun DeviceChip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

private fun ComputerLinkFailure.text(): YoutubeMusicLinkText = when (this) {
    ComputerLinkFailure.InvalidCode -> YoutubeMusicLinkText.ScanInvalid
    ComputerLinkFailure.Unreachable -> YoutubeMusicLinkText.Unreachable
    ComputerLinkFailure.Refused -> YoutubeMusicLinkText.Refused
    ComputerLinkFailure.SignInFailed -> YoutubeMusicLinkText.SignInFailed
    ComputerLinkFailure.NotSignedIn -> YoutubeMusicLinkText.NotSignedIn
    ComputerLinkFailure.CameraUnavailable -> YoutubeMusicLinkText.CameraUnavailable
}

private const val DOTS = 3
private const val DONE_HOLD_MS = 1_800L
private const val MAX_SCANNED_LENGTH = 1_024
