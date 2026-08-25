package com.luc4n3x.levyra.ui.update

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.AppUpdateInfo
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraGlassBorder
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.update.formatUpdateBytes
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm

@Composable
fun LevyraUpdateScreen(
    update: AppUpdateInfo,
    strings: LevyraStrings,
    languageCode: String,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notes = remember(update.releaseNotes, update.latestVersionName) {
        levyraUpdateNoteLines(update.releaseNotes, update.latestVersionName)
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
        IconButton(
            onClick = onLater,
            modifier = Modifier
                .padding(start = 12.dp, top = 8.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(LevyraGlassBorder)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = strings.later,
                tint = LevyraText,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = strings.newUpdate,
                color = LevyraMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = update.latestVersionName,
                color = LevyraText,
                fontSize = 56.sp,
                lineHeight = LevyraTypeRhythm.lineHeight(56.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Brush.horizontalGradient(listOf(LevyraCyan, LevyraViolet)))
            )
            if (meta.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = meta, color = LevyraMuted, fontSize = 13.sp)
            }

            if (notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(34.dp))
                Text(
                    text = strings.whatsNew,
                    color = LevyraText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(14.dp))
                notes.forEach { note ->
                    Row(modifier = Modifier.padding(bottom = 14.dp)) {
                        Box(
                            modifier = Modifier
                                .padding(top = 3.dp)
                                .width(2.dp)
                                .height(17.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(LevyraCyan)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = note,
                            color = LevyraMuted,
                            fontSize = 14.sp,
                            lineHeight = 21.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onLater,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = CircleShape,
                border = BorderStroke(1.dp, LevyraGlassBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LevyraText)
            ) {
                Text(text = strings.later, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            Button(
                onClick = onUpdate,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LevyraCyan,
                    contentColor = LevyraOnAccent
                )
            ) {
                Text(text = strings.update, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
