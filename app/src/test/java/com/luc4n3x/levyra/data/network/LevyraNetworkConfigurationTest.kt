package com.luc4n3x.levyra.data.network

import com.luc4n3x.levyra.domain.LevyraDnsMode
import com.luc4n3x.levyra.domain.LevyraNetworkSettings
import com.luc4n3x.levyra.domain.LevyraProxyMode
import java.net.Proxy
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LevyraNetworkConfigurationTest {

    @Before
    @After
    fun resetConfiguration() {
        LevyraNetworkConfiguration.apply(LevyraNetworkSettings(), "")
    }

    @Test
    fun dnsHolderMaintainsCoherentGenerationAcrossFastPath() {
        val initialGeneration = LevyraNetworkConfiguration.generation
        val dns1 = LevyraNetworkConfiguration.dns()
        assertNotNull(dns1)

        val settings = LevyraNetworkSettings(
            dnsMode = LevyraDnsMode.Cloudflare
        )
        LevyraNetworkConfiguration.apply(settings, "")

        val newGeneration = LevyraNetworkConfiguration.generation
        assertTrue(newGeneration > initialGeneration)

        val dns2 = LevyraNetworkConfiguration.dns()
        assertNotNull(dns2)
        assertEquals(dns2, LevyraNetworkConfiguration.dns())
    }

    @Test
    fun concurrentDnsReadsAndConfigurationUpdatesDoNotProduceTornGenerations() {
        val threads = 4
        val iterations = 200
        val executor = Executors.newFixedThreadPool(threads + 1)
        val failureOccurred = AtomicBoolean(false)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threads + 1)

        for (i in 0 until threads) {
            executor.execute {
                try {
                    startLatch.await()
                    for (j in 0 until iterations) {
                        val dns = LevyraNetworkConfiguration.dns()
                        if (dns == null) {
                            failureOccurred.set(true)
                            break
                        }
                    }
                } catch (t: Throwable) {
                    failureOccurred.set(true)
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        executor.execute {
            try {
                startLatch.await()
                for (j in 0 until iterations) {
                    val mode = if (j % 2 == 0) LevyraDnsMode.Google else LevyraDnsMode.System
                    LevyraNetworkConfiguration.apply(LevyraNetworkSettings(dnsMode = mode), "")
                }
            } catch (t: Throwable) {
                failureOccurred.set(true)
            } finally {
                doneLatch.countDown()
            }
        }

        startLatch.countDown()
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS))
        assertFalse(failureOccurred.get())
        executor.shutdown()
    }

    @Test
    fun buildDnsUsesCandidateProxyPassword() {
        val settings = LevyraNetworkSettings(
            dnsMode = LevyraDnsMode.Cloudflare,
            proxyMode = LevyraProxyMode.Http,
            proxyHost = "127.0.0.1",
            proxyPort = 8080,
            proxyUsername = "testuser",
            proxyAuthenticationEnabled = true
        )

        val dns = LevyraNetworkConfiguration.buildDns(settings, "candidatePassword123")
        assertNotNull(dns)
        assertFalse(LevyraNetworkConfiguration.hasProxyPassword())
    }

    @Test
    fun proxyAuthenticatorEnforcesBasicAuthAndValidation() {
        val settings = LevyraNetworkSettings(
            proxyMode = LevyraProxyMode.Http,
            proxyHost = "127.0.0.1",
            proxyPort = 8080,
            proxyUsername = "myuser",
            proxyAuthenticationEnabled = true
        )
        val authenticator = LevyraNetworkConfiguration.proxyAuthenticatorFor(settings, "mypassword")
        assertNotNull(authenticator)

        val unauthenticatedSettings = settings.copy(proxyAuthenticationEnabled = false)
        assertNull(LevyraNetworkConfiguration.proxyAuthenticatorFor(unauthenticatedSettings, "mypassword"))

        val emptyPasswordSettings = settings.copy(proxyAuthenticationEnabled = true)
        assertNull(LevyraNetworkConfiguration.proxyAuthenticatorFor(emptyPasswordSettings, ""))
    }
}
