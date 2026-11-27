package com.luc4n3x.levyra.data.youtubemusic

import com.luc4n3x.levyra.data.YoutubeMusicPlaylistPageParser
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

internal data class YoutubeMusicPlaylistRow(
    val videoId: String,
    val setVideoId: String,
    val renderer: JSONObject
)

internal data class YoutubeMusicPlaylistRemoval(val videoId: String, val setVideoId: String)

internal data class YoutubeMusicPlaylistSnapshotPage(
    val title: String,
    val rows: List<YoutubeMusicPlaylistRow>,
    val unidentifiedRows: Int,
    val continuation: String,
    val recognized: Boolean = true
)

internal data class YoutubeMusicPlaylistSnapshot(
    val title: String,
    val rows: List<YoutubeMusicPlaylistRow>,
    val complete: Boolean
)

internal data class PlaylistSyncBaseline(
    val localTitle: String,
    val remoteTitle: String,
    val videoIds: Set<String>
)

internal data class PlaylistSyncLocalTrack(
    val key: String,
    val videoId: String?,
    val removable: Boolean = true
)

internal data class PlaylistSyncPlan(
    val addRemote: List<String>,
    val removeRemote: List<YoutubeMusicPlaylistRemoval>,
    val addLocal: List<YoutubeMusicPlaylistRow>,
    val removeLocal: Set<String>,
    val renameRemote: String?,
    val renameLocal: String?,
    val pendingVideoIds: Set<String>,
    val keptLocal: Int,
    val heldBack: Int,
    val unmatched: Int
)

internal data class PlaylistSyncRemoteResult(
    val rejectedAdds: Set<String> = emptySet(),
    val rejectedRemovals: Set<String> = emptySet()
)

internal fun canonicalYoutubeMusicPlaylistId(raw: String): String =
    raw.trim().substringAfter("list=").substringBefore('&').removePrefix("VL").trim()

internal fun youtubeMusicPlaylistUrl(raw: String): String =
    "${YoutubeMusicAuth.ORIGIN}/playlist?list=${canonicalYoutubeMusicPlaylistId(raw)}"

internal fun encodePlaylistSyncVideoIds(videoIds: Collection<String>): String =
    videoIds.asSequence().map(String::trim).filter(YOUTUBE_MUSIC_VIDEO_ID::matches).distinct().joinToString(",")

internal fun decodePlaylistSyncVideoIds(raw: String): Set<String> =
    raw.split(',').asSequence().map(String::trim).filter(YOUTUBE_MUSIC_VIDEO_ID::matches).toCollection(LinkedHashSet())

internal object YoutubeMusicPlaylistSnapshotParser {
    private val headerKeys = listOf(
        "musicPlaylistEditHeaderRenderer",
        "musicResponsiveHeaderRenderer",
        "musicDetailHeaderRenderer",
        "musicEditablePlaylistDetailHeaderRenderer"
    )

    fun page(root: JSONObject, continuation: Boolean = false): YoutubeMusicPlaylistSnapshotPage {
        val recognized = if (continuation) {
            YoutubeMusicPlaylistPageParser.hasContinuationContainer(root)
        } else {
            YoutubeMusicPlaylistPageParser.hasPlaylistShelf(root)
        }
        if (!recognized) return YoutubeMusicPlaylistSnapshotPage(title(root), emptyList(), 0, "", recognized = false)
        var unidentified = YoutubeMusicPlaylistPageParser.unrecognizedItems(root)
        val rows = YoutubeMusicPlaylistPageParser.renderers(root).mapNotNull { renderer ->
            val data = renderer.optJSONObject("playlistItemData")
            val videoId = data?.optString("videoId")?.trim()?.takeIf(YOUTUBE_MUSIC_VIDEO_ID::matches)
                ?: firstObject(renderer, "watchEndpoint")?.optString("videoId")?.trim()?.takeIf(YOUTUBE_MUSIC_VIDEO_ID::matches)
            if (videoId == null) {
                unidentified++
                return@mapNotNull null
            }
            YoutubeMusicPlaylistRow(videoId, setVideoId(renderer, data), renderer)
        }
        return YoutubeMusicPlaylistSnapshotPage(title(root), rows, unidentified, YoutubeMusicPlaylistPageParser.continuation(root))
    }

