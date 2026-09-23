package com.luc4n3x.levyra.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParametricProfilesTest {
    @Test
    fun parseDecimalRejectsNonFiniteAndMalformedInput() {
        assertEquals(1.5f, ParametricProfiles.parseDecimal(" 1,5 "))
        assertEquals(-3f, ParametricProfiles.parseDecimal("-3"))
        listOf("", "abc", "1.2.3", "NaN", "Infinity", "-Infinity", "1e999").forEach {
            assertNull(it, ParametricProfiles.parseDecimal(it))
        }
    }

    @Test
    fun editorBoundsRejectInvalidValues() {
        assertFalse(ParametricProfiles.validFrequency(0f))
        assertFalse(ParametricProfiles.validFrequency(-100f))
        assertFalse(ParametricProfiles.validFrequency(Float.NaN))
        assertFalse(ParametricProfiles.validQ(0f))
        assertFalse(ParametricProfiles.validQ(0.01f))
        assertFalse(ParametricProfiles.validGain(99f))
        assertFalse(ParametricProfiles.validPreamp(12f))
        assertTrue(ParametricProfiles.validFrequency(1_000f))
        assertTrue(ParametricProfiles.validQ(0.71f))
    }

    @Test
    fun duplicateGetsNewStableIdentityAndLeavesSourceUntouched() {
        val source = ParametricEqualizer.defaultProfile.copy(id = "parametric_imported_abc", name = "AutoEQ")
        val copy = ParametricProfiles.duplicate(source, "Mine")
        assertNotEquals(source.id, copy.id)
        assertTrue(ParametricProfiles.isCustom(copy))
        assertEquals(source.bands, copy.bands)
        assertEquals("parametric_imported_abc", source.id)
        assertEquals("AutoEQ", source.name)
    }

    @Test
    fun nameConflictsIgnoreCaseAndSelf() {
        val existing = listOf(ParametricProfiles.create("Warm"))
        assertTrue(ParametricProfiles.nameTaken(" warm ", null, existing))
        assertFalse(ParametricProfiles.nameTaken("Warm", existing.first().id, existing))
        assertEquals("Warm 2", ParametricProfiles.availableName("Warm", "Warm", existing) { stem, n -> "$stem $n" })
        val longName = "L".repeat(ParametricEqualizer.MAX_NAME_CHARS)
        val crowded = listOf(ParametricProfiles.create(longName), ParametricProfiles.create(longName.dropLast(2) + " 2"))
        val next = ParametricProfiles.availableName(longName, longName, crowded) { stem, n -> "$stem $n" }
        assertTrue(next.length <= ParametricEqualizer.MAX_NAME_CHARS)
        assertTrue(next.endsWith(" 3"))
    }

    @Test
    fun upsertKeepsPositionOnRename() {
        val first = ParametricProfiles.create("A")
        val second = ParametricProfiles.create("B")
        val updated = ParametricProfiles.upsert(listOf(first, second), first.copy(name = "Renamed"))
        assertEquals(listOf(first.id, second.id), updated.map { it.id })
        assertEquals("Renamed", updated.first().name)
    }

    @Test
    fun responseOfFlatProfileIsThePreamp() {
        val frequencies = floatArrayOf(50f, 1_000f, 12_000f)
        val out = FloatArray(frequencies.size)
        ParametricBiquad.responseDb(ParametricEqualizer.defaultProfile.copy(preampDb = -2f), frequencies, 48_000, out)
        out.forEach { assertEquals(-2f, it, 0.01f) }
        val boosted = ParametricEqualizer.defaultProfile.copy(
            bands = listOf(ParametricEqBand(1_000f, 6f, 1f, ParametricFilterType.PEAK))
        )
        ParametricBiquad.responseDb(boosted, frequencies, 48_000, out)
        assertEquals(6f, out[1], 0.05f)
    }
}
