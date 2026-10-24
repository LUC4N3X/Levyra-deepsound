package com.luc4n3x.levyra.ui

import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.luc4n3x.levyra.domain.LevyraLyricsStudioSettings
import com.luc4n3x.levyra.domain.LyricLine
import com.luc4n3x.levyra.domain.LyricsTranslationState
import com.luc4n3x.levyra.domain.LyricsViewerStyle
import com.luc4n3x.levyra.ui.i18n.APPLE_MUSIC_LYRICS_STYLE_LABEL
import com.luc4n3x.levyra.ui.i18n.LocalLevyraStrings
import com.luc4n3x.levyra.ui.i18n.appleLyricsCopy
import com.luc4n3x.levyra.ui.lyrics.apple.AppleLyricsAction
import com.luc4n3x.levyra.ui.lyrics.apple.AppleLyricsActions
import com.luc4n3x.levyra.ui.lyrics.apple.AppleLyricsChoice
import com.luc4n3x.levyra.ui.lyrics.apple.AppleLyricsLabels
import com.luc4n3x.levyra.ui.lyrics.apple.AppleLyricsOptionsSheet
import com.luc4n3x.levyra.ui.lyrics.apple.AppleLyricsToggle
import com.luc4n3x.levyra.ui.lyrics.apple.AppleMusicLyricsScreen
import com.luc4n3x.levyra.ui.lyrics.isLyricsShareSelectable
import com.luc4n3x.levyra.ui.lyrics.lyricsOffsetIsolated
import com.luc4n3x.levyra.ui.lyrics.lyricsProviderDisplayName
import com.luc4n3x.levyra.ui.i18n.lyricsSyncCopy
import com.luc4n3x.levyra.ui.theme.LevyraVisualCapabilities
import com.luc4n3x.levyra.viewmodel.LevyraUiState

internal enum class AppleLyricsOverlayAction { CHANGE_LYRICS, SHARE, SYNC, CALIBRATE, ANALYSIS, STUDIO }

@Composable
internal fun AppleLyricsOverlayScreen(
    state: LevyraUiState,
    lines: List<LyricLine>,
    position: () -> Long,
    showTranslation: Boolean,
    showSecondaryVoices: Boolean,
    animations: Boolean,
    heavyBlur: Boolean,
    selectionMode: Boolean,
    selectedRange: IntRange?,
    onLineClick: (Int, LyricLine) -> Unit,
    onLineLongClick: (Int, LyricLine) -> Unit,
    onSeekFraction: (Float) -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onTranslation: (Boolean) -> Unit,
    onOptions: () -> Unit,
    onClose: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val labels = remember(strings, state.lyricsLoading) {
        AppleLyricsLabels(
            unavailable = if (state.lyricsLoading) strings.searchingLyrics else strings.lyricsUnavailable,
            searching = strings.searchingLyrics,
            options = strings.options,
            close = strings.closeLyrics,
            translation = strings.automaticTranslation,
            previous = strings.previous,
            play = strings.play,
            pause = strings.pause,
            next = strings.next
        )
    }
    val translationEnabled = state.lyricsTranslationEnabled
    val actions = AppleLyricsActions(
        onLineClick = onLineClick,
        onLineLongClick = onLineLongClick,
        onSeekFraction = onSeekFraction,
        onPrevious = onPrevious,
        onTogglePlay = onTogglePlay,
        onNext = onNext,
        onToggleTranslation = { onTranslation(!translationEnabled) },
        onOptions = onOptions,
        onClose = onClose
    )
    AppleMusicLyricsScreen(
        track = state.currentTrack,
        lines = lines,
        synced = state.lyricsSynced,
        loading = state.lyricsLoading,
        position = position,
        reportedPositionMs = state.positionMs,
        bufferedMs = state.bufferedPositionMs,
        durationMs = state.durationMs,
        verifiedKbps = state.currentTrack?.playbackManifest?.alternativeSource?.bitrateKbps,
        playing = state.isPlaying,
        resolving = state.isResolving,
        settings = state.lyricsStudioSettings,
        showTranslation = showTranslation,
        translationAvailable = lines.isNotEmpty() && state.lyricsTranslationState != LyricsTranslationState.SAME_LANGUAGE,
        showSecondaryVoices = showSecondaryVoices,
        animations = animations,
        heavyBlur = heavyBlur,
        selectionMode = selectionMode,
        isSelected = { index ->
            selectedRange?.contains(index) == true && lines.getOrNull(index)?.let(::isLyricsShareSelectable) == true
        },
        labels = labels,
        actions = actions
    )
}

