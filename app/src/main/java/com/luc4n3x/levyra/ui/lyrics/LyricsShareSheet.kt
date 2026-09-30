package com.luc4n3x.levyra.ui.lyrics

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val PREVIEW_ASPECT = 4f / 5f
private val PREVIEW_MAX_HEIGHT = 420.dp

private class PreparedShareArtwork(val artwork: Bitmap?, val accents: Pair<Int, Int>)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun LyricsShareSheet(
    snapshot: LyricsShareSnapshot,
    onDismiss: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var style by rememberSaveable { mutableStateOf(LyricsShareCardStyle.ARTWORK) }
    var mode by remember(snapshot) { mutableStateOf(LyricsShareTextMode.ORIGINAL) }
    var prepared by remember(snapshot) { mutableStateOf<PreparedShareArtwork?>(null) }
    var preview by remember(snapshot) { mutableStateOf<ImageBitmap?>(null) }
    var previewFailed by remember(snapshot) { mutableStateOf(false) }
    var exportRequest by remember(snapshot) { mutableIntStateOf(0) }
    var exporting by remember(snapshot) { mutableStateOf(false) }
    var exportFailed by remember(snapshot) { mutableStateOf(false) }
    val content = remember(snapshot, mode) { lyricsShareCardContent(snapshot, mode) }

    LaunchedEffect(snapshot) {
        val artwork = LyricsShareCard.loadArtwork(context, snapshot.track)
        val accents = withContext(Dispatchers.Default) {
            LyricsShareCard.resolveAccents(snapshot.track, artwork)
        }
        prepared = PreparedShareArtwork(artwork, accents)
    }

    LaunchedEffect(content, style, prepared) {
        val ready = prepared ?: return@LaunchedEffect
        val bitmap = withContext(Dispatchers.Default) {
            LyricsShareCard.render(
                content = content,
                artwork = ready.artwork,
                style = style,
                accents = ready.accents,
                widthPx = LyricsShareCard.PREVIEW_WIDTH_PX
            )
        }
        previewFailed = bitmap == null
        if (bitmap != null) preview = bitmap.asImageBitmap()
    }

    LaunchedEffect(exportRequest) {
        if (exportRequest == 0) return@LaunchedEffect
        val ready = prepared ?: return@LaunchedEffect
        exporting = true
        exportFailed = false
        try {
            val intent = LyricsShareCard.createShareIntent(
                context = context,
                content = content,
                artwork = ready.artwork,
                style = style,
                accents = ready.accents
            )
            exportFailed = intent == null || !startChooser(context, intent, strings.shareVia)
        } finally {
            exporting = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = strings.shareLyricsPreviewTitle,
                color = LevyraText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            SharePreview(
                preview = preview,
                failed = previewFailed,
                description = strings.shareLyricsPreviewDescription,
                failedText = strings.shareLyricsFailed
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LyricsShareCardStyle.entries.forEach { option ->
                    ShareOptionChip(
                        label = styleLabel(strings, option),
                        selected = style == option,
                        onClick = { style = option }
                    )
                }
            }
            if (snapshot.availableModes.size > 1) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    snapshot.availableModes.forEach { option ->
                        ShareOptionChip(
                            label = modeLabel(strings, option),
                            selected = mode == option,
                            onClick = { mode = option }
                        )
                    }
                }
            }
            if (exportFailed) {
                Text(
                    text = strings.shareLyricsFailed,
                    color = LevyraPink,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Button(
                onClick = { exportRequest += 1 },
                enabled = prepared != null && !exporting && content.lyrics.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LevyraCyan,
                    contentColor = LevyraOnAccent
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                if (exporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = LevyraOnAccent
                    )
                } else {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Text(
                    text = if (exporting) strings.shareLyricsPreparing else strings.shareLyricsShareImage,
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun SharePreview(
    preview: ImageBitmap?,
    failed: Boolean,
    description: String,
    failedText: String
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .heightIn(max = PREVIEW_MAX_HEIGHT)
                .aspectRatio(PREVIEW_ASPECT)
                .clip(RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                preview != null -> Image(
                    bitmap = preview,
                    contentDescription = description,
                    modifier = Modifier.fillMaxSize()
                )
                failed -> Text(
                    text = failedText,
                    color = LevyraMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
                else -> CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp,
                    color = LevyraCyan
                )
            }
        }
    }
}

@Composable
private fun ShareOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) LevyraCyan.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) LevyraCyan else Color.White.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (selected) LevyraText else LevyraMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

private fun startChooser(context: android.content.Context, intent: Intent, title: String): Boolean = try {
    context.startActivity(Intent.createChooser(intent, title))
    true
} catch (_: ActivityNotFoundException) {
    false
}

private fun styleLabel(strings: LevyraStrings, style: LyricsShareCardStyle): String = when (style) {
    LyricsShareCardStyle.ARTWORK -> strings.shareLyricsStyleArtwork
    LyricsShareCardStyle.GRADIENT -> strings.shareLyricsStyleGradient
    LyricsShareCardStyle.MINIMAL -> strings.shareLyricsStyleMinimal
}

private fun modeLabel(strings: LevyraStrings, mode: LyricsShareTextMode): String = when (mode) {
    LyricsShareTextMode.ORIGINAL -> strings.shareLyricsTextOriginal
    LyricsShareTextMode.TRANSLATION -> strings.shareLyricsTextTranslation
    LyricsShareTextMode.ROMANIZATION -> strings.lyricsRomanization
}
