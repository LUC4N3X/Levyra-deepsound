package com.luc4n3x.levyra.ui.share

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.VisualCraftCopy
import com.luc4n3x.levyra.ui.i18n.VisualCraftText
import com.luc4n3x.levyra.ui.i18n.visualCraftCopy
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraPink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val StoryPreviewMaxHeight = 420.dp
private val StoryThumbWidth = 64.dp
private const val THUMB_WIDTH_PX = 180
private val SheetGround = Color(0xFF111116)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun StoryShareSheet(
    track: Track,
    positionMs: Long,
    durationMs: Long,
    onDismiss: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = strings.visualCraftCopy
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var template by rememberSaveable { mutableStateOf(StoryTemplate.Aura) }
    var tone by rememberSaveable { mutableStateOf(StoryTone.Artwork) }
    var showProgress by rememberSaveable { mutableStateOf(true) }
    var showBrand by rememberSaveable { mutableStateOf(true) }
    var artwork by remember(track.id) { mutableStateOf<Bitmap?>(null) }
    var colors by remember(track.id) { mutableStateOf<StoryShareColors?>(null) }
    var preview by remember(track.id) { mutableStateOf<Bitmap?>(null) }
    var thumbs by remember(track.id) { mutableStateOf<Map<StoryTemplate, Bitmap>>(emptyMap()) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val options = StoryShareOptions(template, tone, showProgress, showBrand)
    val eyebrow = copy[VisualCraftText.StoryEyebrow]
    val content = remember(track.id, positionMs, durationMs, eyebrow) {
        StoryShareContent(
            title = track.title,
            artist = track.artist,
            album = track.album.takeUnless { it.equals(track.title, ignoreCase = true) }.orEmpty(),
            positionMs = positionMs.coerceIn(0L, durationMs.coerceAtLeast(0L)),
            durationMs = durationMs,
            eyebrow = eyebrow
        )
    }

    LaunchedEffect(track.id) {
        val loaded = try {
            StoryShareCard.loadArtwork(context, track)
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
        artwork = loaded
        colors = withContext(Dispatchers.Default) { StoryShareCard.colors(track, loaded) }
    }

    LaunchedEffect(options, colors, content) {
        val palette = colors ?: return@LaunchedEffect
        val rendered = withContext(Dispatchers.Default) {
            StoryShareCard.render(content, artwork, palette, options, StoryShareCard.PREVIEW_WIDTH_PX)
        }
        preview = rendered
    }

    LaunchedEffect(colors, tone, showProgress, showBrand, content) {
        val palette = colors ?: return@LaunchedEffect
        val rendered = withContext(Dispatchers.Default) {
            StoryTemplate.entries.associateWith { option ->
                StoryShareCard.render(content, artwork, palette, options.copy(template = option), THUMB_WIDTH_PX)
            }
        }
        thumbs = rendered
    }

    fun exportFull(action: suspend (Bitmap) -> Unit) {
        val palette = colors ?: return
        if (busy) return
        busy = true
        failed = false
        saved = false
        scope.launch {
            val full = withContext(Dispatchers.Default) {
                StoryShareCard.render(content, artwork, palette, options, StoryShareCard.EXPORT_WIDTH_PX)
            }
            try {
                action(full)
            } finally {
                full.recycle()
                busy = false
            }
        }
    }

    val accentTarget = colors?.let { Color(it.accent) } ?: LevyraCyan
    val accent by animateColorAsState(accentTarget, label = "story-accent")
    val glowTarget = colors?.let { Color(it.start) } ?: LevyraCyan
    val glow by animateColorAsState(glowTarget, label = "story-glow")
    val onAccent = readableOn(accent)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetGround,
        dragHandle = null
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(0f to glow.copy(alpha = 0.32f), 0.55f to glow.copy(alpha = 0.06f), 1f to Color.Transparent))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Box(
                    Modifier
                        .padding(top = 12.dp)
                        .align(Alignment.CenterHorizontally)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.24f))
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(copy[VisualCraftText.StoryTitle], color = SheetText, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(
                        "${track.title} · ${track.artist}",
                        color = SheetMuted,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StoryPreview(preview, glow, copy[VisualCraftText.StoryPreview])
                Row(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StoryTemplate.entries.forEach { option ->
                        TemplateThumb(
                            label = templateLabel(option, copy),
                            image = thumbs[option],
                            selected = template == option,
                            accent = accent,
                            onClick = { template = option }
                        )
                    }
                }
                ToneSegments(
                    first = copy[VisualCraftText.ToneArtwork],
                    second = copy[VisualCraftText.ToneMidnight],
                    secondSelected = tone == StoryTone.Midnight,
                    accent = accent,
                    onSelect = { midnight -> tone = if (midnight) StoryTone.Midnight else StoryTone.Artwork }
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailChip(copy[VisualCraftText.ShowProgress], showProgress, accent) { showProgress = !showProgress }
                    DetailChip(copy[VisualCraftText.ShowBrand], showBrand, accent) { showBrand = !showBrand }
                }
                if (failed || saved) {
                    Text(
                        text = copy[if (failed) VisualCraftText.Failed else VisualCraftText.Saved],
                        color = if (failed) LevyraPink else SheetMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    if (StoryShareCard.canSaveToGallery) {
                        Button(
                            onClick = {
                                exportFull { bitmap ->
                                    val ok = StoryShareCard.saveToGallery(context, bitmap)
                                    saved = ok
                                    failed = !ok
                                }
                            },
                            enabled = colors != null && !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.10f), contentColor = SheetText),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp)
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(copy[VisualCraftText.SaveImage], modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(
                        onClick = {
                            exportFull { bitmap ->
                                val uri = StoryShareCard.export(context, bitmap)
                                val started = uri != null && try {
                                    context.startActivity(
                                        Intent.createChooser(
                                            StoryShareCard.shareIntent(uri, "${track.title} · ${track.artist}"),
                                            copy[VisualCraftText.ShareStory]
                                        )
                                    )
                                    true
                                } catch (_: ActivityNotFoundException) {
                                    false
                                }
                                failed = !started
                            }
                        },
                        enabled = colors != null && !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = onAccent),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1.5f).heightIn(min = 56.dp)
                    ) {
                        if (busy) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = onAccent)
                        } else {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = if (busy) copy[VisualCraftText.Preparing] else copy[VisualCraftText.ShareImage],
                            modifier = Modifier.padding(start = 8.dp),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryPreview(preview: Bitmap?, glow: Color, description: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .heightIn(max = StoryPreviewMaxHeight)
                .aspectRatio(9f / 16f)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(glow.copy(alpha = 0.45f), Color.Transparent),
                            center = center,
                            radius = size.maxDimension * 0.75f
                        ),
                        radius = size.maxDimension * 0.75f
                    )
                }
                .shadow(24.dp, RoundedCornerShape(28.dp), clip = false)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (preview != null && !preview.isRecycled) {
                val bitmap = remember(preview) { preview.asImageBitmap() }
                Image(bitmap = bitmap, contentDescription = description, modifier = Modifier.fillMaxSize())
            } else {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = LevyraCyan)
            }
        }
    }
}

