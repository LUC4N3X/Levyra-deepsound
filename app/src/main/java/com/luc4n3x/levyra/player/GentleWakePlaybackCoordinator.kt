package com.luc4n3x.levyra.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.luc4n3x.levyra.data.PlaylistStore
import com.luc4n3x.levyra.data.TrackJson
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

        val candidateTracks: List<Track> = when (normalized.sourceType) {
            GentleWakeSourceType.PLAYLIST -> {
                val fromStore = if (normalized.sourceId.isNotBlank()) {
                    PlaylistStore(appContext).load(normalized.sourceId)?.tracks.orEmpty()
                } else {
                    emptyList()
                }
                fromStore.ifEmpty { decodePayloadTracks(normalized.sourcePayloadJson) }
            }
            GentleWakeSourceType.ALBUM -> {
                decodePayloadTracks(normalized.sourcePayloadJson)
            }
            GentleWakeSourceType.RADIO -> {
                val payloadTracks = decodePayloadTracks(normalized.sourcePayloadJson)
                if (payloadTracks.isNotEmpty()) {
                    payloadTracks
                } else {
                    val radioStore = RadioStore(appContext)
                    val station = (radioStore.favorites() + radioStore.customStations() + radioStore.recent())
                        .firstOrNull { it.uuid == normalized.sourceId || "live-radio:${it.uuid}" == normalized.sourceId }
                    station?.let { listOf(it.toTrack(artwork = it.safeFaviconUrl)) }.orEmpty()
                }
            }
            GentleWakeSourceType.LOCAL_AUDIO -> {
                val payloadTracks = decodePayloadTracks(normalized.sourcePayloadJson)
                if (payloadTracks.isNotEmpty()) {
                    payloadTracks
                } else {
                    val entity = downloadsByTrackId[normalized.sourceId]
                    if (entity != null) {
                        listOf(
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
                    } else {
                        emptyList()
                    }
                }
            }
            GentleWakeSourceType.BUILT_IN_TONE -> listOf(builtInTrack)
        }

        val hydratedTracks = candidateTracks.mapNotNull { track ->
            val downloaded = downloadsByTrackId[track.id]
            val localUri = downloaded?.uri?.takeIf { isLocalUriReadable(appContext, it) }
                ?: track.streamUrl.takeIf { isLocalUri(it) && isLocalUriReadable(appContext, it) }
            when {
                localUri != null -> track.copy(streamUrl = localUri)
                online && (track.streamUrl.isNotBlank() || track.id.isNotBlank()) -> track
                else -> null
            }
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
