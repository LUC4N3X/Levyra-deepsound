package com.luc4n3x.levyra.desktop.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.luc4n3x.levyra.desktop.app.ui.components.Artwork
import com.luc4n3x.levyra.desktop.app.ui.components.SectionTints
import com.luc4n3x.levyra.desktop.app.ui.icons.LevyraIcons
import com.luc4n3x.levyra.desktop.app.ui.icons.OfflineIcons
import com.luc4n3x.levyra.desktop.core.account.BrowserCookieImport
import com.luc4n3x.levyra.desktop.core.account.BrowserImportException
import com.luc4n3x.levyra.desktop.core.account.BrowserImportFailure
import com.luc4n3x.levyra.desktop.core.account.BrowserKind
import com.luc4n3x.levyra.desktop.core.account.BrowserProfile
import com.luc4n3x.levyra.desktop.core.account.BrowserProfileStatus
import com.luc4n3x.levyra.desktop.core.account.DesktopYoutubeMusicAccount
import com.luc4n3x.levyra.desktop.core.account.DesktopYoutubeMusicAccounts
import com.luc4n3x.levyra.desktop.core.account.LinkRejection
import com.luc4n3x.levyra.desktop.core.account.QrMatrix
import com.luc4n3x.levyra.desktop.core.account.YoutubeMusicLinkHost
import com.luc4n3x.levyra.desktop.core.account.YoutubeMusicLinkState
import com.luc4n3x.levyra.ui.i18n.YoutubeMusicLinkCopy
import com.luc4n3x.levyra.ui.i18n.YoutubeMusicLinkText
import com.luc4n3x.levyra.ui.i18n.youtubeMusicLinkCopy
import com.luc4n3x.levyra.ui.i18n.LevyraStrings as SharedStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val QrInk = Color(0xFF0B0E14)
private val AccountGlow = Color(0xFFFF6B6B)

@Composable
internal fun YoutubeMusicAccountPage(
    accounts: DesktopYoutubeMusicAccounts,
    browserImport: BrowserCookieImport,
    languageCode: String,
    playlistSync: com.luc4n3x.levyra.desktop.core.account.DesktopPlaylistSync? = null
) {
    val copy = remember(languageCode) { SharedStrings.forCode(languageCode).youtubeMusicLinkCopy }
    val signedIn by accounts.accounts.collectAsState()
    var phoneOpen by remember { mutableStateOf(false) }
    var browserOpen by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf<DesktopYoutubeMusicAccount?>(null) }
    val scope = rememberCoroutineScope()

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        val activeAccount = signedIn.firstOrNull { it.active }
        AccountHero(copy, activeAccount)
        if (activeAccount != null && playlistSync != null) YoutubeMusicSyncablePlaylists(playlistSync, activeAccount.id, languageCode)
        if (signedIn.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.74f))
                    .padding(vertical = 8.dp)
            ) {
                signedIn.forEach { account ->
                    AccountRow(
                        account = account,
                        copy = copy,
                        onUse = { scope.launch { accounts.switchTo(account.id) } },
                        onSignOut = { leaving = account }
                    )
                }
            }
            Text(copy[YoutubeMusicLinkText.AddAccount], style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 6.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MethodCard(
                title = copy[YoutubeMusicLinkText.PhoneMethod],
                body = copy[YoutubeMusicLinkText.PhoneMethodDesc],
                badge = copy[YoutubeMusicLinkText.Recommended],
                action = copy[YoutubeMusicLinkText.SignIn],
                glyph = { PhoneGlyph(SectionTints.Rose, Modifier.size(22.dp)) },
                tint = SectionTints.Rose,
                modifier = Modifier.weight(1f),
                onClick = { phoneOpen = true }
            )
            MethodCard(
                title = copy[YoutubeMusicLinkText.BrowserMethod],
                body = copy[YoutubeMusicLinkText.BrowserMethodDesc],
                badge = null,
                action = copy[YoutubeMusicLinkText.SignIn],
                glyph = { GlobeGlyph(SectionTints.Periwinkle, Modifier.size(22.dp)) },
                tint = SectionTints.Periwinkle,
                modifier = Modifier.weight(1f),
                onClick = { browserOpen = true }
            )
        }
    }

    if (phoneOpen) PhoneLinkDialog(accounts, copy) { phoneOpen = false }
    if (browserOpen) BrowserImportDialog(accounts, browserImport, copy) { browserOpen = false }
    leaving?.let { account ->
        ConfirmSignOutDialog(account, copy, onDismiss = { leaving = null }) {
            leaving = null
            scope.launch { accounts.signOut(account.id) }
        }
    }
}

