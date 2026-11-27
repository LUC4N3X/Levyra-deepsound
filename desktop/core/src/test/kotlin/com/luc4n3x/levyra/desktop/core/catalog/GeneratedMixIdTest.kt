package com.luc4n3x.levyra.desktop.core.catalog

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratedMixIdTest {
    @Test
    fun onlyGeneratedMixesAreKeptAsSnapshots() {
        assertTrue(isGeneratedMixId("RDTMAK5uy_lr0LWzGrq6FU9GIxWvFHTRPQD2LHMqlFA"))
        assertTrue(isGeneratedMixId("VLRDTMAK5uy_kset8DisdE7LSD4TNjEVvrKRTmG7a56E"))
        assertFalse(isGeneratedMixId("RDCLAK5uy_kmPRjHDECIcuVwnKsx2Ng7fyNgFKWNJFs"))
        assertFalse(isGeneratedMixId("PL4fGSI1pDJn6O1LS0XSdF3RyO0Rq_LDeI"))
    }
}
