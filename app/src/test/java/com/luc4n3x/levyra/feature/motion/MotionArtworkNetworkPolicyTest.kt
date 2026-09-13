package com.luc4n3x.levyra.feature.motion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionArtworkNetworkPolicyTest {
    @Test
    fun wifiOnlyAllowsOnlyValidatedUnmeteredNetworks() {
        assertTrue(MotionArtworkNetworkPolicy.canResolve(wifiOnly = true, network = online(unmetered = true)))
        assertFalse(MotionArtworkNetworkPolicy.canResolve(wifiOnly = true, network = online(unmetered = false)))
    }

    @Test
    fun disabledWifiOnlyAllowsValidatedMobileAndMeteredNetworks() {
        assertTrue(MotionArtworkNetworkPolicy.canResolve(wifiOnly = false, network = online(unmetered = true)))
        assertTrue(MotionArtworkNetworkPolicy.canResolve(wifiOnly = false, network = online(unmetered = false)))
    }

    @Test
    fun offlineAndIndependentRestrictionsAlwaysBlockRemoteResolution() {
        assertFalse(MotionArtworkNetworkPolicy.canResolve(wifiOnly = false, network = online(validated = false)))
        assertFalse(MotionArtworkNetworkPolicy.canResolve(wifiOnly = false, network = online(internet = false)))
        assertFalse(MotionArtworkNetworkPolicy.canResolve(wifiOnly = false, network = online(dataSaverActive = true)))
        assertFalse(MotionArtworkNetworkPolicy.canResolve(wifiOnly = false, network = online(localAllowed = false)))
    }

    private fun online(
        localAllowed: Boolean = true,
        dataSaverActive: Boolean = false,
        internet: Boolean = true,
        validated: Boolean = true,
        unmetered: Boolean = false
    ) = MotionArtworkNetworkState(localAllowed, dataSaverActive, internet, validated, unmetered)
}
