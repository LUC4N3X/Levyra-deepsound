package com.luc4n3x.levyra.data.hqaudio.lossless

import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.luc4n3x.levyra.data.PlaybackResolver
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.DownloadQualityChoice
import com.luc4n3x.levyra.domain.DownloadQualitySource
import com.luc4n3x.levyra.domain.HighQualityAudioMode
import com.luc4n3x.levyra.player.offline.OfflineAudioExporter
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LosslessSiteDownloadLiveTest {
    @Test
    fun losslessDownloadSavesTheExactAlbumCutAsFullLengthFlac() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("losslessDownload") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val results = YoutubeMusicRepository(context).search("Get Lucky Daft Punk", limit = 20)
        val track = results.firstOrNull { it.title.contains("Get Lucky", true) && abs(it.durationMs - 369_000L) <= 6_000L }
            ?: throw AssertionError(results.joinToString { "${it.title}/${it.artist}/${it.durationMs}" })
        val resolver = PlaybackResolver.getInstance(context).apply { setHighQualityAudioMode(HighQualityAudioMode.PREFER_LOSSLESS) }
        val result = withTimeout(240_000L) {
            OfflineAudioExporter(context, resolver, qualityChoice = DownloadQualityChoice(DownloadQualitySource.Lossless)).export(track)
        }
        assertEquals("audio/flac", result.mimeType)
        val extractor = MediaExtractor()
        try {
            context.contentResolver.openFileDescriptor(result.uri, "r")!!.use { extractor.setDataSource(it.fileDescriptor) }
            val format = extractor.getTrackFormat(0)
            val durationMs = format.getLong(MediaFormat.KEY_DURATION) / 1_000L
            Log.i("LosslessSiteDownloadLiveTest", "file=${result.fileName} mime=${format.getString(MediaFormat.KEY_MIME)} rate=${format.getInteger(MediaFormat.KEY_SAMPLE_RATE)} durationMs=$durationMs")
            assertEquals("audio/flac", format.getString(MediaFormat.KEY_MIME))
            assertTrue("Full length required, durationMs=$durationMs", abs(durationMs - 369_000L) <= 5_000L)
        } finally {
            extractor.release()
        }
    }
}
