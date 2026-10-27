package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.DockScrollEffect
import com.luc4n3x.levyra.domain.LevyraInterfaceSettings
import com.luc4n3x.levyra.domain.LevyraSkin
import com.luc4n3x.levyra.domain.MiniPlayerStyle
import com.luc4n3x.levyra.domain.NavStyle
import com.luc4n3x.levyra.domain.dockMorphActive
import com.luc4n3x.levyra.domain.effectiveDockScrollEffect
import com.luc4n3x.levyra.domain.supportsDockMorph
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockMorphSettingsTest {
    @Test
    fun liquidCapsuleIsSecondInBothStudios() {
        assertEquals(listOf(MiniPlayerStyle.Original, MiniPlayerStyle.LiquidCapsule), MiniPlayerStyle.studioOrder.take(2))
        assertEquals(MiniPlayerStyle.entries.toSet(), MiniPlayerStyle.studioOrder.toSet())
        assertEquals(MiniPlayerStyle.entries.size, MiniPlayerStyle.studioOrder.size)
        assertEquals(listOf(NavStyle.Original, NavStyle.LiquidCapsule), NavStyle.studioOrder.take(2))
        assertEquals(NavStyle.entries.toSet(), NavStyle.studioOrder.toSet())
        assertEquals(NavStyle.entries.size, NavStyle.studioOrder.size)
    }

    @Test
    fun newInstallsKeepTheTideScrollEffect() {
        val settings = LevyraInterfaceSettings()

        assertEquals(DockScrollEffect.Tide, settings.dockScrollEffect)
        assertFalse(settings.dockMorphActive())
    }

    @Test
    fun unknownStoredValuesFallBackToTide() {
        assertEquals(DockScrollEffect.Tide, DockScrollEffect.from(null))
        assertEquals(DockScrollEffect.Tide, DockScrollEffect.from("classic"))
        DockScrollEffect.entries.forEach { assertEquals(it, DockScrollEffect.from(it.storageId)) }
    }

    @Test
    fun backupRoundTripKeepsEveryScrollEffect() {
        DockScrollEffect.entries.forEach { effect ->
            val restored = backupInterfaceSettingsFromJson(
                backupInterfaceSettingsToJson(LevyraInterfaceSettings(dockScrollEffect = effect))
            )
            assertEquals(effect, restored.dockScrollEffect)
        }
    }

    @Test
    fun legacyBackupRestoresTide() {
        assertEquals(DockScrollEffect.Tide, backupInterfaceSettingsFromJson(JSONObject()).dockScrollEffect)
    }

    @Test
    fun onlyDefaultNocturnePrismTorqueAndIvorySupportDockEffects() {
        val supported = LevyraSkin.entries.filter { it.supportsDockMorph }.toSet()

        assertEquals(
            setOf(LevyraSkin.Default, LevyraSkin.Nocturne78, LevyraSkin.Prism, LevyraSkin.Torque, LevyraSkin.Ivory),
            supported
        )
    }

    @Test
    fun unsupportedSkinKeepsTheChoiceButBehavesLikeTide() {
        val merge = LevyraInterfaceSettings(dockScrollEffect = DockScrollEffect.Merge)

        assertTrue(merge.dockMorphActive())
        val spotify = merge.copy(skin = LevyraSkin.SpotifyStyle)
        assertEquals(DockScrollEffect.Merge, spotify.dockScrollEffect)
        assertEquals(DockScrollEffect.Tide, spotify.effectiveDockScrollEffect())
        assertFalse(spotify.dockMorphActive())
        assertTrue(spotify.copy(skin = LevyraSkin.Prism).dockMorphActive())
        assertEquals(
            DockScrollEffect.Tide,
            LevyraInterfaceSettings(dockScrollEffect = DockScrollEffect.Off, skin = LevyraSkin.AppleMusicStyle).effectiveDockScrollEffect()
        )
    }
}
