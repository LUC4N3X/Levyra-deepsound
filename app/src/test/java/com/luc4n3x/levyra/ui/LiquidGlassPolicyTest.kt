package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LiquidGlassPolicyTest {
    @Test
    fun `full glass is selected on capable Android 12 plus devices`() {
        val tier = resolveLiquidGlassTier(
            LiquidGlassCapabilities(
                apiLevel = 31,
                lowRamDevice = false,
                powerSaveMode = false,
                hardwareAccelerated = true,
                enabled = true
            )
        )

        assertEquals(LiquidGlassTier.Full, tier)
    }

    @Test
    fun `power saver disables blur and animation but keeps lightweight glass`() {
        val tier = resolveLiquidGlassTier(
            LiquidGlassCapabilities(
                apiLevel = 35,
                lowRamDevice = false,
                powerSaveMode = true,
                hardwareAccelerated = true,
                enabled = true
            )
        )

        assertEquals(LiquidGlassTier.Lite, tier)
    }

    @Test
    fun `pre Android 12 devices receive the lightweight renderer`() {
        val tier = resolveLiquidGlassTier(
            LiquidGlassCapabilities(
                apiLevel = 30,
                lowRamDevice = false,
                powerSaveMode = false,
                hardwareAccelerated = true,
                enabled = true
            )
        )

        assertEquals(LiquidGlassTier.Lite, tier)
    }

    @Test
    fun `low ram or software rendered devices use the static fallback`() {
        assertEquals(
            LiquidGlassTier.Static,
            resolveLiquidGlassTier(
                LiquidGlassCapabilities(
                    apiLevel = 35,
                    lowRamDevice = true,
                    powerSaveMode = false,
                    hardwareAccelerated = true,
                    enabled = true
                )
            )
        )
        assertEquals(
            LiquidGlassTier.Static,
            resolveLiquidGlassTier(
                LiquidGlassCapabilities(
                    apiLevel = 35,
                    lowRamDevice = false,
                    powerSaveMode = false,
                    hardwareAccelerated = false,
                    enabled = true
                )
            )
        )
    }

    @Test
    fun `disabled host never records a hardware backdrop`() {
        val tier = resolveLiquidGlassTier(
            LiquidGlassCapabilities(
                apiLevel = 35,
                lowRamDevice = false,
                powerSaveMode = false,
                hardwareAccelerated = true,
                enabled = false
            )
        )

        assertEquals(LiquidGlassTier.Static, tier)
    }
}
