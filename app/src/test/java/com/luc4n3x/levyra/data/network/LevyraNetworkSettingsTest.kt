package com.luc4n3x.levyra.data.network

import com.luc4n3x.levyra.domain.LevyraDnsMode
import com.luc4n3x.levyra.domain.LevyraNetworkSettings
import com.luc4n3x.levyra.domain.LevyraNetworkSettingsError
import com.luc4n3x.levyra.domain.LevyraNetworkSettingsValidator
import com.luc4n3x.levyra.domain.LevyraProxyMode
import java.net.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevyraNetworkSettingsTest {

    @Test
    fun defaultsUseSystemDnsAndNoProxy() {
        val settings = LevyraNetworkSettings()

        assertTrue(LevyraNetworkSettingsValidator.validate(settings, false).isEmpty())
        assertNull(LevyraDnsCatalog.endpointFor(settings))
        assertNull(LevyraNetworkConfiguration.proxyFor(settings))
    }

    @Test
    fun normalizationTrimsTextWithoutHidingInvalidPorts() {
        val normalized = LevyraNetworkSettings(
            proxyMode = LevyraProxyMode.Http,
            proxyHost = "  proxy.example.com  ",
            proxyPort = 0,
            proxyUsername = "  user  "
        ).normalized()

        assertEquals("proxy.example.com", normalized.proxyHost)
        assertEquals("user", normalized.proxyUsername)
        assertEquals(0, normalized.proxyPort)
        assertTrue(
            LevyraNetworkSettingsValidator.validate(normalized, false)
                .contains(LevyraNetworkSettingsError.ProxyPortOutOfRange)
        )
    }

    @Test
    fun malformedProxyHostsAreRejected() {
        listOf(
            "https://proxy.example",
            "example..com",
            "-proxy.example",
            "bad_name",
            "proxy/path",
            "::::",
            "a".repeat(LevyraNetworkSettings.MAX_HOST_LENGTH + 1)
        ).forEach { host ->
            val errors = LevyraNetworkSettingsValidator.validate(
                LevyraNetworkSettings(proxyMode = LevyraProxyMode.Http, proxyHost = host),
                false
            )
            assertTrue("Expected invalid host: $host", errors.contains(LevyraNetworkSettingsError.ProxyHostInvalid))
        }
    }

    @Test
    fun proxyCredentialsAreRequiredOnlyWhenAuthenticationIsEnabled() {
        val settings = LevyraNetworkSettings(
            proxyMode = LevyraProxyMode.Socks,
            proxyHost = "proxy.example.com",
            proxyPort = 1080,
            proxyAuthenticationEnabled = true
        )

        val errors = LevyraNetworkSettingsValidator.validate(settings, false)
        assertTrue(errors.contains(LevyraNetworkSettingsError.ProxyUsernameMissing))
        assertTrue(errors.contains(LevyraNetworkSettingsError.ProxyPasswordMissing))
        assertTrue(
            LevyraNetworkSettingsValidator.validate(settings.copy(proxyAuthenticationEnabled = false), false).isEmpty()
        )
    }

    @Test
    fun customDohRequiresAValidHttpsUrlWithoutUserInfoOrFragment() {
        assertTrue(LevyraNetworkSettingsValidator.validateCustomDohUrl("https://dns.example/dns-query").isEmpty())
        assertEquals(
            listOf(LevyraNetworkSettingsError.CustomDohUrlNotHttps),
            LevyraNetworkSettingsValidator.validateCustomDohUrl("http://dns.example/dns-query")
        )
        listOf(
            "https://user:pass@dns.example/dns-query",
            "https:///dns-query",
            "https://dns.example/dns-query#fragment"
        ).forEach { value ->
            assertEquals(
                listOf(LevyraNetworkSettingsError.CustomDohUrlInvalid),
                LevyraNetworkSettingsValidator.validateCustomDohUrl(value)
            )
        }
    }

    @Test
    fun httpAndSocksProxyTypesAreCreatedWithoutResolvingTheHost() {
        val http = LevyraNetworkConfiguration.proxyFor(
            LevyraNetworkSettings(proxyMode = LevyraProxyMode.Http, proxyHost = "proxy.example.com", proxyPort = 8080)
        )
        val socks = LevyraNetworkConfiguration.proxyFor(
            LevyraNetworkSettings(proxyMode = LevyraProxyMode.Socks, proxyHost = "proxy.example.com", proxyPort = 1080)
        )

        assertNotNull(http)
        assertNotNull(socks)
        assertEquals(Proxy.Type.HTTP, http?.type())
        assertEquals(Proxy.Type.SOCKS, socks?.type())
    }

    @Test
    fun dnsCatalogContainsEveryPreset() {
        val modes = listOf(
            LevyraDnsMode.Cloudflare,
            LevyraDnsMode.Google,
            LevyraDnsMode.AdGuard,
            LevyraDnsMode.Quad9
        )

        modes.forEach { mode ->
            val endpoint = LevyraDnsCatalog.endpointFor(LevyraNetworkSettings(dnsMode = mode))
            assertNotNull(endpoint)
            assertTrue(endpoint!!.url.startsWith("https://"))
            assertTrue(endpoint.bootstrapAddresses.isNotEmpty())
        }
    }
}
