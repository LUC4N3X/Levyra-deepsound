package com.luc4n3x.levyra.ui.mixlab

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.luc4n3x.levyra.domain.MixLabDefaults
import com.luc4n3x.levyra.domain.MixLabDuration
import com.luc4n3x.levyra.domain.MixLabParams
import com.luc4n3x.levyra.domain.MoodEngine
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.ui.LevyraAdaptiveCardDeep
import com.luc4n3x.levyra.ui.LevyraAdaptiveHairline
import com.luc4n3x.levyra.ui.components.LevyraPressScale
import com.luc4n3x.levyra.ui.components.levyraPressable
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.formatLibraryDuration
import com.luc4n3x.levyra.ui.theme.LevyraBlack
import com.luc4n3x.levyra.ui.theme.LevyraCyan
import com.luc4n3x.levyra.ui.theme.LevyraHapticAction
import com.luc4n3x.levyra.ui.theme.LevyraInk
import com.luc4n3x.levyra.ui.theme.LevyraMuted
import com.luc4n3x.levyra.ui.theme.LevyraOnAccent
import com.luc4n3x.levyra.ui.theme.LevyraPink
import com.luc4n3x.levyra.ui.theme.LevyraPlayerDesign
import com.luc4n3x.levyra.ui.theme.LevyraText
import com.luc4n3x.levyra.ui.theme.LevyraTypeRhythm
import com.luc4n3x.levyra.ui.theme.LevyraViolet
import com.luc4n3x.levyra.viewmodel.MixLabController
import com.luc4n3x.levyra.viewmodel.MixLabSession
import com.luc4n3x.levyra.viewmodel.MixLabStage
import kotlin.math.roundToInt

