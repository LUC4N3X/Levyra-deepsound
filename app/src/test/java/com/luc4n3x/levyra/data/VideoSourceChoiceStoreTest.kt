package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.feature.radio.MemoryRadioPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoSourceChoiceStoreTest {

    @Test
    fun manualChoiceSurvivesStoreRecreation() {
        val preferences = MemoryRadioPreferences()
        VideoSourceChoiceStore(preferences).put("48ydDUQ16RE", VideoSourceChoice("CTFtOOh47oo", "MUSIC_VIDEO_TYPE_OMV"))

        assertEquals(
            VideoSourceChoice("CTFtOOh47oo", "MUSIC_VIDEO_TYPE_OMV"),
            VideoSourceChoiceStore(preferences).get("48ydDUQ16RE")
        )
    }

    @Test
    fun storeStaysBoundedByEvictingTheOldestChoice() {
        var now = 0L
        val store = VideoSourceChoiceStore(MemoryRadioPreferences(), clock = { ++now }, capacity = 2)
        store.put("track-one", VideoSourceChoice("aaaaaaaaaaa", ""))
        store.put("track-two", VideoSourceChoice("bbbbbbbbbbb", ""))
        store.put("track-one", VideoSourceChoice("ccccccccccc", ""))
        store.put("track-three", VideoSourceChoice("ddddddddddd", ""))

        assertNull(store.get("track-two"))
        assertEquals("ccccccccccc", store.get("track-one")?.videoId)
        assertEquals("ddddddddddd", store.get("track-three")?.videoId)
    }

    @Test
    fun invalidVideoIdsAreNeitherStoredNorReturned() {
        val store = VideoSourceChoiceStore(MemoryRadioPreferences())
        store.put("track", VideoSourceChoice("not-an-id", ""))

        assertNull(store.get("track"))
        assertNull(store.get(""))
    }
}
