package com.luc4n3x.levyra.ui.insights

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.ListeningInsightsPeriod
import com.luc4n3x.levyra.ui.i18n.LevyraStrings
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraText
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val RECEIPT_ASPECT = 4f / 5f
private const val RECEIPT_BRAND = "LEVYRA"
private val ReceiptOnAccent = Color(0xFF07080C)

private enum class ReceiptAction { Share, Save }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListeningReceiptSheet(
    state: ListeningInsightsUiState,
    accent: Color,
    onSelectPeriod: (ListeningInsightsPeriod) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val locale = remember(strings.code) { Locale.forLanguageTag(strings.code) }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val content = remember(state.snapshot, strings, locale) {
        ListeningReceiptModel.content(
            snapshot = state.snapshot,
            labels = receiptLabels(strings, strings.receiptTitle, periodLongLabel(state.snapshot.period, strings), locale),
            locale = locale,
            nowMs = System.currentTimeMillis(),
            zone = ZoneId.systemDefault()
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(text = strings.receiptTitle, color = LevyraText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(
                    text = strings.receiptEntrySubtitle,
                    color = LevyraMuted,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            InsightsPeriodSelector(
                selected = state.period,
                strings = strings,
                accent = accent,
                onSelect = onSelectPeriod
            )
            ListeningReceiptComposer(
                content = content,
                loading = state.loading,
                accent = accent,
                textColor = LevyraText,
                mutedColor = LevyraMuted
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ListeningReceiptComposer(
    content: ListeningReceiptContent?,
    loading: Boolean,
    accent: Color,
    textColor: Color,
    mutedColor: Color,
    previewMaxHeight: Dp = 470.dp
) {
    val strings = LocalLevyraStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var style by rememberSaveable { mutableStateOf(ListeningReceiptStyle.Aurora) }
    val artworkUrls = content?.artworkUrls.orEmpty()
    val artistUrl = content?.topArtistArtworkUrl.orEmpty()
    val assets = remember(artworkUrls, artistUrl) { listeningReceiptCovers() }
    var palette by remember(assets) { mutableStateOf<ListeningReceiptPalette?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var renderFailed by remember { mutableStateOf(false) }
    var rendering by remember { mutableStateOf(false) }
    var action by remember { mutableStateOf<ReceiptAction?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }

    DisposableEffect(assets) {
        onDispose { assets.close() }
    }

    LaunchedEffect(assets) {
        ListeningReceiptCard.loadAssets(context, artworkUrls, artistUrl, assets)
        palette = withContext(Dispatchers.Default) { assets.use { ListeningReceiptCard.resolvePalette(it) } }
    }

    LaunchedEffect(content, style, palette, assets) {
        val current = content ?: return@LaunchedEffect
        val resolved = palette ?: return@LaunchedEffect
        rendering = true
        renderFailed = false
        try {
            val bitmap = withContext(Dispatchers.Default) {
                assets.use {
                    ListeningReceiptCard.render(current, it, style, resolved, ListeningReceiptCard.PREVIEW_WIDTH_PX)
                }
            }
            if (bitmap == null) renderFailed = true else preview = bitmap
        } finally {
            rendering = false
        }
    }

    fun runAction(kind: ReceiptAction) {
        val current = content ?: return
        val resolved = palette ?: return
        if (action != null) return
        action = kind
        message = null
        scope.launch {
            try {
                val outcome = performReceiptAction(kind, context, current, assets, style, resolved, strings)
                message = outcome?.first
                messageIsError = outcome?.second == true
            } finally {
                action = null
            }
        }
    }

    val ready = content != null && palette != null && action == null

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ReceiptPreview(
            preview = preview?.takeIf { content != null && !it.isRecycled },
            busy = loading || rendering || (content != null && palette == null),
            failed = renderFailed,
            empty = content == null && !loading,
            strings = strings,
            accent = accent,
            mutedColor = mutedColor,
            maxHeight = previewMaxHeight
        )
        FlowRow(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ListeningReceiptStyle.entries.forEach { option ->
                ReceiptStyleChip(
                    label = styleLabel(strings, option),
                    selected = style == option,
                    accent = accent,
                    textColor = textColor,
                    mutedColor = mutedColor,
                    onClick = { style = option }
                )
            }
        }
        message?.let { text ->
            Text(
                text = text,
                color = if (messageIsError) LevyraPink else accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
        ReceiptActionRow(
            enabled = ready,
            busy = action,
            accent = accent,
            strings = strings,
            onAction = { runAction(it) }
        )
    }
}

private suspend fun performReceiptAction(
    kind: ReceiptAction,
    context: Context,
    content: ListeningReceiptContent,
    assets: ListeningReceiptCovers,
    style: ListeningReceiptStyle,
    palette: ListeningReceiptPalette,
    strings: LevyraStrings
): Pair<String, Boolean>? = when (kind) {
    ReceiptAction.Share -> {
        val intent = ListeningReceiptCard.createShareIntent(context, content, assets, style, palette, strings.receiptCaption)
        if (intent != null && startReceiptChooser(context, intent, strings.shareVia)) null else strings.shareLyricsFailed to true
    }
    ReceiptAction.Save -> {
        val saved = ListeningReceiptCard.saveToGallery(context, content, assets, style, palette)
        if (saved) strings.receiptSaved to false else strings.receiptSaveFailed to true
    }
}

@Composable
private fun ReceiptActionRow(
    enabled: Boolean,
    busy: ReceiptAction?,
    accent: Color,
    strings: LevyraStrings,
    onAction: (ReceiptAction) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilledTonalButton(
            onClick = { onAction(ReceiptAction.Save) },
            enabled = enabled,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
        ) {
            ReceiptActionContent(
                busy = busy == ReceiptAction.Save,
                label = strings.save,
                icon = { Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp)) },
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Button(
            onClick = { onAction(ReceiptAction.Share) },
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = ReceiptOnAccent),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
        ) {
            ReceiptActionContent(
                busy = busy == ReceiptAction.Share,
                label = strings.share,
                icon = { Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp)) },
                color = ReceiptOnAccent
            )
        }
    }
}

@Composable
private fun ReceiptActionContent(
    busy: Boolean,
    label: String,
    icon: @Composable () -> Unit,
    color: Color
) {
    if (busy) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = color)
    } else {
        icon()
    }
    Text(text = label, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Black, maxLines = 1)
}

@Composable
private fun ReceiptPreview(
    preview: Bitmap?,
    busy: Boolean,
    failed: Boolean,
    empty: Boolean,
    strings: LevyraStrings,
    accent: Color,
    mutedColor: Color,
    maxHeight: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .heightIn(max = maxHeight)
                .aspectRatio(RECEIPT_ASPECT)
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            when {
                empty -> Text(
                    text = strings.receiptEmpty,
                    color = mutedColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(28.dp)
                )
                preview != null -> ReceiptPreviewImage(preview, strings.receiptPreviewDescription)
                failed -> Text(
                    text = strings.shareLyricsFailed,
                    color = mutedColor,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
                else -> CircularProgressIndicator(modifier = Modifier.size(30.dp), strokeWidth = 2.dp, color = accent)
            }
            if (busy && preview != null && !empty) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(18.dp),
                        strokeWidth = 2.dp,
                        color = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiptPreviewImage(bitmap: Bitmap, description: String) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    DisposableEffect(bitmap) {
        onDispose { bitmap.recycle() }
    }
    Image(bitmap = image, contentDescription = description, modifier = Modifier.fillMaxSize())
}

@Composable
private fun ReceiptStyleChip(
    label: String,
    selected: Boolean,
    accent: Color,
    textColor: Color,
    mutedColor: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) accent.copy(alpha = 0.18f) else mutedColor.copy(alpha = 0.10f),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent else mutedColor.copy(alpha = 0.32f)
        ),
        shape = RoundedCornerShape(if (selected) 14.dp else 50.dp),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (selected) textColor else mutedColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

internal fun receiptLabels(strings: LevyraStrings, title: String, periodShort: String, locale: Locale) =
    ListeningReceiptLabels(
        brand = RECEIPT_BRAND,
        title = title,
        periodShort = periodShort,
        lifetime = strings.insightsLifetime,
        minutesListened = strings.receiptMinutesListened,
        topTracks = strings.topTracksTitle.uppercase(locale),
        topArtist = strings.receiptTopArtist,
        plays = strings.statPlays,
        tracks = strings.statTracks,
        artists = strings.statArtists,
        peakHour = strings.pulsePeakHour
    )

private fun styleLabel(strings: LevyraStrings, style: ListeningReceiptStyle): String = when (style) {
    ListeningReceiptStyle.Aurora -> strings.receiptStyleAurora
    ListeningReceiptStyle.Noir -> strings.receiptStyleNoir
    ListeningReceiptStyle.Paper -> strings.receiptStylePaper
}

private fun startReceiptChooser(context: Context, intent: Intent, title: String): Boolean = try {
    context.startActivity(Intent.createChooser(intent, title))
    true
} catch (_: ActivityNotFoundException) {
    false
}
