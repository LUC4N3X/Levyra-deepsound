package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.MixChapter
import com.luc4n3x.levyra.domain.MixChapterList
import com.luc4n3x.levyra.domain.MixChapterParser
import com.luc4n3x.levyra.domain.MixChapterSource
import com.luc4n3x.levyra.domain.RawMixChapter
import com.luc4n3x.levyra.domain.Track
import java.util.Collections
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import timber.log.Timber

internal data class MixChapterSourceData(
    val structured: List<RawMixChapter>,
    val description: String
)

internal object MixChapterSelection {
    fun select(trackId: String, durationMs: Long, data: MixChapterSourceData): MixChapterList? {
        val structured = MixChapterParser.normalize(data.structured, durationMs)
        if (structured.isNotEmpty()) {
            return MixChapterList(trackId, durationMs, MixChapterSource.STRUCTURED, structured)
        }
        val parsed: List<MixChapter> = MixChapterParser.parseDescription(data.description, durationMs)
        if (parsed.isNotEmpty()) {
            return MixChapterList(trackId, durationMs, MixChapterSource.DESCRIPTION, parsed)
        }
        return null
    }
}

internal class MixChaptersRepository(
    private val clock: () -> Long = System::currentTimeMillis,
    private val loader: suspend (String) -> MixChapterSourceData = ::loadFromExtractor
) {
    private sealed interface Entry {
        data class Found(val value: MixChapterList) : Entry
        data class Missing(val expiresAtMs: Long) : Entry
    }

    private val cache: MutableMap<String, Entry> = Collections.synchronizedMap(
        object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean =
                size > MAX_CACHE_ENTRIES
        }
    )

    fun isEligible(track: Track): Boolean =
        track.durationMs >= MIN_MIX_DURATION_MS &&
            YOUTUBE_ID.matches(track.id)

    suspend fun load(track: Track): MixChapterList? {
        if (!isEligible(track)) return null
        when (val cached = cache[track.id]) {
            is Entry.Found -> return cached.value
            is Entry.Missing -> if (clock() < cached.expiresAtMs) return null
            null -> Unit
        }
        val data = try {
            withTimeoutOrNull(LOAD_TIMEOUT_MS) { loader(track.id) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.d(error, "mix chapters unavailable for %s", track.id)
            null
        }
        if (data == null) {
            cache[track.id] = Entry.Missing(clock() + FAILURE_RETRY_MS)
            return null
        }
        val result = MixChapterSelection.select(track.id, track.durationMs, data)
        cache[track.id] = if (result != null) Entry.Found(result) else Entry.Missing(clock() + EMPTY_RETRY_MS)
        return result
    }

    companion object {
        const val MIN_MIX_DURATION_MS = 8L * 60L * 1000L
        private const val MAX_CACHE_ENTRIES = 48
        private const val LOAD_TIMEOUT_MS = 20_000L
        private const val FAILURE_RETRY_MS = 2L * 60L * 1000L
        private const val EMPTY_RETRY_MS = 6L * 60L * 60L * 1000L
        private val YOUTUBE_ID = Regex("""[A-Za-z0-9_-]{11}""")

        private suspend fun loadFromExtractor(videoId: String): MixChapterSourceData =
            withContext(Dispatchers.IO) {
                NewPipeRuntime.ensure()
                val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
                MixChapterSourceData(
                    structured = info.streamSegments.orEmpty().map { segment ->
                        RawMixChapter(segment.startTimeSeconds * 1000L, segment.title.orEmpty())
                    },
                    description = info.description?.content.orEmpty()
                )
            }
    }
}
