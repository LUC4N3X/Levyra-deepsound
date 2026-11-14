package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.domain.OrbitEqualizer
import com.luc4n3x.levyra.domain.OrbitLayout
import com.luc4n3x.levyra.domain.OrbitPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParametricEditorSessionTest {
    private val layout = OrbitLayout(listOf(40f, 45f, 50f, 270f), OrbitPosition(0.7f, 0.7f))
    private val grouped = OrbitEqualizer.shape(layout.puck, "Grouped", layout.poleAngles).profile
        .copy(id = "parametric_custom_grouped", orbitLayout = layout)
        .normalized()!!

    @Test
    fun `untouched grouped profile keeps its layout and is not dirty`() {
        val session = ParametricEditorSession.of(grouped, isNew = false)

        assertFalse(session.dirty)
        assertEquals(grouped, session.profile)
    }

    @Test
    fun `rename and preamp changes keep the layout`() {
        val session = ParametricEditorSession.of(grouped, isNew = false).copy(name = "Renamed", preampDb = -3f)

        assertTrue(session.dirty)
        assertEquals(layout, session.profile.orbitLayout)
    }

    @Test
    fun `band gain edit drops the layout`() {
        val session = ParametricEditorSession.of(grouped, isNew = false)
        val first = session.bands.first()
        val edited = session.withBand(first.key, first.band.copy(gainDb = first.band.gainDb + 1f))

        assertTrue(edited.dirty)
        assertNull(edited.profile.orbitLayout)
    }

    @Test
    fun `legacy profile without a layout is unchanged`() {
        val legacy = OrbitEqualizer.shape(OrbitPosition(0.4f, -0.3f), "Legacy").profile.copy(id = "parametric_custom_old").normalized()!!
        val session = ParametricEditorSession.of(legacy, isNew = false)

        assertFalse(session.dirty)
        assertNull(session.profile.orbitLayout)
        assertEquals(legacy, session.profile)
    }
}
