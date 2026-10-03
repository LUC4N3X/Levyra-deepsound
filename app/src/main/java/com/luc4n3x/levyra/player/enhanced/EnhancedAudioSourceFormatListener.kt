package com.luc4n3x.levyra.player.enhanced

import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.audio.AudioSink
import timber.log.Timber

internal fun isLosslessAudioMimeType(mimeType: String?): Boolean = when (mimeType) {
    MimeTypes.AUDIO_FLAC, MimeTypes.AUDIO_ALAC, MimeTypes.AUDIO_RAW -> true
    else -> false
}

@UnstableApi
internal class EnhancedAudioSourceFormatListener(
    private val processor: EnhancedAudioProcessor,
    private val label: String,
    private val onLosslessSourceChanged: (Boolean) -> Unit = {}
) : AnalyticsListener {
    private fun updateLosslessSource(lossless: Boolean) {
        if (processor.isLosslessSource == lossless) return
        processor.isLosslessSource = lossless
        onLosslessSourceChanged(lossless)
    }

    override fun onMediaItemTransition(
        eventTime: AnalyticsListener.EventTime,
        mediaItem: MediaItem?,
        reason: Int
    ) {
        if (reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) updateLosslessSource(false)
    }

    override fun onAudioInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?
    ) {
        val lossless = isLosslessAudioMimeType(format.sampleMimeType)
        updateLosslessSource(lossless)
        Timber.d(
            "AUDIO_SOURCE_FORMAT player=%s mime=%s codecs=%s sampleRate=%d channels=%d pcmEncoding=%d lossless=%s",
            label,
            format.sampleMimeType,
            format.codecs,
            format.sampleRate,
            format.channelCount,
            format.pcmEncoding,
            lossless
        )
    }

    override fun onAudioDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long
    ) {
        Timber.d("AUDIO_DECODER player=%s decoder=%s", label, decoderName)
    }

    override fun onAudioTrackInitialized(eventTime: AnalyticsListener.EventTime, audioTrackConfig: AudioSink.AudioTrackConfig) {
        Timber.d(
            "AUDIO_TRACK_OUTPUT player=%s encoding=%d sampleRate=%d channelConfig=%d offload=%s",
            label,
            audioTrackConfig.encoding,
            audioTrackConfig.sampleRate,
            audioTrackConfig.channelConfig,
            audioTrackConfig.offload
        )
    }
}
