package com.luc4n3x.levyra.ui.jam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.zxing.common.BitMatrix
import com.luc4n3x.levyra.R
import com.luc4n3x.levyra.feature.jam.JamInvitationQr
import com.luc4n3x.levyra.ui.components.LevyraBackButton
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.jamCopyInvite
import com.luc4n3x.levyra.ui.i18n.jamInviteCopied
import com.luc4n3x.levyra.ui.i18n.jamQrUnavailable
import com.luc4n3x.levyra.ui.i18n.jamScanToJoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun JamQrShare(invitation: String, sessionName: String, hostName: String, onShare: (String) -> Unit, onClose: () -> Unit) {
    val strings = LocalLevyraStrings.current
    val look = LocalJamLook.current
    val scheme = MaterialTheme.colorScheme
    val background = if (look.skinned) look.background else scheme.surface
    val foreground = if (look.skinned) look.text else scheme.onSurface
    val secondary = if (look.skinned) look.textSecondary else scheme.onSurfaceVariant
    val accent = if (look.skinned) look.accent else scheme.primary
    val onAccent = if (look.skinned) look.onAccent else scheme.onPrimary
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember(invitation) { mutableStateOf(false) }
    val qr by produceState<Result<BitMatrix>?>(null, invitation) {
        value = withContext(Dispatchers.Default) { runCatching { JamInvitationQr.encode(invitation) } }
    }
    BackHandler(onBack = onClose)
    Column(
        Modifier.fillMaxSize().background(background)
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.16f), Color.Transparent, accent.copy(alpha = 0.04f))))
            .safeDrawingPadding().verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {
            LevyraBackButton(onClick = onClose, contentDescription = strings.back, size = 48.dp)
            Image(painterResource(R.drawable.levyra_logo), contentDescription = null, modifier = Modifier.align(Alignment.CenterEnd).size(56.dp))
        }
        Column(Modifier.widthIn(max = 400.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(sessionName, color = foreground, style = MaterialTheme.typography.headlineLarge, fontWeight = look.displayWeight,
                textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
            if (hostName.isNotBlank()) Text(hostName, color = secondary, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        }
        JamQrCard(qr, foreground, secondary, accent)
        Column(Modifier.widthIn(max = 400.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onShare(invitation) }, shape = look.buttonShape,
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = onAccent),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(Icons.Rounded.IosShare, null, Modifier.padding(end = 12.dp))
                Text(strings.jamShareInvite)
            }
            OutlinedButton(onClick = {
                scope.launch {
                    clipboard.setClipEntry(ClipEntry(android.content.ClipData.newPlainText(strings.jamTitle, invitation)))
                    copied = true
                }
            }, shape = look.buttonShape, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Icon(Icons.Rounded.ContentCopy, null, Modifier.padding(end = 12.dp), tint = foreground)
                Text(if (copied) strings.jamInviteCopied else strings.jamCopyInvite, color = foreground)
            }
        }
    }
}

@Composable
private fun JamQrCard(qr: Result<BitMatrix>?, foreground: Color, secondary: Color, accent: Color) {
    val strings = LocalLevyraStrings.current
    val look = LocalJamLook.current
    val scheme = MaterialTheme.colorScheme
    val shape = if (look.skinned) look.cardShape else MaterialTheme.shapes.extraLarge
    Surface(shape = shape, color = if (look.skinned) look.card else scheme.surfaceContainerLow,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.2f)), modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth()) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Surface(shape = MaterialTheme.shapes.large, color = Color.White) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp), contentAlignment = Alignment.Center) {
                    val matrix = qr?.getOrNull()
                    when {
                        matrix != null -> JamQrImage(matrix, strings.jamScanToJoin)
                        qr == null -> CircularProgressIndicator(color = Color.Black)
                        else -> Text(strings.jamQrUnavailable, color = Color.Black, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            Text(strings.jamScanToJoin, color = foreground, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(strings.jamLocalNetworkOnly, color = secondary, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun JamQrImage(matrix: BitMatrix, description: String) {
    Canvas(Modifier.fillMaxSize().semantics { contentDescription = description }) {
        val module = (size.minDimension / matrix.width).toInt().coerceAtLeast(1).toFloat()
        val left = ((size.width - matrix.width * module) / 2).toInt().toFloat()
        val top = ((size.height - matrix.height * module) / 2).toInt().toFloat()
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    val finder = (x < 12 && y < 12) || (x >= matrix.width - 12 && y < 12) || (x < 12 && y >= matrix.height - 12)
                    if (finder) drawRect(Color.Black, Offset(left + x * module, top + y * module), Size(module, module))
                    else drawRoundRect(Color.Black, Offset(left + x * module, top + y * module), Size(module, module), CornerRadius(module * 0.22f))
                }
            }
        }
    }
}
