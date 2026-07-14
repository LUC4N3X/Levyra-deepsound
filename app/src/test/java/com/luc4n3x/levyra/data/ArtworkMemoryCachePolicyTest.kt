package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkMemoryCachePolicyTest {
    private val mib = 1024L * 1024L

    @Test
    fun lowRamDevicesUseSmallCache() {
        val bytes = ArtworkMemoryCachePolicy.maxSizeBytes(
            ArtworkMemoryDeviceProfile(
                memoryClassMb = 128,
                largeMemoryClassMb = 128,
                lowRamDevice = true,
                largeHeapEnabled = false
            )
        )

        assertEquals(24L * mib, bytes)
    }

    @Test
    fun commonPhonesStayWellBelowPreviousThirtyFivePercentBudget() {
        val profile = ArtworkMemoryDeviceProfile(
            memoryClassMb = 512,
            largeMemoryClassMb = 512,
            lowRamDevice = false,
            largeHeapEnabled = false
        )
        val bytes = ArtworkMemoryCachePolicy.maxSizeBytes(profile)

        assertEquals(80L * mib, bytes)
        assertTrue(bytes < (profile.memoryClassMb * mib * 35L / 100L))
    }

    @Test
    fun largeHeapDevicesRemainCapped() {
        val bytes = ArtworkMemoryCachePolicy.maxSizeBytes(
            ArtworkMemoryDeviceProfile(
                memoryClassMb = 512,
                largeMemoryClassMb = 1024,
                lowRamDevice = false,
                largeHeapEnabled = true
            )
        )

        assertEquals(112L * mib, bytes)
    }

    @Test
    fun cacheNeverExceedsQuarterOfEffectiveHeap() {
        val profile = ArtworkMemoryDeviceProfile(
            memoryClassMb = 192,
            largeMemoryClassMb = 192,
            lowRamDevice = false,
            largeHeapEnabled = false
        )
        val bytes = ArtworkMemoryCachePolicy.maxSizeBytes(profile)

        assertTrue(bytes <= profile.memoryClassMb * mib / 4L)
    }
}