    private fun setVideoId(renderer: JSONObject, data: JSONObject?): String {
        data?.optString("playlistSetVideoId")?.trim()?.takeIf(String::isNotBlank)?.let { return it }
        val removals = mutableListOf<JSONObject>()
        collect(renderer.optJSONObject("menu"), "playlistEditEndpoint", removals)
        removals.forEach { endpoint ->
            val actions = endpoint.optJSONArray("actions") ?: return@forEach
            for (index in 0 until actions.length()) {
                val action = actions.optJSONObject(index) ?: continue
                if (action.optString("action") == "ACTION_REMOVE_VIDEO") {
                    action.optString("setVideoId").trim().takeIf(String::isNotBlank)?.let { return it }
                }
            }
        }
        return ""
    }

    private fun title(root: JSONObject): String {
        val header = root.optJSONObject("header") ?: root.optJSONObject("contents") ?: return ""
        headerKeys.forEach { key ->
            val found = mutableListOf<JSONObject>()
            collect(header, key, found)
            found.forEach { renderer ->
                val title = renderer.optJSONObject("title") ?: return@forEach
                val runs = title.optJSONArray("runs")
                val text = if (runs != null) {
                    (0 until runs.length()).joinToString("") { runs.optJSONObject(it)?.optString("text").orEmpty() }
                } else {
                    title.optString("simpleText")
                }
                text.trim().takeIf(String::isNotBlank)?.let { return it }
            }
        }
        return ""
    }

    private fun firstObject(value: Any?, key: String): JSONObject? {
        val found = mutableListOf<JSONObject>()
        collect(value, key, found, limit = 1)
        return found.firstOrNull()
    }

    private fun collect(value: Any?, key: String, output: MutableList<JSONObject>, limit: Int = Int.MAX_VALUE) {
        if (output.size >= limit) return
        when (value) {
            is JSONObject -> {
                value.optJSONObject(key)?.let { output += it }
                val keys = value.keys()
                while (keys.hasNext() && output.size < limit) {
                    val name = keys.next()
                    if (name != key) collect(value.opt(name), key, output, limit)
                }
            }
            is JSONArray -> for (index in 0 until value.length()) {
                if (output.size >= limit) return
                collect(value.opt(index), key, output, limit)
            }
        }
    }
}

internal data class YoutubeMusicOwnedPlaylist(
    val playlistId: String,
    val title: String,
    val subtitle: String,
    val thumbnailUrl: String
)

internal val YOUTUBE_MUSIC_SYSTEM_PLAYLISTS = setOf("VLLM", "VLSE")

internal object YoutubeMusicOwnedPlaylistParser {
    private val rendererKeys = listOf("musicTwoRowItemRenderer", "musicResponsiveListItemRenderer")

    fun page(root: JSONObject): YoutubeMusicPage<YoutubeMusicOwnedPlaylist> {
        val content = YoutubeMusicAccountParser.content(root) ?: throw YoutubeMusicProtocolException()
        val owned = YoutubeMusicAccountParser.ownedPlaylistBrowseIds(content)
        val items = rendererKeys.flatMap { key -> YoutubeMusicAccountParser.objects(content, key) }.mapNotNull { renderer ->
            val browseId = YoutubeMusicAccountParser.objects(renderer, "browseEndpoint")
                .map { it.optString("browseId") }
                .firstOrNull { it.startsWith("VL") }
                ?: return@mapNotNull null
            if (browseId !in owned || browseId in YOUTUBE_MUSIC_SYSTEM_PLAYLISTS) return@mapNotNull null
            val columns = YoutubeMusicAccountParser.objects(renderer.optJSONArray("flexColumns"), "musicResponsiveListItemFlexColumnRenderer")
            val title = YoutubeMusicAccountParser.text(renderer.optJSONObject("title"))
                .ifBlank { columns.firstOrNull()?.let { YoutubeMusicAccountParser.text(it.optJSONObject("text")) }.orEmpty() }
            val subtitle = YoutubeMusicAccountParser.text(renderer.optJSONObject("subtitle"))
                .ifBlank { columns.getOrNull(1)?.let { YoutubeMusicAccountParser.text(it.optJSONObject("text")) }.orEmpty() }
            if (title.isBlank()) return@mapNotNull null
            YoutubeMusicOwnedPlaylist(canonicalYoutubeMusicPlaylistId(browseId), title.trim(), subtitle.trim(), YoutubeMusicAccountParser.thumbnail(renderer))
        }.distinctBy { it.playlistId }
        return YoutubeMusicPage(items, YoutubeMusicAccountParser.continuation(root))
    }
}

