package com.luc4n3x.levyra.ui.update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.AppUpdateInfo
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.systemPlayerCopy
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

private val UpdateHeroShape = RoundedCornerShape(30.dp)
private val UpdateCardShape = RoundedCornerShape(22.dp)

@Composable
fun LevyraUpdateScreen(
    update: AppUpdateInfo,
    strings: LevyraStrings,
    languageCode: String,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val copy = strings.systemPlayerCopy()
    val notes = remember(update.releaseNotes, update.latestVersionName) {
        levyraUpdateNoteLines(update.releaseNotes, update.latestVersionName).take(5)
    }
    val meta = remember(update.publishedAtEpochMs, update.assetSizeBytes, languageCode) {
        updateMetaLine(update.publishedAtEpochMs, update.assetSizeBytes, languageCode)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevyraBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 20.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onLater,
                modifier = Modifier
                    .size(48.dp)
                    .background(LevyraPanel, CircleShape)
                    .border(1.dp, LevyraGlassBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings.later,
                    tint = LevyraText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = copy.releaseReady.uppercase(Locale.ROOT),
                color = LevyraMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            UpdateHero(update = update, meta = meta, copy = copy)

            if (notes.isNotEmpty()) {
                UpdateHighlights(
                    title = copy.releaseHighlights,
                    notes = notes
                )
            }

            UpdateProtectionCard(
                title = copy.releaseProtection,
                detail = copy.releaseProtectionDetail
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LevyraBlack.copy(alpha = 0.97f))
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onLater,
                modifier = Modifier
                    .weight(0.82f)
                    .height(54.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, LevyraGlassBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LevyraText)
            ) {
                Text(strings.later, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onUpdate,
                modifier = Modifier
                    .weight(1.18f)
                    .height(54.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LevyraCyan,
                    contentColor = LevyraOnAccent
                )
            ) {
                Icon(Icons.Rounded.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.size(8.dp))
                Text(strings.update, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun UpdateHero(
    update: AppUpdateInfo,
    meta: String,
    copy: com.luc4n3x.levyra.ui.i18n.LevyraSystemPlayerCopy
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        LevyraCyan.copy(alpha = 0.12f),
                        LevyraViolet.copy(alpha = 0.08f),
                        LevyraPanel.copy(alpha = 0.96f)
                    )
                ),
                shape = UpdateHeroShape
            )
            .border(1.dp, LevyraGlassBorder, UpdateHeroShape)
            .padding(horizontal = 20.dp, vertical = 22.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            UpdateOrbitGlyph()
            Spacer(Modifier.height(18.dp))
            Text(
                text = update.releaseTitle.ifBlank { copy.releaseReady },
                color = LevyraMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = update.latestVersionName,
                color = LevyraText,
                fontSize = 48.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(48.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (meta.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(meta, color = LevyraMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(20.dp))
            VersionRunway(
                current = update.currentVersionName,
                latest = update.latestVersionName,
                fromLabel = copy.releaseFrom,
                toLabel = copy.releaseTo
            )
        }
    }
}

@Composable
private fun UpdateOrbitGlyph() {
    Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radii = listOf(size.minDimension * 0.47f, size.minDimension * 0.36f)
            radii.forEachIndexed { index, radius ->
                drawCircle(
                    color = if (index == 0) LevyraViolet.copy(alpha = 0.16f) else LevyraCyan.copy(alpha = 0.24f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = if (index == 0) 1.dp.toPx() else 2.dp.toPx())
                )
            }
            drawArc(
                brush = Brush.sweepGradient(listOf(LevyraCyan, LevyraViolet, LevyraCyan)),
                startAngle = -80f,
                sweepAngle = 126f,
                useCenter = false,
                topLeft = Offset(size.width * 0.03f, size.height * 0.03f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.94f, size.height * 0.94f),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = LevyraBlack.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.28f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.SystemUpdateAlt,
                    contentDescription = null,
                    tint = LevyraCyan,
                    modifier = Modifier.size(27.dp)
                )
            }
        }
    }
}

@Composable
private fun VersionRunway(
    current: String,
    latest: String,
    fromLabel: String,
    toLabel: String
) {
    Surface(
        color = LevyraBlack.copy(alpha = 0.3f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, LevyraGlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            VersionCell(label = fromLabel, version = current, modifier = Modifier.weight(1f))
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = LevyraCyan,
                modifier = Modifier.size(21.dp)
            )
            VersionCell(label = toLabel, version = latest, modifier = Modifier.weight(1f), alignEnd = true)
        }
    }
}

@Composable
private fun VersionCell(
    label: String,
    version: String,
    modifier: Modifier,
    alignEnd: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Text(label.uppercase(Locale.ROOT), color = LevyraMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(version, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun UpdateHighlights(title: String, notes: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = LevyraCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(title, color = LevyraText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        notes.forEachIndexed { index, note ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = (index + 1).toString().padStart(2, '0'),
                    color = LevyraCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 3.dp)
                )
                Spacer(Modifier.size(13.dp))
                Text(
                    text = note,
                    color = LevyraMuted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun UpdateProtectionCard(title: String, detail: String) {
    Surface(
        color = LevyraCyan.copy(alpha = 0.055f),
        shape = UpdateCardShape,
        border = BorderStroke(1.dp, LevyraCyan.copy(alpha = 0.16f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = LevyraCyan.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = LevyraCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.size(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = LevyraText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(detail, color = LevyraMuted, fontSize = 11.sp, lineHeight = 16.sp)
            }
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
