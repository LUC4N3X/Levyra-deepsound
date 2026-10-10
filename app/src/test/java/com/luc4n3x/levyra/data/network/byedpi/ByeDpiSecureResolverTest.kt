package com.luc4n3x.levyra.data.network.byedpi

import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.CopyOnWriteArrayList
import okhttp3.Dns
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ByeDpiSecureResolverTest {
    private val host = "rr4---sn-hpa7znsz.googlevideo.com"
    private val youtubeAddress = byteArrayOf(173.toByte(), 194.toByte(), 182.toByte(), 230.toByte())
    private val calls = CopyOnWriteArrayList<String>()

    private fun answering(name: String, vararg raw: ByteArray) = name to Dns { hostname ->
        calls += name
        raw.map { InetAddress.getByAddress(hostname, it) }
    }

    private fun failing(name: String) = name to Dns { hostname ->
        calls += name
        throw UnknownHostException("$name unreachable for $hostname")
    }

    private val systemFallback = Dns { hostname ->
        calls += "system"
        listOf(InetAddress.getByAddress(hostname, byteArrayOf(10, 10, 34, 34)))
    }

    @Test
    fun firstHealthyDohResolverWinsAndSystemDnsIsNotUsed() {
        val resolver = ByeDpiSecureResolver(
            listOf(answering("google", youtubeAddress), answering("cloudflare", youtubeAddress)),
            systemFallback
        )

        val result = resolver.lookup(host)

        assertArrayEquals(youtubeAddress, result.single().address)
        assertEquals(listOf("google"), calls)
    }

    @Test
    fun failedDohResolverFallsThroughToNextDohResolver() {
        val resolver = ByeDpiSecureResolver(listOf(failing("google"), answering("cloudflare", youtubeAddress)), systemFallback)

        val result = resolver.lookup(host)

        assertArrayEquals(youtubeAddress, result.single().address)
        assertEquals(listOf("google", "cloudflare"), calls)
    }

    @Test
    fun allDohFailuresFallBackToSystemResolverAsLastResort() {
        val resolver = ByeDpiSecureResolver(listOf(failing("google"), failing("cloudflare"), failing("adguard")), systemFallback)

        resolver.lookup(host)

        assertEquals(listOf("google", "cloudflare", "adguard", "system"), calls)
    }

    @Test
    fun emptyResolverListUsesSystemResolver() {
        ByeDpiSecureResolver(emptyList(), systemFallback).lookup(host)

        assertEquals(listOf("system"), calls)
    }

    @Test
    fun nonPublicDohAnswersAreRejectedAndNextResolverIsTried() {
        val loopback = byteArrayOf(127, 0, 0, 1)
        val privateNetwork = byteArrayOf(10, 0, 0, 1)
        val carrierNat = byteArrayOf(100, 64, 0, 1)
        val resolver = ByeDpiSecureResolver(
            listOf(answering("poisoned", loopback, privateNetwork, carrierNat), answering("cloudflare", youtubeAddress)),
            systemFallback
        )

        val result = resolver.lookup(host)

        assertArrayEquals(youtubeAddress, result.single().address)
        assertEquals(listOf("poisoned", "cloudflare"), calls)
    }

    @Test
    fun answersAreCachedUntilTtlExpires() {
        var now = 0L
        val resolver = ByeDpiSecureResolver(
            listOf(answering("google", youtubeAddress)),
            systemFallback,
            ttlMs = 1_000L,
            clockMs = { now }
        )

        resolver.lookup(host)
        now = 999L
        resolver.lookup(host.uppercase())
        assertEquals(listOf("google"), calls)

        now = 1_000L
        resolver.lookup(host)
        assertEquals(listOf("google", "google"), calls)
    }

    @Test
    fun failuresAreNotCached() {
        var healthy = false
        val flaky = "google" to Dns { hostname ->
            calls += "google"
            if (!healthy) throw UnknownHostException(hostname)
            listOf(InetAddress.getByAddress(hostname, youtubeAddress))
        }
        val resolver = ByeDpiSecureResolver(listOf(flaky), systemFallback)

        resolver.lookup(host)
        healthy = true
        val result = resolver.lookup(host)

        assertArrayEquals(youtubeAddress, result.single().address)
        assertEquals(listOf("google", "system", "google"), calls)
    }
}
