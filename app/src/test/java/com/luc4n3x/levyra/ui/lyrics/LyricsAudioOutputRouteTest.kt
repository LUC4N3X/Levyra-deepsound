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

    @Test
    fun `stable route keys return null when reliable address is unavailable`() {
        assertEquals(null, stableLyricsAudioRouteKey(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, null))
        assertEquals(null, stableLyricsAudioRouteKey(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, ""))
        assertEquals(null, stableLyricsAudioRouteKey(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, "   "))
    }

    @Test
    fun `pre-tiramisu route selection matches by exact device name`() {
        val speaker = route("Phone speaker", AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, bluetooth = false)
        val buds = route("Pixel Buds", AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, bluetooth = true)
        val car = route("Car Multimedia", AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, bluetooth = true)

        val result = resolvePreTiramisuAudioOutputRoute(listOf(speaker, buds, car), "Pixel Buds", 0)
        assertEquals(buds, result)
    }

    @Test
    fun `pre-tiramisu bluetooth route selects single bluetooth device when unambiguous`() {
        val speaker = route("Phone speaker", AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, bluetooth = false)
        val buds = route("Pixel Buds", AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, bluetooth = true)

        val result = resolvePreTiramisuAudioOutputRoute(
            listOf(speaker, buds),
            null,
            android.media.MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH
        )
        assertEquals(buds, result)
    }

    @Test
    fun `pre-tiramisu bluetooth route falls back conservatively when multiple bluetooth devices are connected`() {
        val speaker = route("Phone speaker", AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, bluetooth = false)
        val buds = route("Pixel Buds", AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, bluetooth = true)
        val car = route("Car Multimedia", AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, bluetooth = true)

        val result = resolvePreTiramisuAudioOutputRoute(
            listOf(speaker, buds, car),
            "Unmatched Route",
            android.media.MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH
        )
        org.junit.Assert.assertNotNull(result)
        org.junit.Assert.assertNull(result?.stableKey)
        org.junit.Assert.assertTrue(result?.bluetooth == true)
    }

    private fun route(name: String, type: Int, bluetooth: Boolean) = LyricsAudioOutputRoute(
        stableKey = name,
        displayName = name,
        type = type,
        bluetooth = bluetooth
    )
}
