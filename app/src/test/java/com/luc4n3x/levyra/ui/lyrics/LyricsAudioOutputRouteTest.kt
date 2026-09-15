package com.luc4n3x.levyra.ui.lyrics

import android.media.AudioDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LyricsAudioOutputRouteTest {
    @Test
    fun `system ordered routes preserve the platform routing decision`() {
        val speaker = route("speaker", AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, bluetooth = false)
        val buds = route("buds", AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, bluetooth = true)

        assertEquals(speaker, selectLyricsAudioOutputRoute(listOf(speaker, buds), systemOrdered = true))
    }

    @Test
    fun `legacy selection prefers a connected bluetooth media route`() {
        val speaker = route("speaker", AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, bluetooth = false)
        val buds = route("buds", AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, bluetooth = true)

        assertEquals(buds, selectLyricsAudioOutputRoute(listOf(speaker, buds), systemOrdered = false))
    }

    @Test
    fun `stable route keys do not persist the raw bluetooth address`() {
        val first = stableLyricsAudioRouteKey(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "AA:BB:CC:DD:EE:FF")
        val second = stableLyricsAudioRouteKey(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "AA:BB:CC:DD:EE:FF")

        assertEquals(first, second)
        assertFalse(first.orEmpty().contains("AA:BB"))
        assertEquals(null, stableLyricsAudioRouteKey(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, ""))
    }

    private fun route(name: String, type: Int, bluetooth: Boolean) = LyricsAudioOutputRoute(
        stableKey = name,
        displayName = name,
        type = type,
        bluetooth = bluetooth
    )
}
