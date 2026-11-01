package com.luc4n3x.levyra.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.luc4n3x.levyra.data.PlaylistStore
import com.luc4n3x.levyra.data.TrackJson
import com.luc4n3x.levyra.data.local.DownloadEntity
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.domain.GentleWakeSettings
import com.luc4n3x.levyra.domain.GentleWakeSourceType
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.radio.RadioStore
import com.luc4n3x.levyra.ui.i18n.smartListeningCopyForCode
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

internal data class GentleWakeResolvedSource(
    val tracks: List<Track>,
    val displayTitle: String,
    val usingEmergencyFallback: Boolean
)

internal object GentleWakePlaybackCoordinator {
    suspend fun resolveAlarmSource(
        context: Context,
        settings: GentleWakeSettings,
        languageCode: String
    ): GentleWakeResolvedSource {
        val appContext = context.applicationContext
        val copy = smartListeningCopyForCode(languageCode)
        val builtInTrack = GentleWakeToneGenerator.ensureToneTrack(
            context = appContext,
            title = copy.builtInToneName,
            subtitle = copy.builtInToneSubtitle
        )

        val normalized = settings.normalized()
        if (normalized.sourceType == GentleWakeSourceType.BUILT_IN_TONE) {
            return GentleWakeResolvedSource(
                tracks = listOf(builtInTrack),
                displayTitle = copy.builtInToneName,
                usingEmergencyFallback = false
            )
        }

        val online = isNetworkValidated(appContext)
        val downloadsByTrackId = runCatching {
            LevyraDatabase.get(appContext).downloadedTracksDao().all()
                .associateBy { it.trackId }
        }.getOrDefault(emptyMap())

        val candidateTracks = when (normalized.sourceType) {
            GentleWakeSourceType.PLAYLIST -> playlistTracks(appContext, normalized)
            GentleWakeSourceType.ALBUM -> decodePayloadTracks(normalized.sourcePayloadJson)
            GentleWakeSourceType.RADIO -> radioTracks(appContext, normalized)
            GentleWakeSourceType.LOCAL_AUDIO -> localAudioTracks(normalized, downloadsByTrackId)
            GentleWakeSourceType.BUILT_IN_TONE -> listOf(builtInTrack)
        }

        val hydratedTracks = candidateTracks.mapNotNull { track ->
            hydrateTrack(appContext, track, downloadsByTrackId[track.id], online)
        }

        if (hydratedTracks.isEmpty()) {
            return GentleWakeResolvedSource(
                tracks = listOf(builtInTrack),
                displayTitle = copy.builtInToneName,
                usingEmergencyFallback = true
            )
        }

        return GentleWakeResolvedSource(
            tracks = hydratedTracks,
            displayTitle = normalized.sourceTitle.ifBlank { hydratedTracks.first().title },
            usingEmergencyFallback = false
        )
    }

    private suspend fun playlistTracks(context: Context, settings: GentleWakeSettings): List<Track> {
        val fromStore = if (settings.sourceId.isNotBlank()) {
            PlaylistStore(context).load(settings.sourceId)?.tracks.orEmpty()
        } else {
            emptyList()
        }
        return fromStore.ifEmpty { decodePayloadTracks(settings.sourcePayloadJson) }
    }

    private suspend fun radioTracks(context: Context, settings: GentleWakeSettings): List<Track> {
        val payloadTracks = decodePayloadTracks(settings.sourcePayloadJson)
        if (payloadTracks.isNotEmpty()) return payloadTracks
        val radioStore = RadioStore(context)
        val station = (radioStore.favorites() + radioStore.customStations() + radioStore.recent())
            .firstOrNull { it.uuid == settings.sourceId || "live-radio:${it.uuid}" == settings.sourceId }
        return station?.let { listOf(it.toTrack(artwork = it.safeFaviconUrl)) }.orEmpty()
    }

    private fun localAudioTracks(
        settings: GentleWakeSettings,
        downloadsByTrackId: Map<String, DownloadEntity>
    ): List<Track> {
        val payloadTracks = decodePayloadTracks(settings.sourcePayloadJson)
        if (payloadTracks.isNotEmpty()) return payloadTracks
        val entity = downloadsByTrackId[settings.sourceId] ?: return emptyList()
        return listOf(
            Track(
                id = entity.trackId,
                title = entity.title,
                artist = entity.artist,
                album = entity.album,
                durationMs = entity.durationMs,
                streamUrl = entity.uri,
                videoUrl = "",
                thumbnailUrl = "",
                largeThumbnailUrl = "",
                source = "LocalDownload",
                moodTags = emptySet(),
                energy = 50,
                vocal = 50,
                replayScore = 80,
                cacheScore = 100,
                accentStart = 0xFFFFB77C.toInt(),
                accentEnd = 0xFF7B61FF.toInt()
            )
        )
    }

    private fun hydrateTrack(
        context: Context,
        track: Track,
        downloaded: DownloadEntity?,
        online: Boolean
    ): Track? {
        val localUri = downloaded?.uri?.takeIf { isLocalUriReadable(context, it) }
            ?: track.streamUrl.takeIf { isLocalUri(it) && isLocalUriReadable(context, it) }
        return when {
            localUri != null -> track.copy(streamUrl = localUri)
            online && (track.streamUrl.isNotBlank() || track.id.isNotBlank()) -> track
            else -> null
        }
    }

    fun encodePayloadTracks(tracks: List<Track>): String {
        val array = JSONArray()
        tracks.take(25).forEach { track ->
            array.put(TrackJson.toJson(track))
        }
        return array.toString()
    }

    fun decodePayloadTracks(raw: String): List<Track> {
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val trimmed = raw.trim()
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                buildList {
                    for (index in 0 until array.length()) {
                        val obj = array.optJSONObject(index) ?: continue
                        runCatching { TrackJson.fromJson(obj) }.getOrNull()?.let(::add)
                    }
                }
            } else {
                listOfNotNull(runCatching { TrackJson.fromJson(JSONObject(trimmed)) }.getOrNull())
            }
        }.getOrDefault(emptyList())
    }

    private fun isLocalUri(uriString: String): Boolean {
        return uriString.startsWith("file://") ||
            uriString.startsWith("content://") ||
            uriString.startsWith("/")
    }

    private fun isLocalUriReadable(context: Context, uriString: String): Boolean {
        if (uriString.isBlank()) return false
        return runCatching {
            when {
                uriString.startsWith("file://") -> {
                    val path = Uri.parse(uriString).path.orEmpty()
                    path.isNotBlank() && File(path).let { it.exists() && it.length() > 0L }
                }
                uriString.startsWith("/") -> {
                    File(uriString).let { it.exists() && it.length() > 0L }
                }
                uriString.startsWith("content://") -> {
                    context.contentResolver.openAssetFileDescriptor(Uri.parse(uriString), "r")?.use { afd ->
                        afd.length != 0L
                    } ?: false
                }
                else -> false
            }
        }.getOrDefault(false)
    }

    private fun isNetworkValidated(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val active = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(active) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
