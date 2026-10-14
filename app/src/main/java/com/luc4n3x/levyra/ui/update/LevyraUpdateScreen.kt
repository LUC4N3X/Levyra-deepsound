package com.luc4n3x.levyra.ui.update

import com.luc4n3x.levyra.ui.components.LevyraBackButton
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import com.luc4n3x.levyra.domain.AppUpdateInfo
import com.luc4n3x.levyra.ui.LevyraLogoMark
import com.luc4n3x.levyra.ui.LocalAnimationsEnabled
import com.luc4n3x.levyra.ui.components.levyraGroupedListShape
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LevyraUpdateReleaseCopy
import com.luc4n3x.levyra.ui.i18n.updateReleaseCopy
import com.luc4n3x.levyra.ui.i18n.updateReleaseHighlights
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraPanel
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.update.formatUpdateBytes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val UpdateBadgeSpinMillis = 24_000

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LevyraUpdateScreen(
    update: AppUpdateInfo,
    strings: LevyraStrings,
    languageCode: String,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = strings.updateReleaseCopy()
    val notes = remember(strings.code, update.latestVersionName) {
        strings.updateReleaseHighlights(update.latestVersionName).take(5)
    }
    val meta = remember(update.publishedAtEpochMs, update.assetSizeBytes, languageCode) {
        updateMetaLine(update.publishedAtEpochMs, update.assetSizeBytes, languageCode)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevyraBlack)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(LevyraCyan.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.04f),
                        radius = size.maxDimension * 0.6f
                    )
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LevyraBackButton(onClick = onLater, contentDescription = strings.later)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            UpdateBadge()
            Spacer(Modifier.height(28.dp))
            Text(
                text = copy.releaseReady,
                color = LevyraCyan,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = update.latestVersionName,
                color = LevyraText,
                fontSize = 64.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(64.sp),
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (meta.isNotBlank()) {
                Text(meta, color = LevyraMuted, fontSize = 14.sp)
            }
            Spacer(Modifier.height(22.dp))
            VersionRunway(
                current = update.currentVersionName,
                latest = update.latestVersionName,
                copy = copy
            )
            if (notes.isNotEmpty()) {
                Spacer(Modifier.height(30.dp))
                UpdateHighlights(title = copy.releaseHighlights, notes = notes)
            }
            Spacer(Modifier.height(22.dp))
            UpdateProtectionCard(title = copy.releaseProtection, detail = copy.releaseProtectionDetail)
            Spacer(Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onUpdate,
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(containerColor = LevyraCyan, contentColor = LevyraOnAccent),
                contentPadding = PaddingValues(horizontal = 28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(strings.update, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
            TextButton(
                onClick = onLater,
                shapes = ButtonDefaults.shapes(),
                modifier = Modifier.height(48.dp)
            ) {
                Text(strings.later, color = LevyraMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateBadge() {
    val animated = LocalAnimationsEnabled.current
    val bloom = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(animated) {
        if (animated) bloom.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 160f)) else bloom.snapTo(1f)
    }
    val spin = if (animated) {
        rememberInfiniteTransition(label = "update-badge").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(UpdateBadgeSpinMillis, easing = LinearEasing), RepeatMode.Restart),
            label = "update-badge-spin"
        )
    } else {
        null
    }
    Box(modifier = Modifier.size(128.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(128.dp)
                .graphicsLayer {
                    val value = bloom.value
                    scaleX = 0.6f + 0.4f * value
                    scaleY = 0.6f + 0.4f * value
                    alpha = value.coerceIn(0f, 1f)
                    rotationZ = -(spin?.value ?: 0f) - 50f * (1f - value)
                }
                .border(1.5.dp, LevyraCyan.copy(alpha = 0.45f), MaterialShapes.SoftBurst.toShape())
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer {
                    val value = bloom.value
                    scaleX = 0.7f + 0.3f * value
                    scaleY = 0.7f + 0.3f * value
                    rotationZ = (spin?.value ?: 0f) * 1.4f + 70f * (1f - value)
                }
                .background(
                    Brush.linearGradient(listOf(LevyraCyan.copy(alpha = 0.22f), LevyraViolet.copy(alpha = 0.22f))),
                    MaterialShapes.Cookie12Sided.toShape()
                )
        )
        Box(
            modifier = Modifier.graphicsLayer {
                val value = bloom.value
                scaleX = 0.75f + 0.25f * value
                scaleY = 0.75f + 0.25f * value
                alpha = value.coerceIn(0f, 1f)
            }
        ) {
            LevyraLogoMark(size = 72.dp, contentDescription = null)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(40.dp)
                .graphicsLayer {
                    val value = ((bloom.value - 0.4f) / 0.6f).coerceIn(0f, 1.2f)
                    scaleX = value
                    scaleY = value
                }
                .background(LevyraBlack, MaterialShapes.Cookie9Sided.toShape())
                .padding(3.dp)
                .background(LevyraCyan, MaterialShapes.Cookie9Sided.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null, tint = LevyraOnAccent, modifier = Modifier.size(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VersionRunway(current: String, latest: String, copy: LevyraUpdateReleaseCopy) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        VersionCell(
            label = copy.releaseFrom,
            version = current,
            container = LevyraPanel,
            labelColor = LevyraMuted,
            shape = RoundedCornerShape(topStart = 26.dp, bottomStart = 26.dp, topEnd = 8.dp, bottomEnd = 8.dp),
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(LevyraPanel, MaterialShapes.Cookie4Sided.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
        }
        VersionCell(
            label = copy.releaseTo,
            version = latest,
            container = LevyraCyan.copy(alpha = 0.16f),
            labelColor = LevyraCyan,
            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 26.dp, bottomEnd = 26.dp),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun VersionCell(
    label: String,
    version: String,
    container: Color,
    labelColor: Color,
    shape: Shape,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .background(container, shape)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Text(label, color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(version, color = LevyraText, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val UpdateNotePolygons: List<RoundedPolygon> by lazy {
    listOf(
        MaterialShapes.Cookie4Sided,
        MaterialShapes.Clover4Leaf,
        MaterialShapes.Sunny,
        MaterialShapes.Cookie6Sided,
        MaterialShapes.Flower
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateHighlights(title: String, notes: List<String>) {
    Column {
        Text(
            text = title,
            color = LevyraText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .semantics { heading() }
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            notes.forEachIndexed { index, note ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LevyraPanel, levyraGroupedListShape(index, notes.size))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(LevyraCyan.copy(alpha = 0.16f), UpdateNotePolygons[index % UpdateNotePolygons.size].toShape()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text((index + 1).toString(), color = LevyraCyan, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                    Text(
                        text = note,
                        color = LevyraText.copy(alpha = 0.86f),
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateProtectionCard(title: String, detail: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LevyraCyan.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
            .border(1.dp, LevyraGlassBorder, RoundedCornerShape(28.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(LevyraCyan, MaterialShapes.Clover4Leaf.toShape()),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = LevyraOnAccent, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = LevyraText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(detail, color = LevyraMuted, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

internal fun updateMetaLine(publishedAtEpochMs: Long, assetSizeBytes: Long, languageCode: String): String {
    val parts = mutableListOf<String>()
    formatUpdateReleaseDate(publishedAtEpochMs, languageCode)?.let(parts::add)
    if (assetSizeBytes > 0L) parts += formatUpdateBytes(assetSizeBytes)
    return parts.joinToString(separator = " · ")
}

internal fun formatUpdateReleaseDate(publishedAtEpochMs: Long, languageCode: String): String? {
    if (publishedAtEpochMs <= 0L) return null
    return runCatching {
        val locale = Locale.forLanguageTag(languageCode.ifBlank { "en" })
        DateTimeFormatter.ofPattern("d MMM yyyy", locale)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(publishedAtEpochMs))
    }.getOrNull()
}