internal suspend fun YoutubeMusicAccountClient.playlistSnapshot(
    session: YoutubeMusicSession,
    playlistId: String,
    language: String
): YoutubeMusicPlaylistSnapshot {
    val browseId = "VL${canonicalYoutubeMusicPlaylistId(playlistId)}"
    var title = ""
    var unidentified = 0
    val rows = collectYoutubeMusicPages(key = { row: YoutubeMusicPlaylistRow -> row.setVideoId.ifBlank { row.videoId } }) { continuation ->
        val root = browse(session, browseId, language, continuation)
        if (continuation.isBlank() && !root.has("contents") && !root.has("header")) throw YoutubeMusicProtocolException()
        val page = YoutubeMusicPlaylistSnapshotParser.page(root, continuation = continuation.isNotBlank())
        if (continuation.isNotBlank() && !page.recognized) throw YoutubeMusicProtocolException()
        if (continuation.isBlank()) {
            title = page.title
            if (!page.recognized) unidentified++
        }
        unidentified += page.unidentifiedRows
        YoutubeMusicPage(page.rows, page.continuation)
    }
    return YoutubeMusicPlaylistSnapshot(title, rows, complete = unidentified == 0)
}

internal suspend fun YoutubeMusicAccountClient.applyRemotePlan(
    session: YoutubeMusicSession,
    playlistId: String,
    plan: PlaylistSyncPlan
): PlaylistSyncRemoteResult {
    plan.renameRemote?.let { renamePlaylist(session, playlistId, it) }
    val rejectedRemovals = removeFromPlaylist(session, playlistId, plan.removeRemote)
    val rejectedAdds = addToPlaylist(session, playlistId, plan.addRemote)
    return PlaylistSyncRemoteResult(rejectedAdds, rejectedRemovals)
}

internal fun planYoutubeMusicPlaylistSync(
    localTitle: String,
    local: List<PlaylistSyncLocalTrack>,
    snapshot: YoutubeMusicPlaylistSnapshot,
    baseline: PlaylistSyncBaseline?
): PlaylistSyncPlan {
    val localByVideo = LinkedHashMap<String, MutableList<PlaylistSyncLocalTrack>>()
    var unmatched = 0
    local.forEach { track ->
        val videoId = track.videoId?.takeIf(YOUTUBE_MUSIC_VIDEO_ID::matches)
        if (videoId == null) unmatched++ else localByVideo.getOrPut(videoId) { mutableListOf() } += track
    }
    val remoteByVideo = LinkedHashMap<String, MutableList<YoutubeMusicPlaylistRow>>()
    snapshot.rows.forEach { row -> remoteByVideo.getOrPut(row.videoId) { mutableListOf() } += row }
    val known = baseline?.videoIds.orEmpty()
    val pending = LinkedHashSet<String>()
    if (!snapshot.complete) known.forEach { if (it !in remoteByVideo) pending += it }
    val removals = planLocalRemovals(localByVideo, remoteByVideo.keys, known, snapshot.complete, pending)
    val renames = planRenames(localTitle, snapshot.title, baseline)
    return PlaylistSyncPlan(
        addRemote = localByVideo.keys.filter { it !in remoteByVideo && it !in known },
        removeRemote = planRemoteRemovals(remoteByVideo, localByVideo.keys, known, pending),
        addLocal = remoteByVideo.filterKeys { it !in localByVideo && it !in known }.values.map { it.first() },
        removeLocal = removals.keys,
        renameRemote = renames.first,
        renameLocal = renames.second,
        pendingVideoIds = pending,
        keptLocal = removals.kept,
        heldBack = removals.heldBack,
        unmatched = unmatched
    )
}

private class LocalRemovals(val keys: Set<String>, val kept: Int, val heldBack: Int)

private fun planLocalRemovals(
    localByVideo: Map<String, List<PlaylistSyncLocalTrack>>,
    remote: Set<String>,
    known: Set<String>,
    trusted: Boolean,
    pending: MutableSet<String>
): LocalRemovals {
    val keys = LinkedHashSet<String>()
    var kept = 0
    var heldBack = 0
    localByVideo.forEach { (videoId, tracks) ->
        if (videoId in remote || videoId !in known) return@forEach
        when {
            !trusted -> {
                heldBack++
                pending += videoId
            }
            tracks.all { it.removable } -> tracks.forEach { keys += it.key }
            else -> {
                kept++
                pending += videoId
                tracks.filter { it.removable }.forEach { keys += it.key }
            }
        }
    }
    return LocalRemovals(keys, kept, heldBack)
}