@Composable
private fun TemplateThumb(label: String, image: Bitmap?, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val scale by animateFloatAsState(if (selected) 1f else 0.94f, label = "story-thumb-scale")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(StoryThumbWidth + 12.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .width(StoryThumbWidth)
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else Color.White.copy(alpha = 0.12f)),
                    RoundedCornerShape(14.dp)
                )
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            if (image != null && !image.isRecycled) {
                val bitmap = remember(image) { image.asImageBitmap() }
                Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            color = if (selected) SheetText else SheetMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ToneSegments(first: String, second: String, secondSelected: Boolean, accent: Color, onSelect: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.07f))
            .padding(4.dp)
            .selectableGroup()
    ) {
        listOf(first to false, second to true).forEach { (label, midnight) ->
            val selected = midnight == secondSelected
            val fill by animateColorAsState(if (selected) accent.copy(alpha = 0.22f) else Color.Transparent, label = "story-tone")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(fill)
                    .selectable(selected = selected, role = Role.RadioButton, onClick = { onSelect(midnight) }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) SheetText else SheetMuted,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailChip(label: String, selected: Boolean, accent: Color, onToggle: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onToggle,
        label = { Text(label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = if (selected) {
            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        } else {
            null
        },
        shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White.copy(alpha = 0.05f),
            labelColor = SheetMuted,
            selectedContainerColor = accent.copy(alpha = 0.20f),
            selectedLabelColor = SheetText,
            selectedLeadingIconColor = accent
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Color.White.copy(alpha = 0.12f),
            selectedBorderColor = accent.copy(alpha = 0.6f)
        ),
        modifier = Modifier.heightIn(min = 48.dp)
    )
}

private val SheetText = Color(0xFFF4F4F7)
private val SheetMuted = Color(0xFFA3A3AE)

private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return maxOf(la, lb) / minOf(la, lb)
}

private fun readableOn(background: Color): Color {
    val dark = Color(0xFF0E0F14)
    return if (contrast(dark, background) >= contrast(Color.White, background)) dark else Color.White
}

private fun templateLabel(template: StoryTemplate, copy: VisualCraftCopy): String = when (template) {
    StoryTemplate.Aura -> copy[VisualCraftText.TemplateAura]
    StoryTemplate.Vinyl -> copy[VisualCraftText.TemplateVinyl]
    StoryTemplate.Poster -> copy[VisualCraftText.TemplatePoster]
    StoryTemplate.Glass -> copy[VisualCraftText.TemplateGlass]
}
