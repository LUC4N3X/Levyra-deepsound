package com.luc4n3x.levyra.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkMeshTest {
    private fun solid(color: Int, w: Int = 120, h: Int = 120) = IntArray(w * h) { color }

    @Test
    fun solidCoverYieldsUniformTexture() {
        val mesh = buildArtworkMesh(solid(0xFF2040A0.toInt()), 120, 120, seed = 7)!!
        assertEquals(ArtworkMeshTexture * ArtworkMeshTexture, mesh.texels.size)
        assertEquals(ArtworkMeshTexture, mesh.topEdge.size)
        assertTrue(mesh.texels.all { it == mesh.texels[0] })
    }

    @Test
    fun seamRowIsTheCoverBottomBand() {
        val w = 120
        val h = 120
        val pixels = IntArray(w * h) { i -> if (i / w >= h - h / 6) 0xFFFF0000.toInt() else 0xFF0000FF.toInt() }
        val mesh = buildArtworkMesh(pixels, w, h, seed = 3)!!
        val seam = mesh.texels[0]
        val seamRed = (seam shr 16) and 0xFF
        val seamBlue = seam and 0xFF
        assertTrue(seamRed > seamBlue)
        val top = mesh.topEdge[0]
        val topBlue = top and 0xFF
        val topRed = (top shr 16) and 0xFF
        assertTrue(topBlue > topRed)
    }

    @Test
    fun blackCoverStaysDarkButNotPureBlack() {
        val mesh = buildArtworkMesh(solid(0xFF000000.toInt()), 120, 120, seed = 1)!!
        val r = (mesh.texels[0] shr 16) and 0xFF
        assertTrue(r in 1..20)
    }

    @Test
    fun brightTopBandRaisesTopLuminance() {
        val bright = buildArtworkMesh(solid(0xFFFFFFFF.toInt()), 120, 120, 0)!!
        val dark = buildArtworkMesh(solid(0xFF101010.toInt()), 120, 120, 0)!!
        assertTrue(bright.topLuminance > 0.9f && dark.topLuminance < 0.1f)
    }

    @Test
    fun sameSeedIsDeterministic() {
        val pixels = IntArray(120 * 120) { it * 2654435761.toInt() or 0xFF000000.toInt() }
        assertTrue(buildArtworkMesh(pixels, 120, 120, 42)!!.texels.contentEquals(buildArtworkMesh(pixels, 120, 120, 42)!!.texels))
    }

    @Test
    fun emptyInputIsNull() {
        assertNull(buildArtworkMesh(IntArray(0), 0, 0, 0))
        assertNull(buildArtworkMesh(IntArray(10), 120, 120, 0))
    }
}