@Composable
internal fun MixLabScreen(
    session: MixLabSession,
    controller: MixLabController,
    animated: Boolean,
    onClose: () -> Unit,
    onPlayResult: (shuffled: Boolean) -> Unit,
    onAddResultToQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)
    var showSaveDialog by remember { mutableStateOf(false) }
    LaunchedEffect(session.savedPlaylistId) {
        if (session.savedPlaylistId != null) showSaveDialog = false
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = LevyraBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            MixLabTopBar(
                stage = session.stage,
                onClose = onClose,
                onTuneParameters = controller::tuneParameters
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (session.stage) {
                    MixLabStage.Configure -> {
                        MixLabConfigureContent(
                            session = session,
                            controller = controller
                        )
                    }
                    MixLabStage.Generating -> {
                        MixLabGeneratingContent()
                    }
                    MixLabStage.Preview -> {
                        session.result?.let { result ->
                            MixLabPreviewContent(
                                session = session,
                                result = result,
                                controller = controller,
                                onPlay = { onPlayResult(false) },
                                onShuffle = { onPlayResult(true) },
                                onAddToQueue = onAddResultToQueue,
                                onOpenSave = { showSaveDialog = true }
                            )
                        }
                    }
                    MixLabStage.Error -> {
                        MixLabErrorContent(
                            isEmptyArtistsError = session.params.artistKeys.isNotEmpty() && session.result?.tracks?.isEmpty() == true,
                            onRetry = controller::generate,
                            onTune = controller::tuneParameters
                        )
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        MixLabSavePlaylistDialog(
            isSaving = session.saving,
            isFailed = session.saveFailed,
            onDismiss = { showSaveDialog = false },
            onConfirm = { name -> controller.saveAsPlaylist(name) }
        )
    }
}

@Composable
private fun MixLabTopBar(
    stage: MixLabStage,
    onClose: () -> Unit,
    onTuneParameters: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = strings.close,
                tint = LevyraText
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = strings.mixLab,
                color = LevyraText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = strings.mixLabSubtitle,
                color = LevyraMuted,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (stage == MixLabStage.Preview) {
            TextButton(onClick = onTuneParameters) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = LevyraCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = strings.mixLabTuneParameters,
                    color = LevyraCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MixLabConfigureContent(
    session: MixLabSession,
    controller: MixLabController
) {
    val strings = LocalLevyraStrings.current
    val params = session.params
    val moods = remember(strings.code) { MoodEngine().moodsForLanguage(strings.code) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "presets") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.mixLabPresets,
                    color = LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MixLabPresetChip(
                        label = strings.mixLabPresetChill,
                        onClick = {
                            controller.updateParams {
                                it.copy(
                                    familiarity = 0.5f,
                                    recency = 0.4f,
                                    duration = MixLabDuration.Medium,
                                    moodTags = setOf("chill", "ambient", "night")
                                )
                            }
                        }
                    )
                    MixLabPresetChip(
                        label = strings.mixLabPresetWorkout,
                        onClick = {
                            controller.updateParams {
                                it.copy(
                                    familiarity = 0.7f,
                                    recency = 0.7f,
                                    duration = MixLabDuration.Short,
                                    moodTags = setOf("gym", "bass", "rap", "energy")
                                )
                            }
                        }
                    )
                    MixLabPresetChip(
                        label = strings.mixLabPresetLateNight,
                        onClick = {
                            controller.updateParams {
                                it.copy(
                                    familiarity = 0.4f,
                                    recency = 0.3f,
                                    duration = MixLabDuration.Long,
                                    moodTags = setOf("night", "chill", "deep")
                                )
                            }
                        }
                    )
                    MixLabPresetChip(
                        label = strings.mixLabPresetRediscover,
                        onClick = {
                            controller.updateParams {
                                it.copy(
                                    familiarity = 0.9f,
                                    recency = 0.1f,
                                    duration = null,
                                    moodTags = emptySet()
                                )
                            }
                        }
                    )
                    MixLabPresetChip(
                        label = strings.mixLabPresetFreshFinds,
                        onClick = {
                            controller.updateParams {
                                it.copy(
                                    familiarity = 0.1f,
                                    recency = 0.9f,
                                    duration = null,
                                    moodTags = emptySet()
                                )
                            }
                        }
                    )
                }
            }
        }

        item(key = "familiarity") {
            MixLabSliderSection(
                title = strings.mixLabFamiliarity,
                leftLabel = strings.mixLabFamiliarityDiscovery,
                rightLabel = strings.mixLabFamiliarityFavorites,
                value = params.familiarity,
                onValueChange = { value -> controller.updateParams { it.copy(familiarity = value) } }
            )
        }

        item(key = "recency") {
            MixLabSliderSection(
                title = strings.mixLabRecency,
                leftLabel = strings.mixLabRecencyClassics,
                rightLabel = strings.mixLabRecencyNew,
                value = params.recency,
                onValueChange = { value -> controller.updateParams { it.copy(recency = value) } }
            )
        }

        item(key = "duration") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.mixLabDuration,
                    color = LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MixLabSelectChip(
                        label = strings.mixLabDurationAny,
                        selected = params.duration == null,
                        onClick = { controller.updateParams { it.copy(duration = null) } },
                        modifier = Modifier.weight(1f)
                    )
                    MixLabSelectChip(
                        label = strings.mixLabDurationShort,
                        selected = params.duration == MixLabDuration.Short,
                        onClick = { controller.updateParams { it.copy(duration = MixLabDuration.Short) } },
                        modifier = Modifier.weight(1f)
                    )
                    MixLabSelectChip(
                        label = strings.mixLabDurationMedium,
                        selected = params.duration == MixLabDuration.Medium,
                        onClick = { controller.updateParams { it.copy(duration = MixLabDuration.Medium) } },
                        modifier = Modifier.weight(1f)
                    )
                    MixLabSelectChip(
                        label = strings.mixLabDurationLong,
                        selected = params.duration == MixLabDuration.Long,
                        onClick = { controller.updateParams { it.copy(duration = MixLabDuration.Long) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item(key = "trackCount") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.mixLabTrackCount,
                    color = LevyraText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 20, 30, 50).forEach { count ->
                        MixLabSelectChip(
                            label = "$count",
                            selected = params.trackCount == count,
                            onClick = { controller.updateParams { it.copy(trackCount = count) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item(key = "moods") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strings.mixLabMood,
                        color = LevyraText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.mixLabMoodHint,
                        color = LevyraMuted,
                        fontSize = 11.sp
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    moods.forEach { mood ->
                        val moodTagsNormalized = mood.tags.map { it.lowercase() }.toSet()
                        val isSelected = moodTagsNormalized.any { it in params.moodTags }
                        MixLabSelectChip(
                            label = "${mood.icon} ${mood.title}",
                            selected = isSelected,
                            onClick = {
                                controller.updateParams { current ->
                                    val nextTags = if (isSelected) {
                                        current.moodTags - moodTagsNormalized
                                    } else {
                                        current.moodTags + moodTagsNormalized
                                    }
                                    current.copy(moodTags = nextTags)
                                }
                            }
                        )
                    }
                }
            }
        }

        if (session.availableGenres.isNotEmpty()) {
            item(key = "genres") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = strings.mixLabGenres,
                            color = LevyraText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.mixLabGenresHint,
                            color = LevyraMuted,
                            fontSize = 11.sp
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        session.availableGenres.forEach { genre ->
                            val isSelected = genre in params.genres
                            MixLabSelectChip(
                                label = genre.replaceFirstChar { it.uppercase() },
                                selected = isSelected,
                                onClick = {
                                    controller.updateParams { current ->
                                        val nextGenres = if (isSelected) {
                                            current.genres - genre
                                        } else {
                                            current.genres + genre
                                        }
                                        current.copy(genres = nextGenres)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        if (session.availableArtists.isNotEmpty()) {
            item(key = "artists") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = strings.mixLabArtists,
                            color = LevyraText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.mixLabArtistsHint,
                            color = LevyraMuted,
                            fontSize = 11.sp
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        session.availableArtists.forEach { (displayName, key) ->
                            val isSelected = key in params.artistKeys
                            MixLabSelectChip(
                                label = displayName,
                                selected = isSelected,
                                onClick = {
                                    controller.updateParams { current ->
                                        val nextArtists = if (isSelected) {
                                            current.artistKeys - key
                                        } else {
                                            current.artistKeys + key
                                        }
                                        current.copy(artistKeys = nextArtists)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        item(key = "generateButton") {
            Spacer(modifier = Modifier.height(8.dp))
            MixLabPrimaryActionButton(
                label = strings.mixLabGenerate,
                icon = Icons.Rounded.PlayArrow,
                accent = LevyraCyan,
                onClick = controller::generate,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MixLabPresetChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(LevyraAdaptiveCardDeep)
            .border(Dp.Hairline, LevyraAdaptiveHairline, shape)
            .levyraPressable(
                onClick = onClick,
                pressedScale = LevyraPressScale.Tile,
                role = Role.Button
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = LevyraCyan,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MixLabSelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    val background by animateColorAsState(
        targetValue = if (selected) LevyraCyan.copy(alpha = 0.22f) else LevyraAdaptiveCardDeep,
        label = "mix-lab-chip-bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) LevyraCyan else LevyraText,
        label = "mix-lab-chip-text"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) LevyraCyan else LevyraAdaptiveHairline,
        label = "mix-lab-chip-border"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(background)
            .border(Dp.Hairline, borderColor, shape)
            .levyraPressable(
                onClick = onClick,
                pressedScale = LevyraPressScale.Tile,
                role = Role.Button
            )
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MixLabSliderSection(
    title: String,
    leftLabel: String,
    rightLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val sliderValue = value.coerceIn(0f, 1f)
    val sliderState = rememberSliderState(
        value = sliderValue,
        trackRange = 0f..1f
    )
    LaunchedEffect(sliderValue) {
        if (sliderState.value != sliderValue) sliderState.value = sliderValue
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            color = LevyraText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Slider(
            state = sliderState,
            onValueChange = { nextValue ->
                sliderState.value = nextValue
                onValueChange(nextValue)
            },
            colors = SliderDefaults.colors(
                thumbColor = LevyraCyan,
                activeTrackColor = LevyraCyan,
                inactiveTrackColor = LevyraPlayerDesign.TrackInactive
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = leftLabel,
                color = if (value < 0.5f) LevyraCyan else LevyraMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = rightLabel,
                color = if (value >= 0.5f) LevyraCyan else LevyraMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun MixLabGeneratingContent() {
    val strings = LocalLevyraStrings.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = LevyraCyan,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = strings.mixLabGenerating,
                color = LevyraText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MixLabPreviewContent(
    session: MixLabSession,
    result: com.luc4n3x.levyra.domain.MixLabResult,
    controller: MixLabController,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    onAddToQueue: () -> Unit,
    onOpenSave: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val familiarPercent = (result.familiarShare * 100f).roundToInt()
    val discoveryPercent = (result.discoveryShare * 100f).roundToInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "stats") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LevyraAdaptiveCardDeep)
                    .border(Dp.Hairline, LevyraAdaptiveHairline, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.mixLabPreviewTitle,
                    color = LevyraText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.mixLabPreviewTracks(result.tracks.size),
                        color = LevyraCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.mixLabPreviewArtists(result.artistCount),
                        color = LevyraPink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.mixLabPreviewFamiliarShare(familiarPercent),
                        color = LevyraText,
                        fontSize = 13.sp
                    )
                    Text(
                        text = strings.mixLabPreviewDiscoveryShare(discoveryPercent),
                        color = LevyraMuted,
                        fontSize = 13.sp
                    )
                }
            }
        }

        item(key = "actionsRow1") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MixLabPrimaryActionButton(
                    label = strings.mixLabPlay,
                    icon = Icons.Rounded.PlayArrow,
                    accent = LevyraCyan,
                    onClick = onPlay,
                    modifier = Modifier.weight(1f)
                )
                MixLabPrimaryActionButton(
                    label = strings.mixLabShuffle,
                    icon = Icons.Rounded.Shuffle,
                    accent = LevyraPink,
                    onClick = onShuffle,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item(key = "actionsRow2") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MixLabSecondaryActionButton(
                    label = strings.mixLabAddToQueue,
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    onClick = onAddToQueue,
                    modifier = Modifier.weight(1f)
                )
                MixLabSecondaryActionButton(
                    label = if (session.savedPlaylistId != null) strings.mixLabSaveSuccess else strings.mixLabSaveAsPlaylist,
                    icon = if (session.savedPlaylistId != null) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.PlaylistAdd,
                    enabled = session.savedPlaylistId == null,
                    onClick = onOpenSave,
                    modifier = Modifier.weight(1f)
                )
                MixLabSecondaryActionButton(
                    label = strings.mixLabRegenerate,
                    icon = Icons.Rounded.Refresh,
                    onClick = controller::regenerate,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        itemsIndexed(
            items = result.tracks,
            key = { index, track -> "${index}_${track.id}" }
        ) { _, track ->
            MixLabTrackRow(track = track)
        }

        item(key = "bottomPadding") {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun MixLabTrackRow(
    track: Track,
    modifier: Modifier = Modifier
) {
    val strings = LocalLevyraStrings.current
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LevyraAdaptiveCardDeep.copy(alpha = 0.5f))
            .border(Dp.Hairline, LevyraAdaptiveHairline, shape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val artworkUrl = track.largeThumbnailUrl.ifBlank { track.thumbnailUrl }
        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LevyraInk)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = LevyraText,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = LevyraMuted,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = strings.formatLibraryDuration(track.durationMs),
            color = LevyraMuted,
            fontSize = 11.5.sp
        )
    }
}

@Composable
private fun MixLabErrorContent(
    isEmptyArtistsError: Boolean,
    onRetry: () -> Unit,
    onTune: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.mixLabErrorTitle,
                color = LevyraText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isEmptyArtistsError) strings.mixLabEmptyArtists else strings.mixLabErrorBody,
                color = LevyraMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MixLabPrimaryActionButton(
                    label = strings.mixLabErrorRetry,
                    icon = Icons.Rounded.Refresh,
                    accent = LevyraCyan,
                    onClick = onRetry
                )
                MixLabSecondaryActionButton(
                    label = strings.mixLabTuneParameters,
                    icon = Icons.Rounded.Tune,
                    onClick = onTune
                )
            }
        }
    }
}

@Composable
private fun MixLabPrimaryActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonShape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(buttonShape)
            .background(accent)
            .levyraPressable(
                onClick = onClick,
                pressedScale = LevyraPressScale.Surface,
                role = Role.Button,
                haptic = LevyraHapticAction.Confirm
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LevyraBlack,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = LevyraBlack,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MixLabSecondaryActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(shape)
            .background(LevyraAdaptiveCardDeep)
            .border(Dp.Hairline, LevyraAdaptiveHairline, shape)
            .levyraPressable(
                onClick = onClick,
                enabled = enabled,
                pressedScale = LevyraPressScale.Tile,
                role = Role.Button
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) LevyraText else LevyraMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = if (enabled) LevyraText else LevyraMuted,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MixLabSavePlaylistDialog(
    isSaving: Boolean,
    isFailed: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val strings = LocalLevyraStrings.current
    var name by remember { mutableStateOf("Mix Lab") }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = {
            Text(
                text = strings.mixLabSaveDialogTitle,
                color = LevyraText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LevyraInk)
                        .border(1.dp, if (isFailed) LevyraPink else LevyraAdaptiveHairline, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        textStyle = TextStyle(
                            color = LevyraText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(LevyraCyan),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (isFailed) {
                    Text(
                        text = strings.mixLabSaveFailed,
                        color = LevyraPink,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank() && !isSaving) onConfirm(name.trim()) },
                enabled = name.isNotBlank() && !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = LevyraCyan,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = if (isFailed) strings.mixLabSaveRetry else strings.mixLabSaveConfirm,
                        color = LevyraCyan,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(
                    text = strings.mixLabSaveCancel,
                    color = LevyraMuted
                )
            }
        },
        containerColor = LevyraAdaptiveCardDeep
    )
}
