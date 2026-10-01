package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfilePhotoStoreTest {
    @Test
    fun `square crop keeps the centre of landscape and portrait photos`() {
        assertEquals(ProfilePhotoCrop(left = 200, top = 0, size = 600), profilePhotoSquareCrop(1000, 600))
        assertEquals(ProfilePhotoCrop(left = 0, top = 150, size = 300), profilePhotoSquareCrop(300, 600))
        assertEquals(ProfilePhotoCrop(left = 0, top = 0, size = 512), profilePhotoSquareCrop(512, 512))
    }

    @Test
    fun `square crop tolerates empty bounds`() {
        assertEquals(0, profilePhotoSquareCrop(0, 400).size)
    }

    @Test
    fun `sampling keeps the short edge at least the target size`() {
        assertEquals(1, profilePhotoSampleSize(400, 300, PROFILE_PHOTO_EDGE_PX))
        assertEquals(8, profilePhotoSampleSize(4000, 3000, PROFILE_PHOTO_EDGE_PX))
        assertEquals(2, profilePhotoSampleSize(600, 900, PROFILE_PHOTO_EDGE_PX))
        assertEquals(1, profilePhotoSampleSize(0, 0, PROFILE_PHOTO_EDGE_PX))
    }
}