@Composable
private fun AccountHero(copy: YoutubeMusicLinkCopy, active: DesktopYoutubeMusicAccount?) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    listOf(AccountGlow.copy(alpha = 0.20f), SectionTints.Lavender.copy(alpha = 0.10f), MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.74f))
                )
            )
            .padding(26.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
            if (active != null && active.avatarUrl.isNotBlank()) {
                Artwork(active.avatarUrl, Modifier.size(72.dp), cornerRadius = 36.dp)
            } else {
                Box(
                    Modifier.size(72.dp).clip(CircleShape)
                        .background(Brush.linearGradient(listOf(AccountGlow, SectionTints.Lavender))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(LevyraIcons.Play, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "YouTube Music",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.2.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (active != null) active.name else copy[YoutubeMusicLinkText.SignedOutTitle],
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (active != null) copy[YoutubeMusicLinkText.Subtitle] else copy[YoutubeMusicLinkText.SignedOutBody],
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountRow(account: DesktopYoutubeMusicAccount, copy: YoutubeMusicLinkCopy, onUse: () -> Unit, onSignOut: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Artwork(account.avatarUrl, Modifier.size(44.dp), cornerRadius = 22.dp, iconSize = 18.dp)
        Column(Modifier.weight(1f)) {
            Text(account.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (account.handle.isNotBlank()) {
                Text(account.handle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        if (account.active) {
            Text(
                copy[YoutubeMusicLinkText.Active],
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        } else {
            OutlinedButton(onClick = onUse) { Text(copy[YoutubeMusicLinkText.UseAccount]) }
        }
        TextButton(onClick = onSignOut) { Text(copy[YoutubeMusicLinkText.SignOut]) }
    }
}

@Composable
private fun MethodCard(
    title: String,
    body: String,
    badge: String?,
    action: String,
    glyph: @Composable () -> Unit,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val lift by animateFloatAsState(if (hovered) 1.015f else 1f, tween(180))
    val surface by animateColorAsState(
        if (hovered) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.74f),
        tween(180)
    )
    val nudge by animateFloatAsState(if (hovered) 4f else 0f, tween(180))
    Column(
        modifier.scale(lift).clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(tint.copy(alpha = if (hovered) 0.14f else 0.08f), surface, surface)))
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                glyph()
            }
            Spacer(Modifier.weight(1f))
            if (badge != null) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = tint,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(tint.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, minLines = 2)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
            Text(action, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = tint)
            Icon(
                LevyraIcons.ChevronLeft,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = 180f; translationX = nudge }
            )
        }
    }
}

@Composable
private fun PhoneGlyph(tint: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.1f
        val width = size.width * 0.56f
        val left = (size.width - width) / 2f
        drawRoundRect(tint, Offset(left, stroke / 2f), Size(width, size.height - stroke), CornerRadius(width * 0.24f), style = Stroke(stroke))
        drawLine(tint, Offset(size.width / 2f - width * 0.14f, size.height * 0.80f), Offset(size.width / 2f + width * 0.14f, size.height * 0.80f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun GlobeGlyph(tint: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.09f
        val radius = size.minDimension / 2f - stroke
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(tint, radius, center, style = Stroke(stroke))
        drawOval(tint, Offset(center.x - radius * 0.45f, center.y - radius), Size(radius * 0.9f, radius * 2f), style = Stroke(stroke))
        drawLine(tint, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), stroke)
    }
}

@Composable
private fun PhoneLinkDialog(accounts: DesktopYoutubeMusicAccounts, copy: YoutubeMusicLinkCopy, onClose: () -> Unit) {
    val host = remember { accounts.newLinkHost() }
    val scope = rememberCoroutineScope()
    DisposableEffect(host) { onDispose { host.dispose() } }
    LaunchedEffect(host) { withContext(Dispatchers.IO) { host.start() } }
    val state by host.state.collectAsState()
    LaunchedEffect(state) {
        if (state is YoutubeMusicLinkState.Done) {
            delay(SUCCESS_HOLD_MS)
            onClose()
        }
    }
    LinkDialogFrame(onClose) {
        PhoneLinkContent(state, copy, onNewCode = { scope.launch(Dispatchers.IO) { host.start() } }, onClose = onClose)
    }
}

@Composable
internal fun PhoneLinkContent(state: YoutubeMusicLinkState, copy: YoutubeMusicLinkCopy, onNewCode: () -> Unit, onClose: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(30.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            QrPanel(state, copy)
            ExpiryChip(state, copy)
        }
        Column(Modifier.width(330.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(copy[YoutubeMusicLinkText.QrTitle], style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            val step = when (state) {
                is YoutubeMusicLinkState.Waiting -> 2
                YoutubeMusicLinkState.PhoneConnected, YoutubeMusicLinkState.SigningIn -> 3
                is YoutubeMusicLinkState.Done -> 4
                else -> 0
            }
            StepRow(1, copy[YoutubeMusicLinkText.QrStep1], step)
            StepRow(2, copy[YoutubeMusicLinkText.QrStep2], step)
            StepRow(3, copy[YoutubeMusicLinkText.QrStep3], step)
            StatusPill(state, copy)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                GlobeGlyph(MaterialTheme.colorScheme.onSurfaceVariant, Modifier.padding(top = 2.dp).size(15.dp))
                Text(copy[YoutubeMusicLinkText.FirewallHint], style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            (state as? YoutubeMusicLinkState.Waiting)?.addresses?.takeIf { it.isNotEmpty() }?.let { addresses ->
                Text(
                    addresses.joinToString("  ·  ") { it.ip },
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val restartable = state is YoutubeMusicLinkState.Expired || state is YoutubeMusicLinkState.Failed ||
                    state is YoutubeMusicLinkState.Rejected || state is YoutubeMusicLinkState.NoNetwork || state is YoutubeMusicLinkState.Waiting
                if (restartable) {
                    OutlinedButton(onClick = onNewCode) { Text(copy[YoutubeMusicLinkText.NewCode]) }
                }
                TextButton(onClick = onClose) { Text(copy[YoutubeMusicLinkText.Close]) }
            }
        }
    }
}

@Composable
private fun QrPanel(state: YoutubeMusicLinkState, copy: YoutubeMusicLinkCopy) {
    val code = (state as? YoutubeMusicLinkState.Waiting)?.code
    var lastCode by remember { mutableStateOf<String?>(null) }
    if (code != null) lastCode = code
    val matrix = remember(lastCode) { lastCode?.let(QrMatrix::encode) }
    val dimmed = state !is YoutubeMusicLinkState.Waiting
    val fade by animateFloatAsState(if (dimmed) 0.12f else 1f, tween(260))
    Box(
        Modifier.size(296.dp).clip(RoundedCornerShape(28.dp)).background(Color.White)
            .semantics { contentDescription = copy[YoutubeMusicLinkText.QrTitle] },
        contentAlignment = Alignment.Center
    ) {
        if (matrix != null) {
            QrCanvas(matrix, Modifier.size(248.dp).alpha(fade))
            if (!dimmed) ScanSweep(Modifier.size(248.dp))
        }
        AnimatedContent(
            targetState = qrOverlay(state),
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) }
        ) { overlay ->
            when (overlay) {
                QrOverlay.Loading -> CircularProgressIndicator(color = QrInk, strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                QrOverlay.Phone -> PulseBadge(null, SectionTints.Rose)
                QrOverlay.Done -> PulseBadge(OfflineIcons.Check, SectionTints.Mint)
                QrOverlay.Problem -> PulseBadge(LevyraIcons.Refresh, SectionTints.Amber)
                QrOverlay.None -> Spacer(Modifier.size(1.dp))
            }
        }
    }
}

private enum class QrOverlay { None, Loading, Phone, Done, Problem }

private fun qrOverlay(state: YoutubeMusicLinkState): QrOverlay = when (state) {
    is YoutubeMusicLinkState.Waiting -> QrOverlay.None
    YoutubeMusicLinkState.Idle, YoutubeMusicLinkState.Starting, YoutubeMusicLinkState.SigningIn -> QrOverlay.Loading
    YoutubeMusicLinkState.PhoneConnected -> QrOverlay.Phone
    is YoutubeMusicLinkState.Done -> QrOverlay.Done
    else -> QrOverlay.Problem
}

@Composable
private fun QrCanvas(matrix: QrMatrix, modifier: Modifier) {
    Canvas(modifier) {
        val cell = size.minDimension / matrix.size
        val dot = cell * 0.86f
        val inset = (cell - dot) / 2f
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (!matrix[x, y] || matrix.isFinder(x, y)) continue
                drawRoundRect(QrInk, Offset(x * cell + inset, y * cell + inset), Size(dot, dot), CornerRadius(dot * 0.32f))
            }
        }
        val finder = cell * QrMatrix.FINDER
        listOf(Offset(0f, 0f), Offset(size.width - finder, 0f), Offset(0f, size.height - finder)).forEach { origin ->
            drawRoundRect(QrInk, origin, Size(finder, finder), CornerRadius(cell * 1.8f))
            drawRoundRect(Color.White, origin + Offset(cell, cell), Size(finder - cell * 2, finder - cell * 2), CornerRadius(cell * 1.2f))
            drawRoundRect(QrInk, origin + Offset(cell * 2, cell * 2), Size(finder - cell * 4, finder - cell * 4), CornerRadius(cell * 0.9f))
        }
    }
}

@Composable
private fun ScanSweep(modifier: Modifier) {
    val sweep = rememberInfiniteTransition()
    val position by sweep.animateFloat(0f, 1f, infiniteRepeatable(tween(2_600, easing = LinearEasing), RepeatMode.Reverse))
    val glow = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val y = size.height * position
        val tail = 22.dp.toPx()
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, glow.copy(alpha = 0.10f)), startY = y - tail, endY = y),
            topLeft = Offset(0f, y - tail),
            size = Size(size.width, tail)
        )
        drawLine(
            Brush.horizontalGradient(listOf(Color.Transparent, glow.copy(alpha = 0.85f), Color.Transparent)),
            Offset(0f, y), Offset(size.width, y), 2.dp.toPx(), StrokeCap.Round
        )
    }
}

