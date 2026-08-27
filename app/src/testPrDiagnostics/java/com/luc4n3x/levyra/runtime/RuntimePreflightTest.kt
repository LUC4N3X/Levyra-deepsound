package com.luc4n3x.levyra.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        assertTrue(jsonDocumentValid("{\"version\":1}"))
        assertTrue(jsonDocumentValid("[1,2,3]"))
        assertFalse(jsonDocumentValid("not-json"))
    }
}