private fun planRemoteRemovals(
    remoteByVideo: Map<String, List<YoutubeMusicPlaylistRow>>,
    local: Set<String>,
    known: Set<String>,
    pending: MutableSet<String>
): List<YoutubeMusicPlaylistRemoval> {
    val removals = mutableListOf<YoutubeMusicPlaylistRemoval>()
    remoteByVideo.forEach { (videoId, rows) ->
        if (videoId in local || videoId !in known) return@forEach
        val removable = rows.filter { it.setVideoId.isNotBlank() }
        if (removable.size == rows.size) {
            removable.forEach { removals += YoutubeMusicPlaylistRemoval(videoId, it.setVideoId) }
        } else {
            pending += videoId
        }
    }
    return removals
}

private fun planRenames(localTitle: String, remoteTitle: String, baseline: PlaylistSyncBaseline?): Pair<String?, String?> {
    val cleanLocal = localTitle.trim()
    val cleanRemote = remoteTitle.trim()
    if (baseline == null || cleanLocal.isBlank() || cleanRemote.isBlank() || cleanLocal == cleanRemote) return null to null
    return when {
        cleanLocal != baseline.localTitle.trim() -> cleanLocal.take(YOUTUBE_MUSIC_PLAYLIST_TITLE_LIMIT) to null
        cleanRemote != baseline.remoteTitle.trim() -> null to cleanRemote
        else -> null to null
    }
}

internal fun settlePlaylistSync(
    plan: PlaylistSyncPlan,
    local: List<PlaylistSyncLocalTrack>,
    snapshot: YoutubeMusicPlaylistSnapshot,
    remote: PlaylistSyncRemoteResult,
    addedLocally: Set<String>,
    localTitle: String,
    knownRemoteTitle: String
): PlaylistSyncBaseline {
    val stillLocal = local.filterNot { it.key in plan.removeLocal }.mapNotNullTo(hashSetOf()) { it.videoId }
    val postLocal = stillLocal + addedLocally
    val removedRemotely = plan.removeRemote.mapTo(hashSetOf()) { it.videoId } - remote.rejectedRemovals
    val postRemote = snapshot.rows.mapTo(hashSetOf()) { it.videoId } - removedRemotely
    val ids = LinkedHashSet<String>()
    snapshot.rows.forEach { if (it.videoId in postLocal && it.videoId in postRemote) ids += it.videoId }
    plan.addRemote.forEach { if (it !in remote.rejectedAdds) ids += it }
    ids += plan.pendingVideoIds
    ids += remote.rejectedRemovals
    val finalLocalTitle = plan.renameLocal ?: localTitle.trim()
    val finalRemoteTitle = plan.renameRemote ?: snapshot.title.trim().ifBlank { knownRemoteTitle.trim().ifBlank { finalLocalTitle } }
    return PlaylistSyncBaseline(finalLocalTitle, finalRemoteTitle, ids)
}

internal fun settleRemoteOnlySync(
    plan: PlaylistSyncPlan,
    local: List<PlaylistSyncLocalTrack>,
    snapshot: YoutubeMusicPlaylistSnapshot,
    remote: PlaylistSyncRemoteResult,
    localTitle: String,
    knownRemoteTitle: String
): PlaylistSyncBaseline {
    val deferredRemovals = local.filter { it.key in plan.removeLocal }.mapNotNull { it.videoId }
    val remoteOnly = plan.copy(
        addLocal = emptyList(),
        removeLocal = emptySet(),
        renameLocal = null,
        pendingVideoIds = plan.pendingVideoIds + deferredRemovals
    )
    val settled = settlePlaylistSync(remoteOnly, local, snapshot, remote, emptySet(), localTitle, knownRemoteTitle)
    return if (plan.renameLocal != null) settled.copy(remoteTitle = knownRemoteTitle.trim()) else settled
}

internal const val YOUTUBE_MUSIC_PLAYLIST_TITLE_LIMIT = 150

private val YOUTUBE_MUSIC_VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

internal class KeyedMutex {
    private class Entry(val mutex: Mutex = Mutex(), var holders: Int = 0)

    private val entries = HashMap<String, Entry>()

    suspend fun <T> withLock(key: String, block: suspend () -> T): T {
        val entry = synchronized(entries) { entries.getOrPut(key) { Entry() }.also { it.holders++ } }
        try {
            return entry.mutex.withLock { block() }
        } finally {
            synchronized(entries) {
                entry.holders--
                if (entry.holders == 0) entries.remove(key)
            }
        }
    }

    fun size(): Int = synchronized(entries) { entries.size }
}
