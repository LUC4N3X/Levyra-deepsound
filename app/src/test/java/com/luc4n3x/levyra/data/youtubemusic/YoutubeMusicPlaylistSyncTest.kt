package com.luc4n3x.levyra.data.youtubemusic

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicPlaylistSyncTest {
    @Test
    fun firstSyncMergesBothSidesWithoutRemovingAnything() {
        val plan = plan(local = listOf(local("a", A), local("b", B)), remote = snapshot(row(B, "sb"), row(C, "sc")), baseline = null)

        assertEquals(listOf(A), plan.addRemote)
        assertEquals(listOf(C), plan.addLocal.map { it.videoId })
        assertTrue(plan.removeLocal.isEmpty())
        assertTrue(plan.removeRemote.isEmpty())
        assertNull(plan.renameRemote)
        assertNull(plan.renameLocal)
    }

    @Test
    fun localRemovalReachesRemoteThroughItsSetVideoId() {
        val plan = plan(local = listOf(local("a", A)), remote = snapshot(row(A, "sa"), row(B, "sb")), baseline = baseline(A, B))

        assertEquals(listOf(YoutubeMusicPlaylistRemoval(B, "sb")), plan.removeRemote)
        assertTrue(plan.addLocal.isEmpty())
    }

    @Test
    fun remoteRemovalReachesLocal() {
        val plan = plan(local = listOf(local("a", A), local("b", B)), remote = snapshot(row(A, "sa")), baseline = baseline(A, B))

        assertEquals(setOf("b"), plan.removeLocal)
        assertTrue(plan.addRemote.isEmpty())
    }

    @Test
    fun unavailableRemoteRowStillCountsAsMembership() {
        val root = page(rawRow(B, "sb", title = null))
        val parsed = YoutubeMusicPlaylistSnapshotParser.page(root)
        val plan = plan(
            local = listOf(local("b", B)),
            remote = YoutubeMusicPlaylistSnapshot("Mix", parsed.rows, complete = parsed.unidentifiedRows == 0),
            baseline = baseline(B)
        )

        assertEquals(listOf(B), parsed.rows.map { it.videoId })
        assertTrue(plan.removeLocal.isEmpty())
    }

    @Test
    fun incompleteSnapshotHoldsBackLocalRemovals() {
        val plan = plan(local = listOf(local("a", A), local("b", B)), remote = snapshot(row(A, "sa"), complete = false), baseline = baseline(A, B))

        assertTrue(plan.removeLocal.isEmpty())
        assertEquals(1, plan.heldBack)
        assertEquals(setOf(B), plan.pendingVideoIds)
    }

    @Test
    fun unverifiedEmptyRemoteWithHistoryHoldsBackLocalRemovals() {
        val plan = plan(local = listOf(local("a", A)), remote = snapshot(complete = false), baseline = baseline(A))

        assertTrue(plan.removeLocal.isEmpty())
        assertEquals(1, plan.heldBack)
    }

    @Test
    fun verifiedEmptyRemoteRemovesLocalTracks() {
        val plan = plan(local = listOf(local("a", A)), remote = snapshot(), baseline = baseline(A))

        assertEquals(setOf("a"), plan.removeLocal)
        assertEquals(0, plan.heldBack)
    }

    @Test
    fun localDeletionSurvivesAnIncompleteSnapshotThatOmitsIt() {
        val local = listOf(local("a", A))
        val remote = snapshot(row(A, "sa"), complete = false)
        val plan = plan(local = local, remote = remote, baseline = baseline(A, B))
        val settled = settlePlaylistSync(plan, local, remote, PlaylistSyncRemoteResult(), emptySet(), "Mix", "Mix")

        assertEquals(setOf(A, B), settled.videoIds)
        val next = plan(local = local, remote = snapshot(row(A, "sa"), row(B, "sb")), baseline = PlaylistSyncBaseline("Mix", "Mix", settled.videoIds))
        assertTrue(next.addLocal.isEmpty())
        assertEquals(listOf(YoutubeMusicPlaylistRemoval(B, "sb")), next.removeRemote)
    }

    @Test
    fun albumTracksStayLocalWhenRemovedRemotely() {
        val plan = plan(local = listOf(local("album-b", B, removable = false)), remote = snapshot(row(A, "sa")), baseline = baseline(A, B))

        assertTrue(plan.removeLocal.isEmpty())
        assertEquals(1, plan.keptLocal)
        assertTrue(B in plan.pendingVideoIds)
    }

    @Test
    fun remoteRowWithoutSetVideoIdIsNeverRemoved() {
        val plan = plan(local = emptyList(), remote = snapshot(row(A, "")), baseline = baseline(A))

        assertTrue(plan.removeRemote.isEmpty())
        assertTrue(A in plan.pendingVideoIds)
    }

    @Test
    fun localTitleChangeRenamesRemote() {
        val plan = plan(title = "Road trip", remote = snapshot(row(A, "sa"), title = "Mix"), baseline = baseline(A, title = "Mix"), local = listOf(local("a", A)))

        assertEquals("Road trip", plan.renameRemote)
        assertNull(plan.renameLocal)
    }

    @Test
    fun remoteTitleChangeRenamesLocal() {
        val plan = plan(title = "Mix", remote = snapshot(row(A, "sa"), title = "Night drive"), baseline = baseline(A, title = "Mix"), local = listOf(local("a", A)))

        assertEquals("Night drive", plan.renameLocal)
        assertNull(plan.renameRemote)
    }

    @Test
    fun firstSyncKeepsDifferentTitlesAndRemembersBoth() {
        val snapshot = snapshot(row(A, "sa"), title = "Remote name")
        val plan = plan(title = "Local name", remote = snapshot, baseline = null, local = listOf(local("a", A)))
        val settled = settlePlaylistSync(plan, listOf(local("a", A)), snapshot, PlaylistSyncRemoteResult(), emptySet(), "Local name", "")

        assertNull(plan.renameRemote)
        assertNull(plan.renameLocal)
        assertEquals("Local name", settled.localTitle)
        assertEquals("Remote name", settled.remoteTitle)
    }

    @Test
    fun rejectedAddIsRetriedAndRejectedRemovalIsRemembered() {
        val localTracks = listOf(local("a", A), local("c", C))
        val snapshot = snapshot(row(A, "sa"), row(B, "sb"))
        val plan = plan(local = localTracks, remote = snapshot, baseline = baseline(A, B))
        val settled = settlePlaylistSync(
            plan,
            localTracks,
            snapshot,
            PlaylistSyncRemoteResult(rejectedAdds = setOf(C), rejectedRemovals = setOf(B)),
            emptySet(),
            "Mix",
            "Mix"
        )

        assertEquals(setOf(A, B), settled.videoIds)
    }

    @Test
    fun remoteRowThatCannotBecomeLocalIsNotRemembered() {
        val snapshot = snapshot(row(A, "sa"), row(B, "sb"))
        val plan = plan(local = listOf(local("a", A)), remote = snapshot, baseline = null)
        val settled = settlePlaylistSync(plan, listOf(local("a", A)), snapshot, PlaylistSyncRemoteResult(), emptySet(), "Mix", "Mix")

        assertEquals(setOf(A), settled.videoIds)
    }

    @Test
    fun parserReadsEditableTitleAndMenuSetVideoId() {
        val root = JSONObject()
            .put("header", JSONObject().put("musicEditablePlaylistDetailHeaderRenderer", JSONObject()
                .put("editHeader", JSONObject().put("musicPlaylistEditHeaderRenderer", JSONObject()
                    .put("title", JSONObject().put("runs", JSONArray().put(JSONObject().put("text", "Road trip"))))))))
            .put("contents", page(rawRow(A, setVideoId = null, title = "Song", menuSetVideoId = "menu-set")).getJSONObject("contents"))
        val parsed = YoutubeMusicPlaylistSnapshotParser.page(root)

        assertEquals("Road trip", parsed.title)
        assertEquals("menu-set", parsed.rows.single().setVideoId)
    }

    @Test
    fun rowsWithoutVideoIdMakeTheSnapshotIncomplete() {
        val parsed = YoutubeMusicPlaylistSnapshotParser.page(page(JSONObject().put("musicResponsiveListItemRenderer", JSONObject())))

        assertTrue(parsed.rows.isEmpty())
        assertEquals(1, parsed.unidentifiedRows)
    }

    @Test
    fun firstPageWithoutPlaylistShelfIsNotTrusted() {
        val suggestions = JSONObject().put("musicShelfRenderer", JSONObject().put("contents", JSONArray().put(rawRow(A, "sa", "Song"))))
        val root = JSONObject().put("contents", JSONObject().put("sectionListRenderer", JSONObject().put("contents", JSONArray().put(suggestions))))
        val parsed = YoutubeMusicPlaylistSnapshotParser.page(root)

        assertFalse(parsed.recognized)
        assertTrue(parsed.rows.isEmpty())
    }

    @Test
    fun unknownShelfItemMakesTheSnapshotIncomplete() {
        val parsed = YoutubeMusicPlaylistSnapshotParser.page(page(rawRow(A, "sa", "Song"), JSONObject().put("messageRenderer", JSONObject())))

        assertEquals(1, parsed.rows.size)
        assertEquals(1, parsed.unidentifiedRows)
    }

    @Test
    fun continuationPageNeedsAContinuationContainer() {
        val items = JSONArray().put(rawRow(B, "sb", "Song"))
        val good = JSONObject().put("continuationContents", JSONObject().put("musicPlaylistShelfContinuation", JSONObject().put("contents", items)))

        assertTrue(YoutubeMusicPlaylistSnapshotParser.page(good, continuation = true).recognized)
        assertEquals(B, YoutubeMusicPlaylistSnapshotParser.page(good, continuation = true).rows.single().videoId)
        assertFalse(YoutubeMusicPlaylistSnapshotParser.page(JSONObject().put("responseContext", JSONObject()), continuation = true).recognized)
    }

    @Test
    fun remoteOnlySettleKeepsRemoteAddSoALocalRemovalPropagates() {
        val local = listOf(local("a", A), local("b", B))
        val remote = snapshot(row(A, "sa"))
        val plan = plan(local = local, remote = remote, baseline = baseline(A))
        val settled = settleRemoteOnlySync(plan, local, remote, PlaylistSyncRemoteResult(), "Mix", "Mix")

        assertEquals(setOf(A, B), settled.videoIds)
        val next = plan(local = listOf(local("a", A)), remote = snapshot(row(A, "sa"), row(B, "sb")), baseline = PlaylistSyncBaseline("Mix", "Mix", settled.videoIds))
        assertTrue(next.addLocal.isEmpty())
        assertEquals(listOf(YoutubeMusicPlaylistRemoval(B, "sb")), next.removeRemote)
    }

    @Test
    fun remoteOnlySettleKeepsDeferredRemovalsAndRemoteRename() {
        val local = listOf(local("a", A), local("b", B))
        val remote = snapshot(row(A, "sa"), title = "Renamed")
        val plan = plan(local = local, remote = remote, baseline = baseline(A, B))
        val settled = settleRemoteOnlySync(plan, local, remote, PlaylistSyncRemoteResult(), "Mix", "Mix")

        assertTrue(B in settled.videoIds)
        assertEquals("Mix", settled.remoteTitle)
        val next = plan(local = local, remote = remote, baseline = settled)
        assertEquals(setOf("b"), next.removeLocal)
        assertEquals("Renamed", next.renameLocal)
    }

    @Test
    fun longLocalTitleIsRememberedAsSent() {
        val long = "x".repeat(200)
        val plan = plan(local = emptyList(), remote = snapshot(title = "Mix"), baseline = baseline(), title = long)

        assertEquals(150, plan.renameRemote?.length)
        val settled = settlePlaylistSync(plan, emptyList(), snapshot(title = "Mix"), PlaylistSyncRemoteResult(), emptySet(), long, "Mix")
        val next = plan(local = emptyList(), remote = snapshot(title = long.take(150)), baseline = settled, title = long)
        assertNull(next.renameLocal)
        assertNull(next.renameRemote)
    }

    @Test
    fun emptyOrUnknownContinuationContainersAreRejected() {
        val empty = JSONObject().put("continuationContents", JSONObject())
        val unknown = JSONObject().put("continuationContents", JSONObject().put("musicPlaylistShelfContinuation", JSONObject()))

        assertFalse(YoutubeMusicPlaylistSnapshotParser.page(empty, continuation = true).recognized)
        assertFalse(YoutubeMusicPlaylistSnapshotParser.page(unknown, continuation = true).recognized)
    }

    @Test
    fun baselineCodecDropsInvalidIds() {
        val decoded = decodePlaylistSyncVideoIds(encodePlaylistSyncVideoIds(listOf(A, "bad", B, A)))

        assertEquals(setOf(A, B), decoded)
        assertFalse("bad" in decoded)
        assertEquals("PL123", canonicalYoutubeMusicPlaylistId("https://music.youtube.com/playlist?list=PL123&si=x"))
        assertEquals("PL123", canonicalYoutubeMusicPlaylistId("VLPL123"))
    }

    private fun plan(
        local: List<PlaylistSyncLocalTrack>,
        remote: YoutubeMusicPlaylistSnapshot,
        baseline: PlaylistSyncBaseline?,
        title: String = "Mix"
    ) = planYoutubeMusicPlaylistSync(title, local, remote, baseline)

    private fun local(key: String, videoId: String?, removable: Boolean = true) = PlaylistSyncLocalTrack(key, videoId, removable)

    private fun row(videoId: String, setVideoId: String) = YoutubeMusicPlaylistRow(videoId, setVideoId, JSONObject())

    private fun snapshot(vararg rows: YoutubeMusicPlaylistRow, title: String = "Mix", complete: Boolean = true) =
        YoutubeMusicPlaylistSnapshot(title, rows.toList(), complete)

    private fun baseline(vararg ids: String, title: String = "Mix") = PlaylistSyncBaseline(title, title, ids.toSet())

    private fun rawRow(videoId: String, setVideoId: String?, title: String?, menuSetVideoId: String? = null): JSONObject {
        val data = JSONObject().put("videoId", videoId)
        if (setVideoId != null) data.put("playlistSetVideoId", setVideoId)
        val renderer = JSONObject().put("playlistItemData", data)
        if (title != null) {
            renderer.put("flexColumns", JSONArray().put(JSONObject().put("musicResponsiveListItemFlexColumnRenderer",
                JSONObject().put("text", JSONObject().put("runs", JSONArray().put(JSONObject().put("text", title)))))))
        }
        if (menuSetVideoId != null) {
            val action = JSONObject().put("action", "ACTION_REMOVE_VIDEO").put("setVideoId", menuSetVideoId)
            val endpoint = JSONObject().put("playlistEditEndpoint", JSONObject().put("actions", JSONArray().put(action)))
            val item = JSONObject().put("menuServiceItemRenderer", JSONObject().put("serviceEndpoint", endpoint))
            renderer.put("menu", JSONObject().put("menuRenderer", JSONObject().put("items", JSONArray().put(item))))
        }
        return JSONObject().put("musicResponsiveListItemRenderer", renderer)
    }

    private fun page(vararg items: JSONObject): JSONObject {
        val shelf = JSONObject().put("musicPlaylistShelfRenderer", JSONObject().put("contents", JSONArray(items.toList())))
        val section = JSONObject().put("sectionListRenderer", JSONObject().put("contents", JSONArray().put(shelf)))
        val tab = JSONObject().put("tabRenderer", JSONObject().put("content", section))
        val browse = JSONObject().put("tabs", JSONArray().put(tab))
        return JSONObject().put("contents", JSONObject().put("singleColumnBrowseResultsRenderer", browse))
    }

    private companion object {
        const val A = "AAAAAAAAAAA"
        const val B = "BBBBBBBBBBB"
        const val C = "CCCCCCCCCCC"
    }
}