@Composable
internal fun AppleLyricsOverlayOptions(
    state: LevyraUiState,
    lines: List<LyricLine>,
    hasRomanization: Boolean,
    hasMultipleVoices: Boolean,
    showSecondaryVoices: Boolean,
    canShareLyrics: Boolean,
    trackOffsetMs: Long,
    capabilities: LevyraVisualCapabilities,
    onTranslation: (Boolean) -> Unit,
    onUpdateStudio: (LevyraLyricsStudioSettings) -> Unit,
    onSecondaryVoices: (Boolean) -> Unit,
    onAction: (AppleLyricsOverlayAction) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val copy = appleLyricsCopy(strings.code)
    val settings = state.lyricsStudioSettings
    val hasTranslatedLines = lines.any { it.translated.isNotBlank() }
    AppleLyricsOptionsSheet(
        title = strings.options,
        subtitle = lyricsProviderDisplayName(state.lyricsProvider).takeIf(String::isNotBlank)?.let { "${strings.lyricsSource} · $it" },
        styleTitle = copy.lyricsStyle,
        styles = listOf(
            AppleLyricsChoice(copy.skinDefault, settings.viewerStyle == LyricsViewerStyle.SKIN_DEFAULT) {
                onUpdateStudio(settings.copy(viewerStyle = LyricsViewerStyle.SKIN_DEFAULT))
            },
            AppleLyricsChoice(APPLE_MUSIC_LYRICS_STYLE_LABEL, settings.viewerStyle == LyricsViewerStyle.APPLE_MUSIC) {
                onUpdateStudio(settings.copy(viewerStyle = LyricsViewerStyle.APPLE_MUSIC))
            }
        ),
        toggles = buildList {
            add(AppleLyricsToggle(strings.automaticTranslation, state.lyricsTranslationEnabled, onTranslation))
            if (hasRomanization) add(AppleLyricsToggle(strings.lyricsRomanization, settings.romanizationEnabled) { onUpdateStudio(settings.copy(romanizationEnabled = it)) })
            if (hasTranslatedLines || state.lyricsTranslationEnabled) {
                add(AppleLyricsToggle(copy.originalLyrics, settings.showOriginalLyrics) { onUpdateStudio(settings.copy(showOriginalLyrics = it)) })
            }
            if (state.lyricsSynced) {
                add(AppleLyricsToggle(strings.lyricsStudioWordKaraoke, settings.progressiveKaraokeEnabled) { onUpdateStudio(settings.copy(progressiveKaraokeEnabled = it)) })
                add(AppleLyricsToggle(copy.autoScroll, settings.autoScrollEnabled) { onUpdateStudio(settings.copy(autoScrollEnabled = it)) })
                if (capabilities.heavyBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(AppleLyricsToggle(strings.lyricsStudioDepthBlur, settings.appleBlurEnabled) { onUpdateStudio(settings.copy(appleBlurEnabled = it)) })
                }
            }
            if (hasMultipleVoices) add(AppleLyricsToggle(strings.lyricsDuet, showSecondaryVoices, onSecondaryVoices))
        },
        actions = buildList {
            add(AppleLyricsAction(strings.changeLyrics, Icons.Rounded.LibraryMusic) { onAction(AppleLyricsOverlayAction.CHANGE_LYRICS) })
            if (canShareLyrics) add(AppleLyricsAction(strings.shareLyrics, Icons.Rounded.Share) { onAction(AppleLyricsOverlayAction.SHARE) })
            if (state.lyricsSynced && state.currentTrack != null) {
                add(AppleLyricsAction("${lyricsSyncCopy(strings.code).title} · ${lyricsOffsetIsolated(trackOffsetMs)}", Icons.Rounded.Timer) {
                    onAction(AppleLyricsOverlayAction.SYNC)
                })
                add(AppleLyricsAction(strings.lyricsCalibrate, Icons.Rounded.Schedule) { onAction(AppleLyricsOverlayAction.CALIBRATE) })
            }
            if (state.intelligenceSummary.available) add(AppleLyricsAction(strings.lyricsAnalysis, Icons.Rounded.Insights) { onAction(AppleLyricsOverlayAction.ANALYSIS) })
            add(AppleLyricsAction(strings.lyricsStudio, Icons.Rounded.AutoAwesome) { onAction(AppleLyricsOverlayAction.STUDIO) })
        },
        onDismiss = onDismiss
    )
}

internal fun appleLyricsStyleChipLabel(code: String): String = "${appleLyricsCopy(code).lyricsStyle} · $APPLE_MUSIC_LYRICS_STYLE_LABEL"

internal fun LevyraLyricsStudioSettings.withAppleMusicViewer(): LevyraLyricsStudioSettings = copy(viewerStyle = LyricsViewerStyle.APPLE_MUSIC)
