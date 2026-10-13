package com.luc4n3x.levyra.data.youtubemusic

import android.content.Context
import android.util.AtomicFile
import com.luc4n3x.levyra.data.TrackJson
import com.luc4n3x.levyra.domain.AlbumHit
import com.luc4n3x.levyra.domain.HomeSection
import com.luc4n3x.levyra.domain.Track
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal sealed interface YoutubeMusicHomeSource {
    data object Standard : YoutubeMusicHomeSource
    data object Pending : YoutubeMusicHomeSource
    data class Ready(val home: YoutubeMusicHome) : YoutubeMusicHomeSource
}

internal fun youtubeMusicHomeSource(
    restoring: Boolean,
    activeAccountId: String?,
    home: YoutubeMusicHome?,
    homeFailed: Boolean = false
): YoutubeMusicHomeSource = when {
    restoring && home != null -> YoutubeMusicHomeSource.Ready(home)
    restoring -> YoutubeMusicHomeSource.Pending
    activeAccountId == null -> YoutubeMusicHomeSource.Standard
    home != null && home.accountId == activeAccountId -> YoutubeMusicHomeSource.Ready(home)
    homeFailed -> YoutubeMusicHomeSource.Standard
    else -> YoutubeMusicHomeSource.Pending
}

internal fun youtubeMusicHomeNeedsFetch(
    activeAccountId: String?,
    languageCode: String,
    fetchedKey: String?,
    inFlightKey: String?
): Boolean {
    if (activeAccountId == null) return false
    val key = youtubeMusicHomeKey(activeAccountId, languageCode)
    return key != fetchedKey && key != inFlightKey
}

internal fun youtubeMusicHomeKey(accountId: String, languageCode: String): String = "$accountId|$languageCode"

internal object YoutubeMusicHomeCodec {
    private const val VERSION = 1
    private const val MAX_SECTION_TRACKS = 24
    private const val MAX_SECTIONS = 16
    private const val MAX_ALBUMS = 24

    fun encode(home: YoutubeMusicHome): String = JSONObject()
        .put("version", VERSION)
        .put("accountId", home.accountId)
        .put("languageCode", home.languageCode)
        .put("orbit", tracks(home.orbit))
        .put("sections", JSONArray().apply {
            home.sections.take(MAX_SECTIONS).forEach { section ->
                put(JSONObject().put("title", section.title).put("tracks", tracks(section.tracks.take(MAX_SECTION_TRACKS))))
            }
        })
        .put("albums", JSONArray().apply { home.albums.take(MAX_ALBUMS).forEach { put(album(it)) } })
        .toString()

    fun decode(raw: String, expectedAccountId: String?): YoutubeMusicHome? {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        if (root.optInt("version") != VERSION) return null
        val accountId = root.optString("accountId")
        if (accountId.isBlank() || (expectedAccountId != null && accountId != expectedAccountId)) return null
        val orbit = parseTracks(root.optJSONArray("orbit"))
        val sections = root.optJSONArray("sections").objects().mapNotNull { item ->
            val sectionTracks = parseTracks(item.optJSONArray("tracks"))
            HomeSection(item.optString("title"), sectionTracks).takeIf { sectionTracks.isNotEmpty() }
        }
        val albums = root.optJSONArray("albums").objects().mapNotNull(::parseAlbum)
        if (orbit.isEmpty() && sections.isEmpty() && albums.isEmpty()) return null
        return YoutubeMusicHome(accountId, root.optString("languageCode"), orbit, sections, albums)
    }

    private fun tracks(items: List<Track>): JSONArray = JSONArray().apply {
        items.forEach { put(TrackJson.toJson(it.copy(streamUrl = "", videoStreamUrl = ""))) }
    }

    private fun parseTracks(array: JSONArray?): List<Track> =
        array.objects().mapNotNull { runCatching { TrackJson.fromJson(it) }.getOrNull() }.filter { it.id.isNotBlank() }

    private fun album(album: AlbumHit): JSONObject = JSONObject()
        .put("title", album.title)
        .put("artist", album.artist)
        .put("year", album.year)
        .put("thumbnailUrl", album.thumbnailUrl)
        .put("query", album.query)
        .put("browseId", album.browseId)
        .put("artistBrowseId", album.artistBrowseId)
        .put("audioPlaylistId", album.audioPlaylistId)
        .put("explicit", album.explicit)

    private fun parseAlbum(item: JSONObject): AlbumHit? {
        val title = item.optString("title").trim()
        val artist = item.optString("artist").trim()
        if (title.isBlank()) return null
        return AlbumHit(
            title = title,
            artist = artist,
            year = item.optString("year"),
            thumbnailUrl = item.optString("thumbnailUrl"),
            query = item.optString("query").ifBlank { "$title $artist".trim() },
            browseId = item.optString("browseId"),
            artistBrowseId = item.optString("artistBrowseId"),
            audioPlaylistId = item.optString("audioPlaylistId"),
            explicit = item.optBoolean("explicit")
        )
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
}

internal class YoutubeMusicHomeCache(context: Context) {
    private val file = AtomicFile(File(context.applicationContext.noBackupFilesDir, "youtube-music-home.json"))

    fun load(accountId: String?): YoutubeMusicHome? {
        if (!file.baseFile.exists() || file.baseFile.length() > MAX_BYTES) return null
        val raw = runCatching { file.readFully().toString(Charsets.UTF_8) }.getOrNull() ?: return null
        return YoutubeMusicHomeCodec.decode(raw, accountId)
    }

    fun save(home: YoutubeMusicHome) {
        val bytes = YoutubeMusicHomeCodec.encode(home).toByteArray(Charsets.UTF_8)
        if (bytes.size > MAX_BYTES) return
        val output = file.startWrite()
        try {
            output.write(bytes)
            file.finishWrite(output)
        } catch (error: Exception) {
            file.failWrite(output)
            throw error
        }
    }

    fun clear() {
        file.delete()
    }

    private companion object {
        const val MAX_BYTES = 2L * 1024 * 1024
    }
}
