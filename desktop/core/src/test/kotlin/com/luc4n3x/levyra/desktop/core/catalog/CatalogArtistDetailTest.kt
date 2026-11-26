package com.luc4n3x.levyra.desktop.core.catalog

import com.luc4n3x.levyra.desktop.core.model.CollectionKind
import com.luc4n3x.levyra.desktop.core.model.CollectionRef
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogArtistDetailTest {
    @Test
    fun parsesMonthlyListenersViewsAndDurationFromTheMusicProfile() {
        for ((label, expected) in listOf("5.6M views" to 5_600_000L, "9K views" to 9_000L, "791 views" to 791L, "2001 remaster" to 0L)) {
            assertTrackViewCount(label, expected)
        }
    }

    private fun assertTrackViewCount(label: String, expected: Long) {
        val response = JSONObject(
            """{"header":{"musicImmersiveHeaderRenderer":{
                "title":{"runs":[{"text":"Artist"}]},
                "monthlyListenerCount":{"runs":[{"text":"4.2M monthly listeners"}]}
            }},"contents":{"musicResponsiveListItemRenderer":{
                "playlistItemData":{"videoId":"video"},
                "flexColumns":[
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Song"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Artist"}]}}},
                    {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"$label"}]}}}
                ],
                "fixedColumns":[{"musicResponsiveListItemFixedColumnRenderer":{"text":{"runs":[{"text":"3:22"}]}}}]
            }}}"""
        )
        val ref = CollectionRef("UCartist", "Artist", url = "https://www.youtube.com/channel/UCartist", kind = CollectionKind.ARTIST)
        val artist = requireNotNull(YoutubeMusicArtistPage.parse(response, ref))
        assertEquals(4_200_000L, artist.monthlyListeners)
        assertEquals(expected, artist.tracks.single().viewCount)
        assertEquals(202_000L, artist.tracks.single().durationMs)
    }

    @Test
    fun trackLikeResolvedChannelNameDoesNotReplaceArtistName() {
        assertEquals(
            "HUGEL",
            chooseDesktopArtistName(
                requestedName = "HUGEL",
                resolvedName = "HUGEL - Isihloko"
            )
        )
    }

    @Test
    fun officialArtistNameIsKeptWhenItIsNotAFalseTrackTitle() {
        assertEquals(
            "The Weeknd",
            chooseDesktopArtistName(
                requestedName = "Weeknd",
                resolvedName = "The Weeknd"
            )
        )
    }

    @Test
    fun youtubePortraitIsPromotedToHighResolution() {
        val upgraded = desktopArtistArtworkUrl(
            "https://yt3.googleusercontent.com/example=s88-c-k-c0x00ffffff-no-rj",
            size = 720
        )

        assertTrue(upgraded.endsWith("=s720-c-k-c0x00ffffff-no-rj"))
    }

    @Test
    fun channelTabsAreRecognizedWithLocalizedLabels() {
        assertEquals(
            DesktopArtistTabKind.TRACKS,
            desktopArtistTabKind(listOf("Brani"))
        )
        assertEquals(
            DesktopArtistTabKind.ALBUMS,
            desktopArtistTabKind(listOf("Discografia album"))
        )
        assertEquals(
            DesktopArtistTabKind.VIDEOS,
            desktopArtistTabKind(listOf("Music videos"))
        )
    }
}
