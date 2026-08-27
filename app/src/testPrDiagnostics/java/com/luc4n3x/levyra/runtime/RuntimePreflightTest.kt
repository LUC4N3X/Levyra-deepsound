package com.luc4n3x.levyra.runtime

import com.luc4n3x.levyra.data.MAX_UPDATE_METADATA_BYTES
import com.luc4n3x.levyra.data.readUpdateMetadataBody
import com.luc4n3x.levyra.data.updateMetadataContentTypeAccepted
import java.io.IOException
import java.net.SocketTimeoutException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimePreflightTest {
    @Test
    fun failDominatesWarningAndPass() {
        val report = PreflightReport.from(
            generatedAtMs = 1L,
            results = listOf(
                PreflightResult("one", "resolver", PreflightStatus.PASS, "pass"),
                PreflightResult("two", "canvas", PreflightStatus.WARNING, "warning"),
                PreflightResult("three", "network", PreflightStatus.FAIL, "fail")
            )
        )

        assertEquals(PreflightStatus.FAIL, report.status)
    }

    @Test
    fun warningDominatesPass() {
        val report = PreflightReport.from(
            generatedAtMs = 1L,
            results = listOf(
                PreflightResult("one", "resolver", PreflightStatus.PASS, "pass"),
                PreflightResult("two", "canvas", PreflightStatus.WARNING, "warning")
            )
        )

        assertEquals(PreflightStatus.WARNING, report.status)
    }

    @Test
    fun validatesResolverAndCanvasRegistries() {
        assertTrue(
            extractorRegistryComplete(
                setOf("REEL_MUXED", "REEL_AUDIO", "PERSISTED", "DIRECT", "SEARCH"),
                setOf("PERSISTED", "STANDARD", "REEL")
            )
        )
        assertFalse(extractorRegistryComplete(setOf("DIRECT"), setOf("STANDARD")))
        assertTrue(
            canvasConfigurationComplete(
                setOf("Auto", "DataSaver", "High"),
                setOf("Auto", "Community", "Apple", "Tidal")
            )
        )
        assertFalse(canvasConfigurationComplete(setOf("Auto"), setOf("Auto")))
    }

    @Test
    fun validatesProviderEndpointAndJsonDocuments() {
        assertTrue(providerEndpointAllowed("https://api.github.com/repos/LUC4N3X/Levyra-deepsound/releases/latest"))
        assertFalse(providerEndpointAllowed("https://api.github.com.evil.example/repos/LUC4N3X/Levyra-deepsound/releases/latest"))
        assertFalse(providerEndpointAllowed("https://user@api.github.com/repos/LUC4N3X/Levyra-deepsound/releases/latest"))
        assertFalse(providerEndpointAllowed("http://api.github.com/repos/LUC4N3X/Levyra-deepsound/releases/latest"))
        assertTrue(providerEndpointAllowed("https://api.github.com:443/repos/LUC4N3X/Levyra-deepsound/releases/latest"))
        assertFalse(providerEndpointAllowed("https://api.github.com/repos/OTHER/Levyra-deepsound/releases/latest"))
        assertFalse(providerEndpointAllowed("https://api.github.com/repos/LUC4N3X/Levyra-deepsound/releases/latest?redirect=true"))
        assertFalse(providerEndpointAllowed("https://evil.example/repos/LUC4N3X/Levyra-deepsound/releases/latest"))
        assertTrue(jsonDocumentValid("{\"version\":1}"))
        assertTrue(jsonDocumentValid("[1,2,3]"))
        assertFalse(jsonDocumentValid("not-json"))
    }

    @Test
    fun boundsMetadataMimeSizeAndTimeoutFailures() {
        assertTrue(updateMetadataContentTypeAccepted("application/json; charset=utf-8"))
        assertTrue(updateMetadataContentTypeAccepted("application/vnd.github+json"))
        assertFalse(updateMetadataContentTypeAccepted("text/html"))

        val accepted = "{\"tag_name\":\"v1\"}".toResponseBody("application/json".toMediaType())
        assertEquals("{\"tag_name\":\"v1\"}", readUpdateMetadataBody(accepted))
        val oversized = ByteArray((MAX_UPDATE_METADATA_BYTES + 1L).toInt())
            .toResponseBody("application/json".toMediaType())
        assertThrows(IOException::class.java) { readUpdateMetadataBody(oversized) }

        assertEquals(
            PreflightStatus.WARNING,
            providerPreflightStatus(configAllowed = true) { throw SocketTimeoutException("timeout") }
        )
        assertEquals(
            PreflightStatus.FAIL,
            providerPreflightStatus(configAllowed = false) { true }
        )
    }
}
