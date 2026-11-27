package com.luc4n3x.levyra.data.youtubemusic

import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicPlaylistSyncPlannerTest {

    private fun track(
        id: String,
        title: String = "Track $id",
        audioVideoId: String = id,
        videoUrl: String = if (audioVideoId.isNotBlank()) "https://music.youtube.com/watch?v=$audioVideoId" else "",
        streamUrl: String = ""
    ) = Track(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = streamUrl,
        videoUrl = videoUrl,
        thumbnailUrl = "",
        largeThumbnailUrl = "",
        source = "test",
        moodTags = emptySet(),
        energy = 0,
        vocal = 0,
        replayScore = 0,
        cacheScore = 0,
        accentStart = 0,
        accentEnd = 0,
        audioVideoId = audioVideoId
    )

    private fun remote(videoId: String, setVideoId: String = "set-$videoId", title: String = "Remote $videoId") =
        YoutubeMusicRepository.RemotePlaylistItem(
            track = track(id = videoId, title = title, audioVideoId = videoId, streamUrl = "https://cached.example/$videoId"),
            videoId = videoId,
            setVideoId = setVideoId
        )

    @Test
    fun initialLinkMergesUniqueLocalAndRemoteTracksAndKeepsLocalOnlyTracks() {
        val vidA = "vidA0000001"
        val vidB = "vidB0000002"
        val vidC = "vidC0000003"
        val local = listOf(
            track(id = "loc-1", audioVideoId = vidA),
            track(id = "local:42", audioVideoId = "", streamUrl = "content://media/external/audio/1"),
            track(id = "loc-2", audioVideoId = vidB)
        )
        val remote = listOf(
            remote(vidB),
            remote(vidC)
        )

        val plan = planYoutubeMusicTwoWaySync(
            localName = "Road Trip",
            localTracks = local,
            remoteTitle = "YTM Road Trip",
            remoteItems = remote,
            baseline = null
        )

        assertEquals(listOf(vidA), plan.videoIdsToAddRemotely)
        assertTrue(plan.remoteItemsToRemove.isEmpty())
        assertEquals(listOf(vidC), plan.tracksToAddLocally.map { it.audioVideoId })
        assertEquals("", plan.tracksToAddLocally.single().streamUrl)
        assertTrue(plan.localTrackIdsToRemove.isEmpty())
        assertNull(plan.renameLocalTitle)
        assertNull(plan.renameRemoteTitle)
        assertEquals(1, plan.unmatchedLocalCount)
        assertEquals(listOf(vidB, vidC, vidA), plan.newBaselineVideoIds)
        assertEquals("YTM Road Trip", plan.newBaselineTitle)
    }

    @Test
    fun subsequentSyncPushesLocalAdditionsAndRemovalsAndPullsRemoteChanges() {
        val vidKeep = "vidKeep0001"
        val vidLocalAdd = "vidLocalAdd"
        val vidLocalRemoved = "vidLocRem01"
        val vidRemoteAdd = "vidRemAdd01"
        val vidRemoteRemoved = "vidRemRem01"
        val local = listOf(
            track(id = "t-keep", audioVideoId = vidKeep),
            track(id = "t-local-add", audioVideoId = vidLocalAdd),
            track(id = "local:99", audioVideoId = "", streamUrl = "file:///music/local.flac")
        )
        val remote = listOf(
            remote(vidKeep, "set-keep"),
            remote(vidLocalRemoved, "set-rem-1"),
            remote(vidLocalRemoved, "set-rem-2"),
            remote(vidRemoteAdd, "set-radd")
        )
        val baseline = PlaylistSyncBaseline(
            remoteTitle = "Shared Mix",
            videoIds = listOf(vidKeep, vidLocalRemoved, vidRemoteRemoved)
        )
        val localWithRemoteRemoved = local + track(id = "t-rem-removed", audioVideoId = vidRemoteRemoved)

        val plan = planYoutubeMusicTwoWaySync(
            localName = "Shared Mix",
            localTracks = localWithRemoteRemoved,
            remoteTitle = "Shared Mix",
            remoteItems = remote,
            baseline = baseline
        )

        assertEquals(listOf(vidLocalAdd), plan.videoIdsToAddRemotely)
        assertEquals(
            listOf(
                RemotePlaylistRemoval(vidLocalRemoved, "set-rem-1"),
                RemotePlaylistRemoval(vidLocalRemoved, "set-rem-2")
            ),
            plan.remoteItemsToRemove
        )
        assertEquals(listOf(vidRemoteAdd), plan.tracksToAddLocally.map { it.audioVideoId })
        assertEquals(setOf("t-rem-removed"), plan.localTrackIdsToRemove)
        assertEquals(1, plan.unmatchedLocalCount)
        assertEquals(listOf(vidKeep, vidRemoteAdd, vidLocalAdd), plan.newBaselineVideoIds)
    }

    @Test
    fun renamePropagationFollowsChangedSideAndBreaksTiesTowardLocal() {
        val baseline = PlaylistSyncBaseline(remoteTitle = "Original", videoIds = emptyList())

        val localRenamedOnly = planYoutubeMusicTwoWaySync(
            localName = "Renamed Local",
            localTracks = emptyList(),
            remoteTitle = "Original",
            remoteItems = emptyList(),
            baseline = baseline
        )
        assertEquals("Renamed Local", localRenamedOnly.renameRemoteTitle)
        assertNull(localRenamedOnly.renameLocalTitle)
        assertEquals("Renamed Local", localRenamedOnly.newBaselineTitle)

        val remoteRenamedOnly = planYoutubeMusicTwoWaySync(
            localName = "Original",
            localTracks = emptyList(),
            remoteTitle = "Renamed Remote",
            remoteItems = emptyList(),
            baseline = baseline
        )
        assertNull(remoteRenamedOnly.renameRemoteTitle)
        assertEquals("Renamed Remote", remoteRenamedOnly.renameLocalTitle)
        assertEquals("Renamed Remote", remoteRenamedOnly.newBaselineTitle)

        val bothRenamed = planYoutubeMusicTwoWaySync(
            localName = "Local Wins",
            localTracks = emptyList(),
            remoteTitle = "Remote Loses",
            remoteItems = emptyList(),
            baseline = baseline
        )
        assertEquals("Local Wins", bothRenamed.renameRemoteTitle)
        assertNull(bothRenamed.renameLocalTitle)
        assertEquals("Local Wins", bothRenamed.newBaselineTitle)
    }

    @Test
    fun canonicalIdsAndBaselineEncodingRoundTripCleanly() {
        assertEquals("PL12345", canonicalYoutubePlaylistId("  VLPL12345 "))
        assertEquals("https://music.youtube.com/playlist?list=PL12345", youtubeMusicPlaylistUrl("VLPL12345"))

        val encoded = encodeSyncedVideoIds(listOf("a", "b", "a", "", " c "))
        assertEquals("a,b,c", encoded)
        assertEquals(listOf("a", "b", "c"), decodeSyncedVideoIds(encoded))
    }
}
