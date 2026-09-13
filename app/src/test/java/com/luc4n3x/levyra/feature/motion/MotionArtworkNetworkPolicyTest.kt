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

    @Test
    fun prefetchFollowsWifiOnlyPolicyInsteadOfRequiringUnmeteredNetworks() {
        try {
            MotionArtworkNetworkPolicy.updateWifiOnly(false)
            assertTrue(MotionArtworkNetworkPolicy.canPrefetch(online(unmetered = false)))
            assertTrue(MotionArtworkNetworkPolicy.canResolve(online(unmetered = false)))
            assertTrue(MotionArtworkNetworkPolicy.canPrefetch(online(unmetered = true)))

            MotionArtworkNetworkPolicy.updateWifiOnly(true)
            assertFalse(MotionArtworkNetworkPolicy.canPrefetch(online(unmetered = false)))
            assertFalse(MotionArtworkNetworkPolicy.canResolve(online(unmetered = false)))
            assertTrue(MotionArtworkNetworkPolicy.canPrefetch(online(unmetered = true)))
            assertFalse(MotionArtworkNetworkPolicy.canPrefetch(online(unmetered = true, dataSaverActive = true)))
        } finally {
            MotionArtworkNetworkPolicy.updateWifiOnly(false)
        }
    }

    @Test
    fun restoredWifiOnlyValueImmediatelyChangesRuntimePolicy() {
        try {
            MotionArtworkNetworkPolicy.updateWifiOnly(false)
            assertTrue(MotionArtworkNetworkPolicy.canResolve(online(unmetered = false)))

            MotionArtworkNetworkPolicy.updateWifiOnly(true)
            assertFalse(MotionArtworkNetworkPolicy.canResolve(online(unmetered = false)))

            MotionArtworkNetworkPolicy.updateWifiOnly(false)
            assertTrue(MotionArtworkNetworkPolicy.canResolve(online(unmetered = false)))
        } finally {
            MotionArtworkNetworkPolicy.updateWifiOnly(false)
        }
    }

    private fun online(
        localAllowed: Boolean = true,
        dataSaverActive: Boolean = false,
        internet: Boolean = true,
        validated: Boolean = true,
        unmetered: Boolean = false
    ) = MotionArtworkNetworkState(localAllowed, dataSaverActive, internet, validated, unmetered)
}