@Composable
private fun PulseBadge(icon: ImageVector?, tint: Color) {
    val pulse = rememberInfiniteTransition()
    val ring by pulse.animateFloat(0.85f, 1.15f, infiniteRepeatable(tween(1_100), RepeatMode.Reverse))
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(92.dp).scale(ring).clip(CircleShape).background(tint.copy(alpha = 0.22f)))
        Box(Modifier.size(64.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
            if (icon == null) PhoneGlyph(QrInk, Modifier.size(28.dp))
            else Icon(icon, contentDescription = null, tint = QrInk, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun ExpiryChip(state: YoutubeMusicLinkState, copy: YoutubeMusicLinkCopy) {
    val waiting = state as? YoutubeMusicLinkState.Waiting ?: return Spacer(Modifier.height(32.dp))
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(waiting.expiresAtMs) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    val remaining = (waiting.expiresAtMs - now).coerceAtLeast(0L)
    val fraction = remaining.toFloat() / YoutubeMusicLinkHost.SESSION_MS
    val seconds = remaining / 1_000
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(Modifier.size(16.dp)) {
            val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, false, style = stroke)
            drawArc(accent, -90f, 360f * fraction, false, style = stroke)
        }
        Text(
            copy.format(YoutubeMusicLinkText.QrExpires, time = "%d:%02d".format(seconds / 60, seconds % 60)),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StepRow(number: Int, text: String, current: Int) {
    val done = current > number
    val active = current == number
    val tint by animateColorAsState(
        when {
            done -> SectionTints.Mint
            active -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        },
        tween(220)
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
            if (done) Icon(OfflineIcons.Check, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            else Text(number.toString(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = tint)
        }
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal),
            color = if (active || done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusPill(state: YoutubeMusicLinkState, copy: YoutubeMusicLinkCopy) {
    val (text, tint) = when (state) {
        is YoutubeMusicLinkState.Waiting -> copy[YoutubeMusicLinkText.Waiting] to MaterialTheme.colorScheme.primary
        YoutubeMusicLinkState.PhoneConnected -> copy[YoutubeMusicLinkText.PhoneConnected] to SectionTints.Rose
        YoutubeMusicLinkState.SigningIn -> copy[YoutubeMusicLinkText.SigningIn] to SectionTints.Lavender
        is YoutubeMusicLinkState.Done -> copy.format(YoutubeMusicLinkText.SignedInAs, name = state.accounts.firstOrNull().orEmpty()) to SectionTints.Mint
        YoutubeMusicLinkState.Expired -> copy[YoutubeMusicLinkText.Expired] to SectionTints.Amber
        YoutubeMusicLinkState.NoNetwork -> copy[YoutubeMusicLinkText.NoNetwork] to MaterialTheme.colorScheme.error
        YoutubeMusicLinkState.Failed -> copy[YoutubeMusicLinkText.OpenFailed] to MaterialTheme.colorScheme.error
        is YoutubeMusicLinkState.Rejected -> copy[
            if (state.reason == LinkRejection.InvalidCode) YoutubeMusicLinkText.InvalidCode else YoutubeMusicLinkText.SignInFailed
        ] to MaterialTheme.colorScheme.error
        YoutubeMusicLinkState.Idle, YoutubeMusicLinkState.Starting -> copy[YoutubeMusicLinkText.Waiting] to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val live = state is YoutubeMusicLinkState.Waiting || state == YoutubeMusicLinkState.PhoneConnected || state == YoutubeMusicLinkState.SigningIn
    val blink = rememberInfiniteTransition()
    val dot by blink.animateFloat(0.35f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse))
    Row(
        Modifier.clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.12f)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(8.dp).alpha(if (live) dot else 1f).clip(CircleShape).background(tint))
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun BrowserImportDialog(
    accounts: DesktopYoutubeMusicAccounts,
    browserImport: BrowserCookieImport,
    copy: YoutubeMusicLinkCopy,
    onClose: () -> Unit
) {
    var profiles by remember { mutableStateOf<List<BrowserProfile>?>(null) }
    var busy by remember { mutableStateOf<String?>(null) }
    val errors = remember { mutableStateOf(mapOf<String, String>()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(browserImport) {
        profiles = runCatching { browserImport.profiles() }.getOrDefault(emptyList())
    }
    LinkDialogFrame(onClose) {
        Column(Modifier.width(560.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(copy[YoutubeMusicLinkText.BrowserTitle], style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            Text(copy[YoutubeMusicLinkText.BrowserBody], style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val found = profiles
            when {
                found == null -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                found.isEmpty() -> Text(
                    copy[YoutubeMusicLinkText.BrowserEmpty],
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(20.dp)
                )
                else -> Column(
                    Modifier.fillMaxWidth().heightIn(max = 360.dp).clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh).verticalScroll(rememberScrollState())
                ) {
                    found.forEach { profile ->
                        BrowserRow(
                            profile = profile,
                            copy = copy,
                            busy = busy == profile.id,
                            enabled = busy == null && profile.status == BrowserProfileStatus.Ready,
                            error = errors.value[profile.id]
                        ) {
                            busy = profile.id
                            scope.launch {
                                val message = try {
                                    val cookie = browserImport.cookieHeader(profile)
                                    if (accounts.signIn(cookie)) null else copy[YoutubeMusicLinkText.SignInFailed]
                                } catch (error: CancellationException) {
                                    throw error
                                } catch (error: BrowserImportException) {
                                    copy[if (error.failure == BrowserImportFailure.NotSignedIn) YoutubeMusicLinkText.BrowserNotSignedIn else YoutubeMusicLinkText.BrowserFailed]
                                } catch (_: Exception) {
                                    copy[YoutubeMusicLinkText.BrowserFailed]
                                }
                                busy = null
                                if (message == null) onClose() else errors.value = errors.value + (profile.id to message)
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onClose) { Text(copy[YoutubeMusicLinkText.Close]) }
            }
        }
    }
}

@Composable
private fun BrowserRow(
    profile: BrowserProfile,
    copy: YoutubeMusicLinkCopy,
    busy: Boolean,
    enabled: Boolean,
    error: String?,
    onImport: () -> Unit
) {
    val tint = browserTint(profile.kind)
    Row(
        Modifier.fillMaxWidth().alpha(if (profile.status == BrowserProfileStatus.Ready) 1f else 0.55f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            Text(profile.kind.label.take(1), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = tint)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(profile.kind.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(
                when {
                    error != null -> error
                    profile.status == BrowserProfileStatus.ProtectedOnWindows -> copy[YoutubeMusicLinkText.BrowserProtected]
                    else -> profile.name
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (busy) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        } else if (profile.status == BrowserProfileStatus.Ready) {
            Button(onClick = onImport, enabled = enabled) { Text(copy[YoutubeMusicLinkText.SignIn]) }
        }
    }
}

private fun browserTint(kind: BrowserKind): Color = when (kind) {
    BrowserKind.Firefox, BrowserKind.LibreWolf -> SectionTints.Amber
    BrowserKind.Chrome, BrowserKind.Chromium -> SectionTints.Sky
    BrowserKind.Edge -> SectionTints.Mint
    BrowserKind.Brave, BrowserKind.Opera -> SectionTints.Rose
    BrowserKind.Vivaldi -> SectionTints.Lavender
}

@Composable
private fun ConfirmSignOutDialog(account: DesktopYoutubeMusicAccount, copy: YoutubeMusicLinkCopy, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    LinkDialogFrame(onDismiss) {
        Column(Modifier.width(420.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("${copy[YoutubeMusicLinkText.SignOut]} · ${account.name}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(copy[YoutubeMusicLinkText.SignOutBody], style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                TextButton(onClick = onDismiss) { Text(copy[YoutubeMusicLinkText.Cancel]) }
                Button(onClick = onConfirm) { Text(copy[YoutubeMusicLinkText.SignOut]) }
            }
        }
    }
}

@Composable
private fun LinkDialogFrame(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 6.dp,
            shadowElevation = 24.dp
        ) {
            Box(Modifier.padding(32.dp)) { content() }
        }
    }
}

private const val SUCCESS_HOLD_MS = 1_600L
