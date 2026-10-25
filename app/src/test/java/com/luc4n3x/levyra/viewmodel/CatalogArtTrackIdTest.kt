package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.YoutubeMusicWatchPlaylist
import com.luc4n3x.levyra.data.YoutubeMusicWatchTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogArtTrackIdTest {
    private fun watch(id: String, type: String, counterpart: YoutubeMusicWatchTrack? = null) = YoutubeMusicWatchTrack(
        videoId = id,
        title = "t",
        artists = emptyList(),
        albumTitle = "",
        albumBrowseId = "",
        durationMs = 0L,
        thumbnailUrl = "",
        videoType = type,
        explicit = false,
        counterpart = counterpart
    )

    private fun playlist(vararg tracks: YoutubeMusicWatchTrack) =
        YoutubeMusicWatchPlaylist(tracks.toList(), "", "", "", "")

    @Test
    fun acceptsOnlyTheOfficialArtTrackCounterpartOfTheSameVideo() {
        val atv = watch("ATVsong0001", "MUSIC_VIDEO_TYPE_ATV")
        assertEquals("ATVsong0001", catalogArtTrackId("OMVvideo001", playlist(watch("OMVvideo001", "MUSIC_VIDEO_TYPE_OMV", atv))))
    }

    @Test
    fun rejectsOtherVideosNonArtTrackCounterpartsAndMissingData() {
        val atv = watch("ATVsong0001", "MUSIC_VIDEO_TYPE_ATV")
        assertNull(catalogArtTrackId("OMVvideo001", playlist(watch("OTHERvideo1", "MUSIC_VIDEO_TYPE_OMV", atv))))
        assertNull(catalogArtTrackId("OMVvideo001", playlist(watch("OMVvideo001", "MUSIC_VIDEO_TYPE_OMV", watch("UGCvideo001", "MUSIC_VIDEO_TYPE_UGC")))))
        assertNull(catalogArtTrackId("OMVvideo001", playlist(watch("OMVvideo001", "MUSIC_VIDEO_TYPE_OMV"))))
        assertNull(catalogArtTrackId("OMVvideo001", null))
    }
}
